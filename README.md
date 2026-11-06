# Code Insight: Source Code Complexity Analyzer

[![Java](https://img.shields.io/badge/Java-17%2B-blue.svg)](https://www.oracle.com/java/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Build Status](https://img.shields.io/badge/Build-Passing-brightgreen.svg)]()
[![Dependencies](https://img.shields.io/badge/Dependencies-Zero%20External-orange.svg)]()

> **Java Programming Group PBL Project**  
> An automated, zero-dependency Java static code complexity analyzer powered by the standard **JDK Compiler API** (`jdk.compiler`), featuring a modern desktop GUI, scriptable CLI, interactive terminal menu, fault-tolerant scanning, and exportable CSV and HTML reports.

---

## 1. Project Overview

As software codebases grow, methods often accumulate complex branching logic, deeply nested control structures, and excessive line lengths. This cognitive complexity makes code harder to understand, test, and maintain.

**Code Insight** provides an automated, objective first-pass review of Java source code. It parses `.java` files directly into Abstract Syntax Trees (AST) using the official Java Compiler API and calculates three core software engineering metrics:
1. **Cyclomatic Complexity (McCabe Metric):** Quantifies decision paths and branch density.
2. **Maximum Control-Flow Nesting Depth:** Measures how deeply control statements (`if`, loops, `switch`, `catch`) are nested within one another.
3. **Method Line Count (LOC):** Measures the physical line span of each declared method or constructor.

Code Insight runs completely dynamically: users can select any Java source file or project directory at runtime, adjust thresholds on the fly, observe real-time progress, sort and filter results, and export comprehensive CSV and interactive HTML reports.

---

## 2. Visual Interface & Features

```text
+---------------------------------------------------------------------------------------+
|  Code Insight: Source Code Complexity Analyzer (PBL Group Project)                    |
+---------------------------------------------------------------------------------------+
|  [Target Path: C:\path\to\project                ] [Browse File...] [Browse Folder..] |
|  Max Cyclo: [ 10 ]  Max Nesting: [ 4 ]  Max Lines: [ 30 ]  [Reset]   [ Run Analysis ] |
|  Status: Analysis completed in 42 ms. Found 5 methods across 3 analyzed file(s).      |
+---------------------------------------------------------------------------------------+
|  [FILES SCANNED: 4]  [ANALYZED: 3]  [SKIPPED: 1]  [METHODS: 5]  [FLAGGED REVIEW: 1]   |
+---------------------------------------------------------------------------------------+
|  Filter Table: [ filter text...       ]  [X] Show Flagged for Review Only             |
|---------------------------------------------------------------------------------------|
|  FILE                 CLASS           METHOD        RANGE  LINES  CYCLO  NEST  STATUS |
|---------------------------------------------------------------------------------------|
|  ComplexityDemo.java  ComplexityDemo  evaluate      5-18      14      6     5  REVIEW |
|  MathUtility.java     MathUtility     factorial     8-17      10      3     1  OK     |
|  MathUtility.java     MathUtility     isPrime       19-27      9      6     2  OK     |
|  OrderService.java    OrderService    discount      19-21      3      2     0  OK     |
|  OrderService.java    OrderService    shippingCost  4-17      14      6     2  OK     |
+---------------------------------------------------------------------------------------+
|  * A 'REVIEW' flag is a prompt for refactoring scrutiny, not proof of bad code.       |
|                                [View Skipped Files]  [Export CSV...]  [Export HTML...] |
+---------------------------------------------------------------------------------------+
```

### Key Capabilities:
- **Responsive Desktop GUI:** Built with pure Java Swing and `SwingWorker` background threading to ensure the interface never freezes, even when analyzing large enterprise codebases.
- **Dynamic File / Folder Selection:** Choose any target path using native file and directory choosers.
- **Live Search & Filter:** Filter table rows in real-time by method name, class name, or file name, or toggle the "Show Flagged for Review Only" checkbox.
- **Customizable Review Thresholds:** Modify thresholds at runtime via spinners or CLI flags. Settings persist across application sessions via `UserPreferences`.
- **Fault-Tolerant Scanning:** Recursively traverses directories using `Files.walkFileTree`. If a file has syntax errors, its exact line and column diagnostics are captured and reported in a skipped files summary, while valid files continue being analyzed without interruption.
- **Multi-Format Export:** Export fresh results to standard CSV and modern, responsive HTML reports with client-side table search and statistics cards.
- **Dual Operating Modes:** Run with the Desktop GUI, headless scriptable CLI, or interactive console menu.
- **100% Dependency-Free:** Uses exclusively the standard Java Development Kit (JDK).

---

## 3. Project Architecture

```text
code insight/
├── .github/
│   └── workflows/
│       └── build.yml                     # GitHub Actions CI matrix (JDK 17 & 21)
├── pom.xml                               # Standard Maven build configuration
├── run.bat                               # Windows application runner (GUI/CLI/Menu)
├── test.bat                              # Automated test suite executor
├── CONTRIBUTING.md                       # Team git workflow and contribution guide
├── LICENSE                               # MIT Open Source License
├── PROJECT_SYNOPSIS.md                   # Formal PBL project synopsis
├── README.md                             # Project documentation
├── samples/                              # Demonstration test source files
│   ├── ComplexityDemo.java               # Sample with high nesting (triggers review)
│   ├── OrderService.java                 # Business logic sample with switch/branches
│   ├── invalid/
│   │   └── BrokenSyntaxSample.java       # Malformed syntax to demonstrate fault isolation
│   ├── nested/
│   │   └── MathUtility.java              # Subfolder sample verifying recursive scan
│   └── reports/                          # Checked-in demonstration sample reports
│       ├── sample-report.csv
│       └── sample-report.html
└── src/
    ├── main/java/com/codeinsight/
    │   ├── Main.java                     # Application entry point & CLI dispatcher
    │   ├── model/
    │   │   ├── MethodMetrics.java        # Record holding single method metrics
    │   │   ├── ThresholdConfig.java      # Configurable review thresholds & remarks
    │   │   ├── SkippedFileInfo.java      # Record tracking skipped file diagnostics
    │   │   └── AnalysisResult.java       # Full analysis run summary container
    │   ├── analyzer/
    │   │   ├── JavaSourceAnalyzer.java   # AST parser & metric calculator (JDK Compiler API)
    │   │   └── ProjectScanner.java       # Recursive scanner with fault tolerance
    │   ├── report/
    │   │   ├── ReportFormatter.java      # Console tabular report generator
    │   │   ├── CsvReportExporter.java    # Standard CSV file exporter
    │   │   └── HtmlReportExporter.java   # Responsive HTML report exporter
    │   ├── config/
    │   │   └── UserPreferences.java      # User setting persistence across sessions
    │   ├── ui/
    │   │   ├── CodeInsightGUI.java       # Swing desktop GUI with SwingWorker
    │   │   └── StatusBadgeRenderer.java  # Color-coded JTable cell badge renderer
    │   └── cli/
    │       └── InteractiveMenu.java      # Interactive terminal console menu
    └── test/java/com/codeinsight/
        ├── TestRunner.java               # Standalone automated test runner
        ├── analyzer/
        │   ├── JavaSourceAnalyzerTest.java
        │   └── ProjectScannerTest.java
        ├── model/
        │   └── ThresholdConfigTest.java
        └── report/
            ├── CsvReportExporterTest.java
            └── HtmlReportExporterTest.java
```

---

## 4. Requirements and Setup

- **Java Development Kit (JDK):** JDK 17 or higher (tested on JDK 17, 21, and 26).
- **Environment:** Ensure `java` and `javac` are available on your system `PATH`.
- **Operating System:** Windows, Linux, or macOS.
- **External Dependencies:** **Zero.**

### Verifying Java Setup
Open your terminal and verify:
```bash
javac -version
java -version
```

---

## 5. How to Run

### A. Launch Desktop GUI (Default)
Simply run without arguments:
```bat
run.bat
```
*(Or specify `run.bat --gui`)*

### B. Command-Line Interface (CLI)
Analyze the sample directory:
```bat
run.bat samples
```

Analyze a single Java file:
```bat
run.bat samples\OrderService.java
```

Customize review thresholds via CLI flags:
```bat
run.bat samples --max-cyclo 5 --max-nest 2 --max-lines 20
```

Export reports to CSV and HTML simultaneously:
```bat
run.bat samples --csv outputs\report.csv --html outputs\report.html
```

Display CLI help:
```bat
run.bat --help
```

### C. Interactive Console Menu
Run the guided terminal menu:
```bat
run.bat --menu
```

### D. Maven Commands (If Maven is installed)
```bash
# Compile and run automated tests
mvn clean test

# Package standalone JAR
mvn package

# Run application via Maven
mvn exec:java
```

---

## 6. Metric Definitions and Rules

| Metric | Rule & AST Nodes Monitored | Default Threshold |
|---|---|---|
| **Cyclomatic Complexity** | Base score of `1`. Increments by `+1` for: `if`, ternary (`? :`), `for`, `enhanced-for`, `while`, `do-while`, `catch`, non-default `switch` cases, and boolean operators (`&&`, `\|\|`). Lambda bodies and nested class declarations are isolated and do not inflate the outer method. | `> 10` |
| **Maximum Nesting Depth** | Measures the peak depth of nested control-flow structures (`if`, loops, `switch`, `catch`, ternary). Sequential blocks at the same depth level do not increase the depth counter. | `> 4` |
| **Method Line Count (LOC)** | Physical line count calculated from compiler AST line position maps: `endLine - startLine + 1`. | `> 30` |

> **Important Note:** A `REVIEW` status is an objective prompt for human inspection and potential refactoring, not proof that the code contains defects.

---

## 7. Automated Testing & Verification

Code Insight includes 17 automated unit tests covering all core modules. Run the test suite using `test.bat`:

```bat
test.bat
```

### Test Suite Summary:
- **`ThresholdConfigTest` (5 tests):** Validates default thresholds, threshold boundaries, review conditions, and remark generation.
- **`JavaSourceAnalyzerTest` (7 tests):** Tests basic methods, branching complexity, loops, deep nesting, constructor naming, lambda isolation, and syntax error diagnostic messages.
- **`ProjectScannerTest` (3 tests):** Validates recursive directory traversal, fault-tolerant recovery from syntax errors, and invalid path handling.
- **`CsvReportExporterTest` (1 test):** Validates CSV header structure, row counts, escaping, and formatting.
- **`HtmlReportExporterTest` (1 test):** Validates HTML generation, CSS inclusion, stat cards, and status badges.

---

## 8. Group Presentation Demonstration Script

This structured walkthrough is designed for team members to present the project during PBL evaluation:

### Phase 1: Introduction & Problem Statement (Speaker 1)
> *"Good morning. Today, our group is presenting our Java Programming PBL project: **Code Insight: Source Code Complexity Analyzer**. As software projects expand, methods often accumulate complex branching and deep nesting, making them error-prone and hard to test. Code Insight automates code review by measuring cyclomatic complexity, nesting depth, and method line counts using standard Java AST parsing."*

### Phase 2: Architecture & Technology (Speaker 2)
> *"Unlike tools that rely on basic regex or heavy third-party plugins, Code Insight uses Java's built-in `jdk.compiler` API to parse source code into an Abstract Syntax Tree (AST). We designed our project with clean object-oriented principles: `ProjectScanner` traverses folders recursively, `JavaSourceAnalyzer` computes metrics, and `CodeInsightGUI` uses a background `SwingWorker` thread to ensure smooth, responsive performance without UI freezing."*

### Phase 3: Live Demonstration (Speaker 3)
1. **Launch Desktop GUI:** Run `run.bat`. Show the modern desktop window with file choosers, threshold spinners, and stats cards.
2. **Execute Analysis on `samples`:** Click **Run Analysis**. Point out the progress bar, the 5 discovered methods, and the summary statistics.
3. **Show Fault Tolerance:** Click **View Skipped Files**. Show that `samples\invalid\BrokenSyntaxSample.java` had a syntax error that was caught with line and column numbers, **without crashing** the analysis of the valid files.
4. **Demonstrate Dynamic Thresholds & Filters:** Lower the `Max Cyclo` threshold to `5`. Notice how `OrderService.shippingCost` immediately updates to `REVIEW`. Check the **Show Flagged for Review Only** box to demonstrate instant table filtering.
5. **Export Reports:** Click **Export HTML** and open `complexity-report.html` in the browser to showcase the responsive report cards and client-side table filter.

### Phase 4: Conclusion & Q&A (Speaker 4)
> *"Through this PBL project, our team gained hands-on experience with compiler technology, AST visitors, modern Java records, non-blocking multithreading, automated testing, and CI/CD pipelines. Thank you, and we welcome your questions!"*

---

## 9. Limitations & Future Extensions

- **Syntactic Analysis:** Code Insight analyzes syntax without requiring external classpath dependencies to be resolved.
- **Constructs Analyzed:** Focuses on declared methods and constructors. Static initializers and field declarations are not reported as methods.
- **Future Enhancements:**
  - Additional software metrics (Cognitive Complexity, Halstead volume).
  - Git integration to calculate complexity deltas across pull requests.
  - IDE plugin extensions for IntelliJ IDEA and VS Code.

---

## 10. Contributing and License

- **Contributing:** Please refer to [CONTRIBUTING.md](CONTRIBUTING.md) for branching, commit guidelines, and code review standards.
- **License:** Licensed under the [MIT License](LICENSE).
