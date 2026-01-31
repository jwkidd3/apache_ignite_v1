package com.example.ignite.tests;

import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.table.RecordView;
import org.apache.ignite.table.Table;
import org.apache.ignite.table.Tuple;
import org.apache.ignite.table.KeyValueView;
import org.apache.ignite.sql.ResultSet;
import org.apache.ignite.sql.SqlRow;
import org.apache.ignite.tx.Transaction;
import org.junit.jupiter.api.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for Lab 13: Version Differences
 * Tests the Apache Ignite 3.x client API against a running Ignite 3 cluster.
 * Tests are skipped if no Ignite 3 node is available at 127.0.0.1:10800.
 */
@Tag("integration")
@DisplayName("Lab 13: Ignite 3.x Integration Tests")
public class Lab13VersionDifferencesTest {

    private static IgniteClient client;

    @BeforeAll
    static void connectToCluster() {
        try {
            client = IgniteClient.builder()
                    .addresses("127.0.0.1:10800")
                    .build();
            // Verify the connection is alive
            client.connections();
            Assumptions.assumeTrue(client != null, "Ignite 3.x cluster not available");
        } catch (Exception e) {
            Assumptions.assumeTrue(false, "Could not connect to Ignite 3.x at 127.0.0.1:10800: " + e.getMessage());
        }
    }

    @AfterAll
    static void closeClient() {
        if (client != null) {
            try {
                client.close();
            } catch (Exception ignored) {
            }
        }
    }

    @AfterEach
    void cleanup() {
        tryExecSql("DROP TABLE IF EXISTS test_persons");
        tryExecSql("DROP TABLE IF EXISTS test_kvtable");
        tryExecSql("DROP TABLE IF EXISTS test_txn");
        tryExecSql("DROP TABLE IF EXISTS test_mixed");
        tryExecSql("DROP TABLE IF EXISTS test_orders");
        tryExecSql("DROP TABLE IF EXISTS test_customers");
        tryExecSql("DROP TABLE IF EXISTS test_colocate_parent");
        tryExecSql("DROP TABLE IF EXISTS test_colocate_child");
        tryExecSql("DROP TABLE IF EXISTS test_ddl");
        tryExecSql("DROP TABLE IF EXISTS test_aggregate");
        tryExecSql("DROP TABLE IF EXISTS test_pojo");
        tryExecSql("DROP TABLE IF EXISTS test_rollback");
        tryExecSql("DROP TABLE IF EXISTS test_notthere");
    }

    private void tryExecSql(String sql) {
        try {
            executeSql(sql);
        } catch (Exception ignored) {
        }
    }

    private void executeSql(String sql, Object... args) {
        try (ResultSet<SqlRow> rs = client.sql().execute(null, sql, args)) {
            // drain
            while (rs.hasNext()) {
                rs.next();
            }
        }
    }

    private long countRows(String table) {
        try (ResultSet<SqlRow> rs = client.sql().execute(null, "SELECT COUNT(*) FROM " + table)) {
            if (rs.hasNext()) {
                return rs.next().longValue(0);
            }
        }
        return 0;
    }

    // ==========================================
    // 1. Client connection
    // ==========================================

    @Test
    @DisplayName("Client connections() returns non-empty list")
    void testClientConnections() {
        assertThat(client.connections()).isNotEmpty();
    }

    // ==========================================
    // 2. SQL DDL
    // ==========================================

    @Test
    @DisplayName("CREATE TABLE via SQL DDL")
    void testCreateTable() {
        executeSql("CREATE TABLE test_ddl (id INT PRIMARY KEY, name VARCHAR)");
        Table table = client.tables().table("TEST_DDL");
        assertThat(table).isNotNull();
    }

    @Test
    @DisplayName("DROP TABLE via SQL DDL")
    void testDropTable() {
        executeSql("CREATE TABLE test_ddl (id INT PRIMARY KEY, name VARCHAR)");
        executeSql("DROP TABLE test_ddl");
        Table table = client.tables().table("TEST_DDL");
        assertThat(table).isNull();
    }

    // ==========================================
    // 3. RecordView CRUD with Tuples
    // ==========================================

