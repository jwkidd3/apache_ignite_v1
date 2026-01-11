package com.example.ignite.solutions.lab01;

import org.apache.ignite.Ignite;
import org.apache.ignite.Ignition;
import org.apache.ignite.configuration.IgniteConfiguration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Lab 1 Challenge 1: Start a 3-Node Cluster
 *
 * This exercise demonstrates starting multiple Ignite nodes
 * in the same JVM and verifying they form a cluster.
 */
public class MultiNodeCluster {

    public static void main(String[] args) {
        List<Ignite> nodes = new ArrayList<>();

        try {
            System.out.println("=== Challenge 1: Starting 3-Node Cluster ===\n");

            // Start 3 nodes
            for (int i = 1; i <= 3; i++) {
                IgniteConfiguration cfg = new IgniteConfiguration();
                cfg.setIgniteInstanceName("node-" + i);
                cfg.setPeerClassLoadingEnabled(true);

                // Set unique attributes for each node
                cfg.setUserAttributes(Collections.singletonMap("nodeNumber", i));

                System.out.println("Starting node-" + i + "...");
                Ignite ignite = Ignition.start(cfg);
                nodes.add(ignite);

                System.out.println("  Node-" + i + " started. Cluster size: "
                    + ignite.cluster().nodes().size());
            }

            // Verify cluster formation
            System.out.println("\n=== Cluster Verification ===");
            Ignite firstNode = nodes.get(0);

            if (firstNode.cluster().nodes().size() == 3) {
                System.out.println("SUCCESS: All 3 nodes have joined the cluster!");
            } else {
                System.out.println("WARNING: Expected 3 nodes, found: "
                    + firstNode.cluster().nodes().size());
            }

            // Display cluster topology
            System.out.println("\n=== Cluster Topology ===");
            firstNode.cluster().nodes().forEach(node -> {
                Integer nodeNum = node.attribute("nodeNumber");
                System.out.printf("  %s (node-%d): %s%n",
                    node.id().toString().substring(0, 8) + "...",
                    nodeNum != null ? nodeNum : 0,
                    node.isLocal() ? "LOCAL" : "REMOTE");
            });

            System.out.println("\nPress Enter to stop all nodes...");
            System.in.read();

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Stop all nodes gracefully
            System.out.println("\nStopping all nodes...");
            for (int i = nodes.size() - 1; i >= 0; i--) {
                String name = nodes.get(i).name();
                nodes.get(i).close();
                System.out.println("  " + name + " stopped.");
            }
            System.out.println("All nodes stopped.");
        }
    }
}
