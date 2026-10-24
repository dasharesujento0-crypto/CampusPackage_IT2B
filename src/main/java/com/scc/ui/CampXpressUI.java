package com.scc.ui;

import com.scc.dao.DepartmentDao;
import com.scc.dao.PackageDao;
import com.scc.dao.RecipientDao;
import com.scc.dao.UserDao;
import com.scc.model.Department;
import com.scc.model.PackageItem;
import com.scc.model.Recipient;
import com.scc.model.User;

import java.util.List;
import java.util.Scanner;

public class CampXpressUI {

    private final Scanner sc;
    private final UserDao userDao;
    private final DepartmentDao departmentDao = new DepartmentDao();
    private final RecipientDao recipientDao = new RecipientDao();
    private final PackageDao packageDao = new PackageDao();

    public CampXpressUI(UserDao userDao, Scanner scanner) {
        this.userDao = java.util.Objects.requireNonNull(userDao, "userDao");
        this.sc = java.util.Objects.requireNonNull(scanner, "scanner");
    }

    /** Runs the authenticated user's role-based session menu. */
    public boolean runUserSession(User user) {
        return userSession(user);
    }

    // ================= USER SESSION =================

    private boolean userSession(User user) {
        boolean loggedIn = true;

        while (loggedIn) {
            System.out.println("\n========================================");
            System.out.println("             CAMPXPRESS HOME");
            System.out.println("========================================");
            System.out.println("Welcome, " + user.fullName() + "!");
            System.out.println("Role: " + user.role());
            System.out.println("Account Status: " + user.accountStatus());

            showRoleWelcome(user.role());

            System.out.println("\n1. View Account Information");
            System.out.println("2. Manage Departments / Offices");
            System.out.println("3. Manage Recipients");
            System.out.println("4. Manage Packages");

            if ("SYSTEM_ADMINISTRATOR".equalsIgnoreCase(user.role())) {
                System.out.println("7. Manage Account Approvals");
            }

            System.out.println("5. Logout");
            System.out.println("6. Exit");
            System.out.print("Choose an option: ");

            String choice = sc.nextLine().trim();

            switch (choice) {
                case "1":
                    viewAccountInformation(user);
                    break;

                case "2":
                    departmentMenu();
                    break;

                case "3":
                    recipientMenu();
                    break;

                case "4":
                    packageMenu();
                    break;

                case "5":
                    loggedIn = false;
                    System.out.println(
                            "\nYou have logged out successfully."
                    );
                    break;

                case "6":
                    System.out.println(
                            "\nThank you for using CampXpress. Goodbye!"
                    );
                    return false;

                case "7":
                    if ("SYSTEM_ADMINISTRATOR".equalsIgnoreCase(user.role())) {
                        accountApprovalMenu();
                    } else {
                        System.out.println(
                                "Access denied. Only the System Administrator can manage account approvals."
                        );
                    }
                    break;

                default:
                    System.out.println(
                            "\nInvalid option! Please choose a valid menu option."
                    );
            }
        }

        return true;
    }

    // ================= ACCOUNT INFORMATION =================

    private void viewAccountInformation(User user) {
        System.out.println("\n------ ACCOUNT INFORMATION ------");
        System.out.println("User ID: " + user.id());
        System.out.println("Full Name: " + user.fullName());
        System.out.println("Username: " + user.username());
        System.out.println("Role: " + user.role());
        System.out.println("Account Status: " + user.accountStatus());
    }

    // ================= ROLE WELCOME =================

    private void showRoleWelcome(String role) {
        switch (role.toUpperCase()) {
            case "PACKAGE_ADMINISTRATOR":
                System.out.println("Package Administrator session.");
                break;

            case "SYSTEM_ADMINISTRATOR":
                System.out.println("System Administrator session.");
                break;

            case "PACKAGE_STAFF":
                System.out.println("Package Staff session.");
                break;

            case "DEPARTMENT_OFFICE_REPRESENTATIVE":
                System.out.println(
                        "Department / Office Representative session."
                );
                break;

            default:
                System.out.println(
                        "Unrecognized role. Please contact the administrator."
                );
        }
    }

    // ================= DEPARTMENT MANAGEMENT =================

    private void departmentMenu() {
        boolean inMenu = true;

        while (inMenu) {
            System.out.println("\n========================================");
            System.out.println("       DEPARTMENT / OFFICE MANAGEMENT");
            System.out.println("========================================");
            System.out.println("1. Register Department / Office");
            System.out.println("2. View All Departments / Offices");
            System.out.println("3. Back to Home");
            System.out.print("Choose an option: ");

            String choice = sc.nextLine().trim();

            switch (choice) {
                case "1":
                    registerDepartment();
                    break;

                case "2":
                    viewAllDepartments();
                    break;

                case "3":
                    inMenu = false;
                    break;

                default:
                    System.out.println(
                            "Invalid option! Please choose 1, 2, or 3."
                    );
            }
        }
    }

