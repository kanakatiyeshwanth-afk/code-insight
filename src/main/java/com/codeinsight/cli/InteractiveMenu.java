package com.codeinsight.cli;

import com.codeinsight.analyzer.ProjectScanner;
import com.codeinsight.config.UserPreferences;
import com.codeinsight.model.AnalysisResult;
import com.codeinsight.model.ThresholdConfig;
import com.codeinsight.report.CsvReportExporter;
import com.codeinsight.report.HtmlReportExporter;
import com.codeinsight.report.ReportFormatter;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Scanner;

/**
 * Provides an interactive, menu-driven console interface for Code Insight.
 */
public final class InteractiveMenu {
    private final Scanner scanner;
    private ThresholdConfig config;

    public InteractiveMenu() {
        this.scanner = new Scanner(System.in);
        this.config = UserPreferences.loadThresholds();
    }

    public void start() {
        boolean running = true;
        while (running) {
            printBanner();
            printMenu();
            System.out.print("Select an option (1-7): ");
            String choice = readLine();
            if (choice == null) {
                break;
            }

            boolean pauseBeforeMenu = true;
            switch (choice) {
                case "1" -> runAnalysis(false, false);
                case "2" -> {
                    configureThresholds();
                    pauseBeforeMenu = false;
                }
                case "3" -> runAnalysis(true, false);
                case "4" -> runAnalysis(false, true);
                case "5" -> showMetricGuide();
                case "6" -> runQuickDemo();
                case "7" -> {
                    System.out.println("\nThank you for using Code Insight! Exiting...\n");
                    running = false;
                    pauseBeforeMenu = false;
                }
                default -> System.out.println("\n[!] Invalid selection. Please enter a number between 1 and 7.\n");
            }

            if (running && pauseBeforeMenu) {
                System.out.print("\nPress ENTER to return to the main menu...");
                readLine();
            }
        }
    }

    private String readLine() {
        if (scanner.hasNextLine()) {
            return scanner.nextLine().trim();
        }
        return null;
    }

    private void printBanner() {
        System.out.println();
        System.out.println("=================================================================");
        System.out.println("       CODE INSIGHT: SOURCE CODE COMPLEXITY ANALYZER (PBL)       ");
        System.out.println("=================================================================");
    }

    private void printMenu() {
        System.out.println("Current Thresholds: Cyclomatic > " + config.maxComplexity()
                + " | Nesting > " + config.maxNesting()
                + " | Lines > " + config.maxLines());
        System.out.println("-----------------------------------------------------------------");
        System.out.println("1. Analyze a Java file or folder");
        System.out.println("2. View / Configure review thresholds");
        System.out.println("3. Analyze and export report to CSV file");
        System.out.println("4. Analyze and export report to HTML file");
        System.out.println("5. View metric explanations and review guidelines");
        System.out.println("6. Run quick demo on 'samples' folder");
        System.out.println("7. Exit");
        System.out.println("-----------------------------------------------------------------");
    }

    private void runAnalysis(boolean exportCsv, boolean exportHtml) {
        System.out.println();
        String defaultPath = UserPreferences.loadLastPath();
        System.out.print("Enter Java file or directory path [default: " + defaultPath + "]: ");
        String pathInput = readLine();
        if (pathInput == null) return;
        if (pathInput.isEmpty()) {
            pathInput = defaultPath;
        }

        Path targetPath = Path.of(pathInput);
        if (!Files.exists(targetPath)) {
            System.err.println("\n[Error] Path does not exist: " + targetPath.toAbsolutePath());
            return;
        }

        Path csvPath = null;
        if (exportCsv) {
            System.out.print("Enter CSV report output path [default: outputs/report.csv]: ");
            String csvInput = readLine();
            if (csvInput == null) return;
            csvPath = Path.of(csvInput.isEmpty() ? "outputs/report.csv" : csvInput);
        }

        Path htmlPath = null;
        if (exportHtml) {
            System.out.print("Enter HTML report output path [default: outputs/report.html]: ");
            String htmlInput = readLine();
            if (htmlInput == null) return;
            htmlPath = Path.of(htmlInput.isEmpty() ? "outputs/report.html" : htmlInput);
        }

        try {
            AnalysisResult result = ProjectScanner.scan(targetPath, config, (curr, tot, name) -> {
                System.out.printf("\rAnalyzing [%d/%d]: %-30s", curr, tot, name);
            });
            System.out.println();

            ReportFormatter.printReport(result);
            UserPreferences.saveLastPath(pathInput);

            if (csvPath != null) {
                CsvReportExporter.export(result, csvPath);
                System.out.println("[OK] CSV report saved to: " + csvPath.toAbsolutePath());
            }
            if (htmlPath != null) {
                HtmlReportExporter.export(result, htmlPath);
                System.out.println("[OK] HTML report saved to: " + htmlPath.toAbsolutePath());
            }
        } catch (Exception e) {
            System.err.println("\n[Error] Analysis failed: " + e.getMessage());
        }
    }

