package com.codeinsight.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Holds configurable thresholds for code complexity metrics.
 * A method is flagged for REVIEW if any metric exceeds its threshold.
 * Note: A review flag is an objective suggestion for refactoring or testing scrutiny,
 * not proof that code is bad.
 */
public record ThresholdConfig(int maxComplexity, int maxNesting, int maxLines) {
    public static final int DEFAULT_MAX_COMPLEXITY = 10;
    public static final int DEFAULT_MAX_NESTING = 4;
    public static final int DEFAULT_MAX_LINES = 30;

    public static ThresholdConfig defaultConfig() {
        return new ThresholdConfig(DEFAULT_MAX_COMPLEXITY, DEFAULT_MAX_NESTING, DEFAULT_MAX_LINES);
    }

    public boolean isReviewRequired(MethodMetrics m) {
        return m.cyclomaticComplexity() > maxComplexity
                || m.maximumNestingDepth() > maxNesting
                || (maxLines > 0 && m.lineCount() > maxLines);
    }

    public String getReviewRemarks(MethodMetrics m) {
        List<String> reasons = new ArrayList<>();
        if (m.cyclomaticComplexity() > maxComplexity) {
            reasons.add("CYCLO > " + maxComplexity);
        }
        if (m.maximumNestingDepth() > maxNesting) {
            reasons.add("NEST > " + maxNesting);
        }
        if (maxLines > 0 && m.lineCount() > maxLines) {
            reasons.add("LINES > " + maxLines);
        }
        return reasons.isEmpty() ? "-" : String.join(", ", reasons);
    }
}

