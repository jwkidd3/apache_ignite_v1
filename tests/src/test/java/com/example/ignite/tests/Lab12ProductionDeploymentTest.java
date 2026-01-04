package com.example.ignite.tests;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.cluster.ClusterState;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.ConnectorConfiguration;
import org.apache.ignite.configuration.DataRegionConfiguration;
import org.apache.ignite.configuration.DataStorageConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.ssl.SslContextFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive tests for Lab 12: Production Deployment and Best Practices
 * Coverage: Security configuration, cluster state management, baseline topology
 */
@DisplayName("Lab 12: Production Deployment Tests")
public class Lab12ProductionDeploymentTest extends BaseIgniteTest {

    // ========================================
    // Security Configuration Tests
    // ========================================

    @Test
    @DisplayName("Test SSL context factory configuration")
    public void testSslContextFactoryConfiguration() {
        SslContextFactory sslFactory = new SslContextFactory();

        sslFactory.setKeyStoreFilePath("keystore.jks");
        sslFactory.setKeyStorePassword("changeit".toCharArray());
        sslFactory.setTrustStoreFilePath("truststore.jks");
        sslFactory.setTrustStorePassword("changeit".toCharArray());
        sslFactory.setProtocol("TLSv1.2");

        assertThat(sslFactory.getKeyStoreFilePath()).isEqualTo("keystore.jks");
        assertThat(sslFactory.getTrustStoreFilePath()).isEqualTo("truststore.jks");
        assertThat(sslFactory.getProtocol()).isEqualTo("TLSv1.2");
    }

    @Test
    @DisplayName("Test SSL context factory with TLS 1.3 protocol")
    public void testSslContextFactoryTls13() {
        SslContextFactory sslFactory = new SslContextFactory();

        sslFactory.setProtocol("TLSv1.3");
        sslFactory.setKeyStoreFilePath("/secure/path/keystore.jks");
        sslFactory.setTrustStoreFilePath("/secure/path/truststore.jks");

        assertThat(sslFactory.getProtocol()).isEqualTo("TLSv1.3");
    }

    @Test
    @DisplayName("Test connector configuration with SSL enabled")
    public void testConnectorConfigurationSsl() {
        ConnectorConfiguration connectorCfg = new ConnectorConfiguration();

        connectorCfg.setSslEnabled(true);
        connectorCfg.setSslClientAuth(true);
        connectorCfg.setPort(8443);

        assertThat(connectorCfg.isSslEnabled()).isTrue();
        assertThat(connectorCfg.isSslClientAuth()).isTrue();
        assertThat(connectorCfg.getPort()).isEqualTo(8443);
    }

    @Test
    @DisplayName("Test SSL factory can be attached to Ignite configuration")
    public void testSslFactoryInIgniteConfiguration() {
        IgniteConfiguration cfg = new IgniteConfiguration();
        SslContextFactory sslFactory = new SslContextFactory();

        sslFactory.setProtocol("TLSv1.2");
        sslFactory.setKeyStoreFilePath("keystore.jks");
        sslFactory.setKeyStorePassword("password".toCharArray());

        cfg.setSslContextFactory(sslFactory);

        assertThat(cfg.getSslContextFactory()).isNotNull();
        assertThat(cfg.getSslContextFactory()).isInstanceOf(SslContextFactory.class);
    }

    // ========================================
    // Cluster State Management Tests
    // ========================================

    @Test
    @DisplayName("Test cluster state is active")
    public void testClusterStateActive() {
        ClusterState state = ignite.cluster().state();

        assertThat(state).isEqualTo(ClusterState.ACTIVE);
    }

    @Test
    @DisplayName("Test cluster state transitions")
    public void testClusterStateTransitions() {
        // Cluster should start in active state
        assertThat(ignite.cluster().state()).isEqualTo(ClusterState.ACTIVE);

        // Transition to read-only
        ignite.cluster().state(ClusterState.ACTIVE_READ_ONLY);
        assertThat(ignite.cluster().state()).isEqualTo(ClusterState.ACTIVE_READ_ONLY);

        // Transition back to active
        ignite.cluster().state(ClusterState.ACTIVE);
        assertThat(ignite.cluster().state()).isEqualTo(ClusterState.ACTIVE);
    }

    @Test
    @DisplayName("Test cluster can be deactivated and reactivated")
    public void testClusterDeactivateReactivate() {
        // Deactivate cluster
        ignite.cluster().state(ClusterState.INACTIVE);
        assertThat(ignite.cluster().state()).isEqualTo(ClusterState.INACTIVE);

        // Reactivate cluster
        ignite.cluster().state(ClusterState.ACTIVE);
        assertThat(ignite.cluster().state()).isEqualTo(ClusterState.ACTIVE);
    }

