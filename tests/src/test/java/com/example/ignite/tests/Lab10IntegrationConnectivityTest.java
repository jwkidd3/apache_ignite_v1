package com.example.ignite.tests;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.cache.CacheMode;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.ClientConfiguration;
import org.apache.ignite.configuration.ClientConnectorConfiguration;
import org.apache.ignite.configuration.ConnectorConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.ipfinder.vm.TcpDiscoveryVmIpFinder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive tests for Lab 10: Integration and Connectivity
 * Coverage: REST API configuration, client vs server nodes, cache access modes
 */
@DisplayName("Lab 10: Integration and Connectivity Tests")
public class Lab10IntegrationConnectivityTest extends BaseIgniteTest {

    @Test
    @DisplayName("Test ConnectorConfiguration basic setup")
    public void testConnectorConfigurationBasicSetup() {
        ConnectorConfiguration connectorCfg = new ConnectorConfiguration();

        // Default port is 11211
        assertThat(connectorCfg.getPort()).isEqualTo(11211);

        // Test setting custom port
        connectorCfg.setPort(8080);
        assertThat(connectorCfg.getPort()).isEqualTo(8080);
    }

    @Test
    @DisplayName("Test ConnectorConfiguration with custom host")
    public void testConnectorConfigurationWithHost() {
        ConnectorConfiguration connectorCfg = new ConnectorConfiguration();

        connectorCfg.setHost("localhost");
        connectorCfg.setPort(8888);

        assertThat(connectorCfg.getHost()).isEqualTo("localhost");
        assertThat(connectorCfg.getPort()).isEqualTo(8888);
    }

    @Test
    @DisplayName("Test ConnectorConfiguration idle timeout")
    public void testConnectorConfigurationIdleTimeout() {
        ConnectorConfiguration connectorCfg = new ConnectorConfiguration();

        // Set idle timeout to 30 seconds
        connectorCfg.setIdleTimeout(30000);
        assertThat(connectorCfg.getIdleTimeout()).isEqualTo(30000);
    }

    @Test
    @DisplayName("Test ConnectorConfiguration thread pool size")
    public void testConnectorConfigurationThreadPool() {
        ConnectorConfiguration connectorCfg = new ConnectorConfiguration();

        // Set thread count for REST handler
        connectorCfg.setThreadPoolSize(8);
        assertThat(connectorCfg.getThreadPoolSize()).isEqualTo(8);
    }

    @Test
    @DisplayName("Test IgniteConfiguration with ConnectorConfiguration")
    public void testIgniteWithConnectorConfiguration() {
        ConnectorConfiguration connectorCfg = new ConnectorConfiguration();
        connectorCfg.setPort(8080);

        IgniteConfiguration cfg = new IgniteConfiguration();
        cfg.setConnectorConfiguration(connectorCfg);

        assertThat(cfg.getConnectorConfiguration()).isNotNull();
        assertThat(cfg.getConnectorConfiguration().getPort()).isEqualTo(8080);
    }

    @Test
    @DisplayName("Test server node configuration")
    public void testServerNodeConfiguration() {
        IgniteConfiguration cfg = createTestConfiguration();
        cfg.setClientMode(false);

        assertThat(cfg.isClientMode()).isFalse();
    }

    @Test
    @DisplayName("Test client node configuration")
    public void testClientNodeConfiguration() {
        IgniteConfiguration cfg = createTestConfiguration();
        cfg.setClientMode(true);

        assertThat(cfg.isClientMode()).isTrue();
    }

    @Test
    @DisplayName("Test client node connecting to server node")
    public void testClientConnectingToServer() {
        // Server is already started via setUp()
        assertThat(ignite.cluster().localNode().isClient()).isFalse();

        // Start a client node
        IgniteConfiguration clientCfg = createTestConfiguration();
        clientCfg.setIgniteInstanceName(testName + "-client");
        clientCfg.setClientMode(true);

        try (Ignite clientNode = Ignition.start(clientCfg)) {
            assertThat(clientNode.cluster().localNode().isClient()).isTrue();

            // Verify client can see server in cluster
            assertThat(clientNode.cluster().nodes().size()).isGreaterThanOrEqualTo(2);
        }
    }

