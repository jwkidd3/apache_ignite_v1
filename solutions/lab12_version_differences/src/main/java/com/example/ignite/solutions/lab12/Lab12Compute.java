package com.example.ignite.solutions.lab12;

import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.compute.JobDescriptor;
import org.apache.ignite.compute.JobTarget;
import org.apache.ignite.network.ClusterNode;
import org.apache.ignite.sql.ResultSet;
import org.apache.ignite.sql.SqlRow;
import org.apache.ignite.table.Tuple;

import java.util.Collection;
import java.util.List;

/**
 * Lab 12 Exercises 11-12: Ignite 3.x Compute Grid (Hands-On)
 *
 * Demonstrates:
 * - Enumerating cluster nodes
 * - Creating JobTarget instances (anyNode, node, colocated)
 * - Submitting a compute job (with expected failure due to missing deployment units)
 * - Using distributed SQL as a compute workaround
 * - Feature gap comparison between 2.x and 3.x
 *
 * Prerequisites: Running Ignite 3.x cluster
 */
public class Lab12Compute {

    public static void main(String[] args) {
        System.out.println("=== Lab 12: Ignite 3.x Compute Grid (Hands-On) ===\n");

        try (IgniteClient client = IgniteClient.builder()
                .addresses("127.0.0.1:10800")
                .build()) {

            demonstrateNodeEnumeration(client);
            demonstrateJobTargetCreation(client);
            demonstrateJobSubmission(client);
            demonstrateSqlAsCompute(client);
            printFeatureGapComparison();

        } catch (Exception e) {
            System.err.println("Could not connect to Ignite 3.x cluster.");
            e.printStackTrace();
        }
    }

    // ---------------------------------------------------------------
    // Exercise 11a: Enumerate cluster nodes
    // ---------------------------------------------------------------

    /**
     * Discovers and prints all nodes visible to the client.
     */
    private static void demonstrateNodeEnumeration(IgniteClient client) {
        System.out.println("=== Node Enumeration ===\n");

        Collection<ClusterNode> nodes = client.clusterNodes();

        System.out.println("Cluster contains " + nodes.size() + " node(s):\n");
        for (ClusterNode node : nodes) {
            System.out.println("  Node name : " + node.name());
            System.out.println("  Node ID   : " + node.id());
            System.out.println("  Address   : " + node.address());
            System.out.println();
        }
    }

    // ---------------------------------------------------------------
    // Exercise 11b: Create JobTarget instances
    // ---------------------------------------------------------------

    /**
     * Shows how to build the three main JobTarget types:
     *   anyNode  - pick any cluster node
     *   node     - target a specific node
     *   colocated - target the node owning a particular key
     */
    private static void demonstrateJobTargetCreation(IgniteClient client) {
        System.out.println("=== JobTarget Creation ===\n");

        Collection<ClusterNode> nodes = client.clusterNodes();

        // 1. anyNode - run on any available node
        JobTarget anyTarget = JobTarget.anyNode(nodes);
        System.out.println("1) JobTarget.anyNode(clusterNodes)  -> " + anyTarget);

        // 2. node - run on a specific node
        ClusterNode firstNode = nodes.iterator().next();
        JobTarget nodeTarget = JobTarget.node(firstNode);
        System.out.println("2) JobTarget.node(\"" + firstNode.name() + "\") -> " + nodeTarget);

        // 3. colocated - run on the node that owns a key in a table
        //    We need a table to exist; create Person if it does not already exist.
        ensurePersonTable(client);
        Tuple personKey = Tuple.create().set("personId", 1);
        JobTarget colocatedTarget = JobTarget.colocated("Person", personKey);
        System.out.println("3) JobTarget.colocated(\"Person\", key) -> " + colocatedTarget);
        System.out.println();

        System.out.println("Contrast with Ignite 2.x:");
        System.out.println("  2.x: ignite.compute().affinityRun(cacheName, key, runnable)");
        System.out.println("  3.x: JobTarget.colocated(tableName, keyTuple)");
        System.out.println();
    }

    // ---------------------------------------------------------------
    // Exercise 11c: Attempt to submit a compute job
    // ---------------------------------------------------------------

    /**
     * Tries to execute a compute job using a fake class name.
     * This will fail because no deployment unit contains the class,
     * illustrating the deployment-unit requirement in Ignite 3.x.
     */
    private static void demonstrateJobSubmission(IgniteClient client) {
        System.out.println("=== Job Submission (expected failure) ===\n");

        Collection<ClusterNode> nodes = client.clusterNodes();

        // Build a descriptor that references a class not deployed to the cluster
        JobDescriptor<String, String> descriptor = JobDescriptor
                .<String, String>builder("com.example.fake.NonExistentJob")
                .build();

        try {
            String result = client.compute().execute(
                    JobTarget.anyNode(nodes),
                    descriptor,
                    "hello"
            );
            // Unlikely to reach here
            System.out.println("Unexpected result: " + result);
        } catch (Exception e) {
            System.out.println("Expected exception caught!");
            System.out.println("  Type   : " + e.getClass().getSimpleName());
            System.out.println("  Message: " + e.getMessage());
            System.out.println();
            System.out.println("This confirms that compute jobs require the job class to be");
            System.out.println("deployed to the cluster via deployment units.\n");
            System.out.println("Deploy a unit with the CLI:");
            System.out.println("  ignite3 cluster unit deploy \\");
            System.out.println("    --id=my-jobs --version=1.0.0 \\");
            System.out.println("    --path=/path/to/my-compute-jobs.jar");
        }
        System.out.println();
    }