    @Test
    @DisplayName("Test read-only mode prevents writes")
    public void testReadOnlyModePreventsWrites() {
        CacheConfiguration<Integer, String> cacheCfg = new CacheConfiguration<>(getTestCacheName());
        IgniteCache<Integer, String> cache = ignite.getOrCreateCache(cacheCfg);

        // Put some data while active
        cache.put(1, "value1");
        assertThat(cache.get(1)).isEqualTo("value1");

        // Switch to read-only mode
        ignite.cluster().state(ClusterState.ACTIVE_READ_ONLY);

        // Reading should work
        assertThat(cache.get(1)).isEqualTo("value1");

        // Writing should fail
        assertThatThrownBy(() -> cache.put(2, "value2"))
            .isInstanceOf(Exception.class);

        // Switch back to active for cleanup
        ignite.cluster().state(ClusterState.ACTIVE);
    }

    @Test
    @DisplayName("Test cluster topology version")
    public void testClusterTopologyVersion() {
        long topologyVersion = ignite.cluster().topologyVersion();

        assertThat(topologyVersion).isGreaterThan(0);
    }

    @Test
    @DisplayName("Test cluster nodes count")
    public void testClusterNodesCount() {
        int nodeCount = ignite.cluster().nodes().size();

        assertThat(nodeCount).isGreaterThanOrEqualTo(1);
    }

    // ========================================
    // Baseline Topology Configuration Tests
    // ========================================

    @Test
    @DisplayName("Test baseline topology is accessible")
    public void testBaselineTopologyAccessible() {
        // For in-memory mode without persistence, baseline auto-adjust is typically enabled
        boolean autoAdjust = ignite.cluster().isBaselineAutoAdjustEnabled();

        // Just verify we can access the setting
        assertThat(autoAdjust).isIn(true, false);
    }

    @Test
    @DisplayName("Test baseline auto-adjust timeout configuration")
    public void testBaselineAutoAdjustTimeout() {
        // Set auto-adjust timeout
        long timeout = 30000L; // 30 seconds
        ignite.cluster().baselineAutoAdjustTimeout(timeout);

        long configuredTimeout = ignite.cluster().baselineAutoAdjustTimeout();
        assertThat(configuredTimeout).isEqualTo(timeout);
    }

    @Test
    @DisplayName("Test enable and disable baseline auto-adjust")
    public void testBaselineAutoAdjustToggle() {
        // Enable auto-adjust
        ignite.cluster().baselineAutoAdjustEnabled(true);
        assertThat(ignite.cluster().isBaselineAutoAdjustEnabled()).isTrue();

        // Disable auto-adjust
        ignite.cluster().baselineAutoAdjustEnabled(false);
        assertThat(ignite.cluster().isBaselineAutoAdjustEnabled()).isFalse();

        // Re-enable for cleanup
        ignite.cluster().baselineAutoAdjustEnabled(true);
    }

    @Test
    @DisplayName("Test local node is in cluster")
    public void testLocalNodeInCluster() {
        assertThat(ignite.cluster().localNode()).isNotNull();
        assertThat(ignite.cluster().nodes()).contains(ignite.cluster().localNode());
    }

    // ========================================
    // Production Configuration Tests
    // ========================================

    @Test
    @DisplayName("Test data storage configuration for production")
    public void testDataStorageConfigurationProduction() {
        DataStorageConfiguration storageCfg = new DataStorageConfiguration();

        // Configure WAL for durability
        storageCfg.setWalPath("/var/ignite/wal");
        storageCfg.setWalArchivePath("/var/ignite/wal-archive");
        storageCfg.setStoragePath("/var/ignite/storage");

        assertThat(storageCfg.getWalPath()).isEqualTo("/var/ignite/wal");
        assertThat(storageCfg.getWalArchivePath()).isEqualTo("/var/ignite/wal-archive");
        assertThat(storageCfg.getStoragePath()).isEqualTo("/var/ignite/storage");
    }

    @Test
    @DisplayName("Test data region configuration for production memory sizing")
    public void testDataRegionConfigurationProduction() {
        DataRegionConfiguration regionCfg = new DataRegionConfiguration();

        regionCfg.setName("production-region");
        regionCfg.setInitialSize(1L * 1024 * 1024 * 1024); // 1 GB
        regionCfg.setMaxSize(4L * 1024 * 1024 * 1024);     // 4 GB
        regionCfg.setPersistenceEnabled(true);
        regionCfg.setMetricsEnabled(true);

        assertThat(regionCfg.getName()).isEqualTo("production-region");
        assertThat(regionCfg.getInitialSize()).isEqualTo(1L * 1024 * 1024 * 1024);
        assertThat(regionCfg.getMaxSize()).isEqualTo(4L * 1024 * 1024 * 1024);
        assertThat(regionCfg.isPersistenceEnabled()).isTrue();
        assertThat(regionCfg.isMetricsEnabled()).isTrue();
    }

