package com.scc.ui;

import com.scc.dao.ActivityLogDao;
import com.scc.dao.DatabaseConnection;
import com.scc.dao.DepartmentDao;
import com.scc.dao.PackageDao;
import com.scc.dao.RecipientDao;
import com.scc.dao.UserDao;
import com.scc.model.Department;
import com.scc.model.PackageItem;
import com.scc.model.Recipient;
import com.scc.model.User;

import java.io.Console;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Scanner;

public class CampXpressUI {

    private final Scanner sc = new Scanner(System.in);
    private final UserDao userDao = new UserDao();
    private final DepartmentDao departmentDao = new DepartmentDao();
    private final RecipientDao recipientDao = new RecipientDao();
    private final PackageDao packageDao = new PackageDao();
    private final ActivityLogDao activityLogDao = new ActivityLogDao();
    private final Authentication authentication;

    public CampXpressUI(Authentication authentication) {
        this.authentication = java.util.Objects.requireNonNull(authentication, "authentication");
    }

    // ================= MAIN MENU =================

    public void run() {
        // One-time local setup; does nothing once a System Administrator exists.
        firstRunAdminSetup();

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

            String choice = sc.nextLine().trim();

            switch (choice) {
                case "1":
                    User loggedInUser = login();

                    if (loggedInUser != null) {
                        authentication.recordLogin(loggedInUser);
                        running = userSession(loggedInUser);
                    }
                    break;

                case "2":
                    register();
                    break;

                case "3":
                    running = false;
                    System.out.println(
                            "\nThank you for using CampXpress. Goodbye!"
                    );
                    break;

                default:
                    System.out.println(
                            "\nInvalid option! Please choose 1, 2, or 3."
                    );
            }
        }