    // ---------------------------------------------------------------
    // SQL as a distributed compute workaround
    // ---------------------------------------------------------------

    /**
     * Demonstrates using distributed SQL queries as an alternative to
     * custom compute jobs for aggregation and analytics workloads.
     */
    private static void demonstrateSqlAsCompute(IgniteClient client) {
        System.out.println("=== SQL as Distributed Compute Workaround ===\n");

        // Make sure we have some data to query
        ensurePersonTable(client);
        insertSamplePersons(client);

        System.out.println("Running distributed aggregation queries on Person table:\n");

        // Aggregation 1: count
        try (ResultSet<SqlRow> rs = client.sql().execute(null,
                "SELECT COUNT(*) AS cnt FROM Person")) {
            if (rs.hasNext()) {
                System.out.println("  Total persons : " + rs.next().longValue("cnt"));
            }
        }

        // Aggregation 2: group by city
        System.out.println("  Persons by city:");
        try (ResultSet<SqlRow> rs = client.sql().execute(null,
                "SELECT city, COUNT(*) AS cnt FROM Person GROUP BY city ORDER BY cnt DESC")) {
            while (rs.hasNext()) {
                SqlRow row = rs.next();
                System.out.println("    " + row.stringValue("city") + " : " + row.longValue("cnt"));
            }
        }

        // Aggregation 3: average age (if column exists)
        try (ResultSet<SqlRow> rs = client.sql().execute(null,
                "SELECT AVG(age) AS avgAge FROM Person")) {
            if (rs.hasNext()) {
                System.out.println("  Average age   : " + rs.next().doubleValue("avgAge"));
            }
        } catch (Exception e) {
            // age column may not exist - that is fine
        }

        System.out.println();
        System.out.println("SQL queries are executed across all partitions in the cluster,");
        System.out.println("making them an effective distributed compute mechanism when");
        System.out.println("custom job deployment is not feasible.\n");
    }

    // ---------------------------------------------------------------
    // Exercise 12: Feature gap comparison
    // ---------------------------------------------------------------

    private static void printFeatureGapComparison() {
        System.out.println("=== Compute Feature Gap Comparison (3.x vs 2.x) ===\n");

        System.out.println("  | Feature           | Ignite 2.x                | Ignite 3.x                |");
        System.out.println("  |-------------------|---------------------------|---------------------------|");
        System.out.println("  | Execute on node   | compute().call(callable)  | compute().execute(target) |");
        System.out.println("  | Broadcast         | compute().broadcast()     | executeBroadcast()        |");
        System.out.println("  | Colocated         | affinityRun/affinityCall  | JobTarget.colocated()     |");
        System.out.println("  | MapReduce         | ComputeTaskAdapter        | submitMapReduce()         |");
        System.out.println("  | Service Grid      | serviceGrid().deploy()    | Not available yet         |");
        System.out.println("  | Code deployment   | Classpath / peer loading  | Deployment units (CLI)    |");
        System.out.println("  | Async result      | IgniteFuture              | CompletableFuture         |");
        System.out.println("  | Cluster groups    | cluster().forAttribute()  | Node selection via filter |");
        System.out.println("  | Continuous Query  | ContinuousQuery           | Not available yet         |");
        System.out.println("  | Near Cache        | NearCacheConfiguration    | Not available yet         |");
        System.out.println("  | ExecutorService   | ignite.executorService()  | Not available             |");
        System.out.println();

        System.out.println("New in 3.x:");
        System.out.println("  - Deployment units (versioned code deployment via CLI)");
        System.out.println("  - Job priority and cancellation via CancellationToken");
        System.out.println("  - CompletableFuture-based async API");
        System.out.println("  - MapReduceTask API (submitMapReduce / executeMapReduce)");
        System.out.println();
    }

    // ---------------------------------------------------------------
    // Helper methods
    // ---------------------------------------------------------------

    private static void ensurePersonTable(IgniteClient client) {
        try {
            client.sql().execute(null,
                    "CREATE TABLE IF NOT EXISTS Person ("
                    + "  personId INT PRIMARY KEY,"
                    + "  name     VARCHAR,"
                    + "  city     VARCHAR,"
                    + "  age      INT"
                    + ")");
        } catch (Exception e) {
            // Table may already exist
        }
    }

    private static void insertSamplePersons(IgniteClient client) {
        String[][] people = {
            {"1", "Alice",   "New York",     "30"},
            {"2", "Bob",     "San Francisco","25"},
            {"3", "Charlie", "New York",     "35"},
            {"4", "Diana",   "Chicago",      "28"},
            {"5", "Eve",     "San Francisco","32"}
        };
        for (String[] p : people) {
            try {
                client.sql().execute(null,
                    "INSERT INTO Person (personId, name, city, age) VALUES (?, ?, ?, ?)",
                    Integer.parseInt(p[0]), p[1], p[2], Integer.parseInt(p[3]));
            } catch (Exception e) {
                // Row may already exist
            }
        }
    }
}
