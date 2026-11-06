package com.codeinsight.analyzer;

import com.codeinsight.model.AnalysisResult;
import com.codeinsight.model.ThresholdConfig;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Unit tests for ProjectScanner recursive directory traversal and fault tolerance.
 */
public class ProjectScannerTest {

    public void testRecursiveFolderScanning() throws IOException {
        Path samplesDir = Path.of("samples");
        ThresholdConfig config = ThresholdConfig.defaultConfig();

        AnalysisResult result = ProjectScanner.scan(samplesDir, config, null);

        assert result.totalFilesScanned() >= 3 : "Expected at least 3 files found in samples";
        assert result.metrics().size() >= 3 : "Expected at least 3 methods analyzed";
        assert result.analyzedFilesCount() >= 2 : "Expected at least 2 files successfully analyzed";

        // Check that nested files like MathUtility.java are found
        boolean foundNested = result.metrics().stream().anyMatch(m -> "MathUtility.java".equals(m.fileName()));
        assert foundNested : "Recursive scan should discover files in subdirectories (MathUtility.java)";
    }

    public void testFaultToleranceWithInvalidFile() throws IOException {
        Path samplesDir = Path.of("samples");
        ThresholdConfig config = ThresholdConfig.defaultConfig();

        AnalysisResult result = ProjectScanner.scan(samplesDir, config, null);

        // BrokenSyntaxSample.java should be in skippedFiles
        assert !result.skippedFiles().isEmpty() : "Skipped files list should not be empty due to BrokenSyntaxSample";
        boolean skippedBroken = result.skippedFiles().stream()
                .anyMatch(s -> s.fileName().contains("BrokenSyntaxSample.java"));
        assert skippedBroken : "BrokenSyntaxSample.java must be recorded as skipped";

        // Crucial test: despite the broken file, valid files were still analyzed
        boolean foundOrderService = result.metrics().stream().anyMatch(m -> "OrderService.java".equals(m.fileName()));
        assert foundOrderService : "Valid files must continue being analyzed even when another file has syntax errors";
    }

    public void testNonExistentPathThrowsException() {
        boolean caught = false;
        try {
            ProjectScanner.scan(Path.of("non_existent_folder_xyz_123"), ThresholdConfig.defaultConfig(), null);
        } catch (IOException e) {
            caught = true;
        }
        assert caught : "Scanning non-existent path must throw IOException";
    }
}
