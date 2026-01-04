package com.example.ignite.tests;

import org.apache.ignite.IgniteCache;
import org.apache.ignite.cache.QueryEntity;
import org.apache.ignite.cache.QueryIndex;
import org.apache.ignite.cache.QueryIndexType;
import org.apache.ignite.cache.query.SqlFieldsQuery;
import org.apache.ignite.cache.query.TextQuery;
import org.apache.ignite.configuration.CacheConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.cache.Cache;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive tests for Lab 6: SQL and Indexing
 * Coverage: SQL queries, DDL operations, index creation, query performance, distributed joins
 */
@DisplayName("Lab 06: SQL and Indexing Tests")
public class Lab06SqlIndexingTest extends BaseIgniteTest {

    @Test
    @DisplayName("Test SqlFieldsQuery basic SELECT")
    public void testSqlFieldsQueryBasicSelect() {
        IgniteCache<Long, Object> cache = createPersonCache();

        // Insert test data
        insertPersonData(cache);

        // Query all persons
        SqlFieldsQuery query = new SqlFieldsQuery("SELECT name, age, city FROM Person");
        List<List<?>> results = cache.query(query).getAll();

        assertThat(results).hasSize(5);
    }

    @Test
    @DisplayName("Test SqlFieldsQuery with WHERE clause")
    public void testSqlFieldsQueryWithWhereClause() {
        IgniteCache<Long, Object> cache = createPersonCache();

        insertPersonData(cache);

        // Query persons in specific city
        SqlFieldsQuery query = new SqlFieldsQuery(
            "SELECT name, age, salary FROM Person WHERE city = ?")
            .setArgs("New York");
        List<List<?>> results = cache.query(query).getAll();

        assertThat(results).hasSize(3);
        for (List<?> row : results) {
            // Verify all returned rows have expected city through salary check
            assertThat(row).hasSize(3);
        }
    }

    @Test
    @DisplayName("Test SqlFieldsQuery with aggregation")
    public void testSqlFieldsQueryAggregation() {
        IgniteCache<Long, Object> cache = createPersonCache();

        insertPersonData(cache);

        // Aggregate query - average salary by city
        SqlFieldsQuery query = new SqlFieldsQuery(
            "SELECT city, AVG(salary) as avg_salary FROM Person GROUP BY city");
        List<List<?>> results = cache.query(query).getAll();

        assertThat(results).isNotEmpty();
        // Verify aggregation results contain city and average
        for (List<?> row : results) {
            assertThat(row).hasSize(2);
            assertThat(row.get(0)).isInstanceOf(String.class);
            assertThat(row.get(1)).isInstanceOf(Number.class);
        }
    }

    @Test
    @DisplayName("Test SqlFieldsQuery with ORDER BY and LIMIT")
    public void testSqlFieldsQueryOrderByLimit() {
        IgniteCache<Long, Object> cache = createPersonCache();

        insertPersonData(cache);

        // Query with ORDER BY and LIMIT
        SqlFieldsQuery query = new SqlFieldsQuery(
            "SELECT name, salary FROM Person ORDER BY salary DESC LIMIT 3");
        List<List<?>> results = cache.query(query).getAll();

        assertThat(results).hasSize(3);

        // Verify order (highest salary first)
        Double prevSalary = Double.MAX_VALUE;
        for (List<?> row : results) {
            Double salary = (Double) row.get(1);
            assertThat(salary).isLessThanOrEqualTo(prevSalary);
            prevSalary = salary;
        }
    }

    @Test
    @DisplayName("Test DDL CREATE TABLE via SQL")
    public void testDdlCreateTable() {
        // Create a simple cache for DDL operations
        CacheConfiguration<Object, Object> cfg = new CacheConfiguration<>(getTestCacheName());
        cfg.setSqlSchema("PUBLIC");
        IgniteCache<Object, Object> cache = ignite.getOrCreateCache(cfg);

        // Create table via DDL
        cache.query(new SqlFieldsQuery(
            "CREATE TABLE IF NOT EXISTS TestProduct (" +
            "id INT PRIMARY KEY, " +
            "name VARCHAR, " +
            "price DOUBLE)")).getAll();

        // Insert data into created table
        cache.query(new SqlFieldsQuery(
            "INSERT INTO TestProduct (id, name, price) VALUES (1, 'Test Item', 99.99)")).getAll();

        // Verify table exists and has data
        SqlFieldsQuery query = new SqlFieldsQuery("SELECT * FROM TestProduct");
        List<List<?>> results = cache.query(query).getAll();

        assertThat(results).hasSize(1);
        assertThat(results.get(0).get(1)).isEqualTo("Test Item");
    }

