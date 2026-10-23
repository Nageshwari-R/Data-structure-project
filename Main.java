import java.util.Scanner;

import phase1.AllocationOutcome;
import phase1.Equipment;
import phase1.EquipmentLinkedList;
import phase1.EquipmentRequest;
import phase1.RequestQueue;
import phase2.EquipmentBinaryTree;
import phase2.EquipmentLocationGraph;
import phase3.EquipmentAlgorithms;
import phase3.EquipmentBinarySearchTree;
import phase4.ExpressionEvaluator;
import phase4.GreedyRequestAllocator;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        EquipmentLinkedList equipmentList = new EquipmentLinkedList();
        RequestQueue requestQueue = new RequestQueue();
        EquipmentBinaryTree equipmentTree = new EquipmentBinaryTree();
        EquipmentBinarySearchTree equipmentSearchTree = new EquipmentBinarySearchTree();
        EquipmentLocationGraph locationGraph = new EquipmentLocationGraph();
        boolean running = true;

        System.out.println("===== LABORATORY EQUIPMENT MANAGEMENT SYSTEM =====");

        while (running) {
            displayMenu();
            int choice = readInteger(scanner, "Enter your choice: ");

            switch (choice) {
                case 1:
                    addEquipment(scanner, equipmentList, equipmentTree, equipmentSearchTree);
                    break;
                case 2:
                    equipmentList.displayEquipment();
                    break;
                case 3:
                    searchEquipment(scanner, equipmentList);
                    break;
                case 4:
                    deleteEquipment(scanner, equipmentList, equipmentTree, equipmentSearchTree);
                    break;
                case 5:
                    addRequest(scanner, requestQueue, equipmentList);
                    break;
                case 6:
                    processNextRequest(requestQueue, equipmentList);
                    break;
                case 7:
                    requestQueue.displayRequests();
                    break;
                case 8:
                    System.out.println("Preorder traversal:");
                    equipmentTree.displayPreOrder();
                    System.out.println("Inorder traversal:");
                    equipmentTree.displayInOrder();
                    System.out.println("Postorder traversal:");
                    equipmentTree.displayPostOrder();
                    break;
                case 9:
                    searchEquipmentTree(scanner, equipmentSearchTree);
                    break;
                case 10:
                    addLocationConnection(scanner, locationGraph);
                    break;
                case 11:
                    locationGraph.displayGraph();
                    break;
                case 12:
                    traverseLocations(scanner, locationGraph);
                    break;
                case 13:
                    searchEquipmentBinary(scanner, equipmentList);
                    break;
                case 14:
                    displayEquipmentSorted(equipmentList);
                    break;
                case 15:
                    displayAlgorithmEfficiency();
                    break;
                case 16:
                    running = false;
                    System.out.println("Thank you for using the system.");
                    break;
                case 17:
                    allocateRequestsGreedy(requestQueue, equipmentList);
                    break;
                case 18:
                    evaluateExpression(scanner);
                    break;
                default:
                    System.out.println("Invalid choice. Please try again.");
            }
        }

        scanner.close();
    }

    private static void displayMenu() {
        System.out.println();
        System.out.println("1. Add Equipment");
        System.out.println("2. Display Equipment");
        System.out.println("3. Search Equipment");
        System.out.println("4. Delete Equipment");
        System.out.println("5. Add Equipment Request to Queue");
        System.out.println("6. Process Next Request (FIFO)");
        System.out.println("7. Display Pending Requests");
        System.out.println("8. Display Equipment Tree Traversals");
        System.out.println("9. Search Equipment Using Binary Search Tree");
        System.out.println("10. Add Connection Between Lab Locations");
        System.out.println("11. Display Location Graph");
        System.out.println("12. Traverse Locations Using BFS");
        System.out.println("13. Search Equipment Using Binary Search");
        System.out.println("14. Display Equipment Sorted by ID (Insertion Sort)");
        System.out.println("15. Compare Algorithm Efficiency");
        System.out.println("16. Exit");
        System.out.println("17. Allocate Pending Requests (Greedy)");
        System.out.println("18. Evaluate Arithmetic Expression Using Stack");
    }

    private static void addEquipment(Scanner scanner, EquipmentLinkedList equipmentList,
                                     EquipmentBinaryTree equipmentTree,
                                     EquipmentBinarySearchTree equipmentSearchTree) {
        String id = readText(scanner, "Enter equipment ID: ");
        String name = readText(scanner, "Enter equipment name: ");
        String category = readText(scanner, "Enter category: ");
        int quantity = readInteger(scanner, "Enter quantity: ");
        String availability = readText(scanner, "Enter availability: ");
        String location = readText(scanner, "Enter location: ");

        Equipment equipment = new Equipment(id, name, category, quantity, availability, location);
        equipmentList.insertEquipment(equipment);
        equipmentTree.insertEquipment(equipment);
        equipmentSearchTree.insertEquipment(equipment);
        System.out.println("Equipment added successfully.");
    }

    private static void searchEquipment(Scanner scanner, EquipmentLinkedList equipmentList) {
        String id = readText(scanner, "Enter equipment ID to search: ");
        Equipment equipment = equipmentList.searchEquipment(id);

        if (equipment == null) {
            System.out.println("Equipment not found.");
        } else {
            System.out.println("Equipment found:");
            System.out.println(equipment);
        }
    }

    private static void deleteEquipment(Scanner scanner, EquipmentLinkedList equipmentList,
                                        EquipmentBinaryTree equipmentTree,
                                        EquipmentBinarySearchTree equipmentSearchTree) {
        String id = readText(scanner, "Enter equipment ID to delete: ");

        if (equipmentList.deleteEquipment(id)) {
            equipmentTree.deleteEquipment(id);
            equipmentSearchTree.deleteEquipment(id);
            System.out.println("Equipment deleted successfully.");
        } else {
            System.out.println("Equipment not found.");
        }
    }

    private static void addRequest(Scanner scanner, RequestQueue requestQueue,
                                   EquipmentLinkedList equipmentList) {
        String requestId = readText(scanner, "Enter request ID: ");
        String equipmentId = readText(scanner, "Enter equipment ID: ");
        String studentName = readText(scanner, "Enter student name: ");
        int requestedQuantity = readPositiveInteger(scanner, "Enter quantity requested: ");

        if (findEquipmentUsingBinarySearch(equipmentList, equipmentId) == null) {
            System.out.println("Request not added. Equipment ID was not found.");
            return;
        }

        requestQueue.enqueueRequest(new EquipmentRequest(requestId, equipmentId, studentName,
                requestedQuantity));
        System.out.println("Request added to the queue.");
    }

    private static void processNextRequest(RequestQueue requestQueue,
                                          EquipmentLinkedList equipmentList) {
        EquipmentRequest request = requestQueue.dequeueRequest();
        if (request == null) {
            System.out.println("There are no pending requests.");
            return;
        }

        Equipment equipment = equipmentList.searchEquipment(request.getEquipmentId());
        if (equipment != null && equipment.allocateQuantity(request.getRequestedQuantity())) {
            System.out.println("Processed request: " + request);
        } else {
            System.out.println("Request could not be fulfilled: " + request);
        }
    }

    private static void allocateRequestsGreedy(RequestQueue requestQueue,
                                               EquipmentLinkedList equipmentList) {
        AllocationOutcome[] outcomes = GreedyRequestAllocator.allocate(requestQueue, equipmentList);
        if (outcomes.length == 0) {
            System.out.println("There are no pending requests.");
            return;
        }

        for (AllocationOutcome outcome : outcomes) {
            String result = outcome.isAllocated() ? "Allocated" : "Not allocated";
            System.out.println(result + ": " + outcome.getRequest());
        }
    }

    private static void evaluateExpression(Scanner scanner) {
        String expression = readText(scanner, "Enter an arithmetic expression: ");
        try {
            System.out.println("Result: " + ExpressionEvaluator.evaluate(expression));
        } catch (IllegalArgumentException exception) {
            System.out.println("Could not evaluate expression: " + exception.getMessage());
        }
    }

    private static void searchEquipmentTree(Scanner scanner,
                                            EquipmentBinarySearchTree equipmentSearchTree) {
        String id = readText(scanner, "Enter equipment ID to search in the binary search tree: ");
        Equipment equipment = equipmentSearchTree.searchEquipment(id);
        if (equipment == null) {
            System.out.println("Equipment not found in the binary search tree.");
        } else {
            System.out.println("Equipment found:");
            System.out.println(equipment);
        }
    }

    private static void addLocationConnection(Scanner scanner,
                                              EquipmentLocationGraph locationGraph) {
        String firstLocation = readText(scanner, "Enter the first lab location: ");
        String secondLocation = readText(scanner, "Enter the connected lab location: ");
        if (locationGraph.addConnection(firstLocation, secondLocation)) {
            System.out.println("Location connection added.");
        } else {
            System.out.println("Connection not added. Use two different locations and avoid duplicates.");
        }
    }

    private static void traverseLocations(Scanner scanner,
                                          EquipmentLocationGraph locationGraph) {
        String startingLocation = readText(scanner, "Enter the starting lab location: ");
        locationGraph.breadthFirstTraversal(startingLocation);
    }

    private static void searchEquipmentBinary(Scanner scanner,
                                              EquipmentLinkedList equipmentList) {
        String id = readText(scanner, "Enter equipment ID to search using binary search: ");
        Equipment equipment = findEquipmentUsingBinarySearch(equipmentList, id);
        if (equipment == null) {
            System.out.println("Equipment not found.");
        } else {
            System.out.println("Equipment found:");
            System.out.println(equipment);
        }
    }

    private static Equipment findEquipmentUsingBinarySearch(EquipmentLinkedList equipmentList,
                                                             String equipmentId) {
        Equipment[] sortedEquipment = equipmentList.toArray();
        EquipmentAlgorithms.insertionSortById(sortedEquipment);
        return EquipmentAlgorithms.binarySearchById(sortedEquipment, equipmentId);
    }

    private static void displayEquipmentSorted(EquipmentLinkedList equipmentList) {
        Equipment[] sortedEquipment = equipmentList.toArray();
        EquipmentAlgorithms.insertionSortById(sortedEquipment);
        if (sortedEquipment.length == 0) {
            System.out.println("No equipment records available.");
            return;
        }

        for (Equipment equipment : sortedEquipment) {
            System.out.println(equipment);
        }
    }

    private static void displayAlgorithmEfficiency() {
        System.out.println("Search algorithms:");
        System.out.println("Linear search: O(n) time, O(1) extra space; works on unsorted data.");
        System.out.println("Binary search: O(log n) time, O(1) extra space; requires sorted data.");
        System.out.println("Sorting algorithms:");
        System.out.println("Insertion sort (implemented): best O(n), average/worst O(n^2), O(1) extra space.");
        System.out.println("Merge sort (not implemented): O(n log n) time, O(n) extra space.");
        System.out.println("Quick sort (not implemented): average O(n log n), worst O(n^2) time.");
        System.out.println("The binary-search menu sorts a copy first, so that complete operation includes insertion-sort time.");
    }

    private static String readText(Scanner scanner, String message) {
        System.out.print(message);
        return scanner.nextLine();
    }

    private static int readInteger(Scanner scanner, String message) {
        while (true) {
            try {
                System.out.print(message);
                int value = Integer.parseInt(scanner.nextLine());
                if (value < 0) {
                    System.out.println("Please enter zero or a positive number.");
                } else {
                    return value;
                }
            } catch (NumberFormatException exception) {
                System.out.println("Please enter a valid whole number.");
            }
        }
    }

    private static int readPositiveInteger(Scanner scanner, String message) {
        while (true) {
            int value = readInteger(scanner, message);
            if (value > 0) {
                return value;
            }
            System.out.println("Please enter a number greater than zero.");
        }
    }
}