    @Test
    @DisplayName("RecordView upsert and get with Tuple")
    void testRecordViewUpsertGet() {
        executeSql("CREATE TABLE test_persons (id INT PRIMARY KEY, name VARCHAR, age INT)");
        Table table = client.tables().table("TEST_PERSONS");
        RecordView<Tuple> view = table.recordView();

        Tuple record = Tuple.create()
                .set("ID", 1)
                .set("NAME", "Alice")
                .set("AGE", 30);
        view.upsert(null, record);

        Tuple key = Tuple.create().set("ID", 1);
        Tuple result = view.get(null, key);
        assertThat(result).isNotNull();
        assertThat(result.stringValue("NAME")).isEqualTo("Alice");
        assertThat(result.intValue("AGE")).isEqualTo(30);
    }

    @Test
    @DisplayName("RecordView delete with Tuple")
    void testRecordViewDelete() {
        executeSql("CREATE TABLE test_persons (id INT PRIMARY KEY, name VARCHAR, age INT)");
        Table table = client.tables().table("TEST_PERSONS");
        RecordView<Tuple> view = table.recordView();

        view.upsert(null, Tuple.create().set("ID", 1).set("NAME", "Bob").set("AGE", 25));
        boolean deleted = view.delete(null, Tuple.create().set("ID", 1));
        assertThat(deleted).isTrue();

        Tuple result = view.get(null, Tuple.create().set("ID", 1));
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("RecordView upsert overwrites existing record")
    void testRecordViewUpsertOverwrite() {
        executeSql("CREATE TABLE test_persons (id INT PRIMARY KEY, name VARCHAR, age INT)");
        Table table = client.tables().table("TEST_PERSONS");
        RecordView<Tuple> view = table.recordView();

        view.upsert(null, Tuple.create().set("ID", 1).set("NAME", "Alice").set("AGE", 30));
        view.upsert(null, Tuple.create().set("ID", 1).set("NAME", "Alice Updated").set("AGE", 31));

        Tuple result = view.get(null, Tuple.create().set("ID", 1));
        assertThat(result.stringValue("NAME")).isEqualTo("Alice Updated");
        assertThat(result.intValue("AGE")).isEqualTo(31);
    }

    // ==========================================
    // 4. RecordView with POJO mapping
    // ==========================================

    @Test
    @DisplayName("RecordView with POJO mapping")
    void testRecordViewPojo() {
        executeSql("CREATE TABLE test_pojo (id INT PRIMARY KEY, name VARCHAR, age INT)");
        Table table = client.tables().table("TEST_POJO");
        RecordView<PersonPojo> view = table.recordView(PersonPojo.class);

        PersonPojo person = new PersonPojo();
        person.id = 1;
        person.name = "Charlie";
        person.age = 40;
        view.upsert(null, person);

        PersonPojo key = new PersonPojo();
        key.id = 1;
        PersonPojo result = view.get(null, key);
        assertThat(result).isNotNull();
        assertThat(result.name).isEqualTo("Charlie");
        assertThat(result.age).isEqualTo(40);
    }

    // ==========================================
    // 5. KeyValueView operations
    // ==========================================

    @Test
    @DisplayName("KeyValueView put and get")
    void testKeyValueViewPutGet() {
        executeSql("CREATE TABLE test_kvtable (id INT PRIMARY KEY, name VARCHAR, age INT)");
        Table table = client.tables().table("TEST_KVTABLE");
        KeyValueView<Tuple, Tuple> kvView = table.keyValueView();

        Tuple key = Tuple.create().set("ID", 1);
        Tuple val = Tuple.create().set("NAME", "Dave").set("AGE", 35);
        kvView.put(null, key, val);

        Tuple result = kvView.get(null, key);
        assertThat(result).isNotNull();
        assertThat(result.stringValue("NAME")).isEqualTo("Dave");
    }

    @Test
    @DisplayName("KeyValueView contains and remove")
    void testKeyValueViewContainsRemove() {
        executeSql("CREATE TABLE test_kvtable (id INT PRIMARY KEY, name VARCHAR, age INT)");
        Table table = client.tables().table("TEST_KVTABLE");
        KeyValueView<Tuple, Tuple> kvView = table.keyValueView();

        Tuple key = Tuple.create().set("ID", 1);
        Tuple val = Tuple.create().set("NAME", "Eve").set("AGE", 28);
        kvView.put(null, key, val);

        assertThat(kvView.contains(null, key)).isTrue();
        kvView.remove(null, key);
        assertThat(kvView.contains(null, key)).isFalse();
    }

    // ==========================================
    // 6. SQL queries
    // ==========================================

    @Test
    @DisplayName("SQL SELECT query")
    void testSqlSelect() {
        executeSql("CREATE TABLE test_persons (id INT PRIMARY KEY, name VARCHAR, age INT)");
        executeSql("INSERT INTO test_persons VALUES (1, 'Alice', 30)");
        executeSql("INSERT INTO test_persons VALUES (2, 'Bob', 25)");

        try (ResultSet<SqlRow> rs = client.sql().execute(null, "SELECT name FROM test_persons ORDER BY name")) {
            assertThat(rs.hasNext()).isTrue();
            assertThat(rs.next().stringValue("NAME")).isEqualTo("Alice");
            assertThat(rs.hasNext()).isTrue();
            assertThat(rs.next().stringValue("NAME")).isEqualTo("Bob");
        }
    }

    @Test
    @DisplayName("SQL INSERT statement")
    void testSqlInsert() {
        executeSql("CREATE TABLE test_persons (id INT PRIMARY KEY, name VARCHAR, age INT)");
        executeSql("INSERT INTO test_persons VALUES (1, 'Frank', 50)");

        assertThat(countRows("test_persons")).isEqualTo(1);
    }

    @Test
    @DisplayName("SQL parameterized query")
    void testSqlParameterized() {
        executeSql("CREATE TABLE test_persons (id INT PRIMARY KEY, name VARCHAR, age INT)");
        executeSql("INSERT INTO test_persons VALUES (1, 'Alice', 30)");
        executeSql("INSERT INTO test_persons VALUES (2, 'Bob', 25)");
        executeSql("INSERT INTO test_persons VALUES (3, 'Charlie', 35)");

        try (ResultSet<SqlRow> rs = client.sql().execute(null, "SELECT name FROM test_persons WHERE age > ?", 28)) {
            int count = 0;
            while (rs.hasNext()) {
                rs.next();
                count++;
            }
            assertThat(count).isEqualTo(2);
        }
    }

    @Test
    @DisplayName("SQL aggregate query")
    void testSqlAggregate() {
        executeSql("CREATE TABLE test_aggregate (id INT PRIMARY KEY, value INT)");
        executeSql("INSERT INTO test_aggregate VALUES (1, 10)");
        executeSql("INSERT INTO test_aggregate VALUES (2, 20)");
        executeSql("INSERT INTO test_aggregate VALUES (3, 30)");

        try (ResultSet<SqlRow> rs = client.sql().execute(null, "SELECT SUM(value) AS total FROM test_aggregate")) {
            assertThat(rs.hasNext()).isTrue();
            assertThat(rs.next().longValue("TOTAL")).isEqualTo(60L);
        }
    }

    // ==========================================
    // 7. Transactions
    // ==========================================

    @Test
    @DisplayName("Manual transaction begin and commit")
    void testManualTransaction() {
        executeSql("CREATE TABLE test_txn (id INT PRIMARY KEY, balance INT)");
        executeSql("INSERT INTO test_txn VALUES (1, 100)");

        Transaction tx = client.transactions().begin();
        client.sql().execute(tx, "UPDATE test_txn SET balance = balance + 50 WHERE id = 1");
        tx.commit();

        assertThat(countRows("test_txn")).isEqualTo(1);
        try (ResultSet<SqlRow> rs = client.sql().execute(null, "SELECT balance FROM test_txn WHERE id = 1")) {
            assertThat(rs.next().intValue("BALANCE")).isEqualTo(150);
        }
    }

    @Test
    @DisplayName("runInTransaction helper")
    void testRunInTransaction() {
        executeSql("CREATE TABLE test_txn (id INT PRIMARY KEY, balance INT)");
        executeSql("INSERT INTO test_txn VALUES (1, 200)");

        client.transactions().runInTransaction(tx -> {
            client.sql().execute(tx, "UPDATE test_txn SET balance = balance - 75 WHERE id = 1");
        });

        try (ResultSet<SqlRow> rs = client.sql().execute(null, "SELECT balance FROM test_txn WHERE id = 1")) {
            assertThat(rs.next().intValue("BALANCE")).isEqualTo(125);
        }
    }

    @Test
    @DisplayName("Transaction rollback")
    void testTransactionRollback() {
        executeSql("CREATE TABLE test_rollback (id INT PRIMARY KEY, balance INT)");
        executeSql("INSERT INTO test_rollback VALUES (1, 500)");

        Transaction tx = client.transactions().begin();
        client.sql().execute(tx, "UPDATE test_rollback SET balance = 0 WHERE id = 1");
        tx.rollback();

        try (ResultSet<SqlRow> rs = client.sql().execute(null, "SELECT balance FROM test_rollback WHERE id = 1")) {
            assertThat(rs.next().intValue("BALANCE")).isEqualTo(500);
        }
    }

    @Test
    @DisplayName("Mixed SQL and KV in a single transaction")
    void testMixedSqlKvTransaction() {
        executeSql("CREATE TABLE test_mixed (id INT PRIMARY KEY, name VARCHAR, age INT)");
        Table table = client.tables().table("TEST_MIXED");
        KeyValueView<Tuple, Tuple> kvView = table.keyValueView();

        Transaction tx = client.transactions().begin();

        // Insert via SQL
        client.sql().execute(tx, "INSERT INTO test_mixed VALUES (1, 'Alice', 30)");

        // Insert via KV
        kvView.put(tx, Tuple.create().set("ID", 2), Tuple.create().set("NAME", "Bob").set("AGE", 25));

        tx.commit();

        assertThat(countRows("test_mixed")).isEqualTo(2);
    }

    // ==========================================
    // 8. Distribution zones
    // ==========================================

    @Test
    @DisplayName("CREATE ZONE and DROP ZONE")
    void testCreateAndDropZone() {
        tryExecSql("DROP ZONE IF EXISTS test_zone");
        executeSql("CREATE ZONE test_zone WITH STORAGE_PROFILES='default'");
        // If we reach here, zone was created successfully
        executeSql("DROP ZONE test_zone");
    }

    // ==========================================
    // 9. COLOCATE BY tables
    // ==========================================

    @Test
    @DisplayName("Create tables with COLOCATE BY")
    void testColocateByTables() {
        tryExecSql("DROP ZONE IF EXISTS test_colo_zone");
        executeSql("CREATE ZONE test_colo_zone WITH STORAGE_PROFILES='default'");

        executeSql("CREATE TABLE test_colocate_parent (id INT PRIMARY KEY, name VARCHAR) WITH PRIMARY_ZONE='TEST_COLO_ZONE'");
        executeSql("CREATE TABLE test_colocate_child (id INT, parent_id INT, data VARCHAR, PRIMARY KEY (id, parent_id)) COLOCATE BY (parent_id) WITH PRIMARY_ZONE='TEST_COLO_ZONE'");

        executeSql("INSERT INTO test_colocate_parent VALUES (1, 'Parent1')");
        executeSql("INSERT INTO test_colocate_child VALUES (1, 1, 'Child1')");
        executeSql("INSERT INTO test_colocate_child VALUES (2, 1, 'Child2')");

        assertThat(countRows("test_colocate_parent")).isEqualTo(1);
        assertThat(countRows("test_colocate_child")).isEqualTo(2);

        tryExecSql("DROP TABLE IF EXISTS test_colocate_child");
        tryExecSql("DROP TABLE IF EXISTS test_colocate_parent");
        tryExecSql("DROP ZONE IF EXISTS test_colo_zone");
    }

    // ==========================================
    // 10. SQL joins on colocated tables
    // ==========================================

    @Test
    @DisplayName("SQL join on colocated tables")
    void testSqlJoinColocated() {
        executeSql("CREATE TABLE test_customers (id INT PRIMARY KEY, name VARCHAR)");
        executeSql("CREATE TABLE test_orders (id INT, customer_id INT, amount INT, PRIMARY KEY (id, customer_id)) COLOCATE BY (customer_id)");

        executeSql("INSERT INTO test_customers VALUES (1, 'Alice')");
        executeSql("INSERT INTO test_customers VALUES (2, 'Bob')");
        executeSql("INSERT INTO test_orders VALUES (1, 1, 100)");
        executeSql("INSERT INTO test_orders VALUES (2, 1, 200)");
        executeSql("INSERT INTO test_orders VALUES (3, 2, 150)");

        try (ResultSet<SqlRow> rs = client.sql().execute(null,
                "SELECT c.name, SUM(o.amount) AS total " +
                "FROM test_customers c JOIN test_orders o ON c.id = o.customer_id " +
                "GROUP BY c.name ORDER BY total DESC")) {
            assertThat(rs.hasNext()).isTrue();
            SqlRow row = rs.next();
            assertThat(row.stringValue("NAME")).isEqualTo("Alice");
            assertThat(row.longValue("TOTAL")).isEqualTo(300L);
        }
    }

    // ==========================================
    // 11. System views
    // ==========================================

    @Test
    @DisplayName("Query SYSTEM.TABLES system view")
    void testSystemTablesView() {
        executeSql("CREATE TABLE test_ddl (id INT PRIMARY KEY, name VARCHAR)");

        try (ResultSet<SqlRow> rs = client.sql().execute(null, "SELECT * FROM SYSTEM.TABLES WHERE TABLE_NAME = 'TEST_DDL'")) {
            assertThat(rs.hasNext()).isTrue();
        }
    }

    // ==========================================
    // 12. Table not found handling
    // ==========================================

    @Test
    @DisplayName("Table not found returns null")
    void testTableNotFound() {
        Table table = client.tables().table("NONEXISTENT_TABLE_XYZ");
        assertThat(table).isNull();
    }

    // ==========================================
    // Additional coverage tests
    // ==========================================

    @Test
    @DisplayName("Multiple record upsert and scan via SQL")
    void testMultipleRecordsScan() {
        executeSql("CREATE TABLE test_persons (id INT PRIMARY KEY, name VARCHAR, age INT)");
        for (int i = 1; i <= 10; i++) {
            executeSql("INSERT INTO test_persons VALUES (?, ?, ?)", i, "Person" + i, 20 + i);
        }

        try (ResultSet<SqlRow> rs = client.sql().execute(null, "SELECT COUNT(*) AS cnt FROM test_persons")) {
            assertThat(rs.next().longValue("CNT")).isEqualTo(10L);
        }
    }

    @Test
    @DisplayName("RecordView getAll with multiple keys")
    void testRecordViewGetAll() {
        executeSql("CREATE TABLE test_persons (id INT PRIMARY KEY, name VARCHAR, age INT)");
        Table table = client.tables().table("TEST_PERSONS");
        RecordView<Tuple> view = table.recordView();

        for (int i = 1; i <= 5; i++) {
            view.upsert(null, Tuple.create().set("ID", i).set("NAME", "Person" + i).set("AGE", 20 + i));
        }

        var keys = java.util.List.of(
                Tuple.create().set("ID", 1),
                Tuple.create().set("ID", 3),
                Tuple.create().set("ID", 5)
        );
        var results = view.getAll(null, keys);
        assertThat(results).hasSize(3);
    }

    @Test
    @DisplayName("KeyValueView getAll with multiple keys")
    void testKeyValueViewGetAll() {
        executeSql("CREATE TABLE test_kvtable (id INT PRIMARY KEY, name VARCHAR, age INT)");
        Table table = client.tables().table("TEST_KVTABLE");
        KeyValueView<Tuple, Tuple> kvView = table.keyValueView();

        for (int i = 1; i <= 5; i++) {
            kvView.put(null, Tuple.create().set("ID", i), Tuple.create().set("NAME", "KV" + i).set("AGE", 30 + i));
        }

        var keys = java.util.List.of(
                Tuple.create().set("ID", 2),
                Tuple.create().set("ID", 4)
        );
        var results = kvView.getAll(null, keys);
        assertThat(results).hasSize(2);
    }

    @Test
    @DisplayName("SQL DELETE and verify")
    void testSqlDelete() {
        executeSql("CREATE TABLE test_persons (id INT PRIMARY KEY, name VARCHAR, age INT)");
        executeSql("INSERT INTO test_persons VALUES (1, 'Alice', 30)");
        executeSql("INSERT INTO test_persons VALUES (2, 'Bob', 25)");
        executeSql("DELETE FROM test_persons WHERE id = 1");

        assertThat(countRows("test_persons")).isEqualTo(1);
    }

    @Test
    @DisplayName("SQL UPDATE and verify")
    void testSqlUpdate() {
        executeSql("CREATE TABLE test_persons (id INT PRIMARY KEY, name VARCHAR, age INT)");
        executeSql("INSERT INTO test_persons VALUES (1, 'Alice', 30)");
        executeSql("UPDATE test_persons SET age = 31 WHERE id = 1");

        try (ResultSet<SqlRow> rs = client.sql().execute(null, "SELECT age FROM test_persons WHERE id = 1")) {
            assertThat(rs.next().intValue("AGE")).isEqualTo(31);
        }
    }

    @Test
    @DisplayName("Drop table that does not exist with IF EXISTS")
    void testDropIfExistsNonexistent() {
        // Should not throw
        executeSql("DROP TABLE IF EXISTS test_notthere");
    }

    // ==========================================
    // POJO class for RecordView mapping
    // ==========================================

    public static class PersonPojo {
        public int id;
        public String name;
        public int age;
    }
}
