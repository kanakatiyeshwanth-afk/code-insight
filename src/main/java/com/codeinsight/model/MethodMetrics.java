package com.codeinsight.model;

/**
 * Measurements and source location for one Java method or constructor.
 */
public record MethodMetrics(
        String fileName,
        String className,
        String methodName,
        long startLine,
        long endLine,
        int cyclomaticComplexity,
        int maximumNestingDepth) {

    public long lineCount() {
        return Math.max(0, endLine - startLine + 1);
    }

    public String lineRange() {
        return startLine + "-" + endLine;
    }
}
