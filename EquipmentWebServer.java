import phase1.AllocationOutcome;
import phase1.Equipment;
import phase1.EquipmentLinkedList;
import phase1.EquipmentRequest;
import phase1.RequestQueue;
import phase2.EquipmentBinaryTree;
import phase3.EquipmentAlgorithms;
import phase3.EquipmentBinarySearchTree;
import phase4.ExpressionEvaluator;
import phase4.GreedyRequestAllocator;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.ByteArrayOutputStream;
import java.io.Console;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Serves the laboratory inventory dashboard and its MySQL-backed workflow API.
 */
public class EquipmentWebServer {
    private static final long SESSION_DURATION_MILLIS = 8 * 60 * 60 * 1000L;
    private static final SecureRandom SESSION_RANDOM = new SecureRandom();
    private final EquipmentLinkedList equipmentList = new EquipmentLinkedList();
    private final RequestQueue requestQueue = new RequestQueue();
    private final EquipmentBinaryTree equipmentTree = new EquipmentBinaryTree();
    private final EquipmentBinarySearchTree equipmentSearchTree = new EquipmentBinarySearchTree();
    private final DatabaseStore databaseStore;
    private final String adminUsername;
    private final String adminPassword;
    private final Map<String, Long> sessions = new ConcurrentHashMap<String, Long>();

    public EquipmentWebServer(DatabaseStore databaseStore) throws SQLException {
        this(databaseStore, "admin", "admin");
    }

    public EquipmentWebServer(DatabaseStore databaseStore, String adminUsername, String adminPassword)
            throws SQLException {
        if (adminUsername == null || adminUsername.trim().isEmpty()
                || adminPassword == null || adminPassword.isEmpty()) {
            throw new IllegalArgumentException("Admin username and password must not be empty.");
        }
        this.databaseStore = databaseStore;
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
        databaseStore.seedEquipmentIfEmpty(new Equipment[] {
            new Equipment("EQ-101", "USB Oscilloscope", "Measurement", 4,
                    "Available", "Electronics Lab"),
            new Equipment("EQ-102", "Bench Microscope", "Optics", 3,
                    "Available", "Optics Lab"),
            new Equipment("EQ-103", "Digital Multimeter", "Measurement", 6,
                    "Available", "Electronics Lab"),
            new Equipment("EQ-104", "Hot Air Rework Station", "Soldering", 2,
                    "Available", "Workshop")
        });
        for (Equipment equipment : databaseStore.loadEquipment()) {
            addInitialEquipment(equipment);
        }
        for (EquipmentRequest request : databaseStore.loadPendingRequests()) {
            requestQueue.enqueueRequest(request);
        }
    }

