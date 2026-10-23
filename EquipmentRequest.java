package phase1;

public class EquipmentRequest {
    private String requestId;
    private String equipmentId;
    private String studentName;
    private int requestedQuantity;

    public EquipmentRequest(String requestId, String equipmentId, String studentName) {
        this(requestId, equipmentId, studentName, 1);
    }

    public EquipmentRequest(String requestId, String equipmentId, String studentName,
                            int requestedQuantity) {
        this.requestId = requestId;
        this.equipmentId = equipmentId;
        this.studentName = studentName;
        this.requestedQuantity = requestedQuantity;
    }

    /** Returns the request identifier. */
    public String getRequestId() {
        return requestId;
    }

    /** Returns the requested equipment identifier. */
    public String getEquipmentId() {
        return equipmentId;
    }

    /** Returns the name of the requesting student. */
    public String getStudentName() {
        return studentName;
    }

    /** Returns the number of units requested. */
    public int getRequestedQuantity() {
        return requestedQuantity;
    }

    @Override
    public String toString() {
        return "Request ID: " + requestId + ", Equipment ID: " + equipmentId
                + ", Student: " + studentName + ", Units requested: " + requestedQuantity;
    }
}
