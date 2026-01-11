package com.example.ignite.solutions.lab07;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.cache.CacheAtomicityMode;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.transactions.Transaction;
import org.apache.ignite.transactions.TransactionConcurrency;
import org.apache.ignite.transactions.TransactionIsolation;

import java.util.HashMap;
import java.util.Map;

/**
 * Lab 07 Exercise 3: Transaction Best Practices
 *
 * Demonstrates:
 * - Keeping transactions short
 * - Proper exception handling
 * - Batch operations within transactions
 * - Transaction configuration tips
 */
public class Lab07BestPractices {

    public static void main(String[] args) {
        try (Ignite ignite = Ignition.start()) {
            System.out.println("=== Transaction Best Practices Lab ===\n");

            CacheConfiguration<Integer, String> cfg =
                new CacheConfiguration<>("practiceCache");
            cfg.setAtomicityMode(CacheAtomicityMode.TRANSACTIONAL);

            IgniteCache<Integer, String> cache = ignite.getOrCreateCache(cfg);

            // Best Practice 1: Keep transactions short
            System.out.println("1. Keep Transactions Short");
            System.out.println("   - Minimize work inside transaction");
            System.out.println("   - Do heavy computation outside transaction");
            System.out.println("   - Avoid long-running operations\n");

            demonstrateShortTransactions(ignite, cache);

            // Best Practice 2: Use appropriate isolation level
            System.out.println("\n2. Choose Appropriate Isolation Level");
            System.out.println("   - READ_COMMITTED: Fastest, less strict");
            System.out.println("   - REPEATABLE_READ: Balance of speed and consistency");
            System.out.println("   - SERIALIZABLE: Slowest, most strict\n");

            // Best Practice 3: Handle exceptions
            System.out.println("3. Always Handle Exceptions");
            System.out.println("   - Use try-with-resources");
            System.out.println("   - Explicit rollback on error");
            System.out.println("   - Clean up resources\n");

            demonstrateExceptionHandling(ignite, cache);

            // Best Practice 4: Avoid nested transactions
            System.out.println("\n4. Avoid Nested Transactions");
            System.out.println("   - Ignite doesn't support nested transactions");
            System.out.println("   - Complete inner transaction before outer\n");

            // Best Practice 5: Use batch operations
            System.out.println("5. Use Batch Operations");
            System.out.println("   - putAll/getAll within transaction");
            System.out.println("   - Reduces network overhead");
            System.out.println("   - Better performance\n");

            demonstrateBatchOperations(ignite, cache);

            System.out.println("\n=== Transaction Configuration Tips ===");
            System.out.println("- Set appropriate timeout");
            System.out.println("- Use OPTIMISTIC for read-heavy workloads");
            System.out.println("- Use PESSIMISTIC when conflicts are likely");
            System.out.println("- Monitor transaction metrics");
            System.out.println("- Test deadlock scenarios");

            System.out.println("\nPress Enter to exit...");
            System.in.read();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void demonstrateShortTransactions(Ignite ignite,
                                                      IgniteCache<Integer, String> cache) {
        // Heavy computation OUTSIDE transaction
        String processedData = performHeavyComputation();

        // Short transaction - just data updates
        try (Transaction tx = ignite.transactions().txStart(
                TransactionConcurrency.PESSIMISTIC,
                TransactionIsolation.REPEATABLE_READ)) {

            cache.put(1, processedData);
            tx.commit();
        }

        System.out.println("   Transaction completed quickly\n");
    }

    private static String performHeavyComputation() {
        // Simulate heavy work outside transaction
        return "Processed Data";
    }

    private static void demonstrateExceptionHandling(Ignite ignite,
                                                      IgniteCache<Integer, String> cache) {
        Transaction tx = null;
        try {
            tx = ignite.transactions().txStart(
                TransactionConcurrency.PESSIMISTIC,
                TransactionIsolation.REPEATABLE_READ);

            cache.put(2, "Value");

            // Simulate error
            if (Math.random() > 0.5) {
                // Business logic here
            }

            tx.commit();
            System.out.println("   Transaction handled correctly\n");

        } catch (Exception e) {
            if (tx != null) {
                tx.rollback();
            }
            System.out.println("   Exception handled, transaction rolled back\n");
        } finally {
            if (tx != null) {
                tx.close();
            }
        }
    }

    private static void demonstrateBatchOperations(Ignite ignite,
                                                    IgniteCache<Integer, String> cache) {
        try (Transaction tx = ignite.transactions().txStart(
                TransactionConcurrency.PESSIMISTIC,
                TransactionIsolation.REPEATABLE_READ)) {

            // Batch operation within transaction
            Map<Integer, String> batch = new HashMap<>();
            for (int i = 10; i < 20; i++) {
                batch.put(i, "Batch-" + i);
            }
            cache.putAll(batch);

            tx.commit();
            System.out.println("   Batch of 10 entries committed\n");
        }
    }
}
