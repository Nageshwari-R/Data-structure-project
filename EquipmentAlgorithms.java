package phase3;

import phase1.Equipment;

/**
 * Provides the Phase 3 equipment search and sorting algorithms.
 */
public class EquipmentAlgorithms {
    private EquipmentAlgorithms() {
    }

    /**
     * Sorts equipment by ID in ascending, case-insensitive order.
     *
     * @param equipment the array to sort
     */
    public static void insertionSortById(Equipment[] equipment) {
        if (equipment == null || equipment.length < 2) {
            return;
        }

        for (int index = 1; index < equipment.length; index++) {
            Equipment current = equipment[index];
            if (current == null) {
                continue;
            }
            int previousIndex = index - 1;
            while (previousIndex >= 0 && equipment[previousIndex] != null
                    && equipment[previousIndex].getEquipmentId() != null
                    && current.getEquipmentId() != null
                    && equipment[previousIndex].getEquipmentId()
                            .compareToIgnoreCase(current.getEquipmentId()) > 0) {
                equipment[previousIndex + 1] = equipment[previousIndex];
                previousIndex--;
            }
            equipment[previousIndex + 1] = current;
        }
    }

    /**
     * Searches an array sorted by equipment ID using binary search.
     *
     * @param sortedEquipment the equipment array sorted by ID
     * @param equipmentId the ID to find
     * @return the matching equipment, or {@code null} if it is not present
     */
    public static Equipment binarySearchById(Equipment[] sortedEquipment, String equipmentId) {
        if (sortedEquipment == null || equipmentId == null || sortedEquipment.length == 0) {
            return null;
        }

        int low = 0;
        int high = sortedEquipment.length - 1;

        while (low <= high) {
            int middle = low + (high - low) / 2;
            Equipment current = sortedEquipment[middle];
            if (current == null || current.getEquipmentId() == null) {
                return null;
            }
            int comparison = current.getEquipmentId().compareToIgnoreCase(equipmentId);
            if (comparison == 0) {
                return current;
            }
            if (comparison < 0) {
                low = middle + 1;
            } else {
                high = middle - 1;
            }
        }
        return null;
    }
}