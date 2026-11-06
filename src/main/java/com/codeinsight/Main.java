package com.codeinsight;

import com.codeinsight.analyzer.ProjectScanner;
import com.codeinsight.cli.InteractiveMenu;
import com.codeinsight.config.UserPreferences;
import com.codeinsight.model.AnalysisResult;
import com.codeinsight.model.ThresholdConfig;
import com.codeinsight.report.CsvReportExporter;
import com.codeinsight.report.HtmlReportExporter;
import com.codeinsight.report.ReportFormatter;
import com.codeinsight.ui.CodeInsightGUI;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Universal application entry point for Code Insight.
 * Supports Swing GUI, scriptable CLI, and terminal-based interactive menu.
 */
public final class Main {

    private Main() {}

    public static void main(String[] args) {
        if (args.length == 0) {
            // Default behavior: launch GUI if desktop display is available; otherwise interactive menu
            if (!GraphicsEnvironment.isHeadless()) {
                launchGui();
            } else {
                new InteractiveMenu().start();
            }
            return;
        }

        // Check for top-level mode flags
        String firstArg = args[0].toLowerCase();
        if ("--gui".equals(firstArg) || "-g".equals(firstArg)) {
            launchGui();
            return;
        }

        if ("--menu".equals(firstArg) || "-m".equals(firstArg)
                || "--interactive".equals(firstArg) || "-i".equals(firstArg)) {
            new InteractiveMenu().start();
            return;
        }

        if ("--help".equals(firstArg) || "-h".equals(firstArg)) {
            printUsage();
            return;
        }

        // Run CLI mode
        runCli(args);
    }

    private static void launchGui() {
        SwingUtilities.invokeLater(() -> {
            CodeInsightGUI gui = new CodeInsightGUI();
            gui.setVisible(true);
        });
    }

    private static void runCli(String[] args) {
        Path inputPath = null;
        Path csvOutputPath = null;
        Path htmlOutputPath = null;

        ThresholdConfig savedThresholds = UserPreferences.loadThresholds();
        int maxComplexity = savedThresholds.maxComplexity();
        int maxNesting = savedThresholds.maxNesting();
        int maxLines = savedThresholds.maxLines();

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];

            if ("--help".equalsIgnoreCase(arg) || "-h".equalsIgnoreCase(arg)) {
                printUsage();
                return;
            }

            if ("--csv".equalsIgnoreCase(arg)) {
                if (i + 1 >= args.length) {
                    System.err.println("[Error] Missing value for --csv option. Expected path to output CSV file.");
                    System.exit(1);
                }
                csvOutputPath = Path.of(args[++i]);
                continue;
            }

            if ("--html".equalsIgnoreCase(arg)) {
                if (i + 1 >= args.length) {
                    System.err.println("[Error] Missing value for --html option. Expected path to output HTML file.");
                    System.exit(1);
                }
                htmlOutputPath = Path.of(args[++i]);
                continue;
            }

            if ("--max-cyclo".equalsIgnoreCase(arg) || "-c".equalsIgnoreCase(arg)) {
                if (i + 1 >= args.length) {
                    System.err.println("[Error] Missing value for " + arg + ". Expected positive integer.");
                    System.exit(1);
                }
                maxComplexity = parsePositiveInt(args[++i], arg);
                continue;
            }

            if ("--max-nest".equalsIgnoreCase(arg) || "-n".equalsIgnoreCase(arg)) {
                if (i + 1 >= args.length) {
                    System.err.println("[Error] Missing value for " + arg + ". Expected positive integer.");
                    System.exit(1);
                }
                maxNesting = parsePositiveInt(args[++i], arg);
                continue;
            }

            if ("--max-lines".equalsIgnoreCase(arg) || "-l".equalsIgnoreCase(arg)) {
                if (i + 1 >= args.length) {
                    System.err.println("[Error] Missing value for " + arg + ". Expected positive integer.");
                    System.exit(1);
                }
                maxLines = parsePositiveInt(args[++i], arg);
                continue;
            }

