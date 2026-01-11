package com.example.ignite.solutions.lab11;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.cache.CacheMode;
import org.apache.ignite.cache.QueryEntity;
import org.apache.ignite.cache.QueryIndex;
import org.apache.ignite.cache.QueryIndexType;
import org.apache.ignite.cache.query.FieldsQueryCursor;
import org.apache.ignite.cache.query.SqlFieldsQuery;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Random;

/**
 * Lab 11 Exercise: SQL Query Optimization
 *
 * Demonstrates:
 * - Index creation and usage
 * - Query performance comparison (indexed vs non-indexed)
 * - Query plan analysis with EXPLAIN
 * - Lazy query execution
 * - Query parallelism tuning
 */
public class Lab11QueryOptimization {

    private static final int DATA_SIZE = 50000;
    private static final Random random = new Random(42);

    public static void main(String[] args) {
        System.out.println("=== SQL Query Optimization Lab ===\n");

        IgniteConfiguration cfg = new IgniteConfiguration();
        cfg.setIgniteInstanceName("query-optimization-node");

        try (Ignite ignite = Ignition.start(cfg)) {
            System.out.println("Ignite started\n");

            // Create caches with and without indexes
            IgniteCache<Long, Order> indexedCache = createIndexedCache(ignite);
            IgniteCache<Long, Order> nonIndexedCache = createNonIndexedCache(ignite);

            // Populate data
            System.out.println("Populating " + DATA_SIZE + " orders...");
            populateData(indexedCache, nonIndexedCache);
            System.out.println("Data populated\n");

            // Compare query performance
            System.out.println("=== Query Performance Comparison ===\n");

            // Test 1: Point lookup by customer
            System.out.println("--- Test 1: Filter by Customer ID ---");
            compareQueryPerformance(indexedCache, nonIndexedCache,
                "SELECT * FROM Order WHERE customerId = ?",
                new Object[]{1000L});

            // Test 2: Range query on amount
            System.out.println("\n--- Test 2: Range Query on Amount ---");
            compareQueryPerformance(indexedCache, nonIndexedCache,
                "SELECT * FROM Order WHERE amount BETWEEN ? AND ?",
                new Object[]{5000.0, 10000.0});

            // Test 3: Aggregation
            System.out.println("\n--- Test 3: Aggregation Query ---");
            compareQueryPerformance(indexedCache, nonIndexedCache,
                "SELECT customerId, SUM(amount), COUNT(*) FROM Order GROUP BY customerId HAVING COUNT(*) > ?",
                new Object[]{3});

            // Test 4: Sorting with filter
            System.out.println("\n--- Test 4: Sorting with Filter ---");
            compareQueryPerformance(indexedCache, nonIndexedCache,
                "SELECT * FROM Order WHERE status = ? ORDER BY orderDate DESC LIMIT ?",
                new Object[]{"COMPLETED", 100});

            // Demonstrate lazy vs eager execution
            System.out.println("\n=== Lazy vs Eager Query Execution ===");
            testLazyExecution(indexedCache);

            // Show query plan analysis
            System.out.println("\n=== Query Plan Analysis ===");
            analyzeQueryPlan(indexedCache);

            // Display optimization tips
            displayQueryOptimizationTips();

            System.out.println("\nPress Enter to exit...");
            System.in.read();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static IgniteCache<Long, Order> createIndexedCache(Ignite ignite) {
        CacheConfiguration<Long, Order> cfg = new CacheConfiguration<>("indexedOrders");
        cfg.setCacheMode(CacheMode.PARTITIONED);
        cfg.setBackups(0);
        cfg.setStatisticsEnabled(true);
        cfg.setSqlSchema("PUBLIC");

        // Define query entity with indexes
        QueryEntity entity = new QueryEntity(Long.class.getName(), Order.class.getName());
        entity.setTableName("Order");

        LinkedHashMap<String, String> fields = new LinkedHashMap<>();
        fields.put("orderId", Long.class.getName());
        fields.put("customerId", Long.class.getName());
        fields.put("amount", Double.class.getName());
        fields.put("status", String.class.getName());
        fields.put("orderDate", Long.class.getName());
        entity.setFields(fields);

        entity.setKeyFieldName("orderId");

        // Create indexes for common query patterns
        QueryIndex customerIdx = new QueryIndex("customerId", QueryIndexType.SORTED);
        customerIdx.setName("idx_customer");

        QueryIndex amountIdx = new QueryIndex("amount", QueryIndexType.SORTED);
        amountIdx.setName("idx_amount");

        // Composite index for status + orderDate (for filtering and sorting)
        QueryIndex statusDateIdx = new QueryIndex(
            Arrays.asList("status", "orderDate"), QueryIndexType.SORTED);
        statusDateIdx.setName("idx_status_date");

        entity.setIndexes(Arrays.asList(customerIdx, amountIdx, statusDateIdx));

        cfg.setQueryEntities(Collections.singletonList(entity));

        return ignite.getOrCreateCache(cfg);
    }

    private static IgniteCache<Long, Order> createNonIndexedCache(Ignite ignite) {
        CacheConfiguration<Long, Order> cfg = new CacheConfiguration<>("nonIndexedOrders");
        cfg.setCacheMode(CacheMode.PARTITIONED);
        cfg.setBackups(0);
        cfg.setStatisticsEnabled(true);
        cfg.setSqlSchema("PUBLIC");

        // Define query entity WITHOUT indexes
        QueryEntity entity = new QueryEntity(Long.class.getName(), Order.class.getName());
        entity.setTableName("OrderNoIdx");

        LinkedHashMap<String, String> fields = new LinkedHashMap<>();
        fields.put("orderId", Long.class.getName());
        fields.put("customerId", Long.class.getName());
        fields.put("amount", Double.class.getName());
        fields.put("status", String.class.getName());
        fields.put("orderDate", Long.class.getName());
        entity.setFields(fields);

        entity.setKeyFieldName("orderId");
        // No indexes!

        cfg.setQueryEntities(Collections.singletonList(entity));

        return ignite.getOrCreateCache(cfg);
    }

    private static void populateData(IgniteCache<Long, Order> indexed,
                                      IgniteCache<Long, Order> nonIndexed) {
        String[] statuses = {"PENDING", "PROCESSING", "COMPLETED", "CANCELLED"};

        for (long i = 0; i < DATA_SIZE; i++) {
            Order order = new Order();
            order.orderId = i;
            order.customerId = random.nextInt(5000);
            order.amount = 100.0 + random.nextDouble() * 10000;
            order.status = statuses[random.nextInt(statuses.length)];
            order.orderDate = System.currentTimeMillis() - random.nextInt(365 * 24 * 60 * 60 * 1000);

            indexed.put(i, order);
            nonIndexed.put(i, order);

            if (i % 10000 == 0 && i > 0) {
                System.out.println("  Inserted " + i + " orders...");
            }
        }
    }

    private static void compareQueryPerformance(
            IgniteCache<Long, Order> indexed,
            IgniteCache<Long, Order> nonIndexed,
            String sql, Object[] params) {

        // Replace table name for non-indexed
        String sqlNonIndexed = sql.replace("Order", "OrderNoIdx");

        // Warm up
        for (int i = 0; i < 3; i++) {
            executeQuery(indexed, sql, params);
            executeQuery(nonIndexed, sqlNonIndexed, params);
        }

        // Benchmark indexed
        long startTime = System.nanoTime();
        int indexedCount = 0;
        for (int i = 0; i < 10; i++) {
            indexedCount = executeQuery(indexed, sql, params);
        }
        long indexedTime = (System.nanoTime() - startTime) / 10;

        // Benchmark non-indexed
        startTime = System.nanoTime();
        int nonIndexedCount = 0;
        for (int i = 0; i < 10; i++) {
            nonIndexedCount = executeQuery(nonIndexed, sqlNonIndexed, params);
        }
        long nonIndexedTime = (System.nanoTime() - startTime) / 10;

        System.out.println("Results: " + indexedCount + " rows");
        System.out.println("Indexed:     " + String.format("%,d", indexedTime / 1000) + " us");
        System.out.println("Non-Indexed: " + String.format("%,d", nonIndexedTime / 1000) + " us");
        if (indexedTime > 0) {
            System.out.println("Speedup:     " + String.format("%.2fx", (double) nonIndexedTime / indexedTime));
        }
    }

    private static int executeQuery(IgniteCache<?, ?> cache, String sql, Object[] params) {
        SqlFieldsQuery query = new SqlFieldsQuery(sql);
        if (params != null && params.length > 0) {
            query.setArgs(params);
        }

        int count = 0;
        try (FieldsQueryCursor<List<?>> cursor = cache.query(query)) {
            for (List<?> row : cursor) {
                count++;
            }
        }
        return count;
    }

    private static void testLazyExecution(IgniteCache<Long, Order> cache) {
        String sql = "SELECT * FROM Order WHERE amount > 5000 ORDER BY amount DESC";

        System.out.println("Comparing lazy vs eager execution for large result sets...\n");

        // Lazy execution (streaming results)
        SqlFieldsQuery lazyQuery = new SqlFieldsQuery(sql);
        lazyQuery.setLazy(true);
        lazyQuery.setPageSize(100);

        long startTime = System.nanoTime();
        int lazyCount = 0;
        try (FieldsQueryCursor<List<?>> cursor = cache.query(lazyQuery)) {
            for (List<?> row : cursor) {
                lazyCount++;
                if (lazyCount >= 100) break; // Only need first 100
            }
        }
        long lazyTime = System.nanoTime() - startTime;
        System.out.println("Lazy (first 100 rows):  " + String.format("%,d", lazyTime / 1000) + " us");

        // Eager execution (load all then take first 100)
        SqlFieldsQuery eagerQuery = new SqlFieldsQuery(sql);
        eagerQuery.setLazy(false);

        startTime = System.nanoTime();
        int eagerCount = 0;
        try (FieldsQueryCursor<List<?>> cursor = cache.query(eagerQuery)) {
            List<List<?>> all = cursor.getAll();
            for (int i = 0; i < Math.min(100, all.size()); i++) {
                eagerCount++;
            }
        }
        long eagerTime = System.nanoTime() - startTime;
        System.out.println("Eager (all -> first 100): " + String.format("%,d", eagerTime / 1000) + " us");

        if (lazyTime > 0) {
            System.out.println("Lazy advantage:         " + String.format("%.2fx faster", (double) eagerTime / lazyTime));
        }

        System.out.println("\nKey Insight: Use lazy execution when you only need a subset of results");
    }

    private static void analyzeQueryPlan(IgniteCache<Long, Order> cache) {
        String[] queries = {
            "SELECT * FROM Order WHERE customerId = 1000",
            "SELECT * FROM Order WHERE amount BETWEEN 5000 AND 10000",
            "SELECT * FROM Order WHERE status = 'COMPLETED' ORDER BY orderDate DESC",
            "SELECT customerId, COUNT(*), SUM(amount) FROM Order GROUP BY customerId"
        };

        for (String sql : queries) {
            System.out.println("\nQuery: " + sql);
            System.out.println("Plan:");

            SqlFieldsQuery explainQuery = new SqlFieldsQuery("EXPLAIN " + sql);
            try (FieldsQueryCursor<List<?>> cursor = cache.query(explainQuery)) {
                for (List<?> row : cursor) {
                    String plan = row.get(0).toString();
                    // Highlight index usage
                    if (plan.contains("idx_")) {
                        System.out.println("  [INDEX USED] " + plan);
                    } else if (plan.contains("SCAN")) {
                        System.out.println("  [TABLE SCAN] " + plan);
                    } else {
                        System.out.println("  " + plan);
                    }
                }
            }
        }
    }

    private static void displayQueryOptimizationTips() {
        System.out.println("\n=== Query Optimization Best Practices ===");

        System.out.println("\n1. INDEX STRATEGY:");
        System.out.println("   - Create indexes on frequently filtered columns");
        System.out.println("   - Use composite indexes for multi-column filters");
        System.out.println("   - Consider index order (leftmost prefix rule)");
        System.out.println("   - Avoid over-indexing (impacts write performance)");

        System.out.println("\n2. QUERY OPTIMIZATION:");
        System.out.println("   - Use EXPLAIN to analyze query plans");
        System.out.println("   - Avoid SELECT * - specify needed columns");
        System.out.println("   - Use LIMIT for pagination");
        System.out.println("   - Prefer batch operations over single-row queries");

        System.out.println("\n3. LAZY EXECUTION:");
        System.out.println("   - Use setLazy(true) for large result sets");
        System.out.println("   - Streams results instead of loading all");
        System.out.println("   - Essential when using LIMIT queries");
        System.out.println("   - Reduces memory footprint");

        System.out.println("\n4. PAGE SIZE:");
        System.out.println("   - Default is 1024 rows per page");
        System.out.println("   - Increase for large result sets");
        System.out.println("   - Decrease for memory-constrained environments");

        System.out.println("\n5. DATA COLOCATION:");
        System.out.println("   - Design for data colocation to avoid distributed joins");
        System.out.println("   - Use affinity keys for related data");
        System.out.println("   - Only enable distributed joins when necessary");

        System.out.println("\n6. COMMON ISSUES:");
        System.out.println("   - Missing index: Look for TABLE SCAN in EXPLAIN");
        System.out.println("   - Wrong index: Check if correct index is being used");
        System.out.println("   - Over-fetching: Use projection to select only needed columns");
        System.out.println("   - N+1 queries: Use JOINs or batch operations instead");
    }

    // Order class for testing
    public static class Order implements Serializable {
        private static final long serialVersionUID = 1L;

        public long orderId;
        public long customerId;
        public double amount;
        public String status;
        public long orderDate;

        @Override
        public String toString() {
            return "Order{" +
                "orderId=" + orderId +
                ", customerId=" + customerId +
                ", amount=" + amount +
                ", status='" + status + '\'' +
                '}';
        }
    }
}
