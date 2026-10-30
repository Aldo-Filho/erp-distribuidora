package com.pi.erp.product;

import static org.junit.jupiter.api.Assertions.*;

import jakarta.servlet.http.Cookie;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.HttpMethod;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    properties = {
        "server.address=127.0.0.1",
        "server.port=18082",
        "erp.auth.bootstrap-enabled=false",
        "erp.auth.login-per-ip-per-minute=10000",
        "spring.mail.host=127.0.0.1",
        "spring.mail.port=11026",
        "spring.mail.username=",
        "spring.mail.password=",
        "spring.jpa.show-sql=false",
        "spring.jpa.properties.hibernate.show_sql=false",
    }
)
@ContextConfiguration(initializers = ProductCatalogIntegrationTests.IsolatedDatabaseGuard.class)
@AutoConfigureMockMvc
class ProductCatalogIntegrationTests {

    public static class IsolatedDatabaseGuard
        implements ApplicationContextInitializer<ConfigurableApplicationContext>
    {

        @Override
        public void initialize(ConfigurableApplicationContext context) {
            String url = context.getEnvironment().getProperty("spring.datasource.url");
            if (!"jdbc:postgresql://127.0.0.1:55434/products_verify".equals(url)
                && !"jdbc:postgresql://127.0.0.1:55435/jamshub_test".equals(url)) {
                throw new IllegalStateException(
                    "Product tests require the dedicated products_verify database on port 55434."
                );
            }
        }
    }

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PasswordEncoder encoder;

    @Autowired
    private ObjectMapper json;

    @Autowired
    private MockMvc mvc;

    private Map<String, Cookie> cookies;
    private long brandId;
    private final String suffix = UUID.randomUUID().toString();

    @BeforeEach
    void authenticate() throws Exception {
        cookies = new HashMap<>();
        String email = UUID.randomUUID() + "@example.test";
        String password = UUID.randomUUID().toString();
        Long roleId = jdbc.queryForObject(
            "INSERT INTO roles(name,active) VALUES (?,true) RETURNING role_id",
            Long.class,
            email
        );
        jdbc.update(
            "INSERT INTO role_permissions(role_id,permission_id) SELECT ?,permission_id FROM permissions",
            roleId
        );
        jdbc.update(
            "INSERT INTO users(name,email,password_hash,role_id,active) VALUES (?,?,?,?,true)",
            "Product verification",
            email,
            encoder.encode(password),
            roleId
        );
        assertEquals(
            200,
            call("POST", "/auth/login", Map.of("email", email, "password", password)).statusCode()
        );
        brandId = created("/brand", Map.of("name", "Brand " + UUID.randomUUID()))
            .path("id")
            .asLong();
    }

    private record Reply(int statusCode, String body) {}

    private Reply call(String method, String path, Object body) throws Exception {
        var request = MockMvcRequestBuilders.request(HttpMethod.valueOf(method), path)
            .servletPath(path.split("\\?", 2)[0])
            .accept("application/json");
        if (!method.equals("GET")) {
            JsonNode csrf = json.readTree(call("GET", "/auth/csrf", null).body());
            request.header(csrf.path("headerName").asText(), csrf.path("token").asText());
            request.contentType("application/json");
        }
        if (!cookies.isEmpty()) {
            request.cookie(cookies.values().toArray(Cookie[]::new));
        }
        if (body != null) request.content(json.writeValueAsString(body));
        var response = mvc.perform(request).andReturn().getResponse();
        for (Cookie cookie : response.getCookies()) cookies.put(cookie.getName(), cookie);
        return new Reply(response.getStatus(), response.getContentAsString());
    }

    private JsonNode created(String path, Object body) throws Exception {
        var response = call("POST", path, body);
        assertEquals(201, response.statusCode(), path + ": " + response.body());
        return json.readTree(response.body());
    }

    private Map<String, Object> catalog(String unit, Object item) {
        return Map.of(
            "name",
            "Verification " + suffix,
            "brandId",
            brandId,
            "unit",
            unit,
            "type",
            "SIMPLE",
            "characteristics",
            List.of(),
            "items",
            List.of(item)
        );
    }

    private JsonNode item(long catalogId) throws Exception {
        var response = call("GET", "/productCatalog/" + catalogId + "/items/search", null);
        assertEquals(200, response.statusCode(), response.body());
        return json.readTree(response.body()).path("content").get(0);
    }

    @Test
    void simpleRegistrationAndList() throws Exception {
        JsonNode root = created(
            "/productCatalog",
            catalog("UN", Map.of("cost", 0, "price", 0, "color", "", "size", ""))
        );
        JsonNode sku = item(root.path("id").asLong());
        assertTrue(sku.path("sku").asText().startsWith("PRD-"));
        assertEquals(0, sku.path("price").asDouble());
        var search = call("GET", "/productCatalog/search?query=" + sku.path("sku").asText(), null);
        assertEquals(200, search.statusCode(), search.body());
        assertEquals(1, json.readTree(search.body()).path("totalElements").asInt());
        var patch = call(
            "PATCH",
            "/product/" + sku.path("id").asLong(),
            Map.of("name", "Edited " + suffix, "price", 2)
        );
        assertEquals(200, patch.statusCode(), patch.body());
        assertEquals("Edited " + suffix, json.readTree(patch.body()).path("name").asText());
    }

