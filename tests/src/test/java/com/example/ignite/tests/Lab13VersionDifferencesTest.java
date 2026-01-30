package com.example.ignite.tests;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.IgniteCompute;
import org.apache.ignite.binary.BinaryObject;
import org.apache.ignite.cache.CacheAtomicityMode;
import org.apache.ignite.cache.CacheMode;
import org.apache.ignite.cache.query.QueryCursor;
import org.apache.ignite.cache.query.ScanQuery;
import org.apache.ignite.cache.query.SqlFieldsQuery;
import org.apache.ignite.cache.query.annotations.QuerySqlField;
import org.apache.ignite.cluster.ClusterNode;
import org.apache.ignite.cluster.ClusterState;
import org.apache.ignite.compute.ComputeJob;
import org.apache.ignite.compute.ComputeJobResult;
import org.apache.ignite.compute.ComputeTaskAdapter;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.lang.IgniteCallable;
import org.apache.ignite.lang.IgniteFuture;
import org.apache.ignite.lang.IgniteRunnable;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.ipfinder.vm.TcpDiscoveryVmIpFinder;
import org.apache.ignite.transactions.Transaction;
import org.apache.ignite.transactions.TransactionConcurrency;
import org.apache.ignite.transactions.TransactionIsolation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.cache.Cache;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.*;

/**
 * Comprehensive tests for Lab 13: Version Differences (2.x vs 3.x)
 * Tests validate the Ignite 2.x concepts demonstrated in the lab,
 * since Ignite 3.x requires a separate cluster and cannot be tested here.
 *
 * Coverage areas:
 * - Discovery configuration (TcpDiscoverySpi)
 * - Cluster lifecycle and state management
 * - Configuration approaches (programmatic)
 * - Cache API operations (CRUD, batch, BinaryObject)
 * - SQL queries via cache
 * - Transaction models (PESSIMISTIC/OPTIMISTIC)
 * - Compute grid (broadcast, callable, affinity-aware, MapReduce)
 * - Async operations (IgniteFuture)
 * - Multi-node cluster formation
 * - Schema via annotations (@QuerySqlField)
 */
@DisplayName("Lab 13: Version Differences Tests")
public class Lab13VersionDifferencesTest extends BaseIgniteTest {

    // ==========================================
    // Part 1: Discovery and Cluster Lifecycle
    // ==========================================

    @Test
    @DisplayName("Test TcpDiscoverySpi configuration (2.x discovery model)")
    public void testTcpDiscoverySpiConfiguration() {
        TcpDiscoverySpi spi = new TcpDiscoverySpi();
        TcpDiscoveryVmIpFinder ipFinder = new TcpDiscoveryVmIpFinder();
        ipFinder.setAddresses(Arrays.asList("127.0.0.1:47500..47509"));
        spi.setIpFinder(ipFinder);

        assertThat(spi.getIpFinder()).isNotNull();
        assertThat(spi.getIpFinder()).isInstanceOf(TcpDiscoveryVmIpFinder.class);

        IgniteConfiguration cfg = new IgniteConfiguration();
        cfg.setDiscoverySpi(spi);
        assertThat(cfg.getDiscoverySpi()).isEqualTo(spi);
    }

    @Test
    @DisplayName("Test cluster auto-formation on node start (2.x behavior)")
    public void testClusterAutoFormation() {
        // In 2.x, cluster forms automatically when nodes discover each other
        assertThat(ignite.cluster().nodes()).hasSize(1);
        assertThat(ignite.cluster().state()).isEqualTo(ClusterState.ACTIVE);

        Ignite node2 = startAdditionalNode(testName + "-node2");
        await().until(() -> ignite.cluster().nodes().size() == 2);

        // Both nodes automatically form a cluster — no explicit init required (unlike 3.x)
        assertThat(ignite.cluster().nodes()).hasSize(2);
        assertThat(node2.cluster().nodes()).hasSize(2);

        node2.close();
    }

