package com.pi.erp.auth;

import static org.junit.jupiter.api.Assertions.*;

import com.pi.erp.auth.protection.AuthenticationThrottleService;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.function.BooleanSupplier;
import java.util.regex.Pattern;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
    properties = {
        "server.address=127.0.0.1",
        "server.port=18081",
        "erp.auth.bootstrap-enabled=false",
        "erp.auth.public-base-url=http://localhost:5173",
        "erp.auth.mail-from=access-tests@example.test",
        "erp.auth.login-per-ip-per-minute=10000",
        "erp.auth.recovery-per-ip-per-hour=10000",
        "erp.auth.argon-concurrency=4",
        "spring.mail.host=127.0.0.1",
        "spring.mail.port=11025",
        "spring.mail.username=",
        "spring.mail.password=",
        "spring.mail.properties.mail.smtp.auth=false",
        "spring.mail.properties.mail.smtp.starttls.enable=false",
        "spring.mail.properties.mail.smtp.starttls.required=false",
    }
)
@ContextConfiguration(
    initializers = AuthenticationAccessIntegrationTests.IsolatedDatabaseGuard.class
)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuthenticationAccessIntegrationTests {

    public static class IsolatedDatabaseGuard
        implements ApplicationContextInitializer<ConfigurableApplicationContext>
    {

        @Override
        public void initialize(ConfigurableApplicationContext context) {
            String url = context.getEnvironment().getProperty("spring.datasource.url", "");
            if (!url.matches("jdbc:postgresql://127\\.0\\.0\\.1:55433/auth_verify_[0-9]+")
                && !"jdbc:postgresql://127.0.0.1:55435/jamshub_test".equals(url)) {
                throw new IllegalStateException(
                    "Authentication tests require a dedicated auth_verify database on port 55433."
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
    private AuthenticationThrottleService throttle;

    @Autowired
    private JavaMailSenderImpl mailSender;

    @Autowired
    private ApplicationContext context;

    private final String password = "Access verification " + UUID.randomUUID();
    private final String suffix = UUID.randomUUID().toString().substring(0, 8);
    private Browser administrator;
    private long administratorId;
    private long administratorRoleId;
    private long operatorRoleId;
    private SmtpCapture smtp;

    @BeforeAll
    void fixtures() throws Exception {
        smtp = new SmtpCapture();
        smtp.start();
        administratorRoleId = jdbc.queryForObject(
            """
            SELECT rp.role_id FROM role_permissions rp JOIN permissions p USING(permission_id)
            WHERE p.permission_key IN ('access.users.manage','access.roles.manage','security.settings.manage')
            GROUP BY rp.role_id HAVING count(DISTINCT p.permission_key) = 3
            ORDER BY rp.role_id DESC LIMIT 1
            """,
            Long.class
        );
        operatorRoleId = jdbc.queryForObject(
            """
            INSERT INTO roles(name, active) VALUES (?, true) RETURNING role_id
            """,
            Long.class,
            "Verification operators " + suffix
        );
        jdbc.update(
            """
            INSERT INTO role_permissions(role_id, permission_id)
            SELECT ?, permission_id FROM permissions WHERE permission_key NOT IN
            ('access.users.manage','access.roles.manage','security.settings.manage')
            """,
            operatorRoleId
        );
        administratorId = createUser("administrator", administratorRoleId);
        administrator = loginBrowser(email("administrator"));
    }

    @AfterAll
    void shutdown() throws Exception {
        if (smtp != null) {
            smtp.close();
        }
    }

    @Test
    @Order(0)
    void frameworkMustNotCreateADefaultLoginAccount() {
        assertTrue(
            context.getBeansOfType(UserDetailsService.class).isEmpty(),
            "Only the explicit ERP authentication flow may supply login accounts."
        );
    }

    @Test
    @Order(1)
    void migrationsAndSessionSerializationUseRealPostgres() throws Exception {
        assertEquals(
            5,
            jdbc.queryForObject(
                "SELECT count(*) FROM flyway_schema_history WHERE success AND version IN ('1','2','3','4','5')",
                Integer.class
            )
        );
        assertEquals(
            1,
            jdbc.queryForObject("SELECT count(*) FROM security_settings", Integer.class)
        );
        Reply session = administrator.call("GET", "/auth/session", null);
        status(200, session);
        assertEquals(administratorId, number(session.object(), "userId"));
        assertTrue(array(session.object().get("permissionKeys")).contains("access.roles.manage"));
        assertTrue(
            jdbc.queryForObject(
                "SELECT count(*) FROM spring_session_attributes WHERE attribute_name = 'SPRING_SECURITY_CONTEXT'",
                Integer.class
            ) > 0
        );
        assertFalse(session.text().contains("passwordHash"));
        assertFalse(session.text().contains("credentialVersion"));
        Reply sessions = administrator.call("GET", "/auth/sessions", null);
        status(200, sessions);
        Map<String, Object> view = object(sessions.list().getFirst());
        assertFalse(view.containsKey("sessionId"));
        assertFalse(view.containsKey("userId"));
        assertFalse(view.containsKey("credentialVersion"));
        String storedSession = jdbc.queryForObject(
            "SELECT session_id FROM user_sessions WHERE user_id = ?",
            String.class,
            administratorId
        );
        assertFalse(sessions.text().contains(storedSession));
    }

    @Test
    @Order(2)
    void authenticationErrorsCsrfAndReadOnlyContracts() throws Exception {
        Browser anonymous = new Browser();
        status(401, anonymous.raw("PATCH", "/security/settings", Map.of("maxSessions", 3), null));
        status(
            403,
            anonymous.raw(
                "POST",
                "/auth/login",
                Map.of("email", email("administrator"), "password", password),
                null
            )
        );
        anonymous.csrf();
        status(
            401,
            anonymous.call(
                "POST",
                "/auth/login",
                Map.of("email", email("administrator"), "password", "incorrect")
            )
        );
        status(
            401,
            anonymous.call(
                "POST",
                "/auth/login",
                Map.of("email", email("missing"), "password", password)
            )
        );
        status(405, administrator.call("POST", "/user", Map.of()));
        status(405, administrator.call("POST", "/permission", Map.of()));
        status(
            405,
            administrator.call("PATCH", "/permission/1", Map.of("permissionKey", "made.up"))
        );
        status(405, administrator.call("DELETE", "/permission/1", null));
        status(
            400,
            administrator.call(
                "PATCH",
                "/user/" + administratorId,
                Map.of("password", "Forbidden administrative password")
            )
        );
        status(403, administrator.call("GET", "/auth/not-a-public-endpoint", null));
        assertEquals(
            "no-store",
            administrator
                .call("GET", "/auth/session", null)
                .response()
                .headers()
                .firstValue("Cache-Control")
                .orElse("")
        );
    }

    @Test
    @Order(3)
    void loginReplacesPhysicalSessionAndSameContextReusesSlot() throws Exception {
        long userId = createUser("rotation", operatorRoleId);
        Browser browser = new Browser();
        browser.csrf();
        String anonymousPrimary = jdbc.queryForObject(
            "SELECT primary_id FROM spring_session ORDER BY creation_time DESC LIMIT 1",
            String.class
        );
        List<String> before = jdbc.queryForList(
            "SELECT primary_id FROM spring_session",
            String.class
        );
        String staleCsrf = browser.csrfToken;
        Reply login = browser.call(
            "POST",
            "/auth/login",
            Map.of("email", email("rotation"), "password", password)
        );
        status(200, login);
        String cookie = login
            .response()
            .headers()
            .allValues("Set-Cookie")
            .stream()
            .filter(value -> value.startsWith("ERP_SESSION="))
            .findFirst()
            .orElse("");
        assertTrue(cookie.contains("HttpOnly"), "Session cookie must be HttpOnly.");
        assertTrue(cookie.contains("SameSite=Lax"), "Session cookie must use SameSite=Lax.");
        assertFalse(
            cookie.toLowerCase(java.util.Locale.ROOT).contains("domain="),
            "Session cookie must be host-only."
        );
        browser.csrfToken = null;
        String sessionId = jdbc.queryForObject(
            "SELECT session_id FROM user_sessions WHERE user_id = ?",
            String.class,
            userId
        );
        String primary = jdbc.queryForObject(
            "SELECT primary_id FROM spring_session WHERE session_id = ?",
            String.class,
            sessionId
        );
        assertFalse(
            before.contains(primary),
            "Authentication must create a new physical session row."
        );
        assertEquals(
            0,
            jdbc.update(
                "UPDATE spring_session SET session_id = ? WHERE primary_id = ?",
                UUID.randomUUID().toString(),
                anonymousPrimary
            ),
            "Stale writes must not restore the anonymous session."
        );
        status(403, browser.raw("POST", "/auth/activity", null, staleCsrf));
        browser.csrfToken = null;
        status(
            200,
            browser.call(
                "POST",
                "/auth/login",
                Map.of("email", email("rotation"), "password", password)
            )
        );
        assertEquals(1, activeSessions(userId));
        status(
            400,
            browser.call(
                "POST",
                "/auth/login",
                Map.of("email", email("administrator"), "password", password)
            )
        );
        status(204, browser.call("POST", "/auth/logout", null));
        browser.csrfToken = null;
        status(401, browser.call("GET", "/auth/session", null));
        browser.csrf();
        status(204, browser.call("POST", "/auth/logout", null));
    }

    @Test
    @Order(4)
    void simultaneousLoginsRespectLimitAndChallengeIsBoundToBrowserAndSingleUse() throws Exception {
        long userId = createUser("concurrent", operatorRoleId);
        Browser first = new Browser();
        Browser second = new Browser();
        Browser third = new Browser();
        first.csrf();
        second.csrf();
        third.csrf();
        List<Browser> browsers = List.of(first, second, third);
        List<Reply> replies;
        try (var executor = Executors.newFixedThreadPool(3)) {
            var jobs = browsers
                .stream()
                .<java.util.concurrent.Callable<Reply>>map(
                    browser -> () ->
                        browser.call(
                            "POST",
                            "/auth/login",
                            Map.of("email", email("concurrent"), "password", password)
                        )
                )
                .toList();
            replies = new ArrayList<>();
            for (var future : executor.invokeAll(jobs)) {
                replies.add(future.get());
            }
        }
        assertEquals(
            2,
            replies
                .stream()
                .filter(reply -> reply.status() == 200)
                .count()
        );
        assertEquals(
            1,
            replies
                .stream()
                .filter(reply -> reply.status() == 409)
                .count()
        );
        assertEquals(2, activeSessions(userId));
        int blocked = replies.get(0).status() == 409 ? 0 : replies.get(1).status() == 409 ? 1 : 2;
        Browser pending = browsers.get(blocked);
        Map<String, Object> challenge = replies.get(blocked).object();
        assertEquals("SESSION_LIMIT_REACHED", challenge.get("code"));
        status(401, pending.call("GET", "/auth/session", null));
        Map<String, Object> selected = object(array(challenge.get("sessions")).getFirst());
        String publicId = selected.get("id").toString();
        String token = challenge.get("challengeToken").toString();
        Browser attacker = new Browser();
        status(
            400,
            attacker.call(
                "POST",
                "/auth/sessionReplacement",
                Map.of("challengeToken", token, "sessionId", publicId)
            )
        );
        status(
            200,
            pending.call(
                "POST",
                "/auth/sessionReplacement",
                Map.of("challengeToken", token, "sessionId", publicId)
            )
        );
        pending.csrfToken = null;
        status(
            400,
            pending.call(
                "POST",
                "/auth/sessionReplacement",
                Map.of("challengeToken", token, "sessionId", publicId)
            )
        );
        assertEquals(2, activeSessions(userId));
        assertEquals(
            1,
            jdbc.queryForObject(
                "SELECT count(*) FROM user_sessions WHERE id = ? AND revoked_at IS NOT NULL",
                Integer.class,
                UUID.fromString(publicId)
            )
        );
        int invalidated = 0;
        for (int index = 0; index < browsers.size(); index++) {
            if (
                index != blocked &&
                browsers.get(index).call("GET", "/auth/session", null).status() == 401
            ) {
                invalidated++;
            }
        }
        assertEquals(1, invalidated);
    }

    @Test
    @Order(5)
    void currentPermissionsAndInactiveRolesTakeEffectOnNextRequest() throws Exception {
        long role = createRole("dynamic");
        long userId = createUser("dynamic", role);
        Browser browser = loginBrowser(email("dynamic"));
        status(403, browser.call("GET", "/product/search", null));
        long permissionId = permission("products.read");
        status(
            201,
            administrator.call(
                "POST",
                "/role/" + role + "/permissions",
                Map.of("permissionId", permissionId)
            )
        );
        status(200, browser.call("GET", "/product/search", null));
        assertTrue(
            array(
                browser.call("GET", "/auth/session", null).object().get("permissionKeys")
            ).contains("products.read")
        );
        status(
            204,
            administrator.call("DELETE", "/role/" + role + "/permissions/" + permissionId, null)
        );
        status(403, browser.call("GET", "/product/search", null));
        status(200, administrator.call("PATCH", "/role/" + role, Map.of("active", false)));
        status(401, browser.call("GET", "/auth/session", null));
        status(200, administrator.call("PATCH", "/role/" + role, Map.of("active", true)));
        status(401, browser.call("GET", "/auth/session", null));
        assertEquals(0, activeSessions(userId));
    }

    @Test
    @Order(6)
    void sessionsCannotBeRevokedAcrossOwnershipAndAdminNestedRouteChecksUser() throws Exception {
        long ownerId = createUser("owner", operatorRoleId);
        long otherId = createUser("other-owner", operatorRoleId);
        Browser owner = loginBrowser(email("owner"));
        Browser other = loginBrowser(email("other-owner"));
        String ownerSession = owner
            .call("GET", "/auth/session", null)
            .object()
            .get("publicSessionId")
            .toString();
        status(204, other.call("DELETE", "/auth/sessions/" + ownerSession, null));
        status(200, owner.call("GET", "/auth/session", null));
        status(
            204,
            administrator.call("DELETE", "/user/" + otherId + "/sessions/" + ownerSession, null)
        );
        status(200, owner.call("GET", "/auth/session", null));
        status(
            204,
            administrator.call("DELETE", "/user/" + ownerId + "/sessions/" + ownerSession, null)
        );
        status(401, owner.call("GET", "/auth/session", null));
        assertEquals(1, activeSessions(otherId));
    }

    @Test
    @Order(7)
    void idleAndAbsoluteExpirationDoNotReviveAfterPolicyIncrease() throws Exception {
        long idleId = createUser("idle", operatorRoleId);
        Browser idle = loginBrowser(email("idle"));
        jdbc.update(
            "UPDATE user_sessions SET last_activity_at = now() - interval '70 minutes' WHERE user_id = ?",
            idleId
        );
        status(200, administrator.call("PATCH", "/security/settings", Map.of("idleMinutes", 120)));
        status(401, idle.call("GET", "/auth/session", null));
        assertEquals(0, activeSessions(idleId));
        long absoluteId = createUser("absolute", operatorRoleId);
        Browser absolute = loginBrowser(email("absolute"));
        jdbc.update(
            "UPDATE user_sessions SET authenticated_at = now() - interval '730 minutes' WHERE user_id = ?",
            absoluteId
        );
        status(
            200,
            administrator.call("PATCH", "/security/settings", Map.of("absoluteMinutes", 1440))
        );
        status(401, absolute.call("GET", "/auth/session", null));
        assertEquals(0, activeSessions(absoluteId));
        status(400, administrator.call("PATCH", "/security/settings", Map.of("idleMinutes", 241)));
        status(400, administrator.call("PATCH", "/security/settings", Map.of("maxSessions", 0)));
        status(
            400,
            administrator.call(
                "PATCH",
                "/security/settings",
                Map.of("idleMinutes", 120, "absoluteMinutes", 60)
            )
        );
        status(
            200,
            administrator.call(
                "PATCH",
                "/security/settings",
                Map.of("idleMinutes", 60, "absoluteMinutes", 720)
            )
        );
    }

    @Test
    @Order(8)
    void pollingDoesNotRenewActivityAndActivityPreservesAbsoluteStart() throws Exception {
        long id = createUser("activity", operatorRoleId);
        Browser browser = loginBrowser(email("activity"));
        jdbc.update(
            "UPDATE user_sessions SET last_activity_at = now() - interval '2 minutes' WHERE user_id = ?",
            id
        );
        var before = jdbc.queryForMap(
            "SELECT last_activity_at, authenticated_at FROM user_sessions WHERE user_id = ?",
            id
        );
        status(200, browser.call("GET", "/auth/session", null));
        var polled = jdbc.queryForMap(
            "SELECT last_activity_at, authenticated_at FROM user_sessions WHERE user_id = ?",
            id
        );
        assertEquals(before, polled);
        status(204, browser.call("POST", "/auth/activity", null));
        var after = jdbc.queryForMap(
            "SELECT last_activity_at, authenticated_at FROM user_sessions WHERE user_id = ?",
            id
        );
        assertEquals(before.get("authenticated_at"), after.get("authenticated_at"));
        assertNotEquals(before.get("last_activity_at"), after.get("last_activity_at"));
        status(204, browser.call("POST", "/auth/activity", null));
        assertEquals(
            after,
            jdbc.queryForMap(
                "SELECT last_activity_at, authenticated_at FROM user_sessions WHERE user_id = ?",
                id
            )
        );
    }

    @Test
    @Order(9)
    void invitationResendConsumesOnlyLatestTokenAndNormalizesEmail() throws Exception {
        String recipient = email("invited");
        Reply created = administrator.call(
            "POST",
            "/user/invitation",
            Map.of(
                "name",
                "Invited employee",
                "email",
                "  " + recipient.toUpperCase(java.util.Locale.ROOT) + "  ",
                "roleId",
                operatorRoleId
            )
        );
        status(201, created);
        String id = created.object().get("id").toString();
        assertEquals(recipient, created.object().get("email"));
        assertFalse(created.text().contains("token"));
        String oldToken = smtp.token(recipient, 0);
        status(429, administrator.call("POST", "/user/invitation/" + id + "/resend", null));
        resetThrottle();
        status(
            409,
            administrator.call(
                "POST",
                "/user/invitation",
                Map.of("name", "Duplicate invitation", "email", recipient, "roleId", operatorRoleId)
            )
        );
        resetThrottle();
        status(202, administrator.call("POST", "/user/invitation/" + id + "/resend", null));
        String newToken = smtp.token(recipient, 1);
        assertFalse(oldToken.equals(newToken), "Resend must rotate the token.");
        Browser accept = new Browser();
        status(
            400,
            accept.call(
                "POST",
                "/auth/invitationAcceptance",
                Map.of("token", oldToken, "password", password)
            )
        );
        try (var executor = Executors.newFixedThreadPool(2)) {
            Browser parallel = new Browser();
            accept.csrf();
            parallel.csrf();
            var results = executor.invokeAll(
                List.<java.util.concurrent.Callable<Reply>>of(
                    () ->
                        accept.call(
                            "POST",
                            "/auth/invitationAcceptance",
                            Map.of("token", newToken, "password", password)
                        ),
                    () ->
                        parallel.call(
                            "POST",
                            "/auth/invitationAcceptance",
                            Map.of("token", newToken, "password", password)
                        )
                )
            );
            var statuses = List.of(results.get(0).get().status(), results.get(1).get().status());
            assertEquals(
                1,
                statuses
                    .stream()
                    .filter(value -> value == 201)
                    .count()
            );
            assertEquals(
                1,
                statuses
                    .stream()
                    .filter(value -> value == 400)
                    .count()
            );
        }
        assertEquals(
            1,
            jdbc.queryForObject(
                "SELECT count(*) FROM users WHERE email = ?",
                Integer.class,
                recipient
            )
        );
        status(401, accept.call("GET", "/auth/session", null));
        loginBrowser(recipient);
    }

    @Test
    @Order(10)
    void inactiveRoleExpiredInvitationAndSmtpFailureDoNotCreateUserOrUndoInvitation()
        throws Exception {
        long role = createRole("invitation-role");
        String recipient = email("inactive-invitation");
        Reply created = administrator.call(
            "POST",
            "/user/invitation",
            Map.of("name", "Inactive target", "email", recipient, "roleId", role)
        );
        status(201, created);
        String token = smtp.token(recipient, 0);
        status(200, administrator.call("PATCH", "/role/" + role, Map.of("active", false)));
        Browser browser = new Browser();
        status(
            400,
            browser.call(
                "POST",
                "/auth/invitationAcceptance",
                Map.of("token", token, "password", password)
            )
        );
        assertEquals(
            0,
            jdbc.queryForObject(
                "SELECT count(*) FROM users WHERE email = ?",
                Integer.class,
                recipient
            )
        );
        jdbc.update(
            "UPDATE user_invitations SET expires_at = now() - interval '1 minute' WHERE email = ?",
            recipient
        );
        status(200, administrator.call("PATCH", "/role/" + role, Map.of("active", true)));
        status(
            400,
            browser.call(
                "POST",
                "/auth/invitationAcceptance",
                Map.of("token", token, "password", password)
            )
        );
        mailSender.setPort(1);
        try {
            String failedEmail = email("smtp-failure");
            Reply failed = administrator.call(
                "POST",
                "/user/invitation",
                Map.of("name", "SMTP target", "email", failedEmail, "roleId", operatorRoleId)
            );
            status(201, failed);
            await(() ->
                "FAILED".equals(
                    jdbc.queryForObject(
                        "SELECT delivery_status FROM user_invitations WHERE email = ?",
                        String.class,
                        failedEmail
                    )
                )
            );
            assertEquals(
                1,
                jdbc.queryForObject(
                    "SELECT count(*) FROM user_invitations WHERE email = ? AND canceled_at IS NULL",
                    Integer.class,
                    failedEmail
                )
            );
        } finally {
            mailSender.setPort(11025);
        }
    }

    @Test
    @Order(11)
    void recoveryIsGenericAndResetRevokesSessionsAndTokensWithoutAutoLogin() throws Exception {
        long id = createUser("recovery", operatorRoleId);
        Browser active = loginBrowser(email("recovery"));
        Browser second = loginBrowser(email("recovery"));
        Browser publicBrowser = new Browser();
        Reply known = publicBrowser.call(
            "POST",
            "/auth/passwordRecovery",
            Map.of("email", email("recovery"))
        );
        Reply unknown = publicBrowser.call(
            "POST",
            "/auth/passwordRecovery",
            Map.of("email", email("absent-recovery"))
        );
        status(202, known);
        status(202, unknown);
        assertEquals(known.text(), unknown.text());
        String token = smtp.token(email("recovery"), 0);
        status(
            400,
            publicBrowser.call(
                "POST",
                "/auth/passwordReset",
                Map.of("token", token, "newPassword", "short")
            )
        );
        String replacement = "New access password " + UUID.randomUUID();
        status(
            204,
            publicBrowser.call(
                "POST",
                "/auth/passwordReset",
                Map.of("token", token, "newPassword", replacement)
            )
        );
        status(
            400,
            publicBrowser.call(
                "POST",
                "/auth/passwordReset",
                Map.of("token", token, "newPassword", replacement)
            )
        );
        status(401, active.call("GET", "/auth/session", null));
        status(401, second.call("GET", "/auth/session", null));
        status(401, publicBrowser.call("GET", "/auth/session", null));
        assertEquals(0, activeSessions(id));
        assertTrue(
            encoder.matches(
                replacement,
                jdbc.queryForObject(
                    "SELECT password_hash FROM users WHERE user_id = ?",
                    String.class,
                    id
                )
            )
        );
        Browser restored = new Browser();
        status(
            200,
            restored.call(
                "POST",
                "/auth/login",
                Map.of("email", email("recovery"), "password", replacement)
            )
        );
    }

    @Test
    @Order(12)
    void ownPasswordChangePreservesSpacesAndRevokesAllSessions() throws Exception {
        long id = createUser("password", operatorRoleId);
        Browser browser = loginBrowser(email("password"));
        Browser second = loginBrowser(email("password"));
        status(
            400,
            browser.call(
                "PATCH",
                "/auth/password",
                Map.of("currentPassword", "wrong", "newPassword", password)
            )
        );
        String spaced = "  New password with spaces " + UUID.randomUUID() + "  ";
        status(
            204,
            browser.call(
                "PATCH",
                "/auth/password",
                Map.of("currentPassword", password, "newPassword", spaced)
            )
        );
        status(401, browser.call("GET", "/auth/session", null));
        status(401, second.call("GET", "/auth/session", null));
        assertEquals(0, activeSessions(id));
        Browser changed = new Browser();
        status(
            401,
            changed.call(
                "POST",
                "/auth/login",
                Map.of("email", email("password"), "password", spaced.trim())
            )
        );
        status(
            200,
            changed.call(
                "POST",
                "/auth/login",
                Map.of("email", email("password"), "password", spaced)
            )
        );
    }

    @Test
    @Order(13)
    void abuseBackoffAndAuditPaginationAreEnforced() throws Exception {
        resetThrottle();
        Browser browser = new Browser();
        for (int attempt = 0; attempt < 5; attempt++) {
            status(
                401,
                browser.call(
                    "POST",
                    "/auth/login",
                    Map.of("email", email("nonexistent-throttle"), "password", password)
                )
            );
        }
        Reply blocked = browser.call(
            "POST",
            "/auth/login",
            Map.of("email", email("nonexistent-throttle"), "password", password)
        );
        status(429, blocked);
        assertTrue(blocked.response().headers().firstValue("Retry-After").isPresent());
        status(400, administrator.call("GET", "/security/audit/search?size=101", null));
        status(
            400,
            administrator.call(
                "GET",
                "/security/audit/search?from=2026-02-01T00:00:00Z&to=2026-01-01T00:00:00Z",
                null
            )
        );
        Reply audit = administrator.call(
            "GET",
            "/security/audit/search?size=25&action=LOGIN_FAILED",
            null
        );
        status(200, audit);
        assertFalse(array(audit.object().get("content")).isEmpty());
        assertFalse(audit.text().contains(password));
        assertFalse(audit.text().contains("challengeToken"));
        assertFalse(audit.text().contains("passwordHash"));
    }

    @Test
    @Order(14)
    void receiptReplayByAnotherAuthorizedOperatorPreservesHistoricalActor() throws Exception {
        long firstId = createUser("receipt-first", operatorRoleId);
        createUser("receipt-second", operatorRoleId);
        Browser first = loginBrowser(email("receipt-first"));
        Browser second = loginBrowser(email("receipt-second"));
        var link = jdbc.queryForMap(
            """
            SELECT ps.supplier_id, ps.product_id, s.warehouse_id
            FROM product_suppliers ps JOIN stock_items s USING(product_id)
            WHERE ps.active ORDER BY ps.product_supplier_id, s.stock_item_id LIMIT 1
            """
        );
        Reply purchase = administrator.call(
            "POST",
            "/purchase",
            Map.of("supplierId", link.get("supplier_id"), "userId", administratorId)
        );
        status(201, purchase);
        long purchaseId = number(purchase.object(), "id");
        Reply item = administrator.call(
            "POST",
            "/purchaseItem",
            Map.of(
                "purchaseId",
                purchaseId,
                "productId",
                link.get("product_id"),
                "warehouseId",
                link.get("warehouse_id"),
                "quantity",
                2,
                "unitCost",
                10
            )
        );
        status(201, item);
        long itemId = number(item.object(), "id");
        status(
            200,
            administrator.call("PATCH", "/purchase/" + purchaseId, Map.of("status", "SENT"))
        );
        status(
            200,
            administrator.call("PATCH", "/purchase/" + purchaseId, Map.of("status", "CONFIRMED"))
        );
        String requestId = UUID.randomUUID().toString();
        Map<String, Object> payload = Map.of(
            "requestId",
            requestId,
            "notes",
            "Receipt verification",
            "items",
            List.of(Map.of("purchaseItemId", itemId, "quantity", 2))
        );
        status(
            400,
            first.call(
                "POST",
                "/purchase/" + purchaseId + "/receive",
                Map.of(
                    "requestId",
                    requestId,
                    "operatorUserId",
                    administratorId,
                    "items",
                    List.of(Map.of("purchaseItemId", itemId, "quantity", 2))
                )
            )
        );
        Reply received = first.call("POST", "/purchase/" + purchaseId + "/receive", payload);
        status(201, received);
        long receiptId = number(received.object(), "id");
        status(200, administrator.call("PATCH", "/user/" + firstId, Map.of("active", false)));
        status(401, first.call("GET", "/auth/session", null));
        Reply replay = second.call("POST", "/purchase/" + purchaseId + "/receive", payload);
        status(200, replay);
        assertEquals(receiptId, number(replay.object(), "id"));
        assertEquals(firstId, number(object(replay.object().get("operator")), "id"));
        assertEquals(
            1,
            jdbc.queryForObject(
                "SELECT count(*) FROM purchase_receipts WHERE purchase_id = ?",
                Integer.class,
                purchaseId
            )
        );
        status(
            400,
            second.call(
                "POST",
                "/purchase/" + purchaseId + "/receive",
                Map.of(
                    "requestId",
                    requestId,
                    "notes",
                    "Different",
                    "items",
                    List.of(Map.of("purchaseItemId", itemId, "quantity", 2))
                )
            )
        );
    }

    @Test
    @Order(15)
    void composedInputsRequireTheirAdditionalOperationPermissions() throws Exception {
        long role = createRole("composed-inputs");
        for (String key : List.of(
            "products.create",
            "orders.create",
            "orders.update",
            "purchases.update"
        )) {
            status(
                201,
                administrator.call(
                    "POST",
                    "/role/" + role + "/permissions",
                    Map.of("permissionId", permission(key))
                )
            );
        }
        createUser("composed-inputs", role);
        Browser scoped = loginBrowser(email("composed-inputs"));
        var stock = jdbc.queryForMap(
            """
            SELECT s.product_id, s.warehouse_id, p.brand_id FROM stock_items s
            JOIN products p USING(product_id) WHERE p.active ORDER BY s.stock_item_id LIMIT 1
            """
        );
        int products = jdbc.queryForObject("SELECT count(*) FROM products", Integer.class);
        status(
            403,
            scoped.call(
                "POST",
                "/product",
                Map.of(
                    "name",
                    "Unauthorized initial stock",
                    "brandId",
                    stock.get("brand_id"),
                    "cost",
                    1,
                    "price",
                    2,
                    "stock",
                    Map.of(
                        "warehouseId",
                        stock.get("warehouse_id"),
                        "quantity",
                        0,
                        "reservedQuantity",
                        0,
                        "minQuantity",
                        0
                    )
                )
            )
        );
        assertEquals(products, jdbc.queryForObject("SELECT count(*) FROM products", Integer.class));
        long customer = jdbc.queryForObject(
            "SELECT customer_id FROM customers WHERE active ORDER BY customer_id LIMIT 1",
            Long.class
        );
        Reply order = administrator.call("POST", "/order", Map.of("customerId", customer));
        status(201, order);
        long orderId = number(order.object(), "id");
        status(
            403,
            scoped.call(
                "POST",
                "/orderItem",
                Map.of(
                    "orderId",
                    orderId,
                    "productId",
                    stock.get("product_id"),
                    "warehouseId",
                    stock.get("warehouse_id"),
                    "quantity",
                    1,
                    "unitPrice",
                    10
                )
            )
        );
        status(
            403,
            scoped.call(
                "POST",
                "/orderItem",
                Map.of(
                    "orderId",
                    orderId,
                    "productId",
                    stock.get("product_id"),
                    "warehouseId",
                    stock.get("warehouse_id"),
                    "quantity",
                    1,
                    "discount",
                    1
                )
            )
        );
        status(403, scoped.call("PATCH", "/order/" + orderId, Map.of("status", "CONFIRMED")));
        assertEquals(
            0,
            jdbc.queryForObject(
                "SELECT count(*) FROM order_items WHERE order_id = ?",
                Integer.class,
                orderId
            )
        );
        assertEquals(
            "PENDING",
            jdbc.queryForObject(
                "SELECT status FROM orders WHERE order_id = ?",
                String.class,
                orderId
            )
        );
        long supplier = jdbc.queryForObject(
            "SELECT supplier_id FROM suppliers WHERE active ORDER BY supplier_id LIMIT 1",
            Long.class
        );
        Reply purchase = administrator.call(
            "POST",
            "/purchase",
            Map.of("supplierId", supplier, "userId", administratorId)
        );
        status(201, purchase);
        long purchaseId = number(purchase.object(), "id");
        status(403, scoped.call("PATCH", "/purchase/" + purchaseId, Map.of("status", "SENT")));
        assertEquals(
            "DRAFT",
            jdbc.queryForObject(
                "SELECT status FROM purchases WHERE purchase_id = ?",
                String.class,
                purchaseId
            )
        );
    }

    @Test
    @Order(90)
    void concurrentAdministratorMutationsCannotRemoveLastAdministrator() throws Exception {
        resetThrottle();
        long secondId = createUser("second-administrator", administratorRoleId);
        Browser second = loginBrowser(email("second-administrator"));
        administrator.csrf();
        second.csrf();
        try (var executor = Executors.newFixedThreadPool(2)) {
            var results = executor.invokeAll(
                List.<java.util.concurrent.Callable<Reply>>of(
                    () ->
                        administrator.call(
                            "PATCH",
                            "/user/" + administratorId,
                            Map.of("active", false)
                        ),
                    () -> second.call("PATCH", "/user/" + secondId, Map.of("active", false))
                )
            );
            var statuses = List.of(results.get(0).get().status(), results.get(1).get().status());
            assertEquals(
                1,
                statuses
                    .stream()
                    .filter(value -> value == 200)
                    .count()
            );
            assertEquals(
                1,
                statuses
                    .stream()
                    .filter(value -> value == 400)
                    .count()
            );
        }
        assertEquals(
            1,
            jdbc.queryForObject(
                """
                SELECT count(*) FROM users WHERE user_id IN (?, ?) AND active
                """,
                Integer.class,
                administratorId,
                secondId
            )
        );
    }

    private String email(String name) {
        return name + "-" + suffix + "@example.test";
    }

    private long createRole(String name) {
        return jdbc.queryForObject(
            "INSERT INTO roles(name, active) VALUES (?, true) RETURNING role_id",
            Long.class,
            name + " " + suffix
        );
    }

    private long createUser(String name, long roleId) {
        return jdbc.queryForObject(
            """
            INSERT INTO users(name,email,password_hash,role_id,active)
            VALUES (?, ?, ?, ?, true) RETURNING user_id
            """,
            Long.class,
            "Verification " + name,
            email(name),
            encoder.encode(password),
            roleId
        );
    }

    private long permission(String key) {
        return jdbc.queryForObject(
            "SELECT permission_id FROM permissions WHERE permission_key = ?",
            Long.class,
            key
        );
    }

    private int activeSessions(long userId) {
        return jdbc.queryForObject(
            "SELECT count(*) FROM user_sessions WHERE user_id = ? AND revoked_at IS NULL",
            Integer.class,
            userId
        );
    }

    private Browser loginBrowser(String email) throws Exception {
        Browser browser = new Browser();
        status(
            200,
            browser.call("POST", "/auth/login", Map.of("email", email, "password", password))
        );
        browser.csrfToken = null;
        return browser;
    }

    @SuppressWarnings("unchecked")
    private void resetThrottle() {
        ((Map<String, ?>) ReflectionTestUtils.getField(throttle, "windows")).clear();
    }

    private void status(int expected, Reply reply) {
        assertEquals(
            expected,
            reply.status(),
            "Unexpected HTTP status; response bodies are omitted to protect secrets."
        );
    }

    private static long number(Map<String, Object> value, String key) {
        return ((Number) value.get(key)).longValue();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> object(Object value) {
        return (Map<String, Object>) value;
    }

    @SuppressWarnings("unchecked")
    private static List<Object> array(Object value) {
        return (List<Object>) value;
    }

    private static void await(BooleanSupplier condition) throws Exception {
        long deadline = System.nanoTime() + Duration.ofSeconds(12).toNanos();
        while (!condition.getAsBoolean()) {
            if (System.nanoTime() >= deadline) {
                fail("Timed out waiting for asynchronous outcome.");
            }
            Thread.sleep(50);
        }
    }

    private record Reply(int status, Object body, String text, HttpResponse<String> response) {
        Map<String, Object> object() {
            return AuthenticationAccessIntegrationTests.object(body);
        }

        List<Object> list() {
            return array(body);
        }
    }

    private class Browser {

        private final HttpClient client = HttpClient.newBuilder()
            .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
            .connectTimeout(Duration.ofSeconds(5))
            .build();
        private String csrfToken;
        private String headerName;

        void csrf() throws Exception {
            Reply reply = raw("GET", "/auth/csrf", null, null);
            status(200, reply);
            csrfToken = reply.object().get("token").toString();
            headerName = reply.object().get("headerName").toString();
        }

        Reply call(String method, String path, Object body) throws Exception {
            if (!method.equals("GET") && csrfToken == null) {
                csrf();
            }
            return raw(method, path, body, method.equals("GET") ? null : csrfToken);
        }

        Reply raw(String method, String path, Object body, String csrf) throws Exception {
            HttpRequest.Builder request = HttpRequest.newBuilder(
                URI.create("http://127.0.0.1:18081" + path)
            )
                .timeout(Duration.ofSeconds(20))
                .header("Accept", "application/json");
            if (csrf != null) {
                request.header(headerName == null ? "X-CSRF-TOKEN" : headerName, csrf);
            }
            if (body != null) {
                request.header("Content-Type", "application/json");
            }
            request.method(
                method,
                body == null
                    ? HttpRequest.BodyPublishers.noBody()
                    : HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body))
            );
            HttpResponse<String> response = client.send(
                request.build(),
                HttpResponse.BodyHandlers.ofString()
            );
            Object value = response.body().isBlank()
                ? null
                : json.readValue(response.body(), Object.class);
            return new Reply(response.statusCode(), value, response.body(), response);
        }
    }

    private static class SmtpCapture implements AutoCloseable {

        private final List<String> messages = new CopyOnWriteArrayList<>();
        private ServerSocket server;
        private Thread listener;

        void start() throws Exception {
            server = new ServerSocket(11025, 20, InetAddress.getByName("127.0.0.1"));
            listener = Thread.ofPlatform()
                .daemon()
                .start(() -> {
                    while (!server.isClosed()) {
                        try {
                            Socket socket = server.accept();
                            Thread.ofPlatform()
                                .daemon()
                                .start(() -> receive(socket));
                        } catch (Exception exception) {
                            if (!server.isClosed()) {
                                throw new IllegalStateException("Local SMTP capture failed.");
                            }
                        }
                    }
                });
        }

        private void receive(Socket socket) {
            try (
                socket;
                var input = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8)
                );
                var output = new PrintWriter(
                    new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8),
                    true
                )
            ) {
                output.print("220 localhost test SMTP\r\n");
                output.flush();
                String line;
                StringBuilder message = null;
                while ((line = input.readLine()) != null) {
                    if (message != null) {
                        if (line.equals(".")) {
                            messages.add(message.toString());
                            message = null;
                            output.print("250 accepted\r\n");
                        } else {
                            message.append(line).append('\n');
                        }
                    } else if (line.startsWith("EHLO") || line.startsWith("HELO")) {
                        output.print("250 localhost\r\n");
                    } else if (line.equals("DATA")) {
                        message = new StringBuilder();
                        output.print("354 send data\r\n");
                    } else if (line.equals("QUIT")) {
                        output.print("221 bye\r\n");
                        output.flush();
                        break;
                    } else {
                        output.print("250 ok\r\n");
                    }
                    output.flush();
                }
            } catch (Exception exception) {
                // Fixture transport only; timeout assertions describe delivery failures without logging secrets.
            }
        }

        String token(String email, int index) throws Exception {
            await(
                () ->
                    messages
                        .stream()
                        .filter(message -> message.contains(email) && message.contains("#token="))
                        .count() > index
            );
            String message = messages
                .stream()
                .filter(value -> value.contains(email) && value.contains("#token="))
                .skip(index)
                .findFirst()
                .orElseThrow();
            var matcher = Pattern.compile("#token=([A-Za-z0-9_-]+)").matcher(message);
            assertTrue(matcher.find(), "A token link must be sent through local SMTP.");
            return matcher.group(1);
        }

        @Override
        public void close() throws Exception {
            if (server != null) server.close();
            if (listener != null) listener.join(1000);
        }
    }
}
