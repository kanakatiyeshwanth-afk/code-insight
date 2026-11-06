package com.codeinsight.analyzer;

import com.codeinsight.model.AnalysisResult;
import com.codeinsight.model.MethodMetrics;
import com.codeinsight.model.SkippedFileInfo;
import com.codeinsight.model.ThresholdConfig;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Scans directories recursively and analyzes all discovered Java source files.
 * Provides progress notifications and isolates failures per file.
 */
public final class ProjectScanner {

    @FunctionalInterface
    public interface ProgressListener {
        void onProgress(int current, int total, String currentFileName);
    }

    private ProjectScanner() {}

    /**
     * Executes analysis on a file or directory with optional progress reporting.
     */
    public static AnalysisResult scan(Path targetPath, ThresholdConfig config, ProgressListener progressListener)
            throws IOException {

        if (targetPath == null) {
            throw new IllegalArgumentException("Target path cannot be null.");
        }
        if (!Files.exists(targetPath)) {
            throw new IOException("Path does not exist: " + targetPath.toAbsolutePath());
        }

        long startTime = System.currentTimeMillis();

        List<Path> javaFiles = new ArrayList<>();
        if (Files.isRegularFile(targetPath)) {
            if (!targetPath.getFileName().toString().endsWith(".java")) {
                throw new IOException("Target is not a Java source file (*.java): " + targetPath.getFileName());
            }
            javaFiles.add(targetPath);
        } else if (Files.isDirectory(targetPath)) {
            javaFiles = findJavaFilesRecursively(targetPath);
        } else {
            throw new IOException("Target path is neither a regular file nor a directory: " + targetPath);
        }

        List<MethodMetrics> allMetrics = new ArrayList<>();
        List<SkippedFileInfo> skippedFiles = new ArrayList<>();

        int total = javaFiles.size();
        for (int i = 0; i < total; i++) {
            Path file = javaFiles.get(i);
            if (progressListener != null) {
                progressListener.onProgress(i + 1, total, file.getFileName().toString());
            }

            try {
                List<MethodMetrics> metrics = JavaSourceAnalyzer.analyze(file);
                allMetrics.addAll(metrics);
            } catch (Exception e) {
                String errorMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                skippedFiles.add(new SkippedFileInfo(file, errorMsg));
            }
        }

        long duration = System.currentTimeMillis() - startTime;
        return new AnalysisResult(targetPath, config, allMetrics, skippedFiles, total, duration);
    }

    /**
     * Recursively traverses a folder using walkFileTree to collect all .java source files.
     * Skips inaccessible directories gracefully.
     */
    public static List<Path> findJavaFilesRecursively(Path rootDir) throws IOException {
        List<Path> results = new ArrayList<>();
        Files.walkFileTree(rootDir, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (file.getFileName().toString().endsWith(".java")) {
                    results.add(file);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                // Inaccessible file or directory skipped
                return FileVisitResult.CONTINUE;
            }
        });
        Collections.sort(results);
        return results;
    }
}

