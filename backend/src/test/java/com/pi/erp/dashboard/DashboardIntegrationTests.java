package com.pi.erp.dashboard;

import static org.junit.jupiter.api.Assertions.*;
import com.pi.erp.auth.ErpPrincipal;
import java.net.*;
import java.net.http.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.annotation.DirtiesContext;
import tools.jackson.databind.*;

@ActiveProfiles("test")
@DirtiesContext(classMode=DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={
    "server.address=127.0.0.1", "erp.auth.bootstrap-enabled=false", "erp.auth.login-per-ip-per-minute=10000",
    "spring.jpa.show-sql=false", "spring.jpa.properties.hibernate.show_sql=false"
})
class DashboardIntegrationTests {
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder encoder;
    @Autowired ObjectMapper json;
    @Autowired DashboardMetricRepository metrics;
    @Autowired DashboardCatalog catalog;
    @Autowired DashboardPeriodResolver periods;
    @LocalServerPort int port;
    HttpClient client;
    String header,token;
    long userId;
    static final List<String> KEYS=List.of("orders.read","finance.read","purchases.read","stock.read");
    @BeforeEach void login() throws Exception {
        assertEquals("jamshub_test",jdbc.queryForObject("SELECT current_database()",String.class));
        assertEquals(55435,jdbc.queryForObject("SELECT inet_server_port()",Integer.class));
        String email=UUID.randomUUID()+"@example.test",password=UUID.randomUUID().toString();
        long role=jdbc.queryForObject("INSERT INTO roles(name) VALUES (?) RETURNING role_id",Long.class,email);
        for(String key:KEYS) {
            jdbc.update("INSERT INTO permissions(module,action,permission_key) VALUES (?,'read',?) ON CONFLICT(permission_key) DO NOTHING",key.split("\\.")[0],key);
            jdbc.update("INSERT INTO role_permissions SELECT ?,permission_id FROM permissions WHERE permission_key=?",role,key);
        }
        userId=jdbc.queryForObject("INSERT INTO users(name,email,password_hash,role_id) VALUES (?,?,?,?) RETURNING user_id",Long.class,"Dashboard verification",email,encoder.encode(password),role);
        client=HttpClient.newBuilder().cookieHandler(new CookieManager(null,CookiePolicy.ACCEPT_ALL)).connectTimeout(Duration.ofSeconds(5)).build();
        csrf();
        var response=call("POST","/auth/sessions",Map.of("email",email,"password",password));
        assertEquals(200,response.statusCode(),response.body());
        csrf();
    }
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }
    void csrf() throws Exception {
        var result=call("GET","/auth/csrf",null);assertEquals(200,result.statusCode(),result.body());
        var data=json.readTree(result.body());header=data.get("headerName").asText();token=data.get("token").asText();
    }
    HttpResponse<String> call(String method,String path,Object body) throws Exception {
        var builder=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+path)).timeout(Duration.ofSeconds(30)).header("Accept","application/json");
        if(!method.equals("GET")) builder.header(header,token).header("Content-Type","application/json");
        builder.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)));
        return client.send(builder.build(),HttpResponse.BodyHandlers.ofString());
    }
    String query() { return "?from="+periods.today().withDayOfMonth(1)+"&to="+periods.today()+"&compare=true"; }
    @Test void preferenceAndDashboardAreReadableOverHttp() throws Exception {
        var preference=call("GET","/preferences/current/dashboard",null);
        assertEquals(200,preference.statusCode(),preference.body());
        var response=call("GET","/dashboard"+query(),null);
        assertEquals(200,response.statusCode(),response.body());
        var widgets=json.readTree(response.body()).get("widgets");assertEquals(21,widgets.size());
        for(var widget:widgets) assertNotEquals("UNAVAILABLE",widget.get("state").asText(),widget.get("id").asText());
        for(var widget:widgets) {
            var detail=call("GET","/dashboard/widgets/"+widget.get("id").asText()+"/details"+query(),null);
            assertEquals(200,detail.statusCode(),widget.get("id").asText()+": "+detail.body());
        }
    }
    @Test void everyAggregateAndComparisonQueryExecutesOnPostgres() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(new ErpPrincipal(userId,0),null,KEYS.stream().map(SimpleGrantedAuthority::new).toList()));
        LocalDate today=periods.today();
        for(String metric:List.of("QUANTITY","REVENUE","AVERAGE_PRICE")) for(String unit:List.of("UN","KG")) {
            var filter=new DashboardFilter(today.withDayOfMonth(1),today,true,null,unit,metric,null);
            for(var d:catalog.allowed()) assertDoesNotThrow(()->{
                var q=metrics.query(d.id(),filter,filter.from(),filter.to(),today);
                metrics.total(q);metrics.count(q);metrics.rows(q,0,10);metrics.distribution(q);
                if(d.kind().equals("LINE")) for(String granularity:List.of("DAY","WEEK","MONTH")) metrics.series(q,granularity,filter.from(),filter.to());
                metrics.rows(metrics.detailQuery(d.id(),filter,filter.from(),filter.to(),today),0,10);
            },d.id()+"/"+metric+"/"+unit);
        }
    }
    @Test void preferenceRoundTripAndStaleTabConflicts() throws Exception {
        var initial=call("GET","/preferences/current/dashboard",null);
        var draft=(tools.jackson.databind.node.ObjectNode)json.readTree(initial.body());
        var first=(tools.jackson.databind.node.ObjectNode)draft.get("widgets").get(0);
        first.put("visible",false);
        var saved=call("PUT","/preferences/current/dashboard",draft);
        assertEquals(200,saved.statusCode(),saved.body());
        long revision=json.readTree(saved.body()).get("revision").asLong();
        assertFalse(json.readTree(saved.body()).get("widgets").get(0).get("visible").asBoolean());
        assertEquals(409,call("PUT","/preferences/current/dashboard",draft).statusCode());
        assertEquals(409,call("DELETE","/preferences/current/dashboard?revision="+(revision-1),null).statusCode());
        var restored=call("DELETE","/preferences/current/dashboard?revision="+revision,null);
        assertEquals(200,restored.statusCode(),restored.body());
        assertTrue(json.readTree(restored.body()).get("widgets").get(0).get("visible").asBoolean());
    }
    @Test void revokedFinancialAccessRemovesMoneyAndRejectsMonetaryRanking() throws Exception {
        jdbc.update("DELETE FROM role_permissions WHERE role_id=(SELECT role_id FROM users WHERE user_id=?) AND permission_id=(SELECT permission_id FROM permissions WHERE permission_key='finance.read')",userId);
        var response=call("GET","/dashboard"+query(),null);
        assertEquals(200,response.statusCode(),response.body());
        var widgets=json.readTree(response.body()).get("widgets");
        for(var w:widgets) {
            assertFalse(w.get("unit").asText().startsWith("BRL"),w.get("id").asText());
            if(w.get("id").asText().equals("sales.products")) for(var row:w.get("rows")) assertFalse(row.has("revenue"));
        }
        assertEquals(403,call("GET","/dashboard"+query()+"&rankingMetric=REVENUE",null).statusCode());
        assertEquals(403,call("GET","/dashboard/widgets/sales.revenue/details"+query(),null).statusCode());
    }
    @Test void displayedTotalsMatchTheirSourceAndSeries() throws Exception {
        var response=call("GET","/dashboard"+query(),null);
        assertEquals(200,response.statusCode(),response.body());
        var from=periods.today().withDayOfMonth(1);var to=periods.today();
        Map<String,java.math.BigDecimal> expected=Map.of(
            "sales.revenue",jdbc.queryForObject("SELECT coalesce(sum(total_amount),0) FROM orders WHERE status IN ('CONFIRMED','INVOICED') AND confirmed_at>=? AND confirmed_at<?",java.math.BigDecimal.class,from.atStartOfDay(),to.plusDays(1).atStartOfDay()),
            "cash.net",jdbc.queryForObject("SELECT coalesce(sum(amount),0) FROM financial_cash_flow WHERE kind='ACTUAL' AND occurred_on BETWEEN ? AND ?",java.math.BigDecimal.class,from,to));
        for(var w:json.readTree(response.body()).get("widgets")) if(expected.containsKey(w.get("id").asText())) {
            var total=new java.math.BigDecimal(w.get("metric").asText());
            assertEquals(0,expected.get(w.get("id").asText()).compareTo(total),w.get("id").asText());
            java.math.BigDecimal seriesTotal=java.math.BigDecimal.ZERO;
            for(var point:w.get("series")) seriesTotal=seriesTotal.add(new java.math.BigDecimal(point.get("value").asText()));
            assertEquals(0,total.compareTo(seriesTotal),w.get("id").asText());
        }
    }

}
