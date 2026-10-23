package phase1;

/**
 * Describes whether a queued equipment request received its requested units.
 */
public class AllocationOutcome {
    private final EquipmentRequest request;
    private final boolean allocated;
    private final int remainingQuantity;

    public AllocationOutcome(EquipmentRequest request, boolean allocated, int remainingQuantity) {
        this.request = request;
        this.allocated = allocated;
        this.remainingQuantity = remainingQuantity;
    }

    /** Returns the request represented by this outcome. */
    public EquipmentRequest getRequest() {
        return request;
    }

    /** Returns whether the request received all requested units. */
    public boolean isAllocated() {
        return allocated;
    }

    /** Returns the stock remaining, or {@code -1} if its equipment was removed. */
    public int getRemainingQuantity() {
        return remainingQuantity;
    }
}
