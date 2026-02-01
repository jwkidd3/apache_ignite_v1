package com.example.ignite.solutions.lab13;

import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.network.ClusterNode;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Collection;

/**
 * Lab 13 Exercises 1-2: Ignite 3.x Discovery and Cluster Lifecycle
 *
 * Demonstrates:
 * - Connecting to an Ignite 3.x cluster via thin client
 * - Listing cluster connections and topology info
 * - SWIM gossip discovery with live REST queries
 * - RAFT consensus (explained)
 * - Cluster lifecycle: init, topology, state via CLI/REST
 *
 * Prerequisites: Running Ignite 3.x cluster (Docker or binary)
 */
public class Lab13Discovery {

    public static void main(String[] args) {
        System.out.println("=== Lab 13: Ignite 3.x Discovery & Cluster Lifecycle ===\n");

        try (IgniteClient client = IgniteClient.builder()
                .addresses("127.0.0.1:10800")
                .build()) {

            demonstrateConnection(client);
            explainSwimDiscovery();
            explainRaftConsensus();
            explainClusterLifecycle();
            explainTopologyManagement();

        } catch (Exception e) {
            System.err.println("Could not connect to Ignite 3.x cluster at 127.0.0.1:10800");
            System.err.println("Make sure the cluster is running. See README for setup.");
            e.printStackTrace();
        }
    }

    /**
     * Exercise 1: Connect and inspect cluster connections and enumerate nodes
     */
    private static void demonstrateConnection(IgniteClient client) {
        System.out.println("=== Exercise 1: Client Connection ===\n");

        System.out.println("Connected to Ignite 3.x cluster!");
        System.out.println("Active connections: " + client.connections().size());
        System.out.println();

        System.out.println("Connection Details:");
        client.connections().forEach(conn -> {
            System.out.println("  Address: " + conn.address());
            System.out.println("  Node ID: " + conn.id());
            System.out.println("  Node Name: " + conn.name());
            System.out.println();
        });

        // Enumerate cluster nodes via client.clusterNodes()
        System.out.println("Cluster Node Enumeration (client.clusterNodes()):");
        Collection<ClusterNode> nodes = client.clusterNodes();
        System.out.println("  Total nodes in cluster: " + nodes.size());
        System.out.println();
        for (ClusterNode node : nodes) {
            System.out.println("  Node: " + node.name());
            System.out.println("    ID:      " + node.id());
            System.out.println("    Address: " + node.address());
            System.out.println();
        }

        System.out.println("Key Difference from 2.x:");
        System.out.println("  - No TcpDiscoverySpi configuration needed");
        System.out.println("  - No IP finder configuration");
        System.out.println("  - Client connects to known addresses via builder pattern");
        System.out.println("  - All clients are thin (no thick/embedded client distinction)");
        System.out.println("  - client.clusterNodes() provides full cluster membership");
        System.out.println();
    }

    /**
     * Queries the cluster REST API to show live physical topology,
     * demonstrating SWIM-based discovery results.
     */
    private static void explainSwimDiscovery() {
        System.out.println("=== SWIM Gossip Discovery Protocol (3.x) ===\n");

        System.out.println("Ignite 3.x uses SWIM (Scalable Weakly-consistent");
        System.out.println("Infection-style Process Group Membership) protocol.\n");

        System.out.println("  How SWIM Works:");
        System.out.println("  1. Each node periodically pings random cluster members");
        System.out.println("  2. If a node doesn't respond, indirect probes through other nodes");
        System.out.println("  3. Failure information spreads via gossip (piggybacked on pings)");
        System.out.println("  4. Membership updates propagate in O(log N) rounds");
        System.out.println();

        // Query the REST API for live physical topology
        System.out.println("  Live Physical Topology (via REST API):");
        System.out.println("  GET http://localhost:10300/management/v1/cluster/topology/physical\n");

        try {
            HttpClient httpClient = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:10300/management/v1/cluster/topology/physical"))
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                System.out.println("  Response (status " + response.statusCode() + "):");
                System.out.println("  " + response.body());
            } else {
                System.out.println("  REST API returned status: " + response.statusCode());
                System.out.println("  Body: " + response.body());
            }
        } catch (Exception e) {
            System.out.println("  Could not reach REST API at localhost:10300.");
            System.out.println("  Error: " + e.getMessage());
            System.out.println("  (Ensure the cluster REST endpoint is accessible.)");
        }

