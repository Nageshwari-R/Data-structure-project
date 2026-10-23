package phase3;

import phase1.Equipment;

/**
 * Stores equipment in a binary search tree ordered by equipment ID.
 */
public class EquipmentBinarySearchTree {
    private static class TreeNode {
        Equipment equipment;
        TreeNode left;
        TreeNode right;

        TreeNode(Equipment equipment) {
            this.equipment = equipment;
        }
    }

    private TreeNode root;

    /**
     * Inserts equipment according to its ID.
     *
     * @param equipment the equipment record to insert
     */
    public void insertEquipment(Equipment equipment) {
        if (equipment == null || equipment.getEquipmentId() == null) {
            return;
        }
        root = insertEquipment(root, equipment);
    }

    /**
     * Finds equipment by its ID.
     *
     * @param equipmentId the ID to search for
     * @return the matching equipment, or {@code null} when it is not present
     */
    public Equipment searchEquipment(String equipmentId) {
        if (equipmentId == null) {
            return null;
        }

        TreeNode current = root;
        while (current != null) {
            String currentEquipmentId = current.equipment.getEquipmentId();
            int comparison = currentEquipmentId == null
                    ? 1
                    : equipmentId.compareToIgnoreCase(currentEquipmentId);
            if (comparison == 0) {
                return current.equipment;
            }
            current = comparison < 0 ? current.left : current.right;
        }
        return null;
    }

    /**
     * Removes one equipment record with the given ID.
     *
     * @param equipmentId the ID to remove
     * @return {@code true} if a record was removed
     */
    public boolean deleteEquipment(String equipmentId) {
        if (equipmentId == null || searchEquipment(equipmentId) == null) {
            return false;
        }
        root = deleteEquipment(root, equipmentId);
        return true;
    }

    private TreeNode insertEquipment(TreeNode node, Equipment equipment) {
        if (node == null) {
            return new TreeNode(equipment);
        }

        String currentEquipmentId = node.equipment.getEquipmentId();
        if (currentEquipmentId == null) {
            node.equipment = equipment;
            return node;
        }

        if (equipment.getEquipmentId().compareToIgnoreCase(currentEquipmentId) < 0) {
            node.left = insertEquipment(node.left, equipment);
        } else {
            node.right = insertEquipment(node.right, equipment);
        }
        return node;
    }

    private TreeNode deleteEquipment(TreeNode node, String equipmentId) {
        String currentEquipmentId = node.equipment.getEquipmentId();
        if (currentEquipmentId == null) {
            return node;
        }

        int comparison = equipmentId.compareToIgnoreCase(currentEquipmentId);
        if (comparison < 0) {
            node.left = deleteEquipment(node.left, equipmentId);
        } else if (comparison > 0) {
            node.right = deleteEquipment(node.right, equipmentId);
        } else if (node.left == null) {
            return node.right;
        } else if (node.right == null) {
            return node.left;
        } else {
            TreeNode successor = minimum(node.right);
            node.equipment = successor.equipment;
            node.right = removeMinimum(node.right);
        }
        return node;
    }

    private TreeNode minimum(TreeNode node) {
        while (node.left != null) {
            node = node.left;
        }
        return node;
    }

    private TreeNode removeMinimum(TreeNode node) {
        if (node.left == null) {
            return node.right;
        }
        node.left = removeMinimum(node.left);
        return node;
    }
}