package com.example.ignite.solutions.lab05;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.cluster.ClusterState;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.DataRegionConfiguration;
import org.apache.ignite.configuration.DataStorageConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.configuration.WALMode;

/**
 * Lab 5 Exercise 3: Native Persistence
 *
 * This exercise demonstrates configuring and using
 * Ignite's native persistence layer.
 */
public class Persistence {

    public static void main(String[] args) {
        System.out.println("=== Native Persistence Lab ===\n");

        IgniteConfiguration cfg = new IgniteConfiguration();
        cfg.setIgniteInstanceName("persistent-node");

        // Configure persistence
        DataStorageConfiguration storageCfg = new DataStorageConfiguration();

        // Persistent data region
        DataRegionConfiguration persistentRegion = new DataRegionConfiguration();
        persistentRegion.setName("Persistent_Region");
        persistentRegion.setPersistenceEnabled(true);
        persistentRegion.setInitialSize(100L * 1024 * 1024);  // 100 MB
        persistentRegion.setMaxSize(500L * 1024 * 1024);      // 500 MB
        persistentRegion.setMetricsEnabled(true);

        storageCfg.setDefaultDataRegionConfiguration(persistentRegion);

        // Configure WAL
        storageCfg.setWalMode(WALMode.LOG_ONLY);
        storageCfg.setWalSegmentSize(64 * 1024 * 1024);  // 64 MB

        // Storage paths
        storageCfg.setStoragePath("./ignite-data");
        storageCfg.setWalPath("./ignite-wal");
        storageCfg.setWalArchivePath("./ignite-wal-archive");

        cfg.setDataStorageConfiguration(storageCfg);

        try (Ignite ignite = Ignition.start(cfg)) {
            // Activate cluster (required for persistent clusters)
            if (ignite.cluster().state() != ClusterState.ACTIVE) {
                System.out.println("Activating cluster...");
                ignite.cluster().state(ClusterState.ACTIVE);
            }

            System.out.println("Cluster state: " + ignite.cluster().state());

            // Create persistent cache
            CacheConfiguration<Integer, String> cacheCfg =
                new CacheConfiguration<>("persistentCache");
            cacheCfg.setDataRegionName("Persistent_Region");

            IgniteCache<Integer, String> cache =
                ignite.getOrCreateCache(cacheCfg);

            // Check if data already exists (from previous run)
            if (cache.size() > 0) {
                System.out.println("\nData recovered from disk!");
                System.out.println("Cache size: " + cache.size());
                System.out.println("Sample data:");
                for (int i = 1; i <= 5; i++) {
                    System.out.println("  Key " + i + ": " + cache.get(i));
                }
            } else {
                System.out.println("\nNo existing data. Creating new data...");

                // Add data
                for (int i = 1; i <= 100; i++) {
                    cache.put(i, "Persistent-Value-" + i);
                }

                System.out.println("Added 100 entries to persistent cache");
                System.out.println("\nRestart the application to see data recovery!");
            }

            // Display WAL information
            System.out.println("\n=== WAL Configuration ===");
            System.out.println("WAL Mode: " + storageCfg.getWalMode());
            System.out.println("WAL Segment Size: " + storageCfg.getWalSegmentSize() / (1024 * 1024) + " MB");
            System.out.println("WAL Path: " + storageCfg.getWalPath());

            System.out.println("\n=== Persistence Features ===");
            System.out.println("- Data survives node restarts");
            System.out.println("- Write-Ahead Logging (WAL) for durability");
            System.out.println("- Checkpointing for crash recovery");
            System.out.println("- Native disk storage (faster than database)");

            System.out.println("\nPress Enter to exit...");
            System.in.read();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
