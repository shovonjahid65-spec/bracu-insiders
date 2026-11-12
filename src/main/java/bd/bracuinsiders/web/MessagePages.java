package bd.bracuinsiders.web;

import bd.bracuinsiders.service.*;

import jakarta.servlet.http.*;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class MessagePages {
    private final Inbox inbox;
    private final RateGuard guard;

    public MessagePages(Inbox inbox, RateGuard guard) {
        this.inbox = inbox;
        this.guard = guard;
    }

    private boolean allowed(
            String kind, int limit, HttpServletRequest req, HttpServletResponse res, Model model) {
        if (guard.allow(kind, req.getRemoteAddr(), limit)) return true;
        res.setStatus(429);
        res.setHeader("Retry-After", "900");
        model.addAttribute("error", "Too many attempts. Please try again in 15 minutes.");
        return false;
    }

    @GetMapping("/contact")
    public String contact(Model model) {
        model.addAttribute("page", "Contact Us");
        return "contact";
    }

    @PostMapping("/contact")
    public String submit(
            @RequestParam String name,
            @RequestParam(defaultValue = "") String email,
            @RequestParam String subject,
            @RequestParam String message,
            @RequestParam(defaultValue = "") String website,
            @RequestParam(required = false) MultipartFile attachment,
            HttpServletRequest req,
            HttpServletResponse res,
            Model model,
            RedirectAttributes flash) {
        contact(model);
        model.addAttribute("name", name);
        model.addAttribute("email", email);
        model.addAttribute("subject", subject);
        model.addAttribute("message", message);
        if (!allowed("contact", 5, req, res, model)) return "contact";
        if (!website.isBlank()) {
            res.setStatus(422);
            model.addAttribute("error", "Submission could not be accepted.");
            return "contact";
        }
        try {
            flash.addFlashAttribute(
                    "receipt", inbox.receive(name, email, subject, message, attachment));
            return "redirect:/contact";
        } catch (Rules.Input ex) {
            res.setStatus(422);
            model.addAttribute("error", ex.getMessage());
            return "contact";
        }
    }

    @GetMapping("/replies")
    public String lookup(Model model) {
        model.addAttribute("page", "Check a reply");
        return "replies";
    }

    @PostMapping("/replies")
    public String replies(
            @RequestParam String code,
            HttpServletRequest req,
            HttpServletResponse res,
            Model model) {
        lookup(model);
        if (!allowed("receipts", 25, req, res, model)) return "replies";
        try {
            model.addAttribute("replies", inbox.check(code));
            model.addAttribute("matched", true);
        } catch (Rules.Input ex) {
            res.setStatus(422);
            model.addAttribute("error", ex.getMessage());
        }
        return "replies";
    }
}
