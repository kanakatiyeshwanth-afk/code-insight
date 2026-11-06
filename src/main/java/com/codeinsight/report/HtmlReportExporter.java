package com.codeinsight.report;

import com.codeinsight.model.AnalysisResult;
import com.codeinsight.model.MethodMetrics;
import com.codeinsight.model.SkippedFileInfo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

/**
 * Generates an interactive, standalone HTML complexity report
 * with embedded CSS, summary cards, and client-side table filtering.
 */
public final class HtmlReportExporter {

    private HtmlReportExporter() {}

    public static void export(AnalysisResult result, Path outputPath) throws IOException {
        if (result == null || outputPath == null) {
            throw new IllegalArgumentException("Result and output path cannot be null.");
        }

        String targetName = result.targetPath().getFileName() != null
                ? result.targetPath().getFileName().toString()
                : result.targetPath().toString();

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n");
        html.append("  <meta charset=\"UTF-8\">\n");
        html.append("  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
        html.append("  <title>Code Insight Report - ").append(escapeHtml(targetName)).append("</title>\n");
        html.append("  <style>\n");
        html.append("    :root {\n");
        html.append("      --bg-color: #f8fafc;\n");
        html.append("      --card-bg: #ffffff;\n");
        html.append("      --text-main: #0f172a;\n");
        html.append("      --text-muted: #64748b;\n");
        html.append("      --border-color: #e2e8f0;\n");
        html.append("      --primary: #3b82f6;\n");
        html.append("      --primary-dark: #1d4ed8;\n");
        html.append("      --badge-ok-bg: #dcfce7;\n");
        html.append("      --badge-ok-text: #166534;\n");
        html.append("      --badge-warn-bg: #fee2e2;\n");
        html.append("      --badge-warn-text: #991b1b;\n");
        html.append("    }\n");
        html.append("    body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background: var(--bg-color); color: var(--text-main); margin: 0; padding: 24px; }\n");
        html.append("    .container { max-width: 1200px; margin: 0 auto; }\n");
        html.append("    header { background: var(--card-bg); border: 1px solid var(--border-color); border-radius: 12px; padding: 24px; margin-bottom: 24px; box-shadow: 0 1px 3px rgba(0,0,0,0.05); }\n");
        html.append("    h1 { margin: 0 0 8px 0; font-size: 24px; color: var(--text-main); }\n");
        html.append("    .subtitle { color: var(--text-muted); font-size: 14px; margin: 0; }\n");
        html.append("    .stats-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 16px; margin-bottom: 24px; }\n");
        html.append("    .stat-card { background: var(--card-bg); border: 1px solid var(--border-color); border-radius: 10px; padding: 16px; box-shadow: 0 1px 2px rgba(0,0,0,0.04); }\n");
        html.append("    .stat-title { font-size: 12px; font-weight: 600; text-transform: uppercase; color: var(--text-muted); letter-spacing: 0.5px; }\n");
        html.append("    .stat-value { font-size: 28px; font-weight: 700; color: var(--text-main); margin-top: 6px; }\n");
        html.append("    .stat-flagged { color: #dc2626; }\n");
        html.append("    .controls-panel { background: var(--card-bg); border: 1px solid var(--border-color); border-radius: 10px; padding: 16px; margin-bottom: 16px; display: flex; flex-wrap: wrap; gap: 16px; align-items: center; justify-content: space-between; }\n");
        html.append("    .search-box { flex: 1; min-width: 260px; }\n");
        html.append("    .search-input { width: 100%; box-sizing: border-box; padding: 8px 12px; border: 1px solid var(--border-color); border-radius: 6px; font-size: 14px; outline: none; }\n");
        html.append("    .search-input:focus { border-color: var(--primary); ring: 2px solid #bfdbfe; }\n");
        html.append("    .filter-label { display: flex; align-items: center; gap: 8px; font-size: 14px; font-weight: 500; cursor: pointer; }\n");
        html.append("    .table-container { background: var(--card-bg); border: 1px solid var(--border-color); border-radius: 12px; overflow-x: auto; box-shadow: 0 1px 3px rgba(0,0,0,0.05); }\n");
        html.append("    table { width: 100%; border-collapse: collapse; text-align: left; font-size: 14px; }\n");
        html.append("    th { background: #f1f5f9; padding: 12px 16px; font-weight: 600; color: var(--text-muted); border-bottom: 1px solid var(--border-color); text-transform: uppercase; font-size: 11px; letter-spacing: 0.5px; }\n");
        html.append("    td { padding: 12px 16px; border-bottom: 1px solid var(--border-color); }\n");
        html.append("    tr:last-child td { border-bottom: none; }\n");
        html.append("    tr:hover { background: #f8fafc; }\n");
        html.append("    .badge { display: inline-block; padding: 3px 8px; border-radius: 9999px; font-size: 12px; font-weight: 600; }\n");
        html.append("    .badge-ok { background: var(--badge-ok-bg); color: var(--badge-ok-text); }\n");
        html.append("    .badge-review { background: var(--badge-warn-bg); color: var(--badge-warn-text); }\n");
        html.append("    .remarks-text { font-size: 12px; color: #b91c1c; font-weight: 500; }\n");
        html.append("    .skipped-section { margin-top: 24px; background: #fff1f2; border: 1px solid #fecdd3; border-radius: 10px; padding: 16px; }\n");
        html.append("    .skipped-title { font-weight: 600; color: #9f1239; margin-bottom: 8px; font-size: 14px; }\n");
        html.append("    .skipped-item { font-family: monospace; font-size: 13px; color: #881337; margin: 4px 0; }\n");
        html.append("    footer { margin-top: 32px; text-align: center; font-size: 12px; color: var(--text-muted); }\n");
        html.append("  </style>\n</head>\n<body>\n<div class=\"container\">\n");

        // Header
        html.append("  <header>\n");
        html.append("    <h1>Code Insight: Source Code Complexity Report</h1>\n");
        html.append("    <p class=\"subtitle\">Target: <strong>").append(escapeHtml(targetName)).append("</strong> | Generated on: ").append(timestamp).append(" | Analysis Time: ").append(result.durationMillis()).append(" ms</p>\n");
        html.append("    <p class=\"subtitle\" style=\"margin-top: 6px;\">Thresholds: Cyclomatic &gt; ").append(result.config().maxComplexity())
                .append(" | Nesting &gt; ").append(result.config().maxNesting())
                .append(" | Lines &gt; ").append(result.config().maxLines())
                .append(" <em>(Review flags represent prompts for refactoring or testing scrutiny)</em></p>\n");
        html.append("  </header>\n");

        // Stats Cards
        html.append("  <div class=\"stats-grid\">\n");
        html.append("    <div class=\"stat-card\"><div class=\"stat-title\">Files Scanned</div><div class=\"stat-value\">").append(result.totalFilesScanned()).append("</div></div>\n");
        html.append("    <div class=\"stat-card\"><div class=\"stat-title\">Files Analyzed</div><div class=\"stat-value\">").append(result.analyzedFilesCount()).append("</div></div>\n");
        html.append("    <div class=\"stat-card\"><div class=\"stat-title\">Files Skipped</div><div class=\"stat-value\">").append(result.skippedFiles().size()).append("</div></div>\n");
        html.append("    <div class=\"stat-card\"><div class=\"stat-title\">Methods Analyzed</div><div class=\"stat-value\">").append(result.metrics().size()).append("</div></div>\n");
        html.append("    <div class=\"stat-card\"><div class=\"stat-title\">Flagged for Review</div><div class=\"stat-value stat-flagged\">").append(result.flaggedMethodsCount()).append("</div></div>\n");
        html.append("  </div>\n");

        // Interactive Table Controls
        html.append("  <div class=\"controls-panel\">\n");
        html.append("    <div class=\"search-box\">\n");
        html.append("      <input type=\"text\" id=\"filterInput\" class=\"search-input\" placeholder=\"Filter by file, class, or method name...\" onkeyup=\"filterTable()\">\n");
        html.append("    </div>\n");
        html.append("    <label class=\"filter-label\">\n");
        html.append("      <input type=\"checkbox\" id=\"flaggedOnly\" onchange=\"filterTable()\"> Show Flagged for Review Only\n");
        html.append("    </label>\n");
        html.append("  </div>\n");

        // Table
        html.append("  <div class=\"table-container\">\n");
        html.append("    <table id=\"metricsTable\">\n");
        html.append("      <thead>\n");
        html.append("        <tr>\n");
        html.append("          <th>File</th>\n");
        html.append("          <th>Class</th>\n");
        html.append("          <th>Method</th>\n");
        html.append("          <th>Lines</th>\n");
        html.append("          <th>Range</th>\n");
        html.append("          <th>Cyclomatic</th>\n");
        html.append("          <th>Nesting</th>\n");
        html.append("          <th>Status</th>\n");
        html.append("          <th>Remarks</th>\n");
        html.append("        </tr>\n");
        html.append("      </thead>\n");
        html.append("      <tbody>\n");

        List<MethodMetrics> sorted = result.metrics().stream()
                .sorted(Comparator.comparing(MethodMetrics::fileName)
                        .thenComparing(MethodMetrics::className)
                        .thenComparing(MethodMetrics::methodName))
                .toList();

        for (MethodMetrics m : sorted) {
            boolean review = result.config().isReviewRequired(m);
            String remarks = result.config().getReviewRemarks(m);

            html.append("        <tr data-status=\"").append(review ? "REVIEW" : "OK").append("\">\n");
            html.append("          <td><strong>").append(escapeHtml(m.fileName())).append("</strong></td>\n");
            html.append("          <td>").append(escapeHtml(m.className())).append("</td>\n");
            html.append("          <td><code>").append(escapeHtml(m.methodName())).append("</code></td>\n");
            html.append("          <td>").append(m.lineCount()).append("</td>\n");
            html.append("          <td>").append(m.lineRange()).append("</td>\n");
            html.append("          <td>").append(m.cyclomaticComplexity()).append("</td>\n");
            html.append("          <td>").append(m.maximumNestingDepth()).append("</td>\n");
            html.append("          <td><span class=\"badge ").append(review ? "badge-review" : "badge-ok").append("\">")
                    .append(review ? "REVIEW" : "OK").append("</span></td>\n");
            html.append("          <td>").append(review ? "<span class=\"remarks-text\">" + escapeHtml(remarks) + "</span>" : "-").append("</td>\n");
            html.append("        </tr>\n");
        }

        if (sorted.isEmpty()) {
            html.append("        <tr><td colspan=\"9\" style=\"text-align: center; color: var(--text-muted);\">No methods or constructors found.</td></tr>\n");
        }

        html.append("      </tbody>\n");
        html.append("    </table>\n");
        html.append("  </div>\n");

        // Skipped Files
        if (!result.skippedFiles().isEmpty()) {
            html.append("  <div class=\"skipped-section\">\n");
            html.append("    <div class=\"skipped-title\">Skipped Files / Syntax Errors (").append(result.skippedFiles().size()).append("):</div>\n");
            for (SkippedFileInfo s : result.skippedFiles()) {
                html.append("    <div class=\"skipped-item\"><strong>").append(escapeHtml(s.fileName())).append("</strong>: ")
                        .append(escapeHtml(s.reason().replace("\n", " | "))).append("</div>\n");
            }
            html.append("  </div>\n");
        }

        // Footer & Script
        html.append("  <footer>Code Insight: Source Code Complexity Analyzer &bull; Java Programming Group PBL Project</footer>\n");
        html.append("</div>\n");
        html.append("<script>\n");
        html.append("  function filterTable() {\n");
        html.append("    const query = document.getElementById('filterInput').value.toLowerCase();\n");
        html.append("    const flaggedOnly = document.getElementById('flaggedOnly').checked;\n");
        html.append("    const rows = document.querySelectorAll('#metricsTable tbody tr');\n");
        html.append("    rows.forEach(row => {\n");
        html.append("      const text = row.innerText.toLowerCase();\n");
        html.append("      const status = row.getAttribute('data-status');\n");
        html.append("      const matchesQuery = text.includes(query);\n");
        html.append("      const matchesFlag = !flaggedOnly || status === 'REVIEW';\n");
        html.append("      row.style.display = (matchesQuery && matchesFlag) ? '' : 'none';\n");
        html.append("    });\n");
        html.append("  }\n");
        html.append("</script>\n</body>\n</html>\n");

        if (outputPath.getParent() != null) {
            Files.createDirectories(outputPath.getParent());
        }
        Files.writeString(outputPath, html.toString());
    }

    private static String escapeHtml(String input) {
        if (input == null) return "";
        return input.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}

