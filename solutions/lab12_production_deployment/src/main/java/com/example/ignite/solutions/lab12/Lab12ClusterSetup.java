package com.example.ignite.solutions.lab12;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.cluster.ClusterState;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.DataRegionConfiguration;
import org.apache.ignite.configuration.DataStorageConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.spi.communication.tcp.TcpCommunicationSpi;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.ipfinder.vm.TcpDiscoveryVmIpFinder;

import java.util.Arrays;

/**
 * Lab 12 Exercise 2: Production Cluster Setup
 *
 * Demonstrates:
 * - Production-ready Ignite configuration
 * - Data persistence setup
 * - Cluster initialization and activation
 * - Baseline topology management
 */
public class Lab12ClusterSetup {

    public static void main(String[] args) {
        System.out.println("=== Production Cluster Setup ===\n");

        IgniteConfiguration cfg = createProductionConfig();

        try (Ignite ignite = Ignition.start(cfg)) {
            System.out.println("Node started: " + ignite.name());

            // Activate cluster for persistence
            if (ignite.cluster().state() != ClusterState.ACTIVE) {
                System.out.println("Activating cluster...");
                ignite.cluster().state(ClusterState.ACTIVE);
            }

            // Print cluster info
            System.out.println("\n=== Cluster Information ===");
            System.out.println("Cluster state: " + ignite.cluster().state());
            System.out.println("Total nodes: " + ignite.cluster().nodes().size());
            System.out.println("Baseline topology: " +
                ignite.cluster().currentBaselineTopology());

            // Create production cache
            CacheConfiguration<Long, String> cacheCfg = new CacheConfiguration<>("production-cache");
            cacheCfg.setBackups(2);
            cacheCfg.setStatisticsEnabled(true);

            IgniteCache<Long, String> cache = ignite.getOrCreateCache(cacheCfg);

            // Test operations
            cache.put(1L, "Production data");
            System.out.println("\nCache created and tested successfully");
            System.out.println("Cache size: " + cache.size());

            // Print production configuration guidelines
            printProductionGuidelines();

            System.out.println("\nPress Enter to exit...");
            System.in.read();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static IgniteConfiguration createProductionConfig() {
        IgniteConfiguration cfg = new IgniteConfiguration();
        cfg.setIgniteInstanceName("production-node");

        // Data storage with persistence
        DataStorageConfiguration storageCfg = new DataStorageConfiguration();

        // Configure default data region
        DataRegionConfiguration regionCfg = new DataRegionConfiguration();
        regionCfg.setName("production-region");
        regionCfg.setPersistenceEnabled(true);
        regionCfg.setMaxSize(2L * 1024 * 1024 * 1024); // 2GB
        regionCfg.setMetricsEnabled(true);
        storageCfg.setDefaultDataRegionConfiguration(regionCfg);

        // WAL configuration for durability
        storageCfg.setWalMode(org.apache.ignite.configuration.WALMode.FSYNC);
        storageCfg.setWalSegmentSize(256 * 1024 * 1024); // 256MB

        cfg.setDataStorageConfiguration(storageCfg);

        // Discovery configuration
        TcpDiscoverySpi discoverySpi = new TcpDiscoverySpi();
        discoverySpi.setLocalPort(47500);
        discoverySpi.setLocalPortRange(10);

        TcpDiscoveryVmIpFinder ipFinder = new TcpDiscoveryVmIpFinder();
        ipFinder.setAddresses(Arrays.asList("127.0.0.1:47500..47509"));
        discoverySpi.setIpFinder(ipFinder);
        cfg.setDiscoverySpi(discoverySpi);

        // Communication configuration
        TcpCommunicationSpi commSpi = new TcpCommunicationSpi();
        commSpi.setLocalPort(47100);
        commSpi.setMessageQueueLimit(1024);
        commSpi.setSocketWriteTimeout(5000);
        cfg.setCommunicationSpi(commSpi);

        // Failure detection
        cfg.setFailureDetectionTimeout(10000);
        cfg.setClientFailureDetectionTimeout(30000);

        // Metrics
        cfg.setMetricsLogFrequency(60000);

        return cfg;
    }

    private static void printProductionGuidelines() {
        System.out.println("\n=== Production Configuration Guidelines ===");

        System.out.println("\n1. Memory Configuration:");
        System.out.println("   - Set Xms = Xmx (e.g., -Xms4g -Xmx4g)");
        System.out.println("   - Configure MaxDirectMemorySize for off-heap");
        System.out.println("   - Size data regions based on expected data volume");

        System.out.println("\n2. Persistence Configuration:");
        System.out.println("   - Enable persistence for durability");
        System.out.println("   - Use FSYNC WAL mode for production");
        System.out.println("   - Configure separate WAL archive path");
        System.out.println("   - Plan for WAL segment rotation");

        System.out.println("\n3. Cluster Configuration:");
        System.out.println("   - Minimum 3 nodes for production");
        System.out.println("   - Configure appropriate backup count");
        System.out.println("   - Enable baseline auto-adjustment carefully");
        System.out.println("   - Plan for rolling upgrades");

        System.out.println("\n4. Network Configuration:");
        System.out.println("   - Use static IP addresses");
        System.out.println("   - Configure appropriate timeouts");
        System.out.println("   - Enable SSL/TLS for secure communication");
        System.out.println("   - Consider network bandwidth requirements");

        System.out.println("\n5. Monitoring:");
        System.out.println("   - Enable cache statistics");
        System.out.println("   - Configure metrics log frequency");
        System.out.println("   - Set up JMX monitoring");
        System.out.println("   - Implement alerting for critical metrics");
    }
}
