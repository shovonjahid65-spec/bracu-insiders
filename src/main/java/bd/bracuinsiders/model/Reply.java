package bd.bracuinsiders.model;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "replies")
public class Reply {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long messageId;

    @Column(columnDefinition = "text")
    private String body;

    private Instant sentAt = Instant.now();
    private String author;

    protected Reply() {}

    public Reply(Long messageId, String body, String author) {
        this.messageId = messageId;
        this.body = body;
        this.author = author;
    }

    public String getBody() {
        return body;
    }

    public Instant getSentAt() {
        return sentAt;
    }

    public String getAuthor() {
        return author;
    }
}
