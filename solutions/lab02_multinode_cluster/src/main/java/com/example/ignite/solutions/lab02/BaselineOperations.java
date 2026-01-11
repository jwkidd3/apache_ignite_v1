package com.example.ignite.solutions.lab02;

import org.apache.ignite.Ignite;
import org.apache.ignite.Ignition;
import org.apache.ignite.cluster.BaselineNode;
import org.apache.ignite.cluster.ClusterState;
import org.apache.ignite.configuration.DataRegionConfiguration;
import org.apache.ignite.configuration.DataStorageConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.ipfinder.vm.TcpDiscoveryVmIpFinder;

import java.util.*;

/**
 * Lab 2 Optional: Baseline Management Operations
 *
 * This exercise demonstrates advanced baseline topology management.
 */
public class BaselineOperations {

    public static void main(String[] args) {
        String operation = "add-all";
        if (args.length >= 1) {
            operation = args[0];
        }

        System.out.println("=== Baseline Management Operations ===");
        System.out.println("Operation: " + operation);
        System.out.println("\nAvailable operations:");
        System.out.println("  add-all        - Add all online nodes to baseline");
        System.out.println("  remove-offline - Remove offline nodes from baseline");
        System.out.println("  reset          - Reset baseline to current topology");

        IgniteConfiguration cfg = createConfiguration("baseline-ops-node");

        try (Ignite ignite = Ignition.start(cfg)) {
            if (ignite.cluster().state() != ClusterState.ACTIVE) {
                ignite.cluster().state(ClusterState.ACTIVE);
            }

            switch (operation) {
                case "add-all":
                    addAllNodesToBaseline(ignite);
                    break;
                case "remove-offline":
                    removeOfflineFromBaseline(ignite);
                    break;
                case "reset":
                    resetBaseline(ignite);
                    break;
                default:
                    System.out.println("Unknown operation: " + operation);
            }

            // Display updated baseline
            displayBaseline(ignite);

            System.out.println("\nPress Enter to stop...");
            System.in.read();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void addAllNodesToBaseline(Ignite ignite) {
        System.out.println("\n=== Adding All Online Nodes to Baseline ===");

        // Get current topology version
        long topologyVersion = ignite.cluster().topologyVersion();
        System.out.println("Current topology version: " + topologyVersion);

        // Set baseline to include all current server nodes
        ignite.cluster().setBaselineTopology(topologyVersion);
        System.out.println("Baseline updated to topology version " + topologyVersion);
    }

    private static void removeOfflineFromBaseline(Ignite ignite) {
        System.out.println("\n=== Removing Offline Nodes from Baseline ===");

        Collection<BaselineNode> currentBaseline = ignite.cluster().currentBaselineTopology();
        if (currentBaseline == null || currentBaseline.isEmpty()) {
            System.out.println("No baseline topology set.");
            return;
        }

        // Get online node IDs
        Set<Object> onlineIds = new HashSet<>();
        ignite.cluster().forServers().nodes()
            .forEach(n -> onlineIds.add(n.consistentId()));

        // Filter baseline to only include online nodes
        List<BaselineNode> newBaseline = new ArrayList<>();
        for (BaselineNode node : currentBaseline) {
            if (onlineIds.contains(node.consistentId())) {
                newBaseline.add(node);
            } else {
                System.out.println("Removing offline node: " + node.consistentId());
            }
        }

        // Update baseline with only online nodes
        ignite.cluster().setBaselineTopology(newBaseline);
        System.out.println("Baseline updated. Removed offline nodes.");
    }

    private static void resetBaseline(Ignite ignite) {
        System.out.println("\n=== Resetting Baseline to Current Topology ===");

        // This effectively recreates baseline from scratch with current nodes
        long currentVersion = ignite.cluster().topologyVersion();
        ignite.cluster().setBaselineTopology(currentVersion);

        System.out.println("Baseline reset to topology version: " + currentVersion);
        System.out.println("Baseline now contains all current server nodes.");
    }

    private static void displayBaseline(Ignite ignite) {
        System.out.println("\n=== Updated Baseline Topology ===");

        Collection<BaselineNode> baseline = ignite.cluster().currentBaselineTopology();
        if (baseline != null) {
            System.out.println("Total nodes: " + baseline.size());
            baseline.forEach(n -> System.out.println("  - " + n.consistentId()));
        } else {
            System.out.println("No baseline set.");
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
        storageCfg.setStoragePath("./ignite-data/baseline-ops");
        storageCfg.setWalPath("./ignite-wal/baseline-ops");
        storageCfg.setWalArchivePath("./ignite-wal-archive/baseline-ops");
        cfg.setDataStorageConfiguration(storageCfg);

        TcpDiscoverySpi discoverySpi = new TcpDiscoverySpi();
        TcpDiscoveryVmIpFinder ipFinder = new TcpDiscoveryVmIpFinder();
        ipFinder.setAddresses(Arrays.asList("127.0.0.1:47500..47509"));
        discoverySpi.setIpFinder(ipFinder);
        cfg.setDiscoverySpi(discoverySpi);

        return cfg;
    }
}
