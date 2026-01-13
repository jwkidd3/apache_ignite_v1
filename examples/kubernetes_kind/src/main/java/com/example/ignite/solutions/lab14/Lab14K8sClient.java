package com.example.ignite.solutions.lab14;

import org.apache.ignite.Ignition;
import org.apache.ignite.client.ClientCache;
import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.configuration.ClientConfiguration;

/**
 * Lab 14: Kubernetes Client Application
 *
 * Demonstrates connecting a thin client to Apache Ignite
 * running on Kubernetes (KIND cluster).
 *
 * Prerequisites:
 * 1. Run ./scripts/setup.sh to create the cluster
 * 2. Ignite pods should be running in the 'ignite' namespace
 * 3. Port 10800 should be accessible via NodePort mapping
 */
public class Lab14K8sClient {

    private static final String IGNITE_ADDRESS = "127.0.0.1:10800";
    private static final String CACHE_NAME = "k8s-demo-cache";

    public static void main(String[] args) {
        System.out.println("╔════════════════════════════════════════════════════════════════╗");
        System.out.println("║   Lab 14: Kubernetes Client - Connecting to Ignite on K8s     ║");
        System.out.println("╚════════════════════════════════════════════════════════════════╝");
        System.out.println();

        // Configure thin client
        ClientConfiguration cfg = new ClientConfiguration()
            .setAddresses(IGNITE_ADDRESS);

        try (IgniteClient client = Ignition.startClient(cfg)) {
            System.out.println("Connected to Ignite cluster on Kubernetes!");
            System.out.println("  Address: " + IGNITE_ADDRESS);
            System.out.println();

            // Get or create cache
            ClientCache<Integer, String> cache = client.getOrCreateCache(CACHE_NAME);
            System.out.println("Cache '" + CACHE_NAME + "' ready");
            System.out.println();

            // Demonstrate CRUD operations
            demonstrateCrudOperations(cache);

            // Show cluster information
            showClusterInfo(client);

            System.out.println();
            System.out.println("Successfully demonstrated Ignite on Kubernetes!");

        } catch (Exception e) {
            System.err.println();
            System.err.println("ERROR: " + e.getMessage());
            System.err.println();
            printTroubleshooting();
        }
    }

    private static void demonstrateCrudOperations(ClientCache<Integer, String> cache) {
        // Write data
        System.out.println("=== Writing Data ===");
        for (int i = 1; i <= 10; i++) {
            String value = "Value-" + i + " (from K8s client)";
            cache.put(i, value);
            System.out.println("  PUT: key=" + i + ", value=" + value);
        }
        System.out.println();

        // Read data
        System.out.println("=== Reading Data ===");
        for (int i = 1; i <= 10; i++) {
            String value = cache.get(i);
            System.out.println("  GET: key=" + i + " -> " + value);
        }
        System.out.println();

        // Update data
        System.out.println("=== Updating Data ===");
        cache.put(1, "Updated-Value-1");
        System.out.println("  PUT: key=1 -> Updated-Value-1");
        System.out.println("  GET: key=1 -> " + cache.get(1));
        System.out.println();

        // Delete data
        System.out.println("=== Deleting Data ===");
        cache.remove(10);
        System.out.println("  REMOVE: key=10");
        System.out.println("  GET: key=10 -> " + cache.get(10) + " (should be null)");
        System.out.println();

        // Cache statistics
        System.out.println("=== Cache Statistics ===");
        System.out.println("  Cache name: " + cache.getName());
        System.out.println("  Cache size: " + cache.size());
    }

    private static void showClusterInfo(IgniteClient client) {
        System.out.println();
        System.out.println("=== Cluster Information ===");
        try {
            int serverNodes = client.cluster().forServers().nodes().size();
            System.out.println("  Server nodes: " + serverNodes);

            client.cluster().forServers().nodes().forEach(node -> {
                System.out.println("    - Node: " + node.consistentId());
            });
        } catch (Exception e) {
            System.out.println("  (Could not retrieve cluster info: " + e.getMessage() + ")");
        }
    }

    private static void printTroubleshooting() {
        System.err.println("Troubleshooting:");
        System.err.println();
        System.err.println("1. Check if KIND cluster is running:");
        System.err.println("   kind get clusters");
        System.err.println();
        System.err.println("2. Check if Ignite pods are ready:");
        System.err.println("   kubectl get pods -n ignite");
        System.err.println();
        System.err.println("3. Verify port mapping (should show 10800):");
        System.err.println("   kubectl get svc -n ignite");
        System.err.println();
        System.err.println("4. Test REST API:");
        System.err.println("   curl http://localhost:8080/ignite?cmd=version");
        System.err.println();
        System.err.println("5. If using port-forward instead of NodePort:");
        System.err.println("   kubectl port-forward -n ignite svc/ignite-client-service 10800:10800");
    }
}
