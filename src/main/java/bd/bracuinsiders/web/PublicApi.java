package bd.bracuinsiders.web;

import bd.bracuinsiders.service.*;

import org.springframework.boot.availability.*;
import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
public class PublicApi {
    private final Edition edition;
    private final JdbcTemplate db;
    private final ApplicationAvailability availability;

    public PublicApi(Edition edition, JdbcTemplate db, ApplicationAvailability availability) {
        this.edition = edition;
        this.db = db;
        this.availability = availability;
    }

    public record Summary(
            String title,
            String url,
            String caption,
            String category,
            String publishedAt,
            String cover) {
        static Summary from(Story s) {
            return new Summary(
                    s.title(),
                    s.getUrl(),
                    s.caption(),
                    s.category().getName(),
                    s.publishedAt().toString(),
                    s.getCover() == null ? null : s.getCover().getUrl());
        }
    }

    public record Article(
            Summary post, String body, List<String> images, String source, String video) {}

    public record Listing(List<Summary> items, int page, int totalPages, long totalItems) {}

    @GetMapping("/api/posts")
    public Listing list(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "") String category,
            @RequestParam(defaultValue = "0") int page) {
        var matches = edition.browse(Edition.Scope.PUBLISHED, category, q, page, 12, null);
        return new Listing(
                matches.map(Summary::from).getContent(),
                matches.getNumber(),
                matches.getTotalPages(),
                matches.getTotalElements());
    }

    @GetMapping("/api/posts/{slug}")
    public Article article(@PathVariable String slug) {
        Story s = edition.published(slug);
        return new Article(
                Summary.from(s),
                s.body(),
                s.images().stream().map(m -> m.getUrl()).toList(),
                s.sourceUrl(),
                s.videoUrl());
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        if (availability.getReadinessState() == ReadinessState.ACCEPTING_TRAFFIC) {
            try {
                if (Integer.valueOf(1).equals(db.queryForObject("SELECT 1", Integer.class)))
                    return ResponseEntity.ok(Map.of("status", "up"));
            } catch (DataAccessException ignored) {
                /* Keep infrastructure details out of the public endpoint. */
            }
        }
        return ResponseEntity.status(503).body(Map.of("status", "unavailable"));
    }
}
