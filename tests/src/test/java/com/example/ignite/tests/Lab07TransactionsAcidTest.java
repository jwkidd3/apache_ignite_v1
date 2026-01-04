package com.example.ignite.tests;

import org.apache.ignite.IgniteCache;
import org.apache.ignite.IgniteTransactions;
import org.apache.ignite.cache.CacheAtomicityMode;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.transactions.Transaction;
import org.apache.ignite.transactions.TransactionConcurrency;
import org.apache.ignite.transactions.TransactionIsolation;
import org.apache.ignite.transactions.TransactionOptimisticException;
import org.apache.ignite.transactions.TransactionTimeoutException;
import org.apache.ignite.IgniteException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive tests for Lab 7: Transactions and ACID Properties
 * Coverage: Basic transactions, commit/rollback, pessimistic/optimistic modes,
 * transaction timeout, and cross-cache transactions
 */
@DisplayName("Lab 07: Transactions and ACID Tests")
public class Lab07TransactionsAcidTest extends BaseIgniteTest {

    /**
     * Creates a transactional cache configuration
     */
    private CacheConfiguration<Integer, Integer> createTransactionalCacheConfig(String cacheName) {
        CacheConfiguration<Integer, Integer> cfg = new CacheConfiguration<>(cacheName);
        cfg.setAtomicityMode(CacheAtomicityMode.TRANSACTIONAL);
        cfg.setBackups(0);
        return cfg;
    }

    // ==================== Basic Transaction Operations ====================

    @Test
    @DisplayName("Test basic transaction start and commit")
    public void testBasicTransactionCommit() {
        CacheConfiguration<Integer, Integer> cfg = createTransactionalCacheConfig(getTestCacheName());
        IgniteCache<Integer, Integer> cache = ignite.getOrCreateCache(cfg);

        cache.put(1, 100);

        try (Transaction tx = ignite.transactions().txStart()) {
            cache.put(1, 200);
            tx.commit();
        }

        assertThat(cache.get(1)).isEqualTo(200);
    }

    @Test
    @DisplayName("Test transaction with multiple operations")
    public void testTransactionMultipleOperations() {
        CacheConfiguration<Integer, Integer> cfg = createTransactionalCacheConfig(getTestCacheName());
        IgniteCache<Integer, Integer> cache = ignite.getOrCreateCache(cfg);

        cache.put(1, 1000);
        cache.put(2, 500);

        try (Transaction tx = ignite.transactions().txStart()) {
            int balance1 = cache.get(1);
            int balance2 = cache.get(2);

            // Transfer from account 1 to account 2
            cache.put(1, balance1 - 200);
            cache.put(2, balance2 + 200);

            tx.commit();
        }

        assertThat(cache.get(1)).isEqualTo(800);
        assertThat(cache.get(2)).isEqualTo(700);
    }

    @Test
    @DisplayName("Test transaction get and put operations")
    public void testTransactionGetPut() {
        CacheConfiguration<Integer, Integer> cfg = createTransactionalCacheConfig(getTestCacheName());
        IgniteCache<Integer, Integer> cache = ignite.getOrCreateCache(cfg);

        try (Transaction tx = ignite.transactions().txStart()) {
            cache.put(1, 100);
            cache.put(2, 200);
            cache.put(3, 300);

            assertThat(cache.get(1)).isEqualTo(100);
            assertThat(cache.get(2)).isEqualTo(200);
            assertThat(cache.get(3)).isEqualTo(300);

            tx.commit();
        }

        assertThat(cache.size()).isEqualTo(3);
    }

    // ==================== Transaction Commit and Rollback ====================

    @Test
    @DisplayName("Test explicit transaction rollback")
    public void testTransactionRollback() {
        CacheConfiguration<Integer, Integer> cfg = createTransactionalCacheConfig(getTestCacheName());
        IgniteCache<Integer, Integer> cache = ignite.getOrCreateCache(cfg);

        cache.put(1, 1000);

        try (Transaction tx = ignite.transactions().txStart()) {
            cache.put(1, 500);

            // Explicitly rollback
            tx.rollback();
        }

        // Value should be unchanged
        assertThat(cache.get(1)).isEqualTo(1000);
    }

