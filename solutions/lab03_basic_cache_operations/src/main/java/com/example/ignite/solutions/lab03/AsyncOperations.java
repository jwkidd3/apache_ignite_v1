package com.example.ignite.solutions.lab03;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.lang.IgniteFuture;

import java.util.ArrayList;
import java.util.List;

/**
 * Lab 3 Exercise 3: Synchronous vs Asynchronous Operations
 *
 * This exercise demonstrates the performance difference between
 * synchronous and asynchronous cache operations.
 */
public class AsyncOperations {

    public static void main(String[] args) {
        try (Ignite ignite = Ignition.start()) {
            System.out.println("=== Synchronous vs Asynchronous Operations ===\n");

            CacheConfiguration<Integer, String> cfg =
                new CacheConfiguration<>("asyncCache");
            IgniteCache<Integer, String> cache = ignite.getOrCreateCache(cfg);

            // Synchronous operations
            System.out.println("1. Synchronous Operations:");
            long startTime = System.currentTimeMillis();

            for (int i = 0; i < 1000; i++) {
                cache.put(i, "Value-" + i);
            }

            long syncTime = System.currentTimeMillis() - startTime;
            System.out.println("   Time taken: " + syncTime + " ms");

            // Clear cache
            cache.clear();

            // Asynchronous operations using modern API
            System.out.println("\n2. Asynchronous Operations:");
            startTime = System.currentTimeMillis();

            List<IgniteFuture<Void>> futures = new ArrayList<>();
            for (int i = 0; i < 1000; i++) {
                IgniteFuture<Void> future = cache.putAsync(i, "Value-" + i);
                futures.add(future);
            }

            // Wait for all operations to complete
            for (IgniteFuture<Void> future : futures) {
                future.get();
            }

            long asyncTime = System.currentTimeMillis() - startTime;
            System.out.println("   Time taken: " + asyncTime + " ms");

            System.out.println("\n3. Performance Comparison:");
            System.out.println("   Synchronous: " + syncTime + " ms");
            System.out.println("   Asynchronous: " + asyncTime + " ms");
            if (asyncTime > 0) {
                System.out.println("   Speedup: " + String.format("%.2f", (float)syncTime / asyncTime) + "x");
            }

            // Async with callback
            System.out.println("\n4. Async Operations with Callback:");
            IgniteFuture<String> getFuture = cache.getAsync(500);

            getFuture.listen(f -> {
                System.out.println("   Callback: Retrieved value = " + f.get());
            });

            // Wait for callback
            Thread.sleep(100);

            // Batch async operations
            System.out.println("\n5. Batch Async Operations:");
            startTime = System.currentTimeMillis();

            List<IgniteFuture<Void>> batchFutures = new ArrayList<>();
            for (int i = 0; i < 100; i++) {
                IgniteFuture<Void> future = cache.putAsync(i + 1000, "Batch-" + i);
                batchFutures.add(future);
            }

            // Wait for all to complete
            for (IgniteFuture<Void> future : batchFutures) {
                future.get();
            }

            long batchTime = System.currentTimeMillis() - startTime;
            System.out.println("   Batch operations time: " + batchTime + " ms");

            System.out.println("\nPress Enter to exit...");
            System.in.read();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
