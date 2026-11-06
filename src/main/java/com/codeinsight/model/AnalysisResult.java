package com.codeinsight.model;

import java.nio.file.Path;
import java.util.List;

/**
 * Encapsulates the complete result of an analysis run.
 */
public record AnalysisResult(
        Path targetPath,
        ThresholdConfig config,
        List<MethodMetrics> metrics,
        List<SkippedFileInfo> skippedFiles,
        int totalFilesScanned,
        long durationMillis) {

    public int analyzedFilesCount() {
        return Math.max(0, totalFilesScanned - skippedFiles.size());
    }

    public long flaggedMethodsCount() {
        return metrics.stream().filter(config::isReviewRequired).count();
    }
}