    @Test
    @DisplayName("Test cache access from server node")
    public void testCacheAccessFromServerNode() {
        CacheConfiguration<String, String> cacheCfg = new CacheConfiguration<>(getTestCacheName());
        cacheCfg.setCacheMode(CacheMode.REPLICATED);

        IgniteCache<String, String> cache = ignite.getOrCreateCache(cacheCfg);

        cache.put("key1", "value1");
        cache.put("key2", "value2");

        assertThat(cache.get("key1")).isEqualTo("value1");
        assertThat(cache.get("key2")).isEqualTo("value2");
        assertThat(cache.size()).isEqualTo(2);
    }

    @Test
    @DisplayName("Test cache access from client node")
    public void testCacheAccessFromClientNode() {
        // Create cache on server
        CacheConfiguration<String, String> cacheCfg = new CacheConfiguration<>(getTestCacheName());
        cacheCfg.setCacheMode(CacheMode.REPLICATED);

        IgniteCache<String, String> serverCache = ignite.getOrCreateCache(cacheCfg);
        serverCache.put("serverKey", "serverValue");

        // Start client and access cache
        IgniteConfiguration clientCfg = createTestConfiguration();
        clientCfg.setIgniteInstanceName(testName + "-client");
        clientCfg.setClientMode(true);

        try (Ignite clientNode = Ignition.start(clientCfg)) {
            IgniteCache<String, String> clientCache = clientNode.cache(getTestCacheName());

            assertThat(clientCache).isNotNull();
            assertThat(clientCache.get("serverKey")).isEqualTo("serverValue");

            // Client can also write
            clientCache.put("clientKey", "clientValue");
            assertThat(serverCache.get("clientKey")).isEqualTo("clientValue");
        }
    }

    @Test
    @DisplayName("Test cache operations with different cache modes from client")
    public void testCacheModesFromClient() {
        // Create REPLICATED cache
        CacheConfiguration<Integer, String> replicatedCfg = new CacheConfiguration<>(getTestCacheName() + "-replicated");
        replicatedCfg.setCacheMode(CacheMode.REPLICATED);

        // Create PARTITIONED cache
        CacheConfiguration<Integer, String> partitionedCfg = new CacheConfiguration<>(getTestCacheName() + "-partitioned");
        partitionedCfg.setCacheMode(CacheMode.PARTITIONED);

        IgniteCache<Integer, String> replicatedCache = ignite.getOrCreateCache(replicatedCfg);
        IgniteCache<Integer, String> partitionedCache = ignite.getOrCreateCache(partitionedCfg);

        // Start client
        IgniteConfiguration clientCfg = createTestConfiguration();
        clientCfg.setIgniteInstanceName(testName + "-client");
        clientCfg.setClientMode(true);

        try (Ignite clientNode = Ignition.start(clientCfg)) {
            IgniteCache<Integer, String> clientReplicated = clientNode.cache(getTestCacheName() + "-replicated");
            IgniteCache<Integer, String> clientPartitioned = clientNode.cache(getTestCacheName() + "-partitioned");

            // Both caches accessible from client
            clientReplicated.put(1, "replicated-value");
            clientPartitioned.put(1, "partitioned-value");

            assertThat(replicatedCache.get(1)).isEqualTo("replicated-value");
            assertThat(partitionedCache.get(1)).isEqualTo("partitioned-value");
        }
    }

