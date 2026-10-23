import phase1.AllocationOutcome;
import phase1.Equipment;
import phase1.EquipmentRequest;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Stores equipment, requests, and allocation outcomes in a MySQL database.
 */
public class DatabaseStore implements AutoCloseable {
    private final Connection connection;

    public DatabaseStore(String jdbcUrl, String username, String password) throws SQLException {
        connection = DriverManager.getConnection(jdbcUrl, username, password);
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE IF NOT EXISTS equipment ("
                    + "equipment_order BIGINT NOT NULL AUTO_INCREMENT UNIQUE, "
                    + "equipment_id VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci PRIMARY KEY, "
                    + "equipment_name VARCHAR(255) NOT NULL, category VARCHAR(100) NOT NULL, "
                    + "quantity INTEGER NOT NULL CHECK (quantity >= 0), "
                    + "availability VARCHAR(50) NOT NULL, location VARCHAR(255) NOT NULL)");
            statement.execute("CREATE TABLE IF NOT EXISTS equipment_requests ("
                    + "queue_order BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, "
                    + "request_id VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL UNIQUE, "
                    + "equipment_id VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL, "
                    + "student_name VARCHAR(255) NOT NULL, "
                    + "requested_quantity INTEGER NOT NULL CHECK (requested_quantity > 0), "
                    + "status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING', 'ALLOCATED', 'REJECTED')))");
        }
    }

    /** Inserts the supplied sample inventory only when the equipment table is empty. */
    public synchronized void seedEquipmentIfEmpty(Equipment[] sampleEquipment) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT COUNT(*) FROM equipment")) {
            if (result.next() && result.getInt(1) == 0) {
                for (Equipment equipment : sampleEquipment) {
                    insertEquipment(equipment);
                }
            }
        }
    }

    /** Returns every inventory record in insertion order. */
    public synchronized List<Equipment> loadEquipment() throws SQLException {
        List<Equipment> equipment = new ArrayList<Equipment>();
        String sql = "SELECT equipment_id, equipment_name, category, quantity, availability, location "
            + "FROM equipment ORDER BY equipment_order";
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(sql)) {
            while (result.next()) {
                equipment.add(new Equipment(result.getString("equipment_id"),
                        result.getString("equipment_name"), result.getString("category"),
                        result.getInt("quantity"), result.getString("availability"),
                        result.getString("location")));
            }
        }
        return equipment;
    }

    /** Returns pending requests in their original queue order. */
    public synchronized List<EquipmentRequest> loadPendingRequests() throws SQLException {
        List<EquipmentRequest> requests = new ArrayList<EquipmentRequest>();
        String sql = "SELECT request_id, equipment_id, student_name, requested_quantity "
                + "FROM equipment_requests WHERE status = 'PENDING' ORDER BY queue_order";
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(sql)) {
            while (result.next()) {
                requests.add(new EquipmentRequest(result.getString("request_id"),
                        result.getString("equipment_id"), result.getString("student_name"),
                        result.getInt("requested_quantity")));
            }
        }
        return requests;
    }

    /** Inserts a new equipment record. */
    public synchronized void insertEquipment(Equipment equipment) throws SQLException {
        String sql = "INSERT INTO equipment "
                + "(equipment_id, equipment_name, category, quantity, availability, location) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, equipment.getEquipmentId());
            statement.setString(2, equipment.getEquipmentName());
            statement.setString(3, equipment.getCategory());
            statement.setInt(4, equipment.getQuantity());
            statement.setString(5, equipment.getAvailability());
            statement.setString(6, equipment.getLocation());
            statement.executeUpdate();
        }
    }

    /** Returns whether a request identifier is already stored. */
    public synchronized boolean hasRequestId(String requestId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT 1 FROM equipment_requests WHERE request_id = ?")) {
            statement.setString(1, requestId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        }
    }

    /** Adds a request to the persistent FIFO queue. */
    public synchronized void insertRequest(EquipmentRequest request) throws SQLException {
        String sql = "INSERT INTO equipment_requests "
                + "(request_id, equipment_id, student_name, requested_quantity, status) "
                + "VALUES (?, ?, ?, ?, 'PENDING')";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, request.getRequestId());
            statement.setString(2, request.getEquipmentId());
            statement.setString(3, request.getStudentName());
            statement.setInt(4, request.getRequestedQuantity());
            statement.executeUpdate();
        }
    }

    /** Deletes equipment and pending requests while retaining processed request history. */
    public synchronized boolean deleteEquipment(String equipmentId) throws SQLException {
        boolean originalAutoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try {
            try (PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM equipment_requests WHERE equipment_id = ? AND status = 'PENDING'")) {
                statement.setString(1, equipmentId);
                statement.executeUpdate();
            }
            boolean deleted;
            try (PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM equipment WHERE equipment_id = ?")) {
                statement.setString(1, equipmentId);
                deleted = statement.executeUpdate() > 0;
            }
            connection.commit();
            return deleted;
        } catch (SQLException exception) {
            connection.rollback();
            throw exception;
        } finally {
            connection.setAutoCommit(originalAutoCommit);
        }
    }

    /** Persists stock changes and marks processed requests in one transaction. */
    public synchronized void saveOutcomes(AllocationOutcome[] outcomes) throws SQLException {
        boolean originalAutoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try {
            for (AllocationOutcome outcome : outcomes) {
                EquipmentRequest request = outcome.getRequest();
                if (outcome.isAllocated() && outcome.getRemainingQuantity() >= 0) {
                    try (PreparedStatement statement = connection.prepareStatement(
                            "UPDATE equipment SET quantity = ? WHERE equipment_id = ?")) {
                        statement.setInt(1, outcome.getRemainingQuantity());
                        statement.setString(2, request.getEquipmentId());
                        if (statement.executeUpdate() != 1) {
                            throw new SQLException("Allocated equipment no longer exists.");
                        }
                    }
                }
                try (PreparedStatement statement = connection.prepareStatement(
                        "UPDATE equipment_requests SET status = ? "
                                + "WHERE request_id = ? AND status = 'PENDING'")) {
                    statement.setString(1, outcome.isAllocated() ? "ALLOCATED" : "REJECTED");
                    statement.setString(2, request.getRequestId());
                    if (statement.executeUpdate() != 1) {
                        throw new SQLException("Pending request no longer exists.");
                    }
                }
            }
            connection.commit();
        } catch (SQLException exception) {
            connection.rollback();
            throw exception;
        } finally {
            connection.setAutoCommit(originalAutoCommit);
        }
    }

    @Override
    public synchronized void close() throws SQLException {
        connection.close();
    }
}