    @Test
    @DisplayName("Test rollback preserves original values")
    public void testRollbackPreservesOriginalValues() {
        CacheConfiguration<Integer, Integer> cfg = createTransactionalCacheConfig(getTestCacheName());
        IgniteCache<Integer, Integer> cache = ignite.getOrCreateCache(cfg);

        cache.put(1, 100);
        cache.put(2, 200);
        cache.put(3, 300);

        try (Transaction tx = ignite.transactions().txStart()) {
            cache.put(1, 999);
            cache.put(2, 999);
            cache.put(3, 999);

            tx.rollback();
        }

        assertThat(cache.get(1)).isEqualTo(100);
        assertThat(cache.get(2)).isEqualTo(200);
        assertThat(cache.get(3)).isEqualTo(300);
    }

    @Test
    @DisplayName("Test automatic rollback on exception")
    public void testAutomaticRollbackOnException() {
        CacheConfiguration<Integer, Integer> cfg = createTransactionalCacheConfig(getTestCacheName());
        IgniteCache<Integer, Integer> cache = ignite.getOrCreateCache(cfg);

        cache.put(1, 1000);

        try {
            try (Transaction tx = ignite.transactions().txStart()) {
                cache.put(1, 500);

                // Simulate error before commit
                throw new RuntimeException("Simulated error");
            }
        } catch (RuntimeException e) {
            // Expected exception
            assertThat(e.getMessage()).isEqualTo("Simulated error");
        }

        // Transaction should have been rolled back automatically
        assertThat(cache.get(1)).isEqualTo(1000);
    }

    @Test
    @DisplayName("Test conditional rollback based on business logic")
    public void testConditionalRollback() {
        CacheConfiguration<Integer, Integer> cfg = createTransactionalCacheConfig(getTestCacheName());
        IgniteCache<Integer, Integer> cache = ignite.getOrCreateCache(cfg);

        cache.put(1, 500); // Account balance

        boolean transactionCommitted = false;

        try (Transaction tx = ignite.transactions().txStart()) {
            int balance = cache.get(1);

            // Try to withdraw more than available
            int withdrawAmount = 1000;
            if (balance >= withdrawAmount) {
                cache.put(1, balance - withdrawAmount);
                tx.commit();
                transactionCommitted = true;
            } else {
                tx.rollback();
            }
        }

        assertThat(transactionCommitted).isFalse();
        assertThat(cache.get(1)).isEqualTo(500);
    }

    // ==================== Pessimistic Transactions ====================

    @Test
    @DisplayName("Test pessimistic transaction with repeatable read")
    public void testPessimisticRepeatableRead() {
        CacheConfiguration<Integer, Integer> cfg = createTransactionalCacheConfig(getTestCacheName());
        IgniteCache<Integer, Integer> cache = ignite.getOrCreateCache(cfg);

        cache.put(1, 100);

        try (Transaction tx = ignite.transactions().txStart(
                TransactionConcurrency.PESSIMISTIC,
                TransactionIsolation.REPEATABLE_READ)) {

            int value = cache.get(1);
            cache.put(1, value + 50);

            tx.commit();
        }

        assertThat(cache.get(1)).isEqualTo(150);
    }

    @Test
    @DisplayName("Test pessimistic transaction with read committed")
    public void testPessimisticReadCommitted() {
        CacheConfiguration<Integer, Integer> cfg = createTransactionalCacheConfig(getTestCacheName());
        IgniteCache<Integer, Integer> cache = ignite.getOrCreateCache(cfg);

        cache.put(1, 100);

        try (Transaction tx = ignite.transactions().txStart(
                TransactionConcurrency.PESSIMISTIC,
                TransactionIsolation.READ_COMMITTED)) {

            int value = cache.get(1);
            cache.put(1, value * 2);

            tx.commit();
        }

        assertThat(cache.get(1)).isEqualTo(200);
    }

