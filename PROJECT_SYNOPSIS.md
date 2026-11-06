# Project Synopsis: Code Insight: Source Code Complexity Analyzer

**Subject:** Java Programming  
**Project Type:** Group Java PBL Project  
**Project Title:** Code Insight: Source Code Complexity Analyzer  

---

## 1. Introduction

Writing maintainable, readable, and verifiable code is a foundational competency in computer science. As software projects expand, individual methods often accumulate complex branching paths, deeply nested conditionals, and excessive lines of code. This cognitive complexity makes code harder to understand, maintain, and unit test.

**Code Insight** is a lightweight, zero-dependency Java static analysis application developed as a Group Problem-Based Learning (PBL) project. Code Insight uses the official **JDK Compiler API** (`jdk.compiler`) to parse Java source files into Abstract Syntax Trees (AST) and compute method-level complexity indicators:
1. **Cyclomatic Complexity (McCabe Metric):** Quantifies decision paths and branch density.
2. **Maximum Control-Flow Nesting Depth:** Measures how deeply control statements (`if`, loops, `switch`, `catch`) are nested inside each other.
3. **Method Line Count (LOC):** Measures the physical line span of each method or constructor.

Code Insight offers a dynamic, user-driven experience: users can select any Java file or folder at runtime, customize review thresholds, observe progress in real time, view results in an aligned sortable table, filter flagged methods, and export clean reports in both CSV and responsive HTML formats.

---

## 2. Problem Statement

Manual code review is time-consuming and prone to human inconsistency. As group projects grow, methods frequently violate the Single Responsibility Principle (SRP) by accumulating nested loops and complex branch conditions. Existing industrial static analysis tools often require heavyweight build-tool plugins, complex configuration files, or external runtime dependencies.

There is a distinct need for a self-contained, dependency-free Java desktop tool that students, professors, and development teams can run directly to inspect code complexity, highlight methods needing refactoring, and generate professional reports without setup friction.

---

## 3. Objectives

1. **AST-Based Syntax Parsing:** Use the standard JDK Compiler API (`javax.tools.JavaCompiler`, `JavacTask`) to parse `.java` files into syntax trees without compiling target projects or requiring external libraries.
2. **Accurate Method-Level Metric Calculations:**
   - Compute Cyclomatic Complexity based on branching statements, loops, exception handlers, switch cases, and short-circuit operators.
   - Calculate maximum control-flow nesting depth hierarchically.
   - Measure method line spans accurately from compiler line maps.
3. **Dynamic User Interface:**
   - Provide a responsive Swing desktop GUI with file/folder choosers, threshold spinners, and live progress reporting.
   - Use `SwingWorker` background threads to prevent UI freezing on large codebases.
   - Retain full command-line (CLI) and interactive terminal menu options for headless environments.
4. **Interactive Result Exploration:**
   - Display results in a sortable `JTable` with color-coded status badges (`OK` / `REVIEW`) and explicit remarks.
   - Provide instant live text filtering and a "Show Flagged for Review Only" filter.
5. **Fault-Tolerant & Recursive Scanning:**
   - Recursively traverse directory structures using `Files.walkFileTree`.
   - Isolate syntax and I/O errors so that an issue in one file never halts or crashes the analysis of valid files.
6. **Multi-Format Reporting:**
   - Generate standard CSV reports for spreadsheets.
   - Generate self-contained, responsive HTML reports with interactive client-side filtering.
7. **Production Engineering Quality:**
   - Standard Maven (`pom.xml`) configuration with matrix CI via GitHub Actions.
   - Comprehensive automated unit tests covering metrics, parsing errors, directory scanning, and exports.

---

## 4. Scope

- **Target Language:** Java source code (`.java` files, JDK 17+).
- **Execution Level:** Source code AST analysis at parse-time. It does not require target classpath resolution or bytecode execution.
- **Granularity:** Method and constructor level.
- **Safety Boundary:** The tool acts as an informational guide and code review assistant; a flagged method indicates a prompt for human review rather than a defect.

---

## 5. System Architecture and Methodology

The system operates across six structured pipeline stages:

