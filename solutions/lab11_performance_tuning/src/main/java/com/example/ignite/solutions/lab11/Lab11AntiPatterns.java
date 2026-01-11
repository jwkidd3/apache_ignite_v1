package com.example.ignite.solutions.lab11;

/**
 * Lab 11 Exercise 4: Performance Anti-Patterns
 *
 * Demonstrates common performance mistakes and their solutions.
 */
public class Lab11AntiPatterns {

    public static void main(String[] args) {
        System.out.println("=== Performance Anti-Patterns ===\n");

        System.out.println("1. Small Heap with Large Off-Heap");
        System.out.println("   BAD:  -Xmx1g with 10GB off-heap");
        System.out.println("   GOOD: -Xmx4g with 10GB off-heap");
        System.out.println("   Reason: GC overhead, metadata needs heap space\n");

        System.out.println("2. Too Many Backups");
        System.out.println("   BAD:  setBackups(3) in 4-node cluster");
        System.out.println("   GOOD: setBackups(1) or setBackups(2)");
        System.out.println("   Reason: Excessive memory usage, network overhead\n");

        System.out.println("3. Large Transactions");
        System.out.println("   BAD:  Updating 10,000 entries in one transaction");
        System.out.println("   GOOD: Batch updates in smaller transactions (100-500)");
        System.out.println("   Reason: Lock contention, memory pressure\n");

        System.out.println("4. Synchronous Operations in Loops");
        System.out.println("   BAD:  for (key : keys) cache.put(key, value)");
        System.out.println("   GOOD: cache.putAll(map)");
        System.out.println("   Reason: Network round trips\n");

        System.out.println("5. No Affinity Keys");
        System.out.println("   BAD:  Related data on different nodes");
        System.out.println("   GOOD: Use @AffinityKeyMapped");
        System.out.println("   Reason: Distributed joins are expensive\n");

        System.out.println("6. Missing Indexes");
        System.out.println("   BAD:  SELECT * FROM Table WHERE name = 'John'");
        System.out.println("   GOOD: CREATE INDEX ON Table(name)");
        System.out.println("   Reason: Full scan instead of index lookup\n");

        System.out.println("7. Overusing Distributed Joins");
        System.out.println("   BAD:  setDistributedJoins(true) everywhere");
        System.out.println("   GOOD: Design for data colocation");
        System.out.println("   Reason: Network overhead, memory usage\n");

        System.out.println("8. Not Monitoring GC");
        System.out.println("   BAD:  No GC logging");
        System.out.println("   GOOD: Enable GC logs and monitor");
        System.out.println("   Reason: Hidden performance issues\n");

        System.out.println("9. Default Thread Pool Sizes");
        System.out.println("   BAD:  Using default pools for high load");
        System.out.println("   GOOD: Tune based on workload");
        System.out.println("   Reason: Thread starvation\n");

        System.out.println("10. Ignoring Metrics");
        System.out.println("   BAD:  Not enabling cache statistics");
        System.out.println("   GOOD: Monitor hit ratios, latencies");
        System.out.println("   Reason: Can't optimize what you don't measure\n");

        System.out.println("=== Performance Checklist ===");
        System.out.println("[ ] JVM heap sized appropriately (60-70% of RAM)");
        System.out.println("[ ] G1GC configured with appropriate pause target");
        System.out.println("[ ] Off-heap memory sized for data");
        System.out.println("[ ] Backups set based on HA requirements");
        System.out.println("[ ] Batch operations used where possible");
        System.out.println("[ ] Affinity colocation implemented");
        System.out.println("[ ] SQL indexes created for query fields");
        System.out.println("[ ] Metrics and logging enabled");
        System.out.println("[ ] Thread pools tuned for workload");
        System.out.println("[ ] Network settings optimized");

        System.out.println("\n=== Profiling Tools ===");
        System.out.println("1. Java Flight Recorder (JFR)");
        System.out.println("2. Async Profiler");
        System.out.println("3. VisualVM");
        System.out.println("4. JProfiler / YourKit");
        System.out.println("5. Ignite built-in metrics");
    }
}
