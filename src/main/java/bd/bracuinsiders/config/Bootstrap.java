package bd.bracuinsiders.config;

import bd.bracuinsiders.model.*;
import bd.bracuinsiders.repository.*;
import bd.bracuinsiders.service.*;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.time.*;
import java.util.*;

@Component
public class Bootstrap implements ApplicationRunner {
    private final JdbcTemplate db;
    private final PasswordEncoder passwords;
    private final CategoryRepository categories;
    private final PostRepository posts;
    private final MediaRepository media;
    private final ObjectMapper json;

    @Value("${app.bootstrap.username}")
    private String username;

    @Value("${app.bootstrap.password}")
    private String password;

    @Value("${app.seed-edition}")
    private boolean seed;

    public Bootstrap(
            JdbcTemplate db,
            PasswordEncoder passwords,
            CategoryRepository categories,
            PostRepository posts,
            MediaRepository media,
            ObjectMapper json) {
        this.db = db;
        this.passwords = passwords;
        this.categories = categories;
        this.posts = posts;
        this.media = media;
        this.json = json;
    }

    record Picture(String key, String file, String alt, String credit) {}

    record Entry(
            String slug,
            String title,
            String caption,
            String body,
            String category,
            String source,
            String video,
            LocalDate event,
            LocalDate end,
            String info,
            boolean featured,
            Instant published,
            List<String> images) {}

    record Starter(List<Picture> images, List<Entry> posts) {}

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        // One initializer at a time, including on multi-instance deployments.
        db.execute("SELECT pg_advisory_xact_lock(56782024)");
        Long admins = db.queryForObject("SELECT count(*) FROM admin_accounts", Long.class);
        if (admins == 0) {
            if (!username.matches("[A-Za-z0-9._@+-]{3,80}")
                    || password.length() < 14
                    || password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72)
                throw new IllegalStateException(
                        "Set ADMIN_USERNAME (3–80 characters) and ADMIN_PASSWORD (14–72 bytes) for"
                            + " the first launch.");
            db.update(
                    "INSERT INTO admin_accounts(username,password_hash,role) VALUES (?,?,'ADMIN')",
                    username,
                    passwords.encode(password));
        }
        password = "";
        int first =
                db.update(
                        "INSERT INTO app_state(key_name,value_text) VALUES ('initial-edition',?) ON"
                            + " CONFLICT DO NOTHING",
                        seed ? "loaded" : "skipped");
        if (first == 0 || !seed) return;
        try (InputStream input =
                new ClassPathResource("edition/opening-edition.json").getInputStream()) {
            Starter edition = json.readValue(input, Starter.class);
            Map<String, Media> assets = new HashMap<>();
            for (Picture picture : edition.images()) {
                assets.put(
                        picture.key(),
                        media.save(
                                new Media(
                                        picture.file(),
                                        picture.alt(),
                                        "image/webp",
                                        "/images/" + picture.file(),
                                        picture.credit())));
            }
            for (Entry item : edition.posts()) {
                Post post = new Post();
                post.setSlug(item.slug());
                post.edit(
                        item.title(),
                        item.caption(),
                        item.body(),
                        categories.findBySlug(item.category()).orElseThrow(),
                        item.source(),
                        item.video(),
                        item.event(),
                        item.end(),
                        item.info(),
                        item.featured(),
                        item.images().stream()
                                .map(key -> Objects.requireNonNull(assets.get(key)))
                                .toList());
                post.setPublication(true, item.published());
                posts.save(post);
            }
        }
    }
}
