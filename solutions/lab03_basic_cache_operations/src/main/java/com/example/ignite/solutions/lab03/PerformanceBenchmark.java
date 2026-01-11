package com.example.ignite.solutions.lab03;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.IgniteDataStreamer;
import org.apache.ignite.Ignition;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.lang.IgniteFuture;

import java.util.*;

/**
 * Lab 3 Optional: Comprehensive Performance Benchmarking
 *
 * This exercise provides detailed performance measurements
 * for various cache operations.
 */
public class PerformanceBenchmark {

    private static final int WARMUP_ITERATIONS = 100;
    private static final int BENCHMARK_ITERATIONS = 1000;
    private static final int BATCH_SIZE = 100;

    public static void main(String[] args) {
        try (Ignite ignite = Ignition.start()) {
            System.out.println("=== Performance Measurement Lab ===\n");

            CacheConfiguration<Integer, String> cfg =
                new CacheConfiguration<>("benchmarkCache");
            IgniteCache<Integer, String> cache = ignite.getOrCreateCache(cfg);

            // Warmup
            System.out.println("Warming up...");
            for (int i = 0; i < WARMUP_ITERATIONS; i++) {
                cache.put(i, "warmup-" + i);
                cache.get(i);
            }
            cache.clear();

            // 1. Individual vs Batch PUT
            System.out.println("\n" + "=".repeat(60));
            System.out.println("1. PUT Performance: Individual vs Batch");
            System.out.println("=".repeat(60));

            // Individual PUTs
            long[] individualLatencies = new long[BENCHMARK_ITERATIONS];
            long startTime = System.nanoTime();
            for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
                long opStart = System.nanoTime();
                cache.put(i, "individual-" + i);
                individualLatencies[i] = System.nanoTime() - opStart;
            }
            long individualTotalTime = System.nanoTime() - startTime;

            cache.clear();

            // Batch PUTs
            long[] batchLatencies = new long[BENCHMARK_ITERATIONS / BATCH_SIZE];
            startTime = System.nanoTime();
            int batchIndex = 0;
            for (int i = 0; i < BENCHMARK_ITERATIONS; i += BATCH_SIZE) {
                Map<Integer, String> batch = new HashMap<>();
                for (int j = i; j < i + BATCH_SIZE; j++) {
                    batch.put(j, "batch-" + j);
                }
                long opStart = System.nanoTime();
                cache.putAll(batch);
                batchLatencies[batchIndex++] = System.nanoTime() - opStart;
            }
            long batchTotalTime = System.nanoTime() - startTime;

            printResults("Individual PUT", individualLatencies, individualTotalTime, BENCHMARK_ITERATIONS);
            printResults("Batch PUT (size " + BATCH_SIZE + ")", batchLatencies, batchTotalTime, BENCHMARK_ITERATIONS);

            // 2. Sync vs Async PUT
            System.out.println("\n" + "=".repeat(60));
            System.out.println("2. PUT Performance: Sync vs Async");
            System.out.println("=".repeat(60));

            cache.clear();

            // Sync PUTs
            long[] syncLatencies = new long[BENCHMARK_ITERATIONS];
            startTime = System.nanoTime();
            for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
                long opStart = System.nanoTime();
                cache.put(i, "sync-" + i);
                syncLatencies[i] = System.nanoTime() - opStart;
            }
            long syncTotalTime = System.nanoTime() - startTime;

            cache.clear();