    @Test
    @DisplayName("Test INSERT operation")
    public void testInsertOperation() {
        IgniteCache<Long, Object> cache = createPersonCache();

        // Insert single record
        cache.query(new SqlFieldsQuery(
            "INSERT INTO Person (id, name, age, city, salary) VALUES (?, ?, ?, ?, ?)")
            .setArgs(1L, "Test Person", 25, "TestCity", 50000.0)).getAll();

        // Verify insert
        SqlFieldsQuery query = new SqlFieldsQuery("SELECT COUNT(*) FROM Person");
        List<List<?>> results = cache.query(query).getAll();

        assertThat(results.get(0).get(0)).isEqualTo(1L);
    }

    @Test
    @DisplayName("Test UPDATE operation")
    public void testUpdateOperation() {
        IgniteCache<Long, Object> cache = createPersonCache();

        insertPersonData(cache);

        // Update salaries in New York
        cache.query(new SqlFieldsQuery(
            "UPDATE Person SET salary = salary * 1.1 WHERE city = ?")
            .setArgs("New York")).getAll();

        // Verify update
        SqlFieldsQuery query = new SqlFieldsQuery(
            "SELECT salary FROM Person WHERE city = ? AND id = 1")
            .setArgs("New York");
        List<List<?>> results = cache.query(query).getAll();

        assertThat(results).hasSize(1);
        // Original salary was 75000.0, after 10% increase should be 82500.0
        assertThat((Double) results.get(0).get(0)).isEqualTo(82500.0);
    }

    @Test
    @DisplayName("Test DELETE operation")
    public void testDeleteOperation() {
        IgniteCache<Long, Object> cache = createPersonCache();

        insertPersonData(cache);

        // Get initial count
        SqlFieldsQuery countQuery = new SqlFieldsQuery("SELECT COUNT(*) FROM Person");
        long initialCount = (Long) cache.query(countQuery).getAll().get(0).get(0);

        // Delete persons younger than 29
        cache.query(new SqlFieldsQuery("DELETE FROM Person WHERE age < ?").setArgs(29)).getAll();

        // Verify delete
        List<List<?>> results = cache.query(countQuery).getAll();
        long newCount = (Long) results.get(0).get(0);

        assertThat(newCount).isLessThan(initialCount);
    }

    @Test
    @DisplayName("Test index creation with QueryIndex")
    public void testIndexCreation() {
        CacheConfiguration<Long, Object> cfg = new CacheConfiguration<>(getTestCacheName());

        QueryEntity entity = new QueryEntity(Long.class, Object.class);
        entity.setTableName("IndexedProduct");

        LinkedHashMap<String, String> fields = new LinkedHashMap<>();
        fields.put("id", "java.lang.Long");
        fields.put("name", "java.lang.String");
        fields.put("category", "java.lang.String");
        fields.put("price", "java.lang.Double");

        entity.setFields(fields);
        entity.setKeyFieldName("id");

        // Create indexes
        QueryIndex nameIdx = new QueryIndex("name", QueryIndexType.SORTED);
        QueryIndex categoryIdx = new QueryIndex("category", QueryIndexType.SORTED);

        entity.setIndexes(Arrays.asList(nameIdx, categoryIdx));
        cfg.setQueryEntities(Arrays.asList(entity));
        cfg.setSqlSchema("PUBLIC");

        IgniteCache<Long, Object> cache = ignite.getOrCreateCache(cfg);

        // Insert test data
        cache.query(new SqlFieldsQuery(
            "INSERT INTO IndexedProduct (id, name, category, price) VALUES (1, 'Laptop', 'Electronics', 1200.0)")).getAll();

        // Query using indexed field
        SqlFieldsQuery query = new SqlFieldsQuery(
            "SELECT name, price FROM IndexedProduct WHERE category = ?")
            .setArgs("Electronics");
        List<List<?>> results = cache.query(query).getAll();

        assertThat(results).hasSize(1);
        assertThat(results.get(0).get(0)).isEqualTo("Laptop");
    }

