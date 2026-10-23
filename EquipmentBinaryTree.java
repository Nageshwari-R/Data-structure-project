package phase2;

import phase1.Equipment;

/**
 * Stores equipment in a complete binary tree and provides its depth-first traversals.
 */
public class EquipmentBinaryTree {
    private static class TreeNode {
        Equipment equipment;
        TreeNode left;
        TreeNode right;

        TreeNode(Equipment equipment) {
            this.equipment = equipment;
        }
    }

    private TreeNode root;
    private int size;

    /**
     * Adds equipment at the next level-order position in the tree.
     *
     * @param equipment the equipment record to add
     */
    public void insertEquipment(Equipment equipment) {
        if (equipment == null) {
            return;
        }

        size++;
        if (root == null) {
            root = new TreeNode(equipment);
            return;
        }

        TreeNode parent = nodeAt(size / 2);
        if (parent == null) {
            root = new TreeNode(equipment);
            size = 1;
            return;
        }
        if (size % 2 == 0) {
            parent.left = new TreeNode(equipment);
        } else {
            parent.right = new TreeNode(equipment);
        }
    }

    /**
     * Removes the first equipment record with the given ID.
     *
     * @param equipmentId the ID to remove
     * @return {@code true} if a record was removed
     */
    public boolean deleteEquipment(String equipmentId) {
        if (equipmentId == null || size == 0) {
            return false;
        }

        for (int position = 1; position <= size; position++) {
            TreeNode matchingNode = nodeAt(position);
            if (matchingNode == null) {
                continue;
            }
            String currentEquipmentId = matchingNode.equipment.getEquipmentId();
            if (currentEquipmentId != null && currentEquipmentId.equalsIgnoreCase(equipmentId)) {
                if (position == size) {
                    removeLastNode();
                } else {
                    matchingNode.equipment = nodeAt(size).equipment;
                    removeLastNode();
                }
                return true;
            }
        }
        return false;
    }

    /** Prints the equipment in preorder. */
    public void displayPreOrder() {
        if (root == null) {
            System.out.println("Equipment tree is empty.");
            return;
        }
        displayPreOrder(root);
    }

    /** Prints the equipment in inorder. */
    public void displayInOrder() {
        if (root == null) {
            System.out.println("Equipment tree is empty.");
            return;
        }
        displayInOrder(root);
    }

    /** Prints the equipment in postorder. */
    public void displayPostOrder() {
        if (root == null) {
            System.out.println("Equipment tree is empty.");
            return;
        }
        displayPostOrder(root);
    }

    private void displayPreOrder(TreeNode node) {
        if (node == null) {
            return;
        }
        System.out.println(node.equipment);
        displayPreOrder(node.left);
        displayPreOrder(node.right);
    }

    private void displayInOrder(TreeNode node) {
        if (node == null) {
            return;
        }
        displayInOrder(node.left);
        System.out.println(node.equipment);
        displayInOrder(node.right);
    }

    private void displayPostOrder(TreeNode node) {
        if (node == null) {
            return;
        }
        displayPostOrder(node.left);
        displayPostOrder(node.right);
        System.out.println(node.equipment);
    }

    private TreeNode nodeAt(int position) {
        if (position < 1 || position > size) {
            return null;
        }

        TreeNode current = root;
        for (int bit = Integer.highestOneBit(position) >> 1; bit > 0; bit >>= 1) {
            current = (position & bit) == 0 ? current.left : current.right;
        }
        return current;
    }

    private void removeLastNode() {
        if (size == 1) {
            root = null;
            size = 0;
            return;
        }

        TreeNode parent = nodeAt(size / 2);
        if (size % 2 == 0) {
            parent.left = null;
        } else {
            parent.right = null;
        }
        size--;
    }
}