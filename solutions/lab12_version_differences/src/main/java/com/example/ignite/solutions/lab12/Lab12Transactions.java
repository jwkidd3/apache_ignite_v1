package com.example.ignite.solutions.lab12;

import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.sql.ResultSet;
import org.apache.ignite.sql.SqlRow;
import org.apache.ignite.table.KeyValueView;
import org.apache.ignite.table.RecordView;
import org.apache.ignite.table.Table;
import org.apache.ignite.table.Tuple;
import org.apache.ignite.tx.Transaction;

/**
 * Lab 12 Exercises 9-10: Ignite 3.x Transaction Model
 *
 * Demonstrates:
 * - Strictly serializable transactions (3.x only model)
 * - Manual transaction control: begin/commit/rollback
 * - runInTransaction automatic management
 * - Mixed SQL + KV operations in same transaction
 * - Transaction with RecordView
 * - Contrast with 2.x PESSIMISTIC/OPTIMISTIC model
 *
 * Prerequisites: Running Ignite 3.x cluster
 */
public class Lab12Transactions {

    public static void main(String[] args) {
        System.out.println("=== Lab 12: Ignite 3.x Transactions ===\n");

        try (IgniteClient client = IgniteClient.builder()
                .addresses("127.0.0.1:10800")
                .build()) {

            setupTables(client);
            demonstrateManualTransaction(client);
            demonstrateRunInTransaction(client);
            demonstrateMixedSqlKvTransaction(client);
            demonstrateRollback(client);
            printTransactionComparison();

            // Cleanup
            client.sql().executeScript(
                "DROP TABLE IF EXISTS accounts; DROP TABLE IF EXISTS tx_log");

        } catch (Exception e) {
            System.err.println("Could not connect to Ignite 3.x cluster.");
            e.printStackTrace();
        }
    }

    private static void setupTables(IgniteClient client) {
        client.sql().executeScript(
            "CREATE TABLE IF NOT EXISTS accounts (" +
            "  id INT PRIMARY KEY," +
            "  name VARCHAR," +
            "  balance DOUBLE" +
            ")");
        client.sql().executeScript(
            "CREATE TABLE IF NOT EXISTS tx_log (" +
            "  id INT PRIMARY KEY," +
            "  description VARCHAR" +
            ")");

        // Seed data
        Table accounts = client.tables().table("ACCOUNTS");
        RecordView<Tuple> view = accounts.recordView();
        view.upsert(null, Tuple.create().set("id", 1).set("name", "Alice").set("balance", 1000.0));
        view.upsert(null, Tuple.create().set("id", 2).set("name", "Bob").set("balance", 500.0));
        System.out.println("Setup: Created accounts table with Alice=$1000, Bob=$500\n");
    }

    /**
     * Exercise 9: Manual transaction control
     */
    private static void demonstrateManualTransaction(IgniteClient client) {
        System.out.println("=== Exercise 9: Manual Transaction Control ===\n");

        Table accounts = client.tables().table("ACCOUNTS");
        RecordView<Tuple> view = accounts.recordView();

        // Begin a transaction explicitly
        Transaction tx = client.transactions().begin();
        try {
            // Read Alice's balance within the transaction
            Tuple aliceKey = Tuple.create().set("id", 1);
            Tuple alice = view.get(tx, aliceKey);
            double aliceBalance = alice.doubleValue("balance");

            // Read Bob's balance within the transaction
            Tuple bobKey = Tuple.create().set("id", 2);
            Tuple bob = view.get(tx, bobKey);
            double bobBalance = bob.doubleValue("balance");

            // Transfer $200 from Alice to Bob
            double transferAmount = 200.0;
            view.upsert(tx, Tuple.create()
                .set("id", 1).set("name", "Alice").set("balance", aliceBalance - transferAmount));
            view.upsert(tx, Tuple.create()
                .set("id", 2).set("name", "Bob").set("balance", bobBalance + transferAmount));

            // Commit
            tx.commit();
            System.out.println("  Transferred $200 from Alice to Bob (committed)");
        } catch (Exception e) {
            tx.rollback();
            System.out.println("  Transaction rolled back: " + e.getMessage());
        }

        // Verify
        Tuple alice = view.get(null, Tuple.create().set("id", 1));
        Tuple bob = view.get(null, Tuple.create().set("id", 2));
        System.out.println("  Alice balance: $" + alice.doubleValue("balance"));
        System.out.println("  Bob balance: $" + bob.doubleValue("balance"));
        System.out.println();

        System.out.println("Key points:");
        System.out.println("  - client.transactions().begin() starts a new transaction");
        System.out.println("  - Pass tx as first arg to all view operations");
        System.out.println("  - tx.commit() or tx.rollback() to finish");
        System.out.println("  - Strictly serializable: all transactions see consistent data");
        System.out.println();
    }

