package com.codeinsight.report;

import com.codeinsight.model.AnalysisResult;
import com.codeinsight.model.MethodMetrics;
import com.codeinsight.model.SkippedFileInfo;
import com.codeinsight.model.ThresholdConfig;

import java.util.Comparator;
import java.util.List;

/**
 * Handles readable, aligned console report formatting with dynamic column sizing.
 */
public final class ReportFormatter {
    private ReportFormatter() {}

    public static void printReport(AnalysisResult result) {
        printReport(result.metrics(), result.config(), result.totalFilesScanned(), result.skippedFiles());
    }

    public static void printReport(
            List<MethodMetrics> metrics,
            ThresholdConfig config,
            int totalScannedFiles,
            List<SkippedFileInfo> skippedFiles) {

        System.out.println();
        System.out.println("==========================================================================================");
        System.out.println("                         CODE INSIGHT - JAVA COMPLEXITY REPORT                            ");
        System.out.println("==========================================================================================");

        if (metrics.isEmpty()) {
            System.out.println("No Java methods or constructors were found to analyze.");
        } else {
            // Determine dynamic column widths based on actual data
            int fileColWidth = 16;
            int classColWidth = 14;
            int methodColWidth = 18;
            int remarksColWidth = 16;

            for (MethodMetrics m : metrics) {
                fileColWidth = Math.max(fileColWidth, m.fileName().length());
                classColWidth = Math.max(classColWidth, m.className().length());
                methodColWidth = Math.max(methodColWidth, m.methodName().length());
                remarksColWidth = Math.max(remarksColWidth, config.getReviewRemarks(m).length());
            }

            // Cap column widths at reasonable limits to avoid excessive horizontal sprawl
            fileColWidth = Math.min(fileColWidth, 28);
            classColWidth = Math.min(classColWidth, 26);
            methodColWidth = Math.min(methodColWidth, 34);
            remarksColWidth = Math.min(remarksColWidth, 32);

            String formatString = String.format("%%-%ds  %%-%ds  %%-%ds  %%6s  %%6s  %%6s  %%-8s  %%-%ds%%n",
                    fileColWidth, classColWidth, methodColWidth, remarksColWidth);

            String rowFormat = String.format("%%-%ds  %%-%ds  %%-%ds  %%6d  %%6d  %%6d  %%-8s  %%-%ds%%n",
                    fileColWidth, classColWidth, methodColWidth, remarksColWidth);

            // Print table header
            System.out.printf(formatString,
                    "FILE", "CLASS", "METHOD", "LINES", "CYCLO", "NEST", "STATUS", "REMARKS");

            int totalTableWidth = fileColWidth + classColWidth + methodColWidth + remarksColWidth + 38;
            System.out.println("-".repeat(Math.max(90, totalTableWidth)));

            // Sort metrics: fileName -> className -> methodName
            metrics.sort(Comparator.comparing(MethodMetrics::fileName)
                    .thenComparing(MethodMetrics::className)
                    .thenComparing(MethodMetrics::methodName));

            int flaggedCount = 0;
            for (MethodMetrics m : metrics) {
                boolean needsReview = config.isReviewRequired(m);
                if (needsReview) {
                    flaggedCount++;
                }

                String displayFile = abbreviate(m.fileName(), fileColWidth);
                String displayClass = abbreviate(m.className(), classColWidth);
                String displayMethod = abbreviate(m.methodName(), methodColWidth);
                String remarks = abbreviate(config.getReviewRemarks(m), remarksColWidth);
                String status = needsReview ? "REVIEW" : "OK";

                System.out.printf(rowFormat,
                        displayFile, displayClass, displayMethod, m.lineCount(),
                        m.cyclomaticComplexity(), m.maximumNestingDepth(), status, remarks);
            }

            System.out.println("-".repeat(Math.max(90, totalTableWidth)));
        }

        // Summary section
        int analyzedFilesCount = totalScannedFiles - skippedFiles.size();
        long flaggedMethods = metrics.stream().filter(config::isReviewRequired).count();

        System.out.println("SUMMARY STATISTICS:");
        System.out.printf("  Files scanned       : %d%n", totalScannedFiles);
        System.out.printf("  Files analyzed      : %d%n", Math.max(0, analyzedFilesCount));
        System.out.printf("  Files skipped       : %d%n", skippedFiles.size());
        System.out.printf("  Methods analyzed    : %d%n", metrics.size());
        System.out.printf("  Flagged for review  : %d%n", flaggedMethods);
        System.out.printf("  Active thresholds   : Cyclomatic > %d | Nesting > %d | Lines > %d%n",
                config.maxComplexity(), config.maxNesting(), config.maxLines());

        // Skipped files report
        if (!skippedFiles.isEmpty()) {
            System.out.println();
            System.out.println("SKIPPED FILES DETAILS (" + skippedFiles.size() + "):");
            for (SkippedFileInfo skipped : skippedFiles) {
                System.out.println("  [!] " + skipped.filePath() + ":");
                String[] reasonLines = skipped.reason().split("\\R");
                for (String line : reasonLines) {
                    System.out.println("      " + line);
                }
            }
        }
        System.out.println("==========================================================================================");
        System.out.println();
    }

    private static String abbreviate(String value, int max) {
        if (value == null) return "";
        return value.length() <= max ? value : value.substring(0, Math.max(0, max - 3)) + "...";
    }
}

