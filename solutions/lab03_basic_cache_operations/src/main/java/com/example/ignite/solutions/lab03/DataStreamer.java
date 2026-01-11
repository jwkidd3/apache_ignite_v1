package com.example.ignite.solutions.lab03;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.IgniteDataStreamer;
import org.apache.ignite.Ignition;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.stream.StreamReceiver;

import java.util.HashMap;
import java.util.Map;

/**
 * Lab 3 Optional: DataStreamer for High-Speed Loading
 *
 * This exercise demonstrates using DataStreamer for
 * efficient bulk data loading.
 */
public class DataStreamer {

    public static void main(String[] args) {
        try (Ignite ignite = Ignition.start()) {
            System.out.println("=== DataStreamer Lab ===\n");

            CacheConfiguration<Integer, String> cfg =
                new CacheConfiguration<>("streamCache");
            IgniteCache<Integer, String> cache = ignite.getOrCreateCache(cfg);

            int totalRecords = 100000;

            // 1. Compare regular putAll vs DataStreamer
            System.out.println("1. Performance Comparison: putAll vs DataStreamer\n");

            // Method 1: Using putAll (batch)
            cache.clear();
            long startTime = System.currentTimeMillis();

            int batchSize = 1000;
            for (int i = 0; i < totalRecords; i += batchSize) {
                Map<Integer, String> batch = new HashMap<>();
                for (int j = i; j < Math.min(i + batchSize, totalRecords); j++) {
                    batch.put(j, "Value-" + j);
                }
                cache.putAll(batch);
            }

            long putAllTime = System.currentTimeMillis() - startTime;
            System.out.println("   putAll time for " + totalRecords + " records: " +
                putAllTime + " ms");
            System.out.println("   Throughput: " +
                (totalRecords * 1000 / putAllTime) + " records/sec");

            // Method 2: Using DataStreamer
            cache.clear();
            startTime = System.currentTimeMillis();

            try (IgniteDataStreamer<Integer, String> streamer =
                    ignite.dataStreamer("streamCache")) {

                // Configure streamer for maximum performance
                streamer.perNodeBufferSize(1024);
                streamer.perNodeParallelOperations(8);
                streamer.allowOverwrite(true);

                for (int i = 0; i < totalRecords; i++) {
                    streamer.addData(i, "Value-" + i);
                }
            } // Auto-flush on close

            long streamerTime = System.currentTimeMillis() - startTime;
            System.out.println("\n   DataStreamer time for " + totalRecords + " records: " +
                streamerTime + " ms");
            System.out.println("   Throughput: " +
                (totalRecords * 1000 / streamerTime) + " records/sec");
            System.out.println("   Speedup: " +
                String.format("%.2f", (double)putAllTime / streamerTime) + "x faster");

            // 2. DataStreamer with custom receiver (transformation)
            System.out.println("\n2. DataStreamer with Custom StreamReceiver:");

            CacheConfiguration<Integer, Integer> numCfg =
                new CacheConfiguration<>("numbersCache");
            IgniteCache<Integer, Integer> numCache = ignite.getOrCreateCache(numCfg);

            try (IgniteDataStreamer<Integer, Integer> streamer =
                    ignite.dataStreamer("numbersCache")) {

                // Set a custom receiver that doubles the values
                streamer.receiver((receiverCache, entries) -> {
                    for (Map.Entry<Integer, Integer> entry : entries) {
                        receiverCache.put(entry.getKey(), entry.getValue() * 2);
                    }
                });

                for (int i = 0; i < 10; i++) {
                    streamer.addData(i, i);
                }
            }

            System.out.println("   Loaded numbers 0-9 with custom receiver (doubles values)");
            System.out.println("   Sample values (should be doubled): ");
            for (int i = 0; i < 5; i++) {
                System.out.println("      Key " + i + " = " + numCache.get(i) + " (original: " + i + ")");
            }

            // 3. DataStreamer options explained
            System.out.println("\n3. DataStreamer Configuration Options:");

            try (IgniteDataStreamer<Integer, String> streamer =
                    ignite.dataStreamer("streamCache")) {

                // Per-node buffer size
                streamer.perNodeBufferSize(2048);
                System.out.println("   perNodeBufferSize: 2048 entries");
                System.out.println("      - Entries buffered before sending to each node");

                // Parallel operations per node
                streamer.perNodeParallelOperations(16);
                System.out.println("   perNodeParallelOperations: 16");
                System.out.println("      - Concurrent streams per node");

                // Allow overwrite
                streamer.allowOverwrite(true);
                System.out.println("   allowOverwrite: true");
                System.out.println("      - Replace existing entries");

                // Auto-flush frequency
                streamer.autoFlushFrequency(1000);
                System.out.println("   autoFlushFrequency: 1000 ms");
                System.out.println("      - Auto-flush interval");
            }

            // 4. Manual flush demonstration
            System.out.println("\n4. Manual Flush Control:");

            try (IgniteDataStreamer<Integer, String> streamer =
                    ignite.dataStreamer("streamCache")) {

                streamer.autoFlushFrequency(0); // Disable auto-flush

                for (int i = 0; i < 100; i++) {
                    streamer.addData(i + 200000, "Batch1-" + i);
                }

                System.out.println("   Added 100 entries (not flushed yet)");
                System.out.println("   Cache size before flush: " + cache.size());

                streamer.flush();
                System.out.println("   After manual flush, cache size: " + cache.size());
            }

            System.out.println("\n=== DataStreamer Best Practices ===");
            System.out.println("- Use for bulk loading (10,000+ entries)");
            System.out.println("- Tune perNodeBufferSize based on entry size");
            System.out.println("- Set allowOverwrite based on your use case");
            System.out.println("- Always use try-with-resources for proper cleanup");

            System.out.println("\nPress Enter to exit...");
            System.in.read();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