    @Test
    @DisplayName("Test multiple data regions configuration")
    public void testMultipleDataRegionsConfiguration() {
        DataStorageConfiguration storageCfg = new DataStorageConfiguration();

        // Default region
        DataRegionConfiguration defaultRegion = new DataRegionConfiguration();
        defaultRegion.setName("Default_Region");
        defaultRegion.setInitialSize(256L * 1024 * 1024);
        defaultRegion.setMaxSize(512L * 1024 * 1024);

        // Hot data region (in-memory only)
        DataRegionConfiguration hotRegion = new DataRegionConfiguration();
        hotRegion.setName("hot-data");
        hotRegion.setInitialSize(512L * 1024 * 1024);
        hotRegion.setMaxSize(1L * 1024 * 1024 * 1024);
        hotRegion.setPersistenceEnabled(false);

        // Cold data region (persistent)
        DataRegionConfiguration coldRegion = new DataRegionConfiguration();
        coldRegion.setName("cold-data");
        coldRegion.setInitialSize(1L * 1024 * 1024 * 1024);
        coldRegion.setMaxSize(8L * 1024 * 1024 * 1024);
        coldRegion.setPersistenceEnabled(true);

        storageCfg.setDefaultDataRegionConfiguration(defaultRegion);
        storageCfg.setDataRegionConfigurations(hotRegion, coldRegion);

        assertThat(storageCfg.getDefaultDataRegionConfiguration().getName())
            .isEqualTo("Default_Region");
        assertThat(storageCfg.getDataRegionConfigurations()).hasSize(2);
    }

    @Test
    @DisplayName("Test checkpoint configuration for production")
    public void testCheckpointConfiguration() {
        DataStorageConfiguration storageCfg = new DataStorageConfiguration();

        // Configure checkpointing
        storageCfg.setCheckpointFrequency(60000L);     // 1 minute
        storageCfg.setCheckpointThreads(4);
        storageCfg.setWalCompactionEnabled(true);

        assertThat(storageCfg.getCheckpointFrequency()).isEqualTo(60000L);
        assertThat(storageCfg.getCheckpointThreads()).isEqualTo(4);
        assertThat(storageCfg.isWalCompactionEnabled()).isTrue();
    }

    @Test
    @DisplayName("Test Ignite configuration with metrics enabled")
    public void testIgniteConfigurationMetrics() {
        IgniteConfiguration cfg = new IgniteConfiguration();

        cfg.setMetricsLogFrequency(60000L); // Log metrics every minute
        cfg.setMetricsUpdateFrequency(5000L); // Update metrics every 5 seconds

        assertThat(cfg.getMetricsLogFrequency()).isEqualTo(60000L);
        assertThat(cfg.getMetricsUpdateFrequency()).isEqualTo(5000L);
    }

    @Test
    @DisplayName("Test cache configuration with backups for high availability")
    public void testCacheConfigurationHighAvailability() {
        CacheConfiguration<Integer, String> cacheCfg = new CacheConfiguration<>(getTestCacheName());

        cacheCfg.setBackups(2); // 2 backup copies for high availability
        cacheCfg.setStatisticsEnabled(true);

        IgniteCache<Integer, String> cache = ignite.getOrCreateCache(cacheCfg);

        cache.put(1, "production-value");
        assertThat(cache.get(1)).isEqualTo("production-value");

        // Verify cache configuration
        CacheConfiguration<?, ?> retrievedCfg = cache.getConfiguration(CacheConfiguration.class);
        assertThat(retrievedCfg.getBackups()).isEqualTo(2);
    }

    @Test
    @DisplayName("Test failure detection timeout configuration")
    public void testFailureDetectionTimeout() {
        IgniteConfiguration cfg = new IgniteConfiguration();

        cfg.setFailureDetectionTimeout(10000L); // 10 seconds
        cfg.setClientFailureDetectionTimeout(30000L); // 30 seconds for clients

        assertThat(cfg.getFailureDetectionTimeout()).isEqualTo(10000L);
        assertThat(cfg.getClientFailureDetectionTimeout()).isEqualTo(30000L);
    }

    @Test
    @DisplayName("Test cluster node attributes")
    public void testClusterNodeAttributes() {
        // Local node should have attributes
        var localNode = ignite.cluster().localNode();

        assertThat(localNode.id()).isNotNull();
        assertThat(localNode.consistentId()).isNotNull();
        assertThat(localNode.hostNames()).isNotEmpty();
    }
}
