package com.example.ignite.examples.versiondiff;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.cache.CacheAtomicityMode;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.transactions.Transaction;
import org.apache.ignite.transactions.TransactionConcurrency;
import org.apache.ignite.transactions.TransactionIsolation;

/**
 * Demonstrates Ignite 2.16 Transaction handling.
 *
 * Compare with Ignite 3.x transactions:
 * - 3.x has full SQL transaction support
 * - 3.x can mix SQL and KV operations in same transaction
 * - 3.x uses strictly serializable isolation by default
 */
public class Ignite2TransactionsExample {

    public static void main(String[] args) {
        System.out.println("=== Ignite 2.16 Transaction Example ===\n");

        IgniteConfiguration cfg = new IgniteConfiguration();
        cfg.setIgniteInstanceName("ignite2-tx-demo");
        cfg.setPeerClassLoadingEnabled(true);

        CacheConfiguration<Integer, Integer> accountCacheCfg = new CacheConfiguration<>("AccountCache");
        accountCacheCfg.setAtomicityMode(CacheAtomicityMode.TRANSACTIONAL);
        cfg.setCacheConfiguration(accountCacheCfg);

        try (Ignite ignite = Ignition.start(cfg)) {
            IgniteCache<Integer, Integer> accounts = ignite.cache("AccountCache");

            // Initialize accounts
            accounts.put(1, 1000);
            accounts.put(2, 500);

            System.out.println("Initial balances:");
            System.out.println("  Account 1: $" + accounts.get(1));
            System.out.println("  Account 2: $" + accounts.get(2));

            // PESSIMISTIC transaction
            System.out.println("\nTransfer $200 from Account 1 to Account 2...");

            try (Transaction tx = ignite.transactions().txStart(
                    TransactionConcurrency.PESSIMISTIC,
                    TransactionIsolation.REPEATABLE_READ)) {

                int balance1 = accounts.get(1);
                int balance2 = accounts.get(2);

                accounts.put(1, balance1 - 200);
                accounts.put(2, balance2 + 200);

                tx.commit();
                System.out.println("Transaction committed!");
            }

            System.out.println("\nFinal balances:");
            System.out.println("  Account 1: $" + accounts.get(1));
            System.out.println("  Account 2: $" + accounts.get(2));

            System.out.println("\nPress Enter to stop...");
            System.in.read();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
