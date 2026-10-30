package com.pi.erp;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.sql.Connection;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.annotation.DirtiesContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/** Real HTTP and PostgreSQL checks. Never run against the habitual database. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "server.address=127.0.0.1",
    "erp.auth.bootstrap-enabled=false",
    "erp.auth.login-per-ip-per-minute=10000",
    "spring.mail.host=127.0.0.1",
    "spring.mail.port=11026"
})
@ContextConfiguration(initializers = CriticalIntegrityIntegrationTests.DatabaseGuard.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class CriticalIntegrityIntegrationTests {

    public static class DatabaseGuard
        implements ApplicationContextInitializer<ConfigurableApplicationContext> {

        @Override
        public void initialize(ConfigurableApplicationContext context) {
            if (!"jdbc:postgresql://127.0.0.1:55435/jamshub_test".equals(
                context.getEnvironment().getProperty("spring.datasource.url"))) {
                throw new IllegalStateException("Critical tests require isolated jamshub_test:55435.");
            }
        }
    }

    @Autowired private JdbcTemplate jdbc;
    @Autowired private DataSource dataSource;
    @Autowired private PasswordEncoder encoder;
    @Autowired private ObjectMapper json;
    @LocalServerPort private int port;

    private HttpClient client;
    private String csrfHeader;
    private String csrfToken;
    private long userId;
    private long brandId;
    private long warehouseId;
    private long customerId;
    private long supplierId;

    @BeforeEach
    void fixtures() throws Exception {
        assertEquals("jamshub_test", jdbc.queryForObject("SELECT current_database()", String.class));
        assertEquals(55435, jdbc.queryForObject("SELECT inet_server_port()", Integer.class));
        client = HttpClient.newBuilder().cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
            .connectTimeout(Duration.ofSeconds(5)).build();
        String email = UUID.randomUUID() + "@example.test";
        String password = UUID.randomUUID().toString();
        long role = jdbc.queryForObject("INSERT INTO roles(name,active) VALUES (?,true) RETURNING role_id",
            Long.class, email);
        jdbc.update("INSERT INTO role_permissions SELECT ?,permission_id FROM permissions", role);
        userId = jdbc.queryForObject(
            "INSERT INTO users(name,email,password_hash,role_id,active) VALUES (?,?,?,?,true) RETURNING user_id",
            Long.class, "Critical verification", email, encoder.encode(password), role);
        csrf();
        status(200, call("POST", "/auth/login", Map.of("email", email, "password", password)));
        csrf();
        brandId = create("/brand", Map.of("name", UUID.randomUUID().toString()));
        warehouseId = create("/warehouse", Map.of("name", UUID.randomUUID().toString()));
        customerId = create("/customer", Map.of("personType", "PF", "taxId", taxId(),
            "legalName", "Critical customer", "phone", "11999999999"));
        supplierId = create("/supplier", Map.of("legalName", "Critical supplier", "tradeName", "Critical supplier",
            "taxId", taxId(), "phone", "11999999999"));
    }

    private String taxId() {
        return Long.toString(java.util.concurrent.ThreadLocalRandom.current()
            .nextLong(10000000000000L, 99999999999999L));
    }

    private void csrf() throws Exception {
        JsonNode response = json.readTree(call("GET", "/auth/csrf", null).body());
        csrfHeader = response.path("headerName").asText();
        csrfToken = response.path("token").asText();
    }

    private HttpRequest request(String method, String path, Object body, Duration timeout) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
            .timeout(timeout).header("Accept", "application/json");
        if (!method.equals("GET")) {
            builder.header(csrfHeader, csrfToken).header("Content-Type", "application/json");
        }
        return builder.method(method, body == null ? HttpRequest.BodyPublishers.noBody()
            : HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body))).build();
    }

    private HttpResponse<String> call(String method, String path, Object body) throws Exception {
        return client.send(request(method, path, body, Duration.ofSeconds(30)), HttpResponse.BodyHandlers.ofString());
    }

    private JsonNode status(int expected, HttpResponse<String> response) throws Exception {
        assertEquals(expected, response.statusCode(), response.body());
        return json.readTree(response.body());
    }

    private long create(String path, Object body) throws Exception {
        return status(201, call("POST", path, body)).path("id").asLong();
    }

    private record Stock(long product, long id) {}

    private Stock stock(String unit, Object quantity) throws Exception {
        long product = create("/product", Map.of("name", "Critical " + UUID.randomUUID(), "brandId", brandId,
            "unit", unit, "cost", 1, "price", 2));
        long id = create("/stockItem", Map.of("productId", product, "warehouseId", warehouseId,
            "quantity", quantity, "reservedQuantity", 0, "minQuantity", 0));
        create("/productSupplier", Map.of("productId", product, "supplierId", supplierId,
            "option", Map.of("name", "Base", "baseFactor", 1, "basePresentation", true, "cost", 1)));
        return new Stock(product, id);
    }

    private Map<String, Object> movement(UUID uuid, Stock stock, String type, Object quantity) {
        return Map.of("requestId", uuid, "stockItemId", stock.id(), "type", type,
            "reason", "MANUAL_ADJUSTMENT", "quantity", quantity, "userId", userId);
    }

    private void balances(Stock stock, String physical, String reserved) {
        var row = jdbc.queryForMap("SELECT quantity,reserved_quantity FROM stock_items WHERE stock_item_id=?", stock.id());
        BigDecimal quantity = (BigDecimal) row.get("quantity");
        BigDecimal reservation = (BigDecimal) row.get("reserved_quantity");
        assertEquals(0, new BigDecimal(physical).compareTo(quantity), "physical balance");
        assertEquals(0, new BigDecimal(reserved).compareTo(reservation), "reserved balance");
        assertEquals(0, new BigDecimal(physical).subtract(new BigDecimal(reserved))
            .compareTo(quantity.subtract(reservation)), "available balance");
        BigDecimal active = jdbc.queryForObject(
            "SELECT coalesce(sum(quantity),0) FROM stock_reservations WHERE stock_item_id=? AND status='ACTIVE'",
            BigDecimal.class, stock.id());
        assertEquals(0, reservation.compareTo(active), "reservation ledger");
    }

    private int count(String table, String column, Object id) {
        return jdbc.queryForObject("SELECT count(*) FROM " + table + " WHERE " + column + "=?", Integer.class, id);
    }

    private void history(Stock stock) {
        var rows = jdbc.queryForList(
            "SELECT type,quantity,previous_balance,new_balance FROM stock_movements WHERE stock_item_id=? ORDER BY stock_movement_id",
            stock.id());
        BigDecimal previous = null;
        for (var row : rows) {
            BigDecimal before = (BigDecimal) row.get("previous_balance");
            BigDecimal after = (BigDecimal) row.get("new_balance");
            BigDecimal amount = (BigDecimal) row.get("quantity");
            if (previous != null) { assertEquals(0, previous.compareTo(before), "history continuity"); }
            BigDecimal expected = "OUTBOUND".equals(row.get("type")) ? before.subtract(amount) : before.add(amount);
            assertEquals(0, expected.compareTo(after), "movement arithmetic");
            previous = after;
        }
        if (previous != null) {
            assertEquals(0, previous.compareTo(jdbc.queryForObject(
                "SELECT quantity FROM stock_items WHERE stock_item_id=?", BigDecimal.class, stock.id())));
        }
    }

    private List<HttpResponse<String>> simultaneous(List<Callable<HttpResponse<String>>> calls) throws Exception {
        try (var pool = Executors.newVirtualThreadPerTaskExecutor()) {
            CountDownLatch ready = new CountDownLatch(calls.size());
            CountDownLatch start = new CountDownLatch(1);
            var futures = calls.stream().map(operation -> pool.submit(() -> {
                ready.countDown();
                assertTrue(start.await(10, TimeUnit.SECONDS));
                return operation.call();
            })).toList();
            assertTrue(ready.await(10, TimeUnit.SECONDS));
            start.countDown();
            List<HttpResponse<String>> replies = new ArrayList<>();
            for (var future : futures) { replies.add(future.get(40, TimeUnit.SECONDS)); }
            return replies;
        }
    }

    @RepeatedTest(5)
    void identicalUuidAndPayloadHaveExactlyOneEffect() throws Exception {
        Stock stock = stock("UN", 100);
        UUID uuid = UUID.randomUUID();
        var payload = movement(uuid, stock, "INBOUND", 3);
        List<Callable<HttpResponse<String>>> calls = new ArrayList<>();
        for (int i = 0; i < 8; i++) { calls.add(() -> call("POST", "/stockMovement", payload)); }
        var replies = simultaneous(calls);
        assertEquals(1, replies.stream().filter(r -> r.statusCode() == 201).count());
        assertEquals(7, replies.stream().filter(r -> r.statusCode() == 200).count(), replies.toString());
        long id = json.readTree(replies.get(0).body()).path("id").asLong();
        for (var reply : replies) { assertEquals(id, json.readTree(reply.body()).path("id").asLong()); }
        status(200, call("POST", "/stockMovement", payload));
        assertEquals(1, count("stock_movements", "request_id", uuid));
        balances(stock, "103", "0");
        history(stock);
    }

    @RepeatedTest(5)
    void divergentUuidPayloadIsRejectedIncludingDifferentStocks() throws Exception {
        Stock first = stock("UN", 100);
        Stock second = stock("UN", 100);
        UUID uuid = UUID.randomUUID();
        var replies = simultaneous(List.of(
            () -> call("POST", "/stockMovement", movement(uuid, first, "INBOUND", 3)),
            () -> call("POST", "/stockMovement", movement(uuid, second, "INBOUND", 7))));
        assertEquals(List.of(201, 400), replies.stream().map(HttpResponse::statusCode).sorted().toList());
        assertEquals(1, count("stock_movements", "request_id", uuid));
        long chosen = jdbc.queryForObject("SELECT stock_item_id FROM stock_movements WHERE request_id=?", Long.class, uuid);
        balances(first, chosen == first.id() ? "103" : "100", "0");
        balances(second, chosen == second.id() ? "107" : "100", "0");
        Stock winner = chosen == first.id() ? first : second;
        status(400, call("POST", "/stockMovement", movement(uuid, winner, "INBOUND", 9)));
    }

    @RepeatedTest(5)
    void distinctUuidsSerializeAndCannotOversell() throws Exception {
        Stock stock = stock("UN", 10);
        List<Callable<HttpResponse<String>>> calls = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            UUID uuid = UUID.randomUUID();
            calls.add(() -> call("POST", "/stockMovement", movement(uuid, stock, "OUTBOUND", 3)));
        }
        var replies = simultaneous(calls);
        assertEquals(3, replies.stream().filter(r -> r.statusCode() == 201).count(), replies.toString());
        assertEquals(5, replies.stream().filter(r -> r.statusCode() == 400).count());
        assertEquals(3, count("stock_movements", "stock_item_id", stock.id()));
        balances(stock, "1", "0");
        history(stock);
    }

    @Test
    void invalidManualQuantitiesDoNotChangeStock() throws Exception {
        Stock stock = stock("UN", 10);
        for (Object value : List.of(0, -1, 0.5, 1.23, 11)) {
            status(400, call("POST", "/stockMovement", movement(UUID.randomUUID(), stock, "OUTBOUND", value)));
        }
        balances(stock, "10", "0");
        assertEquals(0, count("stock_movements", "stock_item_id", stock.id()));
        Stock decimal = stock("KG", 1.5);
        status(201, call("POST", "/stockMovement", movement(UUID.randomUUID(), decimal, "OUTBOUND", 0.2)));
        balances(decimal, "1.3", "0");
        history(decimal);
    }

    @RepeatedTest(3)
    void retryAfterClientTimeoutStillAppliesExactlyOnce() throws Exception {
        Stock stock = stock("UN", 100);
        UUID uuid = UUID.randomUUID();
        long key = uuid.getMostSignificantBits() ^ uuid.getLeastSignificantBits();
        var payload = movement(uuid, stock, "INBOUND", 3);
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try (var lock = connection.prepareStatement("SELECT pg_advisory_xact_lock(?)")) {
                lock.setLong(1, key);
                lock.execute();
            }
            assertThrows(HttpTimeoutException.class, () -> client.send(
                request("POST", "/stockMovement", payload, Duration.ofMillis(700)),
                HttpResponse.BodyHandlers.ofString()));
            assertTrue(jdbc.queryForObject(
                "SELECT count(*)>0 FROM pg_stat_activity WHERE datname=current_database() AND wait_event='advisory'",
                Boolean.class), "Server must actually be blocked after the client times out");
            connection.commit();
        }
        // The original request and this retry may race after the simulated timeout.
        var reply = call("POST", "/stockMovement", payload);
        assertTrue(reply.statusCode() == 200 || reply.statusCode() == 201, reply.body());
        status(200, call("POST", "/stockMovement", payload));
        balances(stock, "103", "0");
        assertEquals(1, count("stock_movements", "request_id", uuid));
        history(stock);
    }

    private long purchase(Stock... stocks) throws Exception {
        long purchase = create("/purchase", Map.of("supplierId", supplierId, "userId", userId));
        for (Stock stock : stocks) {
            create("/purchaseItem", Map.of("purchaseId", purchase, "productId", stock.product(),
                "warehouseId", warehouseId, "quantity", 10, "unitCost", 1));
        }
        status(200, call("PATCH", "/purchase/" + purchase, Map.of("status", "SENT")));
        status(200, call("PATCH", "/purchase/" + purchase, Map.of("status", "CONFIRMED")));
        return purchase;
    }

    private List<Long> purchaseItems(long purchase) {
        return jdbc.queryForList("SELECT purchase_item_id FROM purchase_items WHERE purchase_id=? ORDER BY purchase_item_id",
            Long.class, purchase);
    }

    private Map<String, Object> receipt(UUID uuid, List<Long> items, Object quantity) {
        return Map.of("requestId", uuid, "operatorUserId", userId,
            "items", items.stream().map(id -> Map.of("purchaseItemId", id, "quantity", quantity)).toList());
    }

    private void purchaseStatus(long purchase, String status) {
        assertEquals(status, jdbc.queryForObject("SELECT status FROM purchases WHERE purchase_id=?", String.class, purchase));
    }

    @RepeatedTest(3)
    void partialFinalAndConcurrentReceiptRetriesAreConsistent() throws Exception {
        Stock stock = stock("UN", 0);
        long purchase = purchase(stock);
        var ids = purchaseItems(purchase);
        String path = "/purchase/" + purchase + "/receive";
        var partial = receipt(UUID.randomUUID(), ids, 4);
        var replies = simultaneous(List.of(() -> call("POST", path, partial), () -> call("POST", path, partial)));
        assertEquals(List.of(200, 201), replies.stream().map(HttpResponse::statusCode).sorted().toList());
        balances(stock, "4", "0");
        purchaseStatus(purchase, "PARTIALLY_RECEIVED");
        status(400, call("POST", path, receipt(UUID.fromString(partial.get("requestId").toString()), ids, 5)));
        for (Object quantity : List.of(0, -1, 0.5, 7)) {
            status(400, call("POST", path, receipt(UUID.randomUUID(), ids, quantity)));
        }
        var last = receipt(UUID.randomUUID(), ids, 6);
        status(201, call("POST", path, last));
        status(200, call("POST", path, last));
        status(200, call("POST", path, partial));
        status(400, call("POST", path, receipt(UUID.randomUUID(), ids, 1)));
        purchaseStatus(purchase, "RECEIVED");
        balances(stock, "10", "0");
        assertEquals(2, count("purchase_receipts", "purchase_id", purchase));
        assertEquals(2, count("stock_movements", "purchase_id", purchase));
        assertEquals(0, new BigDecimal("10").compareTo(jdbc.queryForObject(
            "SELECT received_quantity FROM purchase_items WHERE purchase_item_id=?", BigDecimal.class, ids.get(0))));
        history(stock);
    }

    private long order(Object quantity, Stock... stocks) throws Exception {
        long order = create("/order", Map.of("customerId", customerId, "userId", userId, "paymentMethod", "CASH"));
        for (Stock stock : stocks) {
            create("/orderItem", Map.of("orderId", order, "productId", stock.product(),
                "warehouseId", warehouseId, "quantity", quantity, "unitPrice", 2));
        }
        return order;
    }

    private HttpResponse<String> orderStatus(long order, String state) throws Exception {
        return call("PATCH", "/order/" + order, Map.of("status", state));
    }

    private void orderState(long order, String state) {
        assertEquals(state, jdbc.queryForObject("SELECT status FROM orders WHERE order_id=?", String.class, order));
    }

    @RepeatedTest(3)
    void reserveDispatchRetryAndCancellationPreserveBalances() throws Exception {
        Stock stock = stock("UN", 10);
        long order = order(6, stock);
        status(200, orderStatus(order, "CONFIRMED"));
        status(200, orderStatus(order, "CONFIRMED"));
        balances(stock, "10", "6");
        long excessive = order(5, stock);
        status(400, orderStatus(excessive, "CONFIRMED"));
        orderState(excessive, "PENDING");
        assertEquals(0, count("stock_reservations", "order_item_id", jdbc.queryForObject(
            "SELECT order_item_id FROM order_items WHERE order_id=?", Long.class, excessive)));
        status(400, call("POST", "/stockMovement", movement(UUID.randomUUID(), stock, "OUTBOUND", 5)));
        var dispatch = Map.of("operatorUserId", userId, "notes", "critical dispatch");
        String path = "/order/" + order + "/dispatch";
        var replies = simultaneous(List.of(() -> call("POST", path, dispatch), () -> call("POST", path, dispatch)));
        for (var reply : replies) { status(200, reply); }
        status(200, call("POST", path, dispatch));
        orderState(order, "INVOICED");
        balances(stock, "4", "0");
        assertEquals(1, count("stock_movements", "order_id", order));
        status(400, orderStatus(order, "CANCELED"));
        long cancel = order(3, stock);
        status(200, orderStatus(cancel, "CONFIRMED"));
        balances(stock, "4", "3");
        status(200, orderStatus(cancel, "CANCELED"));
        status(200, orderStatus(cancel, "CANCELED"));
        status(400, call("POST", "/order/" + cancel + "/dispatch", dispatch));
        balances(stock, "4", "0");
        assertEquals(0, count("stock_movements", "order_id", cancel));
        history(stock);
    }

    private void injectFailure(Stock stock) {
        // Test-only trigger; raises on the selected later movement, after earlier writes.
        jdbc.execute("CREATE OR REPLACE FUNCTION critical_fail_movement() RETURNS trigger LANGUAGE plpgsql AS $$ "
            + "BEGIN IF NEW.stock_item_id=" + stock.id() + " THEN RAISE EXCEPTION 'Injected critical failure' "
            + "USING ERRCODE='23514'; END IF; RETURN NEW; END $$");
        jdbc.execute("CREATE TRIGGER critical_fail_movement BEFORE INSERT ON stock_movements "
            + "FOR EACH ROW EXECUTE FUNCTION critical_fail_movement()");
    }

    private void removeFailure() {
        jdbc.execute("DROP TRIGGER IF EXISTS critical_fail_movement ON stock_movements");
        jdbc.execute("DROP FUNCTION IF EXISTS critical_fail_movement()");
    }

    @RepeatedTest(3)
    void concurrentDistinctReceiptsCannotExceedPurchase() throws Exception {
        Stock stock = stock("UN", 0);
        long purchase = purchase(stock);
        var ids = purchaseItems(purchase);
        String path = "/purchase/" + purchase + "/receive";
        var replies = simultaneous(List.of(
            () -> call("POST", path, receipt(UUID.randomUUID(), ids, 7)),
            () -> call("POST", path, receipt(UUID.randomUUID(), ids, 7))));
        assertEquals(List.of(201, 400), replies.stream().map(HttpResponse::statusCode).sorted().toList());
        balances(stock, "7", "0");
        purchaseStatus(purchase, "PARTIALLY_RECEIVED");
        assertEquals(1, count("purchase_receipts", "purchase_id", purchase));
        assertEquals(1, count("stock_movements", "purchase_id", purchase));
        history(stock);
    }

    @RepeatedTest(3)
    void competingOrdersCannotOverReserve() throws Exception {
        Stock stock = stock("UN", 10);
        long first = order(7, stock);
        long second = order(7, stock);
        var replies = simultaneous(List.of(
            () -> orderStatus(first, "CONFIRMED"), () -> orderStatus(second, "CONFIRMED")));
        assertEquals(List.of(200, 400), replies.stream().map(HttpResponse::statusCode).sorted().toList());
        balances(stock, "10", "7");
        assertEquals(1, count("stock_reservations", "stock_item_id", stock.id()));
        assertEquals(1, jdbc.queryForObject(
            "SELECT count(*) FROM orders WHERE order_id IN (?,?) AND status='CONFIRMED'", Integer.class, first, second));
    }

    @RepeatedTest(3)
    void receiptDispatchAndManualMovementShareTheSameStockSafely() throws Exception {
        Stock stock = stock("UN", 20);
        long purchase = purchase(stock);
        var receipt = receipt(UUID.randomUUID(), purchaseItems(purchase), 10);
        long order = order(5, stock);
        status(200, orderStatus(order, "CONFIRMED"));
        var replies = simultaneous(List.of(
            () -> call("POST", "/purchase/" + purchase + "/receive", receipt),
            () -> call("POST", "/order/" + order + "/dispatch", Map.of("operatorUserId", userId)),
            () -> call("POST", "/stockMovement", movement(UUID.randomUUID(), stock, "INBOUND", 2))));
        status(201, replies.get(0));
        status(200, replies.get(1));
        status(201, replies.get(2));
        balances(stock, "27", "0");
        purchaseStatus(purchase, "RECEIVED");
        orderState(order, "INVOICED");
        assertEquals(3, count("stock_movements", "stock_item_id", stock.id()));
        history(stock);
    }

    @Test
    void invalidCommercialQuantitiesAndPurchaseCancellationHaveNoStockEffect() throws Exception {
        Stock stock = stock("UN", 10);
        long purchase = create("/purchase", Map.of("supplierId", supplierId, "userId", userId));
        long order = create("/order", Map.of("customerId", customerId));
        for (Object quantity : List.of(0, -1, 0.5, 1.23)) {
            status(400, call("POST", "/purchaseItem", Map.of("purchaseId", purchase, "productId", stock.product(),
                "warehouseId", warehouseId, "quantity", quantity, "unitCost", 1)));
            status(400, call("POST", "/orderItem", Map.of("orderId", order, "productId", stock.product(),
                "warehouseId", warehouseId, "quantity", quantity, "unitPrice", 2)));
        }
        assertEquals(0, count("purchase_items", "purchase_id", purchase));
        assertEquals(0, count("order_items", "order_id", order));
        long canceled = purchase(stock);
        status(200, call("PATCH", "/purchase/" + canceled, Map.of("status", "CANCELED")));
        status(200, call("PATCH", "/purchase/" + canceled, Map.of("status", "CANCELED")));
        status(400, call("POST", "/purchase/" + canceled + "/receive",
            receipt(UUID.randomUUID(), purchaseItems(canceled), 1)));
        purchaseStatus(canceled, "CANCELED");
        balances(stock, "10", "0");
        assertEquals(0, count("stock_movements", "stock_item_id", stock.id()));
    }

    @Test
    void reservationFailureDoesNotPartiallyReserveOtherItems() throws Exception {
        Stock first = stock("UN", 20);
        Stock second = stock("UN", 1);
        long order = order(5, first, second);
        status(400, orderStatus(order, "CONFIRMED"));
        orderState(order, "PENDING");
        balances(first, "20", "0");
        balances(second, "1", "0");
        assertEquals(0, count("stock_reservations", "stock_item_id", first.id()));
        assertEquals(0, count("stock_reservations", "stock_item_id", second.id()));
    }

    @RepeatedTest(3)
    void intermediateFailuresRollbackManualReceiptAndDispatch() throws Exception {
        Stock first = stock("UN", 20);
        Stock second = stock("UN", 20);
        var payload = movement(UUID.randomUUID(), first, "INBOUND", 3);
        injectFailure(first);
        try { status(409, call("POST", "/stockMovement", payload)); }
        finally { removeFailure(); }
        balances(first, "20", "0");
        assertEquals(0, count("stock_movements", "stock_item_id", first.id()));
        status(201, call("POST", "/stockMovement", payload));
        long purchase = purchase(first, second);
        var receipt = receipt(UUID.randomUUID(), purchaseItems(purchase), 10);
        String receive = "/purchase/" + purchase + "/receive";
        injectFailure(second);
        try { status(409, call("POST", receive, receipt)); }
        finally { removeFailure(); }
        balances(first, "23", "0");
        balances(second, "20", "0");
        purchaseStatus(purchase, "CONFIRMED");
        assertEquals(0, count("purchase_receipts", "purchase_id", purchase));
        assertEquals(0, count("stock_movements", "purchase_id", purchase));
        assertEquals(0, jdbc.queryForObject(
            "SELECT count(*) FROM purchase_items WHERE purchase_id=? AND received_quantity<>0", Integer.class, purchase));
        status(201, call("POST", receive, receipt));
        balances(first, "33", "0");
        balances(second, "30", "0");
        long order = order(5, first, second);
        status(200, orderStatus(order, "CONFIRMED"));
        injectFailure(second);
        String dispatch = "/order/" + order + "/dispatch";
        try { status(409, call("POST", dispatch, Map.of("operatorUserId", userId))); }
        finally { removeFailure(); }
        orderState(order, "CONFIRMED");
        balances(first, "33", "5");
        balances(second, "30", "5");
        assertEquals(0, count("stock_movements", "order_id", order));
        status(200, call("POST", dispatch, Map.of("operatorUserId", userId)));
        balances(first, "28", "0");
        balances(second, "25", "0");
        orderState(order, "INVOICED");
        assertEquals(2, count("stock_movements", "order_id", order));
        history(first);
        history(second);
    }
}
