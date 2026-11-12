package bd.bracuinsiders.web;

import bd.bracuinsiders.service.*;

import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;

@RestController
public class FilesController {
    private final Uploads uploads;
    private final Inbox inbox;

    public FilesController(Uploads uploads, Inbox inbox) {
        this.uploads = uploads;
        this.inbox = inbox;
    }

    private ResponseEntity<Resource> file(Uploads.Download file, boolean attachment) {
        ContentDisposition disposition =
                (attachment ? ContentDisposition.attachment() : ContentDisposition.inline())
                        .filename(file.name(), StandardCharsets.UTF_8)
                        .build();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.type()))
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(file.resource());
    }

    @GetMapping("/media/{id}")
    public ResponseEntity<Resource> image(@PathVariable long id) {
        return file(uploads.image(id, false), false);
    }

    @GetMapping("/admin/media/{id}/file")
    public ResponseEntity<Resource> preview(@PathVariable long id) {
        return file(uploads.image(id, true), false);
    }

    @GetMapping("/admin/messages/{id}/attachment")
    public ResponseEntity<Resource> attachment(@PathVariable long id) {
        return file(inbox.attachment(id), true);
    }
}
