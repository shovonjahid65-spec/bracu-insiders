package bd.bracuinsiders.service;

import java.net.URI;
import java.util.Set;
import java.util.regex.*;

public final class VideoLinks {
    private VideoLinks() {}

    public static final String REVEAL = "/video/insiders-logo-reveal.mp4";
    private static final Pattern YOUTUBE =
            Pattern.compile("(?:/shorts/|/embed/|[?&]v=)([A-Za-z0-9_-]{11})(?:[/?&#]|$)");

    public static String embed(String url) {
        if (url == null || !url.startsWith("https://")) return null;
        URI uri = URI.create(url);
        String host = uri.getHost();
        if (host == null) return null;
        if (host.equalsIgnoreCase("youtu.be") && uri.getPath().matches("/[A-Za-z0-9_-]{11}/?"))
            return "https://www.youtube-nocookie.com/embed/" + uri.getPath().substring(1, 12);
        if (Set.of("youtube.com", "www.youtube.com", "m.youtube.com", "www.youtube-nocookie.com")
                .contains(host.toLowerCase())) {
            Matcher match = YOUTUBE.matcher(url);
            if (match.find()) return "https://www.youtube-nocookie.com/embed/" + match.group(1);
        }
        if (Set.of("vimeo.com", "www.vimeo.com", "player.vimeo.com").contains(host.toLowerCase())
                && uri.getPath().matches("/(?:video/)?[0-9]+/?"))
            return "https://player.vimeo.com/video/" + uri.getPath().replaceAll("[^0-9]", "");
        return null;
    }
}
