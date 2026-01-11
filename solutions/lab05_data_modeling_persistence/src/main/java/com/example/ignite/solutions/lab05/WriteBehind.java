package com.example.ignite.solutions.lab05;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.cache.store.CacheStoreAdapter;
import org.apache.ignite.configuration.CacheConfiguration;

import javax.cache.Cache;
import javax.cache.integration.CacheLoaderException;
import javax.cache.integration.CacheWriterException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Lab 5 Exercise 5: Write-Behind Cache Store
 *
 * This exercise demonstrates write-behind caching for
 * asynchronous database updates.
 */
public class WriteBehind {

    public static void main(String[] args) {
        try (Ignite ignite = Ignition.start()) {
            System.out.println("=== Write-Behind Cache Store Lab ===\n");

            CacheConfiguration<Integer, String> cfg =
                new CacheConfiguration<>("writeBehindCache");

            cfg.setReadThrough(true);
            cfg.setWriteThrough(true);
            cfg.setCacheStoreFactory(() -> new CountingCacheStore());

            // Configure write-behind
            cfg.setWriteBehindEnabled(true);
            cfg.setWriteBehindFlushSize(10);        // Flush after 10 entries
            cfg.setWriteBehindFlushFrequency(5000); // Or every 5 seconds
            cfg.setWriteBehindBatchSize(5);         // Batch 5 entries per write

            IgniteCache<Integer, String> cache = ignite.getOrCreateCache(cfg);

            System.out.println("=== Write-Behind Configuration ===");
            System.out.println("Flush size: 10 entries");
            System.out.println("Flush frequency: 5000 ms");
            System.out.println("Batch size: 5 entries");

            System.out.println("\nWriting 20 entries to cache...");
            long startTime = System.currentTimeMillis();

            for (int i = 1; i <= 20; i++) {
                cache.put(i, "Value-" + i);
            }

            long cacheTime = System.currentTimeMillis() - startTime;
            System.out.println("Cache writes completed in: " + cacheTime + " ms");
            System.out.println("(Notice: cache writes return immediately)");

            System.out.println("\nWaiting for write-behind to flush to database...");
            System.out.println("Database writes happen asynchronously in batches:");
            Thread.sleep(6000);  // Wait for flush

            System.out.println("\n=== Write-Behind vs Write-Through ===");
            System.out.println("Write-Through:");
            System.out.println("  - Synchronous: cache.put() waits for DB write");
            System.out.println("  - Immediate consistency");
            System.out.println("  - Higher latency");

            System.out.println("\nWrite-Behind:");
            System.out.println("  - Asynchronous: cache.put() returns immediately");
            System.out.println("  - Batched DB writes");
            System.out.println("  - Lower latency, higher throughput");
            System.out.println("  - Eventual consistency");

            System.out.println("\n=== Write-Behind Benefits ===");
            System.out.println("- Non-blocking cache writes");
            System.out.println("- Batch database updates");
            System.out.println("- Better performance for write-heavy workloads");
            System.out.println("- Configurable flush frequency and size");

            System.out.println("\nPress Enter to exit...");
            System.in.read();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static class CountingCacheStore extends CacheStoreAdapter<Integer, String> {
        private static final ConcurrentHashMap<Integer, String> db = new ConcurrentHashMap<>();
        private static final AtomicInteger writeCount = new AtomicInteger(0);

        @Override
        public String load(Integer key) throws CacheLoaderException {
            return db.get(key);
        }

        @Override
        public void write(Cache.Entry<? extends Integer, ? extends String> entry)
                throws CacheWriterException {
            int count = writeCount.incrementAndGet();
            System.out.println("  [DB Write #" + count + "] " +
                entry.getKey() + " = " + entry.getValue() +
                " (Thread: " + Thread.currentThread().getName() + ")");
            db.put(entry.getKey(), entry.getValue());

            // Simulate slow database write
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        @Override
        public void delete(Object key) throws CacheWriterException {
            db.remove(key);
        }
    }
}
