
package phase1;

public class EquipmentLinkedList {
    private EquipmentNode head;

    public void insertEquipment(Equipment equipment) {
        if (equipment == null) {
            return;
        }

        // A new node stores the equipment and points to the next node.
        EquipmentNode newNode = new EquipmentNode(equipment);

        if (head == null) {
            head = newNode;
            return;
        }

        EquipmentNode current = head;
        while (current.next != null) {
            current = current.next;
        }
        current.next = newNode;
    }

    public void displayEquipment() {
        if (head == null) {
            System.out.println("No equipment records available.");
            return;
        }

        EquipmentNode current = head;
        while (current != null) {
            System.out.println(current.data);
            current = current.next;
        }
    }

    public Equipment searchEquipment(String equipmentId) {
        if (equipmentId == null) {
            return null;
        }

        EquipmentNode current = head;

        // Visit each node until the required equipment ID is found.
        while (current != null) {
            String currentEquipmentId = current.data.getEquipmentId();
            if (currentEquipmentId != null && currentEquipmentId.equalsIgnoreCase(equipmentId)) {
                return current.data;
            }
            current = current.next;
        }
        return null;
    }

    /**
     * Copies the equipment records into an array in their current list order.
     *
     * @return an array containing the equipment records
     */
    public Equipment[] toArray() {
        int count = 0;
        EquipmentNode current = head;
        while (current != null) {
            count++;
            current = current.next;
        }

        Equipment[] equipment = new Equipment[count];
        current = head;
        for (int index = 0; index < count; index++) {
            equipment[index] = current.data;
            current = current.next;
        }
        return equipment;
    }

    public boolean deleteEquipment(String equipmentId) {
        if (head == null || equipmentId == null) {
            return false;
        }

        String headEquipmentId = head.data.getEquipmentId();
        if (headEquipmentId != null && headEquipmentId.equalsIgnoreCase(equipmentId)) {
            head = head.next;
            return true;
        }

        EquipmentNode current = head;
        while (current.next != null) {
            String nextEquipmentId = current.next.data.getEquipmentId();
            if (nextEquipmentId != null && nextEquipmentId.equalsIgnoreCase(equipmentId)) {
                // Skip the matching node so it is removed from the list.
                current.next = current.next.next;
                return true;
            }
            current = current.next;
        }
        return false;
    }
}