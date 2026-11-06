package com.codeinsight.analyzer;

import com.codeinsight.model.MethodMetrics;

import java.io.IOException;
import java.util.List;

/**
 * Unit tests for JavaSourceAnalyzer AST parser and metric calculation.
 */
public class JavaSourceAnalyzerTest {

    public void testBasicMethod() throws IOException {
        String code = """
            public class Sample {
                public void hello() {
                    System.out.println("Hello world");
                }
            }
            """;
        List<MethodMetrics> metrics = JavaSourceAnalyzer.analyzeSource(code, "Sample.java");
        assert metrics.size() == 1 : "Expected 1 method";
        MethodMetrics m = metrics.get(0);
        assert "hello".equals(m.methodName()) : "Method name should be hello";
        assert m.cyclomaticComplexity() == 1 : "Base complexity should be 1";
        assert m.maximumNestingDepth() == 0 : "Nesting depth should be 0";
    }

    public void testBranchingAndLoopComplexity() throws IOException {
        String code = """
            public class LogicSample {
                public int compute(int x, int y, boolean flag) {
                    if (x > 0 && y > 0) { // +1 if, +1 &&
                        while (x > 10) {  // +1 while
                            x--;
                        }
                    } else if (flag || y < 0) { // +1 if, +1 ||
                        for (int i = 0; i < 5; i++) { // +1 for
                            x += i;
                        }
                    }
                    return x;
                }
            }
            """;
        List<MethodMetrics> metrics = JavaSourceAnalyzer.analyzeSource(code, "LogicSample.java");
        assert metrics.size() == 1 : "Expected 1 method";
        MethodMetrics m = metrics.get(0);
        // Base 1 + if(1) + &&(1) + while(1) + else-if(1) + ||(1) + for(1) = 7
        assert m.cyclomaticComplexity() == 7 : "Expected complexity 7, but was: " + m.cyclomaticComplexity();
    }

    public void testNestingDepthCalculation() throws IOException {
        String code = """
            public class NestingSample {
                public void deep(int a, int b, int c) {
                    if (a > 0) {             // depth 1
                        for (int i = 0; i < a; i++) { // depth 2
                            while (b > 0) {   // depth 3
                                if (c > 0) {  // depth 4
                                    System.out.println("deep");
                                }
                            }
                        }
                    }
                }
            }
            """;
        List<MethodMetrics> metrics = JavaSourceAnalyzer.analyzeSource(code, "NestingSample.java");
        assert metrics.size() == 1 : "Expected 1 method";
        MethodMetrics m = metrics.get(0);
        assert m.maximumNestingDepth() == 4 : "Expected maximum nesting depth 4, but was: " + m.maximumNestingDepth();
    }

    public void testCommentsAndStringsDoNotAffectMetrics() throws IOException {
        String code = """
            public class StringCommentSample {
                // if (a > 0) { while(true) {} } -- this is a comment!
                /* for (int i = 0; i < 10; i++) && || switch */
                public void process() {
                    String str = "if (x) { while(true) {} } && || catch";
                    System.out.println(str);
                }
            }
            """;
        List<MethodMetrics> metrics = JavaSourceAnalyzer.analyzeSource(code, "StringCommentSample.java");
        assert metrics.size() == 1 : "Expected 1 method";
        MethodMetrics m = metrics.get(0);
        assert m.cyclomaticComplexity() == 1 : "Comments and strings must not increase complexity (expected 1, got " + m.cyclomaticComplexity() + ")";
        assert m.maximumNestingDepth() == 0 : "Comments and strings must not increase nesting depth (expected 0, got " + m.maximumNestingDepth() + ")";
    }

    public void testConstructorMetrics() throws IOException {
        String code = """
            public class Order {
                private int id;
                public Order(int id) {
                    if (id <= 0) {
                        throw new IllegalArgumentException();
                    }
                    this.id = id;
                }
            }
            """;
        List<MethodMetrics> metrics = JavaSourceAnalyzer.analyzeSource(code, "Order.java");
        assert metrics.size() == 1 : "Expected constructor to be recognized";
        MethodMetrics m = metrics.get(0);
        assert "Order".equals(m.methodName()) : "Constructor name should match class name";
        assert m.cyclomaticComplexity() == 2 : "Base 1 + if(1) = 2";
        assert m.maximumNestingDepth() == 1 : "Expected nesting depth 1";
    }

    public void testSyntaxErrorThrowsException() {
        String brokenCode = """
            public class Broken {
                public void test() {
                    int a = 10  // missing semicolon
                }
            """; // missing closing brace
        boolean caught = false;
        try {
            JavaSourceAnalyzer.analyzeSource(brokenCode, "Broken.java");
        } catch (IOException e) {
            caught = true;
            assert e.getMessage().contains("Syntax error in Broken.java") : "Exception should mention syntax error";
            assert e.getMessage().contains("Line") : "Exception should mention line number";
        }
        assert caught : "Syntax error must throw IOException";
    }

    public void testLambdaAndNestedClassIndependence() throws IOException {
        String code = """
            import java.util.List;
            public class LambdaSample {
                public void test() {
                    List<String> list = List.of("a", "b");
                    list.forEach(s -> {
                        if (s.length() > 0) {
                            System.out.println(s);
                        }
                    });
                }
            }
            """;
        List<MethodMetrics> metrics = JavaSourceAnalyzer.analyzeSource(code, "LambdaSample.java");
        assert metrics.size() == 1 : "Expected 1 outer method";
        MethodMetrics m = metrics.get(0);
        // Lambda body should not increase the enclosing method's complexity
        assert m.cyclomaticComplexity() == 1 : "Enclosing method should not inherit lambda complexity";
    }
}