    @Test
    void variantRegistration() throws Exception {
        JsonNode root = created(
            "/productCatalog",
            Map.of(
                "name",
                "Variant " + suffix,
                "brandId",
                brandId,
                "unit",
                "UN",
                "type",
                "VARIANT",
                "characteristics",
                List.of("COLOR", "SIZE"),
                "items",
                List.of(
                    Map.of(
                        "cost",
                        0,
                        "price",
                        10,
                        "attributes",
                        Map.of("COLOR", "Azul", "SIZE", "M")
                    ),
                    Map.of(
                        "cost",
                        1,
                        "price",
                        11,
                        "attributes",
                        Map.of("COLOR", "Azul", "SIZE", "G")
                    )
                )
            )
        );
        var response = call(
            "GET",
            "/productCatalog/" + root.path("id").asLong() + "/items/search",
            null
        );
        assertEquals(200, response.statusCode(), response.body());
        JsonNode page = json.readTree(response.body());
        assertEquals(2, page.path("totalElements").asInt());
        assertNotEquals(
            page.path("content").get(0).path("sku").asText(),
            page.path("content").get(1).path("sku").asText()
        );
        var duplicate = call(
            "POST",
            "/productCatalog/" + root.path("id").asLong() + "/items",
            Map.of("cost", 1, "price", 2, "attributes", Map.of("COLOR", " azul ", "SIZE", "m"))
        );
        assertEquals(400, duplicate.statusCode(), duplicate.body());
        var patch = call(
            "PATCH",
            "/product/" + page.path("content").get(0).path("id").asLong(),
            Map.of("attributes", Map.of("COLOR", "Verde", "SIZE", "P"))
        );
        assertEquals(200, patch.statusCode(), patch.body());
    }

    @Test
    void initialStockAndPresentation() throws Exception {
        long warehouseId = created("/warehouse", Map.of("name", "Warehouse " + suffix))
            .path("id")
            .asLong();
        JsonNode root = created(
            "/productCatalog",
            catalog(
                "KG",
                Map.of(
                    "cost",
                    0.29,
                    "price",
                    0.29,
                    "stock",
                    Map.of(
                        "warehouseId",
                        warehouseId,
                        "quantity",
                        1.5,
                        "reservedQuantity",
                        0,
                        "minQuantity",
                        0.1,
                        "maxQuantity",
                        2
                    )
                )
            )
        );
        long skuId = item(root.path("id").asLong()).path("id").asLong();
        var stocks = call("GET", "/stockItem/search?productId=" + skuId, null);
        assertEquals(200, stocks.statusCode(), stocks.body());
        assertEquals(1.5, json.readTree(stocks.body()).path("content").get(0).path("quantity").asDouble());
        assertEquals(0, json.readTree(stocks.body()).path("content").get(0).path("reservedQuantity").asDouble());
        JsonNode presentation = created(
            "/productPresentation",
            Map.of("productId", skuId, "name", "Pack", "baseFactor", 1.5, "price", 0)
        );
        var patch = call(
            "PATCH",
            "/productPresentation/" + presentation.path("id").asLong(),
            Map.of("removePrice", true)
        );
        assertEquals(200, patch.statusCode(), patch.body());
        assertTrue(json.readTree(patch.body()).path("price").isNull());
    }

    @Test
    void initialReservationIsRejected() throws Exception {
        long warehouseId = created("/warehouse", Map.of("name", "Reservation " + suffix))
            .path("id")
            .asLong();
        var response = call(
            "POST",
            "/productCatalog",
            catalog(
                "UN",
                Map.of(
                    "cost",
                    1,
                    "price",
                    2,
                    "stock",
                    Map.of(
                        "warehouseId",
                        warehouseId,
                        "quantity",
                        3,
                        "reservedQuantity",
                        1,
                        "minQuantity",
                        0
                    )
                )
            )
        );
        assertEquals(400, response.statusCode(), response.body());
        assertTrue(response.body().contains("Initial reserved quantity must be zero."));
        assertEquals(
            0,
            jdbc.queryForObject(
                "SELECT count(*) FROM product_catalogs WHERE name=?",
                Integer.class,
                "Verification " + suffix
            )
        );
    }

    @Test
    void invalidQuantityRollsBackCatalog() throws Exception {
        long warehouseId = created("/warehouse", Map.of("name", "Invalid quantity " + suffix))
            .path("id")
            .asLong();
        for (String unit : List.of("UN", "KG")) {
            var response = call(
                "POST",
                "/productCatalog",
                catalog(
                    unit,
                    Map.of(
                        "cost",
                        1,
                        "price",
                        1,
                        "stock",
                        Map.of(
                            "warehouseId",
                            warehouseId,
                            "quantity",
                            1.25,
                            "reservedQuantity",
                            0,
                            "minQuantity",
                            0
                        )
                    )
                )
            );
            assertEquals(400, response.statusCode(), response.body());
        }
        assertEquals(
            0,
            jdbc.queryForObject(
                "SELECT count(*) FROM product_catalogs WHERE name=?",
                Integer.class,
                "Verification " + suffix
            )
        );
    }
}