    private void registerDepartment() {
        System.out.println("\n--- REGISTER DEPARTMENT / OFFICE ---");

        System.out.print("Department name: ");
        String name = sc.nextLine().trim();

        System.out.print("Department type: ");
        String type = sc.nextLine().trim();

        System.out.print("Contact number: ");
        String contact = sc.nextLine().trim();

        if (name.isEmpty() || type.isEmpty() || contact.isEmpty()) {
            System.out.println(
                    "Registration failed: All fields are required."
            );
            return;
        }

        departmentDao.registerDepartment(name, type, contact);
    }

    private void viewAllDepartments() {
        System.out.println("\n--- DEPARTMENT / OFFICE LIST ---");

        List<Department> departments = departmentDao.getAllDepartments();

        if (departments.isEmpty()) {
            System.out.println("No departments or offices found.");
            return;
        }

        for (Department department : departments) {
            System.out.println(department);
        }

        System.out.println(
                "\nTotal departments/offices: " + departments.size()
        );
    }

    // ================= RECIPIENT MANAGEMENT =================

    private void recipientMenu() {
        boolean inMenu = true;

        while (inMenu) {
            System.out.println("\n========================================");
            System.out.println("             RECIPIENT MANAGEMENT");
            System.out.println("========================================");
            System.out.println("1. Register Recipient");
            System.out.println("2. View All Recipients");
            System.out.println("3. Back to Home");
            System.out.print("Choose an option: ");

            String choice = sc.nextLine().trim();

            switch (choice) {
                case "1":
                    registerRecipient();
                    break;

                case "2":
                    viewAllRecipients();
                    break;

                case "3":
                    inMenu = false;
                    break;

                default:
                    System.out.println(
                            "Invalid option! Please choose 1, 2, or 3."
                    );
            }
        }
    }

    private void registerRecipient() {
        System.out.println("\n--- REGISTER RECIPIENT ---");

        List<Department> departments = departmentDao.getAllDepartments();

        if (departments.isEmpty()) {
            System.out.println(
                    "No departments found. Register a department first."
            );
            return;
        }

        System.out.println("\nAvailable Departments / Offices:");

        for (Department department : departments) {
            System.out.println(
                    "ID: " + department.departmentId()
                            + " | " + department.departmentName()
                            + " | Type: " + department.departmentType()
            );
        }

        System.out.print("\nEnter Department ID: ");
        String departmentInput = sc.nextLine().trim();

        int departmentId;

        try {
            departmentId = Integer.parseInt(departmentInput);

            if (departmentId <= 0) {
                System.out.println(
                        "Department ID must be a positive number."
                );
                return;
            }
        } catch (NumberFormatException e) {
            System.out.println(
                    "Invalid Department ID. Please enter a number."
            );
            return;
        }

        System.out.print("Recipient name: ");
        String name = sc.nextLine().trim();

        System.out.println("\nRecipient type:");
        System.out.println("1. STUDENT");
        System.out.println("2. TEACHER");
        System.out.println("3. STAFF");
        System.out.println("4. OTHER");
        System.out.print("Choose recipient type: ");

        String typeChoice = sc.nextLine().trim();
        String type;

        switch (typeChoice) {
            case "1":
                type = "STUDENT";
                break;

            case "2":
                type = "TEACHER";
                break;

            case "3":
                type = "STAFF";
                break;

            case "4":
                type = "OTHER";
                break;

            default:
                System.out.println("Invalid recipient type.");
                return;
        }

        System.out.print("Identification number: ");
        String identificationNumber = sc.nextLine().trim();

        System.out.print("Contact number (optional): ");
        String contactNumber = sc.nextLine().trim();

        if (name.isEmpty() || identificationNumber.isEmpty()) {
            System.out.println(
                    "Registration failed: Name and identification number are required."
            );
            return;
        }

        recipientDao.registerRecipient(
                departmentId,
                name,
                type,
                identificationNumber,
                contactNumber
        );
    }

    private void viewAllRecipients() {
        System.out.println("\n--- RECIPIENT LIST ---");

        List<Recipient> recipients = recipientDao.getAllRecipients();

        if (recipients.isEmpty()) {
            System.out.println("No recipients found.");
            return;
        }

        for (Recipient recipient : recipients) {
            System.out.println(recipient);
        }

        System.out.println(
                "\nTotal recipients: " + recipients.size()
        );
    }

    // ================= PACKAGE MANAGEMENT =================

    private void packageMenu() {
        boolean inMenu = true;

        while (inMenu) {
            System.out.println("\n========================================");
            System.out.println("              PACKAGE MANAGEMENT");
            System.out.println("========================================");
            System.out.println("1. Register Package");
            System.out.println("2. View All Packages");
            System.out.println("3. Back to Home");
            System.out.print("Choose an option: ");

            String choice = sc.nextLine().trim();

            switch (choice) {
                case "1":
                    registerPackage();
                    break;

                case "2":
                    viewAllPackages();
                    break;

                case "3":
                    inMenu = false;
                    break;

                default:
                    System.out.println(
                            "Invalid option! Please choose 1, 2, or 3."
                    );
            }
        }
    }