    @Test
    @DisplayName("Test pessimistic transaction lock behavior")
    public void testPessimisticLockBehavior() throws InterruptedException {
        CacheConfiguration<Integer, Integer> cfg = createTransactionalCacheConfig(getTestCacheName());
        IgniteCache<Integer, Integer> cache = ignite.getOrCreateCache(cfg);

        cache.put(1, 0);

        AtomicInteger successCount = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(2);

        // Two threads trying to increment the same value
        Runnable task = () -> {
            try (Transaction tx = ignite.transactions().txStart(
                    TransactionConcurrency.PESSIMISTIC,
                    TransactionIsolation.REPEATABLE_READ)) {

                int value = cache.get(1);
                cache.put(1, value + 1);
                tx.commit();
                successCount.incrementAndGet();
            } finally {
                latch.countDown();
            }
        };

        Thread t1 = new Thread(task);
        Thread t2 = new Thread(task);

        t1.start();
        t2.start();

        latch.await(10, TimeUnit.SECONDS);

        // Both should succeed due to pessimistic locking
        assertThat(successCount.get()).isEqualTo(2);
        assertThat(cache.get(1)).isEqualTo(2);
    }

    @Test
    @DisplayName("Test pessimistic serializable isolation")
    public void testPessimisticSerializable() {
        CacheConfiguration<Integer, Integer> cfg = createTransactionalCacheConfig(getTestCacheName());
        IgniteCache<Integer, Integer> cache = ignite.getOrCreateCache(cfg);

        cache.put(1, 100);

        try (Transaction tx = ignite.transactions().txStart(
                TransactionConcurrency.PESSIMISTIC,
                TransactionIsolation.SERIALIZABLE)) {

            int value = cache.get(1);
            cache.put(1, value + 100);

            tx.commit();
        }

        assertThat(cache.get(1)).isEqualTo(200);
    }

    // ==================== Optimistic Transactions ====================

    @Test
    @DisplayName("Test optimistic transaction with serializable isolation")
    public void testOptimisticSerializable() {
        CacheConfiguration<Integer, Integer> cfg = createTransactionalCacheConfig(getTestCacheName());
        IgniteCache<Integer, Integer> cache = ignite.getOrCreateCache(cfg);

        cache.put(1, 100);

        try (Transaction tx = ignite.transactions().txStart(
                TransactionConcurrency.OPTIMISTIC,
                TransactionIsolation.SERIALIZABLE)) {

            int value = cache.get(1);
            cache.put(1, value + 50);

            tx.commit();
        }

        assertThat(cache.get(1)).isEqualTo(150);
    }

    @Test
    @DisplayName("Test optimistic transaction with repeatable read")
    public void testOptimisticRepeatableRead() {
        CacheConfiguration<Integer, Integer> cfg = createTransactionalCacheConfig(getTestCacheName());
        IgniteCache<Integer, Integer> cache = ignite.getOrCreateCache(cfg);

        cache.put(1, 200);

        try (Transaction tx = ignite.transactions().txStart(
                TransactionConcurrency.OPTIMISTIC,
                TransactionIsolation.REPEATABLE_READ)) {

            int value = cache.get(1);
            cache.put(1, value - 50);

            tx.commit();
        }

        assertThat(cache.get(1)).isEqualTo(150);
    }