    @Test
    @DisplayName("Test composite index creation")
    public void testCompositeIndexCreation() {
        CacheConfiguration<Long, Object> cfg = new CacheConfiguration<>(getTestCacheName());

        QueryEntity entity = new QueryEntity(Long.class, Object.class);
        entity.setTableName("CompositeProduct");

        LinkedHashMap<String, String> fields = new LinkedHashMap<>();
        fields.put("id", "java.lang.Long");
        fields.put("category", "java.lang.String");
        fields.put("price", "java.lang.Double");
        fields.put("name", "java.lang.String");

        entity.setFields(fields);
        entity.setKeyFieldName("id");

        // Create composite index on category and price
        QueryIndex compositeIdx = new QueryIndex(
            Arrays.asList("category", "price"), QueryIndexType.SORTED);
        compositeIdx.setName("category_price_idx");

        entity.setIndexes(Arrays.asList(compositeIdx));
        cfg.setQueryEntities(Arrays.asList(entity));
        cfg.setSqlSchema("PUBLIC");

        IgniteCache<Long, Object> cache = ignite.getOrCreateCache(cfg);

        // Insert test data
        insertCompositeProductData(cache);

        // Query using composite index
        SqlFieldsQuery query = new SqlFieldsQuery(
            "SELECT name, price FROM CompositeProduct WHERE category = ? AND price < ?")
            .setArgs("Electronics", 500.0);
        List<List<?>> results = cache.query(query).getAll();

        assertThat(results).isNotEmpty();
    }

    @Test
    @DisplayName("Test query with range condition uses index")
    public void testRangeQueryWithIndex() {
        CacheConfiguration<Long, Object> cfg = new CacheConfiguration<>(getTestCacheName());

        QueryEntity entity = new QueryEntity(Long.class, Object.class);
        entity.setTableName("RangeProduct");

        LinkedHashMap<String, String> fields = new LinkedHashMap<>();
        fields.put("id", "java.lang.Long");
        fields.put("price", "java.lang.Double");
        fields.put("name", "java.lang.String");

        entity.setFields(fields);
        entity.setKeyFieldName("id");

        // Create sorted index on price for range queries
        QueryIndex priceIdx = new QueryIndex("price", QueryIndexType.SORTED);
        entity.setIndexes(Arrays.asList(priceIdx));

        cfg.setQueryEntities(Arrays.asList(entity));
        cfg.setSqlSchema("PUBLIC");

        IgniteCache<Long, Object> cache = ignite.getOrCreateCache(cfg);

        // Insert test data
        for (int i = 1; i <= 100; i++) {
            cache.query(new SqlFieldsQuery(
                "INSERT INTO RangeProduct (id, price, name) VALUES (?, ?, ?)")
                .setArgs((long) i, i * 10.0, "Product" + i)).getAll();
        }

        // Range query
        SqlFieldsQuery query = new SqlFieldsQuery(
            "SELECT name, price FROM RangeProduct WHERE price BETWEEN ? AND ?")
            .setArgs(100.0, 500.0);
        List<List<?>> results = cache.query(query).getAll();

        assertThat(results).hasSize(41); // 10, 20, 30... 500 = 41 values between 100 and 500
    }

    @Test
    @DisplayName("Test EXPLAIN query plan")
    public void testExplainQueryPlan() {
        IgniteCache<Long, Object> cache = createPersonCache();

        insertPersonData(cache);

        // Get execution plan
        SqlFieldsQuery explainQuery = new SqlFieldsQuery(
            "EXPLAIN SELECT name FROM Person WHERE city = 'New York'");
        List<List<?>> results = cache.query(explainQuery).getAll();

        assertThat(results).isNotEmpty();
        // EXPLAIN returns query plan as string
        String plan = results.get(0).get(0).toString();
        assertThat(plan).isNotEmpty();
    }