    private void registerPackage() {
        System.out.println("\n--- REGISTER PACKAGE ---");

        List<Recipient> recipients = recipientDao.getAllRecipients();

        if (recipients.isEmpty()) {
            System.out.println(
                    "No recipients found. Register a recipient first."
            );
            return;
        }

        System.out.println("\nAvailable Recipients:");

        for (Recipient recipient : recipients) {
            System.out.println(recipient);
        }

        System.out.print("\nEnter Recipient ID: ");
        String recipientInput = sc.nextLine().trim();

        int recipientId;

        try {
            recipientId = Integer.parseInt(recipientInput);

            if (recipientId <= 0) {
                System.out.println(
                        "Recipient ID must be a positive number."
                );
                return;
            }
        } catch (NumberFormatException e) {
            System.out.println(
                    "Invalid Recipient ID. Please enter a number."
            );
            return;
        }

        System.out.print("Enter Tracking Number: ");
        String trackingNumber = sc.nextLine().trim();

        System.out.print("Enter Sender Name: ");
        String senderName = sc.nextLine().trim();

        System.out.print("Enter Package Description: ");
        String description = sc.nextLine().trim();

        System.out.print("Enter Date Received (YYYY-MM-DD): ");
        String dateReceived = sc.nextLine().trim();

        if (trackingNumber.isEmpty()
                || senderName.isEmpty()
                || description.isEmpty()
                || dateReceived.isEmpty()) {
            System.out.println(
                    "Registration failed: All package fields are required."
            );
            return;
        }

        if (!dateReceived.matches("\\d{4}-\\d{2}-\\d{2}")) {
            System.out.println(
                    "Invalid date format. Please use YYYY-MM-DD."
            );
            return;
        }

        packageDao.registerPackage(
                recipientId,
                trackingNumber,
                senderName,
                description,
                dateReceived
        );
    }

    private void viewAllPackages() {
        System.out.println("\n--- PACKAGE LIST ---");

        List<PackageItem> packages = packageDao.getAllPackages();

        if (packages.isEmpty()) {
            System.out.println("No packages found.");
            return;
        }

        for (PackageItem packageItem : packages) {
            System.out.println(packageItem);
            System.out.println("----------------------------------------");
        }

        System.out.println("\nTotal packages: " + packages.size());
    }

    // ================= ACCOUNT APPROVAL =================

    private void accountApprovalMenu() {
        boolean inMenu = true;

        while (inMenu) {
            System.out.println("\n========================================");
            System.out.println("          ACCOUNT APPROVAL");
            System.out.println("========================================");

            List<User> pendingUsers = userDao.getPendingUsers();

            if (pendingUsers.isEmpty()) {
                System.out.println("There are no pending accounts.");
                System.out.println("1. Refresh Pending Accounts");
                System.out.println("2. Back to Home");
                System.out.print("Choose an option: ");

                String choice = sc.nextLine().trim();

                if (choice.equals("2")) {
                    inMenu = false;
                } else if (!choice.equals("1")) {
                    System.out.println(
                            "Invalid option. Please choose 1 or 2."
                    );
                }

                continue;
            }

            System.out.println("Pending accounts:");

            for (User pendingUser : pendingUsers) {
                System.out.println("----------------------------------------");
                System.out.println("User ID: " + pendingUser.id());
                System.out.println("Full Name: " + pendingUser.fullName());
                System.out.println("Username: " + pendingUser.username());
                System.out.println("Requested Role: " + pendingUser.role());
                System.out.println(
                        "Status: " + pendingUser.accountStatus()
                );
            }

            System.out.println("----------------------------------------");
            System.out.println("1. Approve Account");
            System.out.println("2. Reject Account");
            System.out.println("3. Refresh Pending Accounts");
            System.out.println("4. Back to Home");
            System.out.print("Choose an option: ");

            String choice = sc.nextLine().trim();

            switch (choice) {
                case "1":
                case "2":
                    System.out.print("Enter the User ID: ");
                    String idInput = sc.nextLine().trim();

                    int userId;

                    try {
                        userId = Integer.parseInt(idInput);

                        if (userId <= 0) {
                            System.out.println(
                                    "User ID must be a positive number."
                            );
                            break;
                        }
                    } catch (NumberFormatException e) {
                        System.out.println(
                                "Invalid User ID. Please enter a number."
                        );
                        break;
                    }

                    String newStatus =
                            choice.equals("1") ? "APPROVED" : "REJECTED";

                    userDao.updateAccountStatus(userId, newStatus);
                    break;

                case "3":
                    System.out.println("Refreshing pending accounts...");
                    break;

                case "4":
                    inMenu = false;
                    break;

                default:
                    System.out.println(
                            "Invalid option. Please choose 1, 2, 3, or 4."
                    );
            }
        }
    }
}
