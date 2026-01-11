package com.example.ignite.solutions.lab10;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.cache.query.SqlFieldsQuery;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.ClientConnectorConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Lab 10 Exercise: JDBC Thin Driver Connection
 *
 * Demonstrates:
 * - JDBC thin driver connection
 * - DDL operations (CREATE TABLE)
 * - DML operations (INSERT, UPDATE, DELETE)
 * - SQL queries (SELECT)
 * - Prepared statements
 * - Batch operations
 * - Transaction support
 */
public class Lab10JDBC {

    private static final String JDBC_URL = "jdbc:ignite:thin://127.0.0.1:10800";
    private static volatile boolean serverRunning = true;

    public static void main(String[] args) {
        System.out.println("=== JDBC Thin Driver Lab ===\n");

        // Start server node first
        Thread serverThread = startServerNode();

        // Wait for server to be ready
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        try {
            // Demonstrate JDBC features
            demonstrateBasicConnection();
            demonstrateDDLOperations();
            demonstrateDMLOperations();
            demonstratePreparedStatements();
            demonstrateBatchOperations();
            demonstrateTransactions();
            demonstrateMetadata();

            System.out.println("\n=== JDBC Thin Driver Summary ===");
            System.out.println("Benefits:");
            System.out.println("  - Standard JDBC interface");
            System.out.println("  - Compatible with BI tools (Tableau, DBeaver, etc.)");
            System.out.println("  - No special client libraries needed");
            System.out.println("  - Familiar SQL syntax");
            System.out.println("  - Lightweight thin client protocol");
            System.out.println("");
            System.out.println("Connection URL formats:");
            System.out.println("  - jdbc:ignite:thin://host:port");
            System.out.println("  - jdbc:ignite:thin://host1:port1,host2:port2 (multiple nodes)");
            System.out.println("  - jdbc:ignite:thin://host:port?schema=MY_SCHEMA");
            System.out.println("  - jdbc:ignite:thin://host:port?user=admin&password=secret");

            System.out.println("\nPress Enter to stop server...");
            System.in.read();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            serverRunning = false;
            serverThread.interrupt();
        }
    }