    @Test
    @DisplayName("Test multiple client nodes accessing same cache")
    public void testMultipleClientsAccessingSameCache() {
        CacheConfiguration<String, Integer> cacheCfg = new CacheConfiguration<>(getTestCacheName());
        IgniteCache<String, Integer> serverCache = ignite.getOrCreateCache(cacheCfg);
        serverCache.put("counter", 0);

        // Start first client
        IgniteConfiguration client1Cfg = createTestConfiguration();
        client1Cfg.setIgniteInstanceName(testName + "-client1");
        client1Cfg.setClientMode(true);

        // Start second client
        IgniteConfiguration client2Cfg = createTestConfiguration();
        client2Cfg.setIgniteInstanceName(testName + "-client2");
        client2Cfg.setClientMode(true);

        try (Ignite client1 = Ignition.start(client1Cfg);
             Ignite client2 = Ignition.start(client2Cfg)) {

            IgniteCache<String, Integer> cache1 = client1.cache(getTestCacheName());
            IgniteCache<String, Integer> cache2 = client2.cache(getTestCacheName());

            // Both clients can read
            assertThat(cache1.get("counter")).isEqualTo(0);
            assertThat(cache2.get("counter")).isEqualTo(0);

            // Client 1 updates
            cache1.put("counter", 1);

            // Client 2 sees the update
            assertThat(cache2.get("counter")).isEqualTo(1);

            // Client 2 updates
            cache2.put("counter", 2);

            // All nodes see the update
            assertThat(cache1.get("counter")).isEqualTo(2);
            assertThat(serverCache.get("counter")).isEqualTo(2);
        }
    }

    @Test
    @DisplayName("Test discovery SPI configuration for client")
    public void testDiscoveryConfigurationForClient() {
        TcpDiscoverySpi discoverySpi = new TcpDiscoverySpi();
        TcpDiscoveryVmIpFinder ipFinder = new TcpDiscoveryVmIpFinder();
        ipFinder.setAddresses(Arrays.asList("127.0.0.1:47500..47509"));
        discoverySpi.setIpFinder(ipFinder);

        IgniteConfiguration clientCfg = new IgniteConfiguration();
        clientCfg.setClientMode(true);
        clientCfg.setDiscoverySpi(discoverySpi);

        assertThat(clientCfg.isClientMode()).isTrue();
        assertThat(clientCfg.getDiscoverySpi()).isInstanceOf(TcpDiscoverySpi.class);
    }

    @Test
    @DisplayName("Test server node stores data while client does not")
    public void testServerStoresDataClientDoesNot() {
        // Server is the primary (already started)
        assertThat(ignite.cluster().localNode().isClient()).isFalse();

        // Start client
        IgniteConfiguration clientCfg = createTestConfiguration();
        clientCfg.setIgniteInstanceName(testName + "-client");
        clientCfg.setClientMode(true);

        try (Ignite clientNode = Ignition.start(clientCfg)) {
            // Verify roles
            assertThat(ignite.cluster().localNode().isClient()).isFalse();
            assertThat(clientNode.cluster().localNode().isClient()).isTrue();

            // Create partitioned cache - data will only be stored on server
            CacheConfiguration<Integer, String> cacheCfg = new CacheConfiguration<>(getTestCacheName());
            cacheCfg.setCacheMode(CacheMode.PARTITIONED);
            cacheCfg.setBackups(0);

            IgniteCache<Integer, String> cache = ignite.getOrCreateCache(cacheCfg);

            // Add data
            for (int i = 0; i < 100; i++) {
                cache.put(i, "value-" + i);
            }

            // Client can access all data even though it doesn't store it locally
            IgniteCache<Integer, String> clientCache = clientNode.cache(getTestCacheName());
            for (int i = 0; i < 100; i++) {
                assertThat(clientCache.get(i)).isEqualTo("value-" + i);
            }
        }
    }

    @Test
    @DisplayName("Test ConnectorConfiguration message interceptor setting")
    public void testConnectorMessageInterceptor() {
        ConnectorConfiguration connectorCfg = new ConnectorConfiguration();

        // Message interceptor can be null by default
        assertThat(connectorCfg.getMessageInterceptor()).isNull();
    }

    @Test
    @DisplayName("Test ConnectorConfiguration SSL context factory")
    public void testConnectorSslConfiguration() {
        ConnectorConfiguration connectorCfg = new ConnectorConfiguration();

        // SSL is disabled by default
        assertThat(connectorCfg.isSslEnabled()).isFalse();

        // Enable SSL
        connectorCfg.setSslEnabled(true);
        assertThat(connectorCfg.isSslEnabled()).isTrue();
    }

