package com.example.ignite.solutions.lab09;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCompute;
import org.apache.ignite.Ignition;
import org.apache.ignite.cluster.ClusterGroup;
import org.apache.ignite.cluster.ClusterNode;

import java.util.Collection;

/**
 * Lab 09 Exercise 3: Cluster Groups and Targeted Execution
 *
 * Demonstrates:
 * - Executing on server nodes only
 * - Executing on client nodes
 * - Targeting oldest/youngest nodes
 * - Attribute-based node selection
 * - Random node execution
 */
public class Lab09ClusterGroups {

    public static void main(String[] args) {
        try (Ignite ignite = Ignition.start()) {
            System.out.println("=== Cluster Groups and Targeted Execution ===\n");

            // Get cluster information
            Collection<ClusterNode> allNodes = ignite.cluster().nodes();
            System.out.println("Total nodes in cluster: " + allNodes.size());

            // Example 1: Execute on server nodes only
            System.out.println("\n=== Execute on Server Nodes ===");
            ClusterGroup serverGroup = ignite.cluster().forServers();
            IgniteCompute serverCompute = ignite.compute(serverGroup);

            serverCompute.broadcast(() -> {
                System.out.println("Server node: " +
                    Ignition.ignite().cluster().localNode().consistentId());
            });

            Thread.sleep(300);

            // Example 2: Execute on client nodes
            System.out.println("\n=== Execute on Client Nodes ===");
            ClusterGroup clientGroup = ignite.cluster().forClients();

            if (!clientGroup.nodes().isEmpty()) {
                ignite.compute(clientGroup).broadcast(() -> {
                    System.out.println("Client node: " +
                        Ignition.ignite().cluster().localNode().consistentId());
                });
            } else {
                System.out.println("No client nodes in cluster");
            }

            // Example 3: Execute on oldest node (coordinator)
            System.out.println("\n=== Execute on Oldest Node ===");
            ClusterGroup oldestNode = ignite.cluster().forOldest();

            ignite.compute(oldestNode).run(() -> {
                ClusterNode node = Ignition.ignite().cluster().localNode();
                System.out.println("Oldest node: " + node.consistentId());
                System.out.println("Node order: " + node.order());
            });

            Thread.sleep(300);

            // Example 4: Execute on youngest node
            System.out.println("\n=== Execute on Youngest Node ===");
            ClusterGroup youngestNode = ignite.cluster().forYoungest();

            ignite.compute(youngestNode).run(() -> {
                ClusterNode node = Ignition.ignite().cluster().localNode();
                System.out.println("Youngest node: " + node.consistentId());
                System.out.println("Node order: " + node.order());
            });

            Thread.sleep(300);

            // Example 5: Execute on nodes with specific attribute
            System.out.println("\n=== Execute on Nodes with Attribute ===");
            // Note: Attributes must be set in node configuration
            ClusterGroup attrGroup = ignite.cluster().forAttribute("ROLE", "COMPUTE");

            if (!attrGroup.nodes().isEmpty()) {
                ignite.compute(attrGroup).broadcast(() -> {
                    System.out.println("Compute role node executing");
                });
            } else {
                System.out.println("No nodes with ROLE=COMPUTE attribute");
            }

            // Example 6: Execute on random node
            System.out.println("\n=== Execute on Random Node ===");
            ClusterGroup randomNode = ignite.cluster().forRandom();

            for (int i = 0; i < 5; i++) {
                ignite.compute(randomNode).run(() -> {
                    System.out.println("Random execution on: " +
                        Ignition.ignite().cluster().localNode().consistentId());
                });
            }

            Thread.sleep(500);

            // Example 7: Execute on remote nodes (exclude local)
            System.out.println("\n=== Execute on Remote Nodes ===");
            ClusterGroup remoteNodes = ignite.cluster().forRemotes();

            if (!remoteNodes.nodes().isEmpty()) {
                ignite.compute(remoteNodes).broadcast(() -> {
                    System.out.println("Remote node: " +
                        Ignition.ignite().cluster().localNode().consistentId());
                });
            } else {
                System.out.println("No remote nodes (single node cluster)");
            }

            System.out.println("\n=== Cluster Group Use Cases ===");
            System.out.println("- forServers(): Heavy computation tasks");
            System.out.println("- forClients(): Lightweight coordination");
            System.out.println("- forOldest(): Singleton services, coordination");
            System.out.println("- forAttribute(): Role-based task routing");
            System.out.println("- forRemotes(): Exclude local node");

            System.out.println("\nPress Enter to exit...");
            System.in.read();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
