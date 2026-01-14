package com.example.ignite;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCompute;
import org.apache.ignite.Ignition;
import org.apache.ignite.configuration.DeploymentMode;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.lang.IgniteCallable;
import org.apache.ignite.lang.IgniteRunnable;
import org.apache.ignite.spi.communication.tcp.TcpCommunicationSpi;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.ipfinder.vm.TcpDiscoveryVmIpFinder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Peer Class Loading Client
 *
 * This client demonstrates sending compute tasks to the server.
 * The task classes are automatically deployed to the server via
 * peer class loading - the server doesn't need these classes
 * in its classpath.
 *
 * Start PeerClassLoadingServer first, then run this client.
 *
 * Usage:
 *   mvn exec:java -Dexec.mainClass="com.example.ignite.PeerClassLoadingClient"
 */
public class PeerClassLoadingClient {

    private static TcpDiscoveryVmIpFinder sharedIpFinder = new TcpDiscoveryVmIpFinder(true);

    static {
        sharedIpFinder.setAddresses(Arrays.asList("127.0.0.1:47500", "127.0.0.1:47501", "127.0.0.1:47502"));
    }

    public static void main(String[] args) {
        IgniteConfiguration cfg = new IgniteConfiguration();
        cfg.setIgniteInstanceName("peer-class-client");
        cfg.setClientMode(true);

        // Client also needs peer class loading enabled
        cfg.setPeerClassLoadingEnabled(true);
        cfg.setDeploymentMode(DeploymentMode.SHARED);

        // Discovery configuration
        TcpDiscoverySpi discoverySpi = new TcpDiscoverySpi();
        discoverySpi.setLocalAddress("127.0.0.1");
        discoverySpi.setIpFinder(sharedIpFinder);
        cfg.setDiscoverySpi(discoverySpi);

        // Communication configuration
        TcpCommunicationSpi commSpi = new TcpCommunicationSpi();
        commSpi.setLocalAddress("127.0.0.1");
        commSpi.setLocalPort(47100);
        commSpi.setLocalPortRange(10);
        cfg.setCommunicationSpi(commSpi);

        try (Ignite ignite = Ignition.start(cfg)) {
            System.out.println("\n=== Peer Class Loading Client ===\n");
            System.out.println("Client connected: " + ignite.name());
            System.out.println("Server nodes: " + ignite.cluster().forServers().nodes().size());

            IgniteCompute compute = ignite.compute(ignite.cluster().forServers());

            // Demo 1: Simple IgniteRunnable (broadcast to all servers)
            System.out.println("\n=== Demo 1: Broadcast IgniteRunnable ===");
            System.out.println("Sending runnable to all server nodes...");

            compute.broadcast(new SimpleRunnable("Hello from client!"));

            System.out.println("Runnable executed on server (check server console)");

            // Demo 2: IgniteCallable with return value
            System.out.println("\n=== Demo 2: IgniteCallable with Result ===");
            System.out.println("Calling task on server node...");

            String result = compute.call(new SystemInfoCallable());
            System.out.println("Result from server: " + result);

            // Demo 3: Distributed computation (map-reduce style)
            System.out.println("\n=== Demo 3: Distributed Calculation ===");
            System.out.println("Distributing factorial calculations to server...");

            Collection<IgniteCallable<Long>> calls = new ArrayList<>();
            for (int i = 5; i <= 10; i++) {
                calls.add(new FactorialCallable(i));
            }

            Collection<Long> results = compute.call(calls);
            System.out.println("Factorials calculated on server:");
            int n = 5;
            for (Long factorial : results) {
                System.out.println("  " + n + "! = " + factorial);
                n++;
            }

            // Demo 4: Custom task class
            System.out.println("\n=== Demo 4: Custom Task Class ===");
            System.out.println("Sending custom word count task...");

            String text = "Apache Ignite provides peer class loading for dynamic deployment";
            Integer wordCount = compute.call(new WordCountCallable(text));
            System.out.println("Word count result: " + wordCount + " words");

            System.out.println("\n=== Peer Class Loading Benefits ===");
            System.out.println("- No need to deploy task classes to all nodes");
            System.out.println("- Dynamic code updates without cluster restart");
            System.out.println("- Simplified development and testing");
            System.out.println("- Great for iterative development");

            System.out.println("\n=== Client Complete ===");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Simple runnable that executes on remote nodes.
     * This class is automatically deployed via peer class loading.
     */
    private static class SimpleRunnable implements IgniteRunnable {
        private final String message;

        public SimpleRunnable(String message) {
            this.message = message;
        }

        @Override
        public void run() {
            System.out.println("[PEER-LOADED] Received message: " + message);
            System.out.println("[PEER-LOADED] Executing on: " + Thread.currentThread().getName());
        }
    }

    /**
     * Callable that returns system information from the remote node.
     */
    private static class SystemInfoCallable implements IgniteCallable<String> {
        @Override
        public String call() {
            return String.format("OS: %s, Java: %s, Processors: %d",
                System.getProperty("os.name"),
                System.getProperty("java.version"),
                Runtime.getRuntime().availableProcessors());
        }
    }

    /**
     * Callable that calculates factorial on remote node.
     */
    private static class FactorialCallable implements IgniteCallable<Long> {
        private final int n;

        public FactorialCallable(int n) {
            this.n = n;
        }

        @Override
        public Long call() {
            long result = 1;
            for (int i = 2; i <= n; i++) {
                result *= i;
            }
            return result;
        }
    }

    /**
     * Callable that counts words in a string.
     */
    private static class WordCountCallable implements IgniteCallable<Integer> {
        private final String text;

        public WordCountCallable(String text) {
            this.text = text;
        }

        @Override
        public Integer call() {
            if (text == null || text.trim().isEmpty()) {
                return 0;
            }
            return text.trim().split("\\s+").length;
        }
    }
}
