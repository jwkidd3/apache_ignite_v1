package com.example.ignite.examples.versiondiff;

/**
 * Apache Ignite Version Comparison Summary (2.16 vs 3.x).
 * Prints a comprehensive comparison to the console.
 */
public class VersionComparisonSummary {

    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════════════════════════════════╗");
        System.out.println("║     APACHE IGNITE VERSION COMPARISON: 2.16 vs 3.x                    ║");
        System.out.println("╚══════════════════════════════════════════════════════════════════════╝\n");

        printFeatureMatrix();
        printDecisionMatrix();
        printSummary();
    }

    private static void printFeatureMatrix() {
        System.out.println("FEATURE AVAILABILITY:");
        System.out.println("┌────────────────────────────┬─────────────────┬─────────────────┐");
        System.out.println("│ Feature                    │ Ignite 2.16     │ Ignite 3.x      │");
        System.out.println("├────────────────────────────┼─────────────────┼─────────────────┤");
        System.out.println("│ Distributed Cache          │ ✓ Yes           │ ✓ Yes (Table)   │");
        System.out.println("│ SQL Queries                │ ✓ Yes           │ ✓ Yes (better)  │");
        System.out.println("│ ACID Transactions          │ ✓ Yes           │ ✓ Yes (better)  │");
        System.out.println("│ Compute Grid               │ ✓ Yes (full)    │ ~ Basic         │");
        System.out.println("│ Continuous Queries         │ ✓ Yes           │ ✗ Not yet       │");
        System.out.println("│ Near Cache                 │ ✓ Yes           │ ✗ Not yet       │");
        System.out.println("│ Service Grid               │ ✓ Yes           │ ✗ Not yet       │");
        System.out.println("│ RAFT Consensus             │ ✗ No            │ ✓ Yes           │");
        System.out.println("│ Pluggable Storage          │ ✗ No            │ ✓ Yes           │");
        System.out.println("│ Dynamic Configuration      │ ✗ No            │ ✓ Yes           │");
        System.out.println("└────────────────────────────┴─────────────────┴─────────────────┘\n");
    }

    private static void printDecisionMatrix() {
        System.out.println("DECISION MATRIX:");
        System.out.println("┌────────────────────────────────────┬─────────────────────────────────┐");
        System.out.println("│ Scenario                           │ Recommendation                  │");
        System.out.println("├────────────────────────────────────┼─────────────────────────────────┤");
        System.out.println("│ New project, SQL-heavy             │ → Ignite 3.x                    │");
        System.out.println("│ Need Compute Grid features         │ → Ignite 2.16                   │");
        System.out.println("│ Need Continuous Queries            │ → Ignite 2.16                   │");
        System.out.println("│ Existing 2.x deployment            │ → Stay on 2.16                  │");
        System.out.println("│ RDBMS migration                    │ → Ignite 3.x                    │");
        System.out.println("│ Maximum consistency (RAFT)         │ → Ignite 3.x                    │");
        System.out.println("└────────────────────────────────────┴─────────────────────────────────┘\n");
    }

    private static void printSummary() {
        System.out.println("SUMMARY:");
        System.out.println("  Ignite 2.16: Mature, feature-complete, production-proven");
        System.out.println("  Ignite 3.x:  Modern architecture, better SQL, still evolving");
        System.out.println();
        System.out.println("  No direct upgrade path - migration requires new cluster setup.");
    }
}
