# Lab 7: Transactions and ACID Properties

## Duration: 30 minutes

## Objectives
- Understand transaction concepts in distributed systems
- Implement PESSIMISTIC and OPTIMISTIC transaction models
- Work with different isolation levels
- Handle deadlock detection and resolution
- Learn transaction best practices

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

## Part 2: Transaction Models and Isolation Levels (15 minutes)

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

### Exercise 3: Deadlock Handling

Create `Lab07Deadlock.java`:

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

import java.util.concurrent.CountDownLatch;

public class Lab07Deadlock {

    public static void main(String[] args) {
        IgniteConfiguration cfg = new IgniteConfiguration();
        cfg.setIgniteInstanceName("deadlock-node");

        try (Ignite ignite = Ignition.start(cfg)) {
            System.out.println("=== Deadlock Detection Lab ===\n");

            CacheConfiguration<String, Integer> cacheCfg =
                new CacheConfiguration<>("deadlockCache");
            cacheCfg.setAtomicityMode(CacheAtomicityMode.TRANSACTIONAL);

            IgniteCache<String, Integer> cache = ignite.getOrCreateCache(cacheCfg);

            // Initialize data
            cache.put("resource1", 100);
            cache.put("resource2", 200);

            System.out.println("=== Simulating Deadlock Scenario ===");
            System.out.println("Transaction 1: Lock resource1, then resource2");
            System.out.println("Transaction 2: Lock resource2, then resource1\n");

            CountDownLatch latch = new CountDownLatch(2);

            // Transaction 1
            Thread thread1 = new Thread(() -> {
                try {
                    System.out.println("[TX1] Starting transaction 1...");

                    try (Transaction tx = ignite.transactions().txStart(
                            TransactionConcurrency.PESSIMISTIC,
                            TransactionIsolation.REPEATABLE_READ, 5000, 0)) {

                        System.out.println("[TX1] Locking resource1...");
                        cache.get("resource1");

                        Thread.sleep(100);

                        System.out.println("[TX1] Trying to lock resource2...");
                        cache.get("resource2");

                        System.out.println("[TX1] Both resources locked");
                        tx.commit();
                        System.out.println("[TX1] Transaction committed");
                    }

                } catch (TransactionDeadlockException e) {
                    System.out.println("[TX1] DEADLOCK DETECTED!");
                    System.out.println("[TX1] Transaction will be rolled back");
                } catch (Exception e) {
                    System.out.println("[TX1] Error: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });

            // Transaction 2
            Thread thread2 = new Thread(() -> {
                try {
                    System.out.println("[TX2] Starting transaction 2...");

                    try (Transaction tx = ignite.transactions().txStart(
                            TransactionConcurrency.PESSIMISTIC,
                            TransactionIsolation.REPEATABLE_READ, 5000, 0)) {

                        System.out.println("[TX2] Locking resource2...");
                        cache.get("resource2");

                        Thread.sleep(100);

                        System.out.println("[TX2] Trying to lock resource1...");
                        cache.get("resource1");

                        System.out.println("[TX2] Both resources locked");
                        tx.commit();
                        System.out.println("[TX2] Transaction committed");
                    }

                } catch (TransactionDeadlockException e) {
                    System.out.println("[TX2] DEADLOCK DETECTED!");
                    System.out.println("[TX2] Transaction will be rolled back");
                } catch (Exception e) {
                    System.out.println("[TX2] Error: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });

            // Start both transactions
            thread1.start();
            thread2.start();

            // Wait for completion
            latch.await();

            System.out.println("\n=== Deadlock Resolution ===");
            System.out.println("Ignite automatically detects deadlocks");
            System.out.println("One transaction is rolled back");
            System.out.println("Other transaction can proceed");
            System.out.println("\nBest Practice: Use consistent lock ordering");

            // Demonstrate correct approach
            System.out.println("\n=== Correct Approach: Ordered Locking ===");
            demonstrateOrderedLocking(ignite, cache);

            System.out.println("\nPress Enter to exit...");
            System.in.read();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void demonstrateOrderedLocking(Ignite ignite,
                                                   IgniteCache<String, Integer> cache) {
        CountDownLatch latch = new CountDownLatch(2);

        Runnable task = () -> {
            try {
                String threadName = Thread.currentThread().getName();

                try (Transaction tx = ignite.transactions().txStart(
                        TransactionConcurrency.PESSIMISTIC,
                        TransactionIsolation.REPEATABLE_READ)) {

                    System.out.println("[" + threadName + "] Locking resource1...");
                    cache.get("resource1");

                    Thread.sleep(50);

                    System.out.println("[" + threadName + "] Locking resource2...");
                    cache.get("resource2");

                    tx.commit();
                    System.out.println("[" + threadName + "] Success!");
                }

            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            } finally {
                latch.countDown();
            }
        };

        Thread t1 = new Thread(task, "Ordered-TX1");
        Thread t2 = new Thread(task, "Ordered-TX2");

        t1.start();
        t2.start();

        try {
            latch.await();
            System.out.println("Both transactions completed successfully!");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
```

## Part 3: Transaction Best Practices (5 minutes)

### Exercise 4: Best Practices Implementation

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
            System.out.println("   ✓ Minimize work inside transaction");
            System.out.println("   ✓ Do heavy computation outside transaction");
            System.out.println("   ✗ Avoid long-running operations\n");

            demonstrateShortTransactions(ignite, cache);

            // Best Practice 2: Use appropriate isolation level
            System.out.println("\n2. Choose Appropriate Isolation Level");
            System.out.println("   - READ_COMMITTED: Fastest, less strict");
            System.out.println("   - REPEATABLE_READ: Balance of speed and consistency");
            System.out.println("   - SERIALIZABLE: Slowest, most strict\n");

            // Best Practice 3: Handle exceptions
            System.out.println("3. Always Handle Exceptions");
            System.out.println("   ✓ Use try-with-resources");
            System.out.println("   ✓ Explicit rollback on error");
            System.out.println("   ✓ Clean up resources\n");

            demonstrateExceptionHandling(ignite, cache);

            // Best Practice 4: Avoid nested transactions
            System.out.println("\n4. Avoid Nested Transactions");
            System.out.println("   ✗ Ignite doesn't support nested transactions");
            System.out.println("   ✓ Complete inner transaction before outer\n");

            // Best Practice 5: Use batch operations
            System.out.println("5. Use Batch Operations");
            System.out.println("   ✓ putAll/getAll within transaction");
            System.out.println("   ✓ Reduces network overhead");
            System.out.println("   ✓ Better performance\n");

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

## Verification Steps

### Checklist
- [ ] Basic transaction commit/rollback works
- [ ] PESSIMISTIC transactions lock resources immediately
- [ ] OPTIMISTIC transactions validate at commit
- [ ] Different isolation levels understood
- [ ] Deadlock detection demonstrated
- [ ] Exception handling implemented correctly
- [ ] Best practices applied

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

## Lab Questions

1. What is the difference between PESSIMISTIC and OPTIMISTIC concurrency?
2. Which isolation level should you use for most applications?
3. How does Ignite detect and resolve deadlocks?
4. When should you use transactions vs atomic operations?

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
- Detect and resolve deadlocks
- Apply transaction best practices
- Choose appropriate transaction settings for different scenarios
