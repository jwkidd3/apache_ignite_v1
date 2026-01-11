package com.example.ignite.solutions.lab02;

import org.apache.ignite.Ignite;
import org.apache.ignite.Ignition;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.ipfinder.vm.TcpDiscoveryVmIpFinder;

import java.util.Arrays;

/**
 * Lab 2 Optional: Cloud Discovery Configuration
 *
 * This exercise demonstrates configuring discovery for cloud environments
 * using environment variables.
 */
public class CloudDiscovery {

    public static void main(String[] args) {
        IgniteConfiguration cfg = new IgniteConfiguration();
        cfg.setIgniteInstanceName("cloud-node");
        cfg.setClientMode(false);

        TcpDiscoverySpi discoverySpi = new TcpDiscoverySpi();
        TcpDiscoveryVmIpFinder ipFinder = new TcpDiscoveryVmIpFinder();

        // In production, these would come from environment variables
        String discoveryAddresses = System.getenv("IGNITE_DISCOVERY_ADDRESSES");
        if (discoveryAddresses == null) {
            discoveryAddresses = "127.0.0.1:47500..47509";
            System.out.println("IGNITE_DISCOVERY_ADDRESSES not set, using default: " + discoveryAddresses);
        } else {
            System.out.println("Using IGNITE_DISCOVERY_ADDRESSES: " + discoveryAddresses);
        }

        ipFinder.setAddresses(Arrays.asList(discoveryAddresses.split(",")));
        discoverySpi.setIpFinder(ipFinder);
        discoverySpi.setLocalPort(47500);
        discoverySpi.setLocalPortRange(10);
        cfg.setDiscoverySpi(discoverySpi);

        try (Ignite ignite = Ignition.start(cfg)) {
            System.out.println("\n=== Cloud Discovery Demo ===");
            System.out.println("Node started: " + ignite.name());
            System.out.println("Cluster size: " + ignite.cluster().nodes().size());

            System.out.println("\n=== Environment-Based Configuration ===");
            System.out.println("Set IGNITE_DISCOVERY_ADDRESSES to configure discovery addresses");
            System.out.println("Example: export IGNITE_DISCOVERY_ADDRESSES='10.0.0.1:47500,10.0.0.2:47500'");

            System.out.println("\nPress Enter to stop...");
            System.in.read();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
