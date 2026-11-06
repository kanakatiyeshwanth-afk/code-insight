package com.codeinsight.report;

import com.codeinsight.model.AnalysisResult;
import com.codeinsight.model.MethodMetrics;
import com.codeinsight.model.SkippedFileInfo;
import com.codeinsight.model.ThresholdConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Unit tests for HtmlReportExporter.
 */
public class HtmlReportExporterTest {

    public void testHtmlExport() throws IOException {
        Path tempHtml = Files.createTempFile("test_report_", ".html");
        try {
            ThresholdConfig config = new ThresholdConfig(10, 4, 30);
            MethodMetrics m1 = new MethodMetrics("OrderService.java", "OrderService", "shippingCost", 5, 20, 6, 2);
            SkippedFileInfo skipped = new SkippedFileInfo(Path.of("Broken.java"), "Syntax error at line 5");

            AnalysisResult result = new AnalysisResult(
                    Path.of("samples"),
                    config,
                    List.of(m1),
                    List.of(skipped),
                    2,
                    42
            );

            HtmlReportExporter.export(result, tempHtml);

            String content = Files.readString(tempHtml);
            assert content.contains("<!DOCTYPE html>") : "HTML must have doctype";
            assert content.contains("Code Insight: Source Code Complexity Report") : "HTML must contain title";
            assert content.contains("shippingCost") : "HTML must list the method";
            assert content.contains("Broken.java") : "HTML must list skipped files";
            assert content.contains("badge-ok") : "HTML must contain status badge";
        } finally {
            Files.deleteIfExists(tempHtml);
        }
    }
}