    @Test
    @DisplayName("Test cluster state management (2.x API)")
    public void testClusterStateManagement() {
        ClusterState state = ignite.cluster().state();
        assertThat(state).isEqualTo(ClusterState.ACTIVE);
        assertThat(state.active()).isTrue();

        // 2.x supports ACTIVE, ACTIVE_READ_ONLY, INACTIVE
        ignite.cluster().state(ClusterState.ACTIVE);
        assertThat(ignite.cluster().state()).isEqualTo(ClusterState.ACTIVE);
    }

    @Test
    @DisplayName("Test baseline topology API (2.x concept)")
    public void testBaselineTopology() {
        Collection<ClusterNode> serverNodes = ignite.cluster().forServers().nodes();
        assertThat(serverNodes).isNotEmpty();

        // Baseline topology is a 2.x concept for persistent clusters
        // In non-persistent mode it may be null
        Collection<?> baseline = ignite.cluster().currentBaselineTopology();
        // Just verify API is accessible
        assertThat(ignite.cluster().state()).isNotNull();
    }

    @Test
    @DisplayName("Test topology version tracking")
    public void testTopologyVersionTracking() {
        long initialVersion = ignite.cluster().topologyVersion();
        assertThat(initialVersion).isGreaterThan(0);

        Ignite node2 = startAdditionalNode(testName + "-node2");
        await().until(() -> ignite.cluster().nodes().size() == 2);

        long newVersion = ignite.cluster().topologyVersion();
        assertThat(newVersion).isGreaterThan(initialVersion);

        node2.close();
    }

    // ==========================================
    // Part 2: Configuration (2.x approach)
    // ==========================================

    @Test
    @DisplayName("Test programmatic configuration (2.x style)")
    public void testProgrammaticConfiguration() {
        IgniteConfiguration cfg = new IgniteConfiguration();
        cfg.setIgniteInstanceName("test-programmatic");
        cfg.setClientMode(false);
        cfg.setPeerClassLoadingEnabled(true);
        cfg.setMetricsLogFrequency(0);

        assertThat(cfg.getIgniteInstanceName()).isEqualTo("test-programmatic");
        assertThat(cfg.isClientMode()).isFalse();
        assertThat(cfg.isPeerClassLoadingEnabled()).isTrue();
    }

    @Test
    @DisplayName("Test cache configuration options (2.x)")
    public void testCacheConfigurationOptions() {
        CacheConfiguration<Integer, String> cfg = new CacheConfiguration<>("test-cache-cfg");
        cfg.setCacheMode(CacheMode.PARTITIONED);
        cfg.setBackups(2);
        cfg.setAtomicityMode(CacheAtomicityMode.TRANSACTIONAL);

        assertThat(cfg.getCacheMode()).isEqualTo(CacheMode.PARTITIONED);
        assertThat(cfg.getBackups()).isEqualTo(2);
        assertThat(cfg.getAtomicityMode()).isEqualTo(CacheAtomicityMode.TRANSACTIONAL);
    }

    @Test
    @DisplayName("Test cache modes: PARTITIONED and REPLICATED")
    public void testCacheModes() {
        // PARTITIONED
        CacheConfiguration<Integer, String> partCfg = new CacheConfiguration<>(getTestCacheName() + "-part");
        partCfg.setCacheMode(CacheMode.PARTITIONED);
        IgniteCache<Integer, String> partCache = ignite.getOrCreateCache(partCfg);
        assertThat(partCache.getConfiguration(CacheConfiguration.class).getCacheMode())
            .isEqualTo(CacheMode.PARTITIONED);

        // REPLICATED
        CacheConfiguration<Integer, String> replCfg = new CacheConfiguration<>(getTestCacheName() + "-repl");
        replCfg.setCacheMode(CacheMode.REPLICATED);
        IgniteCache<Integer, String> replCache = ignite.getOrCreateCache(replCfg);
        assertThat(replCache.getConfiguration(CacheConfiguration.class).getCacheMode())
            .isEqualTo(CacheMode.REPLICATED);

        // Verify both modes are distinct
        assertThat(partCache.getConfiguration(CacheConfiguration.class).getCacheMode())
            .isNotEqualTo(replCache.getConfiguration(CacheConfiguration.class).getCacheMode());
    }