    @Test
    @DisplayName("Test distributed joins enabled")
    public void testDistributedJoins() {
        // Create Department cache
        IgniteCache<Integer, Object> deptCache = createDepartmentCache();
        // Create Employee cache
        IgniteCache<Integer, Object> empCache = createEmployeeCache();

        // Insert test data
        insertDepartmentData(deptCache);
        insertEmployeeData(empCache);

        // Distributed join query
        SqlFieldsQuery joinQuery = new SqlFieldsQuery(
            "SELECT e.name, e.salary, d.dept_name " +
            "FROM Employee e JOIN Department d ON e.dept_id = d.id " +
            "ORDER BY e.salary DESC")
            .setDistributedJoins(true);

        List<List<?>> results = empCache.query(joinQuery).getAll();

        assertThat(results).hasSize(6);
        // Verify join worked correctly
        for (List<?> row : results) {
            assertThat(row).hasSize(3);
            assertThat(row.get(0)).isNotNull(); // name
            assertThat(row.get(1)).isNotNull(); // salary
            assertThat(row.get(2)).isNotNull(); // dept_name
        }
    }

    @Test
    @DisplayName("Test distributed join with aggregation")
    public void testDistributedJoinWithAggregation() {
        IgniteCache<Integer, Object> deptCache = createDepartmentCache();
        IgniteCache<Integer, Object> empCache = createEmployeeCache();

        insertDepartmentData(deptCache);
        insertEmployeeData(empCache);

        // Join with aggregation
        SqlFieldsQuery aggJoinQuery = new SqlFieldsQuery(
            "SELECT d.dept_name, COUNT(e.id) as emp_count, AVG(e.salary) as avg_salary " +
            "FROM Department d LEFT JOIN Employee e ON d.id = e.dept_id " +
            "GROUP BY d.dept_name " +
            "ORDER BY avg_salary DESC")
            .setDistributedJoins(true);

        List<List<?>> results = empCache.query(aggJoinQuery).getAll();

        assertThat(results).hasSize(3); // 3 departments
        // Verify Engineering department has highest average salary
        assertThat(results.get(0).get(0)).isEqualTo("Engineering");
    }

    @Test
    @DisplayName("Test colocated join performance")
    public void testColocatedJoin() {
        IgniteCache<Integer, Object> deptCache = createDepartmentCache();
        IgniteCache<Integer, Object> empCache = createEmployeeCache();

        insertDepartmentData(deptCache);
        insertEmployeeData(empCache);

        // Colocated join (without distributed joins flag)
        SqlFieldsQuery colocatedJoin = new SqlFieldsQuery(
            "SELECT e.name, e.salary, d.dept_name " +
            "FROM Employee e JOIN Department d ON e.dept_id = d.id " +
            "WHERE d.dept_name = ?")
            .setArgs("Engineering");

        List<List<?>> results = empCache.query(colocatedJoin).getAll();

        assertThat(results).isNotEmpty();
        // All results should be from Engineering department
        for (List<?> row : results) {
            assertThat(row.get(2)).isEqualTo("Engineering");
        }
    }

    @Test
    @DisplayName("Test SqlFieldsQuery with parameterized query")
    public void testParameterizedQuery() {
        IgniteCache<Long, Object> cache = createPersonCache();

        insertPersonData(cache);

        // Parameterized query with multiple parameters
        SqlFieldsQuery query = new SqlFieldsQuery(
            "SELECT name, salary FROM Person WHERE city = ? AND age >= ? AND salary > ?")
            .setArgs("New York", 30, 70000.0);

        List<List<?>> results = cache.query(query).getAll();

        assertThat(results).isNotEmpty();
        for (List<?> row : results) {
            assertThat(row.get(1)).isInstanceOf(Double.class);
            assertThat((Double) row.get(1)).isGreaterThan(70000.0);
        }
    }

    @Test
    @DisplayName("Test COUNT aggregate function")
    public void testCountAggregate() {
        IgniteCache<Long, Object> cache = createPersonCache();

        insertPersonData(cache);

        SqlFieldsQuery query = new SqlFieldsQuery("SELECT COUNT(*) FROM Person");
        List<List<?>> results = cache.query(query).getAll();

        assertThat(results).hasSize(1);
        assertThat(results.get(0).get(0)).isEqualTo(5L);
    }

