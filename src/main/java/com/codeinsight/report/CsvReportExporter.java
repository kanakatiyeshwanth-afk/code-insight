package com.codeinsight.report;

import com.codeinsight.model.AnalysisResult;
import com.codeinsight.model.MethodMetrics;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Exports analysis results to standard CSV format.
 */
public final class CsvReportExporter {
    private CsvReportExporter() {}

    public static void export(AnalysisResult result, Path outputPath) throws IOException {
        if (result == null || outputPath == null) {
            throw new IllegalArgumentException("Result and output path cannot be null.");
        }

        StringBuilder csv = new StringBuilder(
                "file,class,method,start_line,end_line,line_count,cyclomatic_complexity,max_nesting_depth,status,review_remarks\n");

        for (MethodMetrics m : result.metrics()) {
            boolean review = result.config().isReviewRequired(m);
            String remarks = result.config().getReviewRemarks(m);
            csv.append(escape(m.fileName())).append(',')
                    .append(escape(m.className())).append(',')
                    .append(escape(m.methodName())).append(',')
                    .append(m.startLine()).append(',')
                    .append(m.endLine()).append(',')
                    .append(m.lineCount()).append(',')
                    .append(m.cyclomaticComplexity()).append(',')
                    .append(m.maximumNestingDepth()).append(',')
                    .append(escape(review ? "REVIEW" : "OK")).append(',')
                    .append(escape(remarks)).append('\n');
        }

        if (outputPath.getParent() != null) {
            Files.createDirectories(outputPath.getParent());
        }
        Files.writeString(outputPath, csv.toString());
    }

    private static String escape(String value) {
        if (value == null) return "\"\"";
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}