    private void runQuickDemo() {
        System.out.println("\n--- Running Quick Demo on 'samples' directory ---");
        Path samplePath = Path.of("samples");
        if (!Files.exists(samplePath)) {
            System.err.println("[Error] 'samples' directory was not found in the current working directory.");
            return;
        }
        try {
            AnalysisResult result = ProjectScanner.scan(samplePath, config, null);
            ReportFormatter.printReport(result);
        } catch (Exception e) {
            System.err.println("[Error] Demo failed: " + e.getMessage());
        }
    }

    private void configureThresholds() {
        boolean inSubMenu = true;
        while (inSubMenu) {
            System.out.println();
            System.out.println("-----------------------------------------------------------------");
            System.out.println("                  CONFIGURE REVIEW THRESHOLDS                    ");
            System.out.println("-----------------------------------------------------------------");
            System.out.printf("1. Max Cyclomatic Complexity limit : %d%n", config.maxComplexity());
            System.out.printf("2. Max Nesting Depth limit          : %d%n", config.maxNesting());
            System.out.printf("3. Max Method Line Count limit      : %d%n", config.maxLines());
            System.out.println("4. Reset to default thresholds (10 / 4 / 30)");
            System.out.println("5. Back to main menu");
            System.out.println("-----------------------------------------------------------------");
            System.out.print("Enter selection (1-5): ");

            String option = readLine();
            if (option == null) return;

            switch (option) {
                case "1" -> {
                    int val = promptPositiveInt("Enter new Max Cyclomatic Complexity threshold (> 0): ", config.maxComplexity());
                    config = new ThresholdConfig(val, config.maxNesting(), config.maxLines());
                    UserPreferences.saveThresholds(config);
                    System.out.println("[OK] Cyclomatic complexity threshold updated to " + val);
                }
                case "2" -> {
                    int val = promptPositiveInt("Enter new Max Nesting Depth threshold (> 0): ", config.maxNesting());
                    config = new ThresholdConfig(config.maxComplexity(), val, config.maxLines());
                    UserPreferences.saveThresholds(config);
                    System.out.println("[OK] Nesting depth threshold updated to " + val);
                }
                case "3" -> {
                    int val = promptPositiveInt("Enter new Max Method Line Count threshold (> 0): ", config.maxLines());
                    config = new ThresholdConfig(config.maxComplexity(), config.maxNesting(), val);
                    UserPreferences.saveThresholds(config);
                    System.out.println("[OK] Method line count threshold updated to " + val);
                }
                case "4" -> {
                    config = ThresholdConfig.defaultConfig();
                    UserPreferences.saveThresholds(config);
                    System.out.println("[OK] Thresholds reset to defaults (Cyclo: 10, Nesting: 4, Lines: 30).");
                }
                case "5" -> inSubMenu = false;
                default -> System.out.println("[!] Invalid choice. Enter 1 to 5.");
            }
        }
    }

    private int promptPositiveInt(String prompt, int current) {
        while (true) {
            System.out.print(prompt);
            String line = readLine();
            if (line == null || line.isEmpty()) {
                return current;
            }
            try {
                int value = Integer.parseInt(line);
                if (value > 0) {
                    return value;
                }
                System.out.println("[!] Please enter a positive number greater than 0.");
            } catch (NumberFormatException e) {
                System.out.println("[!] Invalid integer: '" + line + "'. Please enter a valid number.");
            }
        }
    }

    private void showMetricGuide() {
        System.out.println();
        System.out.println("=================================================================");
        System.out.println("               CODE INSIGHT - METRIC DEFINITIONS GUIDE           ");
        System.out.println("=================================================================");
        System.out.println("1. CYCLOMATIC COMPLEXITY (McCabe Metric):");
        System.out.println("   - Measures the number of linearly independent paths in code.");
        System.out.println("   - Base score starts at 1 for the method entry.");
        System.out.println("   - +1 is added for each decision point:");
        System.out.println("     * Conditional statements: if, ternary (? :)");
        System.out.println("     * Loops: for, enhanced-for, while, do-while");
        System.out.println("     * Exception handlers: catch blocks");
        System.out.println("     * Switch cases: each non-default case label");
        System.out.println("     * Logical operators: && and ||");
        System.out.println("   - Recommended limit: <= 10. Higher complexity often signals");
        System.out.println("     methods that are hard to unit test and maintain.");
        System.out.println();
        System.out.println("2. MAXIMUM NESTING DEPTH:");
        System.out.println("   - Measures how deeply control-flow blocks are nested within");
        System.out.println("     one another (e.g. an if inside a while inside another if).");
        System.out.println("   - Recommended limit: <= 4. Deep nesting (the 'arrow anti-pattern')");
        System.out.println("     makes code difficult for developers to read and reason about.");
        System.out.println();
        System.out.println("3. METHOD LINE COUNT (LOC):");
        System.out.println("   - Measures physical lines from method signature to closing brace.");
        System.out.println("   - Recommended limit: <= 30-50 lines.");
        System.out.println("   - Helps spot 'God methods' that do too much and violate SRP.");
        System.out.println();
        System.out.println("4. REVIEW STATUS:");
        System.out.println("   - 'OK': Method conforms to all configured thresholds.");
        System.out.println("   - 'REVIEW': Method breached at least one threshold.");
        System.out.println("     A REVIEW flag is a prompt for human refactoring, not a defect.");
        System.out.println("=================================================================");
    }
}

