package phase1;

public class EquipmentNode {
    Equipment data;
    EquipmentNode next;

    public EquipmentNode(Equipment data) {
        this.data = data;
        this.next = null;
    }
}