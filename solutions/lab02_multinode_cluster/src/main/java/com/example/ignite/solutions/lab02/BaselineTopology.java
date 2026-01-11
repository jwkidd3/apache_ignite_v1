package com.example.ignite.solutions.lab02;

import org.apache.ignite.Ignite;
import org.apache.ignite.Ignition;
import org.apache.ignite.cluster.ClusterState;
import org.apache.ignite.configuration.DataRegionConfiguration;
import org.apache.ignite.configuration.DataStorageConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.ipfinder.vm.TcpDiscoveryVmIpFinder;

import java.util.Arrays;

/**
 * Lab 2 Exercise 3-5: Baseline Topology
 *
 * This exercise demonstrates configuring baseline topology
 * for persistent clusters.
 */
public class BaselineTopology {

    public static void main(String[] args) {
        int nodeNumber = 1;
        if (args.length >= 1) {
            nodeNumber = Integer.parseInt(args[0]);
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
            System.out.println("\n=== Node Information ===");
            System.out.println("Node: " + ignite.name());
            System.out.println("Cluster size: " + ignite.cluster().nodes().size());
            System.out.println("Cluster state: " + ignite.cluster().state());

            // If this is node 1 and cluster is inactive, activate it
            if (nodeNumber == 1 && ignite.cluster().state() != ClusterState.ACTIVE) {
                System.out.println("\nActivating cluster...");
                ignite.cluster().state(ClusterState.ACTIVE);
                System.out.println("Cluster activated!");

                // Set baseline topology
                System.out.println("Setting baseline topology...");
                ignite.cluster().setBaselineTopology(ignite.cluster().topologyVersion());
                System.out.println("Baseline topology set!");
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
            long currentTopology = ignite.cluster().topologyVersion();
            System.out.println("Current topology version: " + currentTopology);

            System.out.println("\nNode running. Press Enter to stop...");
            System.in.read();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
