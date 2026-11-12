package bd.bracuinsiders.web;

import bd.bracuinsiders.model.*;
import bd.bracuinsiders.repository.*;
import bd.bracuinsiders.service.*;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.*;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.*;

@Controller
@RequestMapping("/admin")
public class AdminDesk {
    private final Edition edition;
    private final Publishing publishing;
    private final PostRepository posts;
    private final CategoryRepository categories;
    private final MediaRepository media;
    private final LetterRepository letters;
    private final DeskLog log;

    public AdminDesk(
            Edition edition,
            Publishing publishing,
            PostRepository posts,
            CategoryRepository categories,
            MediaRepository media,
            LetterRepository letters,
            DeskLog log) {
        this.edition = edition;
        this.publishing = publishing;
        this.posts = posts;
        this.categories = categories;
        this.media = media;
        this.letters = letters;
        this.log = log;
    }

    @InitBinder("form")
    public void formBinder(WebDataBinder binder) {
        binder.initDirectFieldAccess();
        binder.setAllowedFields(
                "version",
                "title",
                "caption",
                "body",
                "categoryId",
                "featured",
                "sourceUrl",
                "videoUrl",
                "eventDate",
                "eventEnd",
                "eventInfo",
                "publishedAt",
                "selectedMedia",
                "coverId",
                "uploads");
        binder.setAutoGrowCollectionLimit(8);
    }

    @GetMapping("/login")
    public String login(Model m) {
        m.addAttribute("page", "Editor sign-in");
        return "admin/login";
    }

    @GetMapping({"", "/"})
    public String dashboard(Model m) {
        m.addAttribute("page", "Overview");
        m.addAttribute("published", posts.countByPublishedTrue());
        m.addAttribute("drafts", posts.count() - posts.countByPublishedTrue());
        m.addAttribute("messages", letters.count());
        m.addAttribute("unread", letters.countByArchivedFalseAndReadFalse());
        m.addAttribute("activity", log.recent());
        m.addAttribute(
                "recent", edition.browse(Edition.Scope.ALL, null, null, 0, 5, null).getContent());
        return "admin/dashboard";
    }

    @GetMapping("/posts")
    public String list(
            @RequestParam(defaultValue = "all") String state,
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "0") int page,
            Model m) {
        Edition.Scope scope =
                switch (state) {
                    case "published" -> Edition.Scope.PUBLISHED;
                    case "draft" -> Edition.Scope.DRAFT;
                    default -> Edition.Scope.ALL;
                };
        m.addAttribute("page", "Manage posts");
        m.addAttribute("state", state);
        m.addAttribute("q", q);
        m.addAttribute("results", edition.browse(scope, null, q, page, 20, null));
        return "admin/posts";
    }

    private String editor(Long id, PostForm form, Model m) {
        m.addAttribute("page", id == null ? "New post" : "Edit post");
        m.addAttribute("postId", id);
        m.addAttribute("form", form);
        m.addAttribute("categories", categories.findAllByOrderByIdAsc());
        m.addAttribute("assets", media.findAll(Sort.by(Sort.Direction.DESC, "id")));
        return "admin/editor";
    }

    @GetMapping("/posts/new")
    public String create(Model m) {
        return editor(null, new PostForm(), m);
    }

    @GetMapping("/posts/{id}/edit")
    public String edit(@PathVariable long id, Model m) {
        return editor(id, PostForm.from(edition.editorial(id)), m);
    }

    @PostMapping({"/posts/new", "/posts/{id}/edit"})
    public String save(
            @PathVariable(required = false) Long id,
            @ModelAttribute("form") PostForm form,
            BindingResult binding,
            @RequestParam String intent,
            Principal actor,
            Model m,
            HttpServletResponse res,
            RedirectAttributes flash) {
        try {
            if (binding.hasErrors())
                throw new Rules.Input("Check the dates and selected images, then try again.");
            publishing.save(id, form, intent, actor.getName());
            flash.addFlashAttribute(
                    "notice", intent.equals("publish") ? "Post published." : "Draft saved.");
            return "redirect:/admin/posts";
        } catch (Rules.Input e) {
            res.setStatus(422);
            m.addAttribute("error", e.getMessage());
            return editor(id, form, m);
        }
    }

    @PostMapping("/posts/{id}/action")
    public String action(
            @PathVariable long id,
            @RequestParam long version,
            @RequestParam String action,
            Principal actor,
            RedirectAttributes flash) {
        try {
            publishing.action(id, version, action, actor.getName());
            flash.addFlashAttribute("notice", "Post updated.");
        } catch (Rules.Input e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/posts";
    }
}