    /**
     * Exercise 9b: runInTransaction (recommended approach)
     */
    private static void demonstrateRunInTransaction(IgniteClient client) {
        System.out.println("=== runInTransaction (Recommended) ===\n");

        Table accounts = client.tables().table("ACCOUNTS");
        RecordView<Tuple> view = accounts.recordView();

        // runInTransaction handles commit/rollback automatically
        client.transactions().runInTransaction(tx -> {
            Tuple alice = view.get(tx, Tuple.create().set("id", 1));
            double newBalance = alice.doubleValue("balance") + 100.0;
            view.upsert(tx, Tuple.create()
                .set("id", 1).set("name", "Alice").set("balance", newBalance));
        });

        Tuple alice = view.get(null, Tuple.create().set("id", 1));
        System.out.println("  Added $100 to Alice via runInTransaction");
        System.out.println("  Alice balance: $" + alice.doubleValue("balance"));
        System.out.println();

        System.out.println("runInTransaction advantages:");
        System.out.println("  - Auto-commit on success, auto-rollback on exception");
        System.out.println("  - Supports automatic retries (configurable)");
        System.out.println("  - Cleaner code, no try/catch boilerplate");
        System.out.println();
    }

    /**
     * Exercise 10: Mixed SQL + KV in same transaction
     */
    private static void demonstrateMixedSqlKvTransaction(IgniteClient client) {
        System.out.println("=== Exercise 10: Mixed SQL + KV Transaction ===\n");

        Table accounts = client.tables().table("ACCOUNTS");
        RecordView<Tuple> view = accounts.recordView();

        int logId = 1;
        client.transactions().runInTransaction(tx -> {
            // KV operation: update balance
            Tuple bob = view.get(tx, Tuple.create().set("id", 2));
            double newBalance = bob.doubleValue("balance") + 50.0;
            view.upsert(tx, Tuple.create()
                .set("id", 2).set("name", "Bob").set("balance", newBalance));

            // SQL operation in same transaction: insert log entry
            client.sql().execute(tx,
                "INSERT INTO tx_log (id, description) VALUES (?, ?)",
                logId, "Deposited $50 to Bob's account");
        });

        System.out.println("  Mixed SQL+KV transaction committed:");
        System.out.println("  - Updated Bob's balance via KV API");
        System.out.println("  - Inserted tx_log entry via SQL");

        // Verify via SQL
        try (ResultSet<SqlRow> rs = client.sql().execute(null,
                "SELECT description FROM tx_log WHERE id = ?", logId)) {
            if (rs.hasNext()) {
                System.out.println("  - Log entry: " + rs.next().stringValue("DESCRIPTION"));
            }
        }

        System.out.println();
        System.out.println("This is NEW in 3.x:");
        System.out.println("  2.x: SQL and KV operations could NOT share the same transaction");
        System.out.println("  3.x: SQL and Table API operations share a unified transaction");
        System.out.println();
    }

    /**
     * Demonstrate rollback
     */
    private static void demonstrateRollback(IgniteClient client) {
        System.out.println("=== Transaction Rollback ===\n");

        Table accounts = client.tables().table("ACCOUNTS");
        RecordView<Tuple> view = accounts.recordView();

        Tuple aliceBefore = view.get(null, Tuple.create().set("id", 1));
        double balanceBefore = aliceBefore.doubleValue("balance");
        System.out.println("  Alice balance before: $" + balanceBefore);

        Transaction tx = client.transactions().begin();
        try {
            view.upsert(tx, Tuple.create()
                .set("id", 1).set("name", "Alice").set("balance", 0.0));
            // Simulate failure
            throw new RuntimeException("Simulated error");
        } catch (Exception e) {
            tx.rollback();
            System.out.println("  Transaction rolled back due to: " + e.getMessage());
        }

        Tuple aliceAfter = view.get(null, Tuple.create().set("id", 1));
        System.out.println("  Alice balance after rollback: $" + aliceAfter.doubleValue("balance"));
        System.out.println("  Balance unchanged: " + (aliceAfter.doubleValue("balance") == balanceBefore));
        System.out.println();
    }

    private static void printTransactionComparison() {
        System.out.println("=== Transaction Model Comparison ===\n");

        System.out.println("  | Aspect         | Ignite 2.x                     | Ignite 3.x               |");
        System.out.println("  |----------------|--------------------------------|--------------------------|");
        System.out.println("  | Concurrency    | PESSIMISTIC / OPTIMISTIC       | Strictly serializable    |");
        System.out.println("  | Isolation      | READ_COMMITTED, REPEATABLE_READ| Serializable (only)      |");
        System.out.println("  |                | SERIALIZABLE                   |                          |");
        System.out.println("  | API            | ignite.transactions().txStart()| client.transactions()    |");
        System.out.println("  |                |                                |   .begin() or            |");
        System.out.println("  |                |                                |   .runInTransaction()    |");
        System.out.println("  | SQL+KV in tx   | No                             | Yes                      |");
        System.out.println("  | Auto-retry     | No                             | Yes (runInTransaction)   |");
        System.out.println("  | Cross-cache tx | Yes (same concurrency mode)    | Yes (all tables unified) |");
        System.out.println("  | Deadlock detect| OPTIMISTIC SERIALIZABLE        | Built-in                 |");
        System.out.println();
    }
}
