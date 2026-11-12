package bd.bracuinsiders.service;

import bd.bracuinsiders.model.*;
import bd.bracuinsiders.repository.*;
import bd.bracuinsiders.web.PostForm;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.*;
import java.util.*;

@Service
@Transactional
public class Publishing {
    private final PostRepository posts;
    private final CategoryRepository categories;
    private final MediaRepository media;
    private final Uploads uploads;
    private final DeskLog log;

    public Publishing(
            PostRepository posts,
            CategoryRepository categories,
            MediaRepository media,
            Uploads uploads,
            DeskLog log) {
        this.posts = posts;
        this.categories = categories;
        this.media = media;
        this.uploads = uploads;
        this.log = log;
    }

    public long save(Long id, PostForm form, String intent, String actor) {
        if (!Set.of("publish", "draft").contains(intent))
            throw new Rules.Input("Choose draft or publish.");
        Post post = id == null ? new Post() : posts.forUpdate(id).orElseThrow(Rules::missing);
        Rules.version(post.getVersion(), form.version);
        String title = Rules.required(form.title, 160, "a title");
        String caption = Rules.required(form.caption, 600, "a caption");
        String body =
                Objects.requireNonNullElse(Rules.optional(form.body, 30000, "article text"), "");
        Category category =
                categories
                        .findById(Objects.requireNonNullElse(form.categoryId, -1L))
                        .orElseThrow(() -> new Rules.Input("Choose a category."));
        if (category.getSection() == Section.EVENTS
                && form.eventDate == null
                && intent.equals("publish"))
            throw new Rules.Input("Add an event date before publishing an event.");
        if (form.eventEnd != null
                && (form.eventDate == null || form.eventEnd.isBefore(form.eventDate)))
            throw new Rules.Input("The last event date must be on or after its first day.");
        Instant date =
                form.publishedAt == null ? null : form.publishedAt.atZone(Rules.DHAKA).toInstant();
        if (date != null && date.isAfter(Instant.now().plusSeconds(60)))
            throw new Rules.Input("Choose a publication date in the present or past.");
        String video =
                VideoLinks.REVEAL.equals(form.videoUrl)
                        ? form.videoUrl
                        : Rules.https(form.videoUrl, 1200);
        String source = Rules.https(form.sourceUrl, 1200);
        LinkedHashSet<Long> selected = new LinkedHashSet<>(form.selectedMedia);
        if (form.coverId != null) selected.add(form.coverId);
        if (selected.contains(null)) throw new Rules.Input("Choose available images.");
        List<MultipartFile> newFiles =
                form.uploads.stream().filter(x -> x != null && !x.isEmpty()).toList();
        if (selected.size() + newFiles.size() > 6)
            throw new Rules.Input("Each post can contain up to six images.");
        List<Media> pictures = new ArrayList<>();
        if (form.coverId != null && selected.remove(form.coverId))
            pictures.add(media.findById(form.coverId).orElseThrow(Rules::missing));
        // Editing text should preserve the order of an existing gallery.
        for (Media existing : post.getImages()) {
            if (selected.remove(existing.getId())) pictures.add(existing);
        }
        for (long asset : selected) pictures.add(media.findById(asset).orElseThrow(Rules::missing));
        for (MultipartFile file : newFiles) pictures.add(uploads.add(file, null));
        if (id == null) post.setSlug(Rules.slug(title));
        post.edit(
                title,
                caption,
                body,
                category,
                source,
                video,
                form.eventDate,
                form.eventEnd,
                Rules.optional(form.eventInfo, 150, "event details"),
                form.featured,
                pictures);
        post.setPublication(intent.equals("publish"), date);
        posts.saveAndFlush(post);
        log.record(actor, (post.isPublished() ? "Published · " : "Saved draft · ") + title);
        return post.getId();
    }

    public void action(long id, long version, String action, String actor) {
        Post post = posts.forUpdate(id).orElseThrow(Rules::missing);
        Rules.version(post.getVersion(), version);
        switch (action) {
            case "publish" -> {
                if (post.getCategory().getSection() == Section.EVENTS
                        && post.getEventDate() == null)
                    throw new Rules.Input("Add an event date using the editor first.");
                post.setPublication(true, null);
            }
            case "unpublish" -> post.setPublication(false, null);
            case "pin" -> post.setFeatured(true);
            case "unpin" -> post.setFeatured(false);
            case "delete" -> posts.delete(post);
            default -> throw Rules.missing();
        }
        log.record(actor, action + " · " + post.getTitle());
    }

    public void saveCategory(Long id, String name, Section section, boolean notice, String actor) {
        String clean = Rules.required(name, 70, "a category name");
        if (categories.existsByNameIgnoreCaseAndIdNot(clean, id == null ? -1 : id))
            throw new Rules.Input("That category name is already in use.");
        Category cat =
                id == null ? new Category() : categories.findById(id).orElseThrow(Rules::missing);
        if (id == null) cat.setSlug(Rules.slug(clean));
        cat.describe(clean, section, notice);
        categories.save(cat);
        log.record(actor, "Updated category · " + clean);
    }

    public void deleteCategory(long id, String actor) {
        Category cat = categories.findById(id).orElseThrow(Rules::missing);
        if (posts.countByCategoryId(id) > 0)
            throw new Rules.Input("Move this category’s posts to another category first.");
        categories.delete(cat);
        log.record(actor, "Removed category · " + cat.getName());
    }
}
