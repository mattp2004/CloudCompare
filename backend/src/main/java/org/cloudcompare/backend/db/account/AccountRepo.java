package org.cloudcompare.backend.db.account;

import org.cloudcompare.backend.db.util.Rank;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

@Component
public class AccountRepo {
    private DataSource dataSource;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public AccountRepo(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void createAccount(UUID id, String username, String email, String raw){
        String sql = """
                INSERT INTO users(user_id, username, email, password_hash)
                VALUES(?,?,?,?)
                """;

        String hashedPassword = encoder.encode(raw);
        try (Connection connection = dataSource.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setObject(1, id);
            preparedStatement.setString(2, username);
            preparedStatement.setString(3, email);
            preparedStatement.setString(4, hashedPassword);

            preparedStatement.executeUpdate();
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }

    }


    //Retrieve based on username
    public User retrieveByUsername(String username){
        String sql = """
            SELECT user_id, username, email, password_hash,rank
            FROM users
            WHERE username = ?
            """;

        try (Connection connection = dataSource.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setString(1, username);

            try (ResultSet rs = preparedStatement.executeQuery()) {
                if (rs.next()) return toUser(rs);
                return null;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public User retrieveByEmail(String email){
        String sql = """
            SELECT user_id, username, email, password_hash,rank
            FROM users
            WHERE email = ?
            """;

        try (Connection connection = dataSource.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setString(1, email);

            try (ResultSet rs = preparedStatement.executeQuery()) {
                if (rs.next()) return toUser(rs);
                return null;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public User toUser(ResultSet set) throws SQLException {
        return new User(
                UUID.fromString(set.getString("user_id")),
                set.getString("username"),
                set.getString("email"),
                set.getString("password_hash"),
                Rank.valueOf(set.getString("rank"))
        );
    }
}
