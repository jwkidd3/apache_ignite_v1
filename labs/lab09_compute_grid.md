# Lab 9: Compute Grid Fundamentals

## Duration: 45 minutes

## Objectives
- Understand distributed computing concepts in Ignite
- Implement compute closures and jobs
- Use task execution and load balancing
- Implement affinity-aware computing for performance
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
            System.out.println("✓ Automatic load balancing");
            System.out.println("✓ Failover support");
            System.out.println("✓ Task distribution across nodes");
            System.out.println("✓ Parallel execution");

            System.out.println("\nPress Enter to exit...");
            System.in.read();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

## Part 2: Affinity-Aware Computing (15 minutes)

### Exercise 2: Colocation of Compute and Data

Create `Lab09AffinityCompute.java`:

```java
package com.example.ignite;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.IgniteCompute;
import org.apache.ignite.Ignition;
import org.apache.ignite.cluster.ClusterNode;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.lang.IgniteCallable;
import org.apache.ignite.resources.IgniteInstanceResource;

public class Lab09AffinityCompute {

    public static void main(String[] args) {
        try (Ignite ignite = Ignition.start()) {
            System.out.println("=== Affinity-Aware Computing Lab ===\n");

            // Create cache with data
            CacheConfiguration<Integer, String> cfg =
                new CacheConfiguration<>("dataCache");
            cfg.setBackups(1);

            IgniteCache<Integer, String> cache = ignite.getOrCreateCache(cfg);

            // Populate cache
            for (int i = 0; i < 100; i++) {
                cache.put(i, "Value-" + i);
            }

            System.out.println("Cache populated with 100 entries\n");

            // Non-affinity computation (may require network transfer)
            System.out.println("=== Non-Affinity Computation ===");
            long start = System.currentTimeMillis();

            for (int i = 0; i < 10; i++) {
                final int key = i;
                ignite.compute().call(() -> {
                    IgniteCache<Integer, String> localCache =
                        Ignition.ignite().cache("dataCache");
                    String value = localCache.get(key);
                    return value.length();
                });
            }

            long nonAffinityTime = System.currentTimeMillis() - start;
            System.out.println("Time: " + nonAffinityTime + " ms\n");

            // Affinity-aware computation (colocated with data)
            System.out.println("=== Affinity-Aware Computation ===");
            start = System.currentTimeMillis();

            for (int i = 0; i < 10; i++) {
                final int key = i;

                // Run computation on the node where data resides
                ignite.compute().affinityCall("dataCache", key, new IgniteCallable<Integer>() {
                    @IgniteInstanceResource
                    private Ignite ignite;

                    @Override
                    public Integer call() throws Exception {
                        IgniteCache<Integer, String> cache = ignite.cache("dataCache");
                        String value = cache.localPeek(key);  // Local peek, no network
                        return value != null ? value.length() : 0;
                    }
                });
            }

            long affinityTime = System.currentTimeMillis() - start;
            System.out.println("Time: " + affinityTime + " ms");
            System.out.println("Speedup: " + (float)nonAffinityTime/affinityTime + "x\n");

            // Demonstrate affinity run for complex processing
            System.out.println("=== Affinity Run Example ===");

            ignite.compute().affinityRun("dataCache", 42, () -> {
                Ignite localIgnite = Ignition.ignite();
                IgniteCache<Integer, String> localCache = localIgnite.cache("dataCache");

                // Process data locally
                String value = localCache.localPeek(42);
                if (value != null) {
                    System.out.println("Processing key 42 locally on node: " +
                        localIgnite.cluster().localNode().consistentId());
                    System.out.println("Value: " + value);

                    // Update locally
                    localCache.put(42, value.toUpperCase());
                    System.out.println("Updated to: " + localCache.get(42));
                }
            });

            Thread.sleep(500);

            System.out.println("\n=== Affinity Computing Benefits ===");
            System.out.println("✓ No network data transfer");
            System.out.println("✓ Better performance");
            System.out.println("✓ Lower latency");
            System.out.println("✓ Reduced network congestion");
            System.out.println("✓ Data locality");

            System.out.println("\nPress Enter to exit...");
            System.in.read();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

## Part 3: MapReduce Operations (15 minutes)

### Exercise 3: Implement MapReduce

Create `Lab09MapReduce.java`:

```java
package com.example.ignite;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.cache.query.ScanQuery;
import org.apache.ignite.compute.ComputeJob;
import org.apache.ignite.compute.ComputeJobResult;
import org.apache.ignite.compute.ComputeTaskAdapter;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.resources.IgniteInstanceResource;

import javax.cache.Cache;
import java.util.*;

public class Lab09MapReduce {

