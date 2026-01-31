package com.example.ignite.solutions.lab13;

import org.apache.ignite.client.IgniteClient;

/**
 * Lab 13 Exercises 11-12: Ignite 3.x Compute Grid
 *
 * Demonstrates:
 * - Compute job concepts in Ignite 3.x (JobTarget, JobDescriptor)
 * - Colocated compute (run job where data lives)
 * - Broadcast jobs (run on all nodes)
 * - Deployment units for code distribution
 * - Feature gaps vs 2.x (no MapReduce, no Service Grid yet)
 * - Contrast with 2.x compute APIs
 *
 * Prerequisites: Running Ignite 3.x cluster
 *
 * Note: Actual compute job execution requires deploying code to cluster nodes
 * via deployment units. This solution explains the concepts and shows the API
 * patterns without requiring deployment unit setup.
 */
public class Lab13Compute {

    public static void main(String[] args) {
        System.out.println("=== Lab 13: Ignite 3.x Compute Grid ===\n");

        try (IgniteClient client = IgniteClient.builder()
                .addresses("127.0.0.1:10800")
                .build()) {

            explainComputeArchitecture();
            explainJobTargetAndDescriptor();
            explainColocatedCompute();
            explainBroadcastJobs();
            explainDeploymentUnits();
            explainFeatureGaps();
            printComputeComparison();

        } catch (Exception e) {
            System.err.println("Could not connect to Ignite 3.x cluster.");
            e.printStackTrace();
        }
    }

    /**
     * Exercise 11: Compute architecture in 3.x
     */
    private static void explainComputeArchitecture() {
        System.out.println("=== Exercise 11: Compute Architecture (3.x) ===\n");

        System.out.println("Ignite 3.x compute is built on two key concepts:\n");
        System.out.println("  1. JobTarget - WHERE the job runs:");
        System.out.println("     - anyNode(clusterNodes)    : Run on any available node");
        System.out.println("     - colocated(tableName, key): Run on node owning the data");
        System.out.println("     - node(specificNode)       : Run on a specific node");
        System.out.println("     - allNodes(clusterNodes)   : Broadcast to all nodes");
        System.out.println();
        System.out.println("  2. JobDescriptor - WHAT to run:");
        System.out.println("     - References a class that implements compute logic");
        System.out.println("     - Specifies deployment unit containing the code");
        System.out.println("     - Defines input/output types");
        System.out.println();
    }

    private static void explainJobTargetAndDescriptor() {
        System.out.println("=== JobTarget and JobDescriptor API ===\n");

        System.out.println("Submitting a compute job (pseudocode):\n");
        System.out.println("  // Define the job descriptor");
        System.out.println("  JobDescriptor<String, Integer> descriptor =");
        System.out.println("      JobDescriptor.builder(MyComputeJob.class)");
        System.out.println("          .units(deploymentUnit)");
        System.out.println("          .build();");
        System.out.println();
        System.out.println("  // Execute on any node");
        System.out.println("  Integer result = client.compute().execute(");
        System.out.println("      JobTarget.anyNode(client.clusterNodes()),");
        System.out.println("      descriptor,");
        System.out.println("      \"input-argument\"");
        System.out.println("  );");
        System.out.println();
        System.out.println("  // Async execution");
        System.out.println("  CompletableFuture<Integer> future =");
        System.out.println("      client.compute().executeAsync(");
        System.out.println("          JobTarget.anyNode(client.clusterNodes()),");
        System.out.println("          descriptor,");
        System.out.println("          \"input-argument\"");
        System.out.println("      );");
        System.out.println();

        System.out.println("Contrast with 2.x:");
        System.out.println("  2.x: ignite.compute().call(IgniteCallable)");
        System.out.println("  2.x: ignite.compute().broadcast(IgniteCallable)");
        System.out.println("  3.x: client.compute().execute(JobTarget, JobDescriptor, args)");
        System.out.println();
    }

    /**
     * Colocated compute
     */
    private static void explainColocatedCompute() {
        System.out.println("=== Colocated Compute (3.x) ===\n");

        System.out.println("Run computation on the node that owns specific data:\n");
        System.out.println("  // Run job on node that owns key in 'orders' table");
        System.out.println("  Tuple key = Tuple.create().set(\"orderId\", 42);");
        System.out.println();
        System.out.println("  String result = client.compute().execute(");
        System.out.println("      JobTarget.colocated(\"orders\", key),");
        System.out.println("      descriptor,");
        System.out.println("      args");
        System.out.println("  );");
        System.out.println();

        System.out.println("Benefits:");
        System.out.println("  - Zero network overhead for data access");
        System.out.println("  - Job runs where the data partition leader is");
        System.out.println("  - Scales linearly with cluster size");
        System.out.println();

        System.out.println("Contrast with 2.x:");
        System.out.println("  2.x: ignite.compute().affinityRun(cacheName, key, runnable)");
        System.out.println("  2.x: ignite.compute().affinityCall(cacheName, key, callable)");
        System.out.println("  3.x: JobTarget.colocated(tableName, keyTuple)");
        System.out.println();
    }

