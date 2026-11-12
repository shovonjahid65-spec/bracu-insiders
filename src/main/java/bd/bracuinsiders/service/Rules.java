package bd.bracuinsiders.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.text.Normalizer;
import java.util.*;
import java.util.regex.Pattern;

public final class Rules {
    private Rules() {}

    public static final java.time.ZoneId DHAKA = java.time.ZoneId.of("Asia/Dhaka");
    private static final Pattern EMAIL = Pattern.compile("^[^\\s@<>]+@[^\\s@<>]+\\.[^\\s@<>]+$");

    public static class Input extends RuntimeException {
        public Input(String message) {
            super(message);
        }
    }

    public static ResponseStatusException missing() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND);
    }

    public static String required(String value, int max, String label) {
        if (value == null || value.isBlank()) throw new Input("Enter " + label + ".");
        String clean = value.strip();
        if (clean.length() > max)
            throw new Input(label + " can contain up to " + max + " characters.");
        return clean;
    }

    public static String optional(String value, int max, String label) {
        return value == null || value.isBlank() ? null : required(value, max, label);
    }

    public static String email(String raw, boolean optional) {
        String email =
                optional
                        ? optional(raw, 254, "an email address")
                        : required(raw, 254, "an email address");
        if (email != null && !EMAIL.matcher(email).matches())
            throw new Input("Enter a valid email address.");
        return email;
    }

    public static String https(String raw, int max) {
        String value = optional(raw, max, "a link");
        if (value == null) return null;
        try {
            URI uri = URI.create(value);
            if ("https".equalsIgnoreCase(uri.getScheme())
                    && uri.getHost() != null
                    && uri.getUserInfo() == null) return value;
        } catch (IllegalArgumentException ignored) {
            /* Show an editor-facing error below. */
        }
        throw new Input("Use a full HTTPS link.");
    }

    public static String slug(String title) {
        String normalized =
                Normalizer.normalize(title.toLowerCase(Locale.ROOT), Normalizer.Form.NFKD)
                        .replaceAll("\\p{M}+", "")
                        .replaceAll("[^a-z0-9]+", "-")
                        .replaceAll("(^-)|(-$)", "");
        String stem =
                normalized.isEmpty()
                        ? "story"
                        : normalized.substring(0, Math.min(110, normalized.length()));
        return stem + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    public static void version(long current, long submitted) {
        if (current != submitted)
            throw new Input(
                    "Another editor saved changes. Reopen the latest version before saving.");
    }
}
