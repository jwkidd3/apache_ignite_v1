# Lab 6: SQL and Indexing

## Duration: 60 minutes

## Objectives
- Execute SQL queries on Ignite caches (DDL, DML, DQL)
- Create and manage indexes for query optimization
- Use JDBC driver to connect to Ignite
- Optimize query performance
- Understand distributed joins and their implications

## Prerequisites
- Completed Labs 1-5
- Basic SQL knowledge
- Understanding of database indexes

## Part 1: SQL Support in Ignite (15 minutes)

### Exercise 1: Create Tables and Basic Queries

Create `Lab06BasicSQL.java`:

```java
package com.example.ignite;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.cache.QueryEntity;
import org.apache.ignite.cache.QueryIndex;
import org.apache.ignite.cache.query.SqlFieldsQuery;
import org.apache.ignite.configuration.CacheConfiguration;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

public class Lab06BasicSQL {

    public static void main(String[] args) {
        try (Ignite ignite = Ignition.start()) {
            System.out.println("=== SQL Support Lab ===\n");

            // Create cache with SQL schema
            CacheConfiguration<Long, Object> personCfg = new CacheConfiguration<>("Person");

            // Define query entity (table schema)
            QueryEntity personEntity = new QueryEntity(Long.class, Object.class);
            personEntity.setTableName("Person");

            // Define fields
            LinkedHashMap<String, String> fields = new LinkedHashMap<>();
            fields.put("id", "java.lang.Long");
            fields.put("name", "java.lang.String");
            fields.put("age", "java.lang.Integer");
            fields.put("city", "java.lang.String");
            fields.put("salary", "java.lang.Double");

            personEntity.setFields(fields);
            personEntity.setKeyFieldName("id");

            // Add indexes
            personEntity.setIndexes(Arrays.asList(
                new QueryIndex("name"),
                new QueryIndex("city")
            ));

            personCfg.setQueryEntities(Arrays.asList(personEntity));
            personCfg.setSqlSchema("PUBLIC");

            IgniteCache<Long, Object> cache = ignite.getOrCreateCache(personCfg);

            System.out.println("=== DDL: Table Created ===");
            System.out.println("Table: Person");
            System.out.println("Fields: id, name, age, city, salary");
            System.out.println("Indexes: name, city\n");

            // DML: INSERT data
            System.out.println("=== DML: Inserting Data ===");
            cache.query(new SqlFieldsQuery(
                "INSERT INTO Person (id, name, age, city, salary) VALUES (?, ?, ?, ?, ?)")
                .setArgs(1L, "John Doe", 30, "New York", 75000.0)).getAll();

            cache.query(new SqlFieldsQuery(
                "INSERT INTO Person (id, name, age, city, salary) VALUES (?, ?, ?, ?, ?)")
                .setArgs(2L, "Jane Smith", 28, "Los Angeles", 82000.0)).getAll();

            cache.query(new SqlFieldsQuery(
                "INSERT INTO Person (id, name, age, city, salary) VALUES (?, ?, ?, ?, ?)")
                .setArgs(3L, "Bob Johnson", 35, "New York", 95000.0)).getAll();

            cache.query(new SqlFieldsQuery(
                "INSERT INTO Person (id, name, age, city, salary) VALUES (?, ?, ?, ?, ?)")
                .setArgs(4L, "Alice Williams", 32, "Chicago", 88000.0)).getAll();

            cache.query(new SqlFieldsQuery(
                "INSERT INTO Person (id, name, age, city, salary) VALUES (?, ?, ?, ?, ?)")
                .setArgs(5L, "Charlie Brown", 29, "New York", 72000.0)).getAll();

            System.out.println("Inserted 5 records\n");

            // DQL: SELECT queries
            System.out.println("=== DQL: SELECT Queries ===");

            // Simple select
            System.out.println("1. All persons:");
            SqlFieldsQuery query1 = new SqlFieldsQuery("SELECT name, age, city FROM Person");
            List<List<?>> results = cache.query(query1).getAll();
            results.forEach(row -> System.out.println("   " + row));

            // WHERE clause
            System.out.println("\n2. Persons in New York:");
            SqlFieldsQuery query2 = new SqlFieldsQuery(
                "SELECT name, age, salary FROM Person WHERE city = ?")
                .setArgs("New York");
            cache.query(query2).getAll().forEach(row -> System.out.println("   " + row));

            // Aggregation
            System.out.println("\n3. Average salary by city:");
            SqlFieldsQuery query3 = new SqlFieldsQuery(
                "SELECT city, AVG(salary) as avg_salary FROM Person GROUP BY city");
            cache.query(query3).getAll().forEach(row ->
                System.out.println("   " + row.get(0) + ": $" +
                    String.format("%.2f", row.get(1))));

            // ORDER BY
            System.out.println("\n4. Top earners:");
            SqlFieldsQuery query4 = new SqlFieldsQuery(
                "SELECT name, salary FROM Person ORDER BY salary DESC LIMIT 3");
            cache.query(query4).getAll().forEach(row -> System.out.println("   " + row));

            // DML: UPDATE
            System.out.println("\n=== DML: UPDATE Data ===");
            cache.query(new SqlFieldsQuery(
                "UPDATE Person SET salary = salary * 1.1 WHERE city = ?")
                .setArgs("New York")).getAll();
            System.out.println("Updated salaries in New York (+10%)");

            // DML: DELETE
            System.out.println("\n=== DML: DELETE Data ===");
            cache.query(new SqlFieldsQuery(
                "DELETE FROM Person WHERE age < ?")
                .setArgs(29)).getAll();
            System.out.println("Deleted persons younger than 29");

            // Verify
            System.out.println("\nFinal count: " + cache.size() + " records");

            System.out.println("\nPress Enter to exit...");
            System.in.read();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

## Part 2: Advanced Indexing (15 minutes)

### Exercise 2: Create and Test Indexes

Create `Lab06Indexing.java`:

```java
package com.example.ignite;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.cache.QueryEntity;
import org.apache.ignite.cache.QueryIndex;
import org.apache.ignite.cache.QueryIndexType;
import org.apache.ignite.cache.query.SqlFieldsQuery;
import org.apache.ignite.configuration.CacheConfiguration;

