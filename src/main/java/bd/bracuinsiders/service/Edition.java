package bd.bracuinsiders.service;

import bd.bracuinsiders.model.*;
import bd.bracuinsiders.repository.*;

import jakarta.persistence.criteria.*;

import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class Edition {
    public enum Scope {
        ALL,
        PUBLISHED,
        DRAFT,
        CAMPUS,
        STORIES,
        EVENTS,
        PAST_EVENTS,
        NOTICES
    }

    private static final Sort RECENT = Sort.by(Sort.Direction.DESC, "publishedAt", "id");
    private final PostRepository posts;
    private final JdbcTemplate db;

    public Edition(PostRepository posts, JdbcTemplate db) {
        this.posts = posts;
        this.db = db;
    }

    public Page<Story> browse(
            Scope scope, String category, String search, int page, int size, Long exclude) {
        LocalDate today = LocalDate.now(Rules.DHAKA);
        Specification<Post> spec =
                (root, query, cb) -> {
                    List<Predicate> rules = new ArrayList<>();
                    if (scope != Scope.ALL)
                        rules.add(cb.equal(root.get("published"), scope != Scope.DRAFT));
                    if (category != null && !category.isBlank())
                        rules.add(cb.equal(root.get("category").get("slug"), category));
                    if (exclude != null) rules.add(cb.notEqual(root.get("id"), exclude));
                    if (scope == Scope.CAMPUS || scope == Scope.STORIES) {
                        rules.add(
                                cb.equal(
                                        root.get("category").get("section"),
                                        Section.valueOf(scope.name())));
                    }
                    if (scope == Scope.NOTICES)
                        rules.add(cb.isTrue(root.get("category").get("notice")));
                    if (scope == Scope.EVENTS || scope == Scope.PAST_EVENTS) {
                        rules.add(cb.isNotNull(root.get("eventDate")));
                        Expression<LocalDate> lastDay =
                                cb.coalesce(root.get("eventEnd"), root.get("eventDate"));
                        rules.add(
                                scope == Scope.EVENTS
                                        ? cb.greaterThanOrEqualTo(lastDay, today)
                                        : cb.lessThan(lastDay, today));
                    }
                    if (search != null && !search.isBlank()) {
                        String term = search.strip().toLowerCase(Locale.ROOT);
                        term =
                                term.substring(0, Math.min(160, term.length()))
                                        .replace("!", "!!")
                                        .replace("%", "!%")
                                        .replace("_", "!_");
                        String pattern = "%" + term + "%";
                        rules.add(
                                cb.or(
                                        cb.like(cb.lower(root.get("title")), pattern, '!'),
                                        cb.like(cb.lower(root.get("caption")), pattern, '!'),
                                        cb.like(cb.lower(root.get("body")), pattern, '!')));
                    }
                    return cb.and(rules.toArray(Predicate[]::new));
                };
        Sort order =
                switch (scope) {
                    case EVENTS -> Sort.by("eventDate", "id");
                    case PAST_EVENTS -> Sort.by(Sort.Direction.DESC, "eventDate", "id");
                    case ALL, DRAFT -> Sort.by(Sort.Direction.DESC, "updatedAt", "id");
                    default -> RECENT;
                };
        return posts.findAll(
                        spec,
                        PageRequest.of(Math.clamp(page, 0, 100000), Math.clamp(size, 1, 50), order))
                .map(Story::of);
    }

    public Story lead() {
        return posts.findFirstByPublishedTrueOrderByFeaturedDescPublishedAtDescIdDesc()
                .map(Story::of)
                .orElse(null);
    }

    public List<Story> popular() {
        return posts.popular().stream().map(Story::of).toList();
    }

    public Story published(String slug) {
        return posts.findBySlugAndPublishedTrue(slug).map(Story::of).orElseThrow(Rules::missing);
    }

    public Story editorial(long id) {
        return posts.findById(id).map(Story::of).orElseThrow(Rules::missing);
    }

    @Transactional
    public void recordRead(long id) {
        db.update(
                """
INSERT INTO post_reads(post_id,views) SELECT id,1 FROM posts WHERE id=? AND published
ON CONFLICT(post_id) DO UPDATE SET views=post_reads.views+1
""",
                id);
    }
}
