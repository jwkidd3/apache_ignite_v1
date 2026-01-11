# Lab 7: Transactions and ACID Properties

## Duration: 60 minutes

## Objectives
- Understand transaction concepts in distributed systems
- Implement PESSIMISTIC and OPTIMISTIC transaction models
- Work with different isolation levels
- Handle deadlock detection and resolution
- Learn transaction best practices
- Compare transaction isolation levels with concurrent threads
- Implement retry logic with exponential backoff
- Perform cross-cache atomic transactions

## Prerequisites
- Completed Labs 1-6
- Understanding of ACID properties
- Familiarity with transactions

## Part 1: Basic Transactions (10 minutes)

### Exercise 1: Simple Transactional Operations

Create `Lab07BasicTransactions.java`:

```java
package com.example.ignite;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.cache.CacheAtomicityMode;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.transactions.Transaction;
import org.apache.ignite.transactions.TransactionConcurrency;
import org.apache.ignite.transactions.TransactionIsolation;

public class Lab07BasicTransactions {

    public static void main(String[] args) {
        try (Ignite ignite = Ignition.start()) {
            System.out.println("=== Basic Transactions Lab ===\n");

            // Create transactional cache
            CacheConfiguration<Integer, Integer> cfg =
                new CacheConfiguration<>("accountCache");
            cfg.setAtomicityMode(CacheAtomicityMode.TRANSACTIONAL);
            cfg.setBackups(1);

            IgniteCache<Integer, Integer> cache = ignite.getOrCreateCache(cfg);

            // Initialize accounts
            cache.put(1, 1000); // Account 1: $1000
            cache.put(2, 500);  // Account 2: $500

            System.out.println("Initial balances:");
            System.out.println("Account 1: $" + cache.get(1));
            System.out.println("Account 2: $" + cache.get(2));

            // Successful transaction
            System.out.println("\n=== Test 1: Successful Transaction ===");
            try (Transaction tx = ignite.transactions().txStart(
                    TransactionConcurrency.PESSIMISTIC,
                    TransactionIsolation.REPEATABLE_READ)) {

                int balance1 = cache.get(1);
                int balance2 = cache.get(2);

                // Transfer $200 from account 1 to account 2
                cache.put(1, balance1 - 200);
                cache.put(2, balance2 + 200);

                tx.commit();
                System.out.println("Transaction committed successfully");
            }

            System.out.println("New balances:");
            System.out.println("Account 1: $" + cache.get(1));
            System.out.println("Account 2: $" + cache.get(2));

            // Failed transaction (rollback)
            System.out.println("\n=== Test 2: Transaction Rollback ===");
            try (Transaction tx = ignite.transactions().txStart(
                    TransactionConcurrency.PESSIMISTIC,
                    TransactionIsolation.REPEATABLE_READ)) {

                int balance1 = cache.get(1);
                int balance2 = cache.get(2);

                // Try to transfer more than available
                if (balance1 < 2000) {
                    System.out.println("Insufficient funds! Rolling back...");
                    tx.rollback();
                } else {
                    cache.put(1, balance1 - 2000);
                    cache.put(2, balance2 + 2000);
                    tx.commit();
                }
            }

            System.out.println("Balances after rollback:");
            System.out.println("Account 1: $" + cache.get(1));
            System.out.println("Account 2: $" + cache.get(2));

            // Transaction with exception (automatic rollback)
            System.out.println("\n=== Test 3: Exception Handling ===");
            try (Transaction tx = ignite.transactions().txStart(
                    TransactionConcurrency.PESSIMISTIC,
                    TransactionIsolation.REPEATABLE_READ)) {

                int balance1 = cache.get(1);
                cache.put(1, balance1 - 100);

                // Simulate error
                if (true) {
                    throw new RuntimeException("Simulated error");
                }

                tx.commit();

            } catch (Exception e) {
                System.out.println("Exception caught: " + e.getMessage());
                System.out.println("Transaction automatically rolled back");
            }

            System.out.println("Balance after exception:");
            System.out.println("Account 1: $" + cache.get(1) + " (unchanged)");

            System.out.println("\nPress Enter to exit...");
            System.in.read();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

## Part 2: Transaction Models and Isolation Levels (5 minutes)

### Exercise 2: PESSIMISTIC vs OPTIMISTIC

Create `Lab07TransactionModels.java`:

```java
package com.example.ignite;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.cache.CacheAtomicityMode;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.transactions.Transaction;
import org.apache.ignite.transactions.TransactionConcurrency;
import org.apache.ignite.transactions.TransactionIsolation;
import org.apache.ignite.transactions.TransactionOptimisticException;

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
```

## Part 3: Transaction Best Practices (5 minutes)

### Exercise 3: Best Practices Implementation

Create `Lab07BestPractices.java`:

```java
package com.example.ignite;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.cache.CacheAtomicityMode;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.transactions.Transaction;
import org.apache.ignite.transactions.TransactionConcurrency;
import org.apache.ignite.transactions.TransactionIsolation;

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
            java.util.Map<Integer, String> batch = new java.util.HashMap<>();
            for (int i = 10; i < 20; i++) {
                batch.put(i, "Batch-" + i);
            }
            cache.putAll(batch);

            tx.commit();
            System.out.println("   Batch of 10 entries committed\n");
        }
    }
}
```

## Part 4: Transaction Isolation Comparison (10 minutes)

### Exercise 4: Comparing Isolation Levels with Concurrent Threads

This exercise demonstrates the practical differences between READ_COMMITTED, REPEATABLE_READ, and SERIALIZABLE isolation levels using concurrent threads.

Create `Lab07IsolationComparison.java`:

```java
package com.example.ignite;

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
```

**Key Observations:**
- **READ_COMMITTED**: Fastest but may see inconsistent reads
- **REPEATABLE_READ**: Prevents non-repeatable reads with locks
- **SERIALIZABLE**: Strictest but may have more transaction failures

## Part 5: Deadlock Handling (10 minutes)

### Exercise 5: Deliberate Deadlock and Retry Logic

This exercise demonstrates how to create a deadlock scenario, handle `TransactionDeadlockException`, and implement retry logic with exponential backoff.

Create `Lab07DeadlockHandling.java`:

```java
package com.example.ignite;

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
```

**Key Takeaways:**
1. Ignite automatically detects deadlocks and rolls back one transaction
2. Always implement retry logic for production code
3. Use exponential backoff with jitter to prevent retry storms
4. Consistent lock ordering prevents deadlocks entirely
5. Short timeouts help detect and recover from deadlocks quickly

## Part 6: Cross-Cache Transactions (10 minutes)

### Exercise 6: Atomic Operations Across Multiple Caches

This exercise demonstrates how to perform atomic transactions that span multiple caches, ensuring all-or-nothing behavior across cache boundaries.

Create `Lab07CrossCacheTransactions.java`:

```java
package com.example.ignite;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.cache.CacheAtomicityMode;
import org.apache.ignite.cache.CacheMode;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.transactions.Transaction;
import org.apache.ignite.transactions.TransactionConcurrency;
import org.apache.ignite.transactions.TransactionIsolation;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Lab07CrossCacheTransactions {

    public static void main(String[] args) {
        try (Ignite ignite = Ignition.start()) {
            System.out.println("=== Cross-Cache Transactions Lab ===\n");

            // Create multiple transactional caches
            IgniteCache<Integer, Account> accountCache = createCache(ignite,
                "accounts", Account.class);
            IgniteCache<String, OrderRecord> orderCache = createCache(ignite,
                "orders", OrderRecord.class);
            IgniteCache<Integer, InventoryItem> inventoryCache = createCache(ignite,
                "inventory", InventoryItem.class);
            IgniteCache<String, AuditLog> auditCache = createCache(ignite,
                "audit", AuditLog.class);

            // Initialize test data
            initializeData(accountCache, inventoryCache);

            // Part 1: Successful cross-cache transaction
            System.out.println("=== Part 1: Successful Cross-Cache Transaction ===");
            performSuccessfulOrder(ignite, accountCache, orderCache,
                                  inventoryCache, auditCache);

            // Part 2: Failed transaction with rollback
            System.out.println("\n=== Part 2: Cross-Cache Rollback ===");
            demonstrateRollback(ignite, accountCache, orderCache,
                               inventoryCache, auditCache);

            // Part 3: Complex multi-step transaction
            System.out.println("\n=== Part 3: Complex Multi-Step Transaction ===");
            performComplexTransaction(ignite, accountCache, orderCache,
                                      inventoryCache, auditCache);

            // Part 4: Verify atomicity across caches
            System.out.println("\n=== Part 4: Atomicity Verification ===");
            verifyAtomicity(ignite, accountCache, inventoryCache);

            System.out.println("\nPress Enter to exit...");
            System.in.read();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static <K, V> IgniteCache<K, V> createCache(Ignite ignite,
                                                         String name,
                                                         Class<V> valueClass) {
        CacheConfiguration<K, V> cfg = new CacheConfiguration<>(name);
        cfg.setAtomicityMode(CacheAtomicityMode.TRANSACTIONAL);
        cfg.setCacheMode(CacheMode.PARTITIONED);
        cfg.setBackups(1);
        return ignite.getOrCreateCache(cfg);
    }

    private static void initializeData(IgniteCache<Integer, Account> accountCache,
                                        IgniteCache<Integer, InventoryItem> inventoryCache) {
        // Create accounts
        accountCache.put(1, new Account(1, "Alice", 1000.00));
        accountCache.put(2, new Account(2, "Bob", 500.00));
        accountCache.put(3, new Account(3, "Charlie", 2000.00));

        // Create inventory
        inventoryCache.put(101, new InventoryItem(101, "Laptop", 10, 599.99));
        inventoryCache.put(102, new InventoryItem(102, "Mouse", 50, 29.99));
        inventoryCache.put(103, new InventoryItem(103, "Keyboard", 30, 79.99));

        System.out.println("Initial Data:");
        System.out.println("  Accounts: Alice($1000), Bob($500), Charlie($2000)");
        System.out.println("  Inventory: Laptop(10), Mouse(50), Keyboard(30)\n");
    }

    private static void performSuccessfulOrder(Ignite ignite,
                                                IgniteCache<Integer, Account> accountCache,
                                                IgniteCache<String, OrderRecord> orderCache,
                                                IgniteCache<Integer, InventoryItem> inventoryCache,
                                                IgniteCache<String, AuditLog> auditCache) {

        int customerId = 1;
        int productId = 102; // Mouse
        int quantity = 2;

        System.out.println("Alice ordering 2 mice...\n");

        try (Transaction tx = ignite.transactions().txStart(
                TransactionConcurrency.PESSIMISTIC,
                TransactionIsolation.REPEATABLE_READ)) {

            // Step 1: Check and update inventory
            InventoryItem item = inventoryCache.get(productId);
            if (item.getQuantity() < quantity) {
                tx.rollback();
                System.out.println("Insufficient inventory!");
                return;
            }

            double totalCost = item.getPrice() * quantity;
            System.out.println("  [Inventory] Reserving " + quantity + " " + item.getName());
            item.setQuantity(item.getQuantity() - quantity);
            inventoryCache.put(productId, item);

            // Step 2: Check and update account
            Account account = accountCache.get(customerId);
            if (account.getBalance() < totalCost) {
                tx.rollback();
                System.out.println("Insufficient funds!");
                return;
            }

            System.out.println("  [Account] Charging $" + totalCost + " to " + account.getName());
            account.setBalance(account.getBalance() - totalCost);
            accountCache.put(customerId, account);

            // Step 3: Create order record
            String orderId = UUID.randomUUID().toString().substring(0, 8);
            OrderRecord order = new OrderRecord(orderId, customerId, productId,
                                                quantity, totalCost, "COMPLETED");
            System.out.println("  [Order] Creating order " + orderId);
            orderCache.put(orderId, order);

            // Step 4: Create audit log
            String auditId = UUID.randomUUID().toString().substring(0, 8);
            AuditLog audit = new AuditLog(auditId, "ORDER_PLACED",
                "Customer " + customerId + " ordered " + quantity + " x " + item.getName());
            System.out.println("  [Audit] Recording transaction");
            auditCache.put(auditId, audit);

            // Commit all changes atomically
            tx.commit();
            System.out.println("\nTransaction committed successfully!");
            System.out.println("  New balance: $" + accountCache.get(customerId).getBalance());
            System.out.println("  New inventory: " + inventoryCache.get(productId).getQuantity() +
                             " " + item.getName() + "(s)");

        } catch (Exception e) {
            System.out.println("Transaction failed: " + e.getMessage());
        }
    }

    private static void demonstrateRollback(Ignite ignite,
                                            IgniteCache<Integer, Account> accountCache,
                                            IgniteCache<String, OrderRecord> orderCache,
                                            IgniteCache<Integer, InventoryItem> inventoryCache,
                                            IgniteCache<String, AuditLog> auditCache) {

        int customerId = 2; // Bob with $500
        int productId = 101; // Laptop at $599.99
        int quantity = 1;

        System.out.println("Bob attempting to order 1 laptop ($599.99)...");
        System.out.println("Bob's balance: $" + accountCache.get(customerId).getBalance() + "\n");

        // Store initial states for verification
        double initialBalance = accountCache.get(customerId).getBalance();
        int initialInventory = inventoryCache.get(productId).getQuantity();

        try (Transaction tx = ignite.transactions().txStart(
                TransactionConcurrency.PESSIMISTIC,
                TransactionIsolation.REPEATABLE_READ)) {

            // Step 1: Update inventory (this will succeed)
            InventoryItem item = inventoryCache.get(productId);
            double totalCost = item.getPrice() * quantity;

            System.out.println("  [Inventory] Reserving " + quantity + " " + item.getName());
            item.setQuantity(item.getQuantity() - quantity);
            inventoryCache.put(productId, item);

            // Step 2: Try to update account (this will fail)
            Account account = accountCache.get(customerId);
            System.out.println("  [Account] Checking balance: $" + account.getBalance());

            if (account.getBalance() < totalCost) {
                System.out.println("  [Account] INSUFFICIENT FUNDS!");
                System.out.println("  [Transaction] Rolling back ALL changes...");
                tx.rollback();

                // Inventory should be restored!
                System.out.println("\nRollback completed!");
                return;
            }

            account.setBalance(account.getBalance() - totalCost);
            accountCache.put(customerId, account);

            tx.commit();

        } catch (Exception e) {
            System.out.println("Transaction failed: " + e.getMessage());
        }

        // Verify rollback worked
        double finalBalance = accountCache.get(customerId).getBalance();
        int finalInventory = inventoryCache.get(productId).getQuantity();

        System.out.println("\nVerifying rollback:");
        System.out.println("  Account balance: $" + initialBalance + " -> $" + finalBalance +
                         (initialBalance == finalBalance ? " (UNCHANGED)" : " (CHANGED!)"));
        System.out.println("  Laptop inventory: " + initialInventory + " -> " + finalInventory +
                         (initialInventory == finalInventory ? " (UNCHANGED)" : " (CHANGED!)"));

        if (initialBalance == finalBalance && initialInventory == finalInventory) {
            System.out.println("\n  ATOMICITY VERIFIED: All caches rolled back together!");
        }
    }

    private static void performComplexTransaction(Ignite ignite,
                                                   IgniteCache<Integer, Account> accountCache,
                                                   IgniteCache<String, OrderRecord> orderCache,
                                                   IgniteCache<Integer, InventoryItem> inventoryCache,
                                                   IgniteCache<String, AuditLog> auditCache) {

        System.out.println("Complex transaction: Transfer + Purchase + Logging\n");

        try (Transaction tx = ignite.transactions().txStart(
                TransactionConcurrency.PESSIMISTIC,
                TransactionIsolation.REPEATABLE_READ)) {

            // Step 1: Transfer money from Charlie to Alice
            Account charlie = accountCache.get(3);
            Account alice = accountCache.get(1);
            double transferAmount = 100.00;

            System.out.println("  Step 1: Transfer $" + transferAmount +
                             " from Charlie to Alice");
            charlie.setBalance(charlie.getBalance() - transferAmount);
            alice.setBalance(alice.getBalance() + transferAmount);
            accountCache.put(3, charlie);
            accountCache.put(1, alice);

            // Log the transfer
            String transferAuditId = UUID.randomUUID().toString().substring(0, 8);
            auditCache.put(transferAuditId, new AuditLog(transferAuditId, "TRANSFER",
                "Charlie -> Alice: $" + transferAmount));

            // Step 2: Alice purchases a keyboard
            int productId = 103;
            InventoryItem keyboard = inventoryCache.get(productId);
            double cost = keyboard.getPrice();

            System.out.println("  Step 2: Alice purchases keyboard ($" + cost + ")");
            keyboard.setQuantity(keyboard.getQuantity() - 1);
            inventoryCache.put(productId, keyboard);

            alice = accountCache.get(1); // Re-read to get updated balance
            alice.setBalance(alice.getBalance() - cost);
            accountCache.put(1, alice);

            // Create order
            String orderId = UUID.randomUUID().toString().substring(0, 8);
            orderCache.put(orderId, new OrderRecord(orderId, 1, productId, 1, cost, "COMPLETED"));

            // Log the purchase
            String purchaseAuditId = UUID.randomUUID().toString().substring(0, 8);
            auditCache.put(purchaseAuditId, new AuditLog(purchaseAuditId, "PURCHASE",
                "Alice bought keyboard for $" + cost));

            System.out.println("  Step 3: Recording audit logs");

            tx.commit();
            System.out.println("\nComplex transaction committed!");
            System.out.println("  Alice's final balance: $" + accountCache.get(1).getBalance());
            System.out.println("  Charlie's final balance: $" + accountCache.get(3).getBalance());
            System.out.println("  Keyboards remaining: " + inventoryCache.get(103).getQuantity());

        } catch (Exception e) {
            System.out.println("Transaction failed: " + e.getMessage());
        }
    }

    private static void verifyAtomicity(Ignite ignite,
                                        IgniteCache<Integer, Account> accountCache,
                                        IgniteCache<Integer, InventoryItem> inventoryCache) {

        System.out.println("Testing atomicity with simulated failure...\n");

        // Store initial states
        double aliceBalance = accountCache.get(1).getBalance();
        int laptopQty = inventoryCache.get(101).getQuantity();

        System.out.println("Before transaction:");
        System.out.println("  Alice's balance: $" + aliceBalance);
        System.out.println("  Laptop quantity: " + laptopQty);

        try (Transaction tx = ignite.transactions().txStart(
                TransactionConcurrency.PESSIMISTIC,
                TransactionIsolation.REPEATABLE_READ)) {

            // Modify account
            Account alice = accountCache.get(1);
            alice.setBalance(alice.getBalance() - 100);
            accountCache.put(1, alice);
            System.out.println("\n  Modified account (not yet committed)");

            // Modify inventory
            InventoryItem laptop = inventoryCache.get(101);
            laptop.setQuantity(laptop.getQuantity() - 1);
            inventoryCache.put(101, laptop);
            System.out.println("  Modified inventory (not yet committed)");

            // Simulate failure before commit
            System.out.println("\n  Simulating failure...");
            throw new RuntimeException("Simulated system failure!");

        } catch (RuntimeException e) {
            System.out.println("  Exception: " + e.getMessage());
            System.out.println("  Transaction automatically rolled back");
        }

        // Verify nothing changed
        double finalBalance = accountCache.get(1).getBalance();
        int finalQty = inventoryCache.get(101).getQuantity();

        System.out.println("\nAfter rollback:");
        System.out.println("  Alice's balance: $" + finalBalance +
                         (aliceBalance == finalBalance ? " (UNCHANGED)" : " (CHANGED!)"));
        System.out.println("  Laptop quantity: " + finalQty +
                         (laptopQty == finalQty ? " (UNCHANGED)" : " (CHANGED!)"));

        if (aliceBalance == finalBalance && laptopQty == finalQty) {
            System.out.println("\n  CROSS-CACHE ATOMICITY CONFIRMED!");
            System.out.println("  Both caches remain consistent after rollback.");
        }
    }

    // Domain classes
    static class Account implements Serializable {
        private int id;
        private String name;
        private double balance;

        public Account(int id, String name, double balance) {
            this.id = id;
            this.name = name;
            this.balance = balance;
        }

        public int getId() { return id; }
        public String getName() { return name; }
        public double getBalance() { return balance; }
        public void setBalance(double balance) { this.balance = balance; }
    }

    static class InventoryItem implements Serializable {
        private int id;
        private String name;
        private int quantity;
        private double price;

        public InventoryItem(int id, String name, int quantity, double price) {
            this.id = id;
            this.name = name;
            this.quantity = quantity;
            this.price = price;
        }

        public int getId() { return id; }
        public String getName() { return name; }
        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }
        public double getPrice() { return price; }
    }

    static class OrderRecord implements Serializable {
        private String orderId;
        private int customerId;
        private int productId;
        private int quantity;
        private double total;
        private String status;

        public OrderRecord(String orderId, int customerId, int productId,
                          int quantity, double total, String status) {
            this.orderId = orderId;
            this.customerId = customerId;
            this.productId = productId;
            this.quantity = quantity;
            this.total = total;
            this.status = status;
        }

        public String getOrderId() { return orderId; }
    }

    static class AuditLog implements Serializable {
        private String id;
        private String action;
        private String details;
        private String timestamp;

        public AuditLog(String id, String action, String details) {
            this.id = id;
            this.action = action;
            this.details = details;
            this.timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        }

        public String getId() { return id; }
        public String getAction() { return action; }
        public String getDetails() { return details; }
    }
}
```

**Key Concepts:**
1. All caches must have `CacheAtomicityMode.TRANSACTIONAL`
2. A single transaction can span multiple caches
3. Rollback affects all caches in the transaction
4. Use consistent ordering when accessing multiple caches to avoid deadlocks

## Part 7: Challenge Exercises (10 minutes)

### Challenge 1: Bank Transfer with Audit Log

Implement a complete bank transfer system with full audit trail:

Create `Lab07BankTransfer.java`:

```java
package com.example.ignite;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.cache.CacheAtomicityMode;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.transactions.Transaction;
import org.apache.ignite.transactions.TransactionConcurrency;
import org.apache.ignite.transactions.TransactionIsolation;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public class Lab07BankTransfer {

    private static final AtomicLong transactionCounter = new AtomicLong(0);

    public static void main(String[] args) {
        try (Ignite ignite = Ignition.start()) {
            System.out.println("=== Bank Transfer Challenge ===\n");

            // Create caches
            IgniteCache<String, BankAccount> accounts = createCache(ignite,
                "bankAccounts", BankAccount.class);
            IgniteCache<Long, TransferRecord> transfers = createCache(ignite,
                "transferRecords", TransferRecord.class);
            IgniteCache<Long, AuditEntry> auditLog = createCache(ignite,
                "bankAudit", AuditEntry.class);

            // Initialize accounts
            accounts.put("ACC001", new BankAccount("ACC001", "John Doe", 5000.00));
            accounts.put("ACC002", new BankAccount("ACC002", "Jane Smith", 3000.00));
            accounts.put("ACC003", new BankAccount("ACC003", "Bob Wilson", 1000.00));

            System.out.println("Initial Account Balances:");
            printAllAccounts(accounts);

            // Create transfer service
            TransferService service = new TransferService(ignite, accounts,
                                                          transfers, auditLog);

            // Test successful transfer
            System.out.println("\n=== Test 1: Successful Transfer ===");
            TransferResult result1 = service.transfer("ACC001", "ACC002", 500.00,
                                                       "Payment for services");
            System.out.println("Result: " + result1);
            printAllAccounts(accounts);

            // Test insufficient funds
            System.out.println("\n=== Test 2: Insufficient Funds ===");
            TransferResult result2 = service.transfer("ACC003", "ACC001", 2000.00,
                                                       "Large payment");
            System.out.println("Result: " + result2);
            printAllAccounts(accounts);

            // Test same account transfer
            System.out.println("\n=== Test 3: Same Account Transfer ===");
            TransferResult result3 = service.transfer("ACC001", "ACC001", 100.00,
                                                       "Self transfer");
            System.out.println("Result: " + result3);

            // Print audit log
            System.out.println("\n=== Audit Log ===");
            auditLog.forEach(entry -> {
                AuditEntry audit = entry.getValue();
                System.out.println(String.format("  [%s] %s - %s",
                    audit.getTimestamp(), audit.getAction(), audit.getDetails()));
            });

            // Print transfer history
            System.out.println("\n=== Transfer History ===");
            transfers.forEach(entry -> {
                TransferRecord transfer = entry.getValue();
                System.out.println(String.format("  #%d: %s -> %s, $%.2f (%s)",
                    transfer.getId(), transfer.getFromAccount(),
                    transfer.getToAccount(), transfer.getAmount(),
                    transfer.getStatus()));
            });

            System.out.println("\nPress Enter to exit...");
            System.in.read();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static <K, V> IgniteCache<K, V> createCache(Ignite ignite,
                                                         String name,
                                                         Class<V> valueClass) {
        CacheConfiguration<K, V> cfg = new CacheConfiguration<>(name);
        cfg.setAtomicityMode(CacheAtomicityMode.TRANSACTIONAL);
        return ignite.getOrCreateCache(cfg);
    }

    private static void printAllAccounts(IgniteCache<String, BankAccount> accounts) {
        accounts.forEach(entry -> {
            BankAccount acc = entry.getValue();
            System.out.println(String.format("  %s (%s): $%.2f",
                acc.getAccountNumber(), acc.getOwnerName(), acc.getBalance()));
        });
    }

    static class TransferService {
        private final Ignite ignite;
        private final IgniteCache<String, BankAccount> accounts;
        private final IgniteCache<Long, TransferRecord> transfers;
        private final IgniteCache<Long, AuditEntry> auditLog;

        public TransferService(Ignite ignite,
                              IgniteCache<String, BankAccount> accounts,
                              IgniteCache<Long, TransferRecord> transfers,
                              IgniteCache<Long, AuditEntry> auditLog) {
            this.ignite = ignite;
            this.accounts = accounts;
            this.transfers = transfers;
            this.auditLog = auditLog;
        }

        public TransferResult transfer(String fromAcct, String toAcct,
                                       double amount, String description) {
            // Validate input
            if (fromAcct.equals(toAcct)) {
                return new TransferResult(false, "Cannot transfer to same account");
            }
            if (amount <= 0) {
                return new TransferResult(false, "Amount must be positive");
            }

            long transferId = transactionCounter.incrementAndGet();
            String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

            try (Transaction tx = ignite.transactions().txStart(
                    TransactionConcurrency.PESSIMISTIC,
                    TransactionIsolation.REPEATABLE_READ,
                    5000, 0)) {

                // Lock accounts in consistent order to prevent deadlocks
                String firstAcct = fromAcct.compareTo(toAcct) < 0 ? fromAcct : toAcct;
                String secondAcct = fromAcct.compareTo(toAcct) < 0 ? toAcct : fromAcct;

                BankAccount first = accounts.get(firstAcct);
                BankAccount second = accounts.get(secondAcct);

                if (first == null || second == null) {
                    tx.rollback();
                    recordAudit("TRANSFER_FAILED",
                        "Account not found: " + (first == null ? firstAcct : secondAcct));
                    return new TransferResult(false, "Account not found");
                }

                BankAccount source = fromAcct.equals(firstAcct) ? first : second;
                BankAccount target = toAcct.equals(firstAcct) ? first : second;

                // Check balance
                if (source.getBalance() < amount) {
                    tx.rollback();
                    recordTransfer(transferId, fromAcct, toAcct, amount,
                                  description, "FAILED_INSUFFICIENT_FUNDS");
                    recordAudit("TRANSFER_FAILED",
                        String.format("Insufficient funds: %s has $%.2f, needed $%.2f",
                            fromAcct, source.getBalance(), amount));
                    return new TransferResult(false, "Insufficient funds");
                }

                // Perform transfer
                source.setBalance(source.getBalance() - amount);
                target.setBalance(target.getBalance() + amount);

                accounts.put(source.getAccountNumber(), source);
                accounts.put(target.getAccountNumber(), target);

                // Record transfer
                recordTransfer(transferId, fromAcct, toAcct, amount,
                              description, "COMPLETED");

                // Record audit entries
                recordAudit("DEBIT", String.format("%s debited $%.2f for: %s",
                    fromAcct, amount, description));
                recordAudit("CREDIT", String.format("%s credited $%.2f from: %s",
                    toAcct, amount, fromAcct));
                recordAudit("TRANSFER_COMPLETED",
                    String.format("Transfer #%d: $%.2f from %s to %s",
                        transferId, amount, fromAcct, toAcct));

                tx.commit();
                return new TransferResult(true, "Transfer completed successfully",
                                         transferId);

            } catch (Exception e) {
                recordAudit("TRANSFER_ERROR",
                    String.format("Transfer failed with error: %s", e.getMessage()));
                return new TransferResult(false, "Transfer failed: " + e.getMessage());
            }
        }

        private void recordTransfer(long id, String from, String to,
                                    double amount, String desc, String status) {
            transfers.put(id, new TransferRecord(id, from, to, amount, desc, status));
        }

        private void recordAudit(String action, String details) {
            long auditId = System.nanoTime();
            auditLog.put(auditId, new AuditEntry(auditId, action, details));
        }
    }

    static class BankAccount implements Serializable {
        private String accountNumber;
        private String ownerName;
        private double balance;

        public BankAccount(String accountNumber, String ownerName, double balance) {
            this.accountNumber = accountNumber;
            this.ownerName = ownerName;
            this.balance = balance;
        }

        public String getAccountNumber() { return accountNumber; }
        public String getOwnerName() { return ownerName; }
        public double getBalance() { return balance; }
        public void setBalance(double balance) { this.balance = balance; }
    }

    static class TransferRecord implements Serializable {
        private long id;
        private String fromAccount;
        private String toAccount;
        private double amount;
        private String description;
        private String status;
        private String timestamp;

        public TransferRecord(long id, String from, String to,
                             double amount, String desc, String status) {
            this.id = id;
            this.fromAccount = from;
            this.toAccount = to;
            this.amount = amount;
            this.description = desc;
            this.status = status;
            this.timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        }

        public long getId() { return id; }
        public String getFromAccount() { return fromAccount; }
        public String getToAccount() { return toAccount; }
        public double getAmount() { return amount; }
        public String getStatus() { return status; }
    }

    static class AuditEntry implements Serializable {
        private long id;
        private String action;
        private String details;
        private String timestamp;

        public AuditEntry(long id, String action, String details) {
            this.id = id;
            this.action = action;
            this.details = details;
            this.timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        }

        public String getAction() { return action; }
        public String getDetails() { return details; }
        public String getTimestamp() { return timestamp; }
    }

    static class TransferResult {
        private boolean success;
        private String message;
        private Long transferId;

        public TransferResult(boolean success, String message) {
            this(success, message, null);
        }

        public TransferResult(boolean success, String message, Long transferId) {
            this.success = success;
            this.message = message;
            this.transferId = transferId;
        }

        @Override
        public String toString() {
            return String.format("%s: %s%s",
                success ? "SUCCESS" : "FAILED",
                message,
                transferId != null ? " (Transfer #" + transferId + ")" : "");
        }
    }
}
```

### Challenge 2: Transaction Monitoring Utility

Create `Lab07TransactionMonitor.java`:

```java
package com.example.ignite;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.IgniteTransactions;
import org.apache.ignite.Ignition;
import org.apache.ignite.cache.CacheAtomicityMode;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.transactions.Transaction;
import org.apache.ignite.transactions.TransactionConcurrency;
import org.apache.ignite.transactions.TransactionIsolation;
import org.apache.ignite.transactions.TransactionMetrics;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class Lab07TransactionMonitor {

    public static void main(String[] args) {
        try (Ignite ignite = Ignition.start()) {
            System.out.println("=== Transaction Monitoring Utility ===\n");

            CacheConfiguration<Integer, Integer> cfg =
                new CacheConfiguration<>("monitoredCache");
            cfg.setAtomicityMode(CacheAtomicityMode.TRANSACTIONAL);

            IgniteCache<Integer, Integer> cache = ignite.getOrCreateCache(cfg);
            cache.put(1, 0);

            // Create monitoring utility
            TransactionMonitor monitor = new TransactionMonitor(ignite);

            // Start monitoring thread
            Thread monitorThread = new Thread(() -> {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        monitor.printMetrics();
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            }, "MonitorThread");
            monitorThread.start();

            // Generate transaction load
            System.out.println("Generating transaction load...\n");
            generateLoad(ignite, cache);

            // Final metrics
            Thread.sleep(1000);
            monitorThread.interrupt();
            monitorThread.join();

            System.out.println("\n=== Final Metrics ===");
            monitor.printDetailedMetrics();

            System.out.println("\nPress Enter to exit...");
            System.in.read();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void generateLoad(Ignite ignite, IgniteCache<Integer, Integer> cache) {
        int numThreads = 5;
        int transactionsPerThread = 20;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(numThreads);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        for (int i = 0; i < numThreads; i++) {
            final int threadNum = i;
            executor.submit(() -> {
                for (int j = 0; j < transactionsPerThread; j++) {
                    try (Transaction tx = ignite.transactions().txStart(
                            TransactionConcurrency.PESSIMISTIC,
                            TransactionIsolation.REPEATABLE_READ,
                            1000, 0)) {

                        Integer value = cache.get(1);
                        Thread.sleep(10 + (int)(Math.random() * 20));
                        cache.put(1, value + 1);

                        if (Math.random() > 0.9) {
                            tx.rollback();
                            failCount.incrementAndGet();
                        } else {
                            tx.commit();
                            successCount.incrementAndGet();
                        }

                    } catch (Exception e) {
                        failCount.incrementAndGet();
                    }
                }
                latch.countDown();
            });
        }

        try {
            latch.await(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        executor.shutdown();
        System.out.println("\nLoad generation complete.");
        System.out.println("  Successful transactions: " + successCount.get());
        System.out.println("  Failed transactions: " + failCount.get());
        System.out.println("  Final counter value: " + cache.get(1));
    }

    static class TransactionMonitor {
        private final Ignite ignite;
        private long lastCommits = 0;
        private long lastRollbacks = 0;
        private long lastTimestamp = System.currentTimeMillis();

        public TransactionMonitor(Ignite ignite) {
            this.ignite = ignite;
        }

        public void printMetrics() {
            IgniteTransactions transactions = ignite.transactions();
            TransactionMetrics metrics = transactions.metrics();

            long currentCommits = metrics.txCommits();
            long currentRollbacks = metrics.txRollbacks();
            long currentTimestamp = System.currentTimeMillis();

            long commitsDelta = currentCommits - lastCommits;
            long rollbacksDelta = currentRollbacks - lastRollbacks;
            long timeDelta = currentTimestamp - lastTimestamp;

            double commitsPerSecond = (commitsDelta * 1000.0) / timeDelta;
            double rollbacksPerSecond = (rollbacksDelta * 1000.0) / timeDelta;

            System.out.println(String.format(
                "[Monitor] Commits: %d (%.1f/s) | Rollbacks: %d (%.1f/s)",
                currentCommits, commitsPerSecond,
                currentRollbacks, rollbacksPerSecond));

            lastCommits = currentCommits;
            lastRollbacks = currentRollbacks;
            lastTimestamp = currentTimestamp;
        }

        public void printDetailedMetrics() {
            TransactionMetrics metrics = ignite.transactions().metrics();

            System.out.println("Total Commits:        " + metrics.txCommits());
            System.out.println("Total Rollbacks:      " + metrics.txRollbacks());
            System.out.println("Commit Time (avg):    " +
                (metrics.commitTime() / Math.max(1, metrics.txCommits())) + "ms");
            System.out.println("Rollback Time (avg):  " +
                (metrics.rollbackTime() / Math.max(1, metrics.txRollbacks())) + "ms");
        }
    }
}
```

### Challenge 3: Optimistic Retry Wrapper

Create `Lab07OptimisticRetryWrapper.java`:

```java
package com.example.ignite;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.cache.CacheAtomicityMode;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.transactions.Transaction;
import org.apache.ignite.transactions.TransactionConcurrency;
import org.apache.ignite.transactions.TransactionIsolation;
import org.apache.ignite.transactions.TransactionOptimisticException;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;

public class Lab07OptimisticRetryWrapper {

    public static void main(String[] args) {
        try (Ignite ignite = Ignition.start()) {
            System.out.println("=== Optimistic Retry Wrapper Challenge ===\n");

            CacheConfiguration<String, Integer> cfg =
                new CacheConfiguration<>("optimisticCache");
            cfg.setAtomicityMode(CacheAtomicityMode.TRANSACTIONAL);

            IgniteCache<String, Integer> cache = ignite.getOrCreateCache(cfg);
            cache.put("counter", 0);

            // Create the wrapper
            OptimisticTransactionWrapper<Integer> wrapper =
                new OptimisticTransactionWrapper<>(ignite);

            // Test 1: Single operation with retry
            System.out.println("=== Test 1: Single Retry Wrapper ===");
            Integer result = wrapper.execute(tx -> {
                Integer value = cache.get("counter");
                cache.put("counter", value + 10);
                return cache.get("counter");
            });
            System.out.println("Result: " + result);
            System.out.println("Counter value: " + cache.get("counter"));

            // Test 2: Concurrent operations with automatic retry
            System.out.println("\n=== Test 2: Concurrent Operations with Retry ===");
            cache.put("sharedValue", 0);

            int numThreads = 10;
            int incrementsPerThread = 50;
            ExecutorService executor = Executors.newFixedThreadPool(numThreads);
            CountDownLatch latch = new CountDownLatch(numThreads);
            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger totalRetries = new AtomicInteger(0);

            for (int i = 0; i < numThreads; i++) {
                executor.submit(() -> {
                    for (int j = 0; j < incrementsPerThread; j++) {
                        OptimisticTransactionWrapper<Void>.ExecutionResult<Void> execResult =
                            wrapper.executeWithStats(tx -> {
                                Integer value = cache.get("sharedValue");
                                // Small delay to increase conflict chance
                                try { Thread.sleep(1); } catch (InterruptedException e) {}
                                cache.put("sharedValue", value + 1);
                                return null;
                            });

                        if (execResult.isSuccess()) {
                            successCount.incrementAndGet();
                            totalRetries.addAndGet(execResult.getRetryCount());
                        }
                    }
                    latch.countDown();
                });
            }

            latch.await(60, TimeUnit.SECONDS);
            executor.shutdown();

            int expected = numThreads * incrementsPerThread;
            Integer actual = cache.get("sharedValue");

            System.out.println("\nResults:");
            System.out.println("  Expected final value: " + expected);
            System.out.println("  Actual final value:   " + actual);
            System.out.println("  Successful operations: " + successCount.get());
            System.out.println("  Total retries needed:  " + totalRetries.get());
            System.out.println("  Correctness: " + (expected == actual ? "PASSED" : "FAILED"));

            // Test 3: Custom retry configuration
            System.out.println("\n=== Test 3: Custom Retry Configuration ===");
            OptimisticTransactionWrapper<String> customWrapper =
                new OptimisticTransactionWrapper<>(ignite)
                    .withMaxRetries(10)
                    .withBaseBackoff(25)
                    .withMaxBackoff(2000)
                    .withRetryCallback(attempt ->
                        System.out.println("  Custom callback: retry attempt " + attempt));

            cache.put("testKey", 100);
            String testResult = customWrapper.execute(tx -> {
                Integer value = cache.get("testKey");
                cache.put("testKey", value * 2);
                return "Doubled value to: " + cache.get("testKey");
            });
            System.out.println("Result: " + testResult);

            System.out.println("\nPress Enter to exit...");
            System.in.read();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static class OptimisticTransactionWrapper<T> {
        private final Ignite ignite;
        private int maxRetries = 5;
        private long baseBackoffMs = 50;
        private long maxBackoffMs = 5000;
        private Consumer<Integer> retryCallback = attempt -> {};

        public OptimisticTransactionWrapper(Ignite ignite) {
            this.ignite = ignite;
        }

        public OptimisticTransactionWrapper<T> withMaxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
            return this;
        }

        public OptimisticTransactionWrapper<T> withBaseBackoff(long baseBackoffMs) {
            this.baseBackoffMs = baseBackoffMs;
            return this;
        }

        public OptimisticTransactionWrapper<T> withMaxBackoff(long maxBackoffMs) {
            this.maxBackoffMs = maxBackoffMs;
            return this;
        }

        public OptimisticTransactionWrapper<T> withRetryCallback(Consumer<Integer> callback) {
            this.retryCallback = callback;
            return this;
        }

        public T execute(Function<Transaction, T> operation) {
            ExecutionResult<T> result = executeWithStats(operation);
            if (result.isSuccess()) {
                return result.getValue();
            }
            throw new RuntimeException("Transaction failed after " + maxRetries +
                                       " retries: " + result.getError());
        }

        public ExecutionResult<T> executeWithStats(Function<Transaction, T> operation) {
            int attempt = 0;
            Exception lastError = null;

            while (attempt < maxRetries) {
                try (Transaction tx = ignite.transactions().txStart(
                        TransactionConcurrency.OPTIMISTIC,
                        TransactionIsolation.SERIALIZABLE)) {

                    T result = operation.apply(tx);
                    tx.commit();
                    return new ExecutionResult<>(result, attempt, null);

                } catch (TransactionOptimisticException e) {
                    attempt++;
                    lastError = e;
                    retryCallback.accept(attempt);

                    if (attempt < maxRetries) {
                        long backoff = calculateBackoff(attempt);
                        try {
                            Thread.sleep(backoff);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            return new ExecutionResult<>(null, attempt, ie);
                        }
                    }

                } catch (Exception e) {
                    return new ExecutionResult<>(null, attempt, e);
                }
            }

            return new ExecutionResult<>(null, attempt, lastError);
        }

        private long calculateBackoff(int attempt) {
            long exponentialBackoff = baseBackoffMs * (1L << Math.min(attempt, 10));
            long jitter = (long) (Math.random() * exponentialBackoff * 0.3);
            return Math.min(exponentialBackoff + jitter, maxBackoffMs);
        }

        class ExecutionResult<R> {
            private final R value;
            private final int retryCount;
            private final Exception error;

            ExecutionResult(R value, int retryCount, Exception error) {
                this.value = value;
                this.retryCount = retryCount;
                this.error = error;
            }

            public boolean isSuccess() { return error == null; }
            public R getValue() { return value; }
            public int getRetryCount() { return retryCount; }
            public Exception getError() { return error; }
        }
    }
}
```

**Challenge Exercise Summary:**
1. **Bank Transfer**: Complete transaction system with audit trail
2. **Transaction Monitor**: Real-time metrics and monitoring
3. **Optimistic Retry Wrapper**: Reusable pattern for handling conflicts

## Verification Steps

### Checklist
- [ ] Basic transaction commit/rollback works
- [ ] PESSIMISTIC transactions lock resources immediately
- [ ] OPTIMISTIC transactions validate at commit
- [ ] Different isolation levels understood and compared
- [ ] Deadlock detection demonstrated
- [ ] Retry logic with exponential backoff implemented
- [ ] Cross-cache transactions maintain atomicity
- [ ] Exception handling implemented correctly
- [ ] Best practices applied
- [ ] Challenge exercises completed

### Test Scenarios

1. **Verify Atomicity:**
   - Start transaction
   - Make multiple changes
   - Rollback
   - Verify no changes committed

2. **Test Isolation:**
   - Start long transaction
   - Try to read from another thread
   - Verify isolation level behavior

3. **Test Deadlock Detection:**
   - Create circular lock dependency
   - Observe automatic detection and resolution

4. **Test Cross-Cache Rollback:**
   - Start transaction spanning multiple caches
   - Make changes to all caches
   - Force rollback
   - Verify all caches are unchanged

5. **Test Retry Logic:**
   - Create concurrent conflicting transactions
   - Verify retry mechanism works
   - Check exponential backoff timing

## Lab Questions

1. What is the difference between PESSIMISTIC and OPTIMISTIC concurrency?
2. Which isolation level should you use for most applications?
3. How does Ignite detect and resolve deadlocks?
4. When should you use transactions vs atomic operations?
5. How do you implement retry logic with exponential backoff?
6. What happens when a transaction spanning multiple caches fails?
7. Why is consistent lock ordering important for preventing deadlocks?
8. What are the performance trade-offs between different isolation levels?

## Answers

1. **PESSIMISTIC**: Locks acquired immediately on first access, prevents conflicts but may reduce concurrency. **OPTIMISTIC**: No locks until commit, better concurrency but may fail at commit time.

2. **REPEATABLE_READ** is recommended for most applications - provides good consistency without the overhead of SERIALIZABLE, while being stricter than READ_COMMITTED.

3. Ignite uses a **timeout-based mechanism**. When a deadlock is detected (transaction waiting too long), one transaction is rolled back with TransactionDeadlockException, allowing others to proceed.

4. **Use transactions** when:
   - Multiple related changes must succeed/fail together
   - Need consistency across multiple operations
   - Working with multiple cache entries

   **Use atomic operations** when:
   - Single entry operations
   - No cross-entry dependencies
   - Maximum performance needed

5. **Retry with exponential backoff**:
   - Catch TransactionDeadlockException or TransactionOptimisticException
   - Calculate backoff: base * 2^attempt + random jitter
   - Sleep for backoff duration
   - Retry with maximum retry limit
   - Cap maximum backoff to prevent excessive delays

6. **Cross-cache transaction failure**: When any operation in a multi-cache transaction fails or the transaction is rolled back, ALL changes across ALL caches are reverted. This maintains atomicity across cache boundaries.

7. **Consistent lock ordering** prevents deadlocks because:
   - Deadlocks occur when transactions acquire locks in different orders
   - If all transactions lock resources in the same order (e.g., alphabetically), circular wait cannot occur
   - This eliminates the possibility of deadlock without needing detection/recovery

8. **Performance trade-offs**:
   - **READ_COMMITTED**: Highest throughput, but may see non-repeatable reads; use for high-volume, non-critical operations
   - **REPEATABLE_READ**: Moderate overhead from lock holding; good balance for most applications
   - **SERIALIZABLE**: Highest overhead from validation, more transaction failures under contention; use only when strict consistency is required

## Common Issues

**Issue: Transaction timeout**
- Increase timeout in txStart()
- Reduce work inside transaction
- Check for deadlocks

**Issue: Optimistic transaction always fails**
- Too much contention on same keys
- Switch to PESSIMISTIC
- Implement retry logic

**Issue: Deadlock not detected**
- Verify timeout is set
- Check both transactions use locks
- Ensure using PESSIMISTIC mode

**Issue: Cross-cache transaction partially commits**
- Ensure ALL caches use TRANSACTIONAL atomicity mode
- Verify caches are created before transaction starts
- Check for exceptions that bypass rollback

**Issue: Retry logic causes thundering herd**
- Add random jitter to backoff
- Use different base backoff per instance
- Consider limiting concurrent retries

## Next Steps

In Lab 8, you will:
- Implement advanced caching patterns
- Use near caches
- Configure expiry policies
- Work with cache entry processors
- Handle events and continuous queries

## Completion

You have completed Lab 7 when you can:
- Implement both transaction models
- Handle different isolation levels
- Compare isolation level behaviors under concurrent load
- Detect and resolve deadlocks with retry logic
- Perform atomic operations across multiple caches
- Apply transaction best practices
- Choose appropriate transaction settings for different scenarios
- Implement production-ready retry wrappers
