package bd.bracuinsiders.config;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.io.Console;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.Arrays;

/** Operator-only account provisioning. Run from a terminal with database access. */
public final class EditorCommand {
    private EditorCommand() {}

    public static void run() throws SQLException {
        Console console = System.console();
        if (console == null)
            throw new IllegalStateException(
                    "Open an interactive terminal to set an editor password.");
        String username = console.readLine("Editor username: ").strip();
        if (!username.matches("[A-Za-z0-9._@+-]{3,80}"))
            throw new IllegalArgumentException("Use 3–80 username characters.");
        char[] secret = console.readPassword("New password (14–72 bytes): ");
        char[] confirmation = console.readPassword("Repeat new password: ");
        try {
            String value = new String(secret);
            if (!Arrays.equals(secret, confirmation)
                    || value.length() < 14
                    || value.getBytes(StandardCharsets.UTF_8).length > 72)
                throw new IllegalArgumentException(
                        "Passwords must match and meet the length requirement.");
            String hash = new BCryptPasswordEncoder(12).encode(value);
            try (Connection db =
                            DriverManager.getConnection(
                                    System.getenv()
                                            .getOrDefault(
                                                    "DB_URL",
                                                    "jdbc:postgresql://localhost:5432/insiders"),
                                    System.getenv().getOrDefault("DB_USER", "insiders"),
                                    System.getenv("DB_PASSWORD"));
                    PreparedStatement stmt =
                            db.prepareStatement(
                                    """
INSERT INTO admin_accounts(username,password_hash,role) VALUES (?,?,'ADMIN')
ON CONFLICT(username) DO UPDATE SET password_hash=excluded.password_hash
""")) {
                stmt.setString(1, username);
                stmt.setString(2, hash);
                stmt.executeUpdate();
            }
            console.printf("Editor account saved.%n");
        } finally {
            Arrays.fill(secret, '\0');
            Arrays.fill(confirmation, '\0');
        }
    }
}