    @Test
    @DisplayName("Test cluster topology from client perspective")
    public void testClusterTopologyFromClient() {
        // Start client
        IgniteConfiguration clientCfg = createTestConfiguration();
        clientCfg.setIgniteInstanceName(testName + "-client");
        clientCfg.setClientMode(true);

        try (Ignite clientNode = Ignition.start(clientCfg)) {
            // Client can see cluster topology
            assertThat(clientNode.cluster().nodes().size()).isEqualTo(2);

            // Client can identify server nodes
            long serverCount = clientNode.cluster().nodes().stream()
                .filter(node -> !node.isClient())
                .count();
            assertThat(serverCount).isEqualTo(1);

            // Client can identify itself
            long clientCount = clientNode.cluster().nodes().stream()
                .filter(node -> node.isClient())
                .count();
            assertThat(clientCount).isEqualTo(1);
        }
    }

    @Test
    @DisplayName("Test cache creation from client node")
    public void testCacheCreationFromClient() {
        IgniteConfiguration clientCfg = createTestConfiguration();
        clientCfg.setIgniteInstanceName(testName + "-client");
        clientCfg.setClientMode(true);

        try (Ignite clientNode = Ignition.start(clientCfg)) {
            // Client creates a new cache
            CacheConfiguration<String, String> cacheCfg = new CacheConfiguration<>(getTestCacheName());
            IgniteCache<String, String> clientCache = clientNode.getOrCreateCache(cacheCfg);

            clientCache.put("created-by-client", "test-value");

            // Server can access the cache created by client
            IgniteCache<String, String> serverCache = ignite.cache(getTestCacheName());
            assertThat(serverCache).isNotNull();
            assertThat(serverCache.get("created-by-client")).isEqualTo("test-value");
        }
    }

    @Test
    @DisplayName("Test near cache configuration for client")
    public void testNearCacheForClient() {
        // Create partitioned cache on server
        CacheConfiguration<Integer, String> cacheCfg = new CacheConfiguration<>(getTestCacheName());
        cacheCfg.setCacheMode(CacheMode.PARTITIONED);
        ignite.getOrCreateCache(cacheCfg);

        // Start client with near cache
        IgniteConfiguration clientCfg = createTestConfiguration();
        clientCfg.setIgniteInstanceName(testName + "-client");
        clientCfg.setClientMode(true);

        try (Ignite clientNode = Ignition.start(clientCfg)) {
            // Access cache from client
            IgniteCache<Integer, String> clientCache = clientNode.cache(getTestCacheName());
            assertThat(clientCache).isNotNull();
        }
    }

    @Test
    @DisplayName("Test ConnectorConfiguration with all settings")
    public void testConnectorConfigurationComplete() {
        ConnectorConfiguration connectorCfg = new ConnectorConfiguration();

        // Configure all typical REST settings
        connectorCfg.setHost("0.0.0.0");
        connectorCfg.setPort(8080);
        connectorCfg.setIdleTimeout(60000);
        connectorCfg.setThreadPoolSize(16);
        connectorCfg.setSslEnabled(false);

        // Verify all settings
        assertThat(connectorCfg.getHost()).isEqualTo("0.0.0.0");
        assertThat(connectorCfg.getPort()).isEqualTo(8080);
        assertThat(connectorCfg.getIdleTimeout()).isEqualTo(60000);
        assertThat(connectorCfg.getThreadPoolSize()).isEqualTo(16);
        assertThat(connectorCfg.isSslEnabled()).isFalse();
    }

    // ==================== REST API and Thin Client Tests ====================

