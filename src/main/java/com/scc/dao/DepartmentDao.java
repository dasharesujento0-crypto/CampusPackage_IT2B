package com.scc.dao;

import com.scc.model.Department;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class DepartmentDao {

    public boolean registerDepartment(
            String name,
            String type,
            String contact
    ) {
        if (name == null || name.isBlank()
                || type == null || type.isBlank()
                || contact == null || contact.isBlank()) {
            System.out.println("All department fields are required.");
            return false;
        }

        String sql = """
                INSERT INTO Department_Offices
                    (Department_Name, Department_Type,
                     Contact_Number, Department_Status)
                VALUES (?, ?, ?, 'ACTIVE')
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, name.trim());
            statement.setString(2, type.trim());
            statement.setString(3, contact.trim());

            return statement.executeUpdate() == 1;

        } catch (SQLException e) {
            System.out.println(
                    "Department registration failed: " + e.getMessage()
            );
        }

        return false;
    }

    public List<Department> getAllDepartments() {
        List<Department> departments = new ArrayList<>();

        String sql = """
                SELECT Department_ID, Department_Name,
                       Department_Type, Contact_Number, Department_Status
                FROM Department_Offices
                ORDER BY Department_ID
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(sql)) {

            while (result.next()) {
                departments.add(new Department(
                        result.getInt("Department_ID"),
                        result.getString("Department_Name"),
                        result.getString("Department_Type"),
                        result.getString("Contact_Number"),
                        result.getString("Department_Status")
                ));
            }

        } catch (SQLException e) {
            System.out.println(
                    "Unable to load departments: " + e.getMessage()
            );
        }

        return departments;
    }
}