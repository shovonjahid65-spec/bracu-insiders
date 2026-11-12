package bd.bracuinsiders.model;

import jakarta.persistence.*;

@Entity
@Table(name = "site_settings")
public class Settings {
    @Id private Long id;
    @Version private long version;
    private String siteName;
    private String tagline;
    private String email;
    private String facebookUrl;

    public long getVersion() {
        return version;
    }

    public String getSiteName() {
        return siteName;
    }

    public String getTagline() {
        return tagline;
    }

    public String getEmail() {
        return email;
    }

    public String getFacebookUrl() {
        return facebookUrl;
    }

    public String getSocialLink() {
        return facebookUrl == null
                ? "https://www.facebook.com/search/pages/?q=BRACU%20Insiders"
                : facebookUrl;
    }

    public void update(String name, String tagline, String email, String facebookUrl) {
        this.siteName = name;
        this.tagline = tagline;
        this.email = email;
        this.facebookUrl = facebookUrl;
    }
}
