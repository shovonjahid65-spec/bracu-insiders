package bd.bracuinsiders.service;

import bd.bracuinsiders.model.Media;
import bd.bracuinsiders.repository.MediaRepository;

import org.springframework.core.io.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

import javax.imageio.*;
import javax.imageio.stream.ImageInputStream;

@Service
public class Uploads {
    private static final int MAX_BYTES = 4 * 1024 * 1024;
    private static final int MAX_PIXELS = 12_000_000;
    private final MediaRepository media;
    private final JdbcTemplate db;

    public Uploads(MediaRepository media, JdbcTemplate db) {
        this.media = media;
        this.db = db;
    }

    public record FileData(String name, String type, byte[] bytes) {}

    public record Download(String name, String type, Resource resource) {}

    /**
     * Decode and normalize images. A file extension or browser-supplied MIME type is insufficient.
     */
    public FileData inspect(MultipartFile upload, boolean allowPdf) {
        if (upload == null || upload.isEmpty() || upload.getSize() > MAX_BYTES)
            throw new Rules.Input("Choose a file up to 4 MB.");
        String rawName = Objects.toString(upload.getOriginalFilename(), "attachment");
        String name = rawName.replaceAll("[^\\p{L}\\p{N} ._-]", "_");
        name = name.substring(0, Math.min(name.length(), 95)).replaceFirst("\\.[^.]*$", "").strip();
        if (name.isBlank()) name = "attachment";
        try {
            byte[] input = upload.getBytes();
            if (allowPdf
                    && input.length >= 12
                    && new String(input, 0, 5, StandardCharsets.US_ASCII).equals("%PDF-")
                    && new String(
                                    input,
                                    Math.max(0, input.length - 1024),
                                    Math.min(input.length, 1024),
                                    StandardCharsets.ISO_8859_1)
                            .contains("%%EOF"))
                return new FileData(name + ".pdf", "application/pdf", input);
            try (ImageInputStream stream =
                    ImageIO.createImageInputStream(new ByteArrayInputStream(input))) {
                Iterator<ImageReader> candidates = ImageIO.getImageReaders(stream);
                if (!candidates.hasNext())
                    throw new Rules.Input(
                            allowPdf
                                    ? "Choose a JPEG, PNG or PDF."
                                    : "Choose a JPEG or PNG image.");
                ImageReader reader = candidates.next();
                try {
                    String format = reader.getFormatName().toLowerCase(Locale.ROOT);
                    if (!Set.of("jpeg", "png").contains(format))
                        throw new Rules.Input("Choose a JPEG or PNG image.");
                    reader.setInput(stream, true, true);
                    long pixels = (long) reader.getWidth(0) * reader.getHeight(0);
                    if (pixels > MAX_PIXELS)
                        throw new Rules.Input("Resize the image to 12 megapixels or smaller.");
                    BufferedImage decoded = reader.read(0);
                    ByteArrayOutputStream normalized = new ByteArrayOutputStream();
                    ImageIO.write(decoded, format, normalized);
                    if (normalized.size() > MAX_BYTES)
                        throw new Rules.Input("Resize the image and try again (4 MB limit).");
                    return new FileData(
                            name + (format.equals("jpeg") ? ".jpg" : ".png"),
                            "image/" + format,
                            normalized.toByteArray());
                } finally {
                    reader.dispose();
                }
            }
        } catch (IOException | IllegalArgumentException ex) {
            throw new Rules.Input("The file could not be read. Try another export.");
        }
    }

    @Transactional
    public Media add(MultipartFile file, String alt) {
        FileData checked = inspect(file, false);
        String description =
                alt == null || alt.isBlank()
                        ? checked.name().replaceFirst("\\.[^.]+$", "").replace('_', ' ')
                        : Rules.required(alt, 240, "image alternative text");
        Media result =
                media.save(new Media(checked.name(), description, checked.type(), null, null));
        db.update(
                "INSERT INTO media_bytes(media_id,content) VALUES (?,?)",
                result.getId(),
                checked.bytes());
        return result;
    }

    @Transactional(readOnly = true)
    public Download image(long id, boolean admin) {
        Media item = (admin ? media.findById(id) : media.published(id)).orElseThrow(Rules::missing);
        Resource resource =
                item.getBundledPath() != null
                        ? new ClassPathResource("static" + item.getBundledPath())
                        : new ByteArrayResource(
                                db.queryForObject(
                                        "SELECT content FROM media_bytes WHERE media_id=?",
                                        byte[].class,
                                        id));
        return new Download(item.getFilename(), item.getMimeType(), resource);
    }

    @Transactional
    public void remove(long id) {
        Media item = media.forUpdate(id).orElseThrow(Rules::missing);
        if (media.uses(id) != 0)
            throw new Rules.Input("Remove this image from its posts before deleting it.");
        media.delete(item);
    }

    @Transactional
    public void describe(long id, String alt) {
        media.forUpdate(id)
                .orElseThrow(Rules::missing)
                .setAltText(Rules.required(alt, 240, "alternative text"));
    }
}
