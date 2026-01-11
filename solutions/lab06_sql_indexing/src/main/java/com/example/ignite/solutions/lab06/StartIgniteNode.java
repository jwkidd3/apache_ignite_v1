package com.example.ignite.solutions.lab06;

import org.apache.ignite.Ignite;
import org.apache.ignite.Ignition;
import org.apache.ignite.configuration.ClientConnectorConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;

/**
 * Lab 6 Exercise 3: Start Ignite Node for JDBC
 *
 * This starts an Ignite node that accepts JDBC connections.
 * Run this before running JDBCConnection.java.
 */
public class StartIgniteNode {

    public static void main(String[] args) throws Exception {
        System.out.println("=== Starting Ignite Node for JDBC ===\n");

        IgniteConfiguration cfg = new IgniteConfiguration();
        cfg.setIgniteInstanceName("jdbc-server-node");

        // Configure client connector for JDBC thin driver
        ClientConnectorConfiguration clientCfg = new ClientConnectorConfiguration();
        clientCfg.setPort(10800);  // Default JDBC port
        cfg.setClientConnectorConfiguration(clientCfg);

        try (Ignite ignite = Ignition.start(cfg)) {
            System.out.println("Ignite node started successfully!");
            System.out.println("JDBC URL: jdbc:ignite:thin://127.0.0.1/");
            System.out.println("\nNode is ready to accept JDBC connections.");
            System.out.println("Run JDBCConnection.java in another terminal.");
            System.out.println("\nPress Enter to stop the node...");
            System.in.read();
        }

        System.out.println("Node stopped.");
    }
}
