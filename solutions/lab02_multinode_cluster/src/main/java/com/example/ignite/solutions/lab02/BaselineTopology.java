package com.example.ignite.solutions.lab02;

import org.apache.ignite.Ignite;
import org.apache.ignite.Ignition;
import org.apache.ignite.cluster.ClusterState;
import org.apache.ignite.configuration.DataRegionConfiguration;
import org.apache.ignite.configuration.DataStorageConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.ipfinder.vm.TcpDiscoveryVmIpFinder;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Arrays;

/**
 * Lab 2 Exercise 3-5: Baseline Topology
 *
 * This exercise demonstrates configuring baseline topology
 * for persistent clusters.
 *
 * IMPORTANT: All nodes must join the cluster BEFORE activation.
 * Once activated, the baseline is set and new nodes won't automatically
 * be included in the baseline topology.
 *
 * Usage:
 *   1. Start all nodes (e.g., node 1, 2, 3) - they will wait
 *   2. On the coordinator node (node 1), press Enter to activate
 *   3. All nodes present at activation time will be in the baseline
 */
public class BaselineTopology {

    public static void main(String[] args) throws Exception {
        int nodeNumber = 1;
        int expectedNodes = 1;

        if (args.length >= 1) {
            nodeNumber = Integer.parseInt(args[0]);
        }
        if (args.length >= 2) {
            expectedNodes = Integer.parseInt(args[1]);
        }

        // Create configuration
        IgniteConfiguration cfg = new IgniteConfiguration();
        cfg.setIgniteInstanceName("baseline-node-" + nodeNumber);

        // Configure persistence (required for baseline topology)
        DataStorageConfiguration storageCfg = new DataStorageConfiguration();

        // Configure default data region
        DataRegionConfiguration defaultRegion = new DataRegionConfiguration();
        defaultRegion.setPersistenceEnabled(true);
        defaultRegion.setMaxSize(100L * 1024 * 1024); // 100 MB

        storageCfg.setDefaultDataRegionConfiguration(defaultRegion);
        storageCfg.setStoragePath("./ignite-data/node" + nodeNumber);
        storageCfg.setWalPath("./ignite-wal/node" + nodeNumber);
        storageCfg.setWalArchivePath("./ignite-wal-archive/node" + nodeNumber);

        cfg.setDataStorageConfiguration(storageCfg);

        // Configure discovery
        TcpDiscoverySpi discoverySpi = new TcpDiscoverySpi();
        TcpDiscoveryVmIpFinder ipFinder = new TcpDiscoveryVmIpFinder();
        ipFinder.setAddresses(Arrays.asList("127.0.0.1:47500..47509"));
        discoverySpi.setIpFinder(ipFinder);
        cfg.setDiscoverySpi(discoverySpi);

        System.out.println("Starting node " + nodeNumber + " with persistence...");

        try (Ignite ignite = Ignition.start(cfg)) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

            System.out.println("\n=== Node Information ===");
            System.out.println("Node: " + ignite.name());
            System.out.println("Cluster state: " + ignite.cluster().state());

            // If cluster is inactive, handle activation
            if (ignite.cluster().state() != ClusterState.ACTIVE) {
                if (nodeNumber == 1) {
                    // Coordinator node - wait for all nodes then activate
                    System.out.println("\n=== Waiting for Nodes ===");
                    System.out.println("Expected nodes: " + expectedNodes);

                    // Wait for expected nodes to join
                    while (ignite.cluster().forServers().nodes().size() < expectedNodes) {
                        int currentSize = ignite.cluster().forServers().nodes().size();
                        System.out.println("Current nodes: " + currentSize + "/" + expectedNodes +
                            " - waiting for more nodes...");
                        Thread.sleep(2000);
                    }

                    System.out.println("\nAll " + expectedNodes + " node(s) have joined!");
                    System.out.println("Server nodes in cluster:");
                    ignite.cluster().forServers().nodes().forEach(n ->
                        System.out.println("  - " + n.consistentId()));

                    System.out.println("\nPress Enter to ACTIVATE cluster and set baseline...");
                    reader.readLine();

                    System.out.println("Activating cluster...");
                    ignite.cluster().state(ClusterState.ACTIVE);
                    System.out.println("Cluster activated!");

                    // Set baseline topology with ALL current nodes
                    System.out.println("Setting baseline topology with all " +
                        ignite.cluster().forServers().nodes().size() + " nodes...");
                    ignite.cluster().setBaselineTopology(ignite.cluster().topologyVersion());
                    System.out.println("Baseline topology set!");
                } else {
                    // Non-coordinator node - wait for activation
                    System.out.println("\nWaiting for coordinator (node 1) to activate cluster...");
                    while (ignite.cluster().state() != ClusterState.ACTIVE) {
                        Thread.sleep(1000);
                    }
                    System.out.println("Cluster is now ACTIVE!");
                }
            }

            // Display baseline topology
            System.out.println("\n=== Baseline Topology ===");
            if (ignite.cluster().currentBaselineTopology() != null) {
                System.out.println("Baseline nodes: " +
                    ignite.cluster().currentBaselineTopology().size());
                ignite.cluster().currentBaselineTopology().forEach(node -> {
                    System.out.println("  - " + node.consistentId());
                });
            } else {
                System.out.println("Baseline topology not set");
            }

            // Exercise 5: Check if node is in baseline
            if (ignite.cluster().currentBaselineTopology() != null) {
                boolean inBaseline = ignite.cluster().currentBaselineTopology()
                    .stream()
                    .anyMatch(n -> n.consistentId().equals(
                        ignite.cluster().localNode().consistentId()));

                System.out.println("\nThis node in baseline: " + inBaseline);
            }

            // Display current topology version
            System.out.println("Current topology version: " + ignite.cluster().topologyVersion());
            System.out.println("Cluster size: " + ignite.cluster().forServers().nodes().size());

            System.out.println("\nNode running. Press Enter to stop...");
            reader.readLine();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
