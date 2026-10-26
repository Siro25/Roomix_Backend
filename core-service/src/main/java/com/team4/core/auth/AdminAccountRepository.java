package com.team4.core.auth;

import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class AdminAccountRepository {
    private final JdbcTemplate jdbc;
    private static final String COLUMNS = "id, username, password_hash, email, full_name, phone_number, role, status";
    private static final RowMapper<AdminAccount> MAPPER = (row, index) -> new AdminAccount(
            row.getObject("id", UUID.class), row.getString("username"), row.getString("password_hash"),
            row.getString("email"), row.getString("full_name"), row.getString("phone_number"),
            row.getString("role"), row.getString("status"));

    public AdminAccountRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<AdminAccount> findByUsername(String username) {
        return jdbc.query("SELECT " + COLUMNS + " FROM users WHERE username = ?", MAPPER, username)
                .stream().findFirst();
    }

    public Optional<AdminAccount> findById(UUID id) {
        return jdbc.query("SELECT " + COLUMNS + " FROM users WHERE id = ?", MAPPER, id)
                .stream().findFirst();
    }

    public void insertAdmin(UUID id, String username, String passwordHash, String email, String fullName) {
        jdbc.update("""
                INSERT INTO users (id, username, password_hash, email, full_name, phone_number, role, status, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, '', 'ADMIN', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """, id, username, passwordHash, email, fullName);
    }
}
