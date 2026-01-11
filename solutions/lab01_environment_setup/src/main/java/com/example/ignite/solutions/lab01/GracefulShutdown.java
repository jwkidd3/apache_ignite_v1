package com.example.ignite.solutions.lab01;

import org.apache.ignite.Ignite;
import org.apache.ignite.Ignition;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.events.Event;
import org.apache.ignite.events.EventType;
import org.apache.ignite.lang.IgnitePredicate;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Lab 1 Challenge 2: Graceful Shutdown
 *
 * This exercise demonstrates proper shutdown hooks and
 * graceful termination of Ignite nodes.
 */
public class GracefulShutdown {

    private static final AtomicBoolean running = new AtomicBoolean(true);
    private static final CountDownLatch shutdownLatch = new CountDownLatch(1);

    public static void main(String[] args) {
        IgniteConfiguration cfg = new IgniteConfiguration();
        cfg.setIgniteInstanceName("graceful-shutdown-node");
        cfg.setPeerClassLoadingEnabled(true);

        // Enable discovery events
        cfg.setIncludeEventTypes(
            EventType.EVT_NODE_JOINED,
            EventType.EVT_NODE_LEFT,
            EventType.EVT_NODE_FAILED
        );

        System.out.println("=== Challenge 2: Graceful Shutdown Demo ===\n");

        Ignite ignite = null;

        try {
            ignite = Ignition.start(cfg);
            final Ignite finalIgnite = ignite;

            // Register event listeners for node lifecycle
            ignite.events().localListen(new IgnitePredicate<Event>() {
                @Override
                public boolean apply(Event evt) {
                    System.out.println("[EVENT] " + evt.name() + " at " +
                        java.time.LocalTime.now());
                    return true; // Continue listening
                }
            }, EventType.EVT_NODE_JOINED, EventType.EVT_NODE_LEFT, EventType.EVT_NODE_FAILED);

            // Register shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("\n[SHUTDOWN HOOK] Initiating graceful shutdown...");
                running.set(false);

                if (finalIgnite != null && !finalIgnite.cluster().nodes().isEmpty()) {
                    System.out.println("[SHUTDOWN HOOK] Performing pre-shutdown tasks...");

                    // Log final metrics before shutdown
                    try {
                        System.out.println("[SHUTDOWN HOOK] Final cluster size: "
                            + finalIgnite.cluster().nodes().size());
                        System.out.println("[SHUTDOWN HOOK] Total jobs executed: "
                            + finalIgnite.cluster().metrics().getTotalExecutedJobs());
                    } catch (Exception e) {
                        System.out.println("[SHUTDOWN HOOK] Could not retrieve final metrics.");
                    }

                    System.out.println("[SHUTDOWN HOOK] Stopping Ignite node...");
                    finalIgnite.close();
                    System.out.println("[SHUTDOWN HOOK] Ignite node stopped successfully.");
                }

                shutdownLatch.countDown();
            }));

            System.out.println("Node started successfully.");
            System.out.println("Shutdown hook registered.");
            System.out.println("\nNode is running. Use Ctrl+C or send SIGTERM for graceful shutdown.");
            System.out.println("Alternatively, press Enter to trigger manual shutdown.\n");

            // Main loop - simulate work
            Thread monitorThread = new Thread(() -> {
                while (running.get()) {
                    try {
                        Thread.sleep(5000);
                        if (running.get()) {
                            System.out.println("[HEARTBEAT] Node active. Cluster size: "
                                + finalIgnite.cluster().nodes().size()
                                + " | Time: " + java.time.LocalTime.now());
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            });
            monitorThread.setDaemon(true);
            monitorThread.start();

            // Wait for Enter key for manual shutdown
            System.in.read();
            System.out.println("\n[MAIN] Manual shutdown requested...");
            running.set(false);

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (ignite != null) {
                System.out.println("[MAIN] Closing Ignite instance...");
                ignite.close();
                System.out.println("[MAIN] Ignite instance closed.");
            }
        }

        System.out.println("[MAIN] Application terminated gracefully.");
    }
}
