# Contributing to Code Insight

Welcome to the **Code Insight** group project! This document outlines practical guidelines for team members to collaborate smoothly, maintain code quality, and follow standard software engineering practices.

---

## 1. Team Collaboration & Git Workflow

We use a standard feature-branch workflow:
1. **Never commit directly to `main`** for substantial features or refactors.
2. Create a dedicated branch for each task or sub-module from `main`:
   - Features: `feature/gui-improvements`, `feature/html-export`, `feature/metrics-expansion`
   - Bug fixes: `bugfix/windows-spaces`, `bugfix/syntax-diag`
   - Documentation: `docs/synopsis-update`, `docs/readme-diagrams`

### Branch Workflow Example:
```bash
# 1. Pull latest changes from main
git checkout main
git pull origin main

# 2. Create and switch to a new feature branch
git checkout -b feature/my-cool-feature

# 3. Make changes, compile, and run tests locally
test.bat

# 4. Commit and push your branch
git add .
git commit -m "feat(ui): add live search filter to results table"
git push origin feature/my-cool-feature
```

---

## 2. Commit Message Conventions

We follow the [Conventional Commits](https://www.conventionalcommits.org/) format so that commit logs remain structured and readable:

```text
<type>(<scope>): <short description>
```

### Common Types:
- `feat`: A new feature (e.g., `feat(report): add responsive HTML report exporter`)
- `fix`: A bug fix (e.g., `fix(parser): resolve path quoting for directories with spaces`)
- `docs`: Documentation updates (e.g., `docs(readme): add group presentation script`)
- `test`: Adding or updating automated tests (e.g., `test(analyzer): add tests for lambda isolation`)
- `refactor`: Code restructuring without changing external behavior (e.g., `refactor(model): extract AnalysisResult record`)

---

## 3. Java Code Style Guidelines

- **JDK Version:** Standard Java SE 17 or higher.
- **Pure Java / Zero Dependencies:** The core application and test runner must remain dependency-free. Always use standard JDK APIs (`javax.tools`, `javax.swing`, `java.nio.file`).
- **Data Models:** Use modern Java `record` classes for immutable data transfer objects (`MethodMetrics`, `ThresholdConfig`, `SkippedFileInfo`, `AnalysisResult`).
- **Error Handling:** Never let an unhandled exception in one file crash the whole application. Always catch file-level exceptions, record them in `SkippedFileInfo`, and allow processing to continue.
- **Naming Conventions:**
  - Classes & Records: `PascalCase` (`JavaSourceAnalyzer`, `CodeInsightGUI`)
  - Methods & Variables: `camelCase` (`cyclomaticComplexity`, `maxNesting`)
  - Constants: `UPPER_SNAKE_CASE` (`DEFAULT_MAX_COMPLEXITY`)

---

## 4. Verification Checklist Before Submitting a Pull Request

Before pushing your changes or opening a Pull Request:
- [ ] Run `test.bat` and ensure all 17 automated unit tests pass.
- [ ] Run `run.bat` to launch the Desktop GUI and verify UI controls work as expected.
- [ ] Run `run.bat samples --csv outputs/check.csv --html outputs/check.html` to verify CLI and report exports.
- [ ] Ensure no temporary files (`*.class`, `outputs/*.csv`, `.idea/`, `target/`) are committed (check `.gitignore`).
- [ ] Update `README.md` or `PROJECT_SYNOPSIS.md` if public flags or APIs changed.

---

## 5. Peer Review Process

1. Open a Pull Request into `main`.
2. Assign at least one group teammate to review the changes.
3. Reviewers check that:
   - The code compiles cleanly.
   - All automated tests pass.
   - Variable and method names are self-explanatory.
4. Once approved, perform a clean `Squash and Merge` or `Rebase and Merge` into `main`.

