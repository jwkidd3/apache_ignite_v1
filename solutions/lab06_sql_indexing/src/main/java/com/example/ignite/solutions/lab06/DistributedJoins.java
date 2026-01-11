package com.example.ignite.solutions.lab06;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.cache.QueryEntity;
import org.apache.ignite.cache.query.SqlFieldsQuery;
import org.apache.ignite.configuration.CacheConfiguration;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Lab 6 Exercise 4: Distributed Joins
 *
 * This exercise demonstrates performing SQL joins
 * across distributed data.
 */
public class DistributedJoins {

    public static void main(String[] args) {
        try (Ignite ignite = Ignition.start()) {
            System.out.println("=== Distributed Joins Lab ===\n");

            // Create Department cache
            IgniteCache<Integer, Object> deptCache = createDepartmentCache(ignite);

            // Create Employee cache
            IgniteCache<Integer, Object> empCache = createEmployeeCache(ignite);

            // Insert test data
            System.out.println("Inserting test data...");
            insertDepartments(deptCache);
            insertEmployees(empCache);
            System.out.println("Data inserted.\n");

            // Non-distributed join (colocated data)
            System.out.println("=== Colocated Join (Fast) ===");
            long startTime = System.currentTimeMillis();

            SqlFieldsQuery colocatedJoin = new SqlFieldsQuery(
                "SELECT e.name, e.salary, d.dept_name " +
                "FROM Employee e JOIN Department d ON e.dept_id = d.id " +
                "WHERE d.dept_name = ?")
                .setArgs("Engineering");

            List<List<?>> results = empCache.query(colocatedJoin).getAll();
            results.forEach(row -> System.out.println("   " + row));

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

            results = empCache.query(distributedJoin).getAll();
            results.forEach(row -> System.out.println("   " + row));

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

            empCache.query(aggJoin).getAll().forEach(row -> {
                Object avgSalary = row.get(2);
                String avgStr = avgSalary != null ? String.format("$%.2f", avgSalary) : "N/A";
                System.out.printf("   %s: %d employees, avg %s%n",
                    row.get(0), row.get(1), avgStr);
            });

            // Sub-query example
            System.out.println("\n=== Subquery Example ===");
            SqlFieldsQuery subQuery = new SqlFieldsQuery(
                "SELECT name, salary FROM Employee " +
                "WHERE salary > (SELECT AVG(salary) FROM Employee)")
                .setDistributedJoins(true);

            System.out.println("Employees earning above average:");
            empCache.query(subQuery).getAll().forEach(row ->
                System.out.println("   " + row.get(0) + " - $" + row.get(1)));

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
            "MERGE INTO Department (id, dept_name, location) VALUES (1, 'Engineering', 'Building A')")).getAll();
        cache.query(new SqlFieldsQuery(
            "MERGE INTO Department (id, dept_name, location) VALUES (2, 'Sales', 'Building B')")).getAll();
        cache.query(new SqlFieldsQuery(
            "MERGE INTO Department (id, dept_name, location) VALUES (3, 'Marketing', 'Building C')")).getAll();
        cache.query(new SqlFieldsQuery(
            "MERGE INTO Department (id, dept_name, location) VALUES (4, 'HR', 'Building D')")).getAll();
    }

    private static void insertEmployees(IgniteCache<Integer, Object> cache) {
        String[][] employees = {
            {"1", "'Alice'", "1", "95000"},
            {"2", "'Bob'", "2", "75000"},
            {"3", "'Charlie'", "1", "88000"},
            {"4", "'Diana'", "2", "82000"},
            {"5", "'Eve'", "1", "92000"},
            {"6", "'Frank'", "3", "70000"},
            {"7", "'Grace'", "1", "105000"},
            {"8", "'Henry'", "2", "78000"}
        };

        for (String[] emp : employees) {
            cache.query(new SqlFieldsQuery(
                "MERGE INTO Employee (id, name, dept_id, salary) VALUES (" +
                String.join(", ", emp) + ")")).getAll();
        }
    }
}
