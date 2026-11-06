package samples.invalid;

// Deliberate syntax errors to test robust error handling and fault tolerance
public class BrokenSyntaxSample {
    public void brokenMethod() {
        int x = 10   // missing semicolon
        if (x > 0) {
            System.out.println("Invalid syntax sample")
        // missing closing braces

