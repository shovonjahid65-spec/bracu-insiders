package bd.bracuinsiders.model;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "messages")
public class Letter {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String senderName;
    private String email;
    private String subject;

    @Column(columnDefinition = "text")
    private String body;

    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.CHAR)
    @Column(length = 64)
    private String receiptHash;

    private Instant createdAt = Instant.now();

    @Column(name = "is_read")
    private boolean read;

    private boolean archived;

    protected Letter() {}

    public Letter(String name, String email, String subject, String body, String receiptHash) {
        this.senderName = name;
        this.email = email;
        this.subject = subject;
        this.body = body;
        this.receiptHash = receiptHash;
    }

    public Long getId() {
        return id;
    }

    public String getSenderName() {
        return senderName;
    }

    public String getEmail() {
        return email;
    }

    public String getSubject() {
        return subject;
    }

    public String getBody() {
        return body;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public boolean isRead() {
        return read;
    }

    public boolean isArchived() {
        return archived;
    }

    public void markRead() {
        read = true;
    }

    public void setArchived(boolean archived) {
        this.archived = archived;
    }
}
