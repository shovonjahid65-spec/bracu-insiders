package bd.bracuinsiders.service;

import bd.bracuinsiders.model.*;
import bd.bracuinsiders.repository.*;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@Service
@Transactional
public class Inbox {
    private final LetterRepository letters;
    private final ReplyRepository replies;
    private final JdbcTemplate db;
    private final Uploads uploads;
    private final DeskLog log;

    public Inbox(
            LetterRepository letters,
            ReplyRepository replies,
            JdbcTemplate db,
            Uploads uploads,
            DeskLog log) {
        this.letters = letters;
        this.replies = replies;
        this.db = db;
        this.uploads = uploads;
        this.log = log;
    }

    public String receive(
            String name, String email, String subject, String body, MultipartFile file) {
        String code = Codes.generate();
        Letter letter =
                letters.save(
                        new Letter(
                                Rules.required(name, 80, "your name"),
                                Rules.email(email, true),
                                Rules.required(subject, 140, "a subject"),
                                Rules.required(body, 6000, "a message"),
                                Codes.digest(code)));
        if (file != null && !file.isEmpty()) {
            Uploads.FileData checked = uploads.inspect(file, true);
            db.update(
                    "INSERT INTO message_files(message_id,filename,mime_type,content) VALUES"
                        + " (?,?,?,?)",
                    letter.getId(),
                    checked.name(),
                    checked.type(),
                    checked.bytes());
        }
        log.record("Inbox", "New private message");
        return code;
    }

    public record ThreadView(Letter letter, String filename, List<Reply> replies) {}

    public ThreadView open(long id) {
        Letter letter = letters.findById(id).orElseThrow(Rules::missing);
        letter.markRead();
        String name =
                db
                        .query(
                                "SELECT filename FROM message_files WHERE message_id=?",
                                (rs, n) -> rs.getString(1),
                                id)
                        .stream()
                        .findFirst()
                        .orElse(null);
        return new ThreadView(letter, name, replies.findByMessageIdOrderBySentAtAscIdAsc(id));
    }

    /**
     * The receipt only discloses editorial replies. Sender details, messages and attachments stay
     * in the desk.
     */
    @Transactional(readOnly = true)
    public List<Reply> check(String code) {
        if (code == null || !code.strip().matches("[A-Za-z0-9_-]{32}"))
            throw new Rules.Input("We couldn’t find that receipt code.");
        Letter letter =
                letters.findByReceiptHash(Codes.digest(code.strip()))
                        .orElseThrow(() -> new Rules.Input("We couldn’t find that receipt code."));
        return replies.findByMessageIdOrderBySentAtAscIdAsc(letter.getId());
    }

    public void reply(long id, String body, String actor) {
        Letter letter = letters.findById(id).orElseThrow(Rules::missing);
        replies.save(new Reply(id, Rules.required(body, 6000, "a reply"), actor));
        letter.markRead();
        log.record(actor, "Replied to a private message");
    }

    public void archive(long id, boolean archived, String actor) {
        letters.findById(id).orElseThrow(Rules::missing).setArchived(archived);
        log.record(actor, archived ? "Archived a message" : "Restored a message");
    }

    public void delete(long id, String actor) {
        Letter letter = letters.findById(id).orElseThrow(Rules::missing);
        letters.delete(letter);
        log.record(actor, "Deleted a private message");
    }

    @Transactional(readOnly = true)
    public Uploads.Download attachment(long id) {
        return db
                .query(
                        "SELECT filename,mime_type,content FROM message_files WHERE message_id=?",
                        (rs, n) ->
                                new Uploads.Download(
                                        rs.getString(1),
                                        rs.getString(2),
                                        new ByteArrayResource(rs.getBytes(3))),
                        id)
                .stream()
                .findFirst()
                .orElseThrow(Rules::missing);
    }
}
