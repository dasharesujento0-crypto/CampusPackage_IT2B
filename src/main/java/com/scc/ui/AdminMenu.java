package com.scc.ui;

import com.scc.dao.UserDao;
import com.scc.model.User;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Scanner;

public class AdminMenu {
    private final Scanner sc;
    private final UserDao userDao;
    /** The admin who is actually logged in (passed from CampXpressUI, never guessed). */
    private final User currentAdmin;

    public AdminMenu(Scanner sc, UserDao userDao, User currentAdmin) {
        this.sc = sc;
        this.userDao = userDao;
        this.currentAdmin = Objects.requireNonNull(currentAdmin, "currentAdmin");
    }

    public void run() {
        boolean inMenu = true;
        while (inMenu) {
            System.out.println("\n========================================");
            System.out.println("          ADMIN USER MANAGEMENT");
            System.out.println("========================================");
            System.out.println("1. View All System Users");
            System.out.println("2. View Pending Registrations");
            System.out.println("3. Approve User Account");
            System.out.println("4. Update User (Role & Status)");
            System.out.println("5. Back to Home");
            System.out.print("Choose an option: ");

            String choice = sc.nextLine().trim();
            switch (choice) {
                case "1" -> displayUsers(userDao.getAllUsers(), "ALL SYSTEM USERS");
                case "2" -> displayUsers(userDao.getPendingUsers(), "PENDING REGISTRATIONS");
                case "3" -> approvePendingUser();
                case "4" -> updateUser();
                case "5" -> inMenu = false;
                default -> System.out.println("Invalid option. Please choose 1, 2, 3, 4, or 5.");
            }
        }
    }

    // ================= UPDATE USER (Deliverable 7) =================

    private void updateUser() {
        List<User> users = userDao.getAllUsers();
        displayUsers(users, "ALL SYSTEM USERS");
        if (users.isEmpty()) {
            return;
        }

        System.out.println("Enter 0 at any prompt to cancel.");

        // Step 1: target user ID
        int targetUserId = readIntInput("Target User ID: ");
        if (targetUserId == 0) {
            System.out.println("Update cancelled.");
            return;
        }

        boolean exists = users.stream().anyMatch(u -> u.id() == targetUserId);
        if (!exists) {
            System.out.println("User ID " + targetUserId + " does not exist. No changes made.");
            return;
        }

        // Guardrail: an admin cannot modify the account they are logged in with
        if (targetUserId == currentAdmin.id()) {
            System.out.println("WARNING: Modifying the currently active admin account is restricted.");
            return;
        }

        // Step 2: new role
        String newRole = readValidRole();
        if (newRole == null) {
            System.out.println("Update cancelled.");
            return;
        }

        // Step 3: new status
        String newStatus = readValidStatus();
        if (newStatus == null) {
            System.out.println("Update cancelled.");
            return;
        }

        // Step 4: save
        boolean updated = userDao.updateUserRoleAndStatus(targetUserId, newRole, newStatus);
        if (updated) {
            System.out.println("SUCCESS: User ID " + targetUserId
                    + " is now " + newRole + " with status " + newStatus + ".");
        } else {
            System.out.println("FAILED: User ID " + targetUserId + " was not updated.");
        }
    }

    // ================= INPUT VALIDATION WRAPPERS =================

