package com.example.ignite.solutions.lab12;

import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.ipfinder.vm.TcpDiscoveryVmIpFinder;

import java.util.Arrays;

/**
 * Lab 12 Exercise 1: Security Overview
 *
 * Demonstrates:
 * - Security configuration layers
 * - Security best practices
 * - Production security checklist
 */
public class Lab12SecurityOverview {

    public static void main(String[] args) {
        System.out.println("=== Production Security Overview ===\n");

        IgniteConfiguration cfg = new IgniteConfiguration();
        cfg.setIgniteInstanceName("secure-node");

        // Configure discovery
        TcpDiscoverySpi spi = new TcpDiscoverySpi();
        TcpDiscoveryVmIpFinder ipFinder = new TcpDiscoveryVmIpFinder();
        ipFinder.setAddresses(Arrays.asList("127.0.0.1:47500..47509"));
        spi.setIpFinder(ipFinder);
        cfg.setDiscoverySpi(spi);

        System.out.println("=== Security Configuration Layers ===");
        System.out.println("1. Network Security - Firewall rules, port restrictions");
        System.out.println("2. Transport Security - SSL/TLS encryption");
        System.out.println("3. Authentication - User credentials verification");
        System.out.println("4. Authorization - Permission-based access control");
        System.out.println("5. Audit Logging - Track security events\n");

        System.out.println("=== Security Best Practices ===");
        System.out.println("- Use strong passwords (min 12 characters)");
        System.out.println("- Enable SSL/TLS for all communication");
        System.out.println("- Implement role-based access control");
        System.out.println("- Audit security events");
        System.out.println("- Rotate credentials regularly (every 90 days)");
        System.out.println("- Use secure communication between nodes");
        System.out.println("- Keep Ignite version updated");

        System.out.println("\n=== Production Security Checklist ===");
        System.out.println("[ ] Authentication enabled");
        System.out.println("[ ] SSL/TLS configured");
        System.out.println("[ ] Firewall rules in place");
        System.out.println("[ ] Audit logging enabled");
        System.out.println("[ ] Regular security updates");
        System.out.println("[ ] Encrypted communication");
        System.out.println("[ ] Secure credential storage");

        System.out.println("\n=== Required Ports for Apache Ignite ===");
        System.out.println("47500-47509 - Discovery (TCP)");
        System.out.println("47100       - Communication (TCP)");
        System.out.println("10800       - Thin client connections");
        System.out.println("8080        - REST API");
        System.out.println("11211       - Memcache protocol");

        System.out.println("\n=== Network Security Configuration ===");
        System.out.println("1. Configure firewall to allow only required ports");
        System.out.println("2. Use private network for cluster communication");
        System.out.println("3. Separate client and internal cluster traffic");
        System.out.println("4. Use VPN for remote administration");
        System.out.println("5. Implement network segmentation");

        System.out.println("\n=== Security Monitoring ===");
        System.out.println("1. Monitor failed login attempts");
        System.out.println("2. Track permission denied events");
        System.out.println("3. Alert on unusual access patterns");
        System.out.println("4. Log all administrative operations");
        System.out.println("5. Review security logs regularly");
    }
}
