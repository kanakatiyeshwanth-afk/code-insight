package com.codeinsight.report;

import com.codeinsight.model.AnalysisResult;
import com.codeinsight.model.MethodMetrics;
import com.codeinsight.model.ThresholdConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

/**
 * Unit tests for CsvReportExporter.
 */
public class CsvReportExporterTest {

    public void testCsvExport() throws IOException {
        Path tempCsv = Files.createTempFile("test_report_", ".csv");
        try {
            ThresholdConfig config = new ThresholdConfig(10, 4, 30);
            MethodMetrics m1 = new MethodMetrics("Order.java", "Order", "calculate", 10, 25, 4, 2);
            MethodMetrics m2 = new MethodMetrics("Order.java", "Order", "heavy", 30, 80, 15, 6);

            AnalysisResult result = new AnalysisResult(
                    Path.of("Order.java"),
                    config,
                    List.of(m1, m2),
                    Collections.emptyList(),
                    1,
                    15
            );

            CsvReportExporter.export(result, tempCsv);

            List<String> lines = Files.readAllLines(tempCsv);
            assert lines.size() == 3 : "Expected 1 header line + 2 data rows";
            assert lines.get(0).startsWith("file,class,method") : "Header row format invalid";

            String row1 = lines.get(1);
            assert row1.contains("\"calculate\"") : "Row 1 should contain method name";
            assert row1.contains("\"OK\"") : "Row 1 status should be OK";

            String row2 = lines.get(2);
            assert row2.contains("\"heavy\"") : "Row 2 should contain method name";
            assert row2.contains("\"REVIEW\"") : "Row 2 status should be REVIEW";
            assert row2.contains("CYCLO > 10") : "Row 2 remarks should mention CYCLO > 10";
        } finally {
            Files.deleteIfExists(tempCsv);
        }
    }
}
