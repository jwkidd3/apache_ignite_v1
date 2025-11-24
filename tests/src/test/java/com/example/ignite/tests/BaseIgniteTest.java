package com.example.ignite.tests;

import org.apache.ignite.Ignite;
import org.apache.ignite.Ignition;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.logger.slf4j.Slf4jLogger;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.ipfinder.vm.TcpDiscoveryVmIpFinder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.UUID;

/**
 * Base test class for all Ignite tests.
 * Provides common setup, teardown, and utility methods.
 */
public abstract class BaseIgniteTest {

    protected static final Logger log = LoggerFactory.getLogger(BaseIgniteTest.class);

    protected Ignite ignite;
    protected String testName;

    @BeforeEach
    public void setUp() {
        testName = getClass().getSimpleName() + "-" + UUID.randomUUID().toString().substring(0, 8);
        log.info("Starting test: {}", testName);
        ignite = startIgnite();
    }

    @AfterEach
    public void tearDown() {
        if (ignite != null) {
            log.info("Stopping Ignite for test: {}", testName);
            ignite.close();
        }
        Ignition.stopAll(true);
    }

    /**
     * Start Ignite node with test configuration
     */
    protected Ignite startIgnite() {
        IgniteConfiguration cfg = createTestConfiguration();
        return Ignition.start(cfg);
    }

    /**
     * Start additional Ignite node
     */
    protected Ignite startAdditionalNode(String nodeName) {
        IgniteConfiguration cfg = createTestConfiguration();
        cfg.setIgniteInstanceName(nodeName);
        return Ignition.start(cfg);
    }

    /**
     * Create test configuration
     */
    protected IgniteConfiguration createTestConfiguration() {
        IgniteConfiguration cfg = new IgniteConfiguration();
        cfg.setIgniteInstanceName(testName);
        cfg.setClientMode(false);

        // Use SLF4J logger
        cfg.setGridLogger(new Slf4jLogger());

        // Configure discovery
        TcpDiscoverySpi discoverySpi = new TcpDiscoverySpi();
        TcpDiscoveryVmIpFinder ipFinder = new TcpDiscoveryVmIpFinder();
        ipFinder.setAddresses(Arrays.asList("127.0.0.1:47500..47509"));
        discoverySpi.setIpFinder(ipFinder);
        cfg.setDiscoverySpi(discoverySpi);

        // Optimize for testing
        cfg.setMetricsLogFrequency(0);
        cfg.setPeerClassLoadingEnabled(false);

        return cfg;
    }

    /**
     * Wait for cluster to stabilize
     */
    protected void waitForCluster(int expectedNodes) {
        int attempts = 0;
        while (ignite.cluster().nodes().size() < expectedNodes && attempts < 30) {
            try {
                Thread.sleep(1000);
                attempts++;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted while waiting for cluster", e);
            }
        }

        if (ignite.cluster().nodes().size() < expectedNodes) {
            throw new RuntimeException("Cluster did not reach expected size: " + expectedNodes);
        }
    }

    /**
     * Get test cache name
     */
    protected String getTestCacheName() {
        return testName + "-cache";
    }
}
