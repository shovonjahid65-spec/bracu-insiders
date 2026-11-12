package bd.bracuinsiders.web;

import bd.bracuinsiders.model.Settings;
import bd.bracuinsiders.repository.SettingsRepository;
import bd.bracuinsiders.service.Rules;

import org.springframework.web.bind.annotation.*;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@ControllerAdvice(
        assignableTypes = {
            PublicPages.class,
            MessagePages.class,
            AdminDesk.class,
            AdminInbox.class,
            AdminSettings.class
        })
public class PageContext {
    private final SettingsRepository settings;

    public PageContext(SettingsRepository settings) {
        this.settings = settings;
    }

    @ModelAttribute("site")
    public Settings site() {
        return settings.findById(1L).orElseThrow();
    }

    @ModelAttribute("issueDate")
    public String date() {
        return LocalDate.now(Rules.DHAKA)
                .format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.ENGLISH));
    }

    @ModelAttribute("year")
    public int year() {
        return LocalDate.now(Rules.DHAKA).getYear();
    }
}
