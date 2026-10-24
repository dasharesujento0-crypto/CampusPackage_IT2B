
package com.scc.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class AuditLogDao {

    public boolean log(
            Integer userId,
            String username,
            String action,
            String details
    ) {
        if (action == null || action.isBlank()) {
            return false;
        }

        String sql = """
                INSERT INTO Activity_Logs
                    (User_ID, Username, Action, Details)
                VALUES (?, ?, ?, ?)
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            if (userId == null) {
                statement.setNull(1, java.sql.Types.INTEGER);
            } else {
                statement.setInt(1, userId);
            }

            statement.setString(2, username);
            statement.setString(3, action);
            statement.setString(4, details);

            return statement.executeUpdate() == 1;

        } catch (SQLException e) {
            System.err.println(
                    "Audit logging failed: " + e.getMessage()
            );
            return false;
        }
    }
}
