package bd.bracuinsiders;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import bd.bracuinsiders.repository.*;
import bd.bracuinsiders.service.*;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.*;
import org.springframework.test.context.*;
import org.springframework.test.web.servlet.*;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

import javax.imageio.ImageIO;

/** Run against PostgreSQL. Each run owns a fresh schema, populated by the production migrations. */
@SpringBootTest
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "TEST_DB_URL", matches = ".+")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NewsroomIntegrationTest {
    private static final String SCHEMA =
            "insiders_test_" + UUID.randomUUID().toString().replace("-", "");
    private static final String PASSWORD = Codes.generate();
    @Autowired MockMvc web;
    @Autowired JdbcTemplate db;
    @Autowired PostRepository posts;
    @Autowired CategoryRepository categories;
    @Autowired MediaRepository media;
    @Autowired LetterRepository letters;

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry r) {
        String url = System.getenv("TEST_DB_URL");
        r.add(
                "spring.datasource.url",
                () -> url + (url.contains("?") ? "&" : "?") + "currentSchema=" + SCHEMA);
        r.add(
                "spring.datasource.username",
                () ->
                        System.getenv()
                                .getOrDefault(
                                        "TEST_DB_USER",
                                        System.getenv().getOrDefault("DB_USER", "insiders")));
        r.add(
                "spring.datasource.password",
                () ->
                        System.getenv()
                                .getOrDefault("TEST_DB_PASSWORD", System.getenv("DB_PASSWORD")));
        r.add("spring.flyway.schemas", () -> SCHEMA);
        r.add("spring.jpa.properties.hibernate.default_schema", () -> SCHEMA);
        r.add("app.bootstrap.username", () -> "integration-editor");
        r.add("app.bootstrap.password", () -> PASSWORD);
        r.add("app.seed-edition", () -> true);
    }

    @AfterAll
    void removeTestSchema() {
        // SCHEMA is generated above, never derived from operator input.
        db.execute("DROP SCHEMA " + SCHEMA + " CASCADE");
    }

    private MockHttpSession login() throws Exception {
        return (MockHttpSession)
                web.perform(
                                post("/admin/login")
                                        .with(csrf())
                                        .param("username", "integration-editor")
                                        .param("password", PASSWORD))
                        .andExpect(status().is3xxRedirection())
                        .andExpect(redirectedUrl("/admin"))
                        .andReturn()
                        .getRequest()
                        .getSession(false);
    }

    private static MockMultipartFile picture(String field, String name) throws Exception {
        var bytes = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(12, 8, BufferedImage.TYPE_INT_RGB), "png", bytes);
        return new MockMultipartFile(field, name, "image/png", bytes.toByteArray());
    }

    @Test
    void publicPagesAndAdminBoundary() throws Exception {
        for (String path :
                List.of(
                        "/",
                        "/posts",
                        "/events",
                        "/events?past=true",
                        "/campus-updates",
                        "/student-stories",
                        "/contact",
                        "/replies",
                        "/credits",
                        "/admin/login",
                        "/posts/the-insiders-logo-reveal",
                        "/posts/inside-bracus-mars-rover-team",
                        "/api/posts",
                        "/api/posts/inside-bracus-mars-rover-team",
                        "/health",
                        "/video/insiders-logo-reveal.mp4")) {
            web.perform(get(path)).andExpect(status().isOk());
        }
        web.perform(get("/"))
                .andExpect(
                        content()
                                .string(
                                        org.hamcrest.Matchers.containsString(
                                                "https://www.facebook.com/share/1DQx2ySayW/")));
        web.perform(get("/admin/messages")).andExpect(status().is3xxRedirection());
        web.perform(post("/contact")).andExpect(status().isForbidden());
        web.perform(get("/posts/no-such-story")).andExpect(status().isNotFound());
        var session = login();
        for (String path :
                List.of(
                        "/admin",
                        "/admin/posts",
                        "/admin/messages",
                        "/admin/categories",
                        "/admin/media",
                        "/admin/settings",
                        "/admin/posts/new"))
            web.perform(get(path).session(session)).andExpect(status().isOk());
        long id =
                posts.findBySlugAndPublishedTrue("inside-bracus-mars-rover-team")
                        .orElseThrow()
                        .getId();
        web.perform(get("/admin/posts/" + id + "/edit").session(session))
                .andExpect(status().isOk());
        web.perform(post("/admin/logout").session(session).with(csrf()))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void draftPublishEditUnpublishAndMediaVisibility() throws Exception {
        var session = login();
        long category = categories.findBySlug("campus-news").orElseThrow().getId();
        web.perform(
                        multipart("/admin/posts/new")
                                .file(picture("uploads", "desk-test.png"))
                                .file(picture("uploads", "second.png"))
                                .session(session)
                                .with(csrf())
                                .param("title", "Editorial workflow test")
                                .param("caption", "A dated campus caption.")
                                .param("body", "First paragraph.\n\nSecond paragraph.")
                                .param("categoryId", Long.toString(category))
                                .param("intent", "draft"))
                .andExpect(redirectedUrl("/admin/posts"));
        long id =
                db.queryForObject(
                        "SELECT id FROM posts WHERE title='Editorial workflow test'", Long.class);
        String slug = posts.findById(id).orElseThrow().getSlug();
        long imageId =
                db.queryForObject(
                        "SELECT media_id FROM post_media WHERE post_id=? AND position=0",
                        Long.class,
                        id);
        web.perform(get("/posts/" + slug)).andExpect(status().isNotFound());
        web.perform(get("/api/posts/" + slug)).andExpect(status().isNotFound());
        web.perform(get("/media/" + imageId)).andExpect(status().isNotFound());
        web.perform(get("/admin/media/" + imageId + "/file").session(session))
                .andExpect(status().isOk());
        action(session, id, "publish");
        action(session, id, "pin");
        web.perform(get("/posts/" + slug))
                .andExpect(status().isOk())
                .andExpect(
                        content()
                                .string(org.hamcrest.Matchers.containsString("Second paragraph.")));
        web.perform(get("/media/" + imageId))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"));
        long version = posts.findById(id).orElseThrow().getVersion();
        web.perform(
                        multipart("/admin/posts/" + id + "/edit")
                                .session(session)
                                .with(csrf())
                                .param("version", Long.toString(version))
                                .param("title", "Edited workflow")
                                .param("caption", "Edited caption")
                                .param("categoryId", Long.toString(category))
                                .param("selectedMedia", Long.toString(imageId))
                                .param("intent", "publish")
                                .param("videoUrl", "https://youtu.be/abcdefghijk"))
                .andExpect(redirectedUrl("/admin/posts"));
        web.perform(get("/posts/" + slug))
                .andExpect(
                        content()
                                .string(
                                        org.hamcrest.Matchers.containsString(
                                                "youtube-nocookie.com/embed/abcdefghijk")));
        web.perform(
                        post("/admin/posts/" + id + "/action")
                                .session(session)
                                .with(csrf())
                                .param("version", Long.toString(version))
                                .param("action", "delete"))
                .andExpect(flash().attributeExists("error"));
        assertTrue(posts.existsById(id));
        action(session, id, "unpublish");
        web.perform(get("/media/" + imageId)).andExpect(status().isNotFound());
        action(session, id, "delete");
        web.perform(post("/admin/media/" + imageId + "/delete").session(session).with(csrf()))
                .andExpect(redirectedUrl("/admin/media"));
        assertFalse(media.existsById(imageId));
    }

    private void action(MockHttpSession session, long id, String action) throws Exception {
        String version = String.valueOf(posts.findById(id).orElseThrow().getVersion());
        web.perform(
                        post("/admin/posts/" + id + "/action")
                                .session(session)
                                .with(csrf().asHeader())
                                .param("version", version)
                                .param("action", action))
                .andExpect(redirectedUrl("/admin/posts"));
    }

    @Test
    void privateMessageAttachmentReceiptAndArchive() throws Exception {
        byte[] pdf = "%PDF-1.4\nTest document\n%%EOF\n".getBytes(StandardCharsets.US_ASCII);
        var file = new MockMultipartFile("attachment", "notes.pdf", "application/pdf", pdf);
        MvcResult result =
                web.perform(
                                multipart("/contact")
                                        .file(file)
                                        .with(csrf())
                                        .param("name", "Test reader")
                                        .param("subject", "Private desk test")
                                        .param("message", "Private correspondence for the desk."))
                        .andExpect(redirectedUrl("/contact"))
                        .andReturn();
        String code = (String) result.getFlashMap().get("receipt");
        long id =
                db.queryForObject(
                        "SELECT id FROM messages WHERE subject='Private desk test'", Long.class);
        assertNotEquals(
                code,
                db.queryForObject(
                        "SELECT receipt_hash FROM messages WHERE id=?", String.class, id));
        web.perform(get("/admin/messages/" + id + "/attachment"))
                .andExpect(status().is3xxRedirection());
        var session = login();
        web.perform(get("/admin/messages/" + id).session(session)).andExpect(status().isOk());
        web.perform(get("/admin/messages/" + id + "/attachment").session(session))
                .andExpect(content().bytes(pdf));
        web.perform(
                        post("/admin/messages/" + id + "/reply")
                                .session(session)
                                .with(csrf())
                                .param("body", "Your private reply"))
                .andExpect(redirectedUrl("/admin/messages/" + id));
        web.perform(post("/replies").with(csrf()).param("code", code))
                .andExpect(status().isOk())
                .andExpect(
                        content()
                                .string(org.hamcrest.Matchers.containsString("Your private reply")))
                .andExpect(
                        content()
                                .string(
                                        org.hamcrest.Matchers.not(
                                                org.hamcrest.Matchers.containsString(
                                                        "Private correspondence for the desk."))));
        for (String action : List.of("archive", "restore", "delete"))
            web.perform(
                            post("/admin/messages/" + id + "/action")
                                    .session(session)
                                    .with(csrf())
                                    .param("action", action))
                    .andExpect(redirectedUrl("/admin/messages"));
        assertFalse(letters.existsById(id));
        web.perform(post("/replies").with(csrf()).param("code", code))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void settingsCategoryFilteringAndAbusePrevention() throws Exception {
        var session = login();
        web.perform(
                        post("/admin/categories")
                                .session(session)
                                .with(csrf())
                                .param("name", "Test section")
                                .param("section", "CAMPUS"))
                .andExpect(redirectedUrl("/admin/categories"));
        long id =
                db.queryForObject(
                        "SELECT id FROM categories WHERE name='Test section'", Long.class);
        web.perform(
                        post("/admin/categories")
                                .session(session)
                                .with(csrf())
                                .param("id", String.valueOf(id))
                                .param("name", "Renamed section")
                                .param("section", "STORIES"))
                .andExpect(flash().attributeExists("notice"));
        web.perform(post("/admin/categories/" + id + "/delete").session(session).with(csrf()))
                .andExpect(flash().attributeExists("notice"));
        web.perform(get("/api/posts").param("q", "Merul Badda"))
                .andExpect(jsonPath("$.totalItems").value(org.hamcrest.Matchers.greaterThan(0)));
        web.perform(
                        post("/admin/settings")
                                .session(session)
                                .with(csrf())
                                .param("name", "BRACU Insiders")
                                .param("tagline", "Your Campus. Your Community. Your Updates.")
                                .param("email", "bracuinsiders2024@gmail.com")
                                .param("facebook", "https://www.facebook.com/share/1DQx2ySayW/")
                                .param("version", "0"))
                .andExpect(flash().attributeExists("notice"));
        for (int i = 0; i < 6; i++) {
            var req =
                    multipart("/contact")
                            .with(csrf())
                            .with(
                                    r -> {
                                        r.setRemoteAddr("192.0.2.19");
                                        return r;
                                    })
                            .param("name", "Test reader")
                            .param("subject", "Test")
                            .param("message", "Test")
                            .param("website", "bot field");
            web.perform(req).andExpect(status().is(i < 5 ? 422 : 429));
        }
    }
}