1. **Input Selection & Preference Loading:** Users supply a file or folder via GUI file choosers, CLI arguments, or menu prompts. User preferences (thresholds and last analyzed path) are loaded and persisted using `java.util.prefs.Preferences`.
2. **Recursive Discovery:** `ProjectScanner` uses `Files.walkFileTree` to collect all `.java` files while bypassing inaccessible directories.
3. **AST Parsing:** Each source file is parsed via `javax.tools.JavaCompiler` and `JavacTask`. Syntax diagnostics are gathered via `DiagnosticCollector`.
4. **Error Isolation:** If a syntax error is detected, the error details (line and column numbers) are recorded in a `SkippedFileInfo` record, and scanning proceeds to the next file.
5. **AST Visitor Traversal:** `JavaSourceAnalyzer` uses `TreePathScanner` to visit method declarations and an inner `MetricScanner` to traverse method bodies and compute metrics.
6. **Threshold Evaluation & Reporting:** `ThresholdConfig` evaluates each method against active thresholds. Results are displayed in the GUI table, CLI console, or exported to CSV and HTML files.

---

## 6. Main Modules and Class Responsibilities

```text
com.codeinsight
├── Main                         # Universal entry point: dispatches to GUI, CLI, or Menu
├── model/
│   ├── MethodMetrics            # Immutable record holding measurements for one method
│   ├── ThresholdConfig          # Configurable review thresholds and remarks logic
│   ├── SkippedFileInfo          # Stores file path and syntax diagnostics for skipped files
│   └── AnalysisResult           # Encapsulates a complete run (metrics, skipped, timing)
├── analyzer/
│   ├── JavaSourceAnalyzer       # AST parser and visitor using the JDK Compiler API
│   └── ProjectScanner           # Recursive directory scanner with fault isolation
├── report/
│   ├── ReportFormatter          # Dynamic table formatter for console output
│   ├── CsvReportExporter        # Standard CSV report generator
│   └── HtmlReportExporter       # Responsive HTML report generator with search
├── config/
│   └── UserPreferences          # Persists user settings across sessions
├── ui/
│   ├── CodeInsightGUI           # Swing Desktop GUI with non-blocking SwingWorker
│   └── StatusBadgeRenderer      # Custom JTable cell renderer for status badges
└── cli/
    └── InteractiveMenu          # Terminal-based interactive console menu
```

---

## 7. Metric Calculation Rules

- **Cyclomatic Complexity (McCabe Metric):**
  - Starts at base 1.
  - Adds +1 for each: `if`, ternary (`? :`), `for`, `enhanced-for`, `while`, `do-while`, `catch`, non-default `switch` case, `&&`, and `||`.
  - Excludes comments, string literals, lambda bodies, and nested class declarations.
- **Maximum Nesting Depth:**
  - Measures the peak depth of nested control structures (`if`, loops, `switch`, `catch`, ternary). Sibling structures at the same depth do not increment the counter.
- **Method Line Count (LOC):**
  - Computed from compiler source position line mappings (`endLine - startLine + 1`).
- **Default Review Thresholds:**
  - Cyclomatic Complexity: `> 10`
  - Nesting Depth: `> 4`
  - Method Line Count: `> 30`

---

## 8. Verification and Test Results

Code Insight includes an automated test suite executed via `test.bat` and Maven:
- **`ThresholdConfigTest` (5 tests):** Validates default limits, boundary conditions, and remark formatting.
- **`JavaSourceAnalyzerTest` (7 tests):** Validates basic methods, complex loops/branches, deep nesting, comment/string immunity, constructor naming, lambda independence, and syntax error diagnostics.
- **`ProjectScannerTest` (3 tests):** Validates recursive scanning across subdirectories, fault tolerance on malformed files, and invalid path handling.
- **`CsvReportExporterTest` (1 test):** Validates CSV header structure, row counts, escaping, and metrics.
- **`HtmlReportExporterTest` (1 test):** Validates HTML generation, badges, and statistics cards.

**Results:** 17 Passed, 0 Failed (100% Success).

---

## 9. Conclusion

Code Insight successfully delivers a complete, professional, GitHub-ready Java group PBL project. By combining the official JDK Compiler API, modern Java records, non-blocking Swing threading, recursive file visiting, multi-format reporting, and automated CI pipelines, Code Insight serves as a comprehensive demonstration of real-world software engineering in Java.
