package com.example.ignite.tests;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.cluster.ClusterNode;
import org.apache.ignite.configuration.CacheConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collection;

import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.*;

/**
 * Comprehensive tests for Lab 2: Multi-Node Cluster
 * Coverage: Discovery, baseline topology, multi-node operations
 */
@DisplayName("Lab 02: Multi-Node Cluster Tests")
public class Lab02MultiNodeClusterTest extends BaseIgniteTest {

    @Test
    @DisplayName("Test second node can join cluster")
    public void testSecondNodeJoin() {
        assertThat(ignite.cluster().nodes()).hasSize(1);

        Ignite node2 = startAdditionalNode(testName + "-node2");

        await().until(() -> ignite.cluster().nodes().size() == 2);

        assertThat(ignite.cluster().nodes()).hasSize(2);
        assertThat(node2.cluster().nodes()).hasSize(2);

        node2.close();
    }

    @Test
    @DisplayName("Test three-node cluster formation")
    public void testThreeNodeCluster() {
        Ignite node2 = startAdditionalNode(testName + "-node2");
        Ignite node3 = startAdditionalNode(testName + "-node3");

        await().until(() -> ignite.cluster().nodes().size() == 3);

        assertThat(ignite.cluster().nodes()).hasSize(3);
        assertThat(node2.cluster().nodes()).hasSize(3);
        assertThat(node3.cluster().nodes()).hasSize(3);

        node2.close();
        node3.close();
    }

    @Test
    @DisplayName("Test cluster topology visibility")
    public void testClusterTopology() {
        Ignite node2 = startAdditionalNode(testName + "-node2");

        await().until(() -> ignite.cluster().nodes().size() == 2);

        Collection<ClusterNode> nodes = ignite.cluster().nodes();

        assertThat(nodes).hasSize(2);
        assertThat(nodes).allMatch(node -> node.id() != null);
        assertThat(nodes).allMatch(node -> node.consistentId() != null);

        node2.close();
    }

    @Test
    @DisplayName("Test cache accessibility from multiple nodes")
    public void testCacheAccessFromMultipleNodes() {
        CacheConfiguration<Integer, String> cfg = new CacheConfiguration<>(getTestCacheName());
        cfg.setBackups(1);

        IgniteCache<Integer, String> cache1 = ignite.getOrCreateCache(cfg);
        cache1.put(1, "value1");

        Ignite node2 = startAdditionalNode(testName + "-node2");
        await().until(() -> ignite.cluster().nodes().size() == 2);

        IgniteCache<Integer, String> cache2 = node2.cache(getTestCacheName());

        assertThat(cache2).isNotNull();
        assertThat(cache2.get(1)).isEqualTo("value1");

        node2.close();
    }

    @Test
    @DisplayName("Test data replication with backups")
    public void testDataReplication() {
        CacheConfiguration<Integer, String> cfg = new CacheConfiguration<>(getTestCacheName());
        cfg.setBackups(1);

        IgniteCache<Integer, String> cache1 = ignite.getOrCreateCache(cfg);

        Ignite node2 = startAdditionalNode(testName + "-node2");
        await().until(() -> ignite.cluster().nodes().size() == 2);

        cache1.put(1, "value1");
        cache1.put(2, "value2");

        IgniteCache<Integer, String> cache2 = node2.cache(getTestCacheName());

        assertThat(cache2.get(1)).isEqualTo("value1");
        assertThat(cache2.get(2)).isEqualTo("value2");

        node2.close();
    }

    @Test
    @DisplayName("Test node leave detection")
    public void testNodeLeave() {
        Ignite node2 = startAdditionalNode(testName + "-node2");
        await().until(() -> ignite.cluster().nodes().size() == 2);

        assertThat(ignite.cluster().nodes()).hasSize(2);

        node2.close();

        await().until(() -> ignite.cluster().nodes().size() == 1);

        assertThat(ignite.cluster().nodes()).hasSize(1);
    }

