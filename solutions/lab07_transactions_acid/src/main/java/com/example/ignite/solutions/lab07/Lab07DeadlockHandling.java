package com.example.ignite.solutions.lab07;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.cache.CacheAtomicityMode;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.transactions.Transaction;
import org.apache.ignite.transactions.TransactionConcurrency;
import org.apache.ignite.transactions.TransactionDeadlockException;
import org.apache.ignite.transactions.TransactionIsolation;
import org.apache.ignite.transactions.TransactionTimeoutException;

import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Lab 07 Exercise 5: Deadlock Handling
 *
 * Demonstrates:
 * - Creating deliberate deadlock scenarios
 * - Handling TransactionDeadlockException
 * - Retry logic with exponential backoff
 * - Ordered locking to prevent deadlocks
 * - Timeout-based deadlock prevention
 */
public class Lab07DeadlockHandling {

    private static final int MAX_RETRIES = 5;
    private static final long BASE_BACKOFF_MS = 50;
    private static final Random random = new Random();

    public static void main(String[] args) {
        IgniteConfiguration cfg = new IgniteConfiguration();
        cfg.setIgniteInstanceName("deadlock-handling-node");

        try (Ignite ignite = Ignition.start(cfg)) {
            System.out.println("=== Deadlock Handling Lab ===\n");

            CacheConfiguration<String, Integer> cacheCfg =
                new CacheConfiguration<>("deadlockHandlingCache");
            cacheCfg.setAtomicityMode(CacheAtomicityMode.TRANSACTIONAL);

            IgniteCache<String, Integer> cache = ignite.getOrCreateCache(cacheCfg);

            // Initialize resources
            cache.put("resourceA", 1000);
            cache.put("resourceB", 2000);

            // Part 1: Demonstrate deliberate deadlock
            System.out.println("=== Part 1: Creating Deliberate Deadlock ===");
            System.out.println("Transaction 1: Locks A, then tries to lock B");
            System.out.println("Transaction 2: Locks B, then tries to lock A\n");
            demonstrateDeadlock(ignite, cache);

            Thread.sleep(1000);

            // Part 2: Demonstrate retry with exponential backoff
            System.out.println("\n=== Part 2: Retry Logic with Exponential Backoff ===");
            demonstrateRetryWithBackoff(ignite, cache);

            Thread.sleep(1000);

            // Part 3: Deadlock-free pattern using ordered locking
            System.out.println("\n=== Part 3: Deadlock-Free Pattern ===");
            demonstrateOrderedLocking(ignite, cache);

            // Part 4: Timeout-based deadlock prevention
            System.out.println("\n=== Part 4: Timeout-Based Prevention ===");
            demonstrateTimeoutPrevention(ignite, cache);

            System.out.println("\nPress Enter to exit...");
            System.in.read();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void demonstrateDeadlock(Ignite ignite, IgniteCache<String, Integer> cache) {
        CountDownLatch startLatch = new CountDownLatch(2);
        CountDownLatch lockAcquired = new CountDownLatch(2);
        AtomicInteger deadlockCount = new AtomicInteger(0);

        // Transaction 1: Lock A, then B
        Thread tx1 = new Thread(() -> {
            try {
                startLatch.countDown();
                startLatch.await();

                try (Transaction tx = ignite.transactions().txStart(
                        TransactionConcurrency.PESSIMISTIC,
                        TransactionIsolation.REPEATABLE_READ,
                        3000, // 3 second timeout
                        0)) {

                    System.out.println("[TX1] Acquiring lock on resourceA...");
                    Integer valueA = cache.get("resourceA");
                    System.out.println("[TX1] Locked resourceA (value: " + valueA + ")");

                    lockAcquired.countDown();
                    Thread.sleep(100); // Ensure TX2 has locked resourceB

                    System.out.println("[TX1] Trying to acquire lock on resourceB...");
                    Integer valueB = cache.get("resourceB");
                    System.out.println("[TX1] Locked resourceB (value: " + valueB + ")");

                    // Perform transfer
                    cache.put("resourceA", valueA - 100);
                    cache.put("resourceB", valueB + 100);

                    tx.commit();
                    System.out.println("[TX1] Transaction committed successfully!");
                }

            } catch (TransactionDeadlockException e) {
                deadlockCount.incrementAndGet();
                System.out.println("[TX1] DEADLOCK DETECTED!");
                System.out.println("[TX1] Exception: " + extractDeadlockInfo(e));
            } catch (TransactionTimeoutException e) {
                System.out.println("[TX1] Transaction timeout - possible deadlock");
            } catch (Exception e) {
                System.out.println("[TX1] Error: " + e.getClass().getSimpleName());
            }
        }, "Transaction-1");

        // Transaction 2: Lock B, then A (opposite order - causes deadlock)
        Thread tx2 = new Thread(() -> {
            try {
                startLatch.countDown();
                startLatch.await();

                try (Transaction tx = ignite.transactions().txStart(
                        TransactionConcurrency.PESSIMISTIC,
                        TransactionIsolation.REPEATABLE_READ,
                        3000, // 3 second timeout
                        0)) {

                    System.out.println("[TX2] Acquiring lock on resourceB...");
                    Integer valueB = cache.get("resourceB");
                    System.out.println("[TX2] Locked resourceB (value: " + valueB + ")");

                    lockAcquired.countDown();
                    Thread.sleep(100); // Ensure TX1 has locked resourceA

                    System.out.println("[TX2] Trying to acquire lock on resourceA...");
                    Integer valueA = cache.get("resourceA");
                    System.out.println("[TX2] Locked resourceA (value: " + valueA + ")");

                    // Perform transfer
                    cache.put("resourceB", valueB - 50);
                    cache.put("resourceA", valueA + 50);

                    tx.commit();
                    System.out.println("[TX2] Transaction committed successfully!");
                }

            } catch (TransactionDeadlockException e) {
                deadlockCount.incrementAndGet();
                System.out.println("[TX2] DEADLOCK DETECTED!");
                System.out.println("[TX2] Exception: " + extractDeadlockInfo(e));
            } catch (TransactionTimeoutException e) {
                System.out.println("[TX2] Transaction timeout - possible deadlock");
            } catch (Exception e) {
                System.out.println("[TX2] Error: " + e.getClass().getSimpleName());
            }
        }, "Transaction-2");

        tx1.start();
        tx2.start();

        try {
            tx1.join();
            tx2.join();
            System.out.println("\nDeadlocks detected: " + deadlockCount.get());
            System.out.println("One transaction was rolled back to resolve deadlock");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static void demonstrateRetryWithBackoff(Ignite ignite,
                                                     IgniteCache<String, Integer> cache) {
        cache.put("retryResource", 0);

        System.out.println("Executing transaction with retry logic...\n");

        boolean success = executeWithRetry(ignite, cache, () -> {
            Integer value = cache.get("retryResource");

            // Simulate potential conflict
            if (value == 0 && random.nextBoolean()) {
                throw new TransactionDeadlockException("Simulated deadlock for demo");
            }

            cache.put("retryResource", value + 1);
            System.out.println("  Updated retryResource to: " + (value + 1));
        });

        if (success) {
            System.out.println("\nTransaction completed successfully!");
            System.out.println("Final value: " + cache.get("retryResource"));
        } else {
            System.out.println("\nTransaction failed after " + MAX_RETRIES + " retries");
        }

        // Show the retry wrapper in action with concurrent access
        System.out.println("\n--- Testing retry with concurrent transactions ---");

        cache.put("sharedCounter", 0);
        int numThreads = 5;
        CountDownLatch latch = new CountDownLatch(numThreads);
        AtomicInteger successfulTransactions = new AtomicInteger(0);

        for (int i = 0; i < numThreads; i++) {
            final int threadNum = i;
            new Thread(() -> {
                boolean result = executeWithRetry(ignite, cache, () -> {
                    Integer value = cache.get("sharedCounter");
                    Thread.sleep(10); // Small delay to increase conflict chance
                    cache.put("sharedCounter", value + 1);
                });
                if (result) {
                    successfulTransactions.incrementAndGet();
                }
                latch.countDown();
            }, "RetryThread-" + threadNum).start();
        }

        try {
            latch.await();
            System.out.println("Successful transactions: " + successfulTransactions.get() + "/" + numThreads);
            System.out.println("Final counter value: " + cache.get("sharedCounter"));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Executes a transactional operation with exponential backoff retry.
     */
    private static boolean executeWithRetry(Ignite ignite,
                                            IgniteCache<String, Integer> cache,
                                            TransactionalOperation operation) {
        int attempt = 0;

        while (attempt < MAX_RETRIES) {
            try (Transaction tx = ignite.transactions().txStart(
                    TransactionConcurrency.PESSIMISTIC,
                    TransactionIsolation.REPEATABLE_READ,
                    5000, // 5 second timeout
                    0)) {

                operation.execute();
                tx.commit();
                return true; // Success

            } catch (TransactionDeadlockException e) {
                attempt++;
                long backoffMs = calculateBackoff(attempt);
                System.out.println("  Retry " + attempt + "/" + MAX_RETRIES +
                                 " - Deadlock detected, backing off " + backoffMs + "ms");

                try {
                    Thread.sleep(backoffMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return false;
                }

            } catch (TransactionTimeoutException e) {
                attempt++;
                long backoffMs = calculateBackoff(attempt);
                System.out.println("  Retry " + attempt + "/" + MAX_RETRIES +
                                 " - Timeout, backing off " + backoffMs + "ms");

                try {
                    Thread.sleep(backoffMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return false;
                }

            } catch (Exception e) {
                System.out.println("  Non-retryable error: " + e.getMessage());
                return false;
            }
        }

        return false; // All retries exhausted
    }

    /**
     * Calculates exponential backoff with jitter.
     */
    private static long calculateBackoff(int attempt) {
        // Exponential backoff: BASE * 2^attempt
        long exponentialBackoff = BASE_BACKOFF_MS * (1L << attempt);

        // Add random jitter (0-50% of backoff)
        long jitter = (long) (random.nextDouble() * exponentialBackoff * 0.5);

        // Cap at 5 seconds
        return Math.min(exponentialBackoff + jitter, 5000);
    }

    private static void demonstrateOrderedLocking(Ignite ignite,
                                                   IgniteCache<String, Integer> cache) {
        System.out.println("Using consistent lock ordering to prevent deadlocks\n");

        cache.put("orderedA", 500);
        cache.put("orderedB", 500);

        int numTransactions = 4;
        CountDownLatch latch = new CountDownLatch(numTransactions);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < numTransactions; i++) {
            final int txNum = i;
            new Thread(() -> {
                try {
                    // Always lock in alphabetical order: A before B
                    performOrderedTransaction(ignite, cache, "orderedA", "orderedB",
                                             (txNum % 2 == 0) ? 10 : -10);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    System.out.println("[TX" + txNum + "] Failed: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            }, "OrderedTX-" + txNum).start();
        }

        try {
            latch.await();
            System.out.println("\nAll " + successCount.get() + "/" + numTransactions +
                             " transactions completed successfully");
            System.out.println("No deadlocks occurred due to consistent ordering!");
            System.out.println("Final values - A: " + cache.get("orderedA") +
                             ", B: " + cache.get("orderedB"));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static void performOrderedTransaction(Ignite ignite,
                                                   IgniteCache<String, Integer> cache,
                                                   String keyA, String keyB,
                                                   int transferAmount) throws Exception {
        String threadName = Thread.currentThread().getName();

        try (Transaction tx = ignite.transactions().txStart(
                TransactionConcurrency.PESSIMISTIC,
                TransactionIsolation.REPEATABLE_READ,
                5000, 0)) {

            // Always lock in consistent order (alphabetical)
            String firstKey = keyA.compareTo(keyB) < 0 ? keyA : keyB;
            String secondKey = keyA.compareTo(keyB) < 0 ? keyB : keyA;

            System.out.println("[" + threadName + "] Locking " + firstKey + "...");
            Integer firstValue = cache.get(firstKey);

            Thread.sleep(50); // Simulate processing

            System.out.println("[" + threadName + "] Locking " + secondKey + "...");
            Integer secondValue = cache.get(secondKey);

            // Perform the transfer
            if (firstKey.equals(keyA)) {
                cache.put(keyA, firstValue - transferAmount);
                cache.put(keyB, secondValue + transferAmount);
            } else {
                cache.put(keyA, secondValue - transferAmount);
                cache.put(keyB, firstValue + transferAmount);
            }

            tx.commit();
            System.out.println("[" + threadName + "] Committed transfer of " + transferAmount);
        }
    }

    private static void demonstrateTimeoutPrevention(Ignite ignite,
                                                      IgniteCache<String, Integer> cache) {
        System.out.println("Using short timeouts to prevent long-running deadlocks\n");

        cache.put("timeoutKey", 100);

        // Transaction with very short timeout
        try (Transaction tx = ignite.transactions().txStart(
                TransactionConcurrency.PESSIMISTIC,
                TransactionIsolation.REPEATABLE_READ,
                100, // Very short 100ms timeout
                0)) {

            cache.get("timeoutKey");

            // Simulate long operation
            Thread.sleep(200);

            tx.commit();
            System.out.println("Transaction completed within timeout");

        } catch (TransactionTimeoutException e) {
            System.out.println("Transaction timed out as expected!");
            System.out.println("Short timeouts prevent indefinite blocking");
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }

        System.out.println("\nBest Practice: Set timeouts based on expected operation duration");
        System.out.println("Typical values: 1-5 seconds for short transactions");
    }

    private static String extractDeadlockInfo(TransactionDeadlockException e) {
        String message = e.getMessage();
        if (message != null && message.length() > 100) {
            return message.substring(0, 100) + "...";
        }
        return message;
    }

    @FunctionalInterface
    interface TransactionalOperation {
        void execute() throws Exception;
    }
}
