package com.example.ignite.solutions.lab12;

import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.sql.ResultSet;
import org.apache.ignite.sql.SqlRow;
import org.apache.ignite.table.KeyValueView;
import org.apache.ignite.table.RecordView;
import org.apache.ignite.table.Table;
import org.apache.ignite.table.Tuple;

/**
 * Lab 12 Exercises 6-8: Ignite 3.x Table API (replacing Cache API)
 *
 * Demonstrates:
 * - Schema-first approach: CREATE TABLE via SQL DDL
 * - RecordView with Tuples (schema-less access)
 * - RecordView with POJOs (typed access)
 * - KeyValueView with Tuples
 * - KeyValueView with native types
 * - SQL queries via IgniteSql
 * - Batch operations
 * - Contrast with 2.x Cache API
 *
 * Prerequisites: Running Ignite 3.x cluster
 */
public class Lab12CacheAPI {

    public static void main(String[] args) {
        System.out.println("=== Lab 12: Ignite 3.x Table API ===\n");

        try (IgniteClient client = IgniteClient.builder()
                .addresses("127.0.0.1:10800")
                .build()) {

            createTables(client);
            demonstrateRecordViewTuples(client);
            demonstrateRecordViewPojo(client);
            demonstrateKeyValueView(client);
            demonstrateSqlQueries(client);
            printApiComparison();

            // Cleanup
            client.sql().executeScript("DROP TABLE IF EXISTS persons; DROP TABLE IF EXISTS products");

        } catch (Exception e) {
            System.err.println("Could not connect to Ignite 3.x cluster.");
            e.printStackTrace();
        }
    }

    /**
     * Exercise 6: Schema-first table creation
     */
    private static void createTables(IgniteClient client) {
        System.out.println("=== Exercise 6: Schema-First Table Creation ===\n");

        System.out.println("In 3.x, tables are created via SQL DDL (schema-first):");
        System.out.println();

        client.sql().executeScript(
            "CREATE TABLE IF NOT EXISTS persons (" +
            "  id INT PRIMARY KEY," +
            "  name VARCHAR," +
            "  city VARCHAR," +
            "  salary DOUBLE" +
            ")");

        client.sql().executeScript(
            "CREATE TABLE IF NOT EXISTS products (" +
            "  id INT PRIMARY KEY," +
            "  name VARCHAR," +
            "  price DOUBLE," +
            "  quantity INT" +
            ")");

        System.out.println("  Created 'persons' and 'products' tables via SQL DDL");
        System.out.println();
        System.out.println("Contrast with 2.x:");
        System.out.println("  2.x: new CacheConfiguration<>(\"persons\") + @QuerySqlField annotations");
        System.out.println("  3.x: CREATE TABLE ... (SQL DDL, no Java annotations needed)");
        System.out.println();
    }

    /**
     * Exercise 7a: RecordView with Tuples
     */
    private static void demonstrateRecordViewTuples(IgniteClient client) {
        System.out.println("=== Exercise 7a: RecordView with Tuples ===\n");

        Table personsTable = client.tables().table("PERSONS");
        RecordView<Tuple> view = personsTable.recordView();

        // Insert records
        view.upsert(null, Tuple.create()
            .set("id", 1).set("name", "Alice").set("city", "NYC").set("salary", 85000.0));
        view.upsert(null, Tuple.create()
            .set("id", 2).set("name", "Bob").set("city", "SF").set("salary", 92000.0));
        view.upsert(null, Tuple.create()
            .set("id", 3).set("name", "Charlie").set("city", "NYC").set("salary", 78000.0));

        System.out.println("Inserted 3 records via RecordView<Tuple>.upsert()");

        // Get a record by key
        Tuple key = Tuple.create().set("id", 1);
        Tuple result = view.get(null, key);
        System.out.println("Get by key (id=1): " + result.stringValue("name") +
            ", city=" + result.stringValue("city"));

        // Delete a record
        boolean deleted = view.delete(null, Tuple.create().set("id", 3));
        System.out.println("Deleted id=3: " + deleted);

        System.out.println();
        System.out.println("Contrast with 2.x:");
        System.out.println("  2.x: cache.put(key, value), cache.get(key)");
        System.out.println("  3.x: view.upsert(tx, tuple), view.get(tx, keyTuple)");
        System.out.println("  Note: First parameter is always the transaction (null = no tx)");
        System.out.println();
    }

    /**
     * Exercise 7b: RecordView with POJOs
     */
    private static void demonstrateRecordViewPojo(IgniteClient client) {
        System.out.println("=== Exercise 7b: RecordView with POJOs ===\n");

        Table productsTable = client.tables().table("PRODUCTS");
        RecordView<Product> view = productsTable.recordView(Product.class);

        // Insert POJOs directly
        view.upsert(null, new Product(1, "Laptop", 999.99, 50));
        view.upsert(null, new Product(2, "Mouse", 29.99, 200));
        view.upsert(null, new Product(3, "Keyboard", 79.99, 150));

        System.out.println("Inserted 3 products via RecordView<Product>.upsert()");

        // Get by POJO key
        Product keyObj = new Product();
        keyObj.id = 1;
        Product laptop = view.get(null, keyObj);
        System.out.println("Get product id=1: " + laptop.name + " ($" + laptop.price + ")");

        // Replace
        laptop.price = 899.99;
        boolean replaced = view.replace(null, laptop);
        System.out.println("Updated laptop price: " + replaced);

        System.out.println();
        System.out.println("Contrast with 2.x BinaryObject:");
        System.out.println("  2.x: cache.withKeepBinary().get(key) -> BinaryObject");
        System.out.println("  3.x: table.recordView(MyClass.class) -> type-safe POJO view");
        System.out.println("  3.x: No @QuerySqlField annotations needed");
        System.out.println();
    }

