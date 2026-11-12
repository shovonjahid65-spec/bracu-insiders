package bd.bracuinsiders.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

import java.time.Instant;

@Service
public class RateGuard {
    private final JdbcTemplate db;
    private final String secret;

    public RateGuard(JdbcTemplate db) {
        this.db = db;
        db.update(
                "INSERT INTO app_state(key_name,value_text) VALUES ('rate-secret',?) ON CONFLICT DO"
                    + " NOTHING",
                Codes.generate());
        secret =
                db.queryForObject(
                        "SELECT value_text FROM app_state WHERE key_name='rate-secret'",
                        String.class);
    }

    /** Shared, atomic windows across JVMs. Raw network addresses are never persisted. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean allow(String action, String address, int limit) {
        long bucket = Instant.now().getEpochSecond() / 900;
        db.update("DELETE FROM rate_windows WHERE bucket < ?", bucket - 1);
        String key = Codes.digest(secret + "|" + bucket + "|" + action + "|" + address);
        Integer count =
                db.queryForObject(
                        """
INSERT INTO rate_windows(key_hash,bucket,hits) VALUES (?,?,1)
ON CONFLICT(key_hash,bucket) DO UPDATE SET hits=LEAST(rate_windows.hits+1,100000)
RETURNING hits
""",
                        Integer.class,
                        key,
                        bucket);
        return count != null && count <= limit;
    }
}