        System.out.println();
    }

    private static void explainRaftConsensus() {
        System.out.println("=== RAFT Consensus (3.x) ===\n");

        System.out.println("Ignite 3.x uses RAFT for distributed consensus:\n");

        System.out.println("  What RAFT Provides:");
        System.out.println("  - Leader election for each partition group");
        System.out.println("  - Log replication across replicas");
        System.out.println("  - Guaranteed consistency even during failures");
        System.out.println("  - Metastorage group for cluster metadata");
        System.out.println();

        System.out.println("  RAFT Groups in Ignite 3.x:");
        System.out.println("  - Metastorage group: cluster-wide metadata");
        System.out.println("  - CMG (Cluster Management Group): membership decisions");
        System.out.println("  - Partition groups: one per table partition");
        System.out.println();

        System.out.println("  Comparison with Ignite 2.x:");
        System.out.println("  | Aspect      | Ignite 2.x             | Ignite 3.x           |");
        System.out.println("  |-------------|------------------------|----------------------|");
        System.out.println("  | Consensus   | Custom protocol        | RAFT                 |");
        System.out.println("  | Metadata    | Distributed cache      | RAFT metastorage     |");
        System.out.println("  | Consistency | Eventual (configurable)| Strong (linearizable)|");
        System.out.println("  | Partition   | Primary-backup model   | RAFT-replicated      |");
        System.out.println();
    }

    /**
     * Exercise 2: Cluster lifecycle
     */
    private static void explainClusterLifecycle() {
        System.out.println("=== Exercise 2: Cluster Lifecycle (3.x) ===\n");

        System.out.println("Ignite 3.x Cluster Initialization:");
        System.out.println("  # Step 1: Start nodes (each with HOCON network config)");
        System.out.println("  # Config in etc/ignite3-config.conf");
        System.out.println();
        System.out.println("  # Step 2: Initialize the cluster (required, explicit step)");
        System.out.println("  ignite3 cluster init \\");
        System.out.println("    --name=myCluster \\");
        System.out.println("    --meta-storage-node=node1 \\");
        System.out.println("    --meta-storage-node=node2 \\");
        System.out.println("    --meta-storage-node=node3");
        System.out.println();
        System.out.println("  # Step 3: Check cluster state");
        System.out.println("  ignite3 cluster state");
        System.out.println();

        System.out.println("Contrast with Ignite 2.x:");
        System.out.println("  2.x: Nodes auto-discover via IpFinder, cluster forms automatically");
        System.out.println("  3.x: Explicit init step required, metastorage nodes designated");
        System.out.println("  3.x: More controlled, predictable cluster formation");
        System.out.println();
    }

    private static void explainTopologyManagement() {
        System.out.println("=== Topology Management (3.x) ===\n");

        System.out.println("CLI Commands:");
        System.out.println("  ignite3 cluster topology physical  # List physical nodes");
        System.out.println("  ignite3 cluster topology logical   # List logical topology");
        System.out.println("  ignite3 node status                # Node health check");
        System.out.println();

        System.out.println("REST API:");
        System.out.println("  GET http://localhost:10300/management/v1/cluster/state");
        System.out.println("  GET http://localhost:10300/management/v1/cluster/topology/physical");
        System.out.println("  GET http://localhost:10300/management/v1/cluster/topology/logical");
        System.out.println();

        System.out.println("Contrast with Ignite 2.x:");
        System.out.println("  2.x: control.sh --state, control.sh --baseline");
        System.out.println("  3.x: ignite3 cluster state, ignite3 cluster topology");
        System.out.println("  2.x: Baseline topology (manual node management)");
        System.out.println("  3.x: Automatic node management via distribution zones");
        System.out.println();
    }
}