        sc.close();
    }

    // ================= FIRST-RUN ADMIN SETUP =================

    /**
     * One-time, local setup that creates the initial SYSTEM_ADMINISTRATOR.
     * It is offered only while no SYSTEM_ADMINISTRATOR exists, and
     * UserDao.createInitialAdministrator() refuses to insert once one does,
     * so it cannot create extra administrators or overwrite an existing one.
     * It is not part of public registration.
     */
    private void firstRunAdminSetup() {
        if (userDao.hasSystemAdministrator()) {
            return;
        }

        System.out.println("\n========================================");
        System.out.println("     FIRST-RUN ADMINISTRATOR SETUP");
        System.out.println("========================================");
        System.out.println("No System Administrator account exists yet.");
        System.out.println("Database file: " + DatabaseConnection.getDatabasePath());
        System.out.println("Whoever completes this step on this computer becomes");
        System.out.println("the initial System Administrator.");
        System.out.println("Enter 0 at the full name or username prompt to skip.");
        System.out.println("Setup will be offered again on the next launch.");

        try {
            while (true) {
                String fullName = readSetupText("\nAdministrator full name: ", "Full name");
                if (fullName == null) {
                    return;
                }
                String username = readSetupText("Administrator username: ", "Username");
                if (username == null) {
                    return;
                }
                String password = readSetupPassword();

                if (userDao.createInitialAdministrator(username, password, fullName)) {
                    System.out.println("\nSUCCESS: Initial System Administrator '"
                            + username + "' created (APPROVED).");
                    System.out.println("You can now log in with these credentials.");
                    return;
                }

                if (userDao.hasSystemAdministrator()) {
                    System.out.println("A System Administrator already exists. Setup is closed.");
                    return;
                }
                System.out.println("The administrator could not be created. Please try again.");
            }
        } catch (NoSuchElementException e) {
            System.out.println("\nInput ended. Setup skipped.");
        }
    }

    /** Returns the trimmed text, or null if the operator typed 0 to skip. */
    private String readSetupText(String prompt, String label) {
        while (true) {
            System.out.print(prompt);
            String input = sc.nextLine().trim();
            if (input.equals("0")) {
                System.out.println("Setup skipped.");
                return null;
            }
            if (!input.isEmpty()) {
                return input;
            }
            System.out.println(label + " cannot be empty. Enter a value, or 0 to skip.");
        }
    }

    private String readSetupPassword() {
        final int minLength = 8;

        if (System.console() == null) {
            System.out.println("Note: this console cannot hide typed characters.");
        }
        while (true) {
            String password = readSecret("Password (min " + minLength + " characters): ");
            if (password.length() < minLength) {
                System.out.println("Password must be at least " + minLength + " characters.");
                continue;
            }
            String confirm = readSecret("Confirm password: ");
            if (!password.equals(confirm)) {
                System.out.println("Passwords do not match. Try again.");
                continue;
            }
            return password;
        }
    }

    /** Hidden input when a real terminal is available, otherwise a normal line. */
    private String readSecret(String prompt) {
        Console console = System.console();
        if (console != null) {
            char[] chars = console.readPassword("%s", prompt);
            if (chars == null) {
                throw new NoSuchElementException("input ended");
            }
            String secret = new String(chars);
            Arrays.fill(chars, ' ');
            return secret;
        }
        System.out.print(prompt);
        return sc.nextLine();
    }

    // ================= LOGIN =================

    private User login() {
        System.out.println("\n----------- CAMPXPRESS LOGIN -----------");

        System.out.print("Enter username: ");
        String username = sc.nextLine().trim();

        System.out.print("Enter password: ");
        String password = sc.nextLine();

        return userDao.loginUser(username, password);
    }

    // ================= USER SESSION =================

    private boolean userSession(User user) {
        boolean loggedIn = true;
        boolean keepRunning = true;
        boolean isAdmin = "SYSTEM_ADMINISTRATOR".equalsIgnoreCase(user.role());
        String logoutOption = isAdmin ? "6" : "5";
        String exitOption = isAdmin ? "7" : "6";

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

            if (isAdmin) {
                System.out.println("5. Manage User Accounts");
            }

            System.out.println(logoutOption + ". Logout");
            System.out.println(exitOption + ". Exit");
            System.out.print("Choose an option: ");

            String choice = sc.nextLine().trim();

            switch (choice) {
                case "1":
                    viewAccountInformation(user);
                    break;

                case "2":
                    departmentMenu(user);
                    break;

                case "3":
                    recipientMenu(user);
                    break;

                case "4":
                    packageMenu(user);
                    break;

                default:
                    if (isAdmin && choice.equals("5")) {
                        new AdminMenu(sc, userDao, activityLogDao, user).run();
                    } else if (choice.equals(logoutOption)) {
                        loggedIn = false;
                        System.out.println(
                                "\nYou have logged out successfully."
                        );
                    } else if (choice.equals(exitOption)) {
                        loggedIn = false;
                        keepRunning = false;
                        System.out.println(
                                "\nThank you for using CampXpress. Goodbye!"
                        );
                    } else {
                        System.out.println(
                                "\nInvalid option! Please choose a valid menu option."
                        );
                    }
            }
        }

        // Logout and Exit both end up here, so a session is logged out exactly once.
        authentication.recordLogout(user);
        return keepRunning;
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

    // ================= USER REGISTRATION =================

    private void register() {
        System.out.println("\n---------- CAMPXPRESS REGISTER ---------");

        System.out.print("Enter full name: ");
        String fullName = sc.nextLine().trim();

        System.out.print("Enter new username: ");
        String username = sc.nextLine().trim();

        System.out.print("Enter password: ");
        String password = sc.nextLine();

        if (fullName.isEmpty()
                || username.isEmpty()
                || password.isEmpty()) {
            System.out.println(
                    "Registration failed: Fields cannot be empty."
            );
            return;
        }

        // Public registration does not allow users to assign
        // themselves administrator privileges.
        System.out.println("\nSelect Role:");
        System.out.println("[1] Package Staff");
        System.out.println("[2] Department / Office Representative");
        System.out.print("Choose a role: ");

        String roleChoice = sc.nextLine().trim();
        String role;

        switch (roleChoice) {
            case "1":
                role = "PACKAGE_STAFF";
                break;

            case "2":
                role = "DEPARTMENT_OFFICE_REPRESENTATIVE";
                break;

            default:
                System.out.println(
                        "Invalid role selection. Please choose 1 or 2."
                );
                return;
        }

        userDao.registerUser(username, password, fullName, role);
    }

    // ================= DEPARTMENT MANAGEMENT =================

    private void departmentMenu(User user) {
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
                    registerDepartment(user);
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

    private void registerDepartment(User user) {
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

        departmentDao.registerDepartment(name, type, contact, user);
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

    private void recipientMenu(User user) {
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
                    registerRecipient(user);
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

    private void registerRecipient(User user) {
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
                contactNumber,
                user
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

    private void packageMenu(User user) {
        boolean inMenu = true;

        while (inMenu) {
            System.out.println("\n========================================");
            System.out.println("              PACKAGE MANAGEMENT");
            System.out.println("========================================");
            System.out.println("1. Register Package");
            System.out.println("2. View All Packages");
            System.out.println("3. Export Package Summary to CSV");
            System.out.println("4. Update Package Status");
            System.out.println("5. Back to Home");
            System.out.print("Choose an option: ");

            String choice = sc.nextLine().trim();

            switch (choice) {
                case "1":
                    registerPackage(user);
                    break;

                case "2":
                    viewAllPackages();
                    break;

                case "3":
                    exportPackageSummary();
                    break;

                case "4":
                    updatePackageStatus(user);
                    break;

                case "5":
                    inMenu = false;
                    break;

                default:
                    System.out.println(
                            "Invalid option! Please choose 1, 2, 3, 4, or 5."
                    );
            }
        }
    }

    private void registerPackage(User user) {
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
                dateReceived,
                user
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

    // ================= UPDATE PACKAGE STATUS =================

    private void updatePackageStatus(User user) {
        System.out.println("\n--- UPDATE PACKAGE STATUS ---");

        if (!PackageDao.STATUS_UPDATE_ROLES.contains(user.role().toUpperCase(Locale.ROOT))) {
            System.out.println(
                    "Access denied. Your role is not allowed to update package status."
            );
            return;
        }

        List<PackageItem> updatable = packageDao.getAllPackages().stream()
                .filter(p -> PackageDao.UPDATABLE_STATUSES.contains(p.packageStatus()))
                .toList();

        if (updatable.isEmpty()) {
            System.out.println("No packages with a changeable status were found.");
            return;
        }

        for (PackageItem packageItem : updatable) {
            System.out.println(packageItem);
            System.out.println("----------------------------------------");
        }

        System.out.println("Enter 0 at any prompt to cancel.");

        int packageId = readIdOrCancel("Package ID: ");
        if (packageId == 0) {
            System.out.println("Update cancelled.");
            return;
        }

        PackageItem selected = updatable.stream()
                .filter(p -> p.packageId() == packageId)
                .findFirst()
                .orElse(null);

        if (selected == null) {
            System.out.println("Package ID " + packageId
                    + " is not in the list above. No changes made.");
            return;
        }

        List<String> options = PackageDao.UPDATABLE_STATUSES.stream()
                .filter(status -> !status.equals(selected.packageStatus()))
                .toList();

        String newStatus = readStatusChoice(selected, options);
        if (newStatus == null) {
            System.out.println("Update cancelled.");
            return;
        }

        if (packageDao.updateStatus(selected.trackingNumber(), newStatus, user)) {
            System.out.println("SUCCESS: Package " + selected.trackingNumber()
                    + " changed from " + selected.packageStatus()
                    + " to " + newStatus + ".");
        } else {
            System.out.println("FAILED: Package " + selected.trackingNumber()
                    + " was not updated.");
        }
    }

    /** Re-prompts until a whole number >= 0 is typed. Returns 0 to cancel (or if input ends). */
    private int readIdOrCancel(String prompt) {
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
                System.out.println("Invalid input. Please enter a whole number, or 0 to cancel.");
            }
        }
    }

    /** Returns the chosen status (by number or name), or null if cancelled. */
    private String readStatusChoice(PackageItem selected, List<String> options) {
        while (true) {
            System.out.println("\nCurrent status of " + selected.trackingNumber()
                    + ": " + selected.packageStatus());
            System.out.println("Select new status:");
            for (int i = 0; i < options.size(); i++) {
                System.out.println("  [" + (i + 1) + "] " + options.get(i));
            }
            System.out.println("  [0] Cancel");
            System.out.print("Status (number or name): ");

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
                // not a number: try it as a name below
            }

            for (String option : options) {
                if (option.equalsIgnoreCase(input)) {
                    return option;
                }
            }

            System.out.println(
                    "Invalid status. Choose a number from the list, type a valid name, or 0 to cancel."
            );
        }
    }

    // ================= CSV EXPORT =================

    private void exportPackageSummary() {
        System.out.println("\n--- EXPORT PACKAGE SUMMARY TO CSV ---");

        System.out.print("Enter start date (YYYY-MM-DD): ");
        String fromDate = sc.nextLine().trim();

        System.out.print("Enter end date (YYYY-MM-DD): ");
        String toDate = sc.nextLine().trim();

        if (!fromDate.matches("\\d{4}-\\d{2}-\\d{2}")
                || !toDate.matches("\\d{4}-\\d{2}-\\d{2}")) {
            System.out.println(
                    "Invalid date format. Please use YYYY-MM-DD."
            );
            return;
        }

        if (fromDate.compareTo(toDate) > 0) {
            System.out.println(
                    "Invalid date range. The start date must not be after the end date."
            );
            return;
        }

        packageDao.exportSummaryCsv(fromDate, toDate);
    }

    // ================= ACCOUNT APPROVAL =================

    private void accountApprovalMenu(User admin) {
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

                    userDao.updateAccountStatus(userId, newStatus, admin);
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