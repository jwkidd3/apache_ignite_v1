package com.example.ignite;

import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.spi.communication.tcp.TcpCommunicationSpi;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.ipfinder.vm.TcpDiscoveryVmIpFinder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

/**
 * Java-based Spring configuration for Ignite Client
 */
@Configuration
public class IgniteClientConfig {

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
    public IgniteConfiguration igniteConfiguration(
            TcpDiscoverySpi discoverySpi,
            TcpCommunicationSpi communicationSpi) {

        IgniteConfiguration cfg = new IgniteConfiguration();
        cfg.setIgniteInstanceName("java-client-node");
        cfg.setClientMode(true);
        cfg.setDiscoverySpi(discoverySpi);
        cfg.setCommunicationSpi(communicationSpi);
        return cfg;
    }
}
