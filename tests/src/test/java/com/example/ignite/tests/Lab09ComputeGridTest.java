package com.example.ignite.tests;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.IgniteCompute;
import org.apache.ignite.cluster.ClusterGroup;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.lang.IgniteCallable;
import org.apache.ignite.lang.IgniteFuture;
import org.apache.ignite.lang.IgniteRunnable;
import org.apache.ignite.resources.IgniteInstanceResource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive tests for Lab 9: Compute Grid Fundamentals
 * Coverage: Basic compute operations, broadcast, callable, affinity-aware computing, async operations
 */
@DisplayName("Lab 09: Compute Grid Tests")
public class Lab09ComputeGridTest extends BaseIgniteTest {

    // ==================== Basic Compute Operations ====================

    @Test
    @DisplayName("Test IgniteCompute instance creation")
    public void testComputeInstanceCreation() {
        IgniteCompute compute = ignite.compute();

        assertThat(compute).isNotNull();
    }

    @Test
    @DisplayName("Test simple runnable execution")
    public void testSimpleRunnable() {
        IgniteCompute compute = ignite.compute();
        AtomicInteger counter = new AtomicInteger(0);

        // Run a simple task
        compute.run(() -> {
            counter.incrementAndGet();
        });

        assertThat(counter.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("Test run operation executes closure")
    public void testRunOperation() {
        IgniteCompute compute = ignite.compute();
        final boolean[] executed = {false};

        compute.run((IgniteRunnable) () -> {
            executed[0] = true;
        });

        assertThat(executed[0]).isTrue();
    }

    @Test
    @DisplayName("Test call operation with return value")
    public void testCallOperation() {
        IgniteCompute compute = ignite.compute();

        String result = compute.call(() -> {
            return "Hello from compute";
        });

        assertThat(result).isEqualTo("Hello from compute");
    }

    @Test
    @DisplayName("Test callable returns computed value")
    public void testCallableWithComputation() {
        IgniteCompute compute = ignite.compute();

        Integer result = compute.call(() -> {
            int sum = 0;
            for (int i = 1; i <= 10; i++) {
                sum += i;
            }
            return sum;
        });

        assertThat(result).isEqualTo(55);
    }

    // ==================== Broadcast Operations ====================

    @Test
    @DisplayName("Test broadcast to all nodes")
    public void testBroadcast() {
        IgniteCompute compute = ignite.compute();
        AtomicInteger counter = new AtomicInteger(0);

        // Broadcast to all nodes in cluster
        compute.broadcast(() -> {
            counter.incrementAndGet();
        });

        // Single node cluster should execute once
        assertThat(counter.get()).isGreaterThanOrEqualTo(1);
    }

    @Test
    @DisplayName("Test broadcast returns collection of results")
    public void testBroadcastWithReturnValue() {
        IgniteCompute compute = ignite.compute();

        Collection<String> results = compute.broadcast(() -> {
            return "Node executed";
        });

        assertThat(results).isNotEmpty();
        assertThat(results).allMatch(r -> r.equals("Node executed"));
    }

    @Test
    @DisplayName("Test broadcast reaches all cluster nodes")
    public void testBroadcastReachesAllNodes() {
        IgniteCompute compute = ignite.compute();

        Collection<String> nodeIds = compute.broadcast(() -> {
            return ignite.cluster().localNode().id().toString();
        });

        // Should have at least one node
        assertThat(nodeIds).hasSize(ignite.cluster().nodes().size());
    }

    // ==================== Callable with Return Values ====================

    @Test
    @DisplayName("Test callable with integer return")
    public void testCallableIntegerReturn() {
        IgniteCompute compute = ignite.compute();

        Integer result = compute.call(() -> 42);

        assertThat(result).isEqualTo(42);
    }

    @Test
    @DisplayName("Test callable with complex object return")
    public void testCallableComplexReturn() {
        IgniteCompute compute = ignite.compute();

        ComputeResult result = compute.call(() -> {
            return new ComputeResult("test", 100);
        });

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("test");
        assertThat(result.getValue()).isEqualTo(100);
    }

    @Test
    @DisplayName("Test multiple callables execution")
    public void testMultipleCallables() {
        IgniteCompute compute = ignite.compute();

        Collection<IgniteCallable<Integer>> calls = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            final int num = i;
            calls.add(() -> num * num);
        }

        Collection<Integer> results = compute.call(calls);

        assertThat(results).hasSize(5);
        assertThat(results).containsExactlyInAnyOrder(1, 4, 9, 16, 25);
    }

    @Test
    @DisplayName("Test callable with exception handling")
    public void testCallableWithException() {
        IgniteCompute compute = ignite.compute();

        assertThatThrownBy(() -> {
            compute.call(() -> {
                throw new RuntimeException("Test exception");
            });
        }).isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("Test parallel callable execution")
    public void testParallelCallables() {
        IgniteCompute compute = ignite.compute();

        Collection<IgniteCallable<Long>> tasks = IntStream.range(0, 10)
            .mapToObj(i -> (IgniteCallable<Long>) () -> {
                long sum = 0;
                for (long j = 0; j < 1000; j++) {
                    sum += j;
                }
                return sum;
            })
            .collect(Collectors.toList());

        Collection<Long> results = compute.call(tasks);

        assertThat(results).hasSize(10);
        assertThat(results).allMatch(r -> r == 499500L);
    }

    // ==================== Affinity-Aware Computing ====================

    @Test
    @DisplayName("Test affinity run on data node")
    public void testAffinityRun() {
        String cacheName = getTestCacheName();
        CacheConfiguration<Integer, String> cfg = new CacheConfiguration<>(cacheName);
        IgniteCache<Integer, String> cache = ignite.getOrCreateCache(cfg);

        cache.put(1, "value1");

        final boolean[] executed = {false};

        ignite.compute().affinityRun(cacheName, 1, () -> {
            executed[0] = true;
        });

        assertThat(executed[0]).isTrue();
    }

    @Test
    @DisplayName("Test affinity call returns value")
    public void testAffinityCall() {
        String cacheName = getTestCacheName();
        CacheConfiguration<Integer, String> cfg = new CacheConfiguration<>(cacheName);
        IgniteCache<Integer, String> cache = ignite.getOrCreateCache(cfg);

        cache.put(42, "test-value");

        String result = ignite.compute().affinityCall(cacheName, 42, new IgniteCallable<String>() {
            @IgniteInstanceResource
            private Ignite localIgnite;

            @Override
            public String call() {
                IgniteCache<Integer, String> localCache = localIgnite.cache(cacheName);
                return localCache.get(42);
            }
        });

        assertThat(result).isEqualTo("test-value");
    }

    @Test
    @DisplayName("Test affinity run executes on correct partition")
    public void testAffinityRunOnCorrectPartition() {
        String cacheName = getTestCacheName();
        CacheConfiguration<Integer, String> cfg = new CacheConfiguration<>(cacheName);
        IgniteCache<Integer, String> cache = ignite.getOrCreateCache(cfg);

        int testKey = 100;
        cache.put(testKey, "value100");

        int expectedPartition = ignite.affinity(cacheName).partition(testKey);

        Integer actualPartition = ignite.compute().affinityCall(cacheName, testKey, new IgniteCallable<Integer>() {
            @IgniteInstanceResource
            private Ignite localIgnite;

            @Override
            public Integer call() {
                return localIgnite.affinity(cacheName).partition(testKey);
            }
        });

        assertThat(actualPartition).isEqualTo(expectedPartition);
    }

    @Test
    @DisplayName("Test affinity call with local peek")
    public void testAffinityCallWithLocalPeek() {
        String cacheName = getTestCacheName();
        CacheConfiguration<Integer, String> cfg = new CacheConfiguration<>(cacheName);
        IgniteCache<Integer, String> cache = ignite.getOrCreateCache(cfg);

        cache.put(5, "local-value");

        String result = ignite.compute().affinityCall(cacheName, 5, new IgniteCallable<String>() {
            @IgniteInstanceResource
            private Ignite localIgnite;

            @Override
            public String call() {
                IgniteCache<Integer, String> localCache = localIgnite.cache(cacheName);
                // localPeek should work since we're on the data node
                String value = localCache.localPeek(5);
                return value != null ? value : "not-found";
            }
        });

        assertThat(result).isEqualTo("local-value");
    }

    @Test
    @DisplayName("Test affinity run with multiple keys on same partition")
    public void testAffinityRunMultipleKeys() {
        String cacheName = getTestCacheName();
        CacheConfiguration<Integer, String> cfg = new CacheConfiguration<>(cacheName);
        IgniteCache<Integer, String> cache = ignite.getOrCreateCache(cfg);

        // Put multiple entries
        for (int i = 0; i < 100; i++) {
            cache.put(i, "value-" + i);
        }

        AtomicInteger processedCount = new AtomicInteger(0);

        // Process each key on its affinity node
        for (int i = 0; i < 10; i++) {
            final int key = i;
            ignite.compute().affinityRun(cacheName, key, () -> {
                processedCount.incrementAndGet();
            });
        }

        assertThat(processedCount.get()).isEqualTo(10);
    }

    // ==================== Async Compute Operations ====================

    @Test
    @DisplayName("Test async call operation")
    public void testAsyncCall() throws Exception {
        IgniteCompute compute = ignite.compute();

        IgniteFuture<String> future = compute.callAsync(() -> {
            return "async-result";
        });

        assertThat(future).isNotNull();

        String result = future.get(5, TimeUnit.SECONDS);
        assertThat(result).isEqualTo("async-result");
    }

    @Test
    @DisplayName("Test async run operation")
    public void testAsyncRun() throws Exception {
        IgniteCompute compute = ignite.compute();
        AtomicInteger counter = new AtomicInteger(0);

        IgniteFuture<Void> future = compute.runAsync(() -> {
            counter.incrementAndGet();
        });

        future.get(5, TimeUnit.SECONDS);

        assertThat(counter.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("Test async broadcast operation")
    public void testAsyncBroadcast() throws Exception {
        IgniteCompute compute = ignite.compute();

        IgniteFuture<Collection<String>> future = compute.broadcastAsync(() -> {
            return "broadcast-result";
        });

        Collection<String> results = future.get(5, TimeUnit.SECONDS);

        assertThat(results).isNotEmpty();
        assertThat(results).allMatch(r -> r.equals("broadcast-result"));
    }

    @Test
    @DisplayName("Test async operation allows concurrent work")
    public void testAsyncAllowsConcurrentWork() throws Exception {
        IgniteCompute compute = ignite.compute();

        long startTime = System.currentTimeMillis();

        // Start async operation that takes some time
        IgniteFuture<String> future = compute.callAsync(() -> {
            Thread.sleep(100);
            return "completed";
        });

        // Do some work while async operation is running
        int workDone = 0;
        while (!future.isDone() && workDone < 1000) {
            workDone++;
        }

        String result = future.get(5, TimeUnit.SECONDS);
        long endTime = System.currentTimeMillis();

        assertThat(result).isEqualTo("completed");
        assertThat(workDone).isGreaterThan(0);
    }

    @Test
    @DisplayName("Test async future isDone and get")
    public void testAsyncFutureMethods() throws Exception {
        IgniteCompute compute = ignite.compute();

        IgniteFuture<Integer> future = compute.callAsync(() -> {
            return 42;
        });

        // Wait for completion
        Integer result = future.get(5, TimeUnit.SECONDS);

        assertThat(future.isDone()).isTrue();
        assertThat(result).isEqualTo(42);
    }

    @Test
    @DisplayName("Test async affinity call")
    public void testAsyncAffinityCall() throws Exception {
        String cacheName = getTestCacheName();
        CacheConfiguration<Integer, String> cfg = new CacheConfiguration<>(cacheName);
        IgniteCache<Integer, String> cache = ignite.getOrCreateCache(cfg);

        cache.put(1, "async-value");

        IgniteFuture<String> future = ignite.compute().affinityCallAsync(cacheName, 1, new IgniteCallable<String>() {
            @IgniteInstanceResource
            private Ignite localIgnite;

            @Override
            public String call() {
                return localIgnite.cache(cacheName).get(1).toString();
            }
        });

        String result = future.get(5, TimeUnit.SECONDS);
        assertThat(result).isEqualTo("async-value");
    }

    @Test
    @DisplayName("Test multiple async operations")
    public void testMultipleAsyncOperations() throws Exception {
        IgniteCompute compute = ignite.compute();

        List<IgniteFuture<Integer>> futures = new ArrayList<>();

        for (int i = 0; i < 5; i++) {
            final int num = i;
            futures.add(compute.callAsync(() -> num * 2));
        }

        List<Integer> results = new ArrayList<>();
        for (IgniteFuture<Integer> future : futures) {
            results.add(future.get(5, TimeUnit.SECONDS));
        }

        assertThat(results).hasSize(5);
        assertThat(results).containsExactlyInAnyOrder(0, 2, 4, 6, 8);
    }

    // ==================== Cluster Group Operations ====================

    @Test
    @DisplayName("Test compute on server nodes")
    public void testComputeOnServerNodes() {
        ClusterGroup serverNodes = ignite.cluster().forServers();
        IgniteCompute serverCompute = ignite.compute(serverNodes);

        assertThat(serverCompute).isNotNull();

        String result = serverCompute.call(() -> "server-result");
        assertThat(result).isEqualTo("server-result");
    }

    @Test
    @DisplayName("Test compute on local node")
    public void testComputeOnLocalNode() {
        ClusterGroup localNode = ignite.cluster().forLocal();
        IgniteCompute localCompute = ignite.compute(localNode);

        String nodeId = localCompute.call(() -> {
            return ignite.cluster().localNode().id().toString();
        });

        assertThat(nodeId).isEqualTo(ignite.cluster().localNode().id().toString());
    }

    @Test
    @DisplayName("Test broadcast on cluster group")
    public void testBroadcastOnClusterGroup() {
        ClusterGroup servers = ignite.cluster().forServers();
        IgniteCompute compute = ignite.compute(servers);

        Collection<String> results = compute.broadcast(() -> "server-broadcast");

        assertThat(results).isNotEmpty();
        assertThat(results).allMatch(r -> r.equals("server-broadcast"));
    }

    // ==================== Additional Compute Features ====================

    @Test
    @DisplayName("Test compute with timeout")
    public void testComputeWithTimeout() {
        IgniteCompute compute = ignite.compute().withTimeout(5000);

        assertThat(compute).isNotNull();

        String result = compute.call(() -> "timeout-test");
        assertThat(result).isEqualTo("timeout-test");
    }

    @Test
    @DisplayName("Test callable with IgniteInstanceResource injection")
    public void testCallableWithIgniteResource() {
        IgniteCompute compute = ignite.compute();

        String result = compute.call(new IgniteCallable<String>() {
            @IgniteInstanceResource
            private Ignite injectedIgnite;

            @Override
            public String call() {
                return injectedIgnite != null ? "injected" : "not-injected";
            }
        });

        assertThat(result).isEqualTo("injected");
    }

    @Test
    @DisplayName("Test compute job distribution")
    public void testComputeJobDistribution() {
        IgniteCompute compute = ignite.compute();

        Collection<IgniteCallable<String>> jobs = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            final int jobId = i;
            jobs.add(() -> "job-" + jobId);
        }

        Collection<String> results = compute.call(jobs);

        assertThat(results).hasSize(10);
    }

    @Test
    @DisplayName("Test compute with empty callable collection")
    public void testEmptyCallableCollection() {
        IgniteCompute compute = ignite.compute();

        Collection<IgniteCallable<String>> emptyJobs = new ArrayList<>();
        Collection<String> results = compute.call(emptyJobs);

        assertThat(results).isEmpty();
    }

    // ==================== Helper Classes ====================

    /**
     * Test result class for complex return values
     */
    static class ComputeResult implements Serializable {
        private final String name;
        private final int value;

        public ComputeResult(String name, int value) {
            this.name = name;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public int getValue() {
            return value;
        }
    }
}
