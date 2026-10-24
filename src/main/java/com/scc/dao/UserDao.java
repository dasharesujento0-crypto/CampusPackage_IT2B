
package com.scc.dao;

import com.scc.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class UserDao {

    private static final Set<String> PUBLIC_ROLES = Set.of(
            "PACKAGE_STAFF",
            "DEPARTMENT_OFFICE_REPRESENTATIVE"
    );

    public boolean registerUser(
            String username,
            String password,
            String fullName,
            String role
    ) {
        if (username == null || username.isBlank()
                || password == null || password.isBlank()
                || fullName == null || fullName.isBlank()
                || role == null) {
            System.out.println("Registration failed: Fields cannot be empty.");
            return false;
        }

        String normalizedRole = role.trim().toUpperCase(Locale.ROOT);

        if (!PUBLIC_ROLES.contains(normalizedRole)) {
            System.out.println(
                    "Registration failed: Invalid public registration role."
            );
            return false;
        }

        String sql = """
                INSERT INTO Users
                    (Username, Password, Full_Name, Role, Account_Status)
                VALUES (?, ?, ?, ?, 'PENDING')
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, username.trim());
            statement.setString(2, password);
            statement.setString(3, fullName.trim());
            statement.setString(4, normalizedRole);

            if (statement.executeUpdate() == 1) {
                System.out.println(
                        "Registration successful. "
                                + "Please wait for account approval."
                );
                return true;
            }

        } catch (SQLException e) {
            if (e.getMessage() != null
                    && e.getMessage().toLowerCase(Locale.ROOT)
                    .contains("unique")) {
                System.out.println(
                        "Registration failed: Username already exists."
                );
            } else {
                System.out.println(
                        "Registration failed: " + e.getMessage()
                );
            }
        }

        return false;
    }

    public User loginUser(String username, String password) {
        if (username == null || username.isBlank()
                || password == null || password.isEmpty()) {
            System.out.println(
                    "Login failed: Username and password are required."
            );
            return null;
        }

        String sql = """
                SELECT Users_ID, Username, Password, Full_Name,
                       Role, Account_Status
                FROM Users
                WHERE Username = ?
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, username.trim());

            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()
                        || !password.equals(result.getString("Password"))) {
                    System.out.println(
                            "Login failed: Invalid username or password."
                    );
                    return null;
                }

                String status = result.getString("Account_Status");

                if (!"APPROVED".equalsIgnoreCase(status)) {
                    System.out.println(
                            "Login denied. Account status: " + status
                    );
                    return null;
                }

                User user = new User(
                        result.getInt("Users_ID"),
                        result.getString("Username"),
                        result.getString("Password"),
                        result.getString("Full_Name"),
                        result.getString("Role"),
                        status
                );

                System.out.println("Login successful. Welcome!");
                return user;
            }

        } catch (SQLException e) {
            System.out.println("Login failed: " + e.getMessage());
            return null;
        }
    }

    public List<User> getPendingUsers() {
        List<User> users = new ArrayList<>();

        String sql = """
                SELECT Users_ID, Username, Password, Full_Name,
                       Role, Account_Status
                FROM Users
                WHERE Account_Status = 'PENDING'
                ORDER BY Users_ID
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {

            while (result.next()) {
                users.add(new User(
                        result.getInt("Users_ID"),
                        result.getString("Username"),
                        result.getString("Password"),
                        result.getString("Full_Name"),
                        result.getString("Role"),
                        result.getString("Account_Status")
                ));
            }

        } catch (SQLException e) {
            System.out.println(
                    "Unable to load pending users: " + e.getMessage()
            );
        }

        return users;
    }

    public boolean updateAccountStatus(int userId, String newStatus) {
        if (userId <= 0 || newStatus == null) {
            System.out.println("Invalid user ID or account status.");
            return false;
        }

        String status = newStatus.trim().toUpperCase(Locale.ROOT);

        if (!Set.of("APPROVED", "REJECTED").contains(status)) {
            System.out.println("Invalid account status.");
            return false;
        }

        String sql = """
                UPDATE Users
                SET Account_Status = ?
                WHERE Users_ID = ?
                  AND Account_Status = 'PENDING'
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, status);
            statement.setInt(2, userId);

            if (statement.executeUpdate() == 1) {
                System.out.println(
                        "Account " + status.toLowerCase(Locale.ROOT)
                                + " successfully."
                );
                return true;
            }

            System.out.println(
                    "No pending account found for User ID " + userId + "."
            );

        } catch (SQLException e) {
            System.out.println(
                    "Unable to update account status: " + e.getMessage()
            );
        }

        return false;
    }
}
