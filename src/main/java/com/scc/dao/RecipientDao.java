
package com.scc.dao;

import com.scc.model.Recipient;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class RecipientDao {

    public boolean registerRecipient(
            int departmentId,
            String recipientName,
            String recipientType,
            String identificationNumber,
            String contactNumber
    ) {
        String sql = """
                INSERT INTO Recipients
                (Department_ID, Recipient_Name, Recipient_Type,
                 Identification_Number, Contact_Number, Recipient_Status)
                VALUES (?, ?, ?, ?, ?, 'ACTIVE')
                """;

        try (Connection conn = DatabaseConnection.getConnection()) {

            // Make sure the selected department exists.
            String checkSql = """
                    SELECT 1 FROM Department_Offices
                    WHERE Department_ID = ?
                    """;

            try (PreparedStatement check = conn.prepareStatement(checkSql)) {
                check.setInt(1, departmentId);

                try (ResultSet rs = check.executeQuery()) {
                    if (!rs.next()) {
                        System.out.println("Registration failed: Department ID not found.");
                        return false;
                    }
                }
            }

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, departmentId);
                pstmt.setString(2, recipientName);
                pstmt.setString(3, recipientType.toUpperCase());
                pstmt.setString(4, identificationNumber);

                if (contactNumber == null || contactNumber.isBlank()) {
                    pstmt.setNull(5, java.sql.Types.VARCHAR);
                } else {
                    pstmt.setString(5, contactNumber);
                }

                int rowsAffected = pstmt.executeUpdate();

                if (rowsAffected == 1) {
                    System.out.println("Recipient registered successfully!");
                    return true;
                }
            }

        } catch (SQLException e) {
            System.out.println(
                    "Recipient registration failed: " + e.getMessage()
            );
        }

        return false;
    }

    public List<Recipient> getAllRecipients() {
        List<Recipient> recipients = new ArrayList<>();

        String sql = """
                SELECT Recipient_ID, Department_ID, Recipient_Name,
                       Recipient_Type, Identification_Number,
                       Contact_Number, Recipient_Status
                FROM Recipients
                ORDER BY Recipient_ID
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                recipients.add(new Recipient(
                        rs.getInt("Recipient_ID"),
                        rs.getInt("Department_ID"),
                        rs.getString("Recipient_Name"),
                        rs.getString("Recipient_Type"),
                        rs.getString("Identification_Number"),
                        rs.getString("Contact_Number"),
                        rs.getString("Recipient_Status")
                ));
            }

        } catch (SQLException e) {
            System.out.println("Unable to load recipients: " + e.getMessage());
        }

        return recipients;
    }
}