package com.scc.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DatabaseInitializer {

    private static final List<String> REQUIRED_ACTIVITY_COLUMNS =
            List.of("id", "user_id", "action_type", "description", "timestamp");

    private static final String CREATE_ACTIVITY_LOGS = """
            CREATE TABLE IF NOT EXISTS Activity_Logs (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER,
                action_type TEXT NOT NULL,
                description TEXT NOT NULL,
                timestamp TEXT NOT NULL
                    DEFAULT (datetime('now', 'localtime')),
                FOREIGN KEY (user_id)
                    REFERENCES Users(Users_ID) ON DELETE CASCADE
            )
            """;

    public void initialize() {
        String[] statements = {
                """
            CREATE TABLE IF NOT EXISTS Users (
                Users_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                Username TEXT NOT NULL UNIQUE,
                Password TEXT NOT NULL,
                Full_Name TEXT NOT NULL,
                Role TEXT NOT NULL,
                Account_Status TEXT NOT NULL DEFAULT 'PENDING'
            )
            """,
                """
            CREATE TABLE IF NOT EXISTS Department_Offices (
                Department_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                Department_Name TEXT NOT NULL,
                Department_Type TEXT NOT NULL,
                Contact_Number TEXT NOT NULL,
                Department_Status TEXT NOT NULL DEFAULT 'ACTIVE'
            )
            """,
                """
            CREATE TABLE IF NOT EXISTS Recipients (
                Recipient_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                Department_ID INTEGER NOT NULL,
                Recipient_Name TEXT NOT NULL,
                Recipient_Type TEXT NOT NULL,
                Identification_Number TEXT NOT NULL,
                Contact_Number TEXT,
                Recipient_Status TEXT NOT NULL DEFAULT 'ACTIVE',
                FOREIGN KEY (Department_ID)
                    REFERENCES Department_Offices(Department_ID)
            )
            """,
                """
            CREATE TABLE IF NOT EXISTS Packages (
                Package_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                Recipient_ID INTEGER NOT NULL,
                Tracking_Number TEXT NOT NULL UNIQUE,
                Sender_Name TEXT NOT NULL,
                Package_Description TEXT NOT NULL,
                Date_Received TEXT NOT NULL,
                Package_Status TEXT NOT NULL DEFAULT 'RECEIVED',
                Claimant_Name TEXT,
                Claimant_Identification TEXT,
                Date_Claimed TEXT,
                Date_Returned TEXT,
                FOREIGN KEY (Recipient_ID)
                    REFERENCES Recipients(Recipient_ID)
            )
            """
        };

        try (Connection connection = DatabaseConnection.getConnection();
             Statement statement = connection.createStatement()) {

            for (String sql : statements) {
                statement.execute(sql);
            }

            setUpActivityLogs(connection);

            System.out.println(
                    "Database initialization completed successfully."
            );

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Failed to initialize the database: "
                            + e.getMessage(),
                    e
            );
        }
    }


    private void setUpActivityLogs(Connection connection) throws SQLException {
        List<String> columns = tableColumns(connection, "Activity_Logs");

        if (columns.isEmpty()) {
            try (Statement statement = connection.createStatement()) {
                statement.execute(CREATE_ACTIVITY_LOGS);
            }
            return;
        }

        if (columns.contains("action_type")) {
            useCanonicalTableName(connection);
            validateActivityLogsSchema(connection);
            return;
        }

        throw new SQLException(
                "The existing Activity_Logs table has an unrecognized layout " + columns
                        + ". Nothing was changed."
        );
    }

    private void validateActivityLogsSchema(Connection connection) throws SQLException {
        List<String> columns = tableColumns(connection, "Activity_Logs");
        List<String> missing = REQUIRED_ACTIVITY_COLUMNS.stream()
                .filter(column -> !columns.contains(column))
                .toList();

        if (!missing.isEmpty()) {
            throw new SQLException(
                    "The existing Activity_Logs table is missing required column(s) " + missing
                            + ". Nothing was changed. Correct the table manually."
            );
        }

        if (!hasUsersCascadeForeignKey(connection)) {
            throw new SQLException(
                    "Activity_Logs.user_id must reference Users(Users_ID) with ON DELETE CASCADE. "
                            + "Nothing was changed. Correct the table manually."
            );
        }
    }

    private boolean hasUsersCascadeForeignKey(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("PRAGMA foreign_key_list(Activity_Logs)")) {
            while (result.next()) {
                if ("Users".equalsIgnoreCase(result.getString("table"))
                        && "user_id".equalsIgnoreCase(result.getString("from"))
                        && "Users_ID".equalsIgnoreCase(result.getString("to"))
                        && "CASCADE".equalsIgnoreCase(result.getString("on_delete"))) {
                    return true;
                }
            }
        }
        return false;
    }

    private void useCanonicalTableName(Connection connection) throws SQLException {
        String currentName = null;
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT name FROM sqlite_master WHERE type = 'table' AND LOWER(name) = LOWER(?)")) {
            statement.setString(1, "Activity_Logs");
            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    currentName = result.getString("name");
                }
            }
        }

        if (currentName == null || currentName.equals("Activity_Logs")) {
            return;
        }

        boolean originalAutoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try (Statement statement = connection.createStatement()) {
            statement.execute("ALTER TABLE \"" + currentName + "\" RENAME TO Activity_Logs_rename_tmp");
            statement.execute("ALTER TABLE Activity_Logs_rename_tmp RENAME TO Activity_Logs");
            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            throw new SQLException("Could not rename the existing audit table to Activity_Logs "
                    + "(nothing was changed): " + e.getMessage(), e);
        } finally {
            connection.setAutoCommit(originalAutoCommit);
        }
    }

    private List<String> tableColumns(Connection connection, String tableName)
            throws SQLException {
        List<String> columns = new ArrayList<>();
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("PRAGMA table_info(" + tableName + ")")) {
            while (result.next()) {
                columns.add(result.getString("name").toLowerCase(Locale.ROOT));
            }
        }
        return columns;
    }
}