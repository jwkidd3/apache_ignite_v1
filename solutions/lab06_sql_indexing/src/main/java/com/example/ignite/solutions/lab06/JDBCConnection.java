package com.example.ignite.solutions.lab06;

import java.sql.*;

/**
 * Lab 6 Exercise 3: JDBC Driver Usage
 *
 * This exercise demonstrates connecting to Ignite
 * using the JDBC thin driver.
 *
 * Note: Requires a running Ignite node with thin client enabled.
 * Start StartIgniteNode.java first in a separate process.
 */
public class JDBCConnection {

    private static final String JDBC_URL = "jdbc:ignite:thin://127.0.0.1/";

    public static void main(String[] args) {
        System.out.println("=== JDBC Driver Lab ===\n");

        System.out.println("Connecting to Ignite via JDBC...");
        System.out.println("URL: " + JDBC_URL + "\n");

        try {
            // Register JDBC driver
            Class.forName("org.apache.ignite.IgniteJdbcThinDriver");

            // Establish connection
            try (Connection conn = DriverManager.getConnection(JDBC_URL)) {
                System.out.println("Connected successfully!\n");

                // Create table
                System.out.println("=== Creating Table ===");
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute("CREATE TABLE IF NOT EXISTS Employee (" +
                        "id INT PRIMARY KEY, " +
                        "name VARCHAR, " +
                        "department VARCHAR, " +
                        "salary DECIMAL) " +
                        "WITH \"template=partitioned,backups=1\"");
                    System.out.println("Table 'Employee' created\n");
                }

                // Insert data using PreparedStatement
                System.out.println("=== Inserting Data ===");
                String insertSQL = "MERGE INTO Employee (id, name, department, salary) " +
                    "VALUES (?, ?, ?, ?)";

                try (PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {
                    // Insert multiple employees
                    Object[][] employees = {
                        {1, "Alice Johnson", "Engineering", 95000},
                        {2, "Bob Smith", "Marketing", 75000},
                        {3, "Charlie Brown", "Engineering", 88000},
                        {4, "Diana Prince", "Sales", 82000},
                        {5, "Eve Davis", "Engineering", 92000}
                    };

                    for (Object[] emp : employees) {
                        pstmt.setInt(1, (Integer) emp[0]);
                        pstmt.setString(2, (String) emp[1]);
                        pstmt.setString(3, (String) emp[2]);
                        pstmt.setInt(4, (Integer) emp[3]);
                        pstmt.executeUpdate();
                    }

                    System.out.println("Inserted " + employees.length + " employees\n");
                }

                // Query data using ResultSet
                System.out.println("=== Querying Data ===");
                String querySQL = "SELECT name, department, salary FROM Employee " +
                    "WHERE salary > ? ORDER BY salary DESC";

                try (PreparedStatement pstmt = conn.prepareStatement(querySQL)) {
                    pstmt.setInt(1, 80000);

                    try (ResultSet rs = pstmt.executeQuery()) {
                        System.out.println("Employees with salary > 80000:");
                        System.out.println("Name              | Department    | Salary");
                        System.out.println("------------------+---------------+--------");

                        while (rs.next()) {
                            String name = rs.getString("name");
                            String dept = rs.getString("department");
                            int salary = rs.getInt("salary");
                            System.out.printf("%-17s | %-13s | $%d%n",
                                name, dept, salary);
                        }
                    }
                }

                // Aggregate query
                System.out.println("\n=== Aggregate Query ===");
                String aggSQL = "SELECT department, COUNT(*) as count, AVG(salary) as avg_salary " +
                    "FROM Employee GROUP BY department";

                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(aggSQL)) {

                    System.out.println("Department Statistics:");
                    System.out.println("Department    | Count | Avg Salary");
                    System.out.println("--------------+-------+-----------");

                    while (rs.next()) {
                        String dept = rs.getString("department");
                        int count = rs.getInt("count");
                        double avgSalary = rs.getDouble("avg_salary");
                        System.out.printf("%-13s | %-5d | $%.2f%n",
                            dept, count, avgSalary);
                    }
                }

                // Update data
                System.out.println("\n=== Updating Data ===");
                String updateSQL = "UPDATE Employee SET salary = salary * 1.05 " +
                    "WHERE department = ?";

                try (PreparedStatement pstmt = conn.prepareStatement(updateSQL)) {
                    pstmt.setString(1, "Engineering");
                    int updated = pstmt.executeUpdate();
                    System.out.println("Updated " + updated + " engineering salaries (+5%)");
                }

                // Metadata
                System.out.println("\n=== Database Metadata ===");
                DatabaseMetaData metadata = conn.getMetaData();
                System.out.println("Driver: " + metadata.getDriverName());
                System.out.println("Version: " + metadata.getDriverVersion());
                System.out.println("Product: " + metadata.getDatabaseProductName());

            }

        } catch (ClassNotFoundException e) {
            System.err.println("JDBC Driver not found!");
            System.err.println("Make sure ignite-core is in the classpath");
            e.printStackTrace();
        } catch (SQLException e) {
            System.err.println("SQL Error: " + e.getMessage());
            System.err.println("\nMake sure an Ignite node is running.");
            System.err.println("Run StartIgniteNode.java first.");
            e.printStackTrace();
        }
    }
}