    public static void main(String[] args) {
        try (Ignite ignite = Ignition.start()) {
            System.out.println("=== MapReduce Lab ===\n");

            // Create cache with sample data
            CacheConfiguration<Integer, String> cfg =
                new CacheConfiguration<>("textCache");

            IgniteCache<Integer, String> cache = ignite.getOrCreateCache(cfg);

            // Sample text data
            cache.put(1, "hello world");
            cache.put(2, "hello ignite");
            cache.put(3, "world of computing");
            cache.put(4, "ignite computing platform");
            cache.put(5, "hello computing world");

            System.out.println("Cache populated with text data\n");

            // Example 1: Word Count using MapReduce
            System.out.println("=== Word Count MapReduce ===");

            Map<String, Integer> wordCounts =
                ignite.compute().execute(WordCountTask.class, "textCache");

            System.out.println("\nWord Counts:");
            wordCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .forEach(e -> System.out.println("  " + e.getKey() + ": " + e.getValue()));

            // Example 2: Simple aggregation
            System.out.println("\n=== Sum Values MapReduce ===");

            CacheConfiguration<Integer, Integer> numCfg =
                new CacheConfiguration<>("numberCache");
            IgniteCache<Integer, Integer> numCache = ignite.getOrCreateCache(numCfg);

            for (int i = 1; i <= 100; i++) {
                numCache.put(i, i);
            }

            Integer sum = ignite.compute().execute(SumTask.class, "numberCache");
            System.out.println("Sum of 1-100: " + sum);
            System.out.println("Expected: 5050");

            System.out.println("\n=== MapReduce Pattern ===");
            System.out.println("1. MAP: Distribute work across nodes");
            System.out.println("2. Local processing on each node");
            System.out.println("3. REDUCE: Aggregate results");
            System.out.println("\nBenefits: Parallel processing, scalability");

            System.out.println("\nPress Enter to exit...");
            System.in.read();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Word Count Task
    static class WordCountTask extends ComputeTaskAdapter<String, Map<String, Integer>> {
        @IgniteInstanceResource
        private Ignite ignite;

        @Override
        public Map<String, List<ComputeJob>> map(List<ClusterNode> subgrid, String cacheName) {
            Map<String, List<ComputeJob>> map = new HashMap<>();

            IgniteCache<Integer, String> cache = ignite.cache(cacheName);

            // Create a job for each cache entry
            for (Cache.Entry<Integer, String> entry : cache.query(new ScanQuery<>())) {
                String nodeId = ignite.affinity(cacheName)
                    .mapKeyToNode(entry.getKey()).id().toString();

                map.computeIfAbsent(nodeId, k -> new ArrayList<>())
                    .add(new WordCountJob(entry.getValue()));
            }

            return map;
        }

        @Override
        public Map<String, Integer> reduce(List<ComputeJobResult> results) {
            Map<String, Integer> totalCounts = new HashMap<>();

            for (ComputeJobResult result : results) {
                Map<String, Integer> jobCounts = result.getData();

                for (Map.Entry<String, Integer> entry : jobCounts.entrySet()) {
                    totalCounts.merge(entry.getKey(), entry.getValue(), Integer::sum);
                }
            }

            return totalCounts;
        }
    }

    // Word Count Job (Map phase)
    static class WordCountJob implements ComputeJob {
        private final String text;

        WordCountJob(String text) {
            this.text = text;
        }

        @Override
        public Map<String, Integer> execute() {
            Map<String, Integer> counts = new HashMap<>();

            if (text != null) {
                String[] words = text.toLowerCase().split("\\s+");
                for (String word : words) {
                    counts.merge(word, 1, Integer::sum);
                }
            }

            return counts;
        }

        @Override
        public void cancel() {
            // Cancellation logic
        }
    }

    // Sum Task
    static class SumTask extends ComputeTaskAdapter<String, Integer> {
        @IgniteInstanceResource
        private Ignite ignite;

        @Override
        public Map<String, List<ComputeJob>> map(List<ClusterNode> subgrid, String cacheName) {
            Map<String, List<ComputeJob>> map = new HashMap<>();

            IgniteCache<Integer, Integer> cache = ignite.cache(cacheName);

            for (Cache.Entry<Integer, Integer> entry : cache.query(new ScanQuery<>())) {
                String nodeId = ignite.affinity(cacheName)
                    .mapKeyToNode(entry.getKey()).id().toString();

                map.computeIfAbsent(nodeId, k -> new ArrayList<>())
                    .add(new SumJob(entry.getValue()));
            }

            return map;
        }

        @Override
        public Integer reduce(List<ComputeJobResult> results) {
            int sum = 0;
            for (ComputeJobResult result : results) {
                sum += result.<Integer>getData();
            }
            return sum;
        }
    }

    static class SumJob implements ComputeJob {
        private final int value;

        SumJob(int value) {
            this.value = value;
        }

        @Override
        public Integer execute() {
            return value;
        }

        @Override
        public void cancel() {}
    }
}
```

### Exercise 4: Advanced Compute Patterns

Create `Lab09AdvancedCompute.java`:

```java
package com.example.ignite;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCompute;
import org.apache.ignite.Ignition;
import org.apache.ignite.cluster.ClusterGroup;
import org.apache.ignite.lang.IgniteCallable;

import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Lab09AdvancedCompute {

    public static void main(String[] args) {
        try (Ignite ignite = Ignition.start()) {
            System.out.println("=== Advanced Compute Patterns Lab ===\n");

            IgniteCompute compute = ignite.compute();

            // Pattern 1: Compute on specific nodes
            System.out.println("=== Pattern 1: Targeted Execution ===");

            // Execute on server nodes only
            ClusterGroup serverNodes = ignite.cluster().forServers();
            IgniteCompute serverCompute = ignite.compute(serverNodes);

            serverCompute.broadcast(() ->
                System.out.println("Running on server: " +
                    Ignition.ignite().cluster().localNode().consistentId()));

            Thread.sleep(500);

            // Pattern 2: Timeout handling
            System.out.println("\n=== Pattern 2: Timeout Handling ===");

            try {
                compute.withTimeout(2000).call(() -> {
                    System.out.println("Starting long task...");
                    Thread.sleep(5000);  // Will timeout
                    return "Completed";
                });
            } catch (Exception e) {
                System.out.println("Task timed out as expected");
            }

            // Pattern 3: Async execution
            System.out.println("\n=== Pattern 3: Asynchronous Execution ===");

            org.apache.ignite.lang.IgniteFuture<String> future =
                compute.callAsync(() -> {
                    Thread.sleep(1000);
                    return "Async result";
                });

            System.out.println("Task submitted, doing other work...");
            Thread.sleep(500);
            System.out.println("Still working...");

            String asyncResult = future.get(3, TimeUnit.SECONDS);
            System.out.println("Async result: " + asyncResult);

            // Pattern 4: Parallel processing
            System.out.println("\n=== Pattern 4: Parallel Processing ===");

            long startTime = System.currentTimeMillis();

            Collection<IgniteCallable<Long>> tasks = IntStream.range(0, 10)
                .mapToObj(i -> (IgniteCallable<Long>) () -> {
                    // Simulate heavy computation
                    long sum = 0;
                    for (long j = 0; j < 10_000_000; j++) {
                        sum += j;
                    }
                    return sum;
                })
                .collect(Collectors.toList());

            Collection<Long> results = compute.call(tasks);
            long parallelTime = System.currentTimeMillis() - startTime;

            System.out.println("10 tasks completed in: " + parallelTime + " ms");
            System.out.println("Results count: " + results.size());

            // Pattern 5: Load balancing strategies
            System.out.println("\n=== Pattern 5: Load Balancing ===");
            System.out.println("Ignite automatically balances load:");
            System.out.println("- Round-robin (default)");
            System.out.println("- Random");
            System.out.println("- Weighted");
            System.out.println("- Custom strategies");

            System.out.println("\n=== Compute Best Practices ===");
            System.out.println("✓ Use affinity for data-intensive tasks");
            System.out.println("✓ Keep tasks small and focused");
            System.out.println("✓ Handle failures gracefully");
            System.out.println("✓ Use async for long-running operations");
            System.out.println("✓ Monitor resource usage");
            System.out.println("✓ Set appropriate timeouts");

            System.out.println("\nPress Enter to exit...");
            System.in.read();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

## Verification Steps

### Checklist
- [ ] Basic compute tasks execute successfully
- [ ] Broadcast reaches all nodes
- [ ] Callables return results correctly
- [ ] Affinity-aware compute shows performance improvement
- [ ] MapReduce word count produces correct results
- [ ] Async execution works properly
- [ ] Timeout handling functions correctly

### Performance Test

Compare affinity vs non-affinity compute:
- Run both versions with larger datasets
- Measure execution time
- Observe network traffic

## Lab Questions

1. What is the benefit of affinity-aware computing?
2. When should you use MapReduce in Ignite?
3. What happens if a compute task fails on a node?
4. How does load balancing work in Ignite compute grid?

## Answers

1. **Affinity-aware computing** runs computation where data resides, eliminating network data transfer. This provides better performance, lower latency, and reduced network congestion.

2. Use **MapReduce** for:
   - Large-scale data processing
   - Aggregation operations across distributed data
   - Parallel analysis tasks
   - When data can be processed independently
   - Need to leverage cluster parallelism

3. Ignite provides **automatic failover**. If a node fails:
   - Task is resubmitted to another node
   - Configurable failover policies
   - Can set max failover attempts
   - Results still returned to caller

4. **Load balancing** distributes tasks across nodes:
   - Default: Round-robin across available nodes
   - Considers node capacity and current load
   - Can use custom load balancing SPIs
   - Automatic with compute grid

## Common Issues

**Issue: Compute task not distributing**
- Check cluster has multiple nodes
- Verify cluster group includes target nodes
- Ensure tasks are serializable

**Issue: Affinity compute not improving performance**
- Verify data is actually colocated
- Check using correct cache name
- Ensure affinity key configuration correct

**Issue: MapReduce producing wrong results**
- Verify map phase creates correct jobs
- Check reduce logic
- Ensure all results collected

## Next Steps

In Lab 10, you will:
- Use Ignite REST API
- Integrate with Spring Framework
- Configure Kafka connector
- Set up Hibernate L2 cache
- Work with client vs server nodes

## Completion

You have completed Lab 9 when you can:
- Execute basic compute operations
- Implement affinity-aware computing
- Create MapReduce tasks
- Use async compute operations
- Apply appropriate compute patterns for different scenarios