    /**
     * Prompts until the user types a whole number that is 0 or greater.
     * Returns 0 when the user chooses to cancel (valid IDs are always positive,
     * so 0 can never be confused with a real ID). Also returns 0 if input ends.
     */
    private int readIntInput(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input;
            try {
                input = sc.nextLine().trim();
            } catch (NoSuchElementException e) {
                return 0; // input stream closed: treat as cancel instead of crashing
            }

            try {
                int value = Integer.parseInt(input);
                if (value < 0) {
                    System.out.println("Invalid input. Enter a positive whole number, or 0 to cancel.");
                    continue;
                }
                return value;
            } catch (NumberFormatException e) {
                if (input.matches("[+-]?\\d+")) {
                    // digits only, so the value is too large/small for an int
                    System.out.println("Invalid input. That number is out of range. Enter a valid User ID, or 0 to cancel.");
                } else {
                    System.out.println("Invalid input. Please enter a whole number, or 0 to cancel.");
                }
            }
        }
    }

    /**
     * Asks for a role by number or name. Returns the uppercase role,
     * or null if the user cancelled with 0.
     */
    private String readValidRole() {
        List<String> roles = UserDao.VALID_ROLES;
        return readChoice("Select new role:", "Role", roles);
    }

    /**
     * Asks for an account status by number or name. Returns the uppercase status,
     * or null if the user cancelled with 0.
     */
    private String readValidStatus() {
        List<String> statuses = UserDao.UPDATE_STATUSES;
        return readChoice("Select new account status:", "Status", statuses);
    }

    /** Shared loop for readValidRole/readValidStatus. Returns null on cancel. */
    private String readChoice(String heading, String label, List<String> options) {
        while (true) {
            System.out.println("\n" + heading);
            for (int i = 0; i < options.size(); i++) {
                System.out.println("  [" + (i + 1) + "] " + options.get(i));
            }
            System.out.println("  [0] Cancel");
            System.out.print(label + " (number or name): ");

            String input;
            try {
                input = sc.nextLine().trim();
            } catch (NoSuchElementException e) {
                return null;
            }

            if (input.equals("0")) {
                return null;
            }

            // Accept the menu number
            try {
                int number = Integer.parseInt(input);
                if (number >= 1 && number <= options.size()) {
                    return options.get(number - 1);
                }
            } catch (NumberFormatException ignored) {
                // not a number: try it as a name below
            }

            // Accept the exact name, case-insensitive
            for (String option : options) {
                if (option.equalsIgnoreCase(input)) {
                    return option;
                }
            }

            System.out.println("Invalid " + label.toLowerCase()
                    + ". Choose a number from the list, type a valid name, or 0 to cancel.");
        }
    }

    // ================= EXISTING FEATURES (unchanged behavior) =================

    private void approvePendingUser() {
        List<User> pendingUsers = userDao.getPendingUsers();
        if (pendingUsers.isEmpty()) {
            System.out.println("\nThere are no pending accounts to approve.");
            return;
        }

        displayUsers(pendingUsers, "PENDING REGISTRATIONS");
        System.out.println("Enter 0 to cancel approval.");
        System.out.print("Target User ID: ");
        String input = sc.nextLine().trim();

        if ("0".equals(input)) {
            System.out.println("Approval cancelled.");
            return;
        }

        final int userId;
        try {
            userId = Integer.parseInt(input);
        } catch (NumberFormatException e) {
            System.out.println("Invalid User ID. Please enter a whole number.");
            return;
        }

        boolean existsInPendingList = pendingUsers.stream()
                .anyMatch(user -> user.id() == userId);

        if (!existsInPendingList) {
            System.out.println("That User ID is not in the pending list. No changes made.");
            return;
        }

        userDao.approveUser(userId);
    }

    private void displayUsers(List<User> users, String title) {
        System.out.println("\n--- " + title + " ---");
        // ROLE column is 32 wide so DEPARTMENT_OFFICE_REPRESENTATIVE is not cut off
        String border = "+-------+-----------------+----------------------------------+------------+";
        String rowFormat = "| %-5s | %-15s | %-32s | %-10s |%n";
        System.out.println(border);
        System.out.printf(rowFormat, "ID", "USERNAME", "ROLE", "STATUS");
        System.out.println(border);

        if (users.isEmpty()) {
            System.out.printf(rowFormat, "-", "No users found", "-", "-");
        } else {
            for (User user : users) {
                System.out.printf(rowFormat,
                        user.id(),
                        shorten(user.username(), 15),
                        shorten(user.role(), 32),
                        shorten(user.accountStatus(), 10));
            }
        }
        System.out.println(border);
        System.out.println("Total: " + users.size());
    }

    private String shorten(String value, int maxLength) {
        if (value == null) return "";
        if (value.length() <= maxLength) return value;
        return value.substring(0, maxLength);
    }
}