    @Test
    @DisplayName("Test server vs client mode")
    public void testServerClientMode() {
        assertThat(ignite.configuration().isClientMode()).isFalse();
        assertThat(ignite.cluster().localNode().isClient()).isFalse();
    }

    @Test
    @DisplayName("Test cluster has consistent ID")
    public void testClusterConsistentId() {
        Object consistentId = ignite.cluster().localNode().consistentId();

        assertThat(consistentId).isNotNull();
        assertThat(consistentId.toString()).isNotEmpty();
    }

    @Test
    @DisplayName("Test cluster metrics across nodes")
    public void testClusterMetricsAcrossNodes() {
        Ignite node2 = startAdditionalNode(testName + "-node2");
        await().until(() -> ignite.cluster().nodes().size() == 2);

        assertThat(ignite.cluster().metrics().getTotalNodes()).isEqualTo(2);
        assertThat(node2.cluster().metrics().getTotalNodes()).isEqualTo(2);

        node2.close();
    }

    @Test
    @DisplayName("Test data distribution across nodes")
    public void testDataDistribution() {
        CacheConfiguration<Integer, String> cfg = new CacheConfiguration<>(getTestCacheName());
        cfg.setBackups(0); // No backups, only primaries

        IgniteCache<Integer, String> cache1 = ignite.getOrCreateCache(cfg);

        Ignite node2 = startAdditionalNode(testName + "-node2");
        await().until(() -> ignite.cluster().nodes().size() == 2);

        // Put data
        for (int i = 0; i < 100; i++) {
            cache1.put(i, "value" + i);
        }

        // Both nodes should see all data
        IgniteCache<Integer, String> cache2 = node2.cache(getTestCacheName());
        assertThat(cache1.size()).isEqualTo(100);
        assertThat(cache2.size()).isEqualTo(100);

        node2.close();
    }

    @Test
    @DisplayName("Test cluster stable with multiple operations")
    public void testClusterStability() {
        Ignite node2 = startAdditionalNode(testName + "-node2");
        await().until(() -> ignite.cluster().nodes().size() == 2);

        IgniteCache<Integer, String> cache = ignite.getOrCreateCache(getTestCacheName());

        // Perform many operations
        for (int i = 0; i < 1000; i++) {
            cache.put(i, "value" + i);
        }

        assertThat(ignite.cluster().nodes()).hasSize(2);
        assertThat(cache.size()).isEqualTo(1000);

        node2.close();
    }

    @Test
    @DisplayName("Test node reconnection")
    public void testNodeReconnection() {
        Ignite node2 = startAdditionalNode(testName + "-node2");
        await().until(() -> ignite.cluster().nodes().size() == 2);

        node2.close();
        await().until(() -> ignite.cluster().nodes().size() == 1);

        Ignite node3 = startAdditionalNode(testName + "-node3");
        await().until(() -> ignite.cluster().nodes().size() == 2);

        assertThat(ignite.cluster().nodes()).hasSize(2);

        node3.close();
    }

    @Test
    @DisplayName("Test cache backup count configuration")
    public void testBackupConfiguration() {
        CacheConfiguration<Integer, String> cfg = new CacheConfiguration<>(getTestCacheName());
        cfg.setBackups(1);

        IgniteCache<Integer, String> cache = ignite.getOrCreateCache(cfg);

        assertThat(cache.getConfiguration(CacheConfiguration.class).getBackups()).isEqualTo(1);
    }

    @Test
    @DisplayName("Test all nodes see same data")
    public void testDataConsistency() {
        CacheConfiguration<Integer, String> cfg = new CacheConfiguration<>(getTestCacheName());
        cfg.setBackups(1);
        IgniteCache<Integer, String> cache1 = ignite.getOrCreateCache(cfg);

        Ignite node2 = startAdditionalNode(testName + "-node2");
        await().until(() -> ignite.cluster().nodes().size() == 2);

        cache1.put(1, "value1");

        IgniteCache<Integer, String> cache2 = node2.cache(getTestCacheName());

        await().untilAsserted(() ->
            assertThat(cache2.get(1)).isEqualTo("value1")
        );

        node2.close();
    }
}
