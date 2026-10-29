import test from "node:test";
import assert from "node:assert/strict";
import { safeReturn, firstRoute } from "../src/routes/navigation.js";

let moduleId = 0;
const fresh = () => import("../src/services/api.js?test=" + ++moduleId);
const response = (status, body = null) =>
  new Response(status === 204 ? null : JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
const csrf = (token = "test-csrf") => response(200, { token, headerName: "X-CSRF-TOKEN" });
const originalFetch = globalThis.fetch;
const originalWindow = globalThis.window;

test.afterEach(() => {
  globalThis.fetch = originalFetch;
  if (originalWindow === undefined) delete globalThis.window;
  else globalThis.window = originalWindow;
});

test("mutation obtains CSRF and sends credentials through /api exactly once", async () => {
  const calls = [];
  globalThis.fetch = async (url, options) => {
    calls.push({ url, options });
    return url === "/api/auth/csrf" ? csrf() : response(201, { id: 7 });
  };
  const { api } = await fresh();
  assert.deepEqual(await api("/user/invitation", { method: "POST", body: { name: "Employee" } }), {
    id: 7,
  });
  assert.equal(calls.length, 2);
  assert.equal(calls[1].options.headers["X-CSRF-TOKEN"], "test-csrf");
  assert.equal(calls[1].options.credentials, "same-origin");
  assert.equal(calls[1].options.cache, "no-store");
  assert.equal(calls[1].options.body, '{"name":"Employee"}');
});

test("simultaneous writes share CSRF loading without repeating any mutation", async () => {
  let tokenRequests = 0;
  let writes = 0;
  let release;
  const pending = new Promise((resolve) => {
    release = resolve;
  });
  globalThis.fetch = async (url) => {
    if (url === "/api/auth/csrf") {
      tokenRequests++;
      await pending;
      return csrf();
    }
    writes++;
    return response(204);
  };
  const { api } = await fresh();
  const first = api("/auth/activity", { method: "POST" });
  const second = api("/auth/activity", { method: "POST" });
  release();
  await Promise.all([first, second]);
  assert.equal(tokenRequests, 1);
  assert.equal(writes, 2);
});

test("a delayed CSRF response cannot restore the token from an old session generation", async () => {
  let tokenRequests = 0;
  let releaseOld;
  const old = new Promise((resolve) => {
    releaseOld = resolve;
  });
  const sent = [];
  globalThis.fetch = async (url, options) => {
    if (url === "/api/auth/csrf") {
      tokenRequests++;
      if (tokenRequests === 1) {
        await old;
        return csrf("old");
      }
      return csrf("current");
    }
    sent.push(options.headers["X-CSRF-TOKEN"]);
    return response(204);
  };
  const { api, loadCsrf, clearCsrf } = await fresh();
  const first = loadCsrf();
  clearCsrf();
  const second = loadCsrf();
  releaseOld();
  await Promise.all([first, second]);
  await api("/auth/activity", { method: "POST" });
  assert.deepEqual(sent, ["current"]);
  assert.equal(tokenRequests, 2);
});

test("401 emits session expiration once and does not replay the write", async () => {
  let expired = 0;
  let writes = 0;
  globalThis.window = {
    dispatchEvent(event) {
      if (event.type === "erp:session-expired") expired++;
    },
  };
  globalThis.fetch = async (url) => {
    if (url === "/api/auth/csrf") return csrf();
    writes++;
    return response(401, { message: "Authentication required." });
  };
  const { api } = await fresh();
  await assert.rejects(
    api("/security/settings", { method: "PATCH", body: {} }),
    (error) => error.status === 401,
  );
  assert.equal(expired, 1);
  assert.equal(writes, 1);
});

test("public login failure does not signal expiration of another session", async () => {
  let dispatched = 0;
  globalThis.window = {
    dispatchEvent() {
      dispatched++;
    },
  };
  globalThis.fetch = async (url) => (url === "/api/auth/csrf" ? csrf() : response(401));
  const { api } = await fresh();
  await assert.rejects(
    api("/auth/login", { method: "POST", publicRequest: true, body: {} }),
    (error) => error.status === 401,
  );
  assert.equal(dispatched, 0);
});

test("403 clears CSRF for the next conscious attempt without replaying the rejected write", async () => {
  let tokenRequests = 0;
  let writes = 0;
  const sent = [];
  globalThis.fetch = async (url, options) => {
    if (url === "/api/auth/csrf") return csrf("token-" + ++tokenRequests);
    writes++;
    sent.push(options.headers["X-CSRF-TOKEN"]);
    return response(writes === 1 ? 403 : 204);
  };
  const { api } = await fresh();
  await assert.rejects(api("/auth/activity", { method: "POST" }), (error) => error.status === 403);
  assert.equal(writes, 1);
  await api("/auth/activity", { method: "POST" });
  assert.deepEqual(sent, ["token-1", "token-2"]);
});

test("409 keeps the replacement challenge as an error result and never retries login", async () => {
  let writes = 0;
  const challenge = {
    code: "SESSION_LIMIT_REACHED",
    challengeToken: "test-only",
    sessions: [{ id: "public-id" }],
  };
  globalThis.fetch = async (url) => {
    if (url === "/api/auth/csrf") return csrf();
    writes++;
    return response(409, challenge);
  };
  const { api } = await fresh();
  await assert.rejects(
    api("/auth/login", { method: "POST", publicRequest: true, body: {} }),
    (error) => {
      assert.equal(error.status, 409);
      assert.deepEqual(error.data, challenge);
      return true;
    },
  );
  assert.equal(writes, 1);
});

test("network errors preserve the failure and never repeat the operation", async () => {
  let attempts = 0;
  globalThis.fetch = async () => {
    attempts++;
    throw new TypeError("network");
  };
  const { api } = await fresh();
  await assert.rejects(api("/auth/session"), /Não foi possível acessar o servidor/);
  assert.equal(attempts, 1);
});

test("return navigation allows only internal routes authorized for the current session", () => {
  const user = { permissionKeys: ["access.users.manage"] };
  assert.equal(firstRoute(user), "/access/users");
  assert.equal(safeReturn("/access/invitations", user), "/access/invitations");
  assert.equal(safeReturn("/access/roles", user), "/access/users");
  for (const target of [
    "https://external.invalid",
    "//external.invalid",
    "javascript:alert(1)",
    "/missing",
    "/account?redirect=https://external.invalid",
  ]) {
    assert.equal(safeReturn(target, user), "/access/users");
  }
  assert.equal(firstRoute({ permissionKeys: [] }), "/account");
  assert.equal(safeReturn("/security/sessions", { permissionKeys: [] }), "/security/sessions");
});
