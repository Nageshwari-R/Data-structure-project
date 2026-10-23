package phase2;

/**
 * Represents undirected connections between laboratory locations using adjacency lists.
 */
public class EquipmentLocationGraph {
    private static class Vertex {
        String location;
        Neighbor neighbors;
        Vertex next;
        boolean visited;

        Vertex(String location) {
            this.location = location;
        }
    }

    private static class Neighbor {
        Vertex vertex;
        Neighbor next;

        Neighbor(Vertex vertex, Neighbor next) {
            this.vertex = vertex;
            this.next = next;
        }
    }

    private static class QueueNode {
        Vertex vertex;
        QueueNode next;

        QueueNode(Vertex vertex) {
            this.vertex = vertex;
        }
    }

    private Vertex vertices;

    /**
     * Adds an undirected connection between two distinct locations.
     *
     * @param firstLocation one endpoint of the connection
     * @param secondLocation the other endpoint of the connection
     * @return {@code true} if a new connection was added
     */
    public boolean addConnection(String firstLocation, String secondLocation) {
        if (firstLocation == null || secondLocation == null) {
            return false;
        }

        String firstName = firstLocation.trim();
        String secondName = secondLocation.trim();
        if (firstName.isEmpty() || secondName.isEmpty()
                || firstName.equalsIgnoreCase(secondName)) {
            return false;
        }

        Vertex first = findOrCreateVertex(firstName);
        Vertex second = findOrCreateVertex(secondName);
        if (hasNeighbor(first, second)) {
            return false;
        }

        first.neighbors = new Neighbor(second, first.neighbors);
        second.neighbors = new Neighbor(first, second.neighbors);
        return true;
    }

    /** Prints every location and its directly connected locations. */
    public void displayGraph() {
        if (vertices == null) {
            System.out.println("No location connections have been added.");
            return;
        }

        Vertex current = vertices;
        while (current != null) {
            System.out.print(current.location + " ->");
            Neighbor neighbor = current.neighbors;
            while (neighbor != null) {
                System.out.print(" " + neighbor.vertex.location);
                neighbor = neighbor.next;
            }
            System.out.println();
            current = current.next;
        }
    }

    /**
     * Traverses locations reachable from the starting location using breadth-first search.
     *
     * @param startingLocation the location from which to begin
     */
    public void breadthFirstTraversal(String startingLocation) {
        resetVisited();
        Vertex start = findVertex(startingLocation.trim());
        if (start == null) {
            System.out.println("Location not found. Add a connection containing that location first.");
            return;
        }

        QueueNode front = new QueueNode(start);
        QueueNode rear = front;
        start.visited = true;
        System.out.println("Breadth-first traversal:");

        while (front != null) {
            Vertex current = front.vertex;
            System.out.println(current.location);
            front = front.next;
            if (front == null) {
                rear = null;
            }

            Neighbor neighbor = current.neighbors;
            while (neighbor != null) {
                if (!neighbor.vertex.visited) {
                    neighbor.vertex.visited = true;
                    QueueNode newNode = new QueueNode(neighbor.vertex);
                    if (rear == null) {
                        front = newNode;
                        rear = newNode;
                    } else {
                        rear.next = newNode;
                        rear = newNode;
                    }
                }
                neighbor = neighbor.next;
            }
        }
    }

    private Vertex findOrCreateVertex(String location) {
        Vertex existing = findVertex(location);
        if (existing != null) {
            return existing;
        }

        Vertex created = new Vertex(location);
        created.next = vertices;
        vertices = created;
        return created;
    }

    private Vertex findVertex(String location) {
        if (location == null) {
            return null;
        }

        Vertex current = vertices;
        while (current != null) {
            if (current.location.equalsIgnoreCase(location)) {
                return current;
            }
            current = current.next;
        }
        return null;
    }

    private boolean hasNeighbor(Vertex vertex, Vertex expected) {
        Neighbor current = vertex.neighbors;
        while (current != null) {
            if (current.vertex == expected) {
                return true;
            }
            current = current.next;
        }
        return false;
    }

    private void resetVisited() {
        Vertex current = vertices;
        while (current != null) {
            current.visited = false;
            current = current.next;
        }
    }
}