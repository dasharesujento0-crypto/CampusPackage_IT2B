package com.scc.ui;

import com.scc.dao.ActivityLogDao;
import com.scc.dao.UserDao;
import com.scc.model.ActivityLog;
import com.scc.model.User;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Scanner;

public class AdminMenu {
    private final Scanner sc;
    private final UserDao userDao;
    private final ActivityLogDao activityLogDao;
    private final User currentAdmin;

    public AdminMenu(Scanner sc, UserDao userDao, ActivityLogDao activityLogDao,
                     User currentAdmin) {
        this.sc = sc;
        this.userDao = userDao;
        this.activityLogDao = Objects.requireNonNull(activityLogDao, "activityLogDao");
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
            System.out.println("5. View System Audit Logs");
            System.out.println("6. Back to Home");
            System.out.print("Choose an option: ");

            String choice = sc.nextLine().trim();
            switch (choice) {
                case "1" -> displayUsers(userDao.getAllUsers(), "ALL SYSTEM USERS");
                case "2" -> displayUsers(userDao.getPendingUsers(), "PENDING REGISTRATIONS");
                case "3" -> approvePendingUser();
                case "4" -> updateUser();
                case "5" -> auditLogMenu();
                case "6" -> inMenu = false;
                default -> System.out.println("Invalid option. Please choose 1, 2, 3, 4, 5, or 6.");
            }
        }
    }

    private void updateUser() {
        List<User> users = userDao.getAllUsers();
        displayUsers(users, "ALL SYSTEM USERS");
        if (users.isEmpty()) {
            return;
        }

        System.out.println("Enter 0 at any prompt to cancel.");

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

        if (targetUserId == currentAdmin.id()) {
            System.out.println("WARNING: Modifying the currently active admin account is restricted.");
            return;
        }

        String newRole = readValidRole();
        if (newRole == null) {
            System.out.println("Update cancelled.");
            return;
        }

        String newStatus = readValidStatus();
        if (newStatus == null) {
            System.out.println("Update cancelled.");
            return;
        }

        boolean updated = userDao.updateUserRoleAndStatus(targetUserId, newRole, newStatus, currentAdmin);
        if (updated) {
            System.out.println("SUCCESS: User ID " + targetUserId
                    + " is now " + newRole + " with status " + newStatus + ".");
        } else {
            System.out.println("FAILED: User ID " + targetUserId + " was not updated.");
        }
    }

    private static final String LOG_ROW_FORMAT = "%-5s | %-8s | %-18s | %-35s | %-20s%n";
    private static final int ACTION_WIDTH = 18;
    private static final int DESCRIPTION_WIDTH = 35;

    private void auditLogMenu() {
        boolean inMenu = true;
        while (inMenu) {
            System.out.println("\n========================================");
            System.out.println("          SYSTEM AUDIT LOGS");
            System.out.println("========================================");
            System.out.println("1. View All System Logs");
            System.out.println("2. Filter Logs by User ID");
            System.out.println("3. Clear Historical Logs");
            System.out.println("4. Back");
            System.out.print("Choose an option: ");

            String choice = sc.nextLine().trim();
            switch (choice) {
                case "1" -> displayLogs(activityLogDao.getAllLogs(), "ALL SYSTEM LOGS");
                case "2" -> filterLogsByUser();
                case "3" -> clearHistoricalLogs();
                case "4" -> inMenu = false;
                default -> System.out.println("Invalid option. Please choose 1, 2, 3, or 4.");
            }
        }
    }

    private void filterLogsByUser() {
        System.out.println("Enter 0 to cancel.");
        int userId = readIntInput("User ID to filter by: ");
        if (userId == 0) {
            System.out.println("Filter cancelled.");
            return;
        }

        List<ActivityLog> logs = activityLogDao.getLogsByUserId(userId);
        if (logs.isEmpty()) {
            boolean userExists = userDao.getAllUsers().stream().anyMatch(u -> u.id() == userId);
            if (userExists) {
                System.out.println("User ID " + userId + " has no log entries.");
            } else {
                System.out.println("User ID " + userId + " does not exist.");
            }
            return;
        }
        displayLogs(logs, "LOGS FOR USER ID " + userId);
    }

    private void clearHistoricalLogs() {
        if (!"SYSTEM_ADMINISTRATOR".equalsIgnoreCase(currentAdmin.role())) {
            System.out.println("Access denied. Only the System Administrator can clear logs.");
            return;
        }

        System.out.println("\nWARNING: This permanently deletes every record in Activity_Logs.");
        System.out.print("Type YES to confirm, or anything else to cancel: ");

        String confirmation;
        try {
            confirmation = sc.nextLine().trim();
        } catch (NoSuchElementException e) {
            confirmation = "";
        }

        if (!confirmation.equals("YES")) {
            System.out.println("Clear cancelled. No logs were deleted.");
            return;
        }

        int cleared = activityLogDao.clearAllLogs(currentAdmin);
        if (cleared >= 0) {
            System.out.println("SUCCESS: " + cleared + " log record(s) cleared. The action was recorded as LOGS_CLEARED.");
        } else {
            System.out.println("FAILED: Logs were not cleared.");
        }
    }

    private void displayLogs(List<ActivityLog> logs, String title) {
        System.out.println("\n--- " + title + " ---");
        String border = "-".repeat(5) + "-+-" + "-".repeat(8) + "-+-" + "-".repeat(18)
                + "-+-" + "-".repeat(35) + "-+-" + "-".repeat(20);
        System.out.println(border);
        System.out.printf(LOG_ROW_FORMAT, "ID", "User ID", "Action Type", "Description", "Timestamp");
        System.out.println(border);

        if (logs.isEmpty()) {
            System.out.printf(LOG_ROW_FORMAT, "-", "-", "-", "No logs found", "-");
        }

        for (ActivityLog log : logs) {
            List<String> actionLines = wrap(log.actionType(), ACTION_WIDTH);
            List<String> descriptionLines = wrap(log.description(), DESCRIPTION_WIDTH);
            int rows = Math.max(actionLines.size(), descriptionLines.size());

            for (int i = 0; i < rows; i++) {
                System.out.printf(LOG_ROW_FORMAT,
                        i == 0 ? String.valueOf(log.id()) : "",
                        i == 0 ? (log.userId() == null ? "-" : String.valueOf(log.userId())) : "",
                        i < actionLines.size() ? actionLines.get(i) : "",
                        i < descriptionLines.size() ? descriptionLines.get(i) : "",
                        i == 0 ? (log.timestamp() == null ? "" : log.timestamp()) : "");
            }
        }
        System.out.println(border);
        System.out.println("Total: " + logs.size());
    }

    private List<String> wrap(String text, int width) {
        List<String> lines = new ArrayList<>();
        String remaining = text == null ? "" : text.trim();

        while (remaining.length() > width) {
            int cut = remaining.lastIndexOf(' ', width);
            if (cut <= 0) {
                cut = width;
                lines.add(remaining.substring(0, cut));
                remaining = remaining.substring(cut);
            } else {
                lines.add(remaining.substring(0, cut));
                remaining = remaining.substring(cut + 1);
            }
        }
        lines.add(remaining);
        return lines;
    }

    private int readIntInput(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input;
            try {
                input = sc.nextLine().trim();
            } catch (NoSuchElementException e) {
                return 0;
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
                    System.out.println("Invalid input. That number is out of range. Enter a valid User ID, or 0 to cancel.");
                } else {
                    System.out.println("Invalid input. Please enter a whole number, or 0 to cancel.");
                }
            }
        }
    }

    private String readValidRole() {
        List<String> roles = UserDao.VALID_ROLES;
        return readChoice("Select new role:", "Role", roles);
    }

    private String readValidStatus() {
        List<String> statuses = UserDao.UPDATE_STATUSES;
        return readChoice("Select new account status:", "Status", statuses);
    }

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

            try {
                int number = Integer.parseInt(input);
                if (number >= 1 && number <= options.size()) {
                    return options.get(number - 1);
                }
            } catch (NumberFormatException ignored) {
            }

            for (String option : options) {
                if (option.equalsIgnoreCase(input)) {
                    return option;
                }
            }

            System.out.println("Invalid " + label.toLowerCase()
                    + ". Choose a number from the list, type a valid name, or 0 to cancel.");
        }
    }

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

        userDao.approveUser(userId, currentAdmin);
    }

    private void displayUsers(List<User> users, String title) {
        System.out.println("\n--- " + title + " ---");
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