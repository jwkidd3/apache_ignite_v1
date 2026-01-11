# Lab 9: Compute Grid Fundamentals

## Duration: 50 minutes

## Objectives
- Understand distributed computing concepts in Ignite
- Implement compute closures and jobs
- Use task execution and load balancing
- Create MapReduce operations

## Prerequisites
- Completed Labs 1-8
- Understanding of distributed systems
- Java functional programming knowledge

## Part 1: Basic Compute Operations (15 minutes)

### Exercise 1: Simple Compute Tasks

Create `Lab09BasicCompute.java`:

```java
package com.example.ignite;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCompute;
import org.apache.ignite.Ignition;
import org.apache.ignite.lang.IgniteCallable;
import org.apache.ignite.lang.IgniteRunnable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class Lab09BasicCompute {

    public static void main(String[] args) {
        try (Ignite ignite = Ignition.start()) {
            System.out.println("=== Basic Compute Grid Lab ===\n");
            System.out.println("Cluster nodes: " + ignite.cluster().nodes().size());

            IgniteCompute compute = ignite.compute();

            // Example 1: Run a simple task
            System.out.println("\n=== Example 1: Simple Runnable ===");
            compute.run(() -> {
                System.out.println("Hello from node: " +
                    Ignition.ignite().cluster().localNode().id());
            });

            // Example 2: Broadcast to all nodes
            System.out.println("\n=== Example 2: Broadcast to All Nodes ===");
            compute.broadcast(() -> {
                System.out.println("[" +
                    Ignition.ignite().cluster().localNode().consistentId() +
                    "] Received broadcast message");
            });

            Thread.sleep(1000);

            // Example 3: Callable with return value
            System.out.println("\n=== Example 3: Callable with Return Value ===");
            String result = compute.call(() -> {
                return "Computed on node: " +
                    Ignition.ignite().cluster().localNode().consistentId();
            });
            System.out.println("Result: " + result);

            // Example 4: Multiple callables
            System.out.println("\n=== Example 4: Multiple Callables ===");
            Collection<IgniteCallable<Integer>> calls = new ArrayList<>();

            for (int i = 1; i <= 5; i++) {
                final int num = i;
                calls.add(() -> {
                    System.out.println("Computing square of " + num +
                        " on node " + Ignition.ignite().cluster().localNode().consistentId());
                    return num * num;
                });
            }

            Collection<Integer> results = compute.call(calls);
            System.out.println("\nResults: " + results);

            // Example 5: Execute with load balancing
            System.out.println("\n=== Example 5: Load Balancing ===");
            for (int i = 0; i < 10; i++) {
                final int taskNum = i;
                compute.run(() -> {
                    System.out.println("Task " + taskNum + " on node " +
                        Ignition.ignite().cluster().localNode().consistentId());
                });
            }

            Thread.sleep(1000);

            System.out.println("\n=== Compute Grid Features ===");
            System.out.println("- Automatic load balancing");
            System.out.println("- Failover support");
            System.out.println("- Task distribution across nodes");
            System.out.println("- Parallel execution");

            System.out.println("\nPress Enter to exit...");
            System.in.read();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

## Part 2: Closure-Based Computing (10 minutes)

### Exercise 2: Advanced Closures and Resource Injection

Create `Lab09ClosureCompute.java`:

```java
package com.example.ignite;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.IgniteCompute;
import org.apache.ignite.Ignition;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.lang.IgniteClosure;
import org.apache.ignite.lang.IgniteReducer;
import org.apache.ignite.resources.IgniteInstanceResource;

import java.util.Arrays;
import java.util.Collection;

public class Lab09ClosureCompute {