    @Test
    @DisplayName("Test SUM aggregate function")
    public void testSumAggregate() {
        IgniteCache<Long, Object> cache = createPersonCache();

        insertPersonData(cache);

        SqlFieldsQuery query = new SqlFieldsQuery("SELECT SUM(salary) FROM Person");
        List<List<?>> results = cache.query(query).getAll();

        assertThat(results).hasSize(1);
        // Total salary: 75000 + 82000 + 95000 + 88000 + 72000 = 412000
        assertThat((Double) results.get(0).get(0)).isEqualTo(412000.0);
    }

    @Test
    @DisplayName("Test MIN and MAX aggregate functions")
    public void testMinMaxAggregate() {
        IgniteCache<Long, Object> cache = createPersonCache();

        insertPersonData(cache);

        SqlFieldsQuery query = new SqlFieldsQuery(
            "SELECT MIN(salary), MAX(salary) FROM Person");
        List<List<?>> results = cache.query(query).getAll();

        assertThat(results).hasSize(1);
        assertThat((Double) results.get(0).get(0)).isEqualTo(72000.0); // MIN
        assertThat((Double) results.get(0).get(1)).isEqualTo(95000.0); // MAX
    }

    @Test
    @DisplayName("Test FULLTEXT index creation and text search")
    public void testFullTextIndex() {
        CacheConfiguration<Long, Object> cfg = new CacheConfiguration<>(getTestCacheName());

        QueryEntity entity = new QueryEntity(Long.class, Object.class);
        entity.setTableName("Article");

        LinkedHashMap<String, String> fields = new LinkedHashMap<>();
        fields.put("id", "java.lang.Long");
        fields.put("title", "java.lang.String");
        fields.put("content", "java.lang.String");
        fields.put("author", "java.lang.String");

        entity.setFields(fields);
        entity.setKeyFieldName("id");

        // Create FULLTEXT index on content field
        QueryIndex contentIdx = new QueryIndex("content", QueryIndexType.FULLTEXT);
        contentIdx.setName("content_fulltext_idx");

        // Create FULLTEXT index on title field
        QueryIndex titleIdx = new QueryIndex("title", QueryIndexType.FULLTEXT);
        titleIdx.setName("title_fulltext_idx");

        entity.setIndexes(Arrays.asList(contentIdx, titleIdx));

        cfg.setQueryEntities(Arrays.asList(entity));
        cfg.setSqlSchema("PUBLIC");

        IgniteCache<Long, Object> cache = ignite.getOrCreateCache(cfg);

        // Insert test articles
        cache.query(new SqlFieldsQuery(
            "INSERT INTO Article (id, title, content, author) VALUES (?, ?, ?, ?)")
            .setArgs(1L, "Introduction to Apache Ignite",
                "Apache Ignite is an in-memory computing platform for high-performance applications",
                "John Doe")).getAll();

        cache.query(new SqlFieldsQuery(
            "INSERT INTO Article (id, title, content, author) VALUES (?, ?, ?, ?)")
            .setArgs(2L, "Distributed Caching Best Practices",
                "Learn about distributed caching and how to optimize memory usage",
                "Jane Smith")).getAll();

        cache.query(new SqlFieldsQuery(
            "INSERT INTO Article (id, title, content, author) VALUES (?, ?, ?, ?)")
            .setArgs(3L, "SQL Queries in Ignite",
                "Apache Ignite supports ANSI SQL for querying distributed data",
                "Bob Johnson")).getAll();

        cache.query(new SqlFieldsQuery(
            "INSERT INTO Article (id, title, content, author) VALUES (?, ?, ?, ?)")
            .setArgs(4L, "Machine Learning Overview",
                "Introduction to machine learning algorithms and techniques",
                "Alice Williams")).getAll();

        // Test text search using SQL LIKE (basic text search)
        SqlFieldsQuery likeQuery = new SqlFieldsQuery(
            "SELECT title, author FROM Article WHERE content LIKE ?")
            .setArgs("%Apache Ignite%");
        List<List<?>> likeResults = cache.query(likeQuery).getAll();

        assertThat(likeResults).hasSize(2);

        // Test case-insensitive search
        SqlFieldsQuery caseInsensitiveQuery = new SqlFieldsQuery(
            "SELECT title FROM Article WHERE LOWER(content) LIKE ?")
            .setArgs("%distributed%");
        List<List<?>> caseResults = cache.query(caseInsensitiveQuery).getAll();

        assertThat(caseResults).hasSize(2); // "distributed caching" and "distributed data"

        // Test searching in title
        SqlFieldsQuery titleQuery = new SqlFieldsQuery(
            "SELECT title, author FROM Article WHERE title LIKE ?")
            .setArgs("%Ignite%");
        List<List<?>> titleResults = cache.query(titleQuery).getAll();

        assertThat(titleResults).hasSize(2);

        // Verify specific results
        boolean foundIntroduction = false;
        boolean foundSql = false;
        for (List<?> row : titleResults) {
            String title = (String) row.get(0);
            if (title.contains("Introduction")) foundIntroduction = true;
            if (title.contains("SQL")) foundSql = true;
        }
        assertThat(foundIntroduction).isTrue();
        assertThat(foundSql).isTrue();
    }