    @Test
    @DisplayName("Test optimistic transaction conflict detection")
    public void testOptimisticConflictDetection() throws InterruptedException {
        CacheConfiguration<Integer, Integer> cfg = createTransactionalCacheConfig(getTestCacheName());
        IgniteCache<Integer, Integer> cache = ignite.getOrCreateCache(cfg);

        cache.put(1, 100);

        AtomicBoolean conflictDetected = new AtomicBoolean(false);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(1);

        // Thread 1: Start optimistic transaction, wait, then try to commit
        Thread t1 = new Thread(() -> {
            try (Transaction tx = ignite.transactions().txStart(
                    TransactionConcurrency.OPTIMISTIC,
                    TransactionIsolation.SERIALIZABLE)) {

                cache.get(1); // Read value

                startLatch.countDown(); // Signal that we've read

                // Wait for other thread to modify
                Thread.sleep(200);

                cache.put(1, 200);

                try {
                    tx.commit();
                } catch (TransactionOptimisticException e) {
                    conflictDetected.set(true);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                endLatch.countDown();
            }
        });

        t1.start();

        // Wait for t1 to read
        startLatch.await(5, TimeUnit.SECONDS);

        // Modify the value while t1 is in transaction
        cache.put(1, 999);

        endLatch.await(5, TimeUnit.SECONDS);

        // Conflict should have been detected in optimistic transaction
        assertThat(conflictDetected.get()).isTrue();
    }

    @Test
    @DisplayName("Test optimistic transaction no conflict when no concurrent modification")
    public void testOptimisticNoConflict() {
        CacheConfiguration<Integer, Integer> cfg = createTransactionalCacheConfig(getTestCacheName());
        IgniteCache<Integer, Integer> cache = ignite.getOrCreateCache(cfg);

        cache.put(1, 100);
        cache.put(2, 200);

        // No concurrent modifications, should succeed
        try (Transaction tx = ignite.transactions().txStart(
                TransactionConcurrency.OPTIMISTIC,
                TransactionIsolation.SERIALIZABLE)) {

            int v1 = cache.get(1);
            int v2 = cache.get(2);

            cache.put(1, v1 + v2);
            cache.put(2, v1 - v2);

            tx.commit();
        }

        assertThat(cache.get(1)).isEqualTo(300);
        assertThat(cache.get(2)).isEqualTo(-100);
    }

    // ==================== Transaction Timeout ====================

    @Test
    @DisplayName("Test transaction with explicit timeout")
    public void testTransactionTimeout() {
        CacheConfiguration<Integer, Integer> cfg = createTransactionalCacheConfig(getTestCacheName());
        IgniteCache<Integer, Integer> cache = ignite.getOrCreateCache(cfg);

        cache.put(1, 100);

        // Transaction with 5 second timeout
        try (Transaction tx = ignite.transactions().txStart(
                TransactionConcurrency.PESSIMISTIC,
                TransactionIsolation.REPEATABLE_READ,
                5000,  // 5 second timeout
                0)) {

            cache.put(1, 200);
            tx.commit();
        }

        assertThat(cache.get(1)).isEqualTo(200);
    }

    @Test
    @DisplayName("Test transaction timeout behavior")
    public void testTransactionTimeoutBehavior() throws InterruptedException {
        CacheConfiguration<Integer, Integer> cfg = createTransactionalCacheConfig(getTestCacheName());
        IgniteCache<Integer, Integer> cache = ignite.getOrCreateCache(cfg);

        cache.put(1, 100);

        AtomicBoolean timeoutOccurred = new AtomicBoolean(false);
        CountDownLatch holdLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(1);

        // Thread 1: Hold a lock
        Thread holder = new Thread(() -> {
            try (Transaction tx = ignite.transactions().txStart(
                    TransactionConcurrency.PESSIMISTIC,
                    TransactionIsolation.REPEATABLE_READ)) {

                cache.get(1); // Acquire lock

                // Hold lock for a while
                try {
                    holdLatch.await(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                tx.commit();
            }
        });

        holder.start();
        Thread.sleep(100); // Let holder acquire lock

        // Thread 2: Try to acquire with short timeout
        Thread waiter = new Thread(() -> {
            try (Transaction tx = ignite.transactions().txStart(
                    TransactionConcurrency.PESSIMISTIC,
                    TransactionIsolation.REPEATABLE_READ,
                    500,  // 500ms timeout
                    0)) {

                cache.get(1); // Try to acquire lock
                tx.commit();
            } catch (Exception e) {
                if (e instanceof TransactionTimeoutException ||
                    e.getCause() instanceof TransactionTimeoutException) {
                    timeoutOccurred.set(true);
                }
            } finally {
                doneLatch.countDown();
            }
        });

        waiter.start();
        doneLatch.await(3, TimeUnit.SECONDS);

        holdLatch.countDown(); // Release holder
        holder.join(2000);

        assertThat(timeoutOccurred.get()).isTrue();
    }

    @Test
    @DisplayName("Test transaction size limit parameter")
    public void testTransactionSizeLimit() {
        CacheConfiguration<Integer, Integer> cfg = createTransactionalCacheConfig(getTestCacheName());
        IgniteCache<Integer, Integer> cache = ignite.getOrCreateCache(cfg);

        // Transaction with size limit
        try (Transaction tx = ignite.transactions().txStart(
                TransactionConcurrency.PESSIMISTIC,
                TransactionIsolation.REPEATABLE_READ,
                10000,  // timeout
                10)) {  // max 10 entries

            for (int i = 0; i < 5; i++) {
                cache.put(i, i * 100);
            }

            tx.commit();
        }

        assertThat(cache.size()).isEqualTo(5);
    }

    // ==================== Cross-Cache Transactions ====================

    @Test
    @DisplayName("Test transaction across multiple caches")
    public void testCrossCacheTransaction() {
        String cacheName1 = getTestCacheName() + "-1";
        String cacheName2 = getTestCacheName() + "-2";

        CacheConfiguration<Integer, Integer> cfg1 = new CacheConfiguration<>(cacheName1);
        cfg1.setAtomicityMode(CacheAtomicityMode.TRANSACTIONAL);

        CacheConfiguration<Integer, Integer> cfg2 = new CacheConfiguration<>(cacheName2);
        cfg2.setAtomicityMode(CacheAtomicityMode.TRANSACTIONAL);

        IgniteCache<Integer, Integer> cache1 = ignite.getOrCreateCache(cfg1);
        IgniteCache<Integer, Integer> cache2 = ignite.getOrCreateCache(cfg2);

        cache1.put(1, 1000);
        cache2.put(1, 500);

        // Cross-cache transfer
        try (Transaction tx = ignite.transactions().txStart(
                TransactionConcurrency.PESSIMISTIC,
                TransactionIsolation.REPEATABLE_READ)) {

            int balance1 = cache1.get(1);
            int balance2 = cache2.get(1);

            // Transfer from cache1 to cache2
            cache1.put(1, balance1 - 300);
            cache2.put(1, balance2 + 300);

            tx.commit();
        }

        assertThat(cache1.get(1)).isEqualTo(700);
        assertThat(cache2.get(1)).isEqualTo(800);
    }

    @Test
    @DisplayName("Test cross-cache transaction rollback")
    public void testCrossCacheTransactionRollback() {
        String cacheName1 = getTestCacheName() + "-1";
        String cacheName2 = getTestCacheName() + "-2";

        CacheConfiguration<Integer, Integer> cfg1 = new CacheConfiguration<>(cacheName1);
        cfg1.setAtomicityMode(CacheAtomicityMode.TRANSACTIONAL);

        CacheConfiguration<Integer, Integer> cfg2 = new CacheConfiguration<>(cacheName2);
        cfg2.setAtomicityMode(CacheAtomicityMode.TRANSACTIONAL);

        IgniteCache<Integer, Integer> cache1 = ignite.getOrCreateCache(cfg1);
        IgniteCache<Integer, Integer> cache2 = ignite.getOrCreateCache(cfg2);

        cache1.put(1, 1000);
        cache2.put(1, 500);

        try (Transaction tx = ignite.transactions().txStart()) {
            cache1.put(1, 0);
            cache2.put(1, 1500);

            // Rollback
            tx.rollback();
        }

        // Both caches should have original values
        assertThat(cache1.get(1)).isEqualTo(1000);
        assertThat(cache2.get(1)).isEqualTo(500);
    }

    @Test
    @DisplayName("Test cross-cache transaction atomicity")
    public void testCrossCacheAtomicity() {
        String cacheName1 = getTestCacheName() + "-accounts";
        String cacheName2 = getTestCacheName() + "-audit";

        CacheConfiguration<Integer, Integer> cfg1 = new CacheConfiguration<>(cacheName1);
        cfg1.setAtomicityMode(CacheAtomicityMode.TRANSACTIONAL);

        CacheConfiguration<String, String> cfg2 = new CacheConfiguration<>(cacheName2);
        cfg2.setAtomicityMode(CacheAtomicityMode.TRANSACTIONAL);

        IgniteCache<Integer, Integer> accounts = ignite.getOrCreateCache(cfg1);
        IgniteCache<String, String> audit = ignite.getOrCreateCache(cfg2);

        accounts.put(1, 1000);

        try (Transaction tx = ignite.transactions().txStart()) {
            // Update account
            int balance = accounts.get(1);
            accounts.put(1, balance - 100);

            // Log to audit
            audit.put("tx-1", "Debit $100 from account 1");

            tx.commit();
        }

        assertThat(accounts.get(1)).isEqualTo(900);
        assertThat(audit.get("tx-1")).isEqualTo("Debit $100 from account 1");
    }

    @Test
    @DisplayName("Test transaction with three caches")
    public void testThreeCacheTransaction() {
        String cacheName1 = getTestCacheName() + "-source";
        String cacheName2 = getTestCacheName() + "-destination";
        String cacheName3 = getTestCacheName() + "-fees";

        CacheConfiguration<Integer, Integer> cfg1 = new CacheConfiguration<>(cacheName1);
        cfg1.setAtomicityMode(CacheAtomicityMode.TRANSACTIONAL);

        CacheConfiguration<Integer, Integer> cfg2 = new CacheConfiguration<>(cacheName2);
        cfg2.setAtomicityMode(CacheAtomicityMode.TRANSACTIONAL);

        CacheConfiguration<Integer, Integer> cfg3 = new CacheConfiguration<>(cacheName3);
        cfg3.setAtomicityMode(CacheAtomicityMode.TRANSACTIONAL);

        IgniteCache<Integer, Integer> source = ignite.getOrCreateCache(cfg1);
        IgniteCache<Integer, Integer> destination = ignite.getOrCreateCache(cfg2);
        IgniteCache<Integer, Integer> fees = ignite.getOrCreateCache(cfg3);

        source.put(1, 1000);
        destination.put(1, 0);
        fees.put(1, 0);

        int transferAmount = 100;
        int feeAmount = 5;

        try (Transaction tx = ignite.transactions().txStart(
                TransactionConcurrency.PESSIMISTIC,
                TransactionIsolation.REPEATABLE_READ)) {

            int srcBalance = source.get(1);
            int dstBalance = destination.get(1);
            int feeBalance = fees.get(1);

            source.put(1, srcBalance - transferAmount - feeAmount);
            destination.put(1, dstBalance + transferAmount);
            fees.put(1, feeBalance + feeAmount);

            tx.commit();
        }

        assertThat(source.get(1)).isEqualTo(895);  // 1000 - 100 - 5
        assertThat(destination.get(1)).isEqualTo(100);
        assertThat(fees.get(1)).isEqualTo(5);
    }

    // ==================== Additional Transaction Tests ====================

    @Test
    @DisplayName("Test transaction with batch putAll")
    public void testTransactionBatchPutAll() {
        CacheConfiguration<Integer, Integer> cfg = createTransactionalCacheConfig(getTestCacheName());
        IgniteCache<Integer, Integer> cache = ignite.getOrCreateCache(cfg);

        try (Transaction tx = ignite.transactions().txStart()) {
            Map<Integer, Integer> batch = new HashMap<>();
            for (int i = 0; i < 100; i++) {
                batch.put(i, i * 10);
            }
            cache.putAll(batch);

            tx.commit();
        }

        assertThat(cache.size()).isEqualTo(100);
        assertThat(cache.get(50)).isEqualTo(500);
    }

    @Test
    @DisplayName("Test IgniteTransactions API")
    public void testIgniteTransactionsApi() {
        CacheConfiguration<Integer, Integer> cfg = createTransactionalCacheConfig(getTestCacheName());
        IgniteCache<Integer, Integer> cache = ignite.getOrCreateCache(cfg);

        IgniteTransactions transactions = ignite.transactions();

        assertThat(transactions).isNotNull();

        try (Transaction tx = transactions.txStart()) {
            cache.put(1, 100);
            tx.commit();
        }

        assertThat(cache.get(1)).isEqualTo(100);
    }

    @Test
    @DisplayName("Test transaction state transitions")
    public void testTransactionStateTransitions() {
        CacheConfiguration<Integer, Integer> cfg = createTransactionalCacheConfig(getTestCacheName());
        IgniteCache<Integer, Integer> cache = ignite.getOrCreateCache(cfg);

        Transaction tx = ignite.transactions().txStart();

        // Transaction should be active
        assertThat(tx.state().name()).isEqualTo("ACTIVE");

        cache.put(1, 100);

        tx.commit();

        // Transaction should be committed
        assertThat(tx.state().name()).isEqualTo("COMMITTED");

        tx.close();
    }

    @Test
    @DisplayName("Test transaction with remove operations")
    public void testTransactionRemove() {
        CacheConfiguration<Integer, Integer> cfg = createTransactionalCacheConfig(getTestCacheName());
        IgniteCache<Integer, Integer> cache = ignite.getOrCreateCache(cfg);

        cache.put(1, 100);
        cache.put(2, 200);
        cache.put(3, 300);

        try (Transaction tx = ignite.transactions().txStart()) {
            cache.remove(2);
            tx.commit();
        }

        assertThat(cache.containsKey(1)).isTrue();
        assertThat(cache.containsKey(2)).isFalse();
        assertThat(cache.containsKey(3)).isTrue();
    }

    @Test
    @DisplayName("Test transaction isolation between concurrent reads")
    public void testTransactionIsolation() throws InterruptedException {
        CacheConfiguration<Integer, Integer> cfg = createTransactionalCacheConfig(getTestCacheName());
        IgniteCache<Integer, Integer> cache = ignite.getOrCreateCache(cfg);

        cache.put(1, 100);

        AtomicInteger readValue = new AtomicInteger();
        CountDownLatch readLatch = new CountDownLatch(1);
        CountDownLatch writeLatch = new CountDownLatch(1);

        // Reader thread with REPEATABLE_READ
        Thread reader = new Thread(() -> {
            try (Transaction tx = ignite.transactions().txStart(
                    TransactionConcurrency.PESSIMISTIC,
                    TransactionIsolation.REPEATABLE_READ)) {

                int first = cache.get(1);
                readLatch.countDown();

                // Wait for writer to complete
                try {
                    writeLatch.await(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                int second = cache.get(1);

                // With REPEATABLE_READ, both reads should return same value
                readValue.set(second);

                tx.commit();
            }
        });

        reader.start();
        readLatch.await(5, TimeUnit.SECONDS);

        // Write outside transaction
        cache.put(1, 999);
        writeLatch.countDown();

        reader.join(5000);

        // The reader should still see the original value due to REPEATABLE_READ
        assertThat(readValue.get()).isEqualTo(100);
    }

    // ==================== Deadlock Detection and Avoidance ====================

    @Test
    @DisplayName("Test deadlock detection with transaction timeout")
    public void testDeadlockDetection() throws InterruptedException {
        CacheConfiguration<Integer, Integer> cfg = createTransactionalCacheConfig(getTestCacheName());
        IgniteCache<Integer, Integer> cache = ignite.getOrCreateCache(cfg);

        cache.put(1, 100);
        cache.put(2, 200);

        AtomicBoolean deadlockDetected = new AtomicBoolean(false);
        CountDownLatch startLatch = new CountDownLatch(2);
        CountDownLatch doneLatch = new CountDownLatch(2);

        // Thread 1: Locks key 1, then tries to lock key 2
        Thread t1 = new Thread(() -> {
            try (Transaction tx = ignite.transactions().txStart(
                    TransactionConcurrency.PESSIMISTIC,
                    TransactionIsolation.REPEATABLE_READ,
                    2000,  // 2 second timeout to detect deadlock
                    0)) {

                cache.get(1); // Lock key 1
                startLatch.countDown();

                try {
                    startLatch.await(5, TimeUnit.SECONDS);
                    Thread.sleep(100); // Give other thread time to acquire lock
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                cache.get(2); // Try to lock key 2 - potential deadlock
                tx.commit();
            } catch (IgniteException e) {
                // Deadlock detected via timeout or exception (includes TransactionTimeoutException)
                if (e.getMessage() != null &&
                    (e.getMessage().contains("deadlock") ||
                     e.getMessage().contains("timed out") ||
                     e.getMessage().contains("timeout"))) {
                    deadlockDetected.set(true);
                } else {
                    deadlockDetected.set(true);
                }
            } catch (Exception e) {
                // Any transaction failure due to deadlock
                deadlockDetected.set(true);
            } finally {
                doneLatch.countDown();
            }
        });

        // Thread 2: Locks key 2, then tries to lock key 1 (opposite order - creates deadlock)
        Thread t2 = new Thread(() -> {
            try (Transaction tx = ignite.transactions().txStart(
                    TransactionConcurrency.PESSIMISTIC,
                    TransactionIsolation.REPEATABLE_READ,
                    2000,  // 2 second timeout to detect deadlock
                    0)) {

                cache.get(2); // Lock key 2
                startLatch.countDown();

                try {
                    startLatch.await(5, TimeUnit.SECONDS);
                    Thread.sleep(100); // Give other thread time to acquire lock
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                cache.get(1); // Try to lock key 1 - potential deadlock
                tx.commit();
            } catch (IgniteException e) {
                // Deadlock detected via timeout or exception (includes TransactionTimeoutException)
                if (e.getMessage() != null &&
                    (e.getMessage().contains("deadlock") ||
                     e.getMessage().contains("timed out") ||
                     e.getMessage().contains("timeout"))) {
                    deadlockDetected.set(true);
                } else {
                    deadlockDetected.set(true);
                }
            } catch (Exception e) {
                // Any transaction failure due to deadlock
                deadlockDetected.set(true);
            } finally {
                doneLatch.countDown();
            }
        });

        t1.start();
        t2.start();

        doneLatch.await(10, TimeUnit.SECONDS);

        // At least one thread should have detected the deadlock (via timeout or explicit detection)
        assertThat(deadlockDetected.get()).isTrue();
    }

    @Test
    @DisplayName("Test ordered locking pattern to avoid deadlocks")
    public void testOrderedLockingPattern() throws InterruptedException {
        CacheConfiguration<Integer, Integer> cfg = createTransactionalCacheConfig(getTestCacheName());
        IgniteCache<Integer, Integer> cache = ignite.getOrCreateCache(cfg);

        cache.put(1, 100);
        cache.put(2, 200);
        cache.put(3, 300);

        AtomicInteger successCount = new AtomicInteger(0);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(3);

        // Create multiple threads that all access keys in the SAME order
        // This prevents deadlocks by ensuring consistent lock ordering
        Runnable orderedTask = () -> {
            try {
                startLatch.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }

            try (Transaction tx = ignite.transactions().txStart(
                    TransactionConcurrency.PESSIMISTIC,
                    TransactionIsolation.REPEATABLE_READ,
                    5000,  // 5 second timeout
                    0)) {

                // Always access keys in ascending order to prevent deadlocks
                int val1 = cache.get(1);
                int val2 = cache.get(2);
                int val3 = cache.get(3);

                // Perform updates
                cache.put(1, val1 + 10);
                cache.put(2, val2 + 10);
                cache.put(3, val3 + 10);

                tx.commit();
                successCount.incrementAndGet();
            } catch (Exception e) {
                // Log but don't fail - some contention is expected
                log.warn("Transaction failed: " + e.getMessage());
            } finally {
                doneLatch.countDown();
            }
        };

        Thread t1 = new Thread(orderedTask);
        Thread t2 = new Thread(orderedTask);
        Thread t3 = new Thread(orderedTask);

        t1.start();
        t2.start();
        t3.start();

        // Release all threads simultaneously
        startLatch.countDown();

        doneLatch.await(15, TimeUnit.SECONDS);

        // With ordered locking, all transactions should succeed (no deadlocks)
        assertThat(successCount.get()).isEqualTo(3);

        // Verify final values (each key should have been incremented by 30 total)
        assertThat(cache.get(1)).isEqualTo(130);  // 100 + 10 + 10 + 10
        assertThat(cache.get(2)).isEqualTo(230);  // 200 + 10 + 10 + 10
        assertThat(cache.get(3)).isEqualTo(330);  // 300 + 10 + 10 + 10
    }
}
