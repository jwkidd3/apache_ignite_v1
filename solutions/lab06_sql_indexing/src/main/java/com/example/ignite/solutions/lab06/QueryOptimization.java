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
import java.util.Random;

/**
 * Lab 6 Optional: Query Optimization and Performance Analysis
 *
 * This exercise demonstrates query optimization techniques
 * and performance measurement.
 */
public class QueryOptimization {

    private static final int DATA_SIZE = 10000;
    private static final String[] CATEGORIES = {"Electronics", "Clothing", "Books", "Home", "Sports"};
    private static final Random random = new Random(42);

    public static void main(String[] args) {
        try (Ignite ignite = Ignition.start()) {
            System.out.println("=== Query Optimization Lab ===\n");

            // Create cache without indexes
            IgniteCache<Long, Object> noIndexCache = createCache(ignite, "ProductNoIndex", false);

            // Create cache with indexes
            IgniteCache<Long, Object> indexedCache = createCache(ignite, "ProductIndexed", true);

            // Insert data
            System.out.println("Inserting " + DATA_SIZE + " products into each cache...");
            insertData(noIndexCache, "ProductNoIndex");
            insertData(indexedCache, "ProductIndexed");
            System.out.println("Data inserted.\n");

            // Compare query performance
            System.out.println("=".repeat(60));
            System.out.println("Performance Comparison: No Index vs Indexed");
            System.out.println("=".repeat(60));

            // Test 1: Category filter
            System.out.println("\nTest 1: Category Filter");
            compareQueries(
                noIndexCache, indexedCache,
                "SELECT name, price FROM %s WHERE category = ?",
                "Electronics"
            );

            // Test 2: Price range
            System.out.println("\nTest 2: Price Range Query");
            compareQueries(
                noIndexCache, indexedCache,
                "SELECT name, price FROM %s WHERE price BETWEEN ? AND ?",
                100.0, 500.0
            );

            // Test 3: Combined filter
            System.out.println("\nTest 3: Combined Filter (Category + Price)");
            compareQueries(
                noIndexCache, indexedCache,
                "SELECT name, price FROM %s WHERE category = ? AND price < ?",
                "Electronics", 500.0
            );

            // Test 4: Aggregation
            System.out.println("\nTest 4: Aggregation by Category");
            compareQueries(
                noIndexCache, indexedCache,
                "SELECT category, COUNT(*), AVG(price) FROM %s GROUP BY category"
            );

            // Query plan analysis
            System.out.println("\n" + "=".repeat(60));
            System.out.println("Query Plan Analysis");
            System.out.println("=".repeat(60));

            System.out.println("\nIndexed table query plan:");
            SqlFieldsQuery explainQuery = new SqlFieldsQuery(
                "EXPLAIN SELECT name, price FROM ProductIndexed WHERE category = 'Electronics' AND price < 500");
            indexedCache.query(explainQuery).getAll().forEach(row ->
                System.out.println("  " + row.get(0)));

            // Optimization tips
            System.out.println("\n" + "=".repeat(60));
            System.out.println("Query Optimization Tips");
            System.out.println("=".repeat(60));
            System.out.println("\n1. Create indexes on columns used in WHERE clauses");
            System.out.println("2. Use composite indexes for multi-column filters");
            System.out.println("3. Use EXPLAIN to verify index usage");
            System.out.println("4. Avoid SELECT * - specify needed columns");
            System.out.println("5. Use parameterized queries for plan caching");
            System.out.println("6. Consider index order in composite indexes");
            System.out.println("7. Monitor query statistics for slow queries");

            System.out.println("\nPress Enter to exit...");
            System.in.read();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static IgniteCache<Long, Object> createCache(Ignite ignite, String name, boolean withIndexes) {
        CacheConfiguration<Long, Object> cfg = new CacheConfiguration<>(name);

        QueryEntity entity = new QueryEntity(Long.class, Object.class);
        entity.setTableName(name);

        LinkedHashMap<String, String> fields = new LinkedHashMap<>();
        fields.put("id", "java.lang.Long");
        fields.put("name", "java.lang.String");
        fields.put("category", "java.lang.String");
        fields.put("price", "java.lang.Double");
        fields.put("stock", "java.lang.Integer");

        entity.setFields(fields);
        entity.setKeyFieldName("id");

        if (withIndexes) {
            QueryIndex categoryIdx = new QueryIndex("category", QueryIndexType.SORTED);
            QueryIndex priceIdx = new QueryIndex("price", QueryIndexType.SORTED);
            QueryIndex compositeIdx = new QueryIndex(
                Arrays.asList("category", "price"), QueryIndexType.SORTED);
            compositeIdx.setName("category_price_idx");

            entity.setIndexes(Arrays.asList(categoryIdx, priceIdx, compositeIdx));
        }

        cfg.setQueryEntities(Arrays.asList(entity));
        cfg.setSqlSchema("PUBLIC");

        return ignite.getOrCreateCache(cfg);
    }

    private static void insertData(IgniteCache<Long, Object> cache, String tableName) {
        for (int i = 0; i < DATA_SIZE; i++) {
            String category = CATEGORIES[random.nextInt(CATEGORIES.length)];
            double price = 10 + random.nextDouble() * 990;
            int stock = random.nextInt(100);

            cache.query(new SqlFieldsQuery(
                "INSERT INTO " + tableName + " (id, name, category, price, stock) VALUES (?, ?, ?, ?, ?)")
                .setArgs((long) i, "Product-" + i, category, price, stock)).getAll();
        }
    }

    private static void compareQueries(IgniteCache<Long, Object> noIndex,
                                       IgniteCache<Long, Object> indexed,
                                       String queryTemplate,
                                       Object... args) {
        // Run on non-indexed table
        String noIndexQuery = String.format(queryTemplate, "ProductNoIndex");
        long startTime = System.nanoTime();
        List<List<?>> results1 = noIndex.query(new SqlFieldsQuery(noIndexQuery).setArgs(args)).getAll();
        long noIndexTime = (System.nanoTime() - startTime) / 1_000_000;

        // Run on indexed table
        String indexedQuery = String.format(queryTemplate, "ProductIndexed");
        startTime = System.nanoTime();
        List<List<?>> results2 = indexed.query(new SqlFieldsQuery(indexedQuery).setArgs(args)).getAll();
        long indexedTime = (System.nanoTime() - startTime) / 1_000_000;

        System.out.println("  No Index:  " + noIndexTime + " ms (" + results1.size() + " rows)");
        System.out.println("  Indexed:   " + indexedTime + " ms (" + results2.size() + " rows)");
        if (indexedTime > 0) {
            System.out.println("  Speedup:   " + String.format("%.1fx", (double) noIndexTime / indexedTime));
        }
    }
}
