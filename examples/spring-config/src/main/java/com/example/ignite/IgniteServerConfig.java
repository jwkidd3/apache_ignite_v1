package com.example.ignite;

import org.apache.ignite.cache.CacheAtomicityMode;
import org.apache.ignite.cache.CacheMode;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.spi.communication.tcp.TcpCommunicationSpi;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.ipfinder.vm.TcpDiscoveryVmIpFinder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

/**
 * Java-based Spring configuration for Ignite Server
 */
@Configuration
public class IgniteServerConfig {

    @Bean
    public TcpDiscoveryVmIpFinder ipFinder() {
        TcpDiscoveryVmIpFinder ipFinder = new TcpDiscoveryVmIpFinder(true);
        ipFinder.setAddresses(Arrays.asList(
            "127.0.0.1:47500",
            "127.0.0.1:47501",
            "127.0.0.1:47502"
        ));
        return ipFinder;
    }

    @Bean
    public TcpDiscoverySpi discoverySpi(TcpDiscoveryVmIpFinder ipFinder) {
        TcpDiscoverySpi discoverySpi = new TcpDiscoverySpi();
        discoverySpi.setLocalAddress("127.0.0.1");
        discoverySpi.setLocalPort(47500);
        discoverySpi.setLocalPortRange(10);
        discoverySpi.setIpFinder(ipFinder);
        return discoverySpi;
    }

    @Bean
    public TcpCommunicationSpi communicationSpi() {
        TcpCommunicationSpi commSpi = new TcpCommunicationSpi();
        commSpi.setLocalAddress("127.0.0.1");
        commSpi.setLocalPort(47100);
        commSpi.setLocalPortRange(10);
        return commSpi;
    }

    @Bean
    public CacheConfiguration<Integer, String> productsCache() {
        CacheConfiguration<Integer, String> cfg = new CacheConfiguration<>("products");
        cfg.setCacheMode(CacheMode.PARTITIONED);
        cfg.setBackups(1);
        cfg.setAtomicityMode(CacheAtomicityMode.ATOMIC);
        return cfg;
    }

    @Bean
    public CacheConfiguration<Integer, String> categoriesCache() {
        CacheConfiguration<Integer, String> cfg = new CacheConfiguration<>("categories");
        cfg.setCacheMode(CacheMode.REPLICATED);
        cfg.setAtomicityMode(CacheAtomicityMode.ATOMIC);
        return cfg;
    }

    @Bean
    public IgniteConfiguration igniteConfiguration(
            TcpDiscoverySpi discoverySpi,
            TcpCommunicationSpi communicationSpi,
            CacheConfiguration<Integer, String> productsCache,
            CacheConfiguration<Integer, String> categoriesCache) {

        IgniteConfiguration cfg = new IgniteConfiguration();
        cfg.setIgniteInstanceName("java-server-node");
        cfg.setDiscoverySpi(discoverySpi);
        cfg.setCommunicationSpi(communicationSpi);
        cfg.setCacheConfiguration(productsCache, categoriesCache);
        return cfg;
    }
}
