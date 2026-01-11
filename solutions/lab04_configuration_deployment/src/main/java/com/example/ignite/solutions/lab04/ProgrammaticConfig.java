package com.example.ignite.solutions.lab04;

import org.apache.ignite.Ignite;
import org.apache.ignite.Ignition;
import org.apache.ignite.cache.CacheAtomicityMode;
import org.apache.ignite.cache.CacheMode;
import org.apache.ignite.cache.CacheWriteSynchronizationMode;
import org.apache.ignite.configuration.*;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.ipfinder.vm.TcpDiscoveryVmIpFinder;

import java.util.Arrays;

/**
 * Lab 4 Exercise 3: Advanced Programmatic Configuration
 *
 * This exercise demonstrates comprehensive programmatic
 * configuration of Ignite.
 */
public class ProgrammaticConfig {

    public static void main(String[] args) {
        System.out.println("=== Programmatic Configuration Lab ===\n");

        IgniteConfiguration cfg = createIgniteConfiguration();

        try (Ignite ignite = Ignition.start(cfg)) {
            System.out.println("Node started with programmatic configuration");
            displayConfiguration(ignite);

            System.out.println("\nPress Enter to exit...");
            System.in.read();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static IgniteConfiguration createIgniteConfiguration() {
        IgniteConfiguration cfg = new IgniteConfiguration();

        // Basic settings
        cfg.setIgniteInstanceName("programmatic-node");
        cfg.setClientMode(false);
        cfg.setPeerClassLoadingEnabled(true);

        // Discovery configuration
        cfg.setDiscoverySpi(createDiscoverySpi());

        // Data storage configuration
        cfg.setDataStorageConfiguration(createDataStorageConfig());

        // Cache configurations
        cfg.setCacheConfiguration(
            createPartitionedCache(),
            createReplicatedCache(),
            createTransactionalCache()
        );

        // Thread pool configuration
        cfg.setPublicThreadPoolSize(8);
        cfg.setSystemThreadPoolSize(8);

        // Network configuration
        cfg.setNetworkTimeout(5000);
        cfg.setFailureDetectionTimeout(10000);

        return cfg;
    }

    private static TcpDiscoverySpi createDiscoverySpi() {
        TcpDiscoverySpi spi = new TcpDiscoverySpi();

        TcpDiscoveryVmIpFinder ipFinder = new TcpDiscoveryVmIpFinder();
        ipFinder.setAddresses(Arrays.asList("127.0.0.1:47500..47509"));

        spi.setIpFinder(ipFinder);
        spi.setLocalPort(47500);
        spi.setLocalPortRange(10);

        return spi;
    }

    private static DataStorageConfiguration createDataStorageConfig() {
        DataStorageConfiguration storageCfg = new DataStorageConfiguration();

        // Default data region
        DataRegionConfiguration defaultRegion = new DataRegionConfiguration();
        defaultRegion.setName("Default_Region");
        defaultRegion.setInitialSize(100L * 1024 * 1024);  // 100 MB
        defaultRegion.setMaxSize(500L * 1024 * 1024);      // 500 MB
        defaultRegion.setPageEvictionMode(DataPageEvictionMode.RANDOM_2_LRU);
        defaultRegion.setMetricsEnabled(true);

        storageCfg.setDefaultDataRegionConfiguration(defaultRegion);

        // Additional data region for persistent data
        DataRegionConfiguration persistentRegion = new DataRegionConfiguration();
        persistentRegion.setName("Persistent_Region");
        persistentRegion.setInitialSize(50L * 1024 * 1024);
        persistentRegion.setMaxSize(200L * 1024 * 1024);
        persistentRegion.setPersistenceEnabled(true);

        storageCfg.setDataRegionConfigurations(persistentRegion);

        return storageCfg;
    }

    private static CacheConfiguration<Integer, String> createPartitionedCache() {
        CacheConfiguration<Integer, String> cacheCfg =
            new CacheConfiguration<>("programmaticPartitioned");

        cacheCfg.setCacheMode(CacheMode.PARTITIONED);
        cacheCfg.setBackups(1);
        cacheCfg.setAtomicityMode(CacheAtomicityMode.ATOMIC);
        cacheCfg.setWriteSynchronizationMode(
            CacheWriteSynchronizationMode.PRIMARY_SYNC);
        cacheCfg.setStatisticsEnabled(true);

        return cacheCfg;
    }

    private static CacheConfiguration<Integer, String> createReplicatedCache() {
        CacheConfiguration<Integer, String> cacheCfg =
            new CacheConfiguration<>("programmaticReplicated");

        cacheCfg.setCacheMode(CacheMode.REPLICATED);
        cacheCfg.setAtomicityMode(CacheAtomicityMode.ATOMIC);
        cacheCfg.setStatisticsEnabled(true);

        return cacheCfg;
    }

    private static CacheConfiguration<Integer, String> createTransactionalCache() {
        CacheConfiguration<Integer, String> cacheCfg =
            new CacheConfiguration<>("transactionalCache");

        cacheCfg.setCacheMode(CacheMode.PARTITIONED);
        cacheCfg.setBackups(1);
        cacheCfg.setAtomicityMode(CacheAtomicityMode.TRANSACTIONAL);
        cacheCfg.setStatisticsEnabled(true);

        return cacheCfg;
    }

    private static void displayConfiguration(Ignite ignite) {
        System.out.println("\n=== Node Configuration ===");
        System.out.println("Instance name: " + ignite.name());
        System.out.println("Cluster nodes: " + ignite.cluster().nodes().size());

        System.out.println("\n=== Configured Caches ===");
        ignite.cacheNames().forEach(name -> {
            System.out.println("  - " + name);
        });

        System.out.println("\n=== Data Regions ===");
        ignite.dataRegionMetrics().forEach(metrics -> {
            System.out.println("  Region: " + metrics.getName());
            System.out.println("    Total allocated: " +
                metrics.getTotalAllocatedSize() / (1024 * 1024) + " MB");
        });
    }
}
