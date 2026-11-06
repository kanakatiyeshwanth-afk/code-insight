package samples;

public class OrderService {
    public int shippingCost(boolean express, int weight, String region) {
        if (express) {
            if (weight > 10) return 25;
            return 15;
        } else if (weight > 20) {
            return 12;
        }

        switch (region) {
            case "LOCAL": return 5;
            case "NATIONAL": return 8;
            default: return 15;
        }
    }

    public boolean eligibleForDiscount(int itemCount, boolean member) {
        return itemCount > 5 && member;
    }
}