    /**
     * Broadcast jobs
     */
    private static void explainBroadcastJobs() {
        System.out.println("=== Broadcast Jobs (3.x) ===\n");

        System.out.println("Run a job on all cluster nodes:\n");
        System.out.println("  // Broadcast to all nodes");
        System.out.println("  Map<ClusterNode, Integer> results =");
        System.out.println("      client.compute().executeBroadcast(");
        System.out.println("          BroadcastJobTarget.nodes(client.clusterNodes()),");
        System.out.println("          descriptor,");
        System.out.println("          args");
        System.out.println("      );");
        System.out.println();

        System.out.println("Contrast with 2.x:");
        System.out.println("  2.x: ignite.compute().broadcast(() -> { ... })");
        System.out.println("  3.x: client.compute().executeBroadcast(BroadcastJobTarget, descriptor, args)");
        System.out.println();
    }

    /**
     * Deployment units
     */
    private static void explainDeploymentUnits() {
        System.out.println("=== Deployment Units (3.x) ===\n");

        System.out.println("Code must be deployed to cluster nodes before execution:\n");
        System.out.println("  # Deploy a JAR to the cluster via CLI");
        System.out.println("  ignite3 cluster unit deploy \\");
        System.out.println("    --id=my-jobs-v1 \\");
        System.out.println("    --version=1.0.0 \\");
        System.out.println("    --path=/path/to/my-compute-jobs.jar");
        System.out.println();
        System.out.println("  # List deployed units");
        System.out.println("  ignite3 cluster unit list");
        System.out.println();

        System.out.println("In Java, reference the deployment unit:");
        System.out.println("  DeploymentUnit unit = new DeploymentUnit(\"my-jobs-v1\", \"1.0.0\");");
        System.out.println("  JobDescriptor<String, String> descriptor =");
        System.out.println("      JobDescriptor.builder(\"com.example.MyJob\")");
        System.out.println("          .units(unit)");
        System.out.println("          .build();");
        System.out.println();

        System.out.println("Contrast with 2.x:");
        System.out.println("  2.x: Code must be on classpath of all nodes (peer class loading optional)");
        System.out.println("  3.x: Explicit deployment units, versioned, deployable via CLI");
        System.out.println("  3.x: Better isolation, versioned updates, no classpath issues");
        System.out.println();
    }

    /**
     * Exercise 12: Feature gaps
     */
    private static void explainFeatureGaps() {
        System.out.println("=== Exercise 12: Compute Feature Gaps (3.x vs 2.x) ===\n");

        System.out.println("Features available in 2.x but NOT YET in 3.x:\n");
        System.out.println("  1. MapReduce (ComputeTaskAdapter)");
        System.out.println("     2.x: Split work across nodes, reduce results");
        System.out.println("     3.x: Not available; use multiple colocated jobs + client-side reduce");
        System.out.println();
        System.out.println("  2. Service Grid");
        System.out.println("     2.x: Deploy singleton/per-node services to cluster");
        System.out.println("     3.x: Not available; use external service deployment");
        System.out.println();
        System.out.println("  3. Continuous Queries");
        System.out.println("     2.x: ContinuousQuery for real-time cache change notifications");
        System.out.println("     3.x: Not available yet");
        System.out.println();
        System.out.println("  4. Near Cache");
        System.out.println("     2.x: Client-side cache for frequently read data");
        System.out.println("     3.x: Not available yet");
        System.out.println();
        System.out.println("  5. ExecutorService interface");
        System.out.println("     2.x: ignite.executorService() for java.util.concurrent API");
        System.out.println("     3.x: Not available");
        System.out.println();

        System.out.println("Features NEW in 3.x:");
        System.out.println("  - Deployment units (versioned code deployment)");
        System.out.println("  - Job priority and cancellation");
        System.out.println("  - Better error handling with CompletableFuture");
        System.out.println();
    }

    private static void printComputeComparison() {
        System.out.println("=== Compute Grid Comparison ===\n");

        System.out.println("  | Feature           | Ignite 2.x                | Ignite 3.x                |");
        System.out.println("  |-------------------|---------------------------|---------------------------|");
        System.out.println("  | Execute on node   | compute().call(callable)  | compute().execute(target) |");
        System.out.println("  | Broadcast         | compute().broadcast()     | executeBroadcast()        |");
        System.out.println("  | Colocated         | affinityRun/affinityCall  | JobTarget.colocated()     |");
        System.out.println("  | MapReduce         | ComputeTaskAdapter        | Not available             |");
        System.out.println("  | Service Grid      | serviceGrid().deploy()    | Not available             |");
        System.out.println("  | Code deployment   | Classpath / peer loading  | Deployment units          |");
        System.out.println("  | Async result      | IgniteFuture              | CompletableFuture         |");
        System.out.println("  | Cluster groups    | cluster().forAttribute()  | Node selection via filter |");
        System.out.println("  | Continuous Query  | ContinuousQuery           | Not available             |");
        System.out.println();
    }
}