import java.util.Arrays;
import java.util.LinkedHashMap;

public class Lab06Indexing {

    public static void main(String[] args) {
        try (Ignite ignite = Ignition.start()) {
            System.out.println("=== Indexing Lab ===\n");

            // Create cache with different index types
            CacheConfiguration<Long, Object> cfg = new CacheConfiguration<>("Product");

            QueryEntity entity = new QueryEntity(Long.class, Object.class);
            entity.setTableName("Product");

            LinkedHashMap<String, String> fields = new LinkedHashMap<>();
            fields.put("id", "java.lang.Long");
            fields.put("name", "java.lang.String");
            fields.put("category", "java.lang.String");
            fields.put("price", "java.lang.Double");
            fields.put("stock", "java.lang.Integer");
            fields.put("description", "java.lang.String");

            entity.setFields(fields);
            entity.setKeyFieldName("id");

            // Create different types of indexes
            QueryIndex nameIdx = new QueryIndex("name", QueryIndexType.SORTED);
            QueryIndex categoryIdx = new QueryIndex("category", QueryIndexType.SORTED);
            QueryIndex priceIdx = new QueryIndex("price", QueryIndexType.SORTED);

            // Composite index on category and price
            QueryIndex compositeIdx = new QueryIndex(
                Arrays.asList("category", "price"), QueryIndexType.SORTED);
            compositeIdx.setName("category_price_idx");

            // Full-text index on description
            QueryIndex textIdx = new QueryIndex("description", QueryIndexType.FULLTEXT);

            entity.setIndexes(Arrays.asList(nameIdx, categoryIdx, priceIdx,
                compositeIdx, textIdx));

            cfg.setQueryEntities(Arrays.asList(entity));
            cfg.setSqlSchema("PUBLIC");

            IgniteCache<Long, Object> cache = ignite.getOrCreateCache(cfg);

            // Insert test data
            System.out.println("=== Inserting Test Data ===");
            insertProducts(cache);

            // Test index performance
            System.out.println("\n=== Testing Indexed Queries ===");

            // Query using single index
            long startTime = System.currentTimeMillis();
            SqlFieldsQuery query1 = new SqlFieldsQuery(
                "SELECT name, price FROM Product WHERE category = ?")
                .setArgs("Electronics");
            cache.query(query1).getAll();
            long time1 = System.currentTimeMillis() - startTime;
            System.out.println("1. Query with category index: " + time1 + " ms");

            // Query using composite index
            startTime = System.currentTimeMillis();
            SqlFieldsQuery query2 = new SqlFieldsQuery(
                "SELECT name, price FROM Product WHERE category = ? AND price < ?")
                .setArgs("Electronics", 500.0);
            cache.query(query2).getAll();
            long time2 = System.currentTimeMillis() - startTime;
            System.out.println("2. Query with composite index: " + time2 + " ms");

            // Range query with index
            startTime = System.currentTimeMillis();
            SqlFieldsQuery query3 = new SqlFieldsQuery(
                "SELECT name, price FROM Product WHERE price BETWEEN ? AND ?")
                .setArgs(100.0, 500.0);
            List<List<?>> results = cache.query(query3).getAll();
            long time3 = System.currentTimeMillis() - startTime;
            System.out.println("3. Range query with price index: " + time3 + " ms");
            System.out.println("   Found " + results.size() + " products");

            // Full-text search
            System.out.println("\n=== Full-Text Search ===");
            SqlFieldsQuery textQuery = new SqlFieldsQuery(
                "SELECT name, description FROM Product WHERE description LIKE ?")
                .setArgs("%wireless%");
            cache.query(textQuery).getAll().forEach(row ->
                System.out.println("   " + row.get(0) + ": " + row.get(1)));

            // Query execution plan
            System.out.println("\n=== Query Execution Plan ===");
            SqlFieldsQuery explainQuery = new SqlFieldsQuery(
                "EXPLAIN SELECT name FROM Product WHERE category = 'Electronics' AND price < 500");
            cache.query(explainQuery).getAll().forEach(row ->
                System.out.println("   " + row.get(0)));

            System.out.println("\nPress Enter to exit...");
            System.in.read();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void insertProducts(IgniteCache<Long, Object> cache) {
        String[] products = {
            "1, 'Laptop', 'Electronics', 1200.00, 50, 'High-performance wireless laptop'",
            "2, 'Mouse', 'Electronics', 25.00, 200, 'Wireless optical mouse'",
            "3, 'Keyboard', 'Electronics', 75.00, 150, 'Mechanical keyboard'",
            "4, 'Monitor', 'Electronics', 350.00, 80, '27-inch LED monitor'",
            "5, 'Desk', 'Furniture', 299.00, 30, 'Adjustable standing desk'",
            "6, 'Chair', 'Furniture', 199.00, 45, 'Ergonomic office chair'",
            "7, 'Lamp', 'Furniture', 45.00, 100, 'LED desk lamp'",
            "8, 'Notebook', 'Stationery', 5.00, 500, 'Spiral bound notebook'",
            "9, 'Pen Set', 'Stationery', 15.00, 300, 'Premium pen set'",
            "10, 'Tablet', 'Electronics', 599.00, 60, 'Wireless tablet with stylus'"
        };

        for (String product : products) {
            cache.query(new SqlFieldsQuery(
                "INSERT INTO Product (id, name, category, price, stock, description) " +
                "VALUES (" + product + ")")).getAll();
        }

        System.out.println("Inserted " + products.length + " products");
    }
}
```

## Part 3: JDBC Driver Usage (15 minutes)

### Exercise 3: Connect Using JDBC

Create `Lab06JDBC.java`:

```java
package com.example.ignite;

import java.sql.*;

public class Lab06JDBC {

    private static final String JDBC_URL = "jdbc:ignite:thin://127.0.0.1/";

    public static void main(String[] args) {
        System.out.println("=== JDBC Driver Lab ===\n");

        // Start Ignite node first (in separate process or use existing cluster)
        System.out.println("Connecting to Ignite via JDBC...\n");

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
                        "salary DECIMAL)");
                    System.out.println("Table 'Employee' created\n");
                }

                // Insert data using PreparedStatement
                System.out.println("=== Inserting Data ===");
                String insertSQL = "INSERT INTO Employee (id, name, department, salary) " +
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
            e.printStackTrace();
        } catch (SQLException e) {
            System.err.println("SQL Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
```

**Note:** To run this, you need to start an Ignite node first:

```java
// StartIgniteNode.java
import org.apache.ignite.Ignition;

public class StartIgniteNode {
    public static void main(String[] args) throws Exception {
        Ignition.start();
        System.out.println("Ignite node started. Press Enter to stop...");
        System.in.read();
    }
}
```

## Part 4: Distributed Joins (15 minutes)

### Exercise 4: Implement and Test Distributed Joins

Create `Lab06DistributedJoins.java`:

```java
package com.example.ignite;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.cache.QueryEntity;
import org.apache.ignite.cache.query.SqlFieldsQuery;
import org.apache.ignite.configuration.CacheConfiguration;

import java.util.Arrays;
import java.util.LinkedHashMap;

public class Lab06DistributedJoins {

    public static void main(String[] args) {
        try (Ignite ignite = Ignition.start()) {
            System.out.println("=== Distributed Joins Lab ===\n");

            // Create Department cache
            IgniteCache<Integer, Object> deptCache = createDepartmentCache(ignite);

            // Create Employee cache
            IgniteCache<Integer, Object> empCache = createEmployeeCache(ignite);

            // Insert test data
            insertDepartments(deptCache);
            insertEmployees(empCache);

            // Non-distributed join (colocated data)
            System.out.println("=== Colocated Join (Fast) ===");
            long startTime = System.currentTimeMillis();

            SqlFieldsQuery colocatedJoin = new SqlFieldsQuery(
                "SELECT e.name, e.salary, d.dept_name " +
                "FROM Employee e JOIN Department d ON e.dept_id = d.id " +
                "WHERE d.dept_name = ?")
                .setArgs("Engineering");

            empCache.query(colocatedJoin).getAll().forEach(row ->
                System.out.println("   " + row));

            long colocatedTime = System.currentTimeMillis() - startTime;
            System.out.println("Time: " + colocatedTime + " ms\n");

            // Distributed join (non-colocated data)
            System.out.println("=== Distributed Join (Slower, requires all nodes) ===");
            startTime = System.currentTimeMillis();

            SqlFieldsQuery distributedJoin = new SqlFieldsQuery(
                "SELECT e.name, e.salary, d.dept_name " +
                "FROM Employee e JOIN Department d ON e.dept_id = d.id " +
                "ORDER BY e.salary DESC")
                .setDistributedJoins(true);  // Enable distributed joins

            empCache.query(distributedJoin).getAll().forEach(row ->
                System.out.println("   " + row));

            long distributedTime = System.currentTimeMillis() - startTime;
            System.out.println("Time: " + distributedTime + " ms\n");

            // Complex join with aggregation
            System.out.println("=== Join with Aggregation ===");
            SqlFieldsQuery aggJoin = new SqlFieldsQuery(
                "SELECT d.dept_name, COUNT(e.id) as emp_count, AVG(e.salary) as avg_salary " +
                "FROM Department d LEFT JOIN Employee e ON d.id = e.dept_id " +
                "GROUP BY d.dept_name " +
                "ORDER BY avg_salary DESC")
                .setDistributedJoins(true);

            empCache.query(aggJoin).getAll().forEach(row ->
                System.out.printf("   %s: %d employees, avg $%.2f%n",
                    row.get(0), row.get(1), row.get(2)));

            System.out.println("\n=== Distributed Join Considerations ===");
            System.out.println("- Enable with setDistributedJoins(true)");
            System.out.println("- Slower than colocated joins");
            System.out.println("- Requires network communication between nodes");
            System.out.println("- Use affinity keys to avoid when possible");
            System.out.println("- Necessary when data can't be colocated");

            System.out.println("\nPress Enter to exit...");
            System.in.read();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static IgniteCache<Integer, Object> createDepartmentCache(Ignite ignite) {
        CacheConfiguration<Integer, Object> cfg = new CacheConfiguration<>("Department");

        QueryEntity entity = new QueryEntity(Integer.class, Object.class);
        entity.setTableName("Department");

        LinkedHashMap<String, String> fields = new LinkedHashMap<>();
        fields.put("id", "java.lang.Integer");
        fields.put("dept_name", "java.lang.String");
        fields.put("location", "java.lang.String");

        entity.setFields(fields);
        entity.setKeyFieldName("id");

        cfg.setQueryEntities(Arrays.asList(entity));
        cfg.setSqlSchema("PUBLIC");

        return ignite.getOrCreateCache(cfg);
    }

    private static IgniteCache<Integer, Object> createEmployeeCache(Ignite ignite) {
        CacheConfiguration<Integer, Object> cfg = new CacheConfiguration<>("Employee");

        QueryEntity entity = new QueryEntity(Integer.class, Object.class);
        entity.setTableName("Employee");

        LinkedHashMap<String, String> fields = new LinkedHashMap<>();
        fields.put("id", "java.lang.Integer");
        fields.put("name", "java.lang.String");
        fields.put("dept_id", "java.lang.Integer");
        fields.put("salary", "java.lang.Double");

        entity.setFields(fields);
        entity.setKeyFieldName("id");

        cfg.setQueryEntities(Arrays.asList(entity));
        cfg.setSqlSchema("PUBLIC");

        return ignite.getOrCreateCache(cfg);
    }

    private static void insertDepartments(IgniteCache<Integer, Object> cache) {
        cache.query(new SqlFieldsQuery(
            "INSERT INTO Department (id, dept_name, location) VALUES (1, 'Engineering', 'Building A')")).getAll();
        cache.query(new SqlFieldsQuery(
            "INSERT INTO Department (id, dept_name, location) VALUES (2, 'Sales', 'Building B')")).getAll();
        cache.query(new SqlFieldsQuery(
            "INSERT INTO Department (id, dept_name, location) VALUES (3, 'Marketing', 'Building C')")).getAll();
    }

    private static void insertEmployees(IgniteCache<Integer, Object> cache) {
        String[][] employees = {
            {"1", "'Alice'", "1", "95000"},
            {"2", "'Bob'", "2", "75000"},
            {"3", "'Charlie'", "1", "88000"},
            {"4", "'Diana'", "2", "82000"},
            {"5", "'Eve'", "1", "92000"},
            {"6", "'Frank'", "3", "70000"}
        };

        for (String[] emp : employees) {
            cache.query(new SqlFieldsQuery(
                "INSERT INTO Employee (id, name, dept_id, salary) VALUES (" +
                String.join(", ", emp) + ")")).getAll();
        }
    }
}
```

## Verification Steps

### Checklist
- [ ] SQL CREATE TABLE executed successfully
- [ ] INSERT, UPDATE, DELETE operations work
- [ ] SELECT queries with WHERE, ORDER BY, GROUP BY
- [ ] Indexes created and used by query optimizer
- [ ] JDBC connection established
- [ ] Distributed joins executed correctly
- [ ] Query execution plans examined

### Performance Test

```java
// Compare queries with and without indexes
// Run EXPLAIN to see index usage
SqlFieldsQuery explain = new SqlFieldsQuery(
    "EXPLAIN SELECT * FROM Person WHERE name = 'John'");
```

## Lab Questions

1. What types of indexes does Ignite support?
2. When should you enable distributed joins?
3. How can you verify that a query is using an index?
4. What is the performance impact of full-text indexes?

## Answers

1. Ignite supports **SORTED (B-tree) indexes**, **FULLTEXT indexes** for text search, and **GEOSPATIAL indexes** for location queries. Composite indexes on multiple fields are also supported.

2. Enable distributed joins when:
   - Joining non-colocated data
   - Data can't be colocated due to size or design
   - Accept the performance penalty
   - Consider redesigning with affinity keys first

3. Use **EXPLAIN** command before your query to see the execution plan. It shows which indexes are used and the query strategy.

4. Full-text indexes use **more memory** than regular indexes, but enable efficient text search with LIKE clauses. They're optimized for read-heavy text search workloads.

## Common Issues

**Issue: Query not using index**
- Verify index exists
- Check query uses indexed fields in WHERE clause
- Use EXPLAIN to see execution plan
- Ensure statistics are up to date

**Issue: JDBC connection failed**
- Ensure Ignite node is running
- Check port 10800 is accessible
- Verify JDBC driver in classpath

**Issue: Distributed join very slow**
- Expected behavior - requires node-to-node communication
- Use affinity keys for better colocation
- Consider denormalization
- Optimize network configuration

## Next Steps

In Lab 7, you will:
- Implement distributed transactions
- Use different transaction models
- Handle isolation levels
- Deal with deadlocks

## Completion

You have completed Lab 6 when you can:
- Execute all types of SQL operations
- Create and optimize indexes
- Connect via JDBC
- Perform distributed joins
- Analyze query execution plans
- Optimize query performance
