package com.example.ignite.examples.versiondiff;

import org.apache.ignite.Ignite;
import org.apache.ignite.Ignition;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.ipfinder.vm.TcpDiscoveryVmIpFinder;

import java.util.Arrays;

/**
 * Demonstrates Ignite 2.16 programmatic configuration.
 *
 * Compare with Ignite 3.x HOCON configuration:
 *
 * ignite {
 *   network {
 *     port: 3344
 *     nodeFinder { netClusterNodes: ["localhost:3344"] }
 *   }
 *   cluster { name: "myCluster" }
 * }
 */
public class Ignite2ConfigExample {

    public static void main(String[] args) {
        System.out.println("=== Ignite 2.16 Configuration Example ===\n");

        IgniteConfiguration cfg = new IgniteConfiguration();
        cfg.setIgniteInstanceName("ignite2-config-demo");
        cfg.setPeerClassLoadingEnabled(true);

        // Discovery configuration
        TcpDiscoverySpi discoverySpi = new TcpDiscoverySpi();
        TcpDiscoveryVmIpFinder ipFinder = new TcpDiscoveryVmIpFinder();
        ipFinder.setAddresses(Arrays.asList("127.0.0.1:47500..47509"));
        discoverySpi.setIpFinder(ipFinder);
        cfg.setDiscoverySpi(discoverySpi);

        // Cache configuration
        CacheConfiguration<Integer, String> cacheCfg = new CacheConfiguration<>("demoCache");
        cacheCfg.setBackups(1);
        cfg.setCacheConfiguration(cacheCfg);

        System.out.println("Configuration:");
        System.out.println("  Instance Name: " + cfg.getIgniteInstanceName());
        System.out.println("  Peer Class Loading: " + cfg.isPeerClassLoadingEnabled());

        try (Ignite ignite = Ignition.start(cfg)) {
            System.out.println("\nIgnite 2.16 node started!");
            System.out.println("  Node ID: " + ignite.cluster().localNode().id());
            System.out.println("  Cluster size: " + ignite.cluster().nodes().size());

            System.out.println("\nPress Enter to stop...");
            System.in.read();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
