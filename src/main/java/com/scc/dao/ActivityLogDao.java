package com.scc.dao;

import com.scc.model.ActivityLog;
import com.scc.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ActivityLogDao {

    public boolean logAction(int userId, String actionType, String description) {
        if (userId <= 0) {
            System.err.println("Activity log skipped: a valid (positive) user ID is required, but received "
                    + userId + ".");
            return false;
        }
        if (actionType == null || actionType.isBlank()) {
            System.err.println("Activity log skipped: the action type is missing.");
            return false;
        }
        if (description == null || description.isBlank()) {
            System.err.println("Activity log skipped: the description is missing for action "
                    + actionType.trim() + ".");
            return false;
        }

        String sql = """
                INSERT INTO Activity_Logs (user_id, action_type, description)
                VALUES (?, ?, ?)
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, userId);
            statement.setString(2, actionType.trim());
            statement.setString(3, description);

            return statement.executeUpdate() == 1;

        } catch (SQLException e) {
            System.err.println("Activity logging failed: " + describe(e));
            return false;
        }
    }

    public void logAction(Connection connection, int userId, String actionType,
                          String description) throws SQLException {
        if (userId <= 0 || actionType == null || actionType.isBlank()
                || description == null || description.isBlank()) {
            throw new SQLException("Audit record is incomplete.");
        }

        String sql = """
                INSERT INTO Activity_Logs (user_id, action_type, description)
                VALUES (?, ?, ?)
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            statement.setString(2, actionType.trim());
            statement.setString(3, description);

            if (statement.executeUpdate() != 1) {
                throw new SQLException("Audit record was not written.");
            }
        }
    }

    public int clearAllLogs(User admin) {
        if (admin == null || !"SYSTEM_ADMINISTRATOR".equalsIgnoreCase(admin.role())) {
            System.err.println("Clearing logs requires the System Administrator.");
            return -1;
        }

        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);

            try (PreparedStatement statement = connection.prepareStatement("DELETE FROM Activity_Logs")) {
                int cleared = statement.executeUpdate();

                logAction(connection, admin.id(), "LOGS_CLEARED",
                        "Admin " + admin.username() + " (ID: " + admin.id() + ") cleared "
                                + cleared + " activity log record(s).");

                connection.commit();
                return cleared;
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }

        } catch (SQLException e) {
            System.err.println("Clearing activity logs failed: " + describe(e));
            return -1;
        }
    }

    public List<ActivityLog> getAllLogs() {
        String sql = """
                SELECT id, user_id, action_type, description, timestamp
                FROM Activity_Logs
                ORDER BY timestamp DESC, id DESC
                """;

        List<ActivityLog> logs = new ArrayList<>();

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {

            while (result.next()) {
                logs.add(mapLog(result));
            }

        } catch (SQLException e) {
            System.err.println("Error retrieving activity logs: " + describe(e));
        }

        return logs;
    }

    public List<ActivityLog> getLogsByUserId(int userId) {
        String sql = """
                SELECT id, user_id, action_type, description, timestamp
                FROM Activity_Logs
                WHERE user_id = ?
                ORDER BY timestamp DESC, id DESC
                """;

        List<ActivityLog> logs = new ArrayList<>();

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, userId);

            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    logs.add(mapLog(result));
                }
            }

        } catch (SQLException e) {
            System.err.println("Error retrieving activity logs: " + describe(e));
        }

        return logs;
    }

    private static String describe(SQLException e) {
        String message = e.getMessage();
        if (message != null && !message.isBlank()) {
            return message.trim();
        }
        return "unknown database error (" + e.getClass().getSimpleName()
                + (e.getSQLState() != null ? ", SQLState " + e.getSQLState() : "") + ")";
    }

    private ActivityLog mapLog(ResultSet result) throws SQLException {
        int userId = result.getInt("user_id");
        Integer userIdOrNull = result.wasNull() ? null : userId;

        return new ActivityLog(
                result.getInt("id"),
                userIdOrNull,
                result.getString("action_type"),
                result.getString("description"),
                result.getString("timestamp")
        );
    }
}