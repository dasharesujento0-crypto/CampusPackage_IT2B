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

    /**
     * Statuses the administrator may assign through the Deliverable 7
     * "Update User (Role & Status)" workflow. REJECTED stays in VALID_STATUSES
     * because the older account-approval code (updateAccountStatus) still uses it.
     */
    public static final List<String> UPDATE_STATUSES = List.of(
            "APPROVED",
            "PENDING"
    );

    /** The only roles a person may pick for themselves during public registration. */
    public static final List<String> PUBLIC_REGISTRATION_ROLES = List.of(
            "PACKAGE_STAFF",
            "DEPARTMENT_OFFICE_REPRESENTATIVE"
    );

    public boolean registerUser(String username, String password,
                                String fullName, String role) {
        // Public registration can never create an administrator account.
        String normalizedRole = role == null ? "" : role.trim().toUpperCase(Locale.ROOT);
        if (!PUBLIC_REGISTRATION_ROLES.contains(normalizedRole)) {
            System.out.println("Registration failed: that role cannot be chosen during registration.");
            return false;
        }

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
            pstmt.setString(4, normalizedRole);
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
        if (!UPDATE_STATUSES.contains(status)) {
            System.out.println("Invalid status: " + status + ". Use APPROVED or PENDING.");
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

    /**
     * True if any SYSTEM_ADMINISTRATOR account exists (whatever its status).
     * Fails closed: on a database error it reports true so that first-run
     * setup is never offered when the answer is uncertain.
     */
    public boolean hasSystemAdministrator() {
        String sql = "SELECT 1 FROM Users WHERE UPPER(Role) = 'SYSTEM_ADMINISTRATOR' LIMIT 1";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            return rs.next();
        } catch (SQLException e) {
            System.err.println("Error checking for an administrator: " + e.getMessage());
            return true;
        }
    }

    /**
     * Creates the very first SYSTEM_ADMINISTRATOR (APPROVED). The check and the
     * insert are ONE statement, so it can never add a second administrator or
     * touch an existing account: if an administrator already exists, nothing is
     * inserted and false is returned.
     */
    public boolean createInitialAdministrator(String username, String password,
                                              String fullName) {
        if (username == null || username.isBlank()
                || password == null || password.isEmpty()
                || fullName == null || fullName.isBlank()) {
            System.out.println("Username, password, and full name cannot be empty.");
            return false;
        }

        String sql = """
                INSERT INTO Users
                (Username, Password, Full_Name, Role, Account_Status)
                SELECT ?, ?, ?, 'SYSTEM_ADMINISTRATOR', 'APPROVED'
                WHERE NOT EXISTS (
                    SELECT 1 FROM Users WHERE UPPER(Role) = 'SYSTEM_ADMINISTRATOR'
                )
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username.trim());
            pstmt.setString(2, password);
            pstmt.setString(3, fullName.trim());
            return pstmt.executeUpdate() == 1;
        } catch (SQLException e) {
            System.err.println("Administrator setup failed: " + e.getMessage());
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