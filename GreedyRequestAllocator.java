package phase4;

import phase1.AllocationOutcome;
import phase1.Equipment;
import phase1.EquipmentLinkedList;
import phase1.EquipmentRequest;
import phase1.RequestQueue;

/**
 * Allocates available equipment to the smallest pending requests first.
 */
public class GreedyRequestAllocator {
    private GreedyRequestAllocator() {
    }

    /**
     * Processes every queued request in ascending requested-quantity order.
     * Equal-size requests retain their original queue order.
     *
     * @param requestQueue pending requests to process
     * @param equipmentList inventory whose quantities are updated
     * @return an outcome for each processed request
     */
    public static AllocationOutcome[] allocate(RequestQueue requestQueue,
                                                EquipmentLinkedList equipmentList) {
        EquipmentRequest[] requests = requestQueue.drainRequests();
        sortByRequestedQuantity(requests);
        AllocationOutcome[] outcomes = new AllocationOutcome[requests.length];

        for (int index = 0; index < requests.length; index++) {
            EquipmentRequest request = requests[index];
            Equipment equipment = equipmentList.searchEquipment(request.getEquipmentId());
            boolean allocated = equipment != null
                    && equipment.allocateQuantity(request.getRequestedQuantity());
            int remainingQuantity = equipment == null ? -1 : equipment.getQuantity();
            outcomes[index] = new AllocationOutcome(request, allocated, remainingQuantity);
        }
        return outcomes;
    }

    private static void sortByRequestedQuantity(EquipmentRequest[] requests) {
        for (int index = 1; index < requests.length; index++) {
            EquipmentRequest current = requests[index];
            int previousIndex = index - 1;
            while (previousIndex >= 0
                    && requests[previousIndex].getRequestedQuantity()
                            > current.getRequestedQuantity()) {
                requests[previousIndex + 1] = requests[previousIndex];
                previousIndex--;
            }
            requests[previousIndex + 1] = current;
        }
    }
}
