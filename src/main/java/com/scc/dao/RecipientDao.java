
package com.scc.dao;

import com.scc.model.Recipient;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class RecipientDao {

    public boolean registerRecipient(
            int departmentId,
            String name,
            String type,
            String identificationNumber,
            String contactNumber
    ) {
        if (departmentId <= 0
                || name == null || name.isBlank()
                || type == null || type.isBlank()
                || identificationNumber == null
                || identificationNumber.isBlank()) {
            System.out.println("Required recipient fields are missing.");
            return false;
        }

        String checkSql = """
                SELECT 1
                FROM Department_Offices
                WHERE Department_ID = ?
                  AND Department_Status = 'ACTIVE'
                """;

        String insertSql = """
                INSERT INTO Recipients
                    (Department_ID, Recipient_Name, Recipient_Type,
                     Identification_Number, Contact_Number, Recipient_Status)
                VALUES (?, ?, ?, ?, ?, 'ACTIVE')
                """;

        try (Connection connection = DatabaseConnection.getConnection()) {

            try (PreparedStatement check =
                         connection.prepareStatement(checkSql)) {
                check.setInt(1, departmentId);

                try (ResultSet result = check.executeQuery()) {
                    if (!result.next()) {
                        System.out.println(
                                "Active department or office not found."
                        );
                        return false;
                    }
                }
            }

            try (PreparedStatement statement =
                         connection.prepareStatement(insertSql)) {

                statement.setInt(1, departmentId);
                statement.setString(2, name.trim());
                statement.setString(
                        3, type.trim().toUpperCase(Locale.ROOT)
                );
                statement.setString(4, identificationNumber.trim());

                if (contactNumber == null || contactNumber.isBlank()) {
                    statement.setNull(5, Types.VARCHAR);
                } else {
                    statement.setString(5, contactNumber.trim());
                }

                if (statement.executeUpdate() == 1) {
                    System.out.println("Recipient registered successfully.");
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

        try (Connection connection = DatabaseConnection.getConnection();
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(sql)) {

            while (result.next()) {
                recipients.add(new Recipient(
                        result.getInt("Recipient_ID"),
                        result.getInt("Department_ID"),
                        result.getString("Recipient_Name"),
                        result.getString("Recipient_Type"),
                        result.getString("Identification_Number"),
                        result.getString("Contact_Number"),
                        result.getString("Recipient_Status")
                ));
            }

        } catch (SQLException e) {
            System.out.println(
                    "Unable to load recipients: " + e.getMessage()
            );
        }

        return recipients;
    }

    public boolean deactivate(int recipientId) {
        if (recipientId <= 0) {
            System.out.println("Invalid recipient ID.");
            return false;
        }

        String sql = """
                UPDATE Recipients
                SET Recipient_Status = 'INACTIVE'
                WHERE Recipient_ID = ?
                  AND Recipient_Status = 'ACTIVE'
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, recipientId);

            if (statement.executeUpdate() == 1) {
                System.out.println("Recipient deactivated successfully.");
                return true;
            }

            System.out.println(
                    "No active recipient found with that ID."
            );

        } catch (SQLException e) {
            System.out.println(
                    "Recipient deactivation failed: " + e.getMessage()
            );
        }

        return false;
    }
}
