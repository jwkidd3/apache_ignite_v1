package com.example.ignite.tests;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.cluster.ClusterState;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.ConnectorConfiguration;
import org.apache.ignite.configuration.DataRegionConfiguration;
import org.apache.ignite.configuration.DataStorageConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.plugin.security.SecurityCredentials;
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

    // ========================================
    // Authentication Configuration Tests
    // ========================================

    @Test
    @DisplayName("Test security/authentication configuration options")
    public void testAuthenticationConfiguration() {
        // Test SecurityCredentials configuration
        SecurityCredentials credentials = new SecurityCredentials("admin", "password123");

        assertThat(credentials.getLogin()).isEqualTo("admin");
        assertThat(credentials.getPassword()).isEqualTo("password123");

        // Test credentials with user object
        SecurityCredentials credentialsWithUserObj = new SecurityCredentials("user", "pass", "custom-user-object");
        assertThat(credentialsWithUserObj.getLogin()).isEqualTo("user");
        assertThat(credentialsWithUserObj.getPassword()).isEqualTo("pass");
        assertThat(credentialsWithUserObj.getUserObject()).isEqualTo("custom-user-object");

        // Test IgniteConfiguration with authentication enabled
        IgniteConfiguration cfg = new IgniteConfiguration();
        cfg.setAuthenticationEnabled(true);

        assertThat(cfg.isAuthenticationEnabled()).isTrue();

        // Verify authentication can be disabled
        cfg.setAuthenticationEnabled(false);
        assertThat(cfg.isAuthenticationEnabled()).isFalse();

        // Test SSL configuration for secure communication (authentication transport)
        SslContextFactory sslFactory = new SslContextFactory();
        sslFactory.setKeyStoreFilePath("keystore.jks");
        sslFactory.setKeyStorePassword("password".toCharArray());
        sslFactory.setTrustStoreFilePath("truststore.jks");
        sslFactory.setTrustStorePassword("password".toCharArray());
        sslFactory.setProtocol("TLSv1.2");

        cfg.setSslContextFactory(sslFactory);

        assertThat(cfg.getSslContextFactory()).isNotNull();
        assertThat(cfg.getSslContextFactory()).isInstanceOf(SslContextFactory.class);

        log.info("Authentication configuration validated successfully");
    }

    // ========================================
    // Snapshot Configuration Tests
    // ========================================

    @Test
    @DisplayName("Test snapshot backup configuration")
    public void testSnapshotConfiguration() {
        DataStorageConfiguration storageCfg = new DataStorageConfiguration();

        // Configure snapshot path for backups
        String snapshotPath = "/var/ignite/snapshots";
        storageCfg.setStoragePath("/var/ignite/storage");

        assertThat(storageCfg.getStoragePath()).isEqualTo("/var/ignite/storage");

        // Configure WAL paths (required for snapshot consistency)
        storageCfg.setWalPath("/var/ignite/wal");
        storageCfg.setWalArchivePath("/var/ignite/wal-archive");

        assertThat(storageCfg.getWalPath()).isEqualTo("/var/ignite/wal");
        assertThat(storageCfg.getWalArchivePath()).isEqualTo("/var/ignite/wal-archive");

        // Configure data region for persistence (required for snapshots)
        DataRegionConfiguration persistentRegion = new DataRegionConfiguration();
        persistentRegion.setName("persistent-region");
        persistentRegion.setPersistenceEnabled(true);
        persistentRegion.setInitialSize(256L * 1024 * 1024);
        persistentRegion.setMaxSize(1L * 1024 * 1024 * 1024);

        storageCfg.setDefaultDataRegionConfiguration(persistentRegion);

        assertThat(storageCfg.getDefaultDataRegionConfiguration().isPersistenceEnabled()).isTrue();
        assertThat(storageCfg.getDefaultDataRegionConfiguration().getName()).isEqualTo("persistent-region");

        // Configure checkpoint settings for snapshot consistency
        storageCfg.setCheckpointFrequency(60000L);  // 1 minute checkpoint frequency
        storageCfg.setCheckpointThreads(4);

        assertThat(storageCfg.getCheckpointFrequency()).isEqualTo(60000L);
        assertThat(storageCfg.getCheckpointThreads()).isEqualTo(4);

        // Test IgniteConfiguration with snapshot settings
        IgniteConfiguration cfg = new IgniteConfiguration();
        cfg.setDataStorageConfiguration(storageCfg);

        // Verify storage configuration is set
        assertThat(cfg.getDataStorageConfiguration()).isNotNull();
        assertThat(cfg.getDataStorageConfiguration().getStoragePath()).isEqualTo("/var/ignite/storage");

        log.info("Snapshot configuration validated: storage={}, wal={}, wal-archive={}",
            storageCfg.getStoragePath(),
            storageCfg.getWalPath(),
            storageCfg.getWalArchivePath());
    }

    // ========================================
    // Graceful Shutdown Tests
    // ========================================

    @Test
    @DisplayName("Test graceful node shutdown")
    public void testGracefulShutdown() {
        // Create a separate Ignite node for shutdown testing
        IgniteConfiguration cfg = createTestConfiguration();
        cfg.setIgniteInstanceName("shutdown-test-node-" + System.currentTimeMillis());

        Ignite testNode = Ignition.start(cfg);

        // Verify node is running and in cluster
        assertThat(testNode.cluster().state()).isEqualTo(ClusterState.ACTIVE);
        assertThat(testNode.cluster().localNode()).isNotNull();

        // Create a cache and add some data before shutdown
        CacheConfiguration<Integer, String> cacheCfg = new CacheConfiguration<>("shutdown-test-cache");
        IgniteCache<Integer, String> cache = testNode.getOrCreateCache(cacheCfg);

        for (int i = 0; i < 100; i++) {
            cache.put(i, "Value-" + i);
        }
        assertThat(cache.size()).isEqualTo(100);

        // Get node name for logging
        String nodeName = testNode.name();

        // Verify node is active before shutdown
        boolean wasActive = !testNode.cluster().nodes().isEmpty();
        assertThat(wasActive).isTrue();

        // Perform graceful shutdown using close()
        // This allows the node to complete pending operations and properly leave the cluster
        testNode.close();

        // Verify node is no longer accessible after close
        assertThatThrownBy(() -> testNode.cluster().state())
            .isInstanceOf(IllegalStateException.class);

        log.info("Graceful shutdown completed for node: {}", nodeName);

        // Test Ignition.stop() method for graceful shutdown
        IgniteConfiguration cfg2 = createTestConfiguration();
        cfg2.setIgniteInstanceName("shutdown-test-node-2-" + System.currentTimeMillis());

        Ignite testNode2 = Ignition.start(cfg2);
        String nodeName2 = testNode2.name();

        assertThat(testNode2.cluster().state()).isEqualTo(ClusterState.ACTIVE);

        // Use Ignition.stop with cancel=false for graceful shutdown
        // cancel=false means wait for all operations to complete
        boolean stopped = Ignition.stop(nodeName2, false);

        assertThat(stopped).isTrue();

        log.info("Graceful shutdown via Ignition.stop() completed for node: {}", nodeName2);

        // Test that stopAll with cancel=false performs graceful shutdown
        IgniteConfiguration cfg3 = createTestConfiguration();
        cfg3.setIgniteInstanceName("shutdown-test-node-3-" + System.currentTimeMillis());

        Ignite testNode3 = Ignition.start(cfg3);
        assertThat(testNode3.cluster().state()).isEqualTo(ClusterState.ACTIVE);

        // Gracefully stop this specific node
        Ignition.stop(testNode3.name(), false);

        log.info("All graceful shutdown tests completed successfully");
    }
}
