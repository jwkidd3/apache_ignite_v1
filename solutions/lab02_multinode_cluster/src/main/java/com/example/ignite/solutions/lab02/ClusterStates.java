package com.example.ignite.solutions.lab02;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.cache.CacheMode;
import org.apache.ignite.cluster.ClusterState;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.DataRegionConfiguration;
import org.apache.ignite.configuration.DataStorageConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.ipfinder.vm.TcpDiscoveryVmIpFinder;

import java.util.Arrays;

/**
 * Lab 2 Exercise 6: Cluster State Management
 *
 * This exercise demonstrates managing cluster states:
 * ACTIVE, ACTIVE_READ_ONLY, and INACTIVE.
 */
public class ClusterStates {

    public static void main(String[] args) {
        IgniteConfiguration cfg = createConfiguration("cluster-state-node");

        try (Ignite ignite = Ignition.start(cfg)) {
            System.out.println("\n=== Cluster State Management Demo ===");
            System.out.println("Initial cluster state: " + ignite.cluster().state());

            // Activate the cluster
            System.out.println("\n--- Setting to ACTIVE state ---");
            ignite.cluster().state(ClusterState.ACTIVE);
            System.out.println("State: " + ignite.cluster().state());

            // Create a test cache while active
            CacheConfiguration<Integer, String> cacheCfg = new CacheConfiguration<>("stateTestCache");
            cacheCfg.setCacheMode(CacheMode.REPLICATED);
            IgniteCache<Integer, String> cache = ignite.getOrCreateCache(cacheCfg);
            cache.put(1, "test-value");
            System.out.println("Created cache and inserted data in ACTIVE state");

            // Switch to ACTIVE_READ_ONLY
            System.out.println("\n--- Setting to ACTIVE_READ_ONLY state ---");
            ignite.cluster().state(ClusterState.ACTIVE_READ_ONLY);
            System.out.println("State: " + ignite.cluster().state());

            // Try to write (will fail)
            System.out.println("\nAttempting write in ACTIVE_READ_ONLY state...");
            try {
                cache.put(2, "another-value");
                System.out.println("ERROR: Write succeeded (unexpected)");
            } catch (Exception e) {
                System.out.println("Write blocked (expected): " + e.getClass().getSimpleName());
            }

            // Read still works
            String value = cache.get(1);
            System.out.println("Read succeeded: key=1, value=" + value);

            // Return to ACTIVE
            System.out.println("\n--- Returning to ACTIVE state ---");
            ignite.cluster().state(ClusterState.ACTIVE);
            System.out.println("State: " + ignite.cluster().state());

            // Now write should work
            cache.put(2, "another-value");
            System.out.println("Write succeeded after returning to ACTIVE state");
            System.out.println("Key 2 value: " + cache.get(2));

            System.out.println("\n=== State Summary ===");
            System.out.println("ACTIVE: Full read/write access");
            System.out.println("ACTIVE_READ_ONLY: Reads allowed, writes blocked");
            System.out.println("INACTIVE: No cache access (for maintenance)");

            System.out.println("\nPress Enter to stop...");
            System.in.read();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static IgniteConfiguration createConfiguration(String nodeName) {
        IgniteConfiguration cfg = new IgniteConfiguration();
        cfg.setIgniteInstanceName(nodeName);

        DataStorageConfiguration storageCfg = new DataStorageConfiguration();
        DataRegionConfiguration defaultRegion = new DataRegionConfiguration();
        defaultRegion.setPersistenceEnabled(true);
        defaultRegion.setMaxSize(100L * 1024 * 1024);
        storageCfg.setDefaultDataRegionConfiguration(defaultRegion);
        storageCfg.setStoragePath("./ignite-data/cluster-state");
        storageCfg.setWalPath("./ignite-wal/cluster-state");
        storageCfg.setWalArchivePath("./ignite-wal-archive/cluster-state");
        cfg.setDataStorageConfiguration(storageCfg);

        TcpDiscoverySpi discoverySpi = new TcpDiscoverySpi();
        TcpDiscoveryVmIpFinder ipFinder = new TcpDiscoveryVmIpFinder();
        ipFinder.setAddresses(Arrays.asList("127.0.0.1:47500..47509"));
        discoverySpi.setIpFinder(ipFinder);
        cfg.setDiscoverySpi(discoverySpi);

        return cfg;
    }
}
