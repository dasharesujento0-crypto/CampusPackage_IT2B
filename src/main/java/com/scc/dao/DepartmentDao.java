package com.scc.dao;

import com.scc.model.Department;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class DepartmentDao {

    public boolean registerDepartment(
            String departmentName,
            String departmentType,
            String contactNumber
    ) {
        String sql = """
                INSERT INTO Department_Offices
                (Department_Name, Department_Type,
                 Contact_Number, Department_Status)
                VALUES (?, ?, ?, 'ACTIVE')
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, departmentName);
            pstmt.setString(2, departmentType);
            pstmt.setString(3, contactNumber);

            int rowsAffected = pstmt.executeUpdate();

            if (rowsAffected == 1) {
                System.out.println("Department registered successfully!");
                return true;
            }

            System.out.println("Department was not registered.");
            return false;

        } catch (SQLException e) {
            System.out.println("Department registration failed: "
                    + e.getMessage());
            return false;
        }
    }

    public List<Department> getAllDepartments() {
        List<Department> departments = new ArrayList<>();

        String sql = """
                SELECT Department_ID, Department_Name,
                       Department_Type, Contact_Number,
                       Department_Status
                FROM Department_Offices
                ORDER BY Department_ID
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                Department department = new Department(
                        rs.getInt("Department_ID"),
                        rs.getString("Department_Name"),
                        rs.getString("Department_Type"),
                        rs.getString("Contact_Number"),
                        rs.getString("Department_Status")
                );

                departments.add(department);
            }

        } catch (SQLException e) {
            System.out.println("Unable to load departments: "
                    + e.getMessage());
        }

        return departments;
    }
}