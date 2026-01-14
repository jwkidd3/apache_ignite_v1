package com.example.ignite;

import org.apache.ignite.Ignite;
import org.apache.ignite.Ignition;
import org.apache.ignite.configuration.DeploymentMode;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.spi.communication.tcp.TcpCommunicationSpi;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.ipfinder.vm.TcpDiscoveryVmIpFinder;

import java.util.Arrays;

/**
 * Peer Class Loading Server
 *
 * This server enables peer class loading, which allows it to automatically
 * receive and load classes from client nodes. The server doesn't need to
 * have the compute task classes in its classpath - they will be loaded
 * dynamically from the client.
 *
 * Usage:
 *   mvn exec:java -Dexec.mainClass="com.example.ignite.PeerClassLoadingServer"
 */
public class PeerClassLoadingServer {

    private static TcpDiscoveryVmIpFinder sharedIpFinder = new TcpDiscoveryVmIpFinder(true);

    static {
        sharedIpFinder.setAddresses(Arrays.asList("127.0.0.1:47500", "127.0.0.1:47501", "127.0.0.1:47502"));
    }

    public static void main(String[] args) {
        IgniteConfiguration cfg = new IgniteConfiguration();
        cfg.setIgniteInstanceName("peer-class-server");

        // ENABLE PEER CLASS LOADING
        // This is the key configuration that allows dynamic code deployment
        cfg.setPeerClassLoadingEnabled(true);

        // Deployment mode options:
        // - PRIVATE: Classes are deployed to each node independently (default)
        // - ISOLATED: Each task gets its own class loader
        // - SHARED: Classes are shared across all tasks
        // - CONTINUOUS: Classes persist even after originating node leaves
        cfg.setDeploymentMode(DeploymentMode.SHARED);

        // Discovery configuration
        TcpDiscoverySpi discoverySpi = new TcpDiscoverySpi();
        discoverySpi.setLocalAddress("127.0.0.1");
        discoverySpi.setLocalPort(47500);
        discoverySpi.setLocalPortRange(10);
        discoverySpi.setIpFinder(sharedIpFinder);
        cfg.setDiscoverySpi(discoverySpi);

        // Communication configuration
        TcpCommunicationSpi commSpi = new TcpCommunicationSpi();
        commSpi.setLocalAddress("127.0.0.1");
        commSpi.setLocalPort(47100);
        commSpi.setLocalPortRange(10);
        cfg.setCommunicationSpi(commSpi);

        try (Ignite ignite = Ignition.start(cfg)) {
            System.out.println("\n=== Peer Class Loading Server ===\n");
            System.out.println("Server started: " + ignite.name());
            System.out.println("Peer class loading: ENABLED");
            System.out.println("Deployment mode: " + cfg.getDeploymentMode());
            System.out.println("\nThe server will automatically load classes from clients.");
            System.out.println("It does NOT need compute task classes in its classpath!\n");
            System.out.println("Run the client to send compute tasks:");
            System.out.println("  mvn exec:java -Dexec.mainClass=\"com.example.ignite.PeerClassLoadingClient\"");
            System.out.println("\nServer running. Press Ctrl+C to stop...");

            Thread.currentThread().join();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
