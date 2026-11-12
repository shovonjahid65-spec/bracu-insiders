package bd.bracuinsiders.web;

import bd.bracuinsiders.model.Media;
import bd.bracuinsiders.service.Story;

import org.springframework.web.multipart.MultipartFile;

import java.time.*;
import java.util.*;

/** Spring MVC binds this form through direct-field access, separate from the persisted entity. */
public class PostForm {
    public long version;
    public String title = "", caption = "", body = "", sourceUrl, videoUrl, eventInfo;
    public Long categoryId, coverId;
    public boolean featured;
    public LocalDate eventDate, eventEnd;
    public LocalDateTime publishedAt;
    public List<Long> selectedMedia = new ArrayList<>();
    public List<MultipartFile> uploads = new ArrayList<>();

    public static PostForm from(Story story) {
        PostForm form = new PostForm();
        form.version = story.version();
        form.title = story.title();
        form.caption = story.caption();
        form.body = story.body();
        form.sourceUrl = story.sourceUrl();
        form.videoUrl = story.videoUrl();
        form.categoryId = story.category().getId();
        form.featured = story.featured();
        form.eventDate = story.eventDate();
        form.eventEnd = story.eventEnd();
        form.eventInfo = story.eventInfo();
        if (story.publishedAt() != null)
            form.publishedAt = LocalDateTime.parse(story.getLocalPublication());
        form.selectedMedia.addAll(story.images().stream().map(Media::getId).toList());
        if (!form.selectedMedia.isEmpty()) form.coverId = form.selectedMedia.get(0);
        return form;
    }
}
