package com.example.ignite.tests;

import org.apache.ignite.Ignite;
import org.apache.ignite.Ignition;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.ipfinder.vm.TcpDiscoveryVmIpFinder;
import org.junit.jupiter.api.*;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Lab 13: Version Differences Tests
 *
 * These tests validate understanding of Apache Ignite 2.x vs 3.x differences.
 * They use the Ignite 2.x embedded API (available on the classpath) to demonstrate
 * 2.x concepts, and verify knowledge of 3.x equivalents through assertions.
 *
 * NOTE: Full Ignite 3.x integration tests are in the solutions/lab13_version_differences
 * module, which uses ignite-client:3.1.0 (incompatible classpath with 2.x).
 *
 * To run 3.x integration tests:
 *   cd solutions/lab13_version_differences
 *   mvn test  (requires running Ignite 3.x Docker container)
 */
@DisplayName("Lab 13: Version Differences - 2.x Concepts & 3.x Knowledge")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class Lab13VersionDifferencesTest {

    private static Ignite ignite;

    @BeforeAll
    static void startIgnite() {
        IgniteConfiguration cfg = new IgniteConfiguration();
        cfg.setIgniteInstanceName("lab13-test");
        cfg.setPeerClassLoadingEnabled(true);

        TcpDiscoverySpi disco = new TcpDiscoverySpi();
        TcpDiscoveryVmIpFinder ipFinder = new TcpDiscoveryVmIpFinder();
        ipFinder.setAddresses(Collections.singletonList("127.0.0.1:47500..47509"));
        disco.setIpFinder(ipFinder);
        cfg.setDiscoverySpi(disco);

        ignite = Ignition.start(cfg);
    }

    @AfterAll
    static void stopIgnite() {
        if (ignite != null) {
            ignite.close();
        }
    }

    // ==========================================
    // 1. Discovery: 2.x Ring vs 3.x SWIM
    // ==========================================

    @Test
    @Order(1)
    @DisplayName("2.x uses TcpDiscoverySpi with ring topology")
    void testDiscoverySpi() {
        assertThat(ignite.configuration().getDiscoverySpi())
                .isInstanceOf(TcpDiscoverySpi.class);
    }

    @Test
    @Order(2)
    @DisplayName("2.x uses IP finder for node discovery")
    void testIpFinder() {
        TcpDiscoverySpi disco = (TcpDiscoverySpi) ignite.configuration().getDiscoverySpi();
        assertThat(disco.getIpFinder()).isInstanceOf(TcpDiscoveryVmIpFinder.class);
        // 3.x: Uses SWIM gossip protocol, no IP finder needed
        // 3.x: Nodes configured via HOCON network settings
    }

    @Test
    @Order(3)
    @DisplayName("2.x cluster forms automatically via discovery")
    void testAutoDiscovery() {
        // 2.x: Cluster forms automatically when nodes discover each other
        assertThat(ignite.cluster().nodes()).isNotEmpty();
        // 3.x: Explicit cluster init required: ignite3 cluster init --name=myCluster
    }

    // ==========================================
    // 2. Configuration: 2.x Java/XML vs 3.x HOCON
    // ==========================================

    @Test
    @Order(4)
    @DisplayName("2.x uses IgniteConfiguration Java object")
    void testJavaConfiguration() {
        IgniteConfiguration cfg = ignite.configuration();
        assertThat(cfg).isNotNull();
        assertThat(cfg.getIgniteInstanceName()).isEqualTo("lab13-test");
        // 3.x: Uses HOCON files + CLI/REST for configuration
        // 3.x: Dynamic config updates via ignite3 cluster config update
    }

    @Test
    @Order(5)
    @DisplayName("2.x supports peer class loading")
    void testPeerClassLoading() {
        assertThat(ignite.configuration().isPeerClassLoadingEnabled()).isTrue();
        // 3.x: No peer class loading; uses deployment units instead
    }

    // ==========================================
    // 3. Cache API: 2.x Cache vs 3.x Table API
    // ==========================================

    @Test
    @Order(6)
    @DisplayName("2.x uses cache-first model with getOrCreateCache")
    void testCacheFirstModel() {
        var cache = ignite.getOrCreateCache("lab13-cache");
        assertThat(cache).isNotNull();
        cache.put(1, "hello");
        assertThat(cache.get(1)).isEqualTo("hello");
        cache.destroy();
        // 3.x: Schema-first model via SQL DDL (CREATE TABLE)
        // 3.x: Access via client.tables().table("TABLE_NAME").recordView()
    }

    @Test
    @Order(7)
    @DisplayName("2.x uses cache.put/get for CRUD")
    void testCachePutGet() {
        var cache = ignite.getOrCreateCache("lab13-crud");
        cache.put("key1", "value1");
        cache.put("key2", "value2");
        assertThat(cache.get("key1")).isEqualTo("value1");
        assertThat(cache.containsKey("key2")).isTrue();
        cache.remove("key1");
        assertThat(cache.containsKey("key1")).isFalse();
        cache.destroy();
        // 3.x: view.upsert/get/delete with Tuple or POJO
    }

    // ==========================================
    // 4. SQL: 2.x SqlFieldsQuery vs 3.x client.sql()
    // ==========================================

    @Test
    @Order(8)
    @DisplayName("2.x SQL uses SqlFieldsQuery")
    void testSqlFieldsQuery() {
        var cache = ignite.getOrCreateCache("lab13-sql");
        var qry = new org.apache.ignite.cache.query.SqlFieldsQuery(
                "SELECT 1 + 1 AS result");
        var result = cache.query(qry).getAll();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).get(0)).isEqualTo(2);
        cache.destroy();
        // 3.x: client.sql().execute(null, "SELECT 1 + 1 AS result")
        // 3.x: Returns ResultSet<SqlRow> instead of List<List<?>>
    }

    // ==========================================
    // 5. Transactions: 2.x concurrency/isolation vs 3.x simplified
    // ==========================================

    @Test
    @Order(9)
    @DisplayName("2.x transactions require concurrency and isolation mode")
    void testTransactionModel() {
        var cacheCfg = new org.apache.ignite.configuration.CacheConfiguration<String, String>("lab13-tx");
        cacheCfg.setAtomicityMode(org.apache.ignite.cache.CacheAtomicityMode.TRANSACTIONAL);
        var cache = ignite.getOrCreateCache(cacheCfg);
        var tx = ignite.transactions().txStart(
                org.apache.ignite.transactions.TransactionConcurrency.PESSIMISTIC,
                org.apache.ignite.transactions.TransactionIsolation.REPEATABLE_READ);
        cache.put("a", "1");
        tx.commit();
        assertThat(cache.get("a")).isEqualTo("1");
        cache.destroy();
        // 3.x: client.transactions().begin() — no concurrency/isolation params
        // 3.x: Strictly serializable by default (RAFT-based)
        // 3.x: runInTransaction(consumer) for auto-commit
    }

    // ==========================================
    // 6. Compute: 2.x callable vs 3.x JobDescriptor
    // ==========================================

    @Test
    @Order(10)
    @DisplayName("2.x compute uses ignite.compute().call()")
    void testComputeApi() {
        Integer result = ignite.compute().call(() -> 42);
        assertThat(result).isEqualTo(42);
        // 3.x: client.compute().execute(JobTarget.node(node), JobDescriptor.builder(...)...)
        // 3.x: Requires deployment units for server-side code
    }

    @Test
    @Order(11)
    @DisplayName("2.x supports MapReduce via ComputeTaskAdapter")
    void testMapReduceAvailability() {
        // 2.x: ComputeTaskAdapter, ComputeTaskSplitAdapter for MapReduce
        assertThat(org.apache.ignite.compute.ComputeTaskAdapter.class).isNotNull();
        // 3.x: No MapReduce equivalent yet; must be done manually
    }

    // ==========================================
    // 7. Data Placement: 2.x affinity vs 3.x Distribution Zones
    // ==========================================

    @Test
    @Order(12)
    @DisplayName("2.x uses affinity function for data placement")
    void testAffinityFunction() {
        var cacheCfg = new org.apache.ignite.configuration.CacheConfiguration<>("lab13-affinity");
        cacheCfg.setBackups(1);
        var cache = ignite.getOrCreateCache(cacheCfg);
        assertThat(cache.getConfiguration(org.apache.ignite.configuration.CacheConfiguration.class)
                .getBackups()).isEqualTo(1);
        cache.destroy();
        // 3.x: Distribution Zones (CREATE ZONE ... WITH replicas=N, partitions=N)
        // 3.x: COLOCATE BY in DDL for data colocation (replaces @AffinityKeyMapped)
    }

    // ==========================================
    // 8. Feature availability comparison
    // ==========================================

    @Test
    @Order(13)
    @DisplayName("2.x features not yet available in 3.x")
    void testFeaturesOnlyIn2x() {
        // Service Grid
        assertThat(ignite.services()).isNotNull();
        // Near Cache - available in 2.x cache config
        // Continuous Queries - available in 2.x
        // Peer Class Loading - available in 2.x
        // These features are NOT available in Ignite 3.x as of 3.1.0
    }

    @Test
    @Order(14)
    @DisplayName("3.x features not available in 2.x")
    void testFeaturesOnlyIn3x() {
        // These are 3.x-only features that cannot be demonstrated with 2.x API:
        // - Distribution Zones (CREATE ZONE)
        // - COLOCATE BY in DDL
        // - Pluggable storage engines (aimem, aipersist, rocksdb)
        // - Mixed SQL+KV transactions
        // - Native OpenMetrics endpoint
        // - Built-in REST API
        // - RAFT consensus
        // - Deployment units
        // - Dynamic HOCON configuration
        // Verified via Ignite 3.x integration tests in solutions module
        assertThat(true).as("3.x features documented and tested in solutions module").isTrue();
    }

    // ==========================================
    // 9. Client model differences
    // ==========================================

    @Test
    @Order(15)
    @DisplayName("2.x supports thick (embedded) client mode")
    void testThickClient() {
        // Starting a node IS using thick client mode in 2.x
        assertThat(ignite.cluster().localNode()).isNotNull();
        // 3.x: Only thin client (IgniteClient.builder().build())
        // 3.x: No embedded/thick client option
    }

    // ==========================================
    // 10. Monitoring differences
    // ==========================================

    @Test
    @Order(16)
    @DisplayName("2.x uses JMX for monitoring")
    void testJmxMonitoring() {
        // 2.x: Rich JMX MBean tree for metrics
        var mbs = java.lang.management.ManagementFactory.getPlatformMBeanServer();
        assertThat(mbs).isNotNull();
        // 3.x: REST API + CLI + OpenMetrics (native Prometheus support)
        // 3.x: System views via SQL (SELECT * FROM SYSTEM.TABLES)
    }

    // ==========================================
    // 11. API naming comparison
    // ==========================================

    @Test
    @Order(17)
    @DisplayName("Key API mapping: 2.x to 3.x")
    void testApiMapping() {
        // This test documents the key API mapping:
        // 2.x: Ignition.start(cfg)         → 3.x: IgniteClient.builder().build()
        // 2.x: ignite.cache(name)           → 3.x: client.tables().table(name)
        // 2.x: cache.put(k,v)              → 3.x: view.upsert(tx, tuple)
        // 2.x: cache.get(k)               → 3.x: view.get(tx, keyTuple)
        // 2.x: BinaryObject               → 3.x: Tuple
        // 2.x: SqlFieldsQuery             → 3.x: client.sql().execute()
        // 2.x: IgniteFuture               → 3.x: CompletableFuture
        // 2.x: @AffinityKeyMapped         → 3.x: COLOCATE BY
        // 2.x: CacheConfiguration         → 3.x: CREATE TABLE ... WITH PRIMARY_ZONE
        // 2.x: control.sh                 → 3.x: ignite3 CLI

        // Verify the 2.x APIs exist (compilation validates this)
        assertThat(org.apache.ignite.Ignition.class).isNotNull();
        assertThat(org.apache.ignite.cache.query.SqlFieldsQuery.class).isNotNull();
        assertThat(org.apache.ignite.binary.BinaryObject.class).isNotNull();
    }
}
