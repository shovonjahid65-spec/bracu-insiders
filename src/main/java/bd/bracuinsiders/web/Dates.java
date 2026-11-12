package bd.bracuinsiders.web;

import bd.bracuinsiders.service.Rules;

import org.springframework.stereotype.Component;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Component("dates")
public class Dates {
    private static final DateTimeFormatter STAMP =
            DateTimeFormatter.ofPattern("d MMM yyyy, h:mm a", Locale.ENGLISH).withZone(Rules.DHAKA);

    public String stamp(Instant value) {
        return STAMP.format(value);
    }

    public String input(LocalDateTime value) {
        return value == null ? "" : value.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));
    }
}
