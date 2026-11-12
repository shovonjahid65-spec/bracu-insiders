package bd.bracuinsiders.model;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "media")
public class Media {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String filename;
    private String altText;
    private String mimeType;
    private String bundledPath;
    private String credit;
    private Instant createdAt = Instant.now();

    protected Media() {}

    public Media(
            String filename, String altText, String mimeType, String bundledPath, String credit) {
        this.filename = filename;
        this.altText = altText;
        this.mimeType = mimeType;
        this.bundledPath = bundledPath;
        this.credit = credit;
    }

    public Long getId() {
        return id;
    }

    public String getFilename() {
        return filename;
    }

    public String getAltText() {
        return altText;
    }

    public String getMimeType() {
        return mimeType;
    }

    public String getBundledPath() {
        return bundledPath;
    }

    public String getCredit() {
        return credit;
    }

    public String getUrl() {
        return "/media/" + id;
    }

    public void setAltText(String altText) {
        this.altText = altText;
    }
}