            // Async PUTs
            List<IgniteFuture<Void>> futures = new ArrayList<>();
            startTime = System.nanoTime();
            for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
                futures.add(cache.putAsync(i, "async-" + i));
            }
            // Wait for all
            for (IgniteFuture<Void> f : futures) {
                f.get();
            }
            long asyncTotalTime = System.nanoTime() - startTime;

            printResults("Sync PUT", syncLatencies, syncTotalTime, BENCHMARK_ITERATIONS);
            System.out.println("\nAsync PUT:");
            System.out.println("   Total time: " + formatNanos(asyncTotalTime));
            System.out.println("   Throughput: " +
                String.format("%,.0f", BENCHMARK_ITERATIONS * 1_000_000_000.0 / asyncTotalTime) + " ops/sec");

            // 3. GET Performance
            System.out.println("\n" + "=".repeat(60));
            System.out.println("3. GET Performance: Individual vs Batch");
            System.out.println("=".repeat(60));

            // Individual GETs
            long[] getLatencies = new long[BENCHMARK_ITERATIONS];
            startTime = System.nanoTime();
            for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
                long opStart = System.nanoTime();
                cache.get(i);
                getLatencies[i] = System.nanoTime() - opStart;
            }
            long getTotalTime = System.nanoTime() - startTime;

            // Batch GETs
            long[] batchGetLatencies = new long[BENCHMARK_ITERATIONS / BATCH_SIZE];
            startTime = System.nanoTime();
            batchIndex = 0;
            for (int i = 0; i < BENCHMARK_ITERATIONS; i += BATCH_SIZE) {
                Set<Integer> keys = new HashSet<>();
                for (int j = i; j < i + BATCH_SIZE; j++) {
                    keys.add(j);
                }
                long opStart = System.nanoTime();
                cache.getAll(keys);
                batchGetLatencies[batchIndex++] = System.nanoTime() - opStart;
            }
            long batchGetTotalTime = System.nanoTime() - startTime;

            printResults("Individual GET", getLatencies, getTotalTime, BENCHMARK_ITERATIONS);
            printResults("Batch GET (size " + BATCH_SIZE + ")", batchGetLatencies, batchGetTotalTime, BENCHMARK_ITERATIONS);

            // 4. DataStreamer Performance
            System.out.println("\n" + "=".repeat(60));
            System.out.println("4. DataStreamer Performance");
            System.out.println("=".repeat(60));

            int streamCount = 10000;
            cache.clear();

            startTime = System.nanoTime();
            try (IgniteDataStreamer<Integer, String> streamer =
                    ignite.dataStreamer("benchmarkCache")) {
                streamer.perNodeBufferSize(1024);
                for (int i = 0; i < streamCount; i++) {
                    streamer.addData(i, "streamed-" + i);
                }
            }
            long streamerTime = System.nanoTime() - startTime;

            System.out.println("\nDataStreamer (" + streamCount + " entries):");
            System.out.println("   Total time: " + formatNanos(streamerTime));
            System.out.println("   Throughput: " +
                String.format("%,.0f", streamCount * 1_000_000_000.0 / streamerTime) + " ops/sec");

            // Summary
            System.out.println("\n" + "=".repeat(60));
            System.out.println("PERFORMANCE SUMMARY");
            System.out.println("=".repeat(60));
            System.out.println("\nKey Findings:");
            System.out.println("- Batch operations reduce network round-trips");
            System.out.println("- Async operations improve throughput for concurrent workloads");
            System.out.println("- DataStreamer is optimal for bulk loading");
            System.out.println("- Latency percentiles help identify outliers");

            System.out.println("\nPress Enter to exit...");
            System.in.read();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void printResults(String operation, long[] latencies, long totalTime, int operations) {
        Arrays.sort(latencies);

        long min = latencies[0];
        long max = latencies[latencies.length - 1];
        long p50 = latencies[(int)(latencies.length * 0.50)];
        long p90 = latencies[(int)(latencies.length * 0.90)];
        long p99 = latencies[Math.min((int)(latencies.length * 0.99), latencies.length - 1)];

        double avg = 0;
        for (long l : latencies) avg += l;
        avg /= latencies.length;

        System.out.println("\n" + operation + ":");
        System.out.println("   Total time: " + formatNanos(totalTime));
        System.out.println("   Throughput: " +
            String.format("%,.0f", operations * 1_000_000_000.0 / totalTime) + " ops/sec");
        System.out.println("   Latency:");
        System.out.println("      Min:  " + formatNanos(min));
        System.out.println("      Avg:  " + formatNanos((long)avg));
        System.out.println("      P50:  " + formatNanos(p50));
        System.out.println("      P90:  " + formatNanos(p90));
        System.out.println("      P99:  " + formatNanos(p99));
        System.out.println("      Max:  " + formatNanos(max));
    }

    private static String formatNanos(long nanos) {
        if (nanos < 1000) {
            return nanos + " ns";
        } else if (nanos < 1_000_000) {
            return String.format("%.2f us", nanos / 1000.0);
        } else if (nanos < 1_000_000_000) {
            return String.format("%.2f ms", nanos / 1_000_000.0);
        } else {
            return String.format("%.2f s", nanos / 1_000_000_000.0);
        }
    }
}
