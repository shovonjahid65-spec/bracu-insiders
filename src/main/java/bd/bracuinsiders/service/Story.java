package bd.bracuinsiders.service;

import static bd.bracuinsiders.service.Rules.DHAKA;

import bd.bracuinsiders.model.*;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** A detached read model. The public JSON API uses a separate, explicitly selected projection. */
public record Story(
        long id,
        long version,
        String slug,
        String title,
        String caption,
        String body,
        Category category,
        boolean published,
        boolean featured,
        Instant publishedAt,
        LocalDate eventDate,
        LocalDate eventEnd,
        String eventInfo,
        String sourceUrl,
        String videoUrl,
        List<Media> images) {
    public static Story of(Post post) {
        Category cat = post.getCategory();
        cat.getName(); // Initialize the category inside the read transaction.
        List<Media> media = List.copyOf(post.getImages());
        media.forEach(Media::getAltText);
        return new Story(
                post.getId(),
                post.getVersion(),
                post.getSlug(),
                post.getTitle(),
                post.getCaption(),
                post.getBody(),
                cat,
                post.isPublished(),
                post.isFeatured(),
                post.getPublishedAt(),
                post.getEventDate(),
                post.getEventEnd(),
                post.getEventInfo(),
                post.getSourceUrl(),
                post.getVideoUrl(),
                media);
    }

    public String getUrl() {
        return "/posts/" + slug;
    }

    public Media getCover() {
        return images.isEmpty() ? null : images.get(0);
    }

    public int getMinutes() {
        return Math.max(1, (caption + " " + body).strip().split("\\s+").length / 200 + 1);
    }

    public List<String> getParagraphs() {
        return List.of(body.split("\\R\\s*\\R"));
    }

    public String getEmbed() {
        return VideoLinks.embed(videoUrl);
    }

    public boolean isLocalVideo() {
        return VideoLinks.REVEAL.equals(videoUrl);
    }

    public String getDateLabel() {
        return publishedAt == null
                ? "Draft"
                : DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)
                        .withZone(DHAKA)
                        .format(publishedAt);
    }

    public String getLocalPublication() {
        return publishedAt == null
                ? ""
                : DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")
                        .withZone(DHAKA)
                        .format(publishedAt);
    }
}
