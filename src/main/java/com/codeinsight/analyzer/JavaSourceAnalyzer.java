package com.codeinsight.analyzer;

import com.codeinsight.model.MethodMetrics;
import com.sun.source.tree.*;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.TreeScanner;
import com.sun.source.util.Trees;

import javax.tools.*;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Parses Java source code using the JDK Compiler API (jdk.compiler)
 * and calculates method-level metrics from the syntax tree.
 */
public final class JavaSourceAnalyzer {

    private JavaSourceAnalyzer() {}

    /**
     * Analyzes a single .java source file on disk.
     */
    public static List<MethodMetrics> analyze(Path sourceFile) throws IOException {
        if (sourceFile == null) {
            throw new IllegalArgumentException("Source file path cannot be null.");
        }
        if (!Files.exists(sourceFile)) {
            throw new IOException("File does not exist: " + sourceFile);
        }
        if (!Files.isRegularFile(sourceFile)) {
            throw new IOException("Path is not a regular file: " + sourceFile);
        }
        if (!sourceFile.getFileName().toString().endsWith(".java")) {
            throw new IOException("File is not a Java source file (*.java expected): " + sourceFile.getFileName());
        }

        String content = Files.readString(sourceFile);
        return analyzeSource(content, sourceFile.getFileName().toString());
    }

    /**
     * Analyzes Java source code provided as an in-memory string.
     * Useful for automated unit testing and dynamic analysis.
     */
    public static List<MethodMetrics> analyzeSource(String sourceContent, String fileName) throws IOException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new IllegalStateException("A full JDK is required. Make sure you are running on a JDK and javac is on PATH.");
        }

        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        try (StandardJavaFileManager standardFileManager = compiler.getStandardFileManager(diagnostics, null, null)) {
            JavaFileObject fileObject = new SimpleSourceFileObject(fileName, sourceContent);
            JavacTask task = (JavacTask) compiler.getTask(
                    null, standardFileManager, diagnostics,
                    List.of("-proc:none"), null, Collections.singletonList(fileObject));

            Iterable<? extends CompilationUnitTree> units = task.parse();

            // Check for syntax/parsing errors
            List<Diagnostic<? extends JavaFileObject>> errors = diagnostics.getDiagnostics().stream()
                    .filter(d -> d.getKind() == Diagnostic.Kind.ERROR).toList();
            if (!errors.isEmpty()) {
                StringBuilder errorSummary = new StringBuilder();
                errorSummary.append("Syntax error in ").append(fileName).append(":");
                for (Diagnostic<? extends JavaFileObject> d : errors.stream().limit(3).toList()) {
                    errorSummary.append(System.lineSeparator())
                            .append("  -> Line ").append(d.getLineNumber())
                            .append(", Col ").append(d.getColumnNumber())
                            .append(": ").append(d.getMessage(null));
                }
                if (errors.size() > 3) {
                    errorSummary.append(System.lineSeparator())
                            .append("  -> (and ").append(errors.size() - 3).append(" more syntax error(s))");
                }
                throw new IOException(errorSummary.toString());
            }

            Trees trees = Trees.instance(task);
            List<MethodMetrics> results = new ArrayList<>();

            for (CompilationUnitTree unit : units) {
                new TreePathScanner<Void, String>() {
                    @Override
                    public Void visitClass(ClassTree node, String parentName) {
                        String name = node.getSimpleName().toString();
                        String qualifiedName = parentName == null || parentName.isBlank()
                                ? name : parentName + "." + name;
                        return super.visitClass(node, qualifiedName);
                    }

                    @Override
                    public Void visitMethod(MethodTree node, String className) {
                        if (node.getBody() == null) return null; // Ignore abstract / interface declarations

                        long start = trees.getSourcePositions().getStartPosition(unit, node);
                        long end = trees.getSourcePositions().getEndPosition(unit, node);
                        long startLine = unit.getLineMap().getLineNumber(start);
                        long endLine = unit.getLineMap().getLineNumber(end);

                        String methodName = node.getName().contentEquals("<init>")
                                ? className.substring(className.lastIndexOf('.') + 1)
                                : node.getName().toString();

                        MetricScanner scanner = new MetricScanner();
                        scanner.scan(node.getBody(), 0);

                        results.add(new MethodMetrics(
                                fileName,
                                className == null || className.isBlank() ? "<top-level>" : className,
                                methodName,
                                startLine,
                                endLine,
                                scanner.complexity,
                                scanner.maximumDepth
                        ));
                        return null; // Nested declarations handled independently
                    }
                }.scan(unit, "");
            }

            return results;
        }
    }

    /**
     * AST Scanner that counts branching constructs and calculates peak nesting depth.
     */
    private static final class MetricScanner extends TreeScanner<Void, Integer> {
        private int complexity = 1;
        private int maximumDepth = 0;

        private int descend(Integer depth) {
            int childDepth = depth + 1;
            maximumDepth = Math.max(maximumDepth, childDepth);
            return childDepth;
        }

        @Override public Void visitIf(IfTree n, Integer d) { complexity++; return super.visitIf(n, descend(d)); }
        @Override public Void visitForLoop(ForLoopTree n, Integer d) { complexity++; return super.visitForLoop(n, descend(d)); }
        @Override public Void visitEnhancedForLoop(EnhancedForLoopTree n, Integer d) { complexity++; return super.visitEnhancedForLoop(n, descend(d)); }
        @Override public Void visitWhileLoop(WhileLoopTree n, Integer d) { complexity++; return super.visitWhileLoop(n, descend(d)); }
        @Override public Void visitDoWhileLoop(DoWhileLoopTree n, Integer d) { complexity++; return super.visitDoWhileLoop(n, descend(d)); }
        @Override public Void visitConditionalExpression(ConditionalExpressionTree n, Integer d) { complexity++; return super.visitConditionalExpression(n, descend(d)); }
        @Override public Void visitCatch(CatchTree n, Integer d) { complexity++; return super.visitCatch(n, descend(d)); }

        @Override
        public Void visitSwitch(SwitchTree n, Integer d) {
            for (CaseTree c : n.getCases()) {
                if (c.getExpressions() != null && !c.getExpressions().isEmpty()) complexity++;
            }
            return super.visitSwitch(n, descend(d));
        }

        @Override
        public Void visitSwitchExpression(SwitchExpressionTree n, Integer d) {
            for (CaseTree c : n.getCases()) {
                if (c.getExpressions() != null && !c.getExpressions().isEmpty()) complexity++;
            }
            return super.visitSwitchExpression(n, descend(d));
        }

        @Override
        public Void visitBinary(BinaryTree n, Integer d) {
            if (n.getKind() == Tree.Kind.CONDITIONAL_AND || n.getKind() == Tree.Kind.CONDITIONAL_OR) {
                complexity++;
            }
            return super.visitBinary(n, d);
        }

        // Inner classes and lambda bodies are treated as separate constructs
        @Override public Void visitLambdaExpression(LambdaExpressionTree n, Integer d) { return null; }
        @Override public Void visitClass(ClassTree n, Integer d) { return null; }
    }

    /**
     * In-memory representation of Java source for compiler task.
     */
    private static final class SimpleSourceFileObject extends SimpleJavaFileObject {
        private final String content;

        SimpleSourceFileObject(String name, String content) {
            super(URI.create("string:///" + name.replace('\\', '/')), Kind.SOURCE);
            this.content = content;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return content;
        }
    }
}
