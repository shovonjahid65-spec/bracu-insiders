package bd.bracuinsiders.web;

import bd.bracuinsiders.repository.LetterRepository;
import bd.bracuinsiders.service.*;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
@RequestMapping("/admin/messages")
public class AdminInbox {
    private final Inbox inbox;
    private final LetterRepository letters;

    public AdminInbox(Inbox inbox, LetterRepository letters) {
        this.inbox = inbox;
        this.letters = letters;
    }

    @GetMapping
    public String list(
            @RequestParam(defaultValue = "false") boolean archived,
            @RequestParam(defaultValue = "0") int page,
            Model m) {
        m.addAttribute("page", "Messages");
        m.addAttribute("archived", archived);
        m.addAttribute(
                "results",
                letters.findByArchivedOrderByCreatedAtDescIdDesc(
                        archived, PageRequest.of(Math.clamp(page, 0, 100000), 20)));
        return "admin/messages";
    }

    @GetMapping("/{id}")
    public String thread(@PathVariable long id, Model m) {
        m.addAttribute("page", "Private message");
        m.addAttribute("thread", inbox.open(id));
        return "admin/thread";
    }

    @PostMapping("/{id}/reply")
    public String reply(
            @PathVariable long id,
            @RequestParam String body,
            Principal actor,
            RedirectAttributes flash) {
        try {
            inbox.reply(id, body, actor.getName());
            flash.addFlashAttribute(
                    "notice", "Reply is available to the sender using their receipt code.");
        } catch (Rules.Input e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/messages/" + id;
    }

    @PostMapping("/{id}/action")
    public String action(
            @PathVariable long id,
            @RequestParam String action,
            Principal actor,
            RedirectAttributes flash) {
        switch (action) {
            case "archive", "restore" ->
                    inbox.archive(id, action.equals("archive"), actor.getName());
            case "delete" -> inbox.delete(id, actor.getName());
            default -> throw Rules.missing();
        }
        flash.addFlashAttribute("notice", "Message updated.");
        return "redirect:/admin/messages";
    }
}
