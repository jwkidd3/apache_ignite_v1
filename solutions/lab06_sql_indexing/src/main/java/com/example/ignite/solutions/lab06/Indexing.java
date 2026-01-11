package com.example.ignite.solutions.lab06;

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
import java.util.List;

/**
 * Lab 6 Exercise 2: Index Creation and Optimization
 *
 * This exercise demonstrates creating different types of indexes
 * and analyzing query performance.
 */
public class Indexing {

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
            List<List<?>> results1 = cache.query(query1).getAll();
            long time1 = System.currentTimeMillis() - startTime;
            System.out.println("1. Query with category index: " + time1 + " ms");
            System.out.println("   Found " + results1.size() + " products");

            // Query using composite index
            startTime = System.currentTimeMillis();
            SqlFieldsQuery query2 = new SqlFieldsQuery(
                "SELECT name, price FROM Product WHERE category = ? AND price < ?")
                .setArgs("Electronics", 500.0);
            List<List<?>> results2 = cache.query(query2).getAll();
            long time2 = System.currentTimeMillis() - startTime;
            System.out.println("\n2. Query with composite index: " + time2 + " ms");
            System.out.println("   Found " + results2.size() + " products");

            // Range query with index
            startTime = System.currentTimeMillis();
            SqlFieldsQuery query3 = new SqlFieldsQuery(
                "SELECT name, price FROM Product WHERE price BETWEEN ? AND ?")
                .setArgs(100.0, 500.0);
            List<List<?>> results3 = cache.query(query3).getAll();
            long time3 = System.currentTimeMillis() - startTime;
            System.out.println("\n3. Range query with price index: " + time3 + " ms");
            System.out.println("   Found " + results3.size() + " products:");
            results3.forEach(row -> System.out.println("      " + row));

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

            // Display index types
            System.out.println("\n=== Index Types in Ignite ===");
            System.out.println("SORTED (B-tree): Efficient for range queries and equality");
            System.out.println("FULLTEXT: Text search with LIKE operator");
            System.out.println("GEOSPATIAL: Location-based queries (with ignite-geospatial)");
            System.out.println("COMPOSITE: Multiple columns, efficient for AND queries");

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
