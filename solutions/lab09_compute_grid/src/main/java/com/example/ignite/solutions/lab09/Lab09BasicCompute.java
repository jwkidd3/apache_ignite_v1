package com.example.ignite.solutions.lab09;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCompute;
import org.apache.ignite.Ignition;
import org.apache.ignite.lang.IgniteCallable;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Lab 09 Exercise 1: Basic Compute Operations
 *
 * Demonstrates:
 * - Simple runnable tasks
 * - Broadcasting to all nodes
 * - Callables with return values
 * - Multiple callable execution
 * - Load balancing across cluster
 */
public class Lab09BasicCompute {

    public static void main(String[] args) {
        try (Ignite ignite = Ignition.start()) {
            System.out.println("=== Basic Compute Grid Lab ===\n");
            System.out.println("Cluster nodes: " + ignite.cluster().nodes().size());

            IgniteCompute compute = ignite.compute();

            // Example 1: Run a simple task
            System.out.println("\n=== Example 1: Simple Runnable ===");
            compute.run(() -> {
                System.out.println("Hello from node: " +
                    Ignition.ignite().cluster().localNode().id());
            });

            // Example 2: Broadcast to all nodes
            System.out.println("\n=== Example 2: Broadcast to All Nodes ===");
            compute.broadcast(() -> {
                System.out.println("[" +
                    Ignition.ignite().cluster().localNode().consistentId() +
                    "] Received broadcast message");
            });

            Thread.sleep(1000);

            // Example 3: Callable with return value
            System.out.println("\n=== Example 3: Callable with Return Value ===");
            String result = compute.call(() -> {
                return "Computed on node: " +
                    Ignition.ignite().cluster().localNode().consistentId();
            });
            System.out.println("Result: " + result);

            // Example 4: Multiple callables
            System.out.println("\n=== Example 4: Multiple Callables ===");
            Collection<IgniteCallable<Integer>> calls = new ArrayList<>();

            for (int i = 1; i <= 5; i++) {
                final int num = i;
                calls.add(() -> {
                    System.out.println("Computing square of " + num +
                        " on node " + Ignition.ignite().cluster().localNode().consistentId());
                    return num * num;
                });
            }

            Collection<Integer> results = compute.call(calls);
            System.out.println("\nResults: " + results);

            // Example 5: Execute with load balancing
            System.out.println("\n=== Example 5: Load Balancing ===");
            for (int i = 0; i < 10; i++) {
                final int taskNum = i;
                compute.run(() -> {
                    System.out.println("Task " + taskNum + " on node " +
                        Ignition.ignite().cluster().localNode().consistentId());
                });
            }

            Thread.sleep(1000);

            System.out.println("\n=== Compute Grid Features ===");
            System.out.println("- Automatic load balancing");
            System.out.println("- Failover support");
            System.out.println("- Task distribution across nodes");
            System.out.println("- Parallel execution");

            System.out.println("\nPress Enter to exit...");
            System.in.read();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
