package com.example.ignite.solutions.lab01;

import org.apache.ignite.Ignite;
import org.apache.ignite.Ignition;
import org.apache.ignite.configuration.IgniteConfiguration;

/**
 * Lab 1 Exercise 1: First Ignite Cluster
 *
 * This exercise demonstrates starting your first Ignite node
 * and displaying basic cluster information.
 */
public class FirstCluster {

    public static void main(String[] args) {
        // Create Ignite configuration
        IgniteConfiguration cfg = new IgniteConfiguration();

        // Set a unique name for this node
        cfg.setIgniteInstanceName("first-ignite-node");

        // Enable peer class loading (useful for development)
        cfg.setPeerClassLoadingEnabled(true);

        System.out.println("Starting Ignite node...");

        // Start Ignite node
        try (Ignite ignite = Ignition.start(cfg)) {
            System.out.println("Ignite node started successfully!");
            System.out.println("Node ID: " + ignite.cluster().localNode().id());
            System.out.println("Node is client: " + ignite.cluster().localNode().isClient());
            System.out.println("Total nodes in cluster: " + ignite.cluster().nodes().size());

            // Exercise 5: Display cluster information
            System.out.println("\n=== Cluster Information ===");
            System.out.println("Cluster nodes: " + ignite.cluster().nodes().size());

            // Display all node IDs in the cluster
            System.out.println("\n=== Node IDs in Cluster ===");
            ignite.cluster().nodes().forEach(node -> {
                System.out.println("Node ID: " + node.id());
                System.out.println("  - Consistent ID: " + node.consistentId());
                System.out.println("  - Is Local: " + node.isLocal());
                System.out.println("  - Is Client: " + node.isClient());
            });

            // Display cluster state
            System.out.println("\n=== Cluster State ===");
            System.out.println("Active: " + ignite.cluster().state().active());

            // Keep the node running for observation
            System.out.println("\nNode is running. Press Enter to stop...");
            System.in.read();

            System.out.println("Stopping Ignite node...");
        } catch (Exception e) {
            System.err.println("Error running Ignite node: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("Ignite node stopped.");
    }
}
