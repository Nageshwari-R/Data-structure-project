package phase1;

public class RequestQueue {
    private RequestNode front;
    private RequestNode rear;

    private static class RequestNode {
        EquipmentRequest data;
        RequestNode next;

        RequestNode(EquipmentRequest data) {
            this.data = data;
        }
    }

    public void enqueueRequest(EquipmentRequest request) {
        if (request == null) {
            return;
        }

        RequestNode newNode = new RequestNode(request);

        if (rear == null) {
            front = newNode;
            rear = newNode;
            return;
        }

        // New requests are always added at the rear of the queue.
        rear.next = newNode;
        rear = newNode;
    }

    /** Removes and returns the oldest request, or {@code null} when empty. */
    public EquipmentRequest dequeueRequest() {
        if (isEmpty()) {
            return null;
        }

        EquipmentRequest request = front.data;
        front = front.next;
        if (front == null) {
            rear = null;
        }
        return request;
    }

    public void displayRequests() {
        if (isEmpty()) {
            System.out.println("There are no pending requests.");
            return;
        }

        RequestNode current = front;
        while (current != null) {
            System.out.println(current.data);
            current = current.next;
        }
    }

    public boolean isEmpty() {
        return front == null;
    }

    /** Returns pending requests in queue order without removing them. */
    public EquipmentRequest[] toArray() {
        int count = 0;
        RequestNode current = front;
        while (current != null) {
            count++;
            current = current.next;
        }

        EquipmentRequest[] requests = new EquipmentRequest[count];
        current = front;
        for (int index = 0; index < count; index++) {
            requests[index] = current.data;
            current = current.next;
        }
        return requests;
    }

    /** Removes and returns all pending requests in queue order. */
    public EquipmentRequest[] drainRequests() {
        int count = 0;
        RequestNode current = front;
        while (current != null) {
            count++;
            current = current.next;
        }

        EquipmentRequest[] requests = new EquipmentRequest[count];
        current = front;
        for (int index = 0; index < count; index++) {
            requests[index] = current.data;
            current = current.next;
        }
        front = null;
        rear = null;
        return requests;
    }

    /** Removes pending requests for equipment that was deleted from inventory. */
    public void removeRequestsForEquipment(String equipmentId) {
        if (equipmentId == null) {
            return;
        }

        RequestNode previous = null;
        RequestNode current = front;
        while (current != null) {
            String currentEquipmentId = current.data.getEquipmentId();
            if (currentEquipmentId != null && currentEquipmentId.equalsIgnoreCase(equipmentId)) {
                if (previous == null) {
                    front = current.next;
                } else {
                    previous.next = current.next;
                }
                if (current == rear) {
                    rear = previous;
                }
            } else {
                previous = current;
            }
            current = current.next;
        }
        if (front == null) {
            rear = null;
        }
    }
}