package com.example.ignite.solutions.lab07;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.cache.CacheAtomicityMode;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.transactions.Transaction;
import org.apache.ignite.transactions.TransactionConcurrency;
import org.apache.ignite.transactions.TransactionIsolation;
import org.apache.ignite.transactions.TransactionOptimisticException;

/**
 * Lab 07 Exercise 2: Transaction Models - PESSIMISTIC vs OPTIMISTIC
 *
 * Demonstrates:
 * - PESSIMISTIC transactions with immediate locking
 * - OPTIMISTIC transactions with validation at commit
 * - Different isolation levels (READ_COMMITTED, REPEATABLE_READ, SERIALIZABLE)
 */
public class Lab07TransactionModels {

    public static void main(String[] args) {
        try (Ignite ignite = Ignition.start()) {
            System.out.println("=== Transaction Models Lab ===\n");

            CacheConfiguration<String, Integer> cfg =
                new CacheConfiguration<>("txCache");
            cfg.setAtomicityMode(CacheAtomicityMode.TRANSACTIONAL);

            IgniteCache<String, Integer> cache = ignite.getOrCreateCache(cfg);
            cache.put("counter", 0);

            // PESSIMISTIC transactions
            System.out.println("=== PESSIMISTIC Transactions ===");
            System.out.println("Locks acquired immediately on first access");
            System.out.println("Prevents conflicts but may cause contention\n");

            testPessimisticTransaction(ignite, cache);

            // OPTIMISTIC transactions
            System.out.println("\n=== OPTIMISTIC Transactions ===");
            System.out.println("No locks until commit");
            System.out.println("Better concurrency but may fail at commit\n");

            testOptimisticTransaction(ignite, cache);

            // Isolation levels
            System.out.println("\n=== Isolation Levels ===");
            testIsolationLevels(ignite, cache);

            System.out.println("\nPress Enter to exit...");
            System.in.read();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void testPessimisticTransaction(Ignite ignite,
                                                    IgniteCache<String, Integer> cache) {
        System.out.println("Running pessimistic transaction...");

        try (Transaction tx = ignite.transactions().txStart(
                TransactionConcurrency.PESSIMISTIC,
                TransactionIsolation.REPEATABLE_READ)) {

            // Lock is acquired here
            Integer value = cache.get("counter");
            System.out.println("  Read value: " + value + " (lock acquired)");

            // Simulate processing
            Thread.sleep(100);

            cache.put("counter", value + 1);
            System.out.println("  Updated value: " + (value + 1));

            tx.commit();
            System.out.println("  Transaction committed");
        } catch (Exception e) {
            System.out.println("  Transaction failed: " + e.getMessage());
        }
    }

    private static void testOptimisticTransaction(Ignite ignite,
                                                   IgniteCache<String, Integer> cache) {
        System.out.println("Running optimistic transaction...");

        try (Transaction tx = ignite.transactions().txStart(
                TransactionConcurrency.OPTIMISTIC,
                TransactionIsolation.SERIALIZABLE)) {

            // No lock acquired yet
            Integer value = cache.get("counter");
            System.out.println("  Read value: " + value + " (no lock)");

            // Simulate processing
            Thread.sleep(100);

            cache.put("counter", value + 1);
            System.out.println("  Preparing to commit...");

            // Lock acquired and validated at commit
            tx.commit();
            System.out.println("  Transaction committed (validation successful)");

        } catch (TransactionOptimisticException e) {
            System.out.println("  Optimistic transaction failed: " + e.getMessage());
            System.out.println("  Data was modified by another transaction");
        } catch (Exception e) {
            System.out.println("  Transaction failed: " + e.getMessage());
        }
    }

    private static void testIsolationLevels(Ignite ignite,
                                             IgniteCache<String, Integer> cache) {
        System.out.println("\n1. READ_COMMITTED:");
        System.out.println("   Can read different values within transaction");

        System.out.println("\n2. REPEATABLE_READ:");
        System.out.println("   Reads same value throughout transaction");

        System.out.println("\n3. SERIALIZABLE:");
        System.out.println("   Strictest - prevents all anomalies");

        // Demonstrate REPEATABLE_READ
        cache.put("test", 100);

        Thread reader = new Thread(() -> {
            try (Transaction tx = ignite.transactions().txStart(
                    TransactionConcurrency.PESSIMISTIC,
                    TransactionIsolation.REPEATABLE_READ)) {

                Integer value1 = cache.get("test");
                System.out.println("\n  Reader: First read = " + value1);

                Thread.sleep(200);

                Integer value2 = cache.get("test");
                System.out.println("  Reader: Second read = " + value2);
                System.out.println("  Reader: Values are " +
                    (value1.equals(value2) ? "SAME" : "DIFFERENT"));

                tx.commit();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        Thread writer = new Thread(() -> {
            try {
                Thread.sleep(100);
                System.out.println("\n  Writer: Updating value to 200");
                cache.put("test", 200);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        try {
            reader.start();
            writer.start();
            reader.join();
            writer.join();
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
