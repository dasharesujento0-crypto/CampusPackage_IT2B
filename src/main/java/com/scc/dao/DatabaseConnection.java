package com.scc.dao;

import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseConnection {


    private static final String DB_FILE =
            System.getProperty("campxpress.db", "CampusPackage.db");

    private static final String DB_URL =
            "jdbc:sqlite:" + Paths.get(DB_FILE).toAbsolutePath();


    public static String getDatabasePath() {
        return Paths.get(DB_FILE).toAbsolutePath().toString();
    }

    public static Connection getConnection() throws SQLException {
        Connection connection = DriverManager.getConnection(DB_URL);

        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
            return connection;
        } catch (SQLException e) {
            try {
                connection.close();
            } catch (SQLException closeException) {
                e.addSuppressed(closeException);
            }
            throw e;
        }
    }
}