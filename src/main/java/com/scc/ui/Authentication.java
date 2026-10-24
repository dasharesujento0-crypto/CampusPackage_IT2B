package com.scc.ui;

import com.scc.dao.UserDao;
import com.scc.model.User;

import java.util.Objects;
import java.util.Scanner;

/** Handles public authentication, registration, and routing into role menus. */
public class Authentication {
    private final Scanner scanner;
    private final UserDao userDao;
    private final CampXpressUI campXpressUI;

    public Authentication() {
        this(new UserDao(), new Scanner(System.in));
    }

    public Authentication(UserDao userDao, Scanner scanner) {
        this.userDao = Objects.requireNonNull(userDao, "userDao");
        this.scanner = Objects.requireNonNull(scanner, "scanner");
        this.campXpressUI = new CampXpressUI(this.userDao, this.scanner);
    }

    public void start() {
        boolean running = true;
        while (running) {
            System.out.println("\n========================================");
            System.out.println("              CAMPXPRESS");
            System.out.println(" CAMPUS PACKAGE CLAIM & TRACKING SYSTEM");
            System.out.println("========================================");
            System.out.println("1. Login");
            System.out.println("2. Register");
            System.out.println("3. Exit");
            System.out.print("Choose an option: ");

            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1" -> {
                    User user = login();
                    if (user != null) {
                        running = campXpressUI.runUserSession(user);
                    }
                }
                case "2" -> register();
                case "3" -> {
                    running = false;
                    System.out.println("\nThank you for using CampXpress. Goodbye!");
                }
                default -> System.out.println("\nInvalid option! Please choose 1, 2, or 3.");
            }
        }
    }

    private User login() {
        System.out.println("\n----------- CAMPXPRESS LOGIN -----------");
        System.out.print("Enter username: ");
        String username = scanner.nextLine().trim();
        System.out.print("Enter password: ");
        String password = scanner.nextLine();
        return userDao.loginUser(username, password);
    }

    private void register() {
        System.out.println("\n---------- CAMPXPRESS REGISTER ---------");
        System.out.print("Enter full name: ");
        String fullName = scanner.nextLine().trim();
        System.out.print("Enter new username: ");
        String username = scanner.nextLine().trim();
        System.out.print("Enter password: ");
        String password = scanner.nextLine();

        if (fullName.isEmpty() || username.isEmpty() || password.isEmpty()) {
            System.out.println("Registration failed: Fields cannot be empty.");
            return;
        }

        System.out.println("\nSelect Role:");
        System.out.println("[1] Package Staff");
        System.out.println("[2] Department / Office Representative");
        System.out.print("Choose a role: ");
        String role = switch (scanner.nextLine().trim()) {
            case "1" -> "PACKAGE_STAFF";
            case "2" -> "DEPARTMENT_OFFICE_REPRESENTATIVE";
            default -> null;
        };
        if (role == null) {
            System.out.println("Invalid role selection. Please choose 1 or 2.");
            return;
        }
        userDao.registerUser(username, password, fullName, role);
    }
}