    private static Thread startServerNode() {
        Thread serverThread = new Thread(() -> {
            IgniteConfiguration cfg = new IgniteConfiguration();
            cfg.setIgniteInstanceName("jdbc-server");

            // Configure client connector for JDBC connections
            ClientConnectorConfiguration clientCfg = new ClientConnectorConfiguration();
            clientCfg.setPort(10800);
            clientCfg.setPortRange(10);
            clientCfg.setThreadPoolSize(8);
            cfg.setClientConnectorConfiguration(clientCfg);

            try (Ignite ignite = Ignition.start(cfg)) {
                System.out.println("Server node started, JDBC port: 10800\n");

                // Keep server running
                while (serverRunning) {
                    Thread.sleep(1000);
                }
            } catch (InterruptedException e) {
                // Normal shutdown
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        serverThread.start();
        return serverThread;
    }

    private static void demonstrateBasicConnection() {
        System.out.println("=== 1. Basic JDBC Connection ===\n");

        try (Connection conn = DriverManager.getConnection(JDBC_URL)) {
            System.out.println("Connected to Ignite via JDBC thin driver");
            System.out.println("Connection URL: " + JDBC_URL);
            System.out.println("Auto-commit: " + conn.getAutoCommit());
            System.out.println("Connection valid: " + conn.isValid(5));

            // Get database info
            DatabaseMetaData metaData = conn.getMetaData();
            System.out.println("Database product: " + metaData.getDatabaseProductName());
            System.out.println("Database version: " + metaData.getDatabaseProductVersion());
            System.out.println("Driver name: " + metaData.getDriverName());
            System.out.println("Driver version: " + metaData.getDriverVersion());
            System.out.println();

        } catch (SQLException e) {
            System.err.println("JDBC error: " + e.getMessage());
        }
    }

    private static void demonstrateDDLOperations() {
        System.out.println("=== 2. DDL Operations (CREATE TABLE) ===\n");

        try (Connection conn = DriverManager.getConnection(JDBC_URL);
             Statement stmt = conn.createStatement()) {

            // Drop table if exists
            try {
                stmt.executeUpdate("DROP TABLE IF EXISTS Person");
                System.out.println("Dropped existing Person table");
            } catch (SQLException e) {
                // Table might not exist
            }

            // Create table
            String createTableSQL =
                "CREATE TABLE Person (" +
                "  id INT PRIMARY KEY," +
                "  name VARCHAR(100)," +
                "  age INT," +
                "  city VARCHAR(50)," +
                "  salary DECIMAL(10,2)" +
                ") WITH \"template=replicated\"";

            stmt.executeUpdate(createTableSQL);
            System.out.println("Created Person table");

            // Create index
            String createIndexSQL = "CREATE INDEX idx_person_city ON Person(city)";
            stmt.executeUpdate(createIndexSQL);
            System.out.println("Created index on city column");

            // Create another table for joins
            try {
                stmt.executeUpdate("DROP TABLE IF EXISTS Department");
            } catch (SQLException e) {
                // Table might not exist
            }

            String createDeptSQL =
                "CREATE TABLE Department (" +
                "  id INT PRIMARY KEY," +
                "  name VARCHAR(100)," +
                "  location VARCHAR(50)" +
                ") WITH \"template=replicated\"";

            stmt.executeUpdate(createDeptSQL);
            System.out.println("Created Department table");
            System.out.println();

        } catch (SQLException e) {
            System.err.println("DDL error: " + e.getMessage());
        }
    }

    private static void demonstrateDMLOperations() {
        System.out.println("=== 3. DML Operations (INSERT, UPDATE, DELETE) ===\n");

        try (Connection conn = DriverManager.getConnection(JDBC_URL);
             Statement stmt = conn.createStatement()) {

            // INSERT operations
            System.out.println("INSERT operations:");
            int inserted = 0;
            inserted += stmt.executeUpdate(
                "INSERT INTO Person (id, name, age, city, salary) VALUES (1, 'John Doe', 30, 'New York', 75000.00)");
            inserted += stmt.executeUpdate(
                "INSERT INTO Person (id, name, age, city, salary) VALUES (2, 'Jane Smith', 25, 'Los Angeles', 65000.00)");
            inserted += stmt.executeUpdate(
                "INSERT INTO Person (id, name, age, city, salary) VALUES (3, 'Bob Johnson', 35, 'Chicago', 85000.00)");
            inserted += stmt.executeUpdate(
                "INSERT INTO Person (id, name, age, city, salary) VALUES (4, 'Alice Brown', 28, 'New York', 70000.00)");
            inserted += stmt.executeUpdate(
                "INSERT INTO Person (id, name, age, city, salary) VALUES (5, 'Charlie Wilson', 42, 'Boston', 95000.00)");
            System.out.println("  Inserted " + inserted + " rows");

            // INSERT into Department
            stmt.executeUpdate("INSERT INTO Department (id, name, location) VALUES (1, 'Engineering', 'New York')");
            stmt.executeUpdate("INSERT INTO Department (id, name, location) VALUES (2, 'Sales', 'Los Angeles')");
            stmt.executeUpdate("INSERT INTO Department (id, name, location) VALUES (3, 'HR', 'Chicago')");
            System.out.println("  Inserted 3 departments");

            // SELECT to verify
            System.out.println("\nSELECT * FROM Person:");
            ResultSet rs = stmt.executeQuery("SELECT * FROM Person ORDER BY id");
            printResultSet(rs);

            // UPDATE operation
            System.out.println("\nUPDATE operation:");
            int updated = stmt.executeUpdate(
                "UPDATE Person SET salary = salary * 1.10 WHERE city = 'New York'");
            System.out.println("  Updated " + updated + " rows (10% raise for New York employees)");

            // Verify update
            rs = stmt.executeQuery("SELECT name, city, salary FROM Person WHERE city = 'New York'");
            printResultSet(rs);

            // DELETE operation
            System.out.println("\nDELETE operation:");
            int deleted = stmt.executeUpdate("DELETE FROM Person WHERE id = 5");
            System.out.println("  Deleted " + deleted + " row(s)");

            // Count remaining
            rs = stmt.executeQuery("SELECT COUNT(*) as total FROM Person");
            if (rs.next()) {
                System.out.println("  Remaining rows: " + rs.getInt("total"));
            }
            System.out.println();

        } catch (SQLException e) {
            System.err.println("DML error: " + e.getMessage());
        }
    }

    private static void demonstratePreparedStatements() {
        System.out.println("=== 4. Prepared Statements ===\n");

        String insertSQL = "INSERT INTO Person (id, name, age, city, salary) VALUES (?, ?, ?, ?, ?)";
        String selectSQL = "SELECT * FROM Person WHERE city = ? AND age > ?";

        try (Connection conn = DriverManager.getConnection(JDBC_URL)) {

            // Prepared INSERT
            System.out.println("Prepared INSERT:");
            try (PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {
                // Insert multiple rows with different parameters
                Object[][] data = {
                    {6, "David Lee", 29, "Seattle", 72000.00},
                    {7, "Emma Davis", 31, "Seattle", 78000.00},
                    {8, "Frank Miller", 27, "Denver", 68000.00}
                };

                for (Object[] row : data) {
                    pstmt.setInt(1, (Integer) row[0]);
                    pstmt.setString(2, (String) row[1]);
                    pstmt.setInt(3, (Integer) row[2]);
                    pstmt.setString(4, (String) row[3]);
                    pstmt.setDouble(5, (Double) row[4]);
                    pstmt.executeUpdate();
                }
                System.out.println("  Inserted 3 rows using prepared statement");
            }

            // Prepared SELECT
            System.out.println("\nPrepared SELECT (city='Seattle', age>25):");
            try (PreparedStatement pstmt = conn.prepareStatement(selectSQL)) {
                pstmt.setString(1, "Seattle");
                pstmt.setInt(2, 25);

                ResultSet rs = pstmt.executeQuery();
                printResultSet(rs);
            }

            // Demonstrate parameter reuse
            System.out.println("Prepared SELECT (city='New York', age>20):");
            try (PreparedStatement pstmt = conn.prepareStatement(selectSQL)) {
                pstmt.setString(1, "New York");
                pstmt.setInt(2, 20);

                ResultSet rs = pstmt.executeQuery();
                printResultSet(rs);
            }
            System.out.println();

        } catch (SQLException e) {
            System.err.println("Prepared statement error: " + e.getMessage());
        }
    }

    private static void demonstrateBatchOperations() {
        System.out.println("=== 5. Batch Operations ===\n");

        try (Connection conn = DriverManager.getConnection(JDBC_URL)) {
            conn.setAutoCommit(false);

            String insertSQL = "INSERT INTO Person (id, name, age, city, salary) VALUES (?, ?, ?, ?, ?)";

            try (PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {
                long startTime = System.currentTimeMillis();

                // Add batch of inserts
                for (int i = 100; i < 200; i++) {
                    pstmt.setInt(1, i);
                    pstmt.setString(2, "BatchUser-" + i);
                    pstmt.setInt(3, 20 + (i % 30));
                    pstmt.setString(4, "City-" + (i % 5));
                    pstmt.setDouble(5, 50000.0 + (i * 100));
                    pstmt.addBatch();
                }

                // Execute batch
                int[] results = pstmt.executeBatch();
                conn.commit();

                long duration = System.currentTimeMillis() - startTime;
                System.out.println("Batch insert results:");
                System.out.println("  Total statements: " + results.length);
                System.out.println("  Duration: " + duration + "ms");
                System.out.println("  Throughput: " + (results.length * 1000 / Math.max(duration, 1)) + " inserts/sec");

                // Verify count
                try (Statement stmt = conn.createStatement()) {
                    ResultSet rs = stmt.executeQuery("SELECT COUNT(*) as total FROM Person");
                    if (rs.next()) {
                        System.out.println("  Total rows in Person table: " + rs.getInt("total"));
                    }
                }
            }
            System.out.println();

        } catch (SQLException e) {
            System.err.println("Batch operation error: " + e.getMessage());
        }
    }

    private static void demonstrateTransactions() {
        System.out.println("=== 6. Transaction Support ===\n");

        try (Connection conn = DriverManager.getConnection(JDBC_URL)) {
            conn.setAutoCommit(false);
            System.out.println("Auto-commit disabled for transaction demo");

            try (Statement stmt = conn.createStatement()) {
                // Start transaction
                System.out.println("\nTransaction 1: Successful commit");
                stmt.executeUpdate(
                    "INSERT INTO Person (id, name, age, city, salary) VALUES (200, 'Trans User 1', 35, 'Miami', 80000)");
                stmt.executeUpdate(
                    "INSERT INTO Person (id, name, age, city, salary) VALUES (201, 'Trans User 2', 40, 'Miami', 90000)");
                conn.commit();
                System.out.println("  Transaction committed successfully");

                // Verify
                ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM Person WHERE city = 'Miami'");
                if (rs.next()) {
                    System.out.println("  Miami employees: " + rs.getInt(1));
                }

                // Demonstrate rollback
                System.out.println("\nTransaction 2: Rollback demonstration");
                stmt.executeUpdate(
                    "INSERT INTO Person (id, name, age, city, salary) VALUES (202, 'Rollback User', 30, 'Miami', 70000)");
                System.out.println("  Inserted row (not yet committed)");

                // Rollback
                conn.rollback();
                System.out.println("  Transaction rolled back");

                // Verify rollback
                rs = stmt.executeQuery("SELECT COUNT(*) FROM Person WHERE city = 'Miami'");
                if (rs.next()) {
                    System.out.println("  Miami employees after rollback: " + rs.getInt(1));
                }
            }
            System.out.println();

        } catch (SQLException e) {
            System.err.println("Transaction error: " + e.getMessage());
        }
    }

    private static void demonstrateMetadata() {
        System.out.println("=== 7. Database Metadata ===\n");

        try (Connection conn = DriverManager.getConnection(JDBC_URL)) {
            DatabaseMetaData metaData = conn.getMetaData();

            // List tables
            System.out.println("Tables in PUBLIC schema:");
            ResultSet tables = metaData.getTables(null, "PUBLIC", "%", new String[]{"TABLE"});
            while (tables.next()) {
                System.out.println("  - " + tables.getString("TABLE_NAME"));
            }

            // List columns for Person table
            System.out.println("\nColumns in Person table:");
            ResultSet columns = metaData.getColumns(null, "PUBLIC", "PERSON", "%");
            while (columns.next()) {
                System.out.printf("  %-15s %-15s %s%n",
                    columns.getString("COLUMN_NAME"),
                    columns.getString("TYPE_NAME"),
                    columns.getInt("NULLABLE") == 0 ? "NOT NULL" : "NULLABLE");
            }

            // List indexes
            System.out.println("\nIndexes on Person table:");
            ResultSet indexes = metaData.getIndexInfo(null, "PUBLIC", "PERSON", false, false);
            while (indexes.next()) {
                String indexName = indexes.getString("INDEX_NAME");
                String columnName = indexes.getString("COLUMN_NAME");
                if (indexName != null && columnName != null) {
                    System.out.println("  - " + indexName + " on column " + columnName);
                }
            }

            // Aggregate query example
            System.out.println("\nAggregate query example:");
            try (Statement stmt = conn.createStatement()) {
                ResultSet rs = stmt.executeQuery(
                    "SELECT city, COUNT(*) as cnt, AVG(salary) as avg_salary, MAX(salary) as max_salary " +
                    "FROM Person GROUP BY city ORDER BY cnt DESC");
                System.out.println("  City            | Count | Avg Salary | Max Salary");
                System.out.println("  ----------------|-------|------------|------------");
                while (rs.next()) {
                    System.out.printf("  %-15s | %5d | %10.2f | %10.2f%n",
                        rs.getString("city"),
                        rs.getInt("cnt"),
                        rs.getDouble("avg_salary"),
                        rs.getDouble("max_salary"));
                }
            }

        } catch (SQLException e) {
            System.err.println("Metadata error: " + e.getMessage());
        }
    }

    private static void printResultSet(ResultSet rs) throws SQLException {
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();

        // Print header
        StringBuilder header = new StringBuilder("  ");
        StringBuilder separator = new StringBuilder("  ");
        for (int i = 1; i <= columnCount; i++) {
            String colName = metaData.getColumnName(i);
            header.append(String.format("%-15s", colName));
            separator.append("---------------");
        }
        System.out.println(header);
        System.out.println(separator);

        // Print rows
        int rowCount = 0;
        while (rs.next()) {
            StringBuilder row = new StringBuilder("  ");
            for (int i = 1; i <= columnCount; i++) {
                Object value = rs.getObject(i);
                row.append(String.format("%-15s", value != null ? value.toString() : "NULL"));
            }
            System.out.println(row);
            rowCount++;
        }
        System.out.println("  (" + rowCount + " rows)");
    }
}
