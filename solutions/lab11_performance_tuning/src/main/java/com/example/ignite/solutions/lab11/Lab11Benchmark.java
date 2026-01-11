package com.example.ignite.solutions.lab11;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.cache.CacheAtomicityMode;
import org.apache.ignite.cache.CacheMode;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.transactions.Transaction;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Lab 11 Exercise 3: Performance Benchmarking
 *
 * Demonstrates:
 * - Atomic vs Transactional cache performance
 * - Batch operations comparison
 * - Concurrent access benchmarking
 * - Throughput measurement
 */
public class Lab11Benchmark {

    private static final int WARMUP_ITERATIONS = 1000;
    private static final int TEST_ITERATIONS = 10000;
    private static final int THREAD_COUNT = 10;

    public static void main(String[] args) {
        try (Ignite ignite = Ignition.start()) {
            System.out.println("=== Performance Benchmark Lab ===\n");

            // Test different cache configurations
            benchmarkAtomicCache(ignite);
            benchmarkTransactionalCache(ignite);
            benchmarkBatchOperations(ignite);
            benchmarkConcurrentAccess(ignite);

            System.out.println("\n=== Benchmark Summary ===");
            System.out.println("All benchmarks completed");
            System.out.println("Review results to identify bottlenecks");

            System.out.println("\n=== Performance Tips ===");
            System.out.println("1. Use ATOMIC mode for non-transactional workloads");
            System.out.println("2. Use putAll/getAll for batch operations");
            System.out.println("3. Configure appropriate backup count");
            System.out.println("4. Monitor GC and adjust heap size");
            System.out.println("5. Use affinity colocation for related data");

            System.out.println("\nPress Enter to exit...");
            System.in.read();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void benchmarkAtomicCache(Ignite ignite) {
        System.out.println("=== Benchmark 1: Atomic Cache ===");

        CacheConfiguration<Integer, String> cfg =
            new CacheConfiguration<>("atomicCache");
        cfg.setCacheMode(CacheMode.PARTITIONED);
        cfg.setAtomicityMode(CacheAtomicityMode.ATOMIC);
        cfg.setBackups(1);

        IgniteCache<Integer, String> cache = ignite.getOrCreateCache(cfg);

        // Warmup
        System.out.println("Warming up...");
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            cache.put(i, "Warmup-" + i);
        }

        // Benchmark PUT
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            cache.put(i, "Value-" + i);
        }
        long putTime = System.currentTimeMillis() - startTime;

        // Benchmark GET
        startTime = System.currentTimeMillis();
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            cache.get(i);
        }
        long getTime = System.currentTimeMillis() - startTime;

        System.out.println("PUT: " + TEST_ITERATIONS + " operations in " + putTime + " ms");
        System.out.println("Throughput: " + (TEST_ITERATIONS * 1000L / Math.max(1, putTime)) + " ops/sec");
        System.out.println("GET: " + TEST_ITERATIONS + " operations in " + getTime + " ms");
        System.out.println("Throughput: " + (TEST_ITERATIONS * 1000L / Math.max(1, getTime)) + " ops/sec\n");

        ignite.destroyCache("atomicCache");
    }

    private static void benchmarkTransactionalCache(Ignite ignite) {
        System.out.println("=== Benchmark 2: Transactional Cache ===");

        CacheConfiguration<Integer, String> cfg =
            new CacheConfiguration<>("txCache");
        cfg.setCacheMode(CacheMode.PARTITIONED);
        cfg.setAtomicityMode(CacheAtomicityMode.TRANSACTIONAL);
        cfg.setBackups(1);

        IgniteCache<Integer, String> cache = ignite.getOrCreateCache(cfg);

        int txIterations = TEST_ITERATIONS / 10; // Fewer iterations for transactional

        // Benchmark with transactions
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < txIterations; i++) {
            try (Transaction tx = ignite.transactions().txStart()) {
                cache.put(i, "Value-" + i);
                tx.commit();
            }
        }
        long txTime = System.currentTimeMillis() - startTime;

        System.out.println("Transactional PUT: " + txIterations +
            " operations in " + txTime + " ms");
        System.out.println("Throughput: " + (txIterations * 1000L / Math.max(1, txTime)) + " ops/sec");
        System.out.println("(Note: Transactions have overhead for ACID guarantees)\n");

        ignite.destroyCache("txCache");
    }

    private static void benchmarkBatchOperations(Ignite ignite) {
        System.out.println("=== Benchmark 3: Batch Operations ===");

        CacheConfiguration<Integer, String> cfg =
            new CacheConfiguration<>("batchCache");

        IgniteCache<Integer, String> cache = ignite.getOrCreateCache(cfg);

        // Individual operations
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            cache.put(i, "Value-" + i);
        }
        long individualTime = System.currentTimeMillis() - startTime;

        cache.clear();

        // Batch operations (batches of 500)
        startTime = System.currentTimeMillis();
        Map<Integer, String> batch = new HashMap<>();
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            batch.put(i, "Value-" + i);
            if (batch.size() >= 500) {
                cache.putAll(batch);
                batch.clear();
            }
        }
        if (!batch.isEmpty()) {
            cache.putAll(batch);
        }
        long batchTime = System.currentTimeMillis() - startTime;

        System.out.println("Individual PUT: " + individualTime + " ms");
        System.out.println("Batch PUT (500): " + batchTime + " ms");
        if (batchTime > 0) {
            System.out.println("Speedup: " + String.format("%.2fx", (float) individualTime / batchTime));
        }
        System.out.println();

        ignite.destroyCache("batchCache");
    }

    private static void benchmarkConcurrentAccess(Ignite ignite) {
        System.out.println("=== Benchmark 4: Concurrent Access ===");

        CacheConfiguration<Integer, String> cfg =
            new CacheConfiguration<>("concurrentCache");

        IgniteCache<Integer, String> cache = ignite.getOrCreateCache(cfg);

        // Populate cache
        System.out.println("Populating cache with 10,000 entries...");
        Map<Integer, String> initial = new HashMap<>();
        for (int i = 0; i < 10000; i++) {
            initial.put(i, "Value-" + i);
        }
        cache.putAll(initial);

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        AtomicLong operations = new AtomicLong(0);
        AtomicLong totalLatency = new AtomicLong(0);
        Random random = new Random();

        int opsPerThread = TEST_ITERATIONS / THREAD_COUNT;

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < THREAD_COUNT; i++) {
            executor.submit(() -> {
                Random localRandom = new Random();
                for (int j = 0; j < opsPerThread; j++) {
                    int key = localRandom.nextInt(10000);

                    long opStart = System.nanoTime();
                    // Mix of operations: 30% write, 70% read
                    if (j % 10 < 3) {
                        cache.put(key, "Updated-" + key);
                    } else {
                        cache.get(key);
                    }
                    long opTime = System.nanoTime() - opStart;
                    totalLatency.addAndGet(opTime);
                    operations.incrementAndGet();
                }
            });
        }

        executor.shutdown();
        try {
            executor.awaitTermination(2, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        long concurrentTime = System.currentTimeMillis() - startTime;
        long totalOps = operations.get();

        System.out.println("Threads: " + THREAD_COUNT);
        System.out.println("Total operations: " + totalOps);
        System.out.println("Time: " + concurrentTime + " ms");
        System.out.println("Throughput: " + (totalOps * 1000L / Math.max(1, concurrentTime)) + " ops/sec");
        System.out.println("Avg latency: " +
            String.format("%.3f ms", (totalLatency.get() / (double) totalOps) / 1_000_000));
        System.out.println();

        ignite.destroyCache("concurrentCache");
    }
}
