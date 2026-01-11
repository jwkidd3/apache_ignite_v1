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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Lab 07 Exercise 4: Comparing Isolation Levels with Concurrent Threads
 *
 * Demonstrates:
 * - READ_COMMITTED behavior (may see phantom reads)
 * - REPEATABLE_READ behavior (prevents phantom reads)
 * - SERIALIZABLE behavior (strictest isolation)
 * - Concurrent increment comparison across isolation levels
 */
public class Lab07IsolationComparison {

    public static void main(String[] args) {
        try (Ignite ignite = Ignition.start()) {
            System.out.println("=== Transaction Isolation Comparison Lab ===\n");

            CacheConfiguration<String, Integer> cfg =
                new CacheConfiguration<>("isolationCache");
            cfg.setAtomicityMode(CacheAtomicityMode.TRANSACTIONAL);

            IgniteCache<String, Integer> cache = ignite.getOrCreateCache(cfg);

            // Test 1: READ_COMMITTED - may see phantom reads
            System.out.println("=== Test 1: READ_COMMITTED Isolation ===");
            System.out.println("Characteristic: Can see committed changes from other transactions");
            System.out.println("Use case: High throughput, eventual consistency acceptable\n");
            testReadCommitted(ignite, cache);

            Thread.sleep(500);

            // Test 2: REPEATABLE_READ - prevents phantom reads within transaction
            System.out.println("\n=== Test 2: REPEATABLE_READ Isolation ===");
            System.out.println("Characteristic: Same read returns same value within transaction");
            System.out.println("Use case: Most common choice, good balance\n");
            testRepeatableRead(ignite, cache);

            Thread.sleep(500);

            // Test 3: SERIALIZABLE - strictest isolation
            System.out.println("\n=== Test 3: SERIALIZABLE Isolation ===");
            System.out.println("Characteristic: Transactions execute as if serialized");
            System.out.println("Use case: Financial transactions, critical consistency\n");
            testSerializable(ignite, cache);

            Thread.sleep(500);

            // Test 4: Concurrent increment comparison
            System.out.println("\n=== Test 4: Concurrent Increment Comparison ===");
            compareConcurrentIncrements(ignite, cache);

            System.out.println("\nPress Enter to exit...");
            System.in.read();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void testReadCommitted(Ignite ignite, IgniteCache<String, Integer> cache) {
        cache.put("read_committed_key", 100);
        CountDownLatch readerStarted = new CountDownLatch(1);
        CountDownLatch writerDone = new CountDownLatch(1);
        List<Integer> readValues = new ArrayList<>();

        // Reader thread with READ_COMMITTED
        Thread reader = new Thread(() -> {
            try (Transaction tx = ignite.transactions().txStart(
                    TransactionConcurrency.PESSIMISTIC,
                    TransactionIsolation.READ_COMMITTED)) {

                Integer firstRead = cache.get("read_committed_key");
                readValues.add(firstRead);
                System.out.println("  [Reader] First read: " + firstRead);

                readerStarted.countDown();
                writerDone.await(); // Wait for writer to complete

                // Small delay to ensure write is visible
                Thread.sleep(50);

                Integer secondRead = cache.get("read_committed_key");
                readValues.add(secondRead);
                System.out.println("  [Reader] Second read: " + secondRead);

                tx.commit();

                if (!firstRead.equals(secondRead)) {
                    System.out.println("  [Result] NON-REPEATABLE READ occurred!");
                    System.out.println("  [Result] Values changed within same transaction");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        // Writer thread
        Thread writer = new Thread(() -> {
            try {
                readerStarted.await(); // Wait for reader to start
                Thread.sleep(50);

                cache.put("read_committed_key", 200);
                System.out.println("  [Writer] Updated value to 200");
                writerDone.countDown();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        reader.start();
        writer.start();

        try {
            reader.join();
            writer.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static void testRepeatableRead(Ignite ignite, IgniteCache<String, Integer> cache) {
        cache.put("repeatable_read_key", 100);
        CountDownLatch readerStarted = new CountDownLatch(1);
        CountDownLatch readerDone = new CountDownLatch(1);

        // Reader thread with REPEATABLE_READ
        Thread reader = new Thread(() -> {
            try (Transaction tx = ignite.transactions().txStart(
                    TransactionConcurrency.PESSIMISTIC,
                    TransactionIsolation.REPEATABLE_READ)) {

                Integer firstRead = cache.get("repeatable_read_key");
                System.out.println("  [Reader] First read: " + firstRead + " (lock acquired)");

                readerStarted.countDown();
                Thread.sleep(200); // Hold lock while writer tries to update

                Integer secondRead = cache.get("repeatable_read_key");
                System.out.println("  [Reader] Second read: " + secondRead);

                tx.commit();
                readerDone.countDown();

                if (firstRead.equals(secondRead)) {
                    System.out.println("  [Result] REPEATABLE READ guaranteed!");
                    System.out.println("  [Result] Same value read throughout transaction");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        // Writer thread
        Thread writer = new Thread(() -> {
            try {
                readerStarted.await();
                System.out.println("  [Writer] Attempting to update (will wait for lock)...");

                long start = System.currentTimeMillis();
                cache.put("repeatable_read_key", 200);
                long duration = System.currentTimeMillis() - start;

                System.out.println("  [Writer] Update completed after " + duration + "ms");
                System.out.println("  [Writer] Had to wait for reader's lock release");
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        reader.start();
        writer.start();

        try {
            reader.join();
            writer.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static void testSerializable(Ignite ignite, IgniteCache<String, Integer> cache) {
        cache.put("serializable_key", 100);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(2);

        // Two concurrent transactions trying to read-modify-write
        Runnable task = () -> {
            String threadName = Thread.currentThread().getName();
            try (Transaction tx = ignite.transactions().txStart(
                    TransactionConcurrency.OPTIMISTIC,
                    TransactionIsolation.SERIALIZABLE)) {

                Integer value = cache.get("serializable_key");
                System.out.println("  [" + threadName + "] Read value: " + value);

                Thread.sleep(100); // Simulate processing

                cache.put("serializable_key", value + 50);
                System.out.println("  [" + threadName + "] Attempting to commit...");

                tx.commit();
                successCount.incrementAndGet();
                System.out.println("  [" + threadName + "] Commit SUCCESS");

            } catch (TransactionOptimisticException e) {
                failCount.incrementAndGet();
                System.out.println("  [" + threadName + "] Commit FAILED - conflict detected");
                System.out.println("  [" + threadName + "] " + e.getMessage());
            } catch (Exception e) {
                failCount.incrementAndGet();
                System.out.println("  [" + threadName + "] Error: " + e.getMessage());
            } finally {
                latch.countDown();
            }
        };

        Thread t1 = new Thread(task, "TX-1");
        Thread t2 = new Thread(task, "TX-2");

        t1.start();
        t2.start();

        try {
            latch.await();
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("\n  [Result] Successful commits: " + successCount.get());
        System.out.println("  [Result] Failed commits: " + failCount.get());
        System.out.println("  [Result] Final value: " + cache.get("serializable_key"));
        System.out.println("  [Result] SERIALIZABLE prevents lost updates!");
    }

    private static void compareConcurrentIncrements(Ignite ignite,
                                                     IgniteCache<String, Integer> cache) {
        int numThreads = 10;
        int incrementsPerThread = 100;

        // Test with different isolation levels
        TransactionIsolation[] levels = {
            TransactionIsolation.READ_COMMITTED,
            TransactionIsolation.REPEATABLE_READ,
            TransactionIsolation.SERIALIZABLE
        };

        for (TransactionIsolation isolation : levels) {
            cache.put("counter", 0);
            AtomicInteger successfulIncrements = new AtomicInteger(0);
            AtomicInteger failedIncrements = new AtomicInteger(0);

            ExecutorService executor = Executors.newFixedThreadPool(numThreads);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(numThreads);

            for (int i = 0; i < numThreads; i++) {
                executor.submit(() -> {
                    try {
                        startLatch.await(); // Synchronize start
                        for (int j = 0; j < incrementsPerThread; j++) {
                            try (Transaction tx = ignite.transactions().txStart(
                                    TransactionConcurrency.PESSIMISTIC,
                                    isolation)) {
                                Integer value = cache.get("counter");
                                cache.put("counter", value + 1);
                                tx.commit();
                                successfulIncrements.incrementAndGet();
                            } catch (Exception e) {
                                failedIncrements.incrementAndGet();
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            long startTime = System.currentTimeMillis();
            startLatch.countDown(); // Start all threads

            try {
                doneLatch.await(30, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            long duration = System.currentTimeMillis() - startTime;
            Integer finalValue = cache.get("counter");
            int expected = numThreads * incrementsPerThread;

            System.out.println("\n  " + isolation + ":");
            System.out.println("    Duration: " + duration + "ms");
            System.out.println("    Expected: " + expected + ", Actual: " + finalValue);
            System.out.println("    Successful: " + successfulIncrements.get() +
                             ", Failed: " + failedIncrements.get());
            System.out.println("    Correct: " + (finalValue == expected));

            executor.shutdown();
        }
    }
}
