package com.scc.ui;

import com.scc.dao.UserDao;
import com.scc.model.User;

import java.util.List;
import java.util.Scanner;

public class AdminMenu {
    private final Scanner sc;
    private final UserDao userDao;

    public AdminMenu(Scanner sc, UserDao userDao) {
        this.sc = sc;
        this.userDao = userDao;
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
            System.out.println("4. Back to Home");
            System.out.print("Choose an option: ");

            String choice = sc.nextLine().trim();
            switch (choice) {
                case "1" -> displayUsers(userDao.getAllUsers(), "ALL SYSTEM USERS");
                case "2" -> displayUsers(userDao.getPendingUsers(), "PENDING REGISTRATIONS");
                case "3" -> approvePendingUser();
                case "4" -> inMenu = false;
                default -> System.out.println("Invalid option. Please choose 1, 2, 3, or 4.");
            }
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

        userDao.approveUser(userId);
    }

    private void displayUsers(List<User> users, String title) {
        System.out.println("\n--- " + title + " ---");
        String border = "+-------+-----------------+----------------------+------------+";
        System.out.println(border);
        System.out.printf("| %-5s | %-15s | %-20s | %-10s |%n",
                "ID", "USERNAME", "ROLE", "STATUS");
        System.out.println(border);

        if (users.isEmpty()) {
            System.out.printf("| %-5s | %-15s | %-20s | %-10s |%n",
                    "-", "No users found", "-", "-");
        } else {
            for (User user : users) {
                System.out.printf("| %-5s | %-15s | %-20s | %-10s |%n",
                        user.id(),
                        shorten(user.username(), 15),
                        shorten(user.role(), 20),
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
