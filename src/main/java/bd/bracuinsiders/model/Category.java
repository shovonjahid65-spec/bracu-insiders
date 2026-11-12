package bd.bracuinsiders.model;

import jakarta.persistence.*;

@Entity
@Table(name = "categories")
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String slug;

    @Enumerated(EnumType.STRING)
    private Section section;

    @Column(name = "is_notice")
    private boolean notice;

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getSlug() {
        return slug;
    }

    public Section getSection() {
        return section;
    }

    public boolean isNotice() {
        return notice;
    }

    public void describe(String name, Section section, boolean notice) {
        this.name = name;
        this.section = section;
        this.notice = notice;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }
}