    public static void main(String[] args) {
        try (Ignite ignite = Ignition.start()) {
            System.out.println("=== Closure-Based Computing Lab ===\n");

            IgniteCompute compute = ignite.compute();

            // Example 1: Apply closure to collection
            System.out.println("=== Example 1: Apply Closure to Collection ===");

            Collection<String> words = Arrays.asList("Hello", "Apache", "Ignite", "Compute");

            // Apply closure to each element - distributed across cluster
            Collection<Integer> lengths = compute.apply(
                (String word) -> {
                    System.out.println("Processing '" + word + "' on node: " +
                        Ignition.ignite().cluster().localNode().consistentId());
                    return word.length();
                },
                words
            );

            System.out.println("Word lengths: " + lengths);

            // Example 2: Apply with reducer
            System.out.println("\n=== Example 2: Apply with Reducer ===");

            Collection<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);

            Integer sum = compute.apply(
                (Integer n) -> n * n,  // Square each number
                numbers,
                new IgniteReducer<Integer, Integer>() {
                    private int sum = 0;

                    @Override
                    public boolean collect(Integer result) {
                        sum += result;
                        return true;  // Continue collecting
                    }

                    @Override
                    public Integer reduce() {
                        return sum;
                    }
                }
            );

            System.out.println("Sum of squares (1-10): " + sum);
            System.out.println("Expected: 385");

            // Example 3: Chained closures
            System.out.println("\n=== Example 3: Processing Pipeline ===");

            CacheConfiguration<Integer, String> cfg = new CacheConfiguration<>("pipelineCache");
            IgniteCache<Integer, String> cache = ignite.getOrCreateCache(cfg);

            // Load sample data
            cache.put(1, "  Hello World  ");
            cache.put(2, "  Apache Ignite  ");
            cache.put(3, "  Distributed Computing  ");

            // Process with closure pipeline
            for (int key = 1; key <= 3; key++) {
                final int k = key;
                String processed = compute.call(() -> {
                    String value = Ignition.ignite().<Integer, String>cache("pipelineCache").get(k);
                    // Trim, uppercase, add prefix
                    return "PROCESSED: " + value.trim().toUpperCase();
                });
                System.out.println("Key " + key + ": " + processed);
            }

            Thread.sleep(500);

            System.out.println("\n=== Closure Computing Benefits ===");
            System.out.println("- Functional programming style");
            System.out.println("- Automatic distribution");
            System.out.println("- Built-in reduction");
            System.out.println("- Composable operations");

            System.out.println("\nPress Enter to exit...");
            System.in.read();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

## Part 3: Cluster Groups and Targeted Execution (5 minutes)

### Exercise 3: Execute on Specific Node Groups

Create `Lab09ClusterGroups.java`:

```java
package com.example.ignite;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCompute;
import org.apache.ignite.Ignition;
import org.apache.ignite.cluster.ClusterGroup;
import org.apache.ignite.cluster.ClusterNode;

import java.util.Collection;

public class Lab09ClusterGroups {

    public static void main(String[] args) {
        try (Ignite ignite = Ignition.start()) {
            System.out.println("=== Cluster Groups and Targeted Execution ===\n");

            // Get cluster information
            Collection<ClusterNode> allNodes = ignite.cluster().nodes();
            System.out.println("Total nodes in cluster: " + allNodes.size());

            // Example 1: Execute on server nodes only
            System.out.println("\n=== Execute on Server Nodes ===");
            ClusterGroup serverGroup = ignite.cluster().forServers();
            IgniteCompute serverCompute = ignite.compute(serverGroup);

            serverCompute.broadcast(() -> {
                System.out.println("Server node: " +
                    Ignition.ignite().cluster().localNode().consistentId());
            });

            Thread.sleep(300);

            // Example 2: Execute on client nodes
            System.out.println("\n=== Execute on Client Nodes ===");
            ClusterGroup clientGroup = ignite.cluster().forClients();

            if (!clientGroup.nodes().isEmpty()) {
                ignite.compute(clientGroup).broadcast(() -> {
                    System.out.println("Client node: " +
                        Ignition.ignite().cluster().localNode().consistentId());
                });
            } else {
                System.out.println("No client nodes in cluster");
            }

            // Example 3: Execute on oldest node (coordinator)
            System.out.println("\n=== Execute on Oldest Node ===");
            ClusterGroup oldestNode = ignite.cluster().forOldest();

            ignite.compute(oldestNode).run(() -> {
                ClusterNode node = Ignition.ignite().cluster().localNode();
                System.out.println("Oldest node: " + node.consistentId());
                System.out.println("Node order: " + node.order());
            });

            Thread.sleep(300);

            // Example 4: Execute on youngest node
            System.out.println("\n=== Execute on Youngest Node ===");
            ClusterGroup youngestNode = ignite.cluster().forYoungest();

            ignite.compute(youngestNode).run(() -> {
                ClusterNode node = Ignition.ignite().cluster().localNode();
                System.out.println("Youngest node: " + node.consistentId());
                System.out.println("Node order: " + node.order());
            });

            Thread.sleep(300);

            // Example 5: Execute on nodes with specific attribute
            System.out.println("\n=== Execute on Nodes with Attribute ===");
            // Note: Attributes must be set in node configuration
            ClusterGroup attrGroup = ignite.cluster().forAttribute("ROLE", "COMPUTE");

            if (!attrGroup.nodes().isEmpty()) {
                ignite.compute(attrGroup).broadcast(() -> {
                    System.out.println("Compute role node executing");
                });
            } else {
                System.out.println("No nodes with ROLE=COMPUTE attribute");
            }

            // Example 6: Execute on random node
            System.out.println("\n=== Execute on Random Node ===");
            ClusterGroup randomNode = ignite.cluster().forRandom();

            for (int i = 0; i < 5; i++) {
                ignite.compute(randomNode).run(() -> {
                    System.out.println("Random execution on: " +
                        Ignition.ignite().cluster().localNode().consistentId());
                });
            }

            Thread.sleep(500);

            System.out.println("\n=== Cluster Group Use Cases ===");
            System.out.println("- forServers(): Heavy computation tasks");
            System.out.println("- forClients(): Lightweight coordination");
            System.out.println("- forOldest(): Singleton services, coordination");
            System.out.println("- forAttribute(): Role-based task routing");
            System.out.println("- forRemotes(): Exclude local node");

            System.out.println("\nPress Enter to exit...");
            System.in.read();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

## Part 4: MapReduce Implementation (15 minutes)

### Exercise 4: Implement ComputeTaskAdapter

Create `Lab09MapReduce.java`:

```java
package com.example.ignite;

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
        public Map<ClusterNode, List<ComputeJob>> map(
                List<ClusterNode> nodes, String cacheName) {

            Map<ClusterNode, List<ComputeJob>> jobMap = new HashMap<>();
            IgniteCache<Integer, String> cache = ignite.cache(cacheName);

            // Create a job for each cache entry, mapped to primary node
            for (Cache.Entry<Integer, String> entry : cache.query(new ScanQuery<Integer, String>())) {
                ClusterNode primaryNode = ignite.affinity(cacheName)
                    .mapKeyToNode(entry.getKey());

                jobMap.computeIfAbsent(primaryNode, k -> new ArrayList<>())
                    .add(new WordCountJob(entry.getValue()));
            }

            System.out.println("Created " +
                jobMap.values().stream().mapToInt(List::size).sum() +
                " jobs across " + jobMap.size() + " nodes");

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

        void finalize() {
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
        public Map<ClusterNode, List<ComputeJob>> map(
                List<ClusterNode> nodes, String cacheName) {

            Map<ClusterNode, List<ComputeJob>> jobMap = new HashMap<>();
            IgniteCache<Integer, Double> cache = ignite.cache(cacheName);

            // Create batch jobs per node
            Map<ClusterNode, List<Double>> nodeValues = new HashMap<>();

            for (Cache.Entry<Integer, Double> entry : cache.query(new ScanQuery<Integer, Double>())) {
                ClusterNode node = ignite.affinity(cacheName).mapKeyToNode(entry.getKey());
                nodeValues.computeIfAbsent(node, k -> new ArrayList<>())
                    .add(entry.getValue());
            }

            for (Map.Entry<ClusterNode, List<Double>> entry : nodeValues.entrySet()) {
                List<ComputeJob> jobs = new ArrayList<>();
                jobs.add(new SalesStatisticsJob(entry.getValue()));
                jobMap.put(entry.getKey(), jobs);
            }

            return jobMap;
        }

        @Override
        public SalesStatistics reduce(List<ComputeJobResult> results) {
            SalesStatistics total = new SalesStatistics();
            total.min = Double.MAX_VALUE;
            total.max = Double.MIN_VALUE;

            for (ComputeJobResult result : results) {
                if (result.getException() == null) {
                    SalesStatistics partial = result.getData();
                    total.merge(partial);
                }
            }

            total.finalize();
            return total;
        }
    }

    /**
     * Sales Statistics Job
     */
    static class SalesStatisticsJob implements ComputeJob, Serializable {
        private final List<Double> values;

        SalesStatisticsJob(List<Double> values) {
            this.values = values;
        }

        @Override
        public SalesStatistics execute() {
            SalesStatistics stats = new SalesStatistics();

            for (Double value : values) {
                stats.count++;
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
```

---

## Verification Steps

### Checklist
- [ ] Basic compute operations (call, broadcast, run) completed
- [ ] Closure-based distributed computing demonstrated
- [ ] Cluster groups used for targeted execution
- [ ] MapReduce pattern implemented with ComputeTask

### Common Issues

**Issue: Compute task not executing on remote nodes**
- Ensure peer class loading is enabled
- Verify nodes can communicate

**Issue: Results not collected correctly**
- Check reduce() method implementation
- Verify result type matches

## Lab Questions

1. What is the difference between `call()` and `run()` compute methods?
2. When would you use cluster groups for compute operations?
3. How does MapReduce in Ignite differ from traditional MapReduce?

## Answers

1. **call()** returns a result from the callable, while **run()** executes a runnable with no return value.

2. Use cluster groups to target specific nodes (e.g., by attribute, role) for operations that require specific resources or should run on certain node types.

3. Ignite MapReduce is in-memory, doesn't require HDFS, executes on the same cluster storing data, and supports data affinity for co-located processing.

## Next Steps

In Lab 10, you will:
- Learn integration and connectivity options
- Work with REST API and thin clients
- Configure external system integration

## Completion

You have completed Lab 9 when you can:
- Execute basic compute operations
- Implement closures for distributed computing
- Use cluster groups for targeted execution
- Create MapReduce tasks

---

## Optional Exercises (If Time Permits)

### Optional: Affinity-Aware Computing

Create `Lab09AffinityCompute.java` for data-colocated computation:

```java
package com.example.ignite;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.IgniteCompute;
import org.apache.ignite.Ignition;
import org.apache.ignite.cache.CacheMode;
import org.apache.ignite.cache.affinity.Affinity;
import org.apache.ignite.cluster.ClusterNode;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.lang.IgniteCallable;
import org.apache.ignite.lang.IgniteRunnable;
import org.apache.ignite.resources.IgniteInstanceResource;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class Lab09AffinityCompute {

    public static void main(String[] args) {
        try (Ignite ignite = Ignition.start()) {
            System.out.println("=== Affinity-Aware Computing Lab ===\n");

            // Create partitioned cache
            CacheConfiguration<Integer, Account> cfg = new CacheConfiguration<>("accountCache");
            cfg.setCacheMode(CacheMode.PARTITIONED);
            cfg.setBackups(1);

            IgniteCache<Integer, Account> cache = ignite.getOrCreateCache(cfg);

            // Populate cache with account data
            System.out.println("Loading account data...");
            for (int i = 1; i <= 1000; i++) {
                cache.put(i, new Account(i, "Account-" + i, 1000.0 + i));
            }
            System.out.println("Loaded 1000 accounts\n");

            IgniteCompute compute = ignite.compute();

            // ============================================
            // Comparison: Non-Affinity vs Affinity Compute
            // ============================================

            System.out.println("=== Performance Comparison ===\n");

            // Test 1: Non-affinity computation (data may be remote)
            System.out.println("Test 1: Non-Affinity Computation");
            System.out.println("(Tasks may run on nodes without the data)");

            long startNonAffinity = System.currentTimeMillis();
            double totalNonAffinity = 0;

            for (int i = 1; i <= 100; i++) {
                final int accountId = i;
                Double balance = compute.call(() -> {
                    IgniteCache<Integer, Account> c =
                        Ignition.ignite().cache("accountCache");
                    Account account = c.get(accountId);  // May require network hop
                    return account != null ? account.getBalance() : 0.0;
                });
                totalNonAffinity += balance;
            }

            long nonAffinityTime = System.currentTimeMillis() - startNonAffinity;
            System.out.println("Time: " + nonAffinityTime + " ms");
            System.out.println("Total balance: $" + String.format("%.2f", totalNonAffinity));

            // Test 2: Affinity-aware computation (data is always local)
            System.out.println("\nTest 2: Affinity-Aware Computation");
            System.out.println("(Tasks run on nodes owning the data)");

            long startAffinity = System.currentTimeMillis();
            double totalAffinity = 0;

            for (int i = 1; i <= 100; i++) {
                final int accountId = i;
                Double balance = compute.affinityCall("accountCache", accountId,
                    new IgniteCallable<Double>() {
                        @IgniteInstanceResource
                        private Ignite ignite;

                        @Override
                        public Double call() {
                            IgniteCache<Integer, Account> c = ignite.cache("accountCache");
                            // localPeek - no network hop, data is guaranteed local
                            Account account = c.localPeek(accountId);
                            return account != null ? account.getBalance() : 0.0;
                        }
                    });
                totalAffinity += balance;
            }

            long affinityTime = System.currentTimeMillis() - startAffinity;
            System.out.println("Time: " + affinityTime + " ms");
            System.out.println("Total balance: $" + String.format("%.2f", totalAffinity));

            // Performance comparison
            System.out.println("\n--- Performance Summary ---");
            System.out.println("Non-affinity time: " + nonAffinityTime + " ms");
            System.out.println("Affinity time:     " + affinityTime + " ms");
            if (affinityTime > 0) {
                System.out.printf("Speedup:           %.2fx%n",
                    (double) nonAffinityTime / affinityTime);
            }

            // ============================================
            // affinityRun Example: Update Operations
            // ============================================

            System.out.println("\n=== affinityRun Example: Account Updates ===");

            // Apply interest to specific accounts using affinity
            int[] accountsToUpdate = {10, 20, 30, 40, 50};
            double interestRate = 0.05;

            for (int accountId : accountsToUpdate) {
                compute.affinityRun("accountCache", accountId, new IgniteRunnable() {
                    @IgniteInstanceResource
                    private Ignite ignite;

                    @Override
                    public void run() {
                        IgniteCache<Integer, Account> c = ignite.cache("accountCache");
                        Account account = c.localPeek(accountId);

                        if (account != null) {
                            double newBalance = account.getBalance() * (1 + interestRate);
                            account.setBalance(newBalance);
                            c.put(accountId, account);

                            System.out.println("Updated account " + accountId +
                                " on node " + ignite.cluster().localNode().consistentId() +
                                " - New balance: $" + String.format("%.2f", newBalance));
                        }
                    }
                });
            }

            Thread.sleep(500);

            // ============================================
            // Batch Affinity Operations
            // ============================================

            System.out.println("\n=== Batch Affinity Operations ===");

            // Group keys by affinity partition for batch processing
            Affinity<Integer> affinity = ignite.affinity("accountCache");

            System.out.println("Partition distribution:");
            for (int partition = 0; partition < Math.min(5, affinity.partitions()); partition++) {
                ClusterNode primary = affinity.mapPartitionToNode(partition);
                System.out.println("  Partition " + partition + " -> Node " +
                    primary.consistentId());
            }

            // Process all accounts in a specific partition
            int targetPartition = 0;
            System.out.println("\nProcessing all accounts in partition " + targetPartition);

            Collection<Integer> partitionKeys = new ArrayList<>();
            for (int i = 1; i <= 1000; i++) {
                if (affinity.partition(i) == targetPartition) {
                    partitionKeys.add(i);
                }
            }
            System.out.println("Found " + partitionKeys.size() + " accounts in partition " + targetPartition);

            // ============================================
            // Multiple Cache Affinity
            // ============================================

            System.out.println("\n=== Multiple Cache Affinity ===");

            // Create related cache with same affinity
            CacheConfiguration<Integer, Transaction> txCfg =
                new CacheConfiguration<>("transactionCache");
            txCfg.setCacheMode(CacheMode.PARTITIONED);
            txCfg.setBackups(1);

            IgniteCache<Integer, Transaction> txCache = ignite.getOrCreateCache(txCfg);

            // Add some transactions
            for (int i = 1; i <= 100; i++) {
                txCache.put(i, new Transaction(i, i, 50.0, "Purchase"));
            }

            // Process account and its transactions together using affinity
            int accountId = 25;
            compute.affinityRun(
                Arrays.asList("accountCache", "transactionCache"),  // Multiple caches
                accountId,
                new IgniteRunnable() {
                    @IgniteInstanceResource
                    private Ignite ignite;

                    @Override
                    public void run() {
                        IgniteCache<Integer, Account> accounts = ignite.cache("accountCache");
                        IgniteCache<Integer, Transaction> transactions = ignite.cache("transactionCache");

                        Account account = accounts.localPeek(accountId);
                        Transaction tx = transactions.localPeek(accountId);

                        if (account != null && tx != null) {
                            System.out.println("Processing locally on node " +
                                ignite.cluster().localNode().consistentId());
                            System.out.println("  Account: " + account.getName() +
                                " Balance: $" + account.getBalance());
                            System.out.println("  Transaction: " + tx.getDescription() +
                                " Amount: $" + tx.getAmount());
                        }
                    }
                }
            );

            Thread.sleep(500);

            System.out.println("\n=== Affinity Computing Best Practices ===");
            System.out.println("1. Use affinityRun/affinityCall for data-intensive tasks");
            System.out.println("2. Use localPeek() instead of get() for guaranteed local access");
            System.out.println("3. Colocate related data using affinity keys");
            System.out.println("4. Batch operations by partition for efficiency");
            System.out.println("5. Monitor network traffic to verify data locality");

            System.out.println("\nPress Enter to exit...");
            System.in.read();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Helper method for multi-cache affinity
    private static java.util.List<String> Arrays_asList(String... items) {
        java.util.List<String> list = new java.util.ArrayList<>();
        for (String item : items) {
            list.add(item);
        }
        return list;
    }

    static class Account implements Serializable {
        private int id;
        private String name;
        private double balance;

        Account(int id, String name, double balance) {
            this.id = id;
            this.name = name;
            this.balance = balance;
        }

        int getId() { return id; }
        String getName() { return name; }
        double getBalance() { return balance; }
        void setBalance(double balance) { this.balance = balance; }
    }

    static class Transaction implements Serializable {
        private int id;
        private int accountId;
        private double amount;
        private String description;

        Transaction(int id, int accountId, double amount, String description) {
            this.id = id;
            this.accountId = accountId;
            this.amount = amount;
            this.description = description;
        }

        int getId() { return id; }
        int getAccountId() { return accountId; }
        double getAmount() { return amount; }
        String getDescription() { return description; }
    }
}
```

### Optional: Compute with Failover

### Exercise 6: Fault-Tolerant Compute Operations

Create `Lab09FailoverCompute.java`:

```java
package com.example.ignite;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCompute;
import org.apache.ignite.IgniteException;
import org.apache.ignite.Ignition;
import org.apache.ignite.cluster.ClusterNode;
import org.apache.ignite.compute.*;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.lang.IgniteCallable;
import org.apache.ignite.resources.IgniteInstanceResource;
import org.apache.ignite.spi.failover.always.AlwaysFailoverSpi;
import org.apache.ignite.spi.failover.jobstealing.JobStealingFailoverSpi;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class Lab09FailoverCompute {

    public static void main(String[] args) {
        // Configure failover SPI
        IgniteConfiguration cfg = new IgniteConfiguration();

        // Option 1: Always failover (default behavior, enhanced)
        AlwaysFailoverSpi failoverSpi = new AlwaysFailoverSpi();
        failoverSpi.setMaximumFailoverAttempts(5);  // Retry up to 5 times
        cfg.setFailoverSpi(failoverSpi);

        // Alternative Option 2: Job stealing failover
        // JobStealingFailoverSpi stealingSpi = new JobStealingFailoverSpi();
        // stealingSpi.setMaximumFailoverAttempts(3);
        // cfg.setFailoverSpi(stealingSpi);

        try (Ignite ignite = Ignition.start(cfg)) {
            System.out.println("=== Compute with Failover Lab ===\n");
            System.out.println("Failover SPI: AlwaysFailoverSpi (max 5 attempts)");
            System.out.println("Cluster nodes: " + ignite.cluster().nodes().size());

            IgniteCompute compute = ignite.compute();

            // ============================================
            // Example 1: Basic Failover Demonstration
            // ============================================

            System.out.println("\n=== Example 1: Simulated Failover ===");

            try {
                String result = ignite.compute().execute(
                    new FailoverDemoTask(),
                    "test-input"
                );
                System.out.println("Task result: " + result);
            } catch (IgniteException e) {
                System.out.println("Task failed after all retries: " + e.getMessage());
            }

            // ============================================
            // Example 2: Retry Logic with Custom Handler
            // ============================================

            System.out.println("\n=== Example 2: Custom Retry Logic ===");

            RetryableCompute retryableCompute = new RetryableCompute(compute, 3);

            try {
                Integer result = retryableCompute.callWithRetry(() -> {
                    // Simulate occasional failures
                    if (Math.random() < 0.3) {
                        throw new RuntimeException("Simulated transient failure");
                    }
                    System.out.println("Computation successful on node: " +
                        Ignition.ignite().cluster().localNode().consistentId());
                    return 42;
                });
                System.out.println("Result with retry: " + result);
            } catch (Exception e) {
                System.out.println("Failed after retries: " + e.getMessage());
            }

            // ============================================
            // Example 3: Fault-Tolerant Task with Checkpointing
            // ============================================

            System.out.println("\n=== Example 3: Task with Progress Tracking ===");

            try {
                ProcessingResult result = ignite.compute().execute(
                    new FaultTolerantProcessingTask(),
                    100  // Process 100 items
                );
                System.out.println("Processing complete:");
                System.out.println("  Items processed: " + result.processedCount);
                System.out.println("  Failed items: " + result.failedCount);
                System.out.println("  Success rate: " +
                    String.format("%.1f%%", result.successRate * 100));
            } catch (Exception e) {
                System.out.println("Task failed: " + e.getMessage());
            }

            // ============================================
            // Example 4: Compute with Timeout and Failover
            // ============================================

            System.out.println("\n=== Example 4: Timeout with Failover ===");

            try {
                // Set timeout - if exceeded, task will failover to another node
                String result = compute.withTimeout(5000).call(() -> {
                    System.out.println("Starting task with 5s timeout on node: " +
                        Ignition.ignite().cluster().localNode().consistentId());

                    // Simulate work
                    Thread.sleep(1000);

                    return "Completed within timeout";
                });
                System.out.println("Result: " + result);
            } catch (ComputeTaskTimeoutException e) {
                System.out.println("Task timed out - would failover in multi-node cluster");
            }

            // ============================================
            // Example 5: No-Failover Mode
            // ============================================

            System.out.println("\n=== Example 5: No-Failover Mode ===");

            try {
                // Disable failover for this specific computation
                String result = compute.withNoFailover().call(() -> {
                    System.out.println("Running without failover on node: " +
                        Ignition.ignite().cluster().localNode().consistentId());
                    return "No-failover result";
                });
                System.out.println("Result: " + result);
            } catch (Exception e) {
                System.out.println("Failed without retry: " + e.getMessage());
            }

            // ============================================
            // Failover Best Practices
            // ============================================

            System.out.println("\n=== Failover Configuration Options ===");
            System.out.println("1. AlwaysFailoverSpi - Always retry on failure");
            System.out.println("2. JobStealingFailoverSpi - Redistribute to less busy nodes");
            System.out.println("3. NeverFailoverSpi - Never retry (for idempotency concerns)");
            System.out.println("4. Custom FailoverSpi - Implement your own logic");

            System.out.println("\n=== Best Practices ===");
            System.out.println("- Make tasks idempotent when possible");
            System.out.println("- Set reasonable timeout values");
            System.out.println("- Use checkpointing for long-running tasks");
            System.out.println("- Log failover events for debugging");
            System.out.println("- Consider using withNoFailover() for non-retryable ops");
            System.out.println("- Implement proper error handling in reduce phase");

            System.out.println("\nPress Enter to exit...");
            System.in.read();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Demonstrates failover behavior in ComputeTask
     */
    static class FailoverDemoTask extends ComputeTaskAdapter<String, String> {
        private static final AtomicInteger attemptCount = new AtomicInteger(0);

        @Override
        public Map<ClusterNode, List<ComputeJob>> map(
                List<ClusterNode> nodes, String arg) {

            Map<ClusterNode, List<ComputeJob>> map = new HashMap<>();

            // Distribute job to first available node
            if (!nodes.isEmpty()) {
                List<ComputeJob> jobs = new ArrayList<>();
                jobs.add(new FailoverDemoJob(arg));
                map.put(nodes.get(0), jobs);
            }

            return map;
        }

        @Override
        public String reduce(List<ComputeJobResult> results) {
            for (ComputeJobResult result : results) {
                if (result.getException() == null) {
                    return result.getData();
                }
            }
            return "All jobs failed";
        }

        @Override
        public ComputeJobResultPolicy result(ComputeJobResult res,
                List<ComputeJobResult> rcvd) {

            if (res.getException() != null) {
                int attempt = attemptCount.incrementAndGet();
                System.out.println("Job failed (attempt " + attempt + "), requesting failover");

                if (attempt < 3) {
                    return ComputeJobResultPolicy.FAILOVER;
                } else {
                    System.out.println("Max attempts reached, accepting failure");
                    attemptCount.set(0);  // Reset for next task
                    return ComputeJobResultPolicy.WAIT;
                }
            }

            attemptCount.set(0);
            return ComputeJobResultPolicy.WAIT;
        }
    }

    static class FailoverDemoJob implements ComputeJob, Serializable {
        private static final AtomicInteger jobAttempts = new AtomicInteger(0);
        private final String input;

        FailoverDemoJob(String input) {
            this.input = input;
        }

        @Override
        public String execute() {
            int attempt = jobAttempts.incrementAndGet();
            System.out.println("Executing job, attempt " + attempt);

            // Simulate failure on first two attempts
            if (attempt <= 2) {
                throw new RuntimeException("Simulated failure on attempt " + attempt);
            }

            jobAttempts.set(0);  // Reset for next task
            return "Success on attempt " + attempt + " with input: " + input;
        }

        @Override
        public void cancel() {
            System.out.println("Job cancelled");
        }
    }

    /**
     * Utility class for retry logic
     */
    static class RetryableCompute {
        private final IgniteCompute compute;
        private final int maxRetries;

        RetryableCompute(IgniteCompute compute, int maxRetries) {
            this.compute = compute;
            this.maxRetries = maxRetries;
        }

        <T> T callWithRetry(IgniteCallable<T> callable) throws Exception {
            Exception lastException = null;

            for (int attempt = 1; attempt <= maxRetries; attempt++) {
                try {
                    System.out.println("Attempt " + attempt + " of " + maxRetries);
                    return compute.call(callable);
                } catch (Exception e) {
                    lastException = e;
                    System.out.println("Attempt " + attempt + " failed: " + e.getMessage());

                    if (attempt < maxRetries) {
                        // Exponential backoff
                        long waitTime = (long) Math.pow(2, attempt) * 100;
                        System.out.println("Waiting " + waitTime + "ms before retry");
                        Thread.sleep(waitTime);
                    }
                }
            }

            throw lastException;
        }
    }

    /**
     * Processing result with statistics
     */
    static class ProcessingResult implements Serializable {
        int processedCount;
        int failedCount;
        double successRate;
    }

    /**
     * Fault-tolerant processing task with progress tracking
     */
    static class FaultTolerantProcessingTask
            extends ComputeTaskAdapter<Integer, ProcessingResult> {

        @IgniteInstanceResource
        private Ignite ignite;

        @Override
        public Map<ClusterNode, List<ComputeJob>> map(
                List<ClusterNode> nodes, Integer itemCount) {

            Map<ClusterNode, List<ComputeJob>> map = new HashMap<>();

            // Distribute items across nodes
            int itemsPerNode = itemCount / Math.max(1, nodes.size());
            int remaining = itemCount % Math.max(1, nodes.size());

            int itemIndex = 0;
            for (int i = 0; i < nodes.size() && itemIndex < itemCount; i++) {
                int nodeItems = itemsPerNode + (i < remaining ? 1 : 0);

                List<ComputeJob> jobs = new ArrayList<>();
                for (int j = 0; j < nodeItems; j++) {
                    jobs.add(new ProcessingJob(itemIndex++));
                }
                map.put(nodes.get(i), jobs);
            }

            return map;
        }

        @Override
        public ProcessingResult reduce(List<ComputeJobResult> results) {
            ProcessingResult total = new ProcessingResult();

            for (ComputeJobResult result : results) {
                if (result.getException() == null) {
                    Boolean success = result.getData();
                    total.processedCount++;
                    if (!success) {
                        total.failedCount++;
                    }
                } else {
                    total.failedCount++;
                }
            }

            total.successRate = total.processedCount > 0
                ? (double)(total.processedCount - total.failedCount) / total.processedCount
                : 0;

            return total;
        }

        @Override
        public ComputeJobResultPolicy result(ComputeJobResult res,
                List<ComputeJobResult> rcvd) {
            // Continue collecting results even if some jobs fail
            return ComputeJobResultPolicy.WAIT;
        }
    }

    static class ProcessingJob implements ComputeJob, Serializable {
        private final int itemId;

        ProcessingJob(int itemId) {
            this.itemId = itemId;
        }

        @Override
        public Boolean execute() {
            // Simulate processing with occasional failures
            if (Math.random() < 0.05) {  // 5% failure rate
                return false;
            }
            return true;
        }

        @Override
        public void cancel() {}
    }
}
```

### Optional: Async Compute Operations

### Exercise 7: Asynchronous and Parallel Execution

Create `Lab09AsyncCompute.java`:

```java
package com.example.ignite;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCompute;
import org.apache.ignite.Ignition;
import org.apache.ignite.lang.IgniteCallable;
import org.apache.ignite.lang.IgniteFuture;
import org.apache.ignite.lang.IgniteInClosure;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Lab09AsyncCompute {

    public static void main(String[] args) {
        try (Ignite ignite = Ignition.start()) {
            System.out.println("=== Async Compute Operations Lab ===\n");

            IgniteCompute compute = ignite.compute();

            // ============================================
            // Example 1: Basic Async Execution
            // ============================================

            System.out.println("=== Example 1: Basic Async Execution ===");

            // Submit async task
            IgniteFuture<String> future = compute.callAsync(() -> {
                System.out.println("Async task starting...");
                Thread.sleep(1000);
                return "Async result from node: " +
                    Ignition.ignite().cluster().localNode().consistentId();
            });

            System.out.println("Task submitted, doing other work...");

            // Do other work while task executes
            for (int i = 1; i <= 3; i++) {
                System.out.println("Main thread working... " + i);
                Thread.sleep(300);
            }

            // Get result (blocks if not complete)
            String result = future.get();
            System.out.println("Async result: " + result);

            // ============================================
            // Example 2: Future with Timeout
            // ============================================

            System.out.println("\n=== Example 2: Future with Timeout ===");

            IgniteFuture<Integer> timedFuture = compute.callAsync(() -> {
                Thread.sleep(500);
                return 42;
            });

            try {
                Integer timedResult = timedFuture.get(2, TimeUnit.SECONDS);
                System.out.println("Got result within timeout: " + timedResult);
            } catch (Exception e) {
                System.out.println("Timeout waiting for result");
            }

            // ============================================
            // Example 3: Async with Callback (Listen)
            // ============================================

            System.out.println("\n=== Example 3: Async with Callback ===");

            CountDownLatch latch = new CountDownLatch(1);
            AtomicInteger callbackResult = new AtomicInteger();

            IgniteFuture<Integer> callbackFuture = compute.callAsync(() -> {
                Thread.sleep(500);
                return 100;
            });

            // Add listener for async notification
            callbackFuture.listen(new IgniteInClosure<IgniteFuture<Integer>>() {
                @Override
                public void apply(IgniteFuture<Integer> f) {
                    try {
                        int value = f.get();
                        System.out.println("Callback received result: " + value);
                        callbackResult.set(value);
                    } catch (Exception e) {
                        System.out.println("Callback error: " + e.getMessage());
                    } finally {
                        latch.countDown();
                    }
                }
            });

            System.out.println("Waiting for callback...");
            latch.await(5, TimeUnit.SECONDS);
            System.out.println("Callback completed with value: " + callbackResult.get());

            // ============================================
            // Example 4: Parallel Job Execution
            // ============================================

            System.out.println("\n=== Example 4: Parallel Job Execution ===");

            // Create multiple compute tasks
            int numTasks = 10;
            List<IgniteFuture<Integer>> futures = new ArrayList<>();

            long startTime = System.currentTimeMillis();

            // Submit all tasks asynchronously
            for (int i = 0; i < numTasks; i++) {
                final int taskId = i;
                IgniteFuture<Integer> f = compute.callAsync(() -> {
                    // Simulate work
                    Thread.sleep(500);
                    System.out.println("Task " + taskId + " completed on node: " +
                        Ignition.ignite().cluster().localNode().consistentId());
                    return taskId * taskId;
                });
                futures.add(f);
            }

            System.out.println("Submitted " + numTasks + " parallel tasks");

            // Collect all results
            List<Integer> results = new ArrayList<>();
            for (IgniteFuture<Integer> f : futures) {
                results.add(f.get());
            }

            long parallelTime = System.currentTimeMillis() - startTime;

            System.out.println("All tasks completed in " + parallelTime + " ms");
            System.out.println("Results: " + results);
            System.out.println("Sequential would take: ~" + (numTasks * 500) + " ms");

            // ============================================
            // Example 5: Async Broadcast
            // ============================================

            System.out.println("\n=== Example 5: Async Broadcast ===");

            IgniteFuture<Void> broadcastFuture = compute.broadcastAsync(() -> {
                System.out.println("Broadcast received on node: " +
                    Ignition.ignite().cluster().localNode().consistentId());
            });

            System.out.println("Broadcast sent asynchronously");
            broadcastFuture.get();  // Wait for all nodes to receive
            System.out.println("All nodes processed broadcast");

            Thread.sleep(300);

            // ============================================
            // Example 6: Chaining Async Operations
            // ============================================

            System.out.println("\n=== Example 6: Chaining Async Operations ===");

            // First async operation
            IgniteFuture<Integer> step1 = compute.callAsync(() -> {
                System.out.println("Step 1: Fetching data...");
                Thread.sleep(300);
                return 10;
            });

            // Chain with callback
            CountDownLatch chainLatch = new CountDownLatch(1);
            final AtomicInteger finalResult = new AtomicInteger();

            step1.listen(f1 -> {
                try {
                    int value1 = f1.get();
                    System.out.println("Step 1 result: " + value1);

                    // Start step 2 based on step 1 result
                    IgniteFuture<Integer> step2 = compute.callAsync(() -> {
                        System.out.println("Step 2: Processing...");
                        Thread.sleep(300);
                        return value1 * 2;
                    });

                    step2.listen(f2 -> {
                        try {
                            int value2 = f2.get();
                            System.out.println("Step 2 result: " + value2);

                            // Final step
                            IgniteFuture<Integer> step3 = compute.callAsync(() -> {
                                System.out.println("Step 3: Finalizing...");
                                return value2 + 5;
                            });

                            finalResult.set(step3.get());
                            System.out.println("Final result: " + finalResult.get());
                        } catch (Exception e) {
                            e.printStackTrace();
                        } finally {
                            chainLatch.countDown();
                        }
                    });

                } catch (Exception e) {
                    e.printStackTrace();
                    chainLatch.countDown();
                }
            });

            chainLatch.await(10, TimeUnit.SECONDS);

            // ============================================
            // Example 7: Bulk Async with Collection
            // ============================================

            System.out.println("\n=== Example 7: Bulk Async Callables ===");

            Collection<IgniteCallable<Long>> callables = IntStream.range(0, 5)
                .mapToObj(i -> (IgniteCallable<Long>) () -> {
                    // Compute factorial
                    long factorial = 1;
                    for (int n = 1; n <= (i + 5); n++) {
                        factorial *= n;
                    }
                    return factorial;
                })
                .collect(Collectors.toList());

            // Execute all callables and get future for collection of results
            IgniteFuture<Collection<Long>> bulkFuture = compute.callAsync(callables);

            System.out.println("Bulk async submitted");
            Collection<Long> bulkResults = bulkFuture.get();
            System.out.println("Factorial results: " + bulkResults);

            // ============================================
            // Async Best Practices
            // ============================================

            System.out.println("\n=== Async Compute Best Practices ===");
            System.out.println("1. Use async for I/O-bound or long-running operations");
            System.out.println("2. Add listeners instead of blocking on get()");
            System.out.println("3. Set appropriate timeouts on future.get()");
            System.out.println("4. Handle exceptions in callbacks");
            System.out.println("5. Use parallel execution for independent tasks");
            System.out.println("6. Consider backpressure for large task volumes");
            System.out.println("7. Monitor pending futures count");

            System.out.println("\nPress Enter to exit...");
            System.in.read();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

### Optional: Challenge Exercises

### Challenge 1: Distributed Word Counter

Build a complete distributed word counting system:

```java
package com.example.ignite;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.cache.query.ScanQuery;
import org.apache.ignite.cluster.ClusterNode;
import org.apache.ignite.compute.*;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.resources.IgniteInstanceResource;

import javax.cache.Cache;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

public class Lab09Challenge1WordCounter {

    public static void main(String[] args) {
        try (Ignite ignite = Ignition.start()) {
            System.out.println("=== Challenge 1: Distributed Word Counter ===\n");

            // Create document cache
            CacheConfiguration<String, String> docCfg =
                new CacheConfiguration<>("documentCache");
            IgniteCache<String, String> docCache = ignite.getOrCreateCache(docCfg);

            // Load sample documents
            loadSampleDocuments(docCache);

            // Execute distributed word count
            System.out.println("Executing distributed word count...\n");

            WordCountResult result = ignite.compute().execute(
                new DistributedWordCountTask(),
                new WordCountConfig("documentCache", 10, true)
            );

            // Display results
            System.out.println("=== Word Count Results ===");
            System.out.println("Total documents: " + result.documentCount);
            System.out.println("Total words: " + result.totalWords);
            System.out.println("Unique words: " + result.uniqueWords);
            System.out.println("\nTop 10 words:");
            result.topWords.forEach((word, count) ->
                System.out.printf("  %-20s : %d%n", word, count));

            System.out.println("\nWord frequency distribution:");
            System.out.printf("  1 occurrence:    %d words%n",
                result.frequencyDistribution.getOrDefault(1, 0));
            System.out.printf("  2-5 occurrences: %d words%n",
                result.frequencyDistribution.getOrDefault(2, 0));
            System.out.printf("  5+ occurrences:  %d words%n",
                result.frequencyDistribution.getOrDefault(5, 0));

            System.out.println("\nPress Enter to exit...");
            System.in.read();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static void loadSampleDocuments(IgniteCache<String, String> cache) {
        cache.put("doc1", "Apache Ignite is a distributed database for high-performance computing");
        cache.put("doc2", "Ignite provides in-memory data grid capabilities with SQL support");
        cache.put("doc3", "The compute grid in Ignite enables distributed task execution");
        cache.put("doc4", "High performance computing requires distributed processing");
        cache.put("doc5", "Apache Ignite supports ACID transactions and SQL queries");
        cache.put("doc6", "In-memory computing provides low latency data access");
        cache.put("doc7", "Distributed systems scale horizontally for performance");
        cache.put("doc8", "Ignite integrates with Apache Spark for big data processing");
        cache.put("doc9", "The data grid caches frequently accessed data in memory");
        cache.put("doc10", "Computing at scale requires efficient resource management");

        System.out.println("Loaded 10 sample documents\n");
    }

    // Configuration for word count
    static class WordCountConfig implements Serializable {
        String cacheName;
        int topN;
        boolean excludeStopWords;

        WordCountConfig(String cacheName, int topN, boolean excludeStopWords) {
            this.cacheName = cacheName;
            this.topN = topN;
            this.excludeStopWords = excludeStopWords;
        }
    }

    // Result object
    static class WordCountResult implements Serializable {
        int documentCount;
        int totalWords;
        int uniqueWords;
        Map<String, Integer> topWords;
        Map<Integer, Integer> frequencyDistribution;
    }

    // Partial result from each job
    static class PartialWordCount implements Serializable {
        int documentCount;
        Map<String, Integer> wordCounts = new HashMap<>();
    }

    // Main MapReduce task
    static class DistributedWordCountTask
            extends ComputeTaskAdapter<WordCountConfig, WordCountResult> {

        @IgniteInstanceResource
        private Ignite ignite;

        private WordCountConfig config;

        @Override
        public Map<ClusterNode, List<ComputeJob>> map(
                List<ClusterNode> nodes, WordCountConfig config) {

            this.config = config;
            Map<ClusterNode, List<ComputeJob>> jobMap = new HashMap<>();

            IgniteCache<String, String> cache = ignite.cache(config.cacheName);

            // Assign documents to nodes based on affinity
            for (Cache.Entry<String, String> entry : cache.query(new ScanQuery<String, String>())) {
                ClusterNode node = ignite.affinity(config.cacheName)
                    .mapKeyToNode(entry.getKey());

                jobMap.computeIfAbsent(node, k -> new ArrayList<>())
                    .add(new WordCountJob(entry.getValue(), config.excludeStopWords));
            }

            return jobMap;
        }

        @Override
        public WordCountResult reduce(List<ComputeJobResult> results) {
            // Merge all partial results
            Map<String, Integer> totalCounts = new HashMap<>();
            int totalDocs = 0;

            for (ComputeJobResult result : results) {
                if (result.getException() == null) {
                    PartialWordCount partial = result.getData();
                    totalDocs += partial.documentCount;

                    for (Map.Entry<String, Integer> entry : partial.wordCounts.entrySet()) {
                        totalCounts.merge(entry.getKey(), entry.getValue(), Integer::sum);
                    }
                }
            }

            // Build final result
            WordCountResult finalResult = new WordCountResult();
            finalResult.documentCount = totalDocs;
            finalResult.uniqueWords = totalCounts.size();
            finalResult.totalWords = totalCounts.values().stream()
                .mapToInt(Integer::intValue).sum();

            // Get top N words
            finalResult.topWords = totalCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(config.topN)
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    Map.Entry::getValue,
                    (e1, e2) -> e1,
                    LinkedHashMap::new
                ));

            // Calculate frequency distribution
            finalResult.frequencyDistribution = new HashMap<>();
            for (int count : totalCounts.values()) {
                int bucket = count >= 5 ? 5 : (count >= 2 ? 2 : 1);
                finalResult.frequencyDistribution.merge(bucket, 1, Integer::sum);
            }

            return finalResult;
        }
    }

    // Word counting job
    static class WordCountJob implements ComputeJob, Serializable {
        private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
            "a", "an", "the", "is", "are", "was", "were", "be", "been",
            "for", "and", "or", "but", "in", "on", "at", "to", "with"
        ));

        private final String text;
        private final boolean excludeStopWords;

        WordCountJob(String text, boolean excludeStopWords) {
            this.text = text;
            this.excludeStopWords = excludeStopWords;
        }

        @Override
        public PartialWordCount execute() {
            PartialWordCount result = new PartialWordCount();
            result.documentCount = 1;

            if (text != null && !text.isEmpty()) {
                String[] words = text.toLowerCase()
                    .replaceAll("[^a-z0-9\\s]", "")
                    .split("\\s+");

                for (String word : words) {
                    if (!word.isEmpty()) {
                        if (!excludeStopWords || !STOP_WORDS.contains(word)) {
                            result.wordCounts.merge(word, 1, Integer::sum);
                        }
                    }
                }
            }

            return result;
        }

        @Override
        public void cancel() {}
    }
}
```

### Challenge 2: Parallel Data Processor

Implement a parallel data processing pipeline:

```java
package com.example.ignite;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.IgniteCompute;
import org.apache.ignite.Ignition;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.lang.IgniteCallable;
import org.apache.ignite.lang.IgniteFuture;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class Lab09Challenge2ParallelProcessor {

    public static void main(String[] args) {
        try (Ignite ignite = Ignition.start()) {
            System.out.println("=== Challenge 2: Parallel Data Processor ===\n");

            // Create data cache
            CacheConfiguration<Integer, SensorReading> cfg =
                new CacheConfiguration<>("sensorCache");
            IgniteCache<Integer, SensorReading> cache = ignite.getOrCreateCache(cfg);

            // Generate sample sensor data
            System.out.println("Generating sensor data...");
            Random random = new Random(42);
            for (int i = 0; i < 10000; i++) {
                cache.put(i, new SensorReading(
                    i,
                    "sensor-" + (i % 100),
                    random.nextDouble() * 100,
                    System.currentTimeMillis() - random.nextInt(86400000)
                ));
            }
            System.out.println("Generated 10,000 sensor readings\n");

            // Create parallel processor
            ParallelDataProcessor processor = new ParallelDataProcessor(ignite);

            // Process with pipeline stages
            long startTime = System.currentTimeMillis();

            ProcessingResult result = processor.process(
                "sensorCache",
                Arrays.asList(
                    new FilterStage(reading -> reading.value > 50),  // Filter high values
                    new TransformStage(reading -> {
                        reading.value = reading.value * 1.1;  // Apply 10% adjustment
                        return reading;
                    }),
                    new AggregateStage()  // Aggregate by sensor
                )
            );

            long processingTime = System.currentTimeMillis() - startTime;

            // Display results
            System.out.println("=== Processing Results ===");
            System.out.println("Processing time: " + processingTime + " ms");
            System.out.println("Records processed: " + result.recordsProcessed);
            System.out.println("Records after filter: " + result.recordsAfterFilter);
            System.out.println("\nTop 10 sensors by average value:");

            result.sensorAverages.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(10)
                .forEach(e -> System.out.printf("  %-12s : %.2f%n", e.getKey(), e.getValue()));

            System.out.println("\nPress Enter to exit...");
            System.in.read();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static class SensorReading implements Serializable {
        int id;
        String sensorId;
        double value;
        long timestamp;

        SensorReading(int id, String sensorId, double value, long timestamp) {
            this.id = id;
            this.sensorId = sensorId;
            this.value = value;
            this.timestamp = timestamp;
        }
    }

    static class ProcessingResult implements Serializable {
        int recordsProcessed;
        int recordsAfterFilter;
        Map<String, Double> sensorAverages = new HashMap<>();
    }

    interface ProcessingStage extends Serializable {
        String getName();
    }

    static class FilterStage implements ProcessingStage {
        private final java.util.function.Predicate<SensorReading> predicate;

        FilterStage(java.util.function.Predicate<SensorReading> predicate) {
            this.predicate = (java.util.function.Predicate<SensorReading> & Serializable) predicate::test;
        }

        @Override
        public String getName() { return "Filter"; }

        boolean test(SensorReading reading) {
            return predicate.test(reading);
        }
    }

    static class TransformStage implements ProcessingStage {
        private final java.util.function.Function<SensorReading, SensorReading> transformer;

        TransformStage(java.util.function.Function<SensorReading, SensorReading> transformer) {
            this.transformer = (java.util.function.Function<SensorReading, SensorReading> & Serializable) transformer::apply;
        }

        @Override
        public String getName() { return "Transform"; }

        SensorReading apply(SensorReading reading) {
            return transformer.apply(reading);
        }
    }

    static class AggregateStage implements ProcessingStage {
        @Override
        public String getName() { return "Aggregate"; }
    }

    static class ParallelDataProcessor {
        private final Ignite ignite;
        private final IgniteCompute compute;

        ParallelDataProcessor(Ignite ignite) {
            this.ignite = ignite;
            this.compute = ignite.compute();
        }

        ProcessingResult process(String cacheName, List<ProcessingStage> stages) {
            IgniteCache<Integer, SensorReading> cache = ignite.cache(cacheName);

            // Partition data for parallel processing
            int partitions = 10;
            int totalSize = cache.size();
            int partitionSize = totalSize / partitions;

            List<IgniteFuture<PartialResult>> futures = new ArrayList<>();

            // Submit parallel jobs
            for (int p = 0; p < partitions; p++) {
                final int startKey = p * partitionSize;
                final int endKey = (p == partitions - 1) ? totalSize : (p + 1) * partitionSize;

                IgniteFuture<PartialResult> future = compute.callAsync(
                    new ProcessingJob(cacheName, startKey, endKey, stages)
                );
                futures.add(future);
            }

            // Collect and merge results
            ProcessingResult finalResult = new ProcessingResult();
            Map<String, List<Double>> sensorValues = new ConcurrentHashMap<>();

            for (IgniteFuture<PartialResult> future : futures) {
                PartialResult partial = future.get();
                finalResult.recordsProcessed += partial.recordsProcessed;
                finalResult.recordsAfterFilter += partial.recordsAfterFilter;

                for (Map.Entry<String, List<Double>> entry : partial.sensorValues.entrySet()) {
                    sensorValues.computeIfAbsent(entry.getKey(), k ->
                        Collections.synchronizedList(new ArrayList<>()))
                        .addAll(entry.getValue());
                }
            }

            // Calculate averages
            for (Map.Entry<String, List<Double>> entry : sensorValues.entrySet()) {
                double avg = entry.getValue().stream()
                    .mapToDouble(Double::doubleValue)
                    .average()
                    .orElse(0);
                finalResult.sensorAverages.put(entry.getKey(), avg);
            }

            return finalResult;
        }
    }

    static class PartialResult implements Serializable {
        int recordsProcessed;
        int recordsAfterFilter;
        Map<String, List<Double>> sensorValues = new HashMap<>();
    }

    static class ProcessingJob implements IgniteCallable<PartialResult> {
        private final String cacheName;
        private final int startKey;
        private final int endKey;
        private final List<ProcessingStage> stages;

        ProcessingJob(String cacheName, int startKey, int endKey,
                     List<ProcessingStage> stages) {
            this.cacheName = cacheName;
            this.startKey = startKey;
            this.endKey = endKey;
            this.stages = stages;
        }

        @Override
        public PartialResult call() {
            IgniteCache<Integer, SensorReading> cache =
                Ignition.ignite().cache(cacheName);

            PartialResult result = new PartialResult();
            List<SensorReading> readings = new ArrayList<>();

            // Load data
            for (int i = startKey; i < endKey; i++) {
                SensorReading reading = cache.get(i);
                if (reading != null) {
                    readings.add(reading);
                    result.recordsProcessed++;
                }
            }

            // Apply stages
            for (ProcessingStage stage : stages) {
                if (stage instanceof FilterStage) {
                    FilterStage filter = (FilterStage) stage;
                    readings = readings.stream()
                        .filter(filter::test)
                        .collect(Collectors.toList());
                } else if (stage instanceof TransformStage) {
                    TransformStage transform = (TransformStage) stage;
                    readings = readings.stream()
                        .map(transform::apply)
                        .collect(Collectors.toList());
                } else if (stage instanceof AggregateStage) {
                    for (SensorReading reading : readings) {
                        result.sensorValues
                            .computeIfAbsent(reading.sensorId, k -> new ArrayList<>())
                            .add(reading.value);
                    }
                }
            }

            result.recordsAfterFilter = readings.size();
            return result;
        }
    }
}
```

### Challenge 3: Compute-Based Aggregator

Create a flexible aggregation framework:

```java
package com.example.ignite;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.IgniteCompute;
import org.apache.ignite.Ignition;
import org.apache.ignite.cache.query.ScanQuery;
import org.apache.ignite.cluster.ClusterNode;
import org.apache.ignite.compute.*;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.resources.IgniteInstanceResource;

import javax.cache.Cache;
import java.io.Serializable;
import java.util.*;

public class Lab09Challenge3ComputeAggregator {

    public static void main(String[] args) {
        try (Ignite ignite = Ignition.start()) {
            System.out.println("=== Challenge 3: Compute-Based Aggregator ===\n");

            // Create sales cache
            CacheConfiguration<Integer, SalesRecord> cfg =
                new CacheConfiguration<>("salesRecords");
            IgniteCache<Integer, SalesRecord> cache = ignite.getOrCreateCache(cfg);

            // Generate sample data
            System.out.println("Loading sales data...");
            Random random = new Random(42);
            String[] products = {"Laptop", "Phone", "Tablet", "Watch", "Headphones"};
            String[] regions = {"North", "South", "East", "West"};

            for (int i = 0; i < 50000; i++) {
                cache.put(i, new SalesRecord(
                    i,
                    products[random.nextInt(products.length)],
                    regions[random.nextInt(regions.length)],
                    100 + random.nextDouble() * 900,
                    1 + random.nextInt(10)
                ));
            }
            System.out.println("Loaded 50,000 sales records\n");

            // Create aggregator
            ComputeAggregator aggregator = new ComputeAggregator(ignite);

            // Run various aggregations
            System.out.println("=== Aggregation Results ===\n");

            // 1. Total revenue
            System.out.println("1. Total Revenue:");
            AggregationResult totalRevenue = aggregator.aggregate(
                "salesRecords",
                new SumAggregation("revenue")
            );
            System.out.printf("   $%.2f%n", totalRevenue.value);

            // 2. Average order value
            System.out.println("\n2. Average Order Value:");
            AggregationResult avgOrder = aggregator.aggregate(
                "salesRecords",
                new AverageAggregation("revenue")
            );
            System.out.printf("   $%.2f%n", avgOrder.value);

            // 3. Revenue by product
            System.out.println("\n3. Revenue by Product:");
            Map<String, Double> productRevenue = aggregator.groupByAggregate(
                "salesRecords",
                "product",
                new SumAggregation("revenue")
            );
            productRevenue.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .forEach(e -> System.out.printf("   %-12s : $%.2f%n", e.getKey(), e.getValue()));

            // 4. Units sold by region
            System.out.println("\n4. Units Sold by Region:");
            Map<String, Double> regionUnits = aggregator.groupByAggregate(
                "salesRecords",
                "region",
                new SumAggregation("quantity")
            );
            regionUnits.forEach((k, v) ->
                System.out.printf("   %-8s : %.0f units%n", k, v));

            // 5. Min/Max prices
            System.out.println("\n5. Price Range:");
            AggregationResult minPrice = aggregator.aggregate(
                "salesRecords",
                new MinAggregation("revenue")
            );
            AggregationResult maxPrice = aggregator.aggregate(
                "salesRecords",
                new MaxAggregation("revenue")
            );
            System.out.printf("   Min: $%.2f%n", minPrice.value);
            System.out.printf("   Max: $%.2f%n", maxPrice.value);

            // 6. Count by product
            System.out.println("\n6. Order Count by Product:");
            Map<String, Double> productCount = aggregator.groupByAggregate(
                "salesRecords",
                "product",
                new CountAggregation()
            );
            productCount.forEach((k, v) ->
                System.out.printf("   %-12s : %.0f orders%n", k, v));

            System.out.println("\nPress Enter to exit...");
            System.in.read();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static class SalesRecord implements Serializable {
        int id;
        String product;
        String region;
        double revenue;
        int quantity;

        SalesRecord(int id, String product, String region, double revenue, int quantity) {
            this.id = id;
            this.product = product;
            this.region = region;
            this.revenue = revenue;
            this.quantity = quantity;
        }

        double getField(String fieldName) {
            switch (fieldName) {
                case "revenue": return revenue;
                case "quantity": return quantity;
                case "id": return id;
                default: return 0;
            }
        }

        String getGroupField(String fieldName) {
            switch (fieldName) {
                case "product": return product;
                case "region": return region;
                default: return "unknown";
            }
        }
    }

    static class AggregationResult implements Serializable {
        double value;
        int count;

        AggregationResult() {}

        AggregationResult(double value, int count) {
            this.value = value;
            this.count = count;
        }

        void merge(AggregationResult other, String type) {
            switch (type) {
                case "SUM":
                case "COUNT":
                    this.value += other.value;
                    this.count += other.count;
                    break;
                case "AVG":
                    double total1 = this.value * this.count;
                    double total2 = other.value * other.count;
                    this.count += other.count;
                    this.value = this.count > 0 ? (total1 + total2) / this.count : 0;
                    break;
                case "MIN":
                    this.value = Math.min(this.value, other.value);
                    break;
                case "MAX":
                    this.value = Math.max(this.value, other.value);
                    break;
            }
        }
    }

    interface Aggregation extends Serializable {
        String getType();
        String getField();
        AggregationResult initial();
        void accumulate(AggregationResult result, SalesRecord record);
        AggregationResult finalize(AggregationResult result);
    }

    static class SumAggregation implements Aggregation {
        private final String field;

        SumAggregation(String field) {
            this.field = field;
        }

        @Override public String getType() { return "SUM"; }
        @Override public String getField() { return field; }
        @Override public AggregationResult initial() { return new AggregationResult(0, 0); }

        @Override
        public void accumulate(AggregationResult result, SalesRecord record) {
            result.value += record.getField(field);
            result.count++;
        }

        @Override
        public AggregationResult finalize(AggregationResult result) { return result; }
    }

    static class AverageAggregation implements Aggregation {
        private final String field;

        AverageAggregation(String field) {
            this.field = field;
        }

        @Override public String getType() { return "AVG"; }
        @Override public String getField() { return field; }
        @Override public AggregationResult initial() { return new AggregationResult(0, 0); }

        @Override
        public void accumulate(AggregationResult result, SalesRecord record) {
            result.value += record.getField(field);
            result.count++;
        }

        @Override
        public AggregationResult finalize(AggregationResult result) {
            if (result.count > 0) {
                result.value = result.value / result.count;
            }
            return result;
        }
    }

    static class MinAggregation implements Aggregation {
        private final String field;

        MinAggregation(String field) {
            this.field = field;
        }

        @Override public String getType() { return "MIN"; }
        @Override public String getField() { return field; }
        @Override public AggregationResult initial() {
            return new AggregationResult(Double.MAX_VALUE, 0);
        }

        @Override
        public void accumulate(AggregationResult result, SalesRecord record) {
            result.value = Math.min(result.value, record.getField(field));
            result.count++;
        }

        @Override
        public AggregationResult finalize(AggregationResult result) { return result; }
    }

    static class MaxAggregation implements Aggregation {
        private final String field;

        MaxAggregation(String field) {
            this.field = field;
        }

        @Override public String getType() { return "MAX"; }
        @Override public String getField() { return field; }
        @Override public AggregationResult initial() {
            return new AggregationResult(Double.MIN_VALUE, 0);
        }

        @Override
        public void accumulate(AggregationResult result, SalesRecord record) {
            result.value = Math.max(result.value, record.getField(field));
            result.count++;
        }

        @Override
        public AggregationResult finalize(AggregationResult result) { return result; }
    }

    static class CountAggregation implements Aggregation {
        @Override public String getType() { return "COUNT"; }
        @Override public String getField() { return ""; }
        @Override public AggregationResult initial() { return new AggregationResult(0, 0); }

        @Override
        public void accumulate(AggregationResult result, SalesRecord record) {
            result.value++;
            result.count++;
        }

        @Override
        public AggregationResult finalize(AggregationResult result) { return result; }
    }

    static class ComputeAggregator {
        private final Ignite ignite;

        ComputeAggregator(Ignite ignite) {
            this.ignite = ignite;
        }

        AggregationResult aggregate(String cacheName, Aggregation aggregation) {
            return ignite.compute().execute(
                new AggregationTask(aggregation, null),
                cacheName
            );
        }

        Map<String, Double> groupByAggregate(String cacheName, String groupField,
                                              Aggregation aggregation) {
            return ignite.compute().execute(
                new GroupByAggregationTask(aggregation, groupField),
                cacheName
            );
        }
    }

    static class AggregationTask extends ComputeTaskAdapter<String, AggregationResult> {
        @IgniteInstanceResource
        private Ignite ignite;

        private final Aggregation aggregation;
        private final String groupField;

        AggregationTask(Aggregation aggregation, String groupField) {
            this.aggregation = aggregation;
            this.groupField = groupField;
        }

        @Override
        public Map<ClusterNode, List<ComputeJob>> map(List<ClusterNode> nodes, String cacheName) {
            Map<ClusterNode, List<ComputeJob>> jobMap = new HashMap<>();

            IgniteCache<Integer, SalesRecord> cache = ignite.cache(cacheName);

            for (Cache.Entry<Integer, SalesRecord> entry : cache.query(new ScanQuery<Integer, SalesRecord>())) {
                ClusterNode node = ignite.affinity(cacheName).mapKeyToNode(entry.getKey());
                jobMap.computeIfAbsent(node, k -> new ArrayList<>())
                    .add(new AggregationJob(entry.getValue(), aggregation));
            }

            return jobMap;
        }

        @Override
        public AggregationResult reduce(List<ComputeJobResult> results) {
            AggregationResult total = aggregation.initial();

            for (ComputeJobResult result : results) {
                if (result.getException() == null) {
                    AggregationResult partial = result.getData();
                    total.merge(partial, aggregation.getType());
                }
            }

            return aggregation.finalize(total);
        }
    }

    static class AggregationJob implements ComputeJob, Serializable {
        private final SalesRecord record;
        private final Aggregation aggregation;

        AggregationJob(SalesRecord record, Aggregation aggregation) {
            this.record = record;
            this.aggregation = aggregation;
        }

        @Override
        public AggregationResult execute() {
            AggregationResult result = aggregation.initial();
            aggregation.accumulate(result, record);
            return result;
        }

        @Override
        public void cancel() {}
    }

    static class GroupByAggregationTask
            extends ComputeTaskAdapter<String, Map<String, Double>> {

        @IgniteInstanceResource
        private Ignite ignite;

        private final Aggregation aggregation;
        private final String groupField;

        GroupByAggregationTask(Aggregation aggregation, String groupField) {
            this.aggregation = aggregation;
            this.groupField = groupField;
        }

        @Override
        public Map<ClusterNode, List<ComputeJob>> map(List<ClusterNode> nodes, String cacheName) {
            Map<ClusterNode, List<ComputeJob>> jobMap = new HashMap<>();

            // Create one job per node with all its local records
            Map<ClusterNode, List<SalesRecord>> nodeRecords = new HashMap<>();

            IgniteCache<Integer, SalesRecord> cache = ignite.cache(cacheName);

            for (Cache.Entry<Integer, SalesRecord> entry : cache.query(new ScanQuery<Integer, SalesRecord>())) {
                ClusterNode node = ignite.affinity(cacheName).mapKeyToNode(entry.getKey());
                nodeRecords.computeIfAbsent(node, k -> new ArrayList<>())
                    .add(entry.getValue());
            }

            for (Map.Entry<ClusterNode, List<SalesRecord>> entry : nodeRecords.entrySet()) {
                List<ComputeJob> jobs = new ArrayList<>();
                jobs.add(new GroupByJob(entry.getValue(), aggregation, groupField));
                jobMap.put(entry.getKey(), jobs);
            }

            return jobMap;
        }

        @Override
        public Map<String, Double> reduce(List<ComputeJobResult> results) {
            Map<String, AggregationResult> grouped = new HashMap<>();

            for (ComputeJobResult result : results) {
                if (result.getException() == null) {
                    Map<String, AggregationResult> partial = result.getData();

                    for (Map.Entry<String, AggregationResult> entry : partial.entrySet()) {
                        grouped.computeIfAbsent(entry.getKey(), k -> aggregation.initial())
                            .merge(entry.getValue(), aggregation.getType());
                    }
                }
            }

            // Finalize and convert to simple map
            Map<String, Double> finalResult = new HashMap<>();
            for (Map.Entry<String, AggregationResult> entry : grouped.entrySet()) {
                AggregationResult finalized = aggregation.finalize(entry.getValue());
                finalResult.put(entry.getKey(), finalized.value);
            }

            return finalResult;
        }
    }

    static class GroupByJob implements ComputeJob, Serializable {
        private final List<SalesRecord> records;
        private final Aggregation aggregation;
        private final String groupField;

        GroupByJob(List<SalesRecord> records, Aggregation aggregation, String groupField) {
            this.records = records;
            this.aggregation = aggregation;
            this.groupField = groupField;
        }

        @Override
        public Map<String, AggregationResult> execute() {
            Map<String, AggregationResult> grouped = new HashMap<>();

            for (SalesRecord record : records) {
                String groupKey = record.getGroupField(groupField);
                AggregationResult result = grouped.computeIfAbsent(
                    groupKey, k -> aggregation.initial());
                aggregation.accumulate(result, record);
            }

            return grouped;
        }

        @Override
        public void cancel() {}
    }
}
```

