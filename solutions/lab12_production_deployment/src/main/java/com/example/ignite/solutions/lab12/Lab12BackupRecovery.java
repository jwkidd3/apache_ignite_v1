package com.example.ignite.solutions.lab12;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.IgniteSnapshot;
import org.apache.ignite.Ignition;
import org.apache.ignite.cluster.ClusterState;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.DataRegionConfiguration;
import org.apache.ignite.configuration.DataStorageConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.ipfinder.vm.TcpDiscoveryVmIpFinder;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

/**
 * Lab 12 Optional Exercise: Backup and Recovery
 *
 * Demonstrates:
 * - Snapshot creation
 * - Snapshot restoration
 * - WAL configuration for recovery
 * - Recovery procedures
 */
public class Lab12BackupRecovery {

    public static void main(String[] args) {
        System.out.println("=== Backup and Recovery Lab ===\n");

        IgniteConfiguration cfg = createPersistentConfig();

        try (Ignite ignite = Ignition.start(cfg)) {
            // Activate cluster
            if (ignite.cluster().state() != ClusterState.ACTIVE) {
                ignite.cluster().state(ClusterState.ACTIVE);
            }

            System.out.println("Cluster activated\n");

            // Create test cache with data
            CacheConfiguration<Long, String> cacheCfg = new CacheConfiguration<>("backup-test-cache");
            cacheCfg.setBackups(1);
            IgniteCache<Long, String> cache = ignite.getOrCreateCache(cacheCfg);

            // Populate test data
            System.out.println("Populating test data...");
            for (long i = 1; i <= 1000; i++) {
                cache.put(i, "Value-" + i);
            }
            System.out.println("Inserted 1000 records\n");

            // Demonstrate snapshot operations
            IgniteSnapshot snapshotApi = ignite.snapshot();

            String snapshotName = "backup_" +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));

            System.out.println("=== Creating Snapshot ===");
            System.out.println("Snapshot name: " + snapshotName);

            try {
                // Create snapshot
                snapshotApi.createSnapshot(snapshotName).get();
                System.out.println("Snapshot created successfully!\n");
            } catch (Exception e) {
                System.out.println("Snapshot creation: " + e.getMessage());
                System.out.println("(This is expected in single-node development mode)\n");
            }

            // Print snapshot commands
            printSnapshotCommands();

            // Print recovery procedures
            printRecoveryProcedures();

            System.out.println("\nPress Enter to exit...");
            System.in.read();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static IgniteConfiguration createPersistentConfig() {
        IgniteConfiguration cfg = new IgniteConfiguration();
        cfg.setIgniteInstanceName("backup-node");

        // Configure persistence with WAL archiving
        DataStorageConfiguration storageCfg = new DataStorageConfiguration();

        DataRegionConfiguration regionCfg = new DataRegionConfiguration();
        regionCfg.setName("backup-region");
        regionCfg.setPersistenceEnabled(true);
        regionCfg.setMaxSize(1L * 1024 * 1024 * 1024); // 1GB
        storageCfg.setDefaultDataRegionConfiguration(regionCfg);

        // WAL configuration for recovery
        storageCfg.setWalArchivePath("./ignite-work/wal-archive");
        storageCfg.setWalPath("./ignite-work/wal");
        storageCfg.setStoragePath("./ignite-work/storage");

        cfg.setDataStorageConfiguration(storageCfg);

        // Discovery
        TcpDiscoverySpi spi = new TcpDiscoverySpi();
        TcpDiscoveryVmIpFinder ipFinder = new TcpDiscoveryVmIpFinder();
        ipFinder.setAddresses(Arrays.asList("127.0.0.1:47500..47509"));
        spi.setIpFinder(ipFinder);
        cfg.setDiscoverySpi(spi);

        return cfg;
    }

    private static void printSnapshotCommands() {
        System.out.println("=== Snapshot CLI Commands ===\n");

        System.out.println("# Create a snapshot");
        System.out.println("control.sh --snapshot create my_snapshot");
        System.out.println("");

        System.out.println("# List all snapshots");
        System.out.println("control.sh --snapshot list");
        System.out.println("");

        System.out.println("# Check snapshot status");
        System.out.println("control.sh --snapshot status my_snapshot");
        System.out.println("");

        System.out.println("# Restore from snapshot");
        System.out.println("control.sh --snapshot restore my_snapshot");
        System.out.println("");

        System.out.println("# Restore specific caches");
        System.out.println("control.sh --snapshot restore my_snapshot --caches cache1,cache2");
        System.out.println("");

        System.out.println("# Cancel running snapshot");
        System.out.println("control.sh --snapshot cancel my_snapshot");
        System.out.println("");

        System.out.println("=== WAL Archiving Configuration ===\n");
        System.out.println("WAL archiving enables point-in-time recovery:");
        System.out.println("  - walArchivePath: /data/ignite/wal-archive");
        System.out.println("  - walMode: FSYNC (recommended for production)");
        System.out.println("  - walSegmentSize: 256MB (default)");
        System.out.println("  - walHistorySize: 20 (segments to keep)");
        System.out.println("");
    }

    private static void printRecoveryProcedures() {
        System.out.println("=== Recovery Procedures ===\n");

        System.out.println("1. Full Recovery from Snapshot:");
        System.out.println("   a. Stop all cluster nodes");
        System.out.println("   b. Clear work directories (optional)");
        System.out.println("   c. Start cluster nodes");
        System.out.println("   d. Activate cluster");
        System.out.println("   e. Run: control.sh --snapshot restore my_snapshot");
        System.out.println("");

        System.out.println("2. Point-in-Time Recovery:");
        System.out.println("   a. Restore from last full snapshot");
        System.out.println("   b. Apply WAL archives up to desired point");
        System.out.println("   c. Verify data consistency");
        System.out.println("");

        System.out.println("3. Single Node Recovery:");
        System.out.println("   a. Stop failed node");
        System.out.println("   b. Clear node's work directory");
        System.out.println("   c. Restart node");
        System.out.println("   d. Node will sync from cluster automatically");
        System.out.println("");

        System.out.println("=== Backup Best Practices ===");
        System.out.println("1. Schedule regular snapshots (daily/hourly based on RPO)");
        System.out.println("2. Retain multiple snapshots for point-in-time recovery");
        System.out.println("3. Store snapshots on separate storage");
        System.out.println("4. Test recovery procedures regularly");
        System.out.println("5. Document recovery runbooks");
        System.out.println("6. Monitor snapshot creation success");
        System.out.println("7. Configure WAL archiving for fine-grained recovery");
    }
}