    /** Starts the browser application on port 8080 or the supplied port. */
    public static void main(String[] args) throws Exception {
        int port = args.length == 0 ? 8080 : Integer.parseInt(args[0]);
        String url = environmentOrDefault("LAB_EQUIPMENT_DB_URL",
                "jdbc:mysql://localhost:3306/lab_equipment?createDatabaseIfNotExist=true&serverTimezone=UTC");
        String username = environmentOrDefault("LAB_EQUIPMENT_DB_USER", "root");
        DatabaseStore databaseStore = new DatabaseStore(url, username, databasePassword(username));

        EquipmentWebServer application = new EquipmentWebServer(databaseStore);
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", application::handle);
        server.start();
        System.out.println("Laboratory Equipment Management System: http://localhost:" + port);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            server.stop(0);
            try {
                databaseStore.close();
            } catch (SQLException exception) {
                System.err.println("Could not close the equipment database cleanly.");
            }
        }));
    }

    private static String environmentOrDefault(String name, String defaultValue) {
        String value = System.getenv(name);
        return value == null || value.trim().isEmpty() ? defaultValue : value;
    }

    private static String databasePassword(String username) {
        String password = System.getenv("LAB_EQUIPMENT_DB_PASSWORD");
        if (password != null && !password.isEmpty()) {
            return password;
        }

        Console console = System.console();
        if (console == null) {
            throw new IllegalStateException(
                    "Set LAB_EQUIPMENT_DB_PASSWORD, or start the server from an interactive terminal "
                    + "to enter your MySQL password.");
        }
        char[] passwordCharacters = console.readPassword("MySQL password for %s: ", username);
        if (passwordCharacters == null) {
            throw new IllegalStateException("Could not read the MySQL password from the terminal.");
        }
        try {
            return new String(passwordCharacters);
        } finally {
            java.util.Arrays.fill(passwordCharacters, '\0');
        }
    }

    private void addInitialEquipment(Equipment equipment) {
        equipmentList.insertEquipment(equipment);
        equipmentTree.insertEquipment(equipment);
        equipmentSearchTree.insertEquipment(equipment);
    }

    private synchronized void handle(HttpExchange exchange) throws IOException {
        try {
            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod();
            if ("GET".equals(method) && ("/".equals(path) || "/index.html".equals(path))) {
                if (isAuthenticated(exchange)) {
                    serveAsset(exchange, "index.html", "text/html; charset=utf-8");
                } else {
                    redirect(exchange, "/login");
                }
            } else if ("GET".equals(method) && "/login".equals(path)) {
                if (isAuthenticated(exchange)) {
                    redirect(exchange, "/");
                } else {
                    serveAsset(exchange, "login.html", "text/html; charset=utf-8");
                }
            } else if ("GET".equals(method) && "/styles.css".equals(path)) {
                serveAsset(exchange, "styles.css", "text/css; charset=utf-8");
            } else if ("GET".equals(method) && "/app.js".equals(path)) {
                serveAsset(exchange, "app.js", "text/javascript; charset=utf-8");
            } else if ("GET".equals(method) && "/login.css".equals(path)) {
                serveAsset(exchange, "login.css", "text/css; charset=utf-8");
            } else if ("GET".equals(method) && "/login.js".equals(path)) {
                serveAsset(exchange, "login.js", "text/javascript; charset=utf-8");
            } else if ("POST".equals(method) && "/api/login".equals(path)) {
                login(exchange);
            } else if ("POST".equals(method) && "/api/logout".equals(path)) {
                logout(exchange);
            } else if ("GET".equals(method) && "/api/session".equals(path)) {
                send(exchange, 200, "{\"authenticated\":" + isAuthenticated(exchange) + "}");
            } else if (path.startsWith("/api/") && !isAuthenticated(exchange)) {
                send(exchange, 401, "{\"error\":\"Please sign in to continue.\"}");
            } else if ("GET".equals(method) && "/api/state".equals(path)) {
                send(exchange, 200, stateJson());
            } else if ("POST".equals(method) && "/api/equipment".equals(path)) {
                addEquipment(exchange);
            } else if ("POST".equals(method) && "/api/requests".equals(path)) {
                addRequest(exchange);
            } else if ("POST".equals(method) && "/api/allocate".equals(path)) {
                allocateRequests(exchange);
            } else if ("POST".equals(method) && "/api/process".equals(path)) {
                processNextRequest(exchange);
            } else if ("POST".equals(method) && "/api/equipment/delete".equals(path)) {
                deleteEquipment(exchange);
            } else if ("POST".equals(method) && "/api/expression".equals(path)) {
                evaluateExpression(exchange);
            } else {
                send(exchange, 404, "{\"error\":\"Not found\"}");
            }
        } catch (IllegalArgumentException exception) {
            send(exchange, 400, errorJson(exception.getMessage()));
        } catch (Exception exception) {
            send(exchange, 500, errorJson("The request could not be completed."));
        } finally {
            exchange.close();
        }
    }

    private void login(HttpExchange exchange) throws IOException {
        Map<String, String> form = readForm(exchange);
        String username = form.get("username");
        String password = form.get("password");
        if (!credentialsMatch(username, password)) {
            send(exchange, 401, "{\"error\":\"Incorrect username or password.\"}");
            return;
        }

        byte[] tokenBytes = new byte[32];
        SESSION_RANDOM.nextBytes(tokenBytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
        sessions.put(token, Long.valueOf(System.currentTimeMillis() + SESSION_DURATION_MILLIS));
        exchange.getResponseHeaders().add("Set-Cookie",
                "LAB_EQUIPMENT_SESSION=" + token + "; Path=/; Max-Age=28800; HttpOnly; SameSite=Strict");
        send(exchange, 200, "{\"message\":\"Signed in.\"}");
    }

    private boolean credentialsMatch(String username, String password) {
        byte[] suppliedUsername = (username == null ? "" : username).getBytes(StandardCharsets.UTF_8);
        byte[] suppliedPassword = (password == null ? "" : password).getBytes(StandardCharsets.UTF_8);
        byte[] expectedUsername = adminUsername.getBytes(StandardCharsets.UTF_8);
        byte[] expectedPassword = adminPassword.getBytes(StandardCharsets.UTF_8);
        boolean usernameMatches = MessageDigest.isEqual(expectedUsername, suppliedUsername);
        boolean passwordMatches = MessageDigest.isEqual(expectedPassword, suppliedPassword);
        return usernameMatches & passwordMatches;
    }

    private boolean isAuthenticated(HttpExchange exchange) {
        String token = sessionToken(exchange);
        if (token == null) {
            return false;
        }
        Long expiration = sessions.get(token);
        if (expiration == null) {
            return false;
        }
        if (expiration.longValue() <= System.currentTimeMillis()) {
            sessions.remove(token, expiration);
            return false;
        }
        return true;
    }

    private static String sessionToken(HttpExchange exchange) {
        String cookieHeader = exchange.getRequestHeaders().getFirst("Cookie");
        if (cookieHeader == null) {
            return null;
        }
        for (String cookie : cookieHeader.split(";")) {
            String trimmedCookie = cookie.trim();
            String prefix = "LAB_EQUIPMENT_SESSION=";
            if (trimmedCookie.startsWith(prefix)) {
                return trimmedCookie.substring(prefix.length());
            }
        }
        return null;
    }

    private void logout(HttpExchange exchange) throws IOException {
        String token = sessionToken(exchange);
        if (token != null) {
            sessions.remove(token);
        }
        exchange.getResponseHeaders().add("Set-Cookie",
                "LAB_EQUIPMENT_SESSION=; Path=/; Max-Age=0; HttpOnly; SameSite=Strict");
        send(exchange, 200, "{\"message\":\"Signed out.\"}");
    }

    private void addEquipment(HttpExchange exchange) throws IOException, SQLException {
        Map<String, String> form = readForm(exchange);
        String id = required(form, "equipmentId");
        if (equipmentList.searchEquipment(id) != null) {
            throw new IllegalArgumentException("That equipment ID already exists.");
        }
        Equipment equipment = new Equipment(id, required(form, "equipmentName"),
                required(form, "category"), positiveInteger(form, "quantity"),
                required(form, "availability"), required(form, "location"));
        databaseStore.insertEquipment(equipment);
        equipmentList.insertEquipment(equipment);
        equipmentTree.insertEquipment(equipment);
        equipmentSearchTree.insertEquipment(equipment);
        send(exchange, 201, "{\"message\":\"Equipment added.\"}");
    }

    private void addRequest(HttpExchange exchange) throws IOException, SQLException {
        Map<String, String> form = readForm(exchange);
        String equipmentId = required(form, "equipmentId");
        Equipment equipment = findEquipmentUsingBinarySearch(equipmentId);
        if (equipment == null) {
            throw new IllegalArgumentException("Equipment ID was not found.");
        }
        EquipmentRequest request = new EquipmentRequest(required(form, "requestId"), equipmentId,
                required(form, "studentName"), positiveInteger(form, "requestedQuantity"));
        if (databaseStore.hasRequestId(request.getRequestId())) {
            throw new IllegalArgumentException("That request ID already exists.");
        }
        databaseStore.insertRequest(request);
        requestQueue.enqueueRequest(request);
        send(exchange, 201, "{\"message\":\"Request added to the queue.\"}");
    }

    private void allocateRequests(HttpExchange exchange) throws IOException, SQLException {
        AllocationOutcome[] outcomes = GreedyRequestAllocator.allocate(requestQueue, equipmentList);
        databaseStore.saveOutcomes(outcomes);
        StringBuilder json = new StringBuilder("{\"outcomes\":[");
        for (int index = 0; index < outcomes.length; index++) {
            if (index > 0) {
                json.append(',');
            }
            AllocationOutcome outcome = outcomes[index];
            json.append(outcomeJson(outcome));
        }
        json.append("]}");
        send(exchange, 200, json.toString());
    }

    private void processNextRequest(HttpExchange exchange) throws IOException, SQLException {
        EquipmentRequest request = requestQueue.dequeueRequest();
        if (request == null) {
            send(exchange, 200, "{\"outcome\":null}");
            return;
        }
        Equipment equipment = equipmentList.searchEquipment(request.getEquipmentId());
        boolean allocated = equipment != null && equipment.allocateQuantity(request.getRequestedQuantity());
        int remainingQuantity = equipment == null ? -1 : equipment.getQuantity();
        AllocationOutcome outcome = new AllocationOutcome(request, allocated, remainingQuantity);
        databaseStore.saveOutcomes(new AllocationOutcome[] { outcome });
        send(exchange, 200, "{\"outcome\":" + outcomeJson(outcome) + "}");
    }

    private void deleteEquipment(HttpExchange exchange) throws IOException, SQLException {
        Map<String, String> form = readForm(exchange);
        String id = required(form, "equipmentId");
        if (!databaseStore.deleteEquipment(id)) {
            throw new IllegalArgumentException("Equipment ID was not found.");
        }
        equipmentList.deleteEquipment(id);
        requestQueue.removeRequestsForEquipment(id);
        equipmentTree.deleteEquipment(id);
        equipmentSearchTree.deleteEquipment(id);
        send(exchange, 200, "{\"message\":\"Equipment removed.\"}");
    }

    private void evaluateExpression(HttpExchange exchange) throws IOException {
        Map<String, String> form = readForm(exchange);
        double result = ExpressionEvaluator.evaluate(required(form, "expression"));
        send(exchange, 200, "{\"result\":" + Double.toString(result) + "}");
    }

    private String stateJson() {
        StringBuilder json = new StringBuilder("{\"equipment\":[");
        Equipment[] equipment = equipmentList.toArray();
        for (int index = 0; index < equipment.length; index++) {
            if (index > 0) {
                json.append(',');
            }
            Equipment item = equipment[index];
            json.append("{\"equipmentId\":\"").append(escape(item.getEquipmentId()))
                    .append("\",\"equipmentName\":\"").append(escape(item.getEquipmentName()))
                    .append("\",\"category\":\"").append(escape(item.getCategory()))
                    .append("\",\"quantity\":").append(item.getQuantity())
                    .append(",\"availability\":\"").append(escape(item.getAvailability()))
                    .append("\",\"location\":\"").append(escape(item.getLocation()))
                    .append("\"}");
        }
        json.append("],\"requests\":[");
        EquipmentRequest[] requests = requestQueue.toArray();
        for (int index = 0; index < requests.length; index++) {
            if (index > 0) {
                json.append(',');
            }
            json.append(requestJson(requests[index]));
        }
        return json.append("]}").toString();
    }

    private static String requestJson(EquipmentRequest request) {
        return "{\"requestId\":\"" + escape(request.getRequestId())
                + "\",\"equipmentId\":\"" + escape(request.getEquipmentId())
                + "\",\"studentName\":\"" + escape(request.getStudentName())
                + "\",\"requestedQuantity\":" + request.getRequestedQuantity() + "}";
    }

    private static String outcomeJson(AllocationOutcome outcome) {
        return "{\"request\":" + requestJson(outcome.getRequest())
                + ",\"allocated\":" + outcome.isAllocated()
                + ",\"remainingQuantity\":" + outcome.getRemainingQuantity() + "}";
    }

    private Equipment findEquipmentUsingBinarySearch(String equipmentId) {
        Equipment[] sortedEquipment = equipmentList.toArray();
        EquipmentAlgorithms.insertionSortById(sortedEquipment);
        return EquipmentAlgorithms.binarySearchById(sortedEquipment, equipmentId);
    }

    private static Map<String, String> readForm(HttpExchange exchange) throws IOException {
        byte[] body = readRequestBody(exchange.getRequestBody());
        String encoded = new String(body, StandardCharsets.UTF_8);
        Map<String, String> values = new LinkedHashMap<String, String>();
        if (encoded.isEmpty()) {
            return values;
        }
        for (String pair : encoded.split("&")) {
            int separator = pair.indexOf('=');
            String key = separator < 0 ? pair : pair.substring(0, separator);
            String value = separator < 0 ? "" : pair.substring(separator + 1);
                values.put(URLDecoder.decode(key, "UTF-8"), URLDecoder.decode(value, "UTF-8"));
        }
        return values;
    }

    private static byte[] readRequestBody(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int count;
        while ((count = input.read(buffer)) != -1) {
            output.write(buffer, 0, count);
        }
        return output.toByteArray();
    }

    private static String required(Map<String, String> form, String key) {
        String value = form.get(key);
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Please provide " + key + ".");
        }
        return value.trim();
    }

    private static int positiveInteger(Map<String, String> form, String key) {
        try {
            int value = Integer.parseInt(required(form, key));
            if (value > 0) {
                return value;
            }
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(key + " must be a positive whole number.");
        }
        throw new IllegalArgumentException(key + " must be a positive whole number.");
    }

    private static String errorJson(String message) {
        return "{\"error\":\"" + escape(message) + "\"}";
    }

    private static String escape(String value) {
        StringBuilder escaped = new StringBuilder();
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (character == '"' || character == '\\') {
                escaped.append('\\').append(character);
            } else if (character == '\n') {
                escaped.append("\\n");
            } else if (character == '\r') {
                escaped.append("\\r");
            } else if (character == '\t') {
                escaped.append("\\t");
            } else if (character < 0x20) {
                escaped.append(String.format("\\u%04x", (int) character));
            } else {
                escaped.append(character);
            }
        }
        return escaped.toString();
    }

    private static void serveAsset(HttpExchange exchange, String fileName, String contentType)
            throws IOException {
        byte[] content = Files.readAllBytes(Paths.get("web", fileName));
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.getResponseHeaders().set("Cache-Control", "no-cache");
        exchange.sendResponseHeaders(200, content.length);
        exchange.getResponseBody().write(content);
    }

    private static void redirect(HttpExchange exchange, String location) throws IOException {
        exchange.getResponseHeaders().set("Location", location);
        exchange.sendResponseHeaders(303, -1);
    }

    private static void send(HttpExchange exchange, int status, String body) throws IOException {
        byte[] content = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.sendResponseHeaders(status, content.length);
        exchange.getResponseBody().write(content);
    }
}
