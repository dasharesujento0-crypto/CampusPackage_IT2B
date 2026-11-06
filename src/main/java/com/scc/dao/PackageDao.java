package com.scc.dao;

import com.scc.model.PackageItem;
import com.scc.model.User;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

public class PackageDao {

    /** Statuses a package may be moved between by the "Update Package Status" workflow. */
    public static final List<String> UPDATABLE_STATUSES = List.of(
            "RECEIVED",
            "STORED",
            "UNCLAIMED"
    );

    /** Roles allowed to change a package's status (department representatives may not). */
    public static final Set<String> STATUS_UPDATE_ROLES = Set.of(
            "SYSTEM_ADMINISTRATOR",
            "PACKAGE_ADMINISTRATOR",
            "PACKAGE_STAFF"
    );

    private final ActivityLogDao activityLogDao = new ActivityLogDao();

    private static String who(User actor) {
        return "User " + actor.username() + " (ID: " + actor.id() + ")";
    }

    public boolean registerPackage(
            int recipientId,
            String trackingNumber,
            String senderName,
            String description,
            String dateReceived,
            User actor
    ) {
        Objects.requireNonNull(actor, "actor");
        if (recipientId <= 0
                || blank(trackingNumber)
                || blank(senderName)
                || blank(description)
                || blank(dateReceived)) {
            System.out.println("All package fields are required.");
            return false;
        }

        String sql = """
                INSERT INTO Packages
                    (Recipient_ID, Tracking_Number, Sender_Name,
                     Package_Description, Date_Received, Package_Status)
                SELECT Recipient_ID, ?, ?, ?, ?, 'RECEIVED'
                FROM Recipients
                WHERE Recipient_ID = ?
                  AND Recipient_Status = 'ACTIVE'
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, trackingNumber.trim());
            statement.setString(2, senderName.trim());
            statement.setString(3, description.trim());
            statement.setString(4, dateReceived.trim());
            statement.setInt(5, recipientId);

            connection.setAutoCommit(false);

            try {
                if (statement.executeUpdate() == 1) {
                    activityLogDao.logAction(
                            connection, actor.id(), "PACKAGE_REGISTERED",
                            who(actor) + " registered package "
                                    + trackingNumber.trim()
                                    + " for Recipient ID " + recipientId + "."
                    );
                    connection.commit();
                    System.out.println("Package registered successfully.");
                    return true;
                }
                connection.rollback();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }

            System.out.println(
                    "Package registration failed: "
                            + "No active recipient found."
            );

        } catch (SQLException e) {
            System.out.println(
                    "Package registration failed: " + e.getMessage()
            );
        }

        return false;
    }

    public List<PackageItem> getAllPackages() {
        return search("");
    }

    public List<PackageItem> search(String searchTerm) {
        List<PackageItem> packages = new ArrayList<>();

        String sql = """
                SELECT p.Package_ID, p.Recipient_ID, p.Tracking_Number,
                       p.Sender_Name, p.Package_Description,
                       p.Date_Received, p.Package_Status
                FROM Packages p
                JOIN Recipients r
                  ON p.Recipient_ID = r.Recipient_ID
                WHERE p.Package_Status <> 'REMOVED'
                  AND (
                       p.Tracking_Number LIKE ?
                    OR p.Sender_Name LIKE ?
                    OR p.Package_Description LIKE ?
                    OR r.Recipient_Name LIKE ?
                  )
                ORDER BY p.Package_ID DESC
                """;

        String term = "%"
                + (searchTerm == null ? "" : searchTerm.trim())
                + "%";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            for (int i = 1; i <= 4; i++) {
                statement.setString(i, term);
            }

            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    packages.add(new PackageItem(
                            result.getInt("Package_ID"),
                            result.getInt("Recipient_ID"),
                            result.getString("Tracking_Number"),
                            result.getString("Sender_Name"),
                            result.getString("Package_Description"),
                            result.getString("Date_Received"),
                            result.getString("Package_Status")
                    ));
                }
            }

        } catch (SQLException e) {
            System.out.println(
                    "Package search failed: " + e.getMessage()
            );
        }

        return packages;
    }

    public boolean updateStatus(String trackingNumber, String newStatus, User actor) {
        Objects.requireNonNull(actor, "actor");

        if (blank(trackingNumber) || blank(newStatus)) {
            return false;
        }

        String status = newStatus.trim().toUpperCase(Locale.ROOT);

        if (!UPDATABLE_STATUSES.contains(status)) {
            System.out.println("Invalid package status.");
            return false;
        }

        if (!STATUS_UPDATE_ROLES.contains(actor.role().toUpperCase(Locale.ROOT))) {
            System.out.println(
                    "Access denied. Your role is not allowed to update package status."
            );
            return false;
        }

        String selectSql = """
                SELECT Package_ID, Package_Status
                FROM Packages
                WHERE Tracking_Number = ?
                """;

        String sql = """
                UPDATE Packages
                SET Package_Status = ?
                WHERE Tracking_Number = ?
                  AND Package_Status = ?
                """;

        try (Connection connection = DatabaseConnection.getConnection()) {

            int packageId;
            String oldStatus;

            try (PreparedStatement select = connection.prepareStatement(selectSql)) {
                select.setString(1, trackingNumber.trim());

                try (ResultSet result = select.executeQuery()) {
                    if (!result.next()) {
                        System.out.println(
                                "Package not found or its status cannot be changed."
                        );
                        return false;
                    }
                    packageId = result.getInt("Package_ID");
                    oldStatus = result.getString("Package_Status");
                }
            }

            if (!UPDATABLE_STATUSES.contains(oldStatus)) {
                System.out.println(
                        "Package not found or its status cannot be changed."
                );
                return false;
            }

            if (oldStatus.equals(status)) {
                System.out.println(
                        "Package " + trackingNumber.trim()
                                + " is already " + status + ". No change made."
                );
                return false;
            }

            connection.setAutoCommit(false);

            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, status);
                statement.setString(2, trackingNumber.trim());
                statement.setString(3, oldStatus);

                if (statement.executeUpdate() == 1) {
                    activityLogDao.logAction(
                            connection, actor.id(), "PKG_STATUS_UPDATED",
                            who(actor) + " changed package "
                                    + trackingNumber.trim()
                                    + " (ID: " + packageId + ") status from "
                                    + oldStatus + " to " + status + "."
                    );
                    connection.commit();
                    return true;
                }
                connection.rollback();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }

            System.out.println(
                    "Package not found or its status cannot be changed."
            );

        } catch (SQLException e) {
            System.out.println(
                    "Package status update failed: " + e.getMessage()
            );
        }

        return false;
    }

    public boolean claimPackage(
            String trackingNumber,
            String claimantName,
            String identification,
            String dateClaimed,
            User actor
    ) {
        Objects.requireNonNull(actor, "actor");
        if (blank(trackingNumber) || blank(claimantName)
                || blank(identification) || blank(dateClaimed)) {
            System.out.println("Claim details are required.");
            return false;
        }

        String sql = """
                UPDATE Packages
                SET Package_Status = 'CLAIMED',
                    Claimant_Name = ?,
                    Claimant_Identification = ?,
                    Date_Claimed = ?
                WHERE Tracking_Number = ?
                  AND Package_Status IN ('RECEIVED', 'STORED', 'UNCLAIMED')
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, claimantName.trim());
            statement.setString(2, identification.trim());
            statement.setString(3, dateClaimed.trim());
            statement.setString(4, trackingNumber.trim());