    // ==========================================
    // Part 3: Cache API (2.x style)
    // ==========================================

    @Test
    @DisplayName("Test basic CRUD via Cache API (2.x)")
    public void testCacheApiCrud() {
        IgniteCache<Integer, String> cache = ignite.getOrCreateCache(getTestCacheName());

        // Create
        cache.put(1, "alpha");
        cache.put(2, "beta");
        cache.put(3, "gamma");

        // Read
        assertThat(cache.get(1)).isEqualTo("alpha");
        assertThat(cache.get(2)).isEqualTo("beta");

        // Update
        cache.put(1, "alpha-updated");
        assertThat(cache.get(1)).isEqualTo("alpha-updated");

        // Delete
        cache.remove(3);
        assertThat(cache.get(3)).isNull();

        assertThat(cache.size()).isEqualTo(2);
    }

    @Test
    @DisplayName("Test batch operations via putAll/getAll (2.x)")
    public void testBatchOperations() {
        IgniteCache<Integer, String> cache = ignite.getOrCreateCache(getTestCacheName());

        Map<Integer, String> batch = new HashMap<>();
        for (int i = 0; i < 50; i++) {
            batch.put(i, "value-" + i);
        }
        cache.putAll(batch);

        Set<Integer> keys = new HashSet<>(Arrays.asList(0, 10, 20, 30, 40));
        Map<Integer, String> results = cache.getAll(keys);

        assertThat(results).hasSize(5);
        assertThat(results.get(10)).isEqualTo("value-10");
        assertThat(results.get(40)).isEqualTo("value-40");
    }

    @Test
    @DisplayName("Test BinaryObject access (2.x serialization)")
    public void testBinaryObjectAccess() {
        CacheConfiguration<Integer, Person> cfg = new CacheConfiguration<>(getTestCacheName());
        IgniteCache<Integer, Person> cache = ignite.getOrCreateCache(cfg);

        cache.put(1, new Person("Alice", 30));
        cache.put(2, new Person("Bob", 25));

        // Access via BinaryObject — 2.x uses BinaryMarshaller
        IgniteCache<Integer, BinaryObject> binaryCache = cache.withKeepBinary();
        BinaryObject bo = binaryCache.get(1);

        assertThat(bo).isNotNull();
        assertThat(bo.<String>field("name")).isEqualTo("Alice");
        assertThat(bo.<Integer>field("age")).isEqualTo(30);
    }

    @Test
    @DisplayName("Test ScanQuery on cache (2.x)")
    public void testScanQuery() {
        IgniteCache<Integer, String> cache = ignite.getOrCreateCache(getTestCacheName());
        for (int i = 0; i < 20; i++) {
            cache.put(i, "val-" + i);
        }

        List<Cache.Entry<Integer, String>> results = new ArrayList<>();
        try (QueryCursor<Cache.Entry<Integer, String>> cursor =
                 cache.query(new ScanQuery<>((k, v) -> k >= 10))) {
            cursor.forEach(results::add);
        }

        assertThat(results).hasSize(10);
    }

    // ==========================================
    // Part 4: SQL via Cache (2.x approach)
    // ==========================================

    @Test
    @DisplayName("Test SQL queries via SqlFieldsQuery (2.x)")
    public void testSqlFieldsQuery() {
        CacheConfiguration<Integer, PersonIndexed> cfg = new CacheConfiguration<>(getTestCacheName());
        cfg.setIndexedTypes(Integer.class, PersonIndexed.class);

        IgniteCache<Integer, PersonIndexed> cache = ignite.getOrCreateCache(cfg);
        cache.put(1, new PersonIndexed("Alice", 30));
        cache.put(2, new PersonIndexed("Bob", 25));
        cache.put(3, new PersonIndexed("Charlie", 35));

        // SQL query via cache (2.x approach — H2 + Calcite engine)
        SqlFieldsQuery query = new SqlFieldsQuery(
            "SELECT name, age FROM PersonIndexed WHERE age > ?");
        query.setArgs(28);

        List<List<?>> rows = cache.query(query).getAll();
        assertThat(rows).hasSize(2);
    }

