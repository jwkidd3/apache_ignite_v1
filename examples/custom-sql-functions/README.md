# Custom SQL Functions Example

Demonstrates how to create and use Custom SQL Functions in Apache Ignite.

## What are Custom SQL Functions?

Custom SQL functions allow you to extend Ignite's SQL capabilities with your own Java logic. You can call these functions directly in SQL queries just like built-in functions.

## Files

- **MyCustomFunctions.java** - Class containing custom SQL function definitions
- **CustomSqlFunctionsDemo.java** - Demo showing usage of custom functions

## Usage

```bash
mvn compile exec:java -Dexec.mainClass="com.example.ignite.CustomSqlFunctionsDemo"
```

## Defining Custom Functions

Custom SQL functions must be:
- **Static methods** in a class
- **Annotated** with `@QuerySqlFunction`
- Using **serializable** parameter and return types

```java
public class MyCustomFunctions {

    @QuerySqlFunction
    public static String reverseString(String str) {
        if (str == null) return null;
        return new StringBuilder(str).reverse().toString();
    }

    @QuerySqlFunction
    public static double calculateTax(double amount, double taxRate) {
        return amount * taxRate;
    }
}
```

## Registering Functions

Register the function class in the cache configuration:

```java
CacheConfiguration<Integer, Object> cfg = new CacheConfiguration<>("MyCache");
cfg.setSqlFunctionClasses(MyCustomFunctions.class);
```

## Using Functions in SQL

```sql
-- String functions
SELECT REVERSE_STR(name) FROM Employee;
SELECT TITLE_CASE(name) FROM Employee;
SELECT MASK_STR(ssn, 4) FROM Employee;

-- Math functions
SELECT ROUND_TO(salary * 1.1, 2) FROM Employee;
SELECT PERCENTAGE(score, total) FROM Exam;

-- Business logic
SELECT SALARY_GRADE(salary) FROM Employee;
SELECT CALC_TAX(salary, 0.25) FROM Employee;

-- In WHERE clauses
SELECT * FROM Employee WHERE IS_VALID_EMAIL(email);
SELECT * FROM Employee WHERE AGE_FROM_YEAR(birth_year) > 30;
```

## Available Functions in This Example

### String Functions

| Function | Description | Example |
|----------|-------------|---------|
| `REVERSE_STR(str)` | Reverses a string | `REVERSE_STR('hello')` → `'olleh'` |
| `MASK_STR(str, visible)` | Masks all but last N chars | `MASK_STR('123-45-6789', 4)` → `'*******6789'` |
| `TITLE_CASE(str)` | Converts to title case | `TITLE_CASE('john doe')` → `'John Doe'` |
| `INITIALS(name)` | Extracts initials | `INITIALS('John Doe')` → `'JD'` |

### Math Functions

| Function | Description | Example |
|----------|-------------|---------|
| `ROUND_TO(val, decimals)` | Rounds to N decimals | `ROUND_TO(3.14159, 2)` → `3.14` |
| `PERCENTAGE(val, total)` | Calculates percentage | `PERCENTAGE(75, 100)` → `75.0` |
| `CLAMP_VAL(val, min, max)` | Clamps value to range | `CLAMP_VAL(150, 0, 100)` → `100` |
| `COMPOUND_INTEREST(p, r, y)` | Calculates compound interest | `COMPOUND_INTEREST(1000, 5, 10)` |

### Business Functions

| Function | Description | Example |
|----------|-------------|---------|
| `SALARY_GRADE(salary)` | Returns grade level | `SALARY_GRADE(95000)` → `'Mid-Level'` |
| `CALC_TAX(amt, rate)` | Calculates tax | `CALC_TAX(100000, 0.25)` → `25000` |
| `APPLY_DISCOUNT(price, pct, max)` | Applies capped discount | `APPLY_DISCOUNT(100, 15, 10)` → `90` |
| `IS_VALID_EMAIL(email)` | Validates email format | `IS_VALID_EMAIL('test@example.com')` → `true` |

### Date Functions

| Function | Description | Example |
|----------|-------------|---------|
| `AGE_FROM_YEAR(year)` | Calculates age | `AGE_FROM_YEAR(1990)` → `36` |
| `IS_LEAP_YEAR(year)` | Checks leap year | `IS_LEAP_YEAR(2024)` → `true` |
| `GET_QUARTER(month)` | Returns quarter | `GET_QUARTER(7)` → `3` |

## Deployment Requirements

**Important:** Custom SQL functions **cannot** use peer class loading. Unlike compute tasks that can be dynamically deployed, SQL function classes must be on the classpath of all server nodes at startup.

In this example, the functions work because everything runs in a single JVM - the function class is compiled and available on the classpath when the node starts.

### Production Deployment

For a multi-node cluster, you must deploy the JAR containing your functions to every server node:

```bash
# Copy to all server nodes
scp my-sql-functions.jar user@node1:/opt/ignite/libs/
scp my-sql-functions.jar user@node2:/opt/ignite/libs/
scp my-sql-functions.jar user@node3:/opt/ignite/libs/

# Restart all nodes to pick up the new JAR
```

### Why No Peer Class Loading?

SQL functions are registered with the H2 SQL engine when the cache is created. The engine needs the class available locally to invoke the static methods - there's no mechanism to transfer SQL function bytecode between nodes at runtime.

| Feature | Classpath Required | Peer Class Loading |
|---------|-------------------|-------------------|
| Custom SQL Functions | Yes, all server nodes | Not supported |
| Compute Tasks | No | Supported |

## Best Practices

1. **Keep functions simple** - Complex logic should be in application code
2. **Handle nulls** - Always check for null inputs
3. **Use appropriate types** - Match SQL and Java types correctly
4. **Document functions** - Help users understand function behavior
5. **Test thoroughly** - Verify edge cases and error handling
6. **Version carefully** - All nodes must have the same function version

## Performance Considerations

- Custom functions execute on the node where data resides
- Avoid heavy computations in frequently-called functions
- Consider indexing if functions are used in WHERE clauses frequently
- Functions in WHERE clauses may prevent index usage
