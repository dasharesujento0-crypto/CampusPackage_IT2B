
package com.scc.dao;

import com.scc.model.PackageItem;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class PackageDao {

    private final AuditLogDao auditLogDao = new AuditLogDao();

    public boolean registerPackage(
            int recipientId,
            String trackingNumber,
            String senderName,
            String description,
            String dateReceived
    ) {
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
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, trackingNumber.trim());
            statement.setString(2, senderName.trim());
            statement.setString(3, description.trim());
            statement.setString(4, dateReceived.trim());
            statement.setInt(5, recipientId);

            if (statement.executeUpdate() == 1) {
                auditLogDao.log(
                        null, "SYSTEM", "PACKAGE_REGISTERED",
                        trackingNumber.trim()
                );
                System.out.println("Package registered successfully.");
                return true;
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

        String term = "%" + (searchTerm == null
                ? "" : searchTerm.trim()) + "%";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

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

    public boolean updateStatus(String trackingNumber, String newStatus) {
        if (blank(trackingNumber) || blank(newStatus)) {
            return false;
        }

        String status = newStatus.trim().toUpperCase(Locale.ROOT);

        if (!Set.of("RECEIVED", "STORED", "UNCLAIMED").contains(status)) {
            System.out.println("Invalid package status.");
            return false;
        }

        String sql = """
                UPDATE Packages
                SET Package_Status = ?
                WHERE Tracking_Number = ?
                  AND Package_Status IN ('RECEIVED', 'STORED', 'UNCLAIMED')
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, status);
            statement.setString(2, trackingNumber.trim());

            if (statement.executeUpdate() == 1) {
                auditLogDao.log(
                        null, "SYSTEM", "PACKAGE_STATUS_UPDATED",
                        trackingNumber.trim() + " -> " + status
                );
                return true;
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
            String dateClaimed
    ) {
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
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, claimantName.trim());
            statement.setString(2, identification.trim());
            statement.setString(3, dateClaimed.trim());
            statement.setString(4, trackingNumber.trim());

            if (statement.executeUpdate() == 1) {
                auditLogDao.log(
                        null, "SYSTEM", "PACKAGE_CLAIMED",
                        trackingNumber.trim()
                                + "; claimant=" + claimantName.trim()
                );
                return true;
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

    public boolean returnPackage(String trackingNumber, String dateReturned) {
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
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, dateReturned.trim());
            statement.setString(2, trackingNumber.trim());

            if (statement.executeUpdate() == 1) {
                auditLogDao.log(
                        null, "SYSTEM", "PACKAGE_RETURNED",
                        trackingNumber.trim()
                );
                return true;
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

    public boolean softRemove(String trackingNumber) {
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
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, trackingNumber.trim());

            if (statement.executeUpdate() == 1) {
                auditLogDao.log(
                        null, "SYSTEM", "PACKAGE_REMOVED",
                        trackingNumber.trim()
                );
                return true;
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
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

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

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