            if (arg.startsWith("-")) {
                System.err.println("[Error] Unrecognized option: " + arg);
                System.err.println("Run 'run.bat --help' for available options.");
                System.exit(1);
            }

            if (inputPath == null) {
                inputPath = Path.of(arg);
            } else {
                System.err.println("[Error] Unexpected additional argument: " + arg);
                System.err.println("Run 'run.bat --help' for usage syntax.");
                System.exit(1);
            }
        }

        if (inputPath == null) {
            System.err.println("[Error] No input file or directory specified.");
            printUsage();
            System.exit(1);
        }

        if (!Files.exists(inputPath)) {
            System.err.println("\n[Error] The specified path does not exist: " + inputPath.toAbsolutePath());
            System.err.println("Please check the file/folder name and try again.");
            System.exit(1);
        }

        ThresholdConfig config = new ThresholdConfig(maxComplexity, maxNesting, maxLines);
        try {
            System.out.println("Scanning target: " + inputPath.toAbsolutePath());
            AnalysisResult result = ProjectScanner.scan(inputPath, config, (curr, tot, name) -> {
                System.out.printf("\rAnalyzing [%d/%d]: %-30s", curr, tot, name);
            });
            System.out.println();

            ReportFormatter.printReport(result);

            if (csvOutputPath != null) {
                CsvReportExporter.export(result, csvOutputPath);
                System.out.println("[OK] CSV report saved to: " + csvOutputPath.toAbsolutePath());
            }

            if (htmlOutputPath != null) {
                HtmlReportExporter.export(result, htmlOutputPath);
                System.out.println("[OK] HTML report saved to: " + htmlOutputPath.toAbsolutePath());
            }

        } catch (IOException e) {
            System.err.println("\n[Error] Analysis failed: " + e.getMessage());
            System.exit(1);
        }
    }

    private static int parsePositiveInt(String value, String optionName) {
        try {
            int parsed = Integer.parseInt(value);
            if (parsed > 0) {
                return parsed;
            }
            System.err.println("[Error] Value for " + optionName + " must be greater than 0: " + value);
            System.exit(1);
        } catch (NumberFormatException e) {
            System.err.println("[Error] Invalid integer value for " + optionName + ": '" + value + "'");
            System.exit(1);
        }
        return 1;
    }

    public static void printUsage() {
        System.out.println("Code Insight - Source Code Complexity Analyzer (Java Programming PBL)");
        System.out.println();
        System.out.println("Usage:");
        System.out.println("  run.bat                               Launch modern Desktop GUI");
        System.out.println("  run.bat --menu                        Launch interactive console menu");
        System.out.println("  run.bat <file-or-folder> [options]    Analyze directly from command line");
        System.out.println();
        System.out.println("CLI Options:");
        System.out.println("  -c, --max-cyclo <num>     Set cyclomatic complexity review limit (default: 10)");
        System.out.println("  -n, --max-nest <num>      Set maximum nesting depth review limit (default: 4)");
        System.out.println("  -l, --max-lines <num>     Set method line count review limit (default: 30)");
        System.out.println("      --csv <file.csv>      Export analysis results to a CSV file");
        System.out.println("      --html <file.html>    Export analysis results to an interactive HTML file");
        System.out.println("  -g, --gui                 Launch the graphical desktop interface");
        System.out.println("  -m, --menu                Launch the interactive console menu");
        System.out.println("  -h, --help                Display this help screen");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  run.bat");
        System.out.println("  run.bat samples");
        System.out.println("  run.bat samples\\OrderService.java");
        System.out.println("  run.bat samples --csv outputs\\report.csv --html outputs\\report.html");
        System.out.println("  run.bat samples --max-cyclo 5 --max-nest 2 --max-lines 20");
    }
}
