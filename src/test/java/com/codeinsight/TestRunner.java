package com.codeinsight;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Automated test suite runner for Code Insight.
 * Discovers and executes all test methods across test suites.
 */
public final class TestRunner {

    public static void main(String[] args) {
        System.out.println();
        System.out.println("======================================================================");
        System.out.println("              CODE INSIGHT - AUTOMATED TEST SUITE RUNNER              ");
        System.out.println("======================================================================");

        List<Class<?>> testClasses = List.of(
                com.codeinsight.model.ThresholdConfigTest.class,
                com.codeinsight.analyzer.JavaSourceAnalyzerTest.class,
                com.codeinsight.analyzer.ProjectScannerTest.class,
                com.codeinsight.report.CsvReportExporterTest.class,
                com.codeinsight.report.HtmlReportExporterTest.class
        );

        int totalTests = 0;
        int passedTests = 0;
        int failedTests = 0;
        List<String> failureDetails = new ArrayList<>();

        long overallStart = System.currentTimeMillis();

        for (Class<?> testClass : testClasses) {
            System.out.println("\nRunning Suite: " + testClass.getSimpleName());

            Object instance;
            try {
                instance = testClass.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                System.err.println("  [!] Failed to instantiate " + testClass.getName() + ": " + e.getMessage());
                failedTests++;
                continue;
            }

            for (Method method : testClass.getDeclaredMethods()) {
                if (method.getName().startsWith("test") && method.getParameterCount() == 0) {
                    totalTests++;
                    String testName = method.getName();
                    long start = System.currentTimeMillis();
                    try {
                        method.invoke(instance);
                        long duration = System.currentTimeMillis() - start;
                        System.out.printf("  %-45s [PASS] (%d ms)%n", testName, duration);
                        passedTests++;
                    } catch (InvocationTargetException ite) {
                        long duration = System.currentTimeMillis() - start;
                        Throwable cause = ite.getCause() != null ? ite.getCause() : ite;
                        System.out.printf("  %-45s [FAIL] (%d ms)%n", testName, duration);
                        failedTests++;
                        failureDetails.add(testClass.getSimpleName() + "." + testName + " -> " + cause.getMessage());
                    } catch (Exception ex) {
                        System.out.printf("  %-45s [ERROR]%n", testName);
                        failedTests++;
                        failureDetails.add(testClass.getSimpleName() + "." + testName + " -> " + ex.getMessage());
                    }
                }
            }
        }

        long totalDuration = System.currentTimeMillis() - overallStart;

        System.out.println();
        System.out.println("======================================================================");
        if (failedTests == 0) {
            System.out.printf("TEST RESULTS: %d Passed, 0 Failed (100%% Success) in %d ms%n", passedTests, totalDuration);
            System.out.println("ALL TESTS PASSED SUCCESSFULLY!");
            System.out.println("======================================================================");
            System.out.println();
            System.exit(0);
        } else {
            System.err.printf("TEST RESULTS: %d Passed, %d Failed in %d ms%n", passedTests, failedTests, totalDuration);
            System.err.println("\nFAILURES:");
            for (String fail : failureDetails) {
                System.err.println("  - " + fail);
            }
            System.out.println("======================================================================");
            System.out.println();
            System.exit(1);
        }
    }
}
