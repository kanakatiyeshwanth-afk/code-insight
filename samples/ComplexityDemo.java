package samples;

/** Deliberately nested example so the review flag is visible in a live demo. */
public class ComplexityDemo {
    public String evaluate(int a, int b, int c, int d, int e) {
        if (a > 0) {
            if (b > 0) {
                if (c > 0) {
                    if (d > 0) {
                        if (e > 0) {
                            return "all-positive";
                        }
                    }
                }
            }
        }
        return "check-input";
    }
}
