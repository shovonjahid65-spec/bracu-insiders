package bd.bracuinsiders.web;

import bd.bracuinsiders.model.*;
import bd.bracuinsiders.repository.*;
import bd.bracuinsiders.service.*;

import org.springframework.data.domain.*;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
@RequestMapping("/admin")
public class AdminSettings {
    private final MediaRepository media;
    private final CategoryRepository categories;
    private final SettingsRepository settings;
    private final Uploads uploads;
    private final Publishing publishing;
    private final DeskLog log;

    public AdminSettings(
            MediaRepository media,
            CategoryRepository categories,
            SettingsRepository settings,
            Uploads uploads,
            Publishing publishing,
            DeskLog log) {
        this.media = media;
        this.categories = categories;
        this.settings = settings;
        this.uploads = uploads;
        this.publishing = publishing;
        this.log = log;
    }

    @GetMapping("/settings")
    public String settings(Model m) {
        m.addAttribute("page", "Website settings");
        return "admin/settings";
    }

    @PostMapping("/settings")
    @Transactional
    public String save(
            @RequestParam String name,
            @RequestParam String tagline,
            @RequestParam String email,
            @RequestParam(defaultValue = "") String facebook,
            @RequestParam long version,
            Principal actor,
            RedirectAttributes flash) {
        try {
            Settings site = settings.findById(1L).orElseThrow();
            Rules.version(site.getVersion(), version);
            site.update(
                    Rules.required(name, 70, "a publication name"),
                    Rules.required(tagline, 140, "a tagline"),
                    Rules.email(email, false),
                    Rules.https(facebook, 600));
            log.record(actor.getName(), "Updated website settings");
            flash.addFlashAttribute("notice", "Settings saved.");
        } catch (Rules.Input e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/settings";
    }

    @GetMapping("/categories")
    public String categories(Model m) {
        m.addAttribute("page", "Categories");
        m.addAttribute("items", categories.findAllByOrderByIdAsc());
        m.addAttribute("sections", Section.values());
        return "admin/categories";
    }

    @PostMapping("/categories")
    public String category(
            @RequestParam(required = false) Long id,
            @RequestParam String name,
            @RequestParam Section section,
            @RequestParam(defaultValue = "false") boolean notice,
            Principal actor,
            RedirectAttributes flash) {
        try {
            publishing.saveCategory(id, name, section, notice, actor.getName());
            flash.addFlashAttribute("notice", "Category saved.");
        } catch (Rules.Input e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/categories";
    }

    @PostMapping("/categories/{id}/delete")
    public String removeCategory(@PathVariable long id, Principal actor, RedirectAttributes flash) {
        try {
            publishing.deleteCategory(id, actor.getName());
            flash.addFlashAttribute("notice", "Category removed.");
        } catch (Rules.Input e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/categories";
    }

    @GetMapping("/media")
    public String media(@RequestParam(defaultValue = "0") int page, Model m) {
        m.addAttribute("page", "Media library");
        m.addAttribute(
                "results",
                media.findAll(
                        PageRequest.of(
                                Math.clamp(page, 0, 100000),
                                18,
                                Sort.by(Sort.Direction.DESC, "id"))));
        return "admin/media";
    }

    @PostMapping("/media")
    public String upload(
            @RequestParam MultipartFile image, @RequestParam String alt, RedirectAttributes flash) {
        try {
            uploads.add(image, alt);
            flash.addFlashAttribute("notice", "Image added to the library.");
        } catch (Rules.Input e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/media";
    }

    @PostMapping("/media/{id}/edit")
    public String alt(@PathVariable long id, @RequestParam String alt, RedirectAttributes flash) {
        try {
            uploads.describe(id, alt);
            flash.addFlashAttribute("notice", "Alternative text updated.");
        } catch (Rules.Input e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/media";
    }

    @PostMapping("/media/{id}/delete")
    public String remove(@PathVariable long id, RedirectAttributes flash) {
        try {
            uploads.remove(id);
            flash.addFlashAttribute("notice", "Image removed.");
        } catch (Rules.Input e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/media";
    }
}
