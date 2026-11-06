package com.codeinsight.model;

/**
 * Unit tests for ThresholdConfig.
 */
public class ThresholdConfigTest {

    public void testDefaultThresholds() {
        ThresholdConfig config = ThresholdConfig.defaultConfig();
        assert config.maxComplexity() == 10 : "Default max complexity should be 10";
        assert config.maxNesting() == 4 : "Default max nesting should be 4";
        assert config.maxLines() == 30 : "Default max lines should be 30";
    }

    public void testReviewRequiredOnComplexity() {
        ThresholdConfig config = new ThresholdConfig(10, 4, 30);
        MethodMetrics mNormal = new MethodMetrics("Test.java", "Test", "run", 1, 10, 8, 2);
        MethodMetrics mHighCyclo = new MethodMetrics("Test.java", "Test", "run", 1, 10, 11, 2);

        assert !config.isReviewRequired(mNormal) : "Normal method should not require review";
        assert config.isReviewRequired(mHighCyclo) : "Method with cyclo 11 > 10 must require review";
        assert config.getReviewRemarks(mHighCyclo).contains("CYCLO > 10") : "Remarks must mention CYCLO > 10";
    }

    public void testReviewRequiredOnNesting() {
        ThresholdConfig config = new ThresholdConfig(10, 4, 30);
        MethodMetrics mDeepNest = new MethodMetrics("Test.java", "Test", "nested", 1, 15, 6, 5);

        assert config.isReviewRequired(mDeepNest) : "Method with nesting 5 > 4 must require review";
        assert config.getReviewRemarks(mDeepNest).contains("NEST > 4") : "Remarks must mention NEST > 4";
    }

    public void testReviewRequiredOnLineCount() {
        ThresholdConfig config = new ThresholdConfig(10, 4, 30);
        MethodMetrics mLongMethod = new MethodMetrics("Test.java", "Test", "bigMethod", 1, 35, 3, 1);

        assert mLongMethod.lineCount() == 35 : "Line count should be 35";
        assert config.isReviewRequired(mLongMethod) : "Method with 35 lines > 30 must require review";
        assert config.getReviewRemarks(mLongMethod).contains("LINES > 30") : "Remarks must mention LINES > 30";
    }

    public void testMultipleThresholdBreaches() {
        ThresholdConfig config = new ThresholdConfig(10, 4, 30);
        MethodMetrics mWorst = new MethodMetrics("Test.java", "Test", "worstMethod", 1, 50, 15, 6);

        assert config.isReviewRequired(mWorst) : "Must require review";
        String remarks = config.getReviewRemarks(mWorst);
        assert remarks.contains("CYCLO > 10") : "Remarks should include CYCLO";
        assert remarks.contains("NEST > 4") : "Remarks should include NEST";
        assert remarks.contains("LINES > 30") : "Remarks should include LINES";
    }
}
