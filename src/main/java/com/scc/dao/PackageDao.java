package com.scc.dao;

import com.scc.model.PackageItem;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class PackageDao {

    public boolean registerPackage(
            int recipientId,
            String trackingNumber,
            String senderName,
            String packageDescription,
            String dateReceived
    ) {
        String checkSql = """
                SELECT 1
                FROM Recipients
                WHERE Recipient_ID = ?
                  AND Recipient_Status = 'ACTIVE'
                """;

        String insertSql = """
                INSERT INTO Packages
                (Recipient_ID, Tracking_Number, Sender_Name,
                 Package_Description, Date_Received, Package_Status)
                VALUES (?, ?, ?, ?, ?, 'RECEIVED')
                """;

        try (Connection conn = DatabaseConnection.getConnection()) {

            // Confirm that the recipient exists and is active.
            try (PreparedStatement check =
                         conn.prepareStatement(checkSql)) {

                check.setInt(1, recipientId);

                try (ResultSet rs = check.executeQuery()) {
                    if (!rs.next()) {
                        System.out.println(
                                "Registration failed: Recipient ID not found "
                                        + "or recipient is not active."
                        );
                        return false;
                    }
                }
            }

            // Save the package.
            try (PreparedStatement pstmt =
                         conn.prepareStatement(insertSql)) {

                pstmt.setInt(1, recipientId);
                pstmt.setString(2, trackingNumber);
                pstmt.setString(3, senderName);
                pstmt.setString(4, packageDescription);
                pstmt.setString(5, dateReceived);

                int rowsAffected = pstmt.executeUpdate();

                if (rowsAffected == 1) {
                    System.out.println(
                            "Package registered successfully!"
                    );
                    return true;
                }
            }

        } catch (SQLException e) {
            System.out.println(
                    "Package registration failed: " + e.getMessage()
            );
        }

        return false;
    }

    public List<PackageItem> getAllPackages() {
        List<PackageItem> packages = new ArrayList<>();

        String sql = """
                SELECT Package_ID, Recipient_ID, Tracking_Number,
                       Sender_Name, Package_Description,
                       Date_Received, Package_Status
                FROM Packages
                ORDER BY Package_ID
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                packages.add(new PackageItem(
                        rs.getInt("Package_ID"),
                        rs.getInt("Recipient_ID"),
                        rs.getString("Tracking_Number"),
                        rs.getString("Sender_Name"),
                        rs.getString("Package_Description"),
                        rs.getString("Date_Received"),
                        rs.getString("Package_Status")
                ));
            }

        } catch (SQLException e) {
            System.out.println(
                    "Unable to load packages: " + e.getMessage()
            );
        }

        return packages;
    }
}