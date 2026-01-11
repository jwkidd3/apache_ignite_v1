package com.example.ignite.solutions.lab09;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.cache.query.ScanQuery;
import org.apache.ignite.cluster.ClusterNode;
import org.apache.ignite.compute.ComputeJob;
import org.apache.ignite.compute.ComputeJobResult;
import org.apache.ignite.compute.ComputeJobResultPolicy;
import org.apache.ignite.compute.ComputeTaskAdapter;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.resources.IgniteInstanceResource;

import javax.cache.Cache;
import java.io.Serializable;
import java.util.*;

/**
 * Lab 09 Exercise 4: MapReduce Implementation
 *
 * Demonstrates:
 * - ComputeTaskAdapter for MapReduce pattern
 * - Word count across distributed documents
 * - Numeric aggregation (statistics)
 * - Map and Reduce phases
 */
public class Lab09MapReduce {

    public static void main(String[] args) {
        try (Ignite ignite = Ignition.start()) {
            System.out.println("=== MapReduce Implementation Lab ===\n");

            // Create cache with sample text data
            CacheConfiguration<Integer, String> textCfg =
                new CacheConfiguration<>("textCache");
            textCfg.setBackups(1);

            IgniteCache<Integer, String> textCache = ignite.getOrCreateCache(textCfg);

            // Populate with sample documents
            textCache.put(1, "Apache Ignite is a distributed database");
            textCache.put(2, "Ignite provides in-memory computing capabilities");
            textCache.put(3, "MapReduce is a programming model for distributed processing");
            textCache.put(4, "Distributed computing enables parallel data processing");
            textCache.put(5, "Apache Ignite supports SQL and key-value operations");
            textCache.put(6, "In-memory computing provides low latency access");
            textCache.put(7, "Ignite compute grid distributes tasks across nodes");
            textCache.put(8, "Data processing at scale requires distributed systems");

            System.out.println("Loaded " + textCache.size() + " documents\n");

            // Execute Word Count MapReduce
            System.out.println("=== Word Count MapReduce ===");
            System.out.println("Executing distributed word count...\n");

            Map<String, Integer> wordCounts = ignite.compute().execute(
                new WordCountTask(),
                "textCache"
            );

            // Display top 15 words
            System.out.println("Top 15 Word Counts:");
            wordCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(15)
                .forEach(e -> System.out.printf("  %-15s : %d%n", e.getKey(), e.getValue()));

            // Example 2: Numeric aggregation MapReduce
            System.out.println("\n=== Numeric Aggregation MapReduce ===");

            CacheConfiguration<Integer, Double> numCfg =
                new CacheConfiguration<>("salesCache");
            IgniteCache<Integer, Double> salesCache = ignite.getOrCreateCache(numCfg);

            // Simulate sales data
            Random random = new Random(42);
            for (int i = 1; i <= 1000; i++) {
                salesCache.put(i, random.nextDouble() * 1000);
            }

            System.out.println("Loaded 1000 sales records");

            // Execute statistics MapReduce
            SalesStatistics stats = ignite.compute().execute(
                new SalesStatisticsTask(),
                "salesCache"
            );

            System.out.println("\nSales Statistics:");
            System.out.printf("  Count: %d%n", stats.count);
            System.out.printf("  Sum:   $%.2f%n", stats.sum);
            System.out.printf("  Avg:   $%.2f%n", stats.average);
            System.out.printf("  Min:   $%.2f%n", stats.min);
            System.out.printf("  Max:   $%.2f%n", stats.max);

            System.out.println("\n=== MapReduce Pattern Explained ===");
            System.out.println("1. MAP Phase:");
            System.out.println("   - Task splits work into jobs");
            System.out.println("   - Jobs distributed to cluster nodes");
            System.out.println("   - Each job processes local data");
            System.out.println("2. REDUCE Phase:");
            System.out.println("   - Results collected from all jobs");
            System.out.println("   - Aggregated into final result");
            System.out.println("3. Benefits:");
            System.out.println("   - Parallel processing");
            System.out.println("   - Data locality");
            System.out.println("   - Horizontal scalability");

            System.out.println("\nPress Enter to exit...");
            System.in.read();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Word Count MapReduce Task
     */
    static class WordCountTask extends ComputeTaskAdapter<String, Map<String, Integer>> {
        @IgniteInstanceResource
        private Ignite ignite;

        @Override
        public Map<? extends ComputeJob, ClusterNode> map(
                List<ClusterNode> nodes, String cacheName) {

            Map<ComputeJob, ClusterNode> jobMap = new HashMap<>();
            IgniteCache<Integer, String> cache = ignite.cache(cacheName);

            // Create a job for each cache entry, mapped to primary node
            for (Cache.Entry<Integer, String> entry : cache.query(new ScanQuery<Integer, String>())) {
                ClusterNode primaryNode = ignite.affinity(cacheName)
                    .mapKeyToNode(entry.getKey());

                jobMap.put(new WordCountJob(entry.getValue()), primaryNode);
            }

            System.out.println("Created " + jobMap.size() + " word count jobs");
            return jobMap;
        }

        @Override
        public Map<String, Integer> reduce(List<ComputeJobResult> results) {
            Map<String, Integer> totalCounts = new HashMap<>();

            for (ComputeJobResult result : results) {
                if (result.getException() != null) {
                    System.err.println("Job failed: " + result.getException().getMessage());
                    continue;
                }

                Map<String, Integer> jobCounts = result.getData();
                for (Map.Entry<String, Integer> entry : jobCounts.entrySet()) {
                    totalCounts.merge(entry.getKey(), entry.getValue(), Integer::sum);
                }
            }

            System.out.println("Reduced " + results.size() + " job results");
            return totalCounts;
        }

        @Override
        public ComputeJobResultPolicy result(ComputeJobResult res,
                List<ComputeJobResult> rcvd) {
            // Continue even if some jobs fail
            if (res.getException() != null) {
                System.err.println("Job exception: " + res.getException().getMessage());
                return ComputeJobResultPolicy.FAILOVER;
            }
            return ComputeJobResultPolicy.WAIT;
        }
    }

    /**
     * Word Count Job - processes a single document
     */
    static class WordCountJob implements ComputeJob, Serializable {
        private final String text;

        WordCountJob(String text) {
            this.text = text;
        }

        @Override
        public Map<String, Integer> execute() {
            Map<String, Integer> counts = new HashMap<>();

            if (text != null && !text.isEmpty()) {
                // Normalize: lowercase, remove punctuation, split on whitespace
                String[] words = text.toLowerCase()
                    .replaceAll("[^a-z0-9\\s]", "")
                    .split("\\s+");

                for (String word : words) {
                    if (!word.isEmpty()) {
                        counts.merge(word, 1, Integer::sum);
                    }
                }
            }

            return counts;
        }

        @Override
        public void cancel() {
            // Handle cancellation if needed
        }
    }

    /**
     * Sales Statistics Result
     */
    static class SalesStatistics implements Serializable {
        int count;
        double sum;
        double min;
        double max;
        double average;

        SalesStatistics() {
            this.min = Double.MAX_VALUE;
            this.max = Double.MIN_VALUE;
        }

        void merge(SalesStatistics other) {
            this.count += other.count;
            this.sum += other.sum;
            this.min = Math.min(this.min, other.min);
            this.max = Math.max(this.max, other.max);
        }

        void calculateAverage() {
            this.average = count > 0 ? sum / count : 0;
        }
    }

    /**
     * Sales Statistics MapReduce Task
     */
    static class SalesStatisticsTask extends ComputeTaskAdapter<String, SalesStatistics> {
        @IgniteInstanceResource
        private Ignite ignite;

        @Override
        public Map<? extends ComputeJob, ClusterNode> map(
                List<ClusterNode> nodes, String cacheName) {

            Map<ComputeJob, ClusterNode> jobMap = new HashMap<>();
            IgniteCache<Integer, Double> cache = ignite.cache(cacheName);

            // Group values by their primary node
            Map<ClusterNode, List<Double>> nodeValues = new HashMap<>();

            for (Cache.Entry<Integer, Double> entry : cache.query(new ScanQuery<Integer, Double>())) {
                ClusterNode primaryNode = ignite.affinity(cacheName)
                    .mapKeyToNode(entry.getKey());

                nodeValues.computeIfAbsent(primaryNode, k -> new ArrayList<>())
                    .add(entry.getValue());
            }

            // Create one job per node with all its local values
            for (Map.Entry<ClusterNode, List<Double>> entry : nodeValues.entrySet()) {
                jobMap.put(new StatisticsJob(entry.getValue()), entry.getKey());
            }

            System.out.println("Created " + jobMap.size() + " statistics jobs");
            return jobMap;
        }

        @Override
        public SalesStatistics reduce(List<ComputeJobResult> results) {
            SalesStatistics total = new SalesStatistics();

            for (ComputeJobResult result : results) {
                if (result.getException() == null) {
                    total.merge(result.getData());
                }
            }

            total.calculateAverage();
            return total;
        }
    }

    /**
     * Statistics Job - processes a batch of values
     */
    static class StatisticsJob implements ComputeJob, Serializable {
        private final List<Double> values;

        StatisticsJob(List<Double> values) {
            this.values = values;
        }

        @Override
        public SalesStatistics execute() {
            SalesStatistics stats = new SalesStatistics();
            stats.count = values.size();

            for (Double value : values) {
                stats.sum += value;
                stats.min = Math.min(stats.min, value);
                stats.max = Math.max(stats.max, value);
            }

            return stats;
        }

        @Override
        public void cancel() {}
    }
}