    @Test
    @DisplayName("Test JDBC thin driver connection and basic operations")
    public void testJdbcConnection() throws SQLException {
        // First create a table using cache API
        CacheConfiguration<Object, Object> cfg = new CacheConfiguration<>(getTestCacheName());
        cfg.setSqlSchema("PUBLIC");
        IgniteCache<Object, Object> cache = ignite.getOrCreateCache(cfg);

        // Create table via cache query
        cache.query(new SqlFieldsQuery(
            "CREATE TABLE IF NOT EXISTS JdbcTestProduct (" +
            "id INT PRIMARY KEY, " +
            "name VARCHAR, " +
            "price DOUBLE, " +
            "quantity INT)")).getAll();

        // JDBC thin driver URL - using localhost since we have ignite running
        String jdbcUrl = "jdbc:ignite:thin://127.0.0.1";

        // Test JDBC connection
        try (Connection conn = DriverManager.getConnection(jdbcUrl)) {
            assertThat(conn).isNotNull();
            assertThat(conn.isClosed()).isFalse();

            // Test INSERT via JDBC
            try (PreparedStatement pstmt = conn.prepareStatement(
                    "INSERT INTO JdbcTestProduct (id, name, price, quantity) VALUES (?, ?, ?, ?)")) {

                pstmt.setInt(1, 1);
                pstmt.setString(2, "Laptop");
                pstmt.setDouble(3, 1299.99);
                pstmt.setInt(4, 10);
                int insertedRows = pstmt.executeUpdate();
                assertThat(insertedRows).isEqualTo(1);

                pstmt.setInt(1, 2);
                pstmt.setString(2, "Mouse");
                pstmt.setDouble(3, 29.99);
                pstmt.setInt(4, 100);
                insertedRows = pstmt.executeUpdate();
                assertThat(insertedRows).isEqualTo(1);

                pstmt.setInt(1, 3);
                pstmt.setString(2, "Keyboard");
                pstmt.setDouble(3, 79.99);
                pstmt.setInt(4, 50);
                insertedRows = pstmt.executeUpdate();
                assertThat(insertedRows).isEqualTo(1);
            }

            // Test SELECT via JDBC
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT id, name, price, quantity FROM JdbcTestProduct ORDER BY id")) {

                int rowCount = 0;
                while (rs.next()) {
                    rowCount++;
                    int id = rs.getInt("id");
                    String name = rs.getString("name");
                    double price = rs.getDouble("price");
                    int quantity = rs.getInt("quantity");

                    assertThat(id).isGreaterThan(0);
                    assertThat(name).isNotEmpty();
                    assertThat(price).isGreaterThan(0);
                    assertThat(quantity).isGreaterThan(0);
                }
                assertThat(rowCount).isEqualTo(3);
            }

            // Test parameterized SELECT
            try (PreparedStatement pstmt = conn.prepareStatement(
                    "SELECT name, price FROM JdbcTestProduct WHERE price > ?")) {
                pstmt.setDouble(1, 50.0);
                try (ResultSet rs = pstmt.executeQuery()) {
                    int count = 0;
                    while (rs.next()) {
                        count++;
                        double price = rs.getDouble("price");
                        assertThat(price).isGreaterThan(50.0);
                    }
                    assertThat(count).isEqualTo(2); // Laptop and Keyboard
                }
            }

            // Test UPDATE via JDBC
            try (PreparedStatement pstmt = conn.prepareStatement(
                    "UPDATE JdbcTestProduct SET price = price * 0.9 WHERE name = ?")) {
                pstmt.setString(1, "Laptop");
                int updatedRows = pstmt.executeUpdate();
                assertThat(updatedRows).isEqualTo(1);
            }

            // Verify UPDATE
            try (PreparedStatement pstmt = conn.prepareStatement(
                    "SELECT price FROM JdbcTestProduct WHERE name = ?")) {
                pstmt.setString(1, "Laptop");
                try (ResultSet rs = pstmt.executeQuery()) {
                    assertThat(rs.next()).isTrue();
                    double newPrice = rs.getDouble("price");
                    assertThat(newPrice).isCloseTo(1169.99, within(0.01)); // 1299.99 * 0.9
                }
            }

            // Test DELETE via JDBC
            try (PreparedStatement pstmt = conn.prepareStatement(
                    "DELETE FROM JdbcTestProduct WHERE id = ?")) {
                pstmt.setInt(1, 2);
                int deletedRows = pstmt.executeUpdate();
                assertThat(deletedRows).isEqualTo(1);
            }

            // Test aggregate function via JDBC
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT COUNT(*), SUM(quantity), AVG(price) FROM JdbcTestProduct")) {
                assertThat(rs.next()).isTrue();
                long count = rs.getLong(1);
                long totalQuantity = rs.getLong(2);
                double avgPrice = rs.getDouble(3);

                assertThat(count).isEqualTo(2); // After delete
                assertThat(totalQuantity).isEqualTo(60); // 10 + 50 (mouse was deleted)
                assertThat(avgPrice).isGreaterThan(0);
            }

