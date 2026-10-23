package phase1;

public class Equipment {
    private String equipmentId;
    private String equipmentName;
    private String category;
    private int quantity;
    private String availability;
    private String location;

    public Equipment(String equipmentId, String equipmentName, String category,
                     int quantity, String availability, String location) {
        this.equipmentId = equipmentId;
        this.equipmentName = equipmentName;
        this.category = category;
        this.quantity = quantity;
        this.availability = availability;
        this.location = location;
    }

    public String getEquipmentId() {
        return equipmentId;
    }

    /** Returns the equipment name. */
    public String getEquipmentName() {
        return equipmentName;
    }

    /** Returns the equipment category. */
    public String getCategory() {
        return category;
    }

    /** Returns the number of units currently in stock. */
    public int getQuantity() {
        return quantity;
    }

    /** Returns the availability description. */
    public String getAvailability() {
        return availability;
    }

    /** Returns the equipment location. */
    public String getLocation() {
        return location;
    }

    /**
     * Removes units from stock when the complete request can be fulfilled.
     *
     * @param requestedQuantity the number of units to allocate
     * @return {@code true} if the units were allocated
     */
    public boolean allocateQuantity(int requestedQuantity) {
        if (requestedQuantity <= 0 || requestedQuantity > quantity) {
            return false;
        }
        quantity -= requestedQuantity;
        return true;
    }

    @Override
    public String toString() {
        return "ID: " + equipmentId
                + ", Name: " + equipmentName
                + ", Category: " + category
                + ", Quantity: " + quantity
                + ", Availability: " + availability
                + ", Location: " + location;
    }
}