            connection.setAutoCommit(false);

            try {
                if (statement.executeUpdate() == 1) {
                    activityLogDao.logAction(
                            connection, actor.id(), "PACKAGE_CLAIMED",
                            who(actor) + " marked package "
                                    + trackingNumber.trim()
                                    + " as claimed by " + claimantName.trim() + "."
                    );
                    connection.commit();
                    return true;
                }
                connection.rollback();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }

            System.out.println(
                    "Package not found or already claimed/returned."
            );

        } catch (SQLException e) {
            System.out.println(
                    "Package claim failed: " + e.getMessage()
            );
        }

        return false;
    }

    public boolean returnPackage(
            String trackingNumber,
            String dateReturned,
            User actor
    ) {
        Objects.requireNonNull(actor, "actor");
        if (blank(trackingNumber) || blank(dateReturned)) {
            return false;
        }

        String sql = """
                UPDATE Packages
                SET Package_Status = 'RETURNED',
                    Date_Returned = ?
                WHERE Tracking_Number = ?
                  AND Package_Status IN ('RECEIVED', 'STORED', 'UNCLAIMED')
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, dateReturned.trim());
            statement.setString(2, trackingNumber.trim());

            connection.setAutoCommit(false);

            try {
                if (statement.executeUpdate() == 1) {
                    activityLogDao.logAction(
                            connection, actor.id(), "PACKAGE_RETURNED",
                            who(actor) + " marked package "
                                    + trackingNumber.trim() + " as returned."
                    );
                    connection.commit();
                    return true;
                }
                connection.rollback();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }

            System.out.println(
                    "Package not found or cannot be returned."
            );

        } catch (SQLException e) {
            System.out.println(
                    "Package return failed: " + e.getMessage()
            );
        }

        return false;
    }

    public boolean softRemove(String trackingNumber, User actor) {
        Objects.requireNonNull(actor, "actor");
        if (blank(trackingNumber)) {
            return false;
        }

        String sql = """
                UPDATE Packages
                SET Package_Status = 'REMOVED'
                WHERE Tracking_Number = ?
                  AND Package_Status <> 'REMOVED'
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, trackingNumber.trim());

            connection.setAutoCommit(false);

            try {
                if (statement.executeUpdate() == 1) {
                    activityLogDao.logAction(
                            connection, actor.id(), "PACKAGE_REMOVED",
                            who(actor) + " removed package "
                                    + trackingNumber.trim() + "."
                    );
                    connection.commit();
                    return true;
                }
                connection.rollback();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }

        } catch (SQLException e) {
            System.out.println(
                    "Package removal failed: " + e.getMessage()
            );
        }

        return false;
    }

    public void printSummary(String fromDate, String toDate) {
        if (blank(fromDate) || blank(toDate)
                || fromDate.compareTo(toDate) > 0) {
            System.out.println("Invalid report date range.");
            return;
        }

        String sql = """
                SELECT Package_Status, COUNT(*) AS Total
                FROM Packages
                WHERE Date_Received BETWEEN ? AND ?
                  AND Package_Status <> 'REMOVED'
                GROUP BY Package_Status
                ORDER BY Package_Status
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, fromDate.trim());
            statement.setString(2, toDate.trim());

            System.out.println("\n--- PACKAGE PROCESSING SUMMARY ---");
            System.out.println(
                    "Received-date range: " + fromDate + " to " + toDate
            );

            int total = 0;

            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    int count = result.getInt("Total");
                    total += count;

                    System.out.printf(
                            "%-12s %d%n",
                            result.getString("Package_Status"),
                            count
                    );
                }
            }

            System.out.println("TOTAL PACKAGES: " + total);

        } catch (SQLException e) {
            System.out.println(
                    "Report failed: " + e.getMessage()
            );
        }
    }

    public boolean exportSummaryCsv(String fromDate, String toDate) {
        if (blank(fromDate) || blank(toDate)
                || fromDate.compareTo(toDate) > 0) {
            System.out.println("Invalid report date range.");
            return false;
        }

        String sql = """
                SELECT Package_Status, COUNT(*) AS Total
                FROM Packages
                WHERE Date_Received BETWEEN ? AND ?
                  AND Package_Status <> 'REMOVED'
                GROUP BY Package_Status
                ORDER BY Package_Status
                """;

        Path outputPath = Path.of("CampXpress_Report.csv");

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, fromDate.trim());
            statement.setString(2, toDate.trim());

            try (ResultSet result = statement.executeQuery();
                 BufferedWriter writer = Files.newBufferedWriter(
                         outputPath,
                         StandardCharsets.UTF_8,
                         StandardOpenOption.CREATE,
                         StandardOpenOption.TRUNCATE_EXISTING,
                         StandardOpenOption.WRITE)) {

                writer.write("Start Date,End Date,Package Status,Total");
                writer.newLine();

                while (result.next()) {
                    writer.write(
                            csv(fromDate.trim()) + ","
                                    + csv(toDate.trim()) + ","
                                    + csv(result.getString("Package_Status")) + ","
                                    + result.getInt("Total")
                    );
                    writer.newLine();
                }
            }

            System.out.println(
                    "CSV report exported successfully to: "
                            + outputPath.toAbsolutePath()
            );
            return true;

        } catch (SQLException | IOException e) {
            System.out.println(
                    "CSV export failed: " + e.getMessage()
            );
            return false;
        }
    }

    private String csv(String value) {
        if (value == null) {
            return "\"\"";
        }

        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }
}