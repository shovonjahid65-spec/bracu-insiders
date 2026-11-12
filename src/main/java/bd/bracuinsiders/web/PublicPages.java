package bd.bracuinsiders.web;

import static bd.bracuinsiders.service.Edition.Scope.*;

import bd.bracuinsiders.repository.CategoryRepository;
import bd.bracuinsiders.service.*;

import jakarta.servlet.http.*;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Controller
public class PublicPages {
    private final Edition edition;
    private final CategoryRepository categories;

    public PublicPages(Edition edition, CategoryRepository categories) {
        this.edition = edition;
        this.categories = categories;
    }

    @GetMapping("/")
    public String home(Model model) {
        Story lead = edition.lead();
        model.addAttribute("lead", lead);
        model.addAttribute(
                "latest",
                edition.browse(PUBLISHED, null, null, 0, 4, lead == null ? null : lead.id())
                        .getContent());
        model.addAttribute("events", edition.browse(EVENTS, null, null, 0, 3, null).getContent());
        model.addAttribute("notices", edition.browse(NOTICES, null, null, 0, 3, null).getContent());
        model.addAttribute(
                "students", edition.browse(STORIES, null, null, 0, 3, null).getContent());
        model.addAttribute("popular", edition.popular());
        model.addAttribute("page", "Home");
        return "home";
    }

    @GetMapping({"/posts", "/campus-updates", "/events", "/student-stories"})
    public String browse(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "") String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "false") boolean past,
            HttpServletRequest request,
            Model model) {
        String path = request.getServletPath();
        Edition.Scope scope =
                switch (path) {
                    case "/campus-updates" -> CAMPUS;
                    case "/student-stories" -> STORIES;
                    case "/events" -> past ? PAST_EVENTS : EVENTS;
                    default -> PUBLISHED;
                };
        String title =
                switch (path) {
                    case "/campus-updates" -> "Campus Updates";
                    case "/student-stories" -> "Student Stories";
                    case "/events" -> "Events";
                    default -> "Latest Posts";
                };
        var options =
                categories.findAllByOrderByIdAsc().stream()
                        .filter(
                                c ->
                                        scope != CAMPUS && scope != STORIES
                                                || c.getSection().name().equals(scope.name()))
                        .toList();
        model.addAttribute("page", title);
        model.addAttribute("path", path);
        model.addAttribute("q", q);
        model.addAttribute("category", category);
        model.addAttribute("past", past);
        model.addAttribute("options", options);
        model.addAttribute("results", edition.browse(scope, category, q, page, 9, null));
        return "browse";
    }

    @GetMapping("/posts/{slug}")
    public String article(@PathVariable String slug, HttpSession session, Model model) {
        Story story = edition.published(slug);
        // Count the last 80 distinct articles in a reader's session once each.
        synchronized (session) {
            @SuppressWarnings("unchecked")
            Set<Long> seen = (Set<Long>) session.getAttribute("read-posts");
            if (seen == null) seen = new LinkedHashSet<>();
            if (seen.add(story.id())) {
                edition.recordRead(story.id());
                if (seen.size() > 80) seen.remove(seen.iterator().next());
                session.setAttribute("read-posts", seen);
            }
        }
        model.addAttribute("story", story);
        model.addAttribute(
                "more", edition.browse(PUBLISHED, null, null, 0, 3, story.id()).getContent());
        model.addAttribute("page", story.title());
        return "article";
    }

    @GetMapping("/credits")
    public String credits(Model model) {
        model.addAttribute("page", "Image credits");
        return "credits";
    }
}
