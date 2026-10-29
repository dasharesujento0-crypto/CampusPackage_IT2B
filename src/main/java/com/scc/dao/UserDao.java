package com.scc.dao;

import com.scc.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class UserDao {


    public static final List<String> VALID_ROLES = List.of(
            "SYSTEM_ADMINISTRATOR",
            "PACKAGE_ADMINISTRATOR",
            "PACKAGE_STAFF",
            "DEPARTMENT_OFFICE_REPRESENTATIVE"
    );


    public static final List<String> VALID_STATUSES = List.of(
            "PENDING",
            "APPROVED",
            "REJECTED"
    );

    public boolean registerUser(String username, String password,
                                String fullName, String role) {
        String sql = """
                INSERT INTO Users
                (Username, Password, Full_Name, Role, Account_Status)
                VALUES (?, ?, ?, ?, 'PENDING')
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            pstmt.setString(3, fullName);
            pstmt.setString(4, role.toUpperCase());
            pstmt.executeUpdate();
            System.out.println("-> Successful registration! Waiting for approval.");
            return true;
        } catch (SQLException e) {
            System.err.println("Registration failed: " + e.getMessage());
            return false;
        }
    }

    public User loginUser(String username, String password) {
        String sql = """
                SELECT Users_ID, Username, Password, Full_Name, Role, Account_Status
                FROM Users WHERE Username = ?
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (!rs.next()) {
                    System.out.println("Login failed: Invalid username or password.");
                    return null;
                }

                String storedPassword = rs.getString("Password");
                if (!storedPassword.equals(password)) {
                    System.out.println("Login failed: Invalid username or password.");
                    return null;
                }

                String status = rs.getString("Account_Status");
                if (!"APPROVED".equalsIgnoreCase(status)) {
                    System.out.println("Login denied: Your account is not approved yet.");
                    return null;
                }

                User user = new User(rs.getInt("Users_ID"), rs.getString("Username"),
                        storedPassword, rs.getString("Full_Name"),
                        rs.getString("Role"), status);
                System.out.println("Login successful. Welcome, " + user.fullName() + "!");
                return user;
            }
        } catch (SQLException e) {
            System.err.println("Login error: " + e.getMessage());
            return null;
        }
    }


    public List<User> getAllUsers() {
        List<User> users = new ArrayList<>();
        String sql = """
                SELECT Users_ID, Username, Password, Full_Name, Role, Account_Status
                FROM Users ORDER BY Users_ID
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                users.add(mapUser(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving users: " + e.getMessage());
        }
        return users;
    }

    public List<User> getPendingUsers() {
        List<User> users = new ArrayList<>();
        String sql = """
                SELECT Users_ID, Username, Password, Full_Name, Role, Account_Status
                FROM Users WHERE Account_Status = 'PENDING'
                ORDER BY Users_ID
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                users.add(mapUser(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving pending accounts: " + e.getMessage());
        }
        return users;
    }

    public boolean approveUser(int userId) {
        if (userId <= 0) {
            System.out.println("User ID must be a positive number.");
            return false;
        }

        String sql = """
                UPDATE Users
                SET Account_Status = 'APPROVED'
                WHERE Users_ID = ? AND Account_Status = 'PENDING'
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            int updated = pstmt.executeUpdate();
            if (updated == 1) {
                System.out.println("SUCCESS: User ID " + userId + " has been approved.");
                return true;
            }
            System.out.println("Approval failed: User ID not found or account is no longer pending.");
            return false;
        } catch (SQLException e) {
            System.err.println("Error approving user: " + e.getMessage());
            return false;
        }
    }

    public boolean updateAccountStatus(int userId, String newStatus) {
        if (newStatus == null ||
                (!newStatus.equalsIgnoreCase("APPROVED") &&
                        !newStatus.equalsIgnoreCase("REJECTED"))) {
            System.out.println("Invalid status. Use APPROVED or REJECTED.");
            return false;
        }

        String sql = """
                UPDATE Users SET Account_Status = ?
                WHERE Users_ID = ? AND Account_Status = 'PENDING'
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, newStatus.toUpperCase());
            pstmt.setInt(2, userId);
            int updated = pstmt.executeUpdate();
            if (updated == 1) {
                System.out.println("Account status updated to " + newStatus.toUpperCase() + ".");
                return true;
            }
            System.out.println("Account not found or is no longer pending.");
            return false;
        } catch (SQLException e) {
            System.err.println("Error updating account status: " + e.getMessage());
            return false;
        }
    }

    public boolean updateUserRoleAndStatus(int userId, String newRole, String newStatus) {
        if (userId <= 0) {
            System.out.println("User ID must be a positive number.");
            return false;
        }
        if (newRole == null || newRole.isBlank()
                || newStatus == null || newStatus.isBlank()) {
            System.out.println("Role and status cannot be empty.");
            return false;
        }

        String role = newRole.trim().toUpperCase(Locale.ROOT);
        String status = newStatus.trim().toUpperCase(Locale.ROOT);

        if (!VALID_ROLES.contains(role)) {
            System.out.println("Invalid role: " + role);
            return false;
        }
        if (!VALID_STATUSES.contains(status)) {
            System.out.println("Invalid status: " + status);
            return false;
        }

        String sql = "UPDATE Users SET Role = ?, Account_Status = ? WHERE Users_ID = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, role);
            pstmt.setString(2, status);
            pstmt.setInt(3, userId);
            int updated = pstmt.executeUpdate();
            if (updated == 1) {
                return true;
            }
            System.out.println("Update failed: User ID " + userId + " was not found.");
            return false;
        } catch (SQLException e) {
            System.err.println("Error updating user: " + e.getMessage());
            return false;
        }
    }

    private User mapUser(ResultSet rs) throws SQLException {
        return new User(
                rs.getInt("Users_ID"),
                rs.getString("Username"),
                rs.getString("Password"),
                rs.getString("Full_Name"),
                rs.getString("Role"),
                rs.getString("Account_Status")
        );
    }
}