    @Test
    @DisplayName("Test SQL DDL via SqlFieldsQuery (2.x)")
    public void testSqlDdl() {
        IgniteCache<Integer, PersonIndexed> cache = ignite.getOrCreateCache(
            new CacheConfiguration<Integer, PersonIndexed>(getTestCacheName())
                .setIndexedTypes(Integer.class, PersonIndexed.class));

        // Insert via SQL
        cache.query(new SqlFieldsQuery(
            "INSERT INTO PersonIndexed (_key, name, age) VALUES (?, ?, ?)")
            .setArgs(100, "Dave", 40)).getAll();

        // Verify via cache API
        PersonIndexed p = cache.get(100);
        assertThat(p).isNotNull();
        assertThat(p.name).isEqualTo("Dave");
        assertThat(p.age).isEqualTo(40);
    }

    @Test
    @DisplayName("Test @QuerySqlField annotations (2.x schema model)")
    public void testQuerySqlFieldAnnotations() {
        CacheConfiguration<Integer, PersonIndexed> cfg = new CacheConfiguration<>(getTestCacheName());
        cfg.setIndexedTypes(Integer.class, PersonIndexed.class);

        IgniteCache<Integer, PersonIndexed> cache = ignite.getOrCreateCache(cfg);
        cache.put(1, new PersonIndexed("Alice", 30));

        // Annotations define schema in 2.x (vs DDL-first in 3.x)
        List<List<?>> rows = cache.query(
            new SqlFieldsQuery("SELECT name FROM PersonIndexed WHERE name = ?")
                .setArgs("Alice")).getAll();

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).get(0)).isEqualTo("Alice");
    }

    // ==========================================
    // Part 5: Transactions (2.x models)
    // ==========================================

    @Test
    @DisplayName("Test PESSIMISTIC REPEATABLE_READ transaction (2.x)")
    public void testPessimisticTransaction() {
        CacheConfiguration<Integer, Integer> cfg = new CacheConfiguration<>(getTestCacheName());
        cfg.setAtomicityMode(CacheAtomicityMode.TRANSACTIONAL);
        IgniteCache<Integer, Integer> cache = ignite.getOrCreateCache(cfg);

        cache.put(1, 100);

        try (Transaction tx = ignite.transactions().txStart(
                TransactionConcurrency.PESSIMISTIC,
                TransactionIsolation.REPEATABLE_READ)) {
            int val = cache.get(1);
            cache.put(1, val + 50);
            tx.commit();
        }

        assertThat(cache.get(1)).isEqualTo(150);
    }

    @Test
    @DisplayName("Test OPTIMISTIC SERIALIZABLE transaction (2.x)")
    public void testOptimisticTransaction() {
        CacheConfiguration<Integer, Integer> cfg = new CacheConfiguration<>(getTestCacheName());
        cfg.setAtomicityMode(CacheAtomicityMode.TRANSACTIONAL);
        IgniteCache<Integer, Integer> cache = ignite.getOrCreateCache(cfg);

        cache.put(1, 200);

        try (Transaction tx = ignite.transactions().txStart(
                TransactionConcurrency.OPTIMISTIC,
                TransactionIsolation.SERIALIZABLE)) {
            int val = cache.get(1);
            cache.put(1, val - 75);
            tx.commit();
        }

        assertThat(cache.get(1)).isEqualTo(125);
    }

    @Test
    @DisplayName("Test transaction rollback (2.x)")
    public void testTransactionRollback() {
        CacheConfiguration<Integer, Integer> cfg = new CacheConfiguration<>(getTestCacheName());
        cfg.setAtomicityMode(CacheAtomicityMode.TRANSACTIONAL);
        IgniteCache<Integer, Integer> cache = ignite.getOrCreateCache(cfg);

        cache.put(1, 500);

        try (Transaction tx = ignite.transactions().txStart(
                TransactionConcurrency.PESSIMISTIC,
                TransactionIsolation.REPEATABLE_READ)) {
            cache.put(1, 0);
            tx.rollback();
        }

        assertThat(cache.get(1)).isEqualTo(500);
    }

    // ==========================================
    // Part 6: Compute Grid (2.x)
    // ==========================================

    @Test
    @DisplayName("Test compute broadcast (2.x)")
    public void testComputeBroadcast() {
        AtomicInteger counter = new AtomicInteger(0);

        ignite.compute().broadcast((IgniteRunnable) () -> {
            // Runs on each node
        });

        // If we get here without exception, broadcast succeeded
        assertThat(ignite.cluster().nodes()).hasSize(1);
    }

    @Test
    @DisplayName("Test compute callable (2.x)")
    public void testComputeCallable() {
        Collection<Integer> results = ignite.compute().broadcast(
            (IgniteCallable<Integer>) () -> 42
        );

        assertThat(results).hasSize(1);
        assertThat(results.iterator().next()).isEqualTo(42);
    }

    @Test
    @DisplayName("Test compute call with collection (2.x)")
    public void testComputeCallCollection() {
        List<IgniteCallable<Integer>> jobs = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            final int val = i;
            jobs.add(() -> val * val);
        }

        Collection<Integer> results = ignite.compute().call(jobs);
        assertThat(results).hasSize(5);

        int sum = results.stream().mapToInt(Integer::intValue).sum();
        // 1+4+9+16+25 = 55
        assertThat(sum).isEqualTo(55);
    }

    @Test
    @DisplayName("Test affinity-aware compute (2.x)")
    public void testAffinityCompute() {
        IgniteCache<Integer, String> cache = ignite.getOrCreateCache(getTestCacheName());
        cache.put(1, "test-value");

        String result = ignite.compute().affinityCall(
            getTestCacheName(), 1,
            () -> "executed-on-primary"
        );

        assertThat(result).isEqualTo("executed-on-primary");
    }

    @Test
    @DisplayName("Test async compute with IgniteFuture (2.x)")
    public void testAsyncComputeIgniteFuture() {
        IgniteCompute asyncCompute = ignite.compute().withAsync();

        // 2.x uses IgniteFuture (vs CompletableFuture in 3.x)
        IgniteFuture<Collection<Integer>> future = ignite.compute().broadcastAsync(
            (IgniteCallable<Integer>) () -> 99
        );

        Collection<Integer> results = future.get();
        assertThat(results).hasSize(1);
        assertThat(results.iterator().next()).isEqualTo(99);
    }

    @Test
    @DisplayName("Test MapReduce with ComputeTaskAdapter (2.x)")
    public void testMapReduceTask() {
        // 2.x has full MapReduce support (3.x has limited compute)
        Integer result = ignite.compute().execute(
            new SumMapReduceTask(), Arrays.asList(10, 20, 30, 40, 50));

        assertThat(result).isEqualTo(150);
    }

    @Test
    @DisplayName("Test compute on cluster group (2.x)")
    public void testComputeOnClusterGroup() {
        Ignite node2 = startAdditionalNode(testName + "-node2");
        await().until(() -> ignite.cluster().nodes().size() == 2);

        Collection<Integer> results = ignite.compute(
            ignite.cluster().forServers()
        ).broadcast((IgniteCallable<Integer>) () -> 1);

        assertThat(results).hasSize(2);

        node2.close();
    }

    // ==========================================
    // Part 7: Multi-node data distribution
    // ==========================================

    @Test
    @DisplayName("Test data visible across nodes (2.x)")
    public void testDataVisibleAcrossNodes() {
        CacheConfiguration<Integer, String> cfg = new CacheConfiguration<>(getTestCacheName());
        cfg.setBackups(1);
        IgniteCache<Integer, String> cache = ignite.getOrCreateCache(cfg);

        for (int i = 0; i < 50; i++) {
            cache.put(i, "data-" + i);
        }

        Ignite node2 = startAdditionalNode(testName + "-node2");
        await().until(() -> ignite.cluster().nodes().size() == 2);

        IgniteCache<Integer, String> cache2 = node2.cache(getTestCacheName());
        assertThat(cache2).isNotNull();
        assertThat(cache2.get(0)).isEqualTo("data-0");
        assertThat(cache2.get(49)).isEqualTo("data-49");

        node2.close();
    }

    @Test
    @DisplayName("Test affinity key mapping (2.x colocation)")
    public void testAffinityKeyMapping() {
        IgniteCache<Integer, String> cache = ignite.getOrCreateCache(getTestCacheName());
        cache.put(1, "value1");

        ClusterNode primary = ignite.affinity(getTestCacheName()).mapKeyToNode(1);
        assertThat(primary).isNotNull();

        int partition = ignite.affinity(getTestCacheName()).partition(1);
        assertThat(partition).isGreaterThanOrEqualTo(0);
    }

    // ==========================================
    // Part 8: Peer class loading and config
    // ==========================================

    @Test
    @DisplayName("Test peer class loading configuration (2.x feature)")
    public void testPeerClassLoadingConfig() {
        // Peer class loading is a 2.x feature not in 3.x
        IgniteConfiguration cfg = new IgniteConfiguration();
        cfg.setPeerClassLoadingEnabled(true);
        assertThat(cfg.isPeerClassLoadingEnabled()).isTrue();

        cfg.setPeerClassLoadingEnabled(false);
        assertThat(cfg.isPeerClassLoadingEnabled()).isFalse();
    }

    @Test
    @DisplayName("Test metrics configuration (2.x JMX-based)")
    public void testMetricsConfiguration() {
        // 2.x uses JMX for metrics (3.x uses REST + OpenMetrics)
        assertThat(ignite.cluster().metrics()).isNotNull();
        assertThat(ignite.cluster().metrics().getTotalNodes()).isEqualTo(1);
        assertThat(ignite.cluster().metrics().getTotalCpus()).isGreaterThan(0);
    }

    @Test
    @DisplayName("Test node attributes (2.x)")
    public void testNodeAttributes() {
        Map<String, ?> attrs = ignite.cluster().localNode().attributes();
        assertThat(attrs).isNotNull();
        assertThat(attrs).isNotEmpty();
        // 2.x nodes expose many attributes via discovery
        assertThat(ignite.cluster().localNode().id()).isNotNull();
        assertThat(ignite.cluster().localNode().consistentId()).isNotNull();
    }

    @Test
    @DisplayName("Test server vs client mode distinction (2.x)")
    public void testServerClientModeDistinction() {
        // 2.x has explicit client mode on configuration
        assertThat(ignite.configuration().isClientMode()).isFalse();
        assertThat(ignite.cluster().localNode().isClient()).isFalse();

        // forServers/forClients cluster group API
        assertThat(ignite.cluster().forServers().nodes()).hasSize(1);
        assertThat(ignite.cluster().forClients().nodes()).isEmpty();
    }

    // ==========================================
    // Helper classes
    // ==========================================

    static class Person implements Serializable {
        String name;
        int age;

        Person(String name, int age) {
            this.name = name;
            this.age = age;
        }
    }

    static class PersonIndexed implements Serializable {
        @QuerySqlField(index = true)
        String name;

        @QuerySqlField
        int age;

        PersonIndexed(String name, int age) {
            this.name = name;
            this.age = age;
        }
    }

    /**
     * Simple MapReduce task that sums a list of integers.
     * Demonstrates 2.x ComputeTaskAdapter (not available in 3.x).
     */
    static class SumMapReduceTask extends ComputeTaskAdapter<List<Integer>, Integer> {
        @Override
        public Map<? extends ComputeJob, ClusterNode> map(
                List<ClusterNode> nodes, List<Integer> args) {
            Map<ComputeJob, ClusterNode> jobMap = new HashMap<>();
            int nodeIdx = 0;
            for (Integer val : args) {
                jobMap.put(new IdentityJob(val), nodes.get(nodeIdx % nodes.size()));
                nodeIdx++;
            }
            return jobMap;
        }

        @Override
        public Integer reduce(List<ComputeJobResult> results) {
            int sum = 0;
            for (ComputeJobResult r : results) {
                if (r.getException() == null) {
                    sum += r.<Integer>getData();
                }
            }
            return sum;
        }
    }

    static class IdentityJob implements ComputeJob, Serializable {
        private final int value;

        IdentityJob(int value) {
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
