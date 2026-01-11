package com.example.ignite.solutions.lab06;

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

/**
 * Lab 6 Exercise 1: Basic SQL Support
 *
 * This exercise demonstrates DDL, DML, and DQL operations
 * using Ignite's SQL engine.
 */
public class BasicSQL {

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

            // COUNT and SUM
            System.out.println("\n5. Aggregations:");
            SqlFieldsQuery query5 = new SqlFieldsQuery(
                "SELECT COUNT(*), SUM(salary), MIN(age), MAX(age) FROM Person");
            cache.query(query5).getAll().forEach(row ->
                System.out.println("   Count: " + row.get(0) +
                    ", Total Salary: $" + row.get(1) +
                    ", Age Range: " + row.get(2) + "-" + row.get(3)));

            // DML: UPDATE
            System.out.println("\n=== DML: UPDATE Data ===");
            cache.query(new SqlFieldsQuery(
                "UPDATE Person SET salary = salary * 1.1 WHERE city = ?")
                .setArgs("New York")).getAll();
            System.out.println("Updated salaries in New York (+10%)");

            // Verify update
            System.out.println("\nNew York salaries after update:");
            cache.query(new SqlFieldsQuery(
                "SELECT name, salary FROM Person WHERE city = 'New York'"))
                .getAll().forEach(row -> System.out.println("   " + row));

            // DML: DELETE
            System.out.println("\n=== DML: DELETE Data ===");
            cache.query(new SqlFieldsQuery(
                "DELETE FROM Person WHERE age < ?")
                .setArgs(29)).getAll();
            System.out.println("Deleted persons younger than 29");

            // Verify
            System.out.println("\nFinal count: " + cache.size() + " records");
            cache.query(new SqlFieldsQuery("SELECT name, age FROM Person"))
                .getAll().forEach(row -> System.out.println("   " + row));

            System.out.println("\nPress Enter to exit...");
            System.in.read();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
