package dev.sample.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.sql.DataSource;

public class UserDao {

    private final DataSource ds;

    public UserDao(DataSource ds) {
        this.ds = ds;
    }

    public User findByUsername(String user_id) {
        String sql = "SELECT user_id, password, role FROM app_user WHERE user_id = ?";
        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, user_id);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return new User(
                    rs.getString("user_id"),
                    rs.getString("password"),
                    rs.getString("role")
                );
            }
        } catch (Exception e) {
            throw new RuntimeException("User 조회 실패", e);
        }
    }

    public static class User {
        public final String userId;
        public final String password;
        public final String role;

        public User(String userId, String password, String role) {
            this.userId = userId;
            this.password = password;
            this.role = role;
        }
    }
}