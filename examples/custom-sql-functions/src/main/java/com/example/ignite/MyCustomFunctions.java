package com.example.ignite;

import org.apache.ignite.cache.query.annotations.QuerySqlFunction;

/**
 * Custom SQL Functions for Apache Ignite
 *
 * These static methods can be called from SQL queries after
 * registering the class with the cache configuration.
 *
 * All custom SQL functions must:
 * - Be static methods
 * - Be annotated with @QuerySqlFunction
 * - Have serializable parameter and return types
 */
public class MyCustomFunctions {

    // ==================== String Functions ====================

    /**
     * Reverses a string.
     * SQL Usage: SELECT REVERSE_STR(name) FROM Person
     */
    @QuerySqlFunction(alias = "REVERSE_STR")
    public static String reverseStr(String str) {
        if (str == null) return null;
        return new StringBuilder(str).reverse().toString();
    }

    /**
     * Masks sensitive data (e.g., credit card, SSN).
     * SQL Usage: SELECT MASK_STR(ssn, 4) FROM Person
     */
    @QuerySqlFunction(alias = "MASK_STR")
    public static String maskStr(String str, int visibleChars) {
        if (str == null || str.length() <= visibleChars) return str;
        int maskLen = str.length() - visibleChars;
        return "*".repeat(maskLen) + str.substring(maskLen);
    }

    /**
     * Converts string to title case.
     * SQL Usage: SELECT TITLE_CASE(name) FROM Person
     */
    @QuerySqlFunction(alias = "TITLE_CASE")
    public static String titleCase(String str) {
        if (str == null || str.isEmpty()) return str;
        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = true;

        for (char c : str.toCharArray()) {
            if (Character.isWhitespace(c)) {
                capitalizeNext = true;
                result.append(c);
            } else if (capitalizeNext) {
                result.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                result.append(Character.toLowerCase(c));
            }
        }
        return result.toString();
    }

    /**
     * Extracts initials from a name.
     * SQL Usage: SELECT INITIALS(name) FROM Person
     */
    @QuerySqlFunction(alias = "INITIALS")
    public static String initials(String name) {
        if (name == null || name.isEmpty()) return "";
        StringBuilder result = new StringBuilder();
        for (String part : name.split("\\s+")) {
            if (!part.isEmpty()) {
                result.append(Character.toUpperCase(part.charAt(0)));
            }
        }
        return result.toString();
    }

    // ==================== Math Functions ====================

    /**
     * Calculates compound interest.
     * SQL Usage: SELECT COMPOUND_INTEREST(principal, rate, years) FROM Account
     */
    @QuerySqlFunction(alias = "COMPOUND_INTEREST")
    public static double compoundInterest(double principal, double rate, int years) {
        return principal * Math.pow(1 + rate / 100, years);
    }

    /**
     * Calculates percentage.
     * SQL Usage: SELECT PERCENTAGE(score, total) FROM Exam
     */
    @QuerySqlFunction(alias = "PERCENTAGE")
    public static double percentage(double value, double total) {
        if (total == 0) return 0;
        return (value / total) * 100;
    }

    /**
     * Rounds to specified decimal places.
     * SQL Usage: SELECT ROUND_TO(price, 2) FROM Product
     */
    @QuerySqlFunction(alias = "ROUND_TO")
    public static double roundTo(double value, int decimals) {
        double factor = Math.pow(10, decimals);
        return Math.round(value * factor) / factor;
    }

    /**
     * Clamps a value between min and max.
     * SQL Usage: SELECT CLAMP_VAL(score, 0, 100) FROM Exam
     */
    @QuerySqlFunction(alias = "CLAMP_VAL")
    public static double clampVal(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    // ==================== Business Logic Functions ====================

    /**
     * Calculates tax amount.
     * SQL Usage: SELECT CALC_TAX(salary, 0.25) FROM Employee
     */
    @QuerySqlFunction(alias = "CALC_TAX")
    public static double calcTax(double amount, double taxRate) {
        return roundTo(amount * taxRate, 2);
    }

    /**
     * Applies discount with optional cap.
     * SQL Usage: SELECT APPLY_DISCOUNT(price, 15, 50) FROM Product
     */
    @QuerySqlFunction(alias = "APPLY_DISCOUNT")
    public static double applyDiscount(double price, double discountPercent, double maxDiscount) {
        double discount = price * (discountPercent / 100);
        if (maxDiscount > 0 && discount > maxDiscount) {
            discount = maxDiscount;
        }
        return roundTo(price - discount, 2);
    }

    /**
     * Calculates salary grade based on amount.
     * SQL Usage: SELECT SALARY_GRADE(salary) FROM Employee
     */
    @QuerySqlFunction(alias = "SALARY_GRADE")
    public static String salaryGrade(double salary) {
        if (salary >= 150000) return "Executive";
        if (salary >= 100000) return "Senior";
        if (salary >= 70000) return "Mid-Level";
        if (salary >= 40000) return "Junior";
        return "Entry";
    }

    /**
     * Checks if email is valid (simple check).
     * SQL Usage: SELECT * FROM Person WHERE IS_VALID_EMAIL(email)
     */
    @QuerySqlFunction(alias = "IS_VALID_EMAIL")
    public static boolean isValidEmail(String email) {
        if (email == null || email.isEmpty()) return false;
        return email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }

    // ==================== Date/Time Functions ====================

    /**
     * Calculates age from birth year.
     * SQL Usage: SELECT AGE_FROM_YEAR(birth_year) FROM Person
     */
    @QuerySqlFunction(alias = "AGE_FROM_YEAR")
    public static int ageFromYear(int birthYear) {
        return java.time.Year.now().getValue() - birthYear;
    }

    /**
     * Checks if a year is a leap year.
     * SQL Usage: SELECT * FROM Event WHERE IS_LEAP_YEAR(year)
     */
    @QuerySqlFunction(alias = "IS_LEAP_YEAR")
    public static boolean isLeapYear(int year) {
        return java.time.Year.of(year).isLeap();
    }

    /**
     * Returns the quarter for a given month.
     * SQL Usage: SELECT GET_QUARTER(month) FROM Sales
     */
    @QuerySqlFunction(alias = "GET_QUARTER")
    public static int getQuarter(int month) {
        if (month < 1 || month > 12) return 0;
        return (month - 1) / 3 + 1;
    }

    // ==================== Utility Functions ====================

    /**
     * Null-safe coalesce for strings.
     * SQL Usage: SELECT COALESCE_STR(nickname, name, 'Unknown') FROM Person
     */
    @QuerySqlFunction(alias = "COALESCE_STR")
    public static String coalesceStr(String val1, String val2, String defaultVal) {
        if (val1 != null && !val1.isEmpty()) return val1;
        if (val2 != null && !val2.isEmpty()) return val2;
        return defaultVal;
    }

    /**
     * Generates a simple hash code for a string.
     * SQL Usage: SELECT STR_HASH(name) FROM Person
     */
    @QuerySqlFunction(alias = "STR_HASH")
    public static int strHash(String str) {
        return str == null ? 0 : str.hashCode();
    }
}