    /**
     * Exercise 7c: KeyValueView
     */
    private static void demonstrateKeyValueView(IgniteClient client) {
        System.out.println("=== Exercise 7c: KeyValueView ===\n");

        Table personsTable = client.tables().table("PERSONS");

        // KeyValueView with Tuples
        KeyValueView<Tuple, Tuple> kvView = personsTable.keyValueView();

        kvView.put(null,
            Tuple.create().set("id", 10),
            Tuple.create().set("name", "Dave").set("city", "LA").set("salary", 70000.0));

        Tuple val = kvView.get(null, Tuple.create().set("id", 10));
        System.out.println("KV get (id=10): " + val.stringValue("name"));

        // KeyValueView with native types (single-column key/value)
        // For multi-column tables, use Tuple or POJO key/value classes

        System.out.println();
        System.out.println("View Types Summary:");
        System.out.println("  RecordView<Tuple>       - Schema-less, full row as Tuple");
        System.out.println("  RecordView<POJO>        - Type-safe, maps to Java class");
        System.out.println("  KeyValueView<Tuple,Tuple> - Separate key/value Tuples");
        System.out.println("  KeyValueView<K, V>      - Typed key and value classes");
        System.out.println();
    }

    /**
     * Exercise 8: SQL queries
     */
    private static void demonstrateSqlQueries(IgniteClient client) {
        System.out.println("=== Exercise 8: SQL Queries (3.x) ===\n");

        // DML via SQL
        client.sql().execute(null,
            "INSERT INTO persons (id, name, city, salary) VALUES (20, 'Eve', 'Chicago', 88000.0)");
        System.out.println("Inserted via SQL DML");

        // SELECT query
        System.out.println("\nSELECT * FROM persons:");
        try (ResultSet<SqlRow> rs = client.sql().execute(null, "SELECT * FROM persons ORDER BY id")) {
            while (rs.hasNext()) {
                SqlRow row = rs.next();
                System.out.printf("  id=%d, name=%s, city=%s, salary=%.0f%n",
                    row.intValue("ID"), row.stringValue("NAME"),
                    row.stringValue("CITY"), row.doubleValue("SALARY"));
            }
        }

        // Parameterized query
        System.out.println("\nParameterized query (city='NYC'):");
        try (ResultSet<SqlRow> rs = client.sql().execute(null,
                "SELECT name, salary FROM persons WHERE city = ?", "NYC")) {
            while (rs.hasNext()) {
                SqlRow row = rs.next();
                System.out.printf("  %s: $%.0f%n", row.stringValue("NAME"), row.doubleValue("SALARY"));
            }
        }

        // Aggregate query
        System.out.println("\nAggregate query:");
        try (ResultSet<SqlRow> rs = client.sql().execute(null,
                "SELECT city, COUNT(*) as cnt, AVG(salary) as avg_sal FROM persons GROUP BY city")) {
            while (rs.hasNext()) {
                SqlRow row = rs.next();
                System.out.printf("  %s: count=%d, avg_salary=%.0f%n",
                    row.stringValue("CITY"), row.longValue("CNT"), row.doubleValue("AVG_SAL"));
            }
        }

        System.out.println();
        System.out.println("Contrast with 2.x SQL:");
        System.out.println("  2.x: new SqlFieldsQuery(\"SELECT ...\"); cache.query(sqlQuery)");
        System.out.println("  3.x: client.sql().execute(tx, \"SELECT ...\", args)");
        System.out.println("  2.x: Returns List<List<?>> field rows");
        System.out.println("  3.x: Returns ResultSet<SqlRow> with named column access");
        System.out.println();
    }

    private static void printApiComparison() {
        System.out.println("=== Table API vs Cache API Comparison ===\n");

        System.out.println("  | Operation    | Ignite 2.x (Cache API)     | Ignite 3.x (Table API)       |");
        System.out.println("  |-------------|----------------------------|-------------------------------|");
        System.out.println("  | Schema      | @QuerySqlField on POJO     | CREATE TABLE (SQL DDL)        |");
        System.out.println("  | Create      | cache.put(key, value)      | view.upsert(tx, record)       |");
        System.out.println("  | Read        | cache.get(key)             | view.get(tx, key)             |");
        System.out.println("  | Update      | cache.put(key, value)      | view.upsert(tx, record)       |");
        System.out.println("  | Delete      | cache.remove(key)          | view.delete(tx, key)          |");
        System.out.println("  | Batch put   | cache.putAll(map)          | view.upsertAll(tx, collection)|");
        System.out.println("  | SQL query   | SqlFieldsQuery             | client.sql().execute()        |");
        System.out.println("  | Binary/Tuple| BinaryObject               | Tuple                         |");
        System.out.println("  | POJO map    | @QuerySqlField + cache     | recordView(MyClass.class)     |");
        System.out.println("  | Async       | IgniteFuture               | CompletableFuture             |");
        System.out.println();
    }

    // --- POJO classes ---

    public static class Product {
        public int id;
        public String name;
        public double price;
        public int quantity;

        public Product() {}

        public Product(int id, String name, double price, int quantity) {
            this.id = id;
            this.name = name;
            this.price = price;
            this.quantity = quantity;
        }
    }
}
