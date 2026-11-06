package com.codeinsight.model;

import java.nio.file.Path;

/**
 * Stores details of a source file that could not be parsed or analyzed.
 */
public record SkippedFileInfo(Path filePath, String reason) {
    public String fileName() {
        return filePath.getFileName().toString();
    }
}
