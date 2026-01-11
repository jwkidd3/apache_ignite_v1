package com.example.ignite.solutions.lab02;

import org.apache.ignite.Ignite;
import org.apache.ignite.Ignition;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.ipfinder.vm.TcpDiscoveryVmIpFinder;

import java.util.Arrays;

/**
 * Lab 2 Exercise 1-2: Static IP Discovery
 *
 * This exercise demonstrates configuring static IP discovery
 * for multi-node cluster setup.
 */
public class StaticIPDiscovery {

    public static void main(String[] args) {
        // Check if node number is provided
        int nodeNumber = 1;
        if (args.length >= 1) {
            nodeNumber = Integer.parseInt(args[0]);
        }

        // Create Ignite configuration
        IgniteConfiguration cfg = new IgniteConfiguration();
        cfg.setIgniteInstanceName("static-node-" + nodeNumber);

        // Configure TCP Discovery SPI
        TcpDiscoverySpi discoverySpi = new TcpDiscoverySpi();

        // Configure static IP finder
        TcpDiscoveryVmIpFinder ipFinder = new TcpDiscoveryVmIpFinder();

        // Set the addresses of all nodes in the cluster
        ipFinder.setAddresses(Arrays.asList(
            "127.0.0.1:47500..47509"  // Port range for local testing
        ));

        discoverySpi.setIpFinder(ipFinder);
        cfg.setDiscoverySpi(discoverySpi);

        // Start Ignite node
        System.out.println("Starting Ignite node " + nodeNumber + " with static IP discovery...");

        try (Ignite ignite = Ignition.start(cfg)) {
            System.out.println("\n=== Node Started Successfully ===");
            System.out.println("Node Name: " + ignite.name());
            System.out.println("Node ID: " + ignite.cluster().localNode().id());
            System.out.println("Cluster Nodes: " + ignite.cluster().nodes().size());

            // Display all nodes in cluster
            System.out.println("\n=== Cluster Topology ===");
            ignite.cluster().nodes().forEach(node -> {
                System.out.println("Node: " + node.consistentId() +
                                 " | ID: " + node.id().toString().substring(0, 8) + "...");
            });

            System.out.println("\nNode running. Press Enter to stop...");
            System.in.read();

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
