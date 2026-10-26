import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/** Read-only preflight. Never creates tables, users, JWTs or Telegram messages. */
public class CoreDatabaseCheck {
    private static String env(String key) { return System.getenv().getOrDefault(key, ""); }

    public static void main(String[] args) {
        if (Runtime.version().feature() < 21) fail("JAVA_REQUIRED: Use JDK 21 or newer.");
        List<String> missing = new ArrayList<>();
        for (String key : List.of("CORE_DB_URL", "CORE_DB_USERNAME", "CORE_DB_PASSWORD", "JWT_SECRET", "FACE_SERVICE_TOKEN")) {
            if (env(key).isBlank()) missing.add(key);
        }
        boolean bootstrap = "true".equalsIgnoreCase(env("CORE_BOOTSTRAP_ADMIN"));
        if (bootstrap) {
            for (String key : List.of("CORE_ADMIN_USERNAME", "CORE_ADMIN_PASSWORD", "CORE_ADMIN_EMAIL", "CORE_ADMIN_FULL_NAME")) {
                if (env(key).isBlank()) missing.add(key);
            }
        }
        if (!missing.isEmpty()) fail("CONFIG_MISSING: " + String.join(", ", missing));
        if (!env("CORE_DB_URL").startsWith("jdbc:postgresql://")) fail("CONFIG_INVALID: CORE_DB_URL must be a PostgreSQL JDBC URL.");
        for (String key : List.of("JWT_SECRET", "FACE_SERVICE_TOKEN")) {
            if (env(key).getBytes(StandardCharsets.UTF_8).length < 32) fail("CONFIG_INVALID: " + key + " needs at least 32 UTF-8 bytes.");
        }
        if (env("TELEGRAM_BOT_TOKEN").isBlank() != env("TELEGRAM_CHAT_ID").isBlank()) {
            fail("CONFIG_INVALID: Set both Telegram values, or leave both empty.");
        }
        Properties properties = new Properties();
        properties.setProperty("user", env("CORE_DB_USERNAME"));
        properties.setProperty("password", env("CORE_DB_PASSWORD"));
        properties.setProperty("connectTimeout", "5");
        properties.setProperty("socketTimeout", "5");
        try (Connection db = DriverManager.getConnection(env("CORE_DB_URL"), properties)) {
            db.setReadOnly(true);
            List<String> absent = new ArrayList<>();
            try (PreparedStatement query = db.prepareStatement("SELECT to_regclass(?)")) {
                query.setQueryTimeout(5);
                for (String table : List.of("users", "admin_face_sessions", "auth_telegram_outbox")) {
                    query.setString(1, table);
                    try (ResultSet result = query.executeQuery()) {
                        result.next();
                        if (result.getString(1) == null) absent.add(table);
                    }
                }
            }
            System.out.println("DATABASE_OK: Connected to configured Core database.");
            if (!absent.isEmpty()) {
                if (!"always".equalsIgnoreCase(env("CORE_DB_INIT_MODE"))) fail("SCHEMA_MISSING: " + String.join(", ", absent));
                System.out.println("SCHEMA_PENDING: Core will initialize missing tables on startup: " + String.join(", ", absent));
            } else {
                try (Statement query = db.createStatement()) {
                    query.setQueryTimeout(5);
                    try (ResultSet result = query.executeQuery("SELECT count(*) FROM users WHERE role = 'ADMIN' AND status = 'ACTIVE'")) {
                        result.next();
                        System.out.println("ACTIVE_ADMINS: " + result.getLong(1));
                    }
                }
            }
            System.out.println("CONFIG_OK: Preflight only. No data changed and no messages sent.");
        } catch (SQLException exception) {
            String code = exception.getSQLState();
            System.err.println("DATABASE_UNAVAILABLE: Check server, database and credentials. SQLSTATE="
                    + (code != null && code.matches("[A-Z0-9]{5}") ? code : "UNKNOWN"));
            System.exit(1);
        }
    }

    private static void fail(String message) { System.err.println(message); System.exit(1); }
}
