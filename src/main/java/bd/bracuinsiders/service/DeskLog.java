package bd.bracuinsiders.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class DeskLog {
    private final JdbcTemplate db;

    public DeskLog(JdbcTemplate db) {
        this.db = db;
    }

    public void record(String actor, String summary) {
        db.update(
                "INSERT INTO desk_activity(actor,summary,occurred_at) VALUES (?,?,now())",
                actor,
                summary);
    }

    public record Entry(String actor, String summary, Instant time) {}

    public List<Entry> recent() {
        return db.query(
                "SELECT actor,summary,occurred_at FROM desk_activity ORDER BY occurred_at DESC,id"
                    + " DESC LIMIT 12",
                (rs, row) ->
                        new Entry(
                                rs.getString(1), rs.getString(2), rs.getTimestamp(3).toInstant()));
    }
}
