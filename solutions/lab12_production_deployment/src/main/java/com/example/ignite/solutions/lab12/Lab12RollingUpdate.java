package com.example.ignite.solutions.lab12;

import org.apache.ignite.Ignite;
import org.apache.ignite.Ignition;
import org.apache.ignite.cluster.BaselineNode;
import org.apache.ignite.cluster.ClusterNode;
import org.apache.ignite.cluster.ClusterState;
import org.apache.ignite.configuration.DataRegionConfiguration;
import org.apache.ignite.configuration.DataStorageConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.ipfinder.vm.TcpDiscoveryVmIpFinder;

import java.util.Arrays;
import java.util.Collection;

/**
 * Lab 12 Optional Exercise: Rolling Updates
 *
 * Demonstrates:
 * - Zero-downtime upgrade procedure
 * - Baseline topology management
 * - Rollback procedures
 */
public class Lab12RollingUpdate {

    public static void main(String[] args) {
        System.out.println("=== Rolling Update Procedure Lab ===\n");

        IgniteConfiguration cfg = createConfig();

        try (Ignite ignite = Ignition.start(cfg)) {
            // Activate cluster
            if (ignite.cluster().state() != ClusterState.ACTIVE) {
                ignite.cluster().state(ClusterState.ACTIVE);
            }

            System.out.println("=== Current Cluster State ===");
            System.out.println("Cluster state: " + ignite.cluster().state());
            System.out.println("Total nodes: " + ignite.cluster().nodes().size());

            // Show baseline topology
            Collection<BaselineNode> baseline = ignite.cluster().currentBaselineTopology();
            System.out.println("Baseline nodes: " + (baseline != null ? baseline.size() : 0));

            // List nodes
            System.out.println("\nCluster nodes:");
            for (ClusterNode node : ignite.cluster().nodes()) {
                System.out.println("  - " + node.consistentId() +
                    " (server=" + !node.isClient() + ")");
            }

            // Print rolling update procedure
            printRollingUpdateProcedure();

            // Print rollback procedure
            printRollbackProcedure();

            // Print baseline management commands
            printBaselineCommands();

            System.out.println("\nPress Enter to exit...");
            System.in.read();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static IgniteConfiguration createConfig() {
        IgniteConfiguration cfg = new IgniteConfiguration();
        cfg.setIgniteInstanceName("update-node");

        DataStorageConfiguration storageCfg = new DataStorageConfiguration();
        DataRegionConfiguration regionCfg = new DataRegionConfiguration();
        regionCfg.setName("update-region");
        regionCfg.setPersistenceEnabled(true);
        storageCfg.setDefaultDataRegionConfiguration(regionCfg);
        cfg.setDataStorageConfiguration(storageCfg);

        TcpDiscoverySpi spi = new TcpDiscoverySpi();
        TcpDiscoveryVmIpFinder ipFinder = new TcpDiscoveryVmIpFinder();
        ipFinder.setAddresses(Arrays.asList("127.0.0.1:47500..47509"));
        spi.setIpFinder(ipFinder);
        cfg.setDiscoverySpi(spi);

        return cfg;
    }

    private static void printRollingUpdateProcedure() {
        System.out.println("\n=== Rolling Update Procedure ===\n");

        System.out.println("Phase 1: Pre-Update Preparation");
        System.out.println("  1. Create full backup (snapshot)");
        System.out.println("  2. Verify all nodes healthy");
        System.out.println("  3. Check rebalancing complete");
        System.out.println("  4. Document current baseline");
        System.out.println("  5. Notify stakeholders of maintenance");
        System.out.println("");

        System.out.println("Phase 2: Disable Auto-Baseline");
        System.out.println("  control.sh --baseline auto_adjust disable");
        System.out.println("  (Prevents automatic baseline changes during update)");
        System.out.println("");

        System.out.println("Phase 3: Update Each Node (one at a time)");
        System.out.println("  For each server node:");
        System.out.println("    a. Gracefully stop the node:");
        System.out.println("       kill -SIGTERM <pid>");
        System.out.println("       OR: control.sh --shutdown --node-id <node-id>");
        System.out.println("");
        System.out.println("    b. Wait for rebalancing (data redistribution):");
        System.out.println("       control.sh --cache idle_verify");
        System.out.println("");
        System.out.println("    c. Update Ignite binaries");
        System.out.println("       - Replace JAR files");
        System.out.println("       - Update configuration if needed");
        System.out.println("");
        System.out.println("    d. Start updated node:");
        System.out.println("       ignite.sh config/ignite-config.xml");
        System.out.println("");
        System.out.println("    e. Wait for node to join and sync:");
        System.out.println("       control.sh --baseline");
        System.out.println("");
        System.out.println("    f. Verify node health:");
        System.out.println("       curl 'http://localhost:8080/ignite?cmd=top'");
        System.out.println("");
        System.out.println("    g. Wait for rebalancing to complete");
        System.out.println("");
        System.out.println("    h. Proceed to next node");
        System.out.println("");

        System.out.println("Phase 4: Re-enable Auto-Baseline");
        System.out.println("  control.sh --baseline auto_adjust enable");
        System.out.println("");

        System.out.println("Phase 5: Post-Update Verification");
        System.out.println("  1. Verify all nodes running new version");
        System.out.println("  2. Check cluster state: ACTIVE");
        System.out.println("  3. Verify data integrity:");
        System.out.println("     control.sh --cache idle_verify");
        System.out.println("  4. Run application tests");
        System.out.println("  5. Monitor for 24 hours");
    }

    private static void printRollbackProcedure() {
        System.out.println("\n=== Rollback Procedure ===\n");

        System.out.println("If issues detected during update:");
        System.out.println("  1. Stop the problematic node");
        System.out.println("  2. Restore previous version binaries");
        System.out.println("  3. Restart node");
        System.out.println("  4. Verify cluster health");
        System.out.println("");

        System.out.println("Full rollback (restore from backup):");
        System.out.println("  1. Stop all nodes");
        System.out.println("  2. Restore previous version on all nodes");
        System.out.println("  3. Restore from pre-update snapshot");
        System.out.println("  4. Start cluster");
        System.out.println("  5. Verify data integrity");
    }

    private static void printBaselineCommands() {
        System.out.println("\n=== Baseline Management Commands ===\n");

        System.out.println("# View current baseline");
        System.out.println("control.sh --baseline");
        System.out.println("");

        System.out.println("# Add node to baseline");
        System.out.println("control.sh --baseline add <consistent-id>");
        System.out.println("");

        System.out.println("# Remove node from baseline");
        System.out.println("control.sh --baseline remove <consistent-id>");
        System.out.println("");

        System.out.println("# Set baseline to current topology");
        System.out.println("control.sh --baseline set \"node1,node2,node3\"");
        System.out.println("");

        System.out.println("# Enable auto-adjust");
        System.out.println("control.sh --baseline auto_adjust enable");
        System.out.println("");

        System.out.println("# Disable auto-adjust");
        System.out.println("control.sh --baseline auto_adjust disable");
        System.out.println("");

        System.out.println("# Set auto-adjust timeout (60 seconds)");
        System.out.println("control.sh --baseline auto_adjust timeout 60000");
    }
}