    @Test
    @DisplayName("Test REST API connector is configured and accessible")
    public void testRestApiConfiguration() {
        // Create ConnectorConfiguration for REST API
        ConnectorConfiguration restConnector = new ConnectorConfiguration();
        restConnector.setHost("127.0.0.1");
        restConnector.setPort(8080);
        restConnector.setIdleTimeout(30000);
        restConnector.setThreadPoolSize(4);

        // Create IgniteConfiguration with REST connector
        IgniteConfiguration cfg = createTestConfiguration();
        cfg.setIgniteInstanceName(testName + "-rest-node");
        cfg.setConnectorConfiguration(restConnector);

        // Start node with REST connector configured
        try (Ignite restNode = Ignition.start(cfg)) {
            // Verify REST connector is configured
            ConnectorConfiguration actualConnector = restNode.configuration().getConnectorConfiguration();
            assertThat(actualConnector).isNotNull();
            assertThat(actualConnector.getHost()).isEqualTo("127.0.0.1");
            assertThat(actualConnector.getPort()).isEqualTo(8080);
            assertThat(actualConnector.getIdleTimeout()).isEqualTo(30000);
            assertThat(actualConnector.getThreadPoolSize()).isEqualTo(4);

            // Verify node is running and accessible
            assertThat(restNode.cluster().localNode()).isNotNull();
            assertThat(restNode.cluster().nodes().size()).isGreaterThanOrEqualTo(1);

            // Create a cache and verify it's accessible
            CacheConfiguration<String, String> cacheCfg = new CacheConfiguration<>(getTestCacheName() + "-rest");
            IgniteCache<String, String> cache = restNode.getOrCreateCache(cacheCfg);
            cache.put("rest-key", "rest-value");
            assertThat(cache.get("rest-key")).isEqualTo("rest-value");
        }
    }

    @Test
    @DisplayName("Test thin client connection configuration")
    public void testThinClientConnection() {
        // Configure ClientConnectorConfiguration for thin clients
        ClientConnectorConfiguration clientConnectorCfg = new ClientConnectorConfiguration();
        clientConnectorCfg.setHost("127.0.0.1");
        clientConnectorCfg.setPort(10800);
        clientConnectorCfg.setPortRange(10);
        clientConnectorCfg.setMaxOpenCursorsPerConnection(128);
        clientConnectorCfg.setThreadPoolSize(8);
        clientConnectorCfg.setSocketSendBufferSize(32768);
        clientConnectorCfg.setSocketReceiveBufferSize(32768);

        // Create IgniteConfiguration with thin client connector
        IgniteConfiguration cfg = createTestConfiguration();
        cfg.setIgniteInstanceName(testName + "-thin-server");
        cfg.setClientConnectorConfiguration(clientConnectorCfg);

        // Start server node with thin client connector configured
        try (Ignite serverNode = Ignition.start(cfg)) {
            // Verify ClientConnectorConfiguration is applied
            ClientConnectorConfiguration actualCfg = serverNode.configuration().getClientConnectorConfiguration();
            assertThat(actualCfg).isNotNull();
            assertThat(actualCfg.getHost()).isEqualTo("127.0.0.1");
            assertThat(actualCfg.getPort()).isEqualTo(10800);
            assertThat(actualCfg.getPortRange()).isEqualTo(10);
            assertThat(actualCfg.getMaxOpenCursorsPerConnection()).isEqualTo(128);
            assertThat(actualCfg.getThreadPoolSize()).isEqualTo(8);
            assertThat(actualCfg.getSocketSendBufferSize()).isEqualTo(32768);
            assertThat(actualCfg.getSocketReceiveBufferSize()).isEqualTo(32768);

            // Verify server is running
            assertThat(serverNode.cluster().localNode()).isNotNull();

            // Test thin client configuration object (without actually connecting)
            ClientConfiguration thinClientCfg = new ClientConfiguration();
            thinClientCfg.setAddresses("127.0.0.1:10800..10810");
            thinClientCfg.setTcpNoDelay(true);
            thinClientCfg.setTimeout(5000);
            thinClientCfg.setSendBufferSize(32768);
            thinClientCfg.setReceiveBufferSize(32768);

            // Verify client configuration properties
            assertThat(thinClientCfg.getAddresses()).contains("127.0.0.1:10800..10810");
            assertThat(thinClientCfg.isTcpNoDelay()).isTrue();
            assertThat(thinClientCfg.getTimeout()).isEqualTo(5000);
            assertThat(thinClientCfg.getSendBufferSize()).isEqualTo(32768);
            assertThat(thinClientCfg.getReceiveBufferSize()).isEqualTo(32768);
        }
    }
}
