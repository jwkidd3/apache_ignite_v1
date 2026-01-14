package com.example.ignite;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.cache.QueryEntity;
import org.apache.ignite.cache.query.SqlFieldsQuery;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.spi.communication.tcp.TcpCommunicationSpi;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.ipfinder.vm.TcpDiscoveryVmIpFinder;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Custom SQL Functions Demo
 *
 * Demonstrates how to create and use custom SQL functions in Apache Ignite.
 * Custom functions extend Ignite's SQL capabilities with your own Java logic.
 *
 * Usage:
 *   mvn exec:java -Dexec.mainClass="com.example.ignite.CustomSqlFunctionsDemo"
 */
public class CustomSqlFunctionsDemo {

    private static TcpDiscoveryVmIpFinder sharedIpFinder = new TcpDiscoveryVmIpFinder(true);

    static {
        sharedIpFinder.setAddresses(Arrays.asList("127.0.0.1:47500"));
    }

    public static void main(String[] args) {
        IgniteConfiguration cfg = createConfiguration();

        try (Ignite ignite = Ignition.start(cfg)) {
            System.out.println("\n=== Custom SQL Functions Demo ===\n");

            // Create cache with custom SQL functions registered
            IgniteCache<Integer, Object> cache = createCacheWithFunctions(ignite);

            // Insert test data
            insertTestData(cache);

            // Demo 1: String Functions
            System.out.println("=== String Functions ===\n");
            demoStringFunctions(cache);

            // Demo 2: Math Functions
            System.out.println("\n=== Math Functions ===\n");
            demoMathFunctions(cache);

            // Demo 3: Business Logic Functions
            System.out.println("\n=== Business Logic Functions ===\n");
            demoBusinessFunctions(cache);

            // Demo 4: Using Functions in WHERE Clauses
            System.out.println("\n=== Functions in WHERE Clauses ===\n");
            demoFunctionsInWhere(cache);

            // Demo 5: Combining Multiple Functions
            System.out.println("\n=== Combining Functions ===\n");
            demoCombinedFunctions(cache);

            System.out.println("\n=== Custom SQL Functions Summary ===");
            System.out.println("- Define functions as static methods with @QuerySqlFunction");
            System.out.println("- Register class via setSqlFunctionClasses() in CacheConfiguration");
            System.out.println("- Call functions directly in SQL queries");
            System.out.println("- Functions execute on the node where data resides");

            System.out.println("\nPress Enter to exit...");
            System.in.read();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static IgniteConfiguration createConfiguration() {
        IgniteConfiguration cfg = new IgniteConfiguration();
        cfg.setIgniteInstanceName("custom-sql-functions-demo");

        TcpDiscoverySpi discoverySpi = new TcpDiscoverySpi();
        discoverySpi.setLocalAddress("127.0.0.1");
        discoverySpi.setLocalPort(47500);
        discoverySpi.setLocalPortRange(10);
        discoverySpi.setIpFinder(sharedIpFinder);
        cfg.setDiscoverySpi(discoverySpi);

        TcpCommunicationSpi commSpi = new TcpCommunicationSpi();
        commSpi.setLocalAddress("127.0.0.1");
        commSpi.setLocalPort(47100);
        commSpi.setLocalPortRange(10);
        cfg.setCommunicationSpi(commSpi);

        return cfg;
    }

    private static IgniteCache<Integer, Object> createCacheWithFunctions(Ignite ignite) {
        CacheConfiguration<Integer, Object> cfg = new CacheConfiguration<>("Employee");

        // Define query entity
        QueryEntity entity = new QueryEntity(Integer.class, Object.class);
        entity.setTableName("Employee");

        LinkedHashMap<String, String> fields = new LinkedHashMap<>();
        fields.put("id", "java.lang.Integer");
        fields.put("name", "java.lang.String");
        fields.put("email", "java.lang.String");
        fields.put("department", "java.lang.String");
        fields.put("salary", "java.lang.Double");
        fields.put("birth_year", "java.lang.Integer");
        fields.put("ssn", "java.lang.String");

        entity.setFields(fields);
        entity.setKeyFieldName("id");

        cfg.setQueryEntities(Arrays.asList(entity));
        cfg.setSqlSchema("PUBLIC");

        // REGISTER CUSTOM SQL FUNCTIONS HERE
        cfg.setSqlFunctionClasses(MyCustomFunctions.class);

        return ignite.getOrCreateCache(cfg);
    }

    private static void insertTestData(IgniteCache<Integer, Object> cache) {
        System.out.println("Inserting test data...\n");

        String[][] employees = {
            {"1", "'john doe'", "'john.doe@example.com'", "'Engineering'", "95000", "1985", "'123-45-6789'"},
            {"2", "'jane smith'", "'jane.smith@example.com'", "'Marketing'", "82000", "1990", "'987-65-4321'"},
            {"3", "'bob johnson'", "'bob.johnson@example.com'", "'Engineering'", "110000", "1982", "'456-78-9012'"},
            {"4", "'alice williams'", "'invalid-email'", "'Sales'", "75000", "1995", "'321-54-9876'"},
            {"5", "'charlie brown'", "'charlie.brown@example.com'", "'Engineering'", "155000", "1978", "'654-32-1098'"},
            {"6", "'diana prince'", "'diana@example.com'", "'HR'", "68000", "1992", "'789-01-2345'"}
        };

        for (String[] emp : employees) {
            cache.query(new SqlFieldsQuery(
                "MERGE INTO Employee (id, name, email, department, salary, birth_year, ssn) VALUES (" +
                String.join(", ", emp) + ")")).getAll();
        }

        System.out.println("Inserted " + employees.length + " employees.");
    }

    private static void demoStringFunctions(IgniteCache<Integer, Object> cache) {
        // REVERSE_STR
        System.out.println("1. REVERSE_STR - Reverse names:");
        query(cache, "SELECT name, REVERSE_STR(name) as reversed FROM Employee LIMIT 3");

        // TITLE_CASE
        System.out.println("\n2. TITLE_CASE - Proper case names:");
        query(cache, "SELECT name, TITLE_CASE(name) as proper_name FROM Employee LIMIT 3");

        // INITIALS
        System.out.println("\n3. INITIALS - Extract initials:");
        query(cache, "SELECT TITLE_CASE(name) as name, INITIALS(name) as initials FROM Employee");

        // MASK_STR - Mask SSN
        System.out.println("\n4. MASK_STR - Masked SSN (show last 4):");
        query(cache, "SELECT TITLE_CASE(name) as name, MASK_STR(ssn, 4) as masked_ssn FROM Employee LIMIT 3");
    }

    private static void demoMathFunctions(IgniteCache<Integer, Object> cache) {
        // PERCENTAGE
        System.out.println("1. PERCENTAGE - Salary as % of 200000:");
        query(cache, "SELECT TITLE_CASE(name) as name, salary, ROUND_TO(PERCENTAGE(salary, 200000), 1) as pct FROM Employee LIMIT 3");

        // COMPOUND_INTEREST
        System.out.println("\n2. COMPOUND_INTEREST - Salary after 5 years at 3% raise:");
        query(cache, "SELECT TITLE_CASE(name) as name, salary, ROUND_TO(COMPOUND_INTEREST(salary, 3, 5), 2) as future_salary FROM Employee LIMIT 3");

        // CLAMP_VAL
        System.out.println("\n3. CLAMP_VAL - Clamp salary between 70000 and 120000:");
        query(cache, "SELECT TITLE_CASE(name) as name, salary, CLAMP_VAL(salary, 70000, 120000) as clamped FROM Employee");
    }

    private static void demoBusinessFunctions(IgniteCache<Integer, Object> cache) {
        // SALARY_GRADE
        System.out.println("1. SALARY_GRADE - Employee grades:");
        query(cache, "SELECT TITLE_CASE(name) as name, salary, SALARY_GRADE(salary) as grade FROM Employee ORDER BY salary DESC");

        // CALC_TAX
        System.out.println("\n2. CALC_TAX - Tax at 25%:");
        query(cache, "SELECT TITLE_CASE(name) as name, salary, CALC_TAX(salary, 0.25) as tax FROM Employee LIMIT 3");

        // APPLY_DISCOUNT (using salary as example)
        System.out.println("\n3. APPLY_DISCOUNT - 10% discount, max $10000:");
        query(cache, "SELECT TITLE_CASE(name) as name, salary, APPLY_DISCOUNT(salary, 10, 10000) as discounted FROM Employee LIMIT 3");

        // AGE_FROM_YEAR
        System.out.println("\n4. AGE_FROM_YEAR - Calculate ages:");
        query(cache, "SELECT TITLE_CASE(name) as name, birth_year, AGE_FROM_YEAR(birth_year) as age FROM Employee ORDER BY birth_year");
    }

    private static void demoFunctionsInWhere(IgniteCache<Integer, Object> cache) {
        // IS_VALID_EMAIL
        System.out.println("1. IS_VALID_EMAIL - Filter valid emails:");
        query(cache, "SELECT TITLE_CASE(name) as name, email FROM Employee WHERE IS_VALID_EMAIL(email)");

        System.out.println("\n2. Invalid emails:");
        query(cache, "SELECT TITLE_CASE(name) as name, email FROM Employee WHERE NOT IS_VALID_EMAIL(email)");

        // SALARY_GRADE in WHERE
        System.out.println("\n3. SALARY_GRADE in WHERE - Senior+ employees:");
        query(cache, "SELECT TITLE_CASE(name) as name, salary, SALARY_GRADE(salary) as grade FROM Employee WHERE SALARY_GRADE(salary) IN ('Senior', 'Executive')");

        // AGE_FROM_YEAR in WHERE
        System.out.println("\n4. AGE_FROM_YEAR in WHERE - Employees over 35:");
        query(cache, "SELECT TITLE_CASE(name) as name, AGE_FROM_YEAR(birth_year) as age FROM Employee WHERE AGE_FROM_YEAR(birth_year) > 35");
    }

    private static void demoCombinedFunctions(IgniteCache<Integer, Object> cache) {
        System.out.println("Complex query combining multiple custom functions:");
        query(cache,
            "SELECT " +
            "  TITLE_CASE(name) as name, " +
            "  INITIALS(name) as initials, " +
            "  MASK_STR(ssn, 4) as ssn, " +
            "  salary, " +
            "  SALARY_GRADE(salary) as grade, " +
            "  AGE_FROM_YEAR(birth_year) as age, " +
            "  ROUND_TO(COMPOUND_INTEREST(salary, 3, 5), 0) as salary_5yr " +
            "FROM Employee " +
            "WHERE IS_VALID_EMAIL(email) " +
            "ORDER BY salary DESC"
        );
    }

    private static void query(IgniteCache<Integer, Object> cache, String sql) {
        List<List<?>> results = cache.query(new SqlFieldsQuery(sql)).getAll();
        for (List<?> row : results) {
            System.out.println("  " + row);
        }
    }
}
