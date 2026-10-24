
package com.scc.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseInitializer {

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
            """,
                """
            CREATE TABLE IF NOT EXISTS Activity_Logs (
                Log_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                User_ID INTEGER,
                Username TEXT,
                Action TEXT NOT NULL,
                Details TEXT,
                Timestamp TEXT NOT NULL
                    DEFAULT (datetime('now', 'localtime')),
                FOREIGN KEY (User_ID) REFERENCES Users(Users_ID)
            )
            """
        };

        try (Connection connection = DatabaseConnection.getConnection();
             Statement statement = connection.createStatement()) {

            for (String sql : statements) {
                statement.execute(sql);
            }

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
}