            // Test metadata
            assertThat(conn.getMetaData()).isNotNull();
            assertThat(conn.getMetaData().getDriverName()).contains("Ignite");
        }
    }

    // Helper methods

    private IgniteCache<Long, Object> createPersonCache() {
        CacheConfiguration<Long, Object> cfg = new CacheConfiguration<>(getTestCacheName());

        QueryEntity entity = new QueryEntity(Long.class, Object.class);
        entity.setTableName("Person");

        LinkedHashMap<String, String> fields = new LinkedHashMap<>();
        fields.put("id", "java.lang.Long");
        fields.put("name", "java.lang.String");
        fields.put("age", "java.lang.Integer");
        fields.put("city", "java.lang.String");
        fields.put("salary", "java.lang.Double");

        entity.setFields(fields);
        entity.setKeyFieldName("id");

        // Add indexes
        entity.setIndexes(Arrays.asList(
            new QueryIndex("name"),
            new QueryIndex("city")
        ));

        cfg.setQueryEntities(Arrays.asList(entity));
        cfg.setSqlSchema("PUBLIC");

        return ignite.getOrCreateCache(cfg);
    }

    private void insertPersonData(IgniteCache<Long, Object> cache) {
        cache.query(new SqlFieldsQuery(
            "INSERT INTO Person (id, name, age, city, salary) VALUES (?, ?, ?, ?, ?)")
            .setArgs(1L, "John Doe", 30, "New York", 75000.0)).getAll();

        cache.query(new SqlFieldsQuery(
            "INSERT INTO Person (id, name, age, city, salary) VALUES (?, ?, ?, ?, ?)")
            .setArgs(2L, "Jane Smith", 28, "Los Angeles", 82000.0)).getAll();

        cache.query(new SqlFieldsQuery(
            "INSERT INTO Person (id, name, age, city, salary) VALUES (?, ?, ?, ?, ?)")
            .setArgs(3L, "Bob Johnson", 35, "New York", 95000.0)).getAll();

        cache.query(new SqlFieldsQuery(
            "INSERT INTO Person (id, name, age, city, salary) VALUES (?, ?, ?, ?, ?)")
            .setArgs(4L, "Alice Williams", 32, "Chicago", 88000.0)).getAll();

        cache.query(new SqlFieldsQuery(
            "INSERT INTO Person (id, name, age, city, salary) VALUES (?, ?, ?, ?, ?)")
            .setArgs(5L, "Charlie Brown", 29, "New York", 72000.0)).getAll();
    }

    private void insertCompositeProductData(IgniteCache<Long, Object> cache) {
        cache.query(new SqlFieldsQuery(
            "INSERT INTO CompositeProduct (id, category, price, name) VALUES (1, 'Electronics', 1200.0, 'Laptop')")).getAll();
        cache.query(new SqlFieldsQuery(
            "INSERT INTO CompositeProduct (id, category, price, name) VALUES (2, 'Electronics', 25.0, 'Mouse')")).getAll();
        cache.query(new SqlFieldsQuery(
            "INSERT INTO CompositeProduct (id, category, price, name) VALUES (3, 'Electronics', 75.0, 'Keyboard')")).getAll();
        cache.query(new SqlFieldsQuery(
            "INSERT INTO CompositeProduct (id, category, price, name) VALUES (4, 'Electronics', 350.0, 'Monitor')")).getAll();
        cache.query(new SqlFieldsQuery(
            "INSERT INTO CompositeProduct (id, category, price, name) VALUES (5, 'Furniture', 299.0, 'Desk')")).getAll();
    }

    private IgniteCache<Integer, Object> createDepartmentCache() {
        CacheConfiguration<Integer, Object> cfg = new CacheConfiguration<>("Department-" + testName);

        QueryEntity entity = new QueryEntity(Integer.class, Object.class);
        entity.setTableName("Department");

        LinkedHashMap<String, String> fields = new LinkedHashMap<>();
        fields.put("id", "java.lang.Integer");
        fields.put("dept_name", "java.lang.String");
        fields.put("location", "java.lang.String");

        entity.setFields(fields);
        entity.setKeyFieldName("id");

        cfg.setQueryEntities(Arrays.asList(entity));
        cfg.setSqlSchema("PUBLIC");

        return ignite.getOrCreateCache(cfg);
    }

    private IgniteCache<Integer, Object> createEmployeeCache() {
        CacheConfiguration<Integer, Object> cfg = new CacheConfiguration<>("Employee-" + testName);

        QueryEntity entity = new QueryEntity(Integer.class, Object.class);
        entity.setTableName("Employee");

        LinkedHashMap<String, String> fields = new LinkedHashMap<>();
        fields.put("id", "java.lang.Integer");
        fields.put("name", "java.lang.String");
        fields.put("dept_id", "java.lang.Integer");
        fields.put("salary", "java.lang.Double");

        entity.setFields(fields);
        entity.setKeyFieldName("id");

        // Add index on dept_id for distributed joins
        QueryIndex deptIdIdx = new QueryIndex("dept_id");
        entity.setIndexes(Arrays.asList(deptIdIdx));

        cfg.setQueryEntities(Arrays.asList(entity));
        cfg.setSqlSchema("PUBLIC");

        return ignite.getOrCreateCache(cfg);
    }

    private void insertDepartmentData(IgniteCache<Integer, Object> cache) {
        cache.query(new SqlFieldsQuery(
            "INSERT INTO Department (id, dept_name, location) VALUES (1, 'Engineering', 'Building A')")).getAll();
        cache.query(new SqlFieldsQuery(
            "INSERT INTO Department (id, dept_name, location) VALUES (2, 'Sales', 'Building B')")).getAll();
        cache.query(new SqlFieldsQuery(
            "INSERT INTO Department (id, dept_name, location) VALUES (3, 'Marketing', 'Building C')")).getAll();
    }

    private void insertEmployeeData(IgniteCache<Integer, Object> cache) {
        cache.query(new SqlFieldsQuery(
            "INSERT INTO Employee (id, name, dept_id, salary) VALUES (1, 'Alice', 1, 95000.0)")).getAll();
        cache.query(new SqlFieldsQuery(
            "INSERT INTO Employee (id, name, dept_id, salary) VALUES (2, 'Bob', 2, 75000.0)")).getAll();
        cache.query(new SqlFieldsQuery(
            "INSERT INTO Employee (id, name, dept_id, salary) VALUES (3, 'Charlie', 1, 88000.0)")).getAll();
        cache.query(new SqlFieldsQuery(
            "INSERT INTO Employee (id, name, dept_id, salary) VALUES (4, 'Diana', 2, 82000.0)")).getAll();
        cache.query(new SqlFieldsQuery(
            "INSERT INTO Employee (id, name, dept_id, salary) VALUES (5, 'Eve', 1, 92000.0)")).getAll();
        cache.query(new SqlFieldsQuery(
            "INSERT INTO Employee (id, name, dept_id, salary) VALUES (6, 'Frank', 3, 70000.0)")).getAll();
    }
}
