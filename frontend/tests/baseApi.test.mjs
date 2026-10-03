import test from "node:test";
import assert from "node:assert/strict";
import fs from "node:fs";
import path from "node:path";
import vm from "node:vm";
import { createRequire } from "node:module";
import { fileURLToPath } from "node:url";
import ts from "typescript";

const testDirectory = path.dirname(fileURLToPath(import.meta.url));
const localRequire = createRequire(import.meta.url);

function createHarness(initialTokens, handleRequest) {
  const source = fs.readFileSync(path.join(testDirectory, "../src/common/api/baseApi.ts"), "utf8");
  const compiled = ts.transpileModule(source, {
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2020 },
  }).outputText;
  let tokens = initialTokens;
  const calls = [];
  const actions = [];
  let baseUrl;
  const storage = {
    getTokens: () => tokens,
    setTokens: (next) => { tokens = next; },
    updateAccessToken: (accessToken) => {
      if (!tokens) return null;
      tokens = { ...tokens, accessToken };
      return tokens;
    },
    clearTokens: () => { tokens = null; },
  };
  const compiledModule = { exports: {} };
  const requireMock = (name) => {
    if (name === "@reduxjs/toolkit/query/react") {
      return {
        createApi: () => ({ util: { resetApiState: () => ({ type: "api/reset" }) } }),
        fetchBaseQuery: (config) => { baseUrl = config.baseUrl; return async (args) => {
          const headers = config.prepareHeaders(new Headers(), { arg: args });
          const url = typeof args === "string" ? args : args.url;
          calls.push({ url, requestUrl: `${config.baseUrl}/${url}`.replace(/([^:]\/)\//g, "$1"), authorization: headers.get("Authorization"), body: args.body });
          return handleRequest(args, headers);
        }; },
      };
    }
    if (name === "@/auth/storage/tokenStorage") return { tokenStorage: storage };
    if (name === "@/auth/store/authSlice") {
      return {
        clearAuth: () => ({ type: "auth/clearAuth" }),
        setTokens: (payload) => ({ type: "auth/setTokens", payload }),
      };
    }
    if (name === "@/auth/session/clearLocalSession") return {
      clearLocalSession: (dispatch, resetCache) => { storage.clearTokens(); dispatch({ type: "auth/clearAuth" }); resetCache?.(); },
    };
    throw new Error(`Unexpected import: ${name}`);
  };
  vm.runInNewContext(compiled, {
    module: compiledModule,
    exports: compiledModule.exports,
    require: requireMock,
    Headers,
  });
  const api = { dispatch: (action) => actions.push(action) };
  return {
    query: (args) => compiledModule.exports.baseQueryWithReauth(args, api, {}),
    calls,
    baseUrl,
    actions,
    getTokens: () => tokens,
  };
}

test("protected requests attach only the access token and succeed", async () => {
  const harness = createHarness(
    { accessToken: "valid-access", refreshToken: "refresh-secret" },
    () => ({ data: { userUid: 1 } }),
  );
  const result = await harness.query("auth/me");
  assert.equal(result.data.userUid, 1);
  assert.equal(harness.calls[0].authorization, "Bearer valid-access");
  assert.equal(harness.calls.length, 1);
  assert.equal(harness.baseUrl, "/api");
  assert.equal(harness.calls[0].requestUrl, "/api/auth/me");
});

test("signup, login, refresh, MoneyBook, and protected requests stay under the same-origin API prefix", async () => {
  const harness = createHarness(null, () => ({ data: {} }));
  for (const path of ["auth/signup", "auth/login", "auth/refresh", "money-books/3/transactions"]) await harness.query(path);
  assert.deepEqual(harness.calls.map((call) => call.requestUrl), ["/api/auth/signup", "/api/auth/login", "/api/auth/refresh", "/api/money-books/3/transactions"]);
});

test("a 401 refreshes access token and retries the original request once", async () => {
  const harness = createHarness(
    { accessToken: "expired", refreshToken: "refresh-secret" },
    (args, headers) => {
      const url = typeof args === "string" ? args : args.url;
      if (url === "auth/refresh") return { data: { accessToken: "new-access", refreshToken: "rotated-refresh" } };
      if (headers.get("Authorization") === "Bearer expired") return { error: { status: 401 } };
      return { data: { userUid: 1 } };
    },
  );
  const result = await harness.query("auth/me");
  assert.equal(result.data.userUid, 1);
  assert.equal(harness.calls.length, 3);
  assert.equal(harness.calls[1].url, "auth/refresh");
  assert.equal(harness.calls[1].authorization, null);
  assert.equal(harness.calls[1].body.refreshToken, "refresh-secret");
  assert.equal(harness.calls[2].authorization, "Bearer new-access");
  assert.equal(harness.getTokens().accessToken, "new-access");
  assert.equal(harness.getTokens().refreshToken, "rotated-refresh");
  assert.equal(harness.actions[0].type, "auth/setTokens");
});

test("a stored refresh token can restore a session without an access token", async () => {
  const harness = createHarness(
    { accessToken: "", refreshToken: "refresh-secret" },
    (args, headers) => {
      const url = typeof args === "string" ? args : args.url;
      if (url === "auth/refresh") return { data: { accessToken: "restored-access", refreshToken: "restored-refresh" } };
      if (!headers.get("Authorization")) return { error: { status: 401 } };
      return { data: { userUid: 1 } };
    },
  );
  const result = await harness.query("auth/me");
  assert.equal(result.data.userUid, 1);
  assert.equal(harness.calls[0].authorization, null);
  assert.equal(harness.calls[2].authorization, "Bearer restored-access");
});

test("simultaneous 401 responses share one refresh request", async () => {
  let refreshCount = 0;
  const harness = createHarness(
    { accessToken: "expired", refreshToken: "refresh-secret" },
    async (args, headers) => {
      const url = typeof args === "string" ? args : args.url;
      if (url === "auth/refresh") {
        refreshCount++;
        await new Promise((resolve) => setTimeout(resolve, 10));
        return { data: { accessToken: "new-access", refreshToken: "rotated-refresh" } };
      }
      if (headers.get("Authorization") === "Bearer expired") return { error: { status: 401 } };
      return { data: { userUid: 1 } };
    },
  );
  const results = await Promise.all([harness.query("auth/me"), harness.query("auth/me")]);
  assert.equal(refreshCount, 1);
  assert.ok(results.every((result) => result.data.userUid === 1));
  assert.equal(harness.calls.length, 5);
});

test("failed refresh clears tokens and auth state without repeating", async () => {
  const harness = createHarness(
    { accessToken: "expired", refreshToken: "bad-refresh" },
    () => ({ error: { status: 401 } }),
  );
  const result = await harness.query("auth/me");
  assert.equal(result.error.status, 401);
  assert.equal(harness.getTokens(), null);
  assert.equal(harness.actions[0].type, "auth/clearAuth");
  assert.equal(harness.actions[1].type, "api/reset");
  assert.equal(harness.calls.length, 2);
});

test("login and refresh 401 responses never trigger another refresh", async () => {
  for (const url of ["auth/login", "auth/refresh"]) {
    const harness = createHarness(
      { accessToken: "expired", refreshToken: "refresh-secret" },
      () => ({ error: { status: 401 } }),
    );
    await harness.query(url);
    assert.equal(harness.calls.length, 1);
    assert.equal(harness.calls[0].authorization, null);
  }
});

test("429 responses carry Retry-After seconds for shared error messaging", async () => {
  const harness = createHarness(null, () => ({
    error: { status: 429, data: { message: "too many requests" } },
    meta: { response: new Response(null, { status: 429, headers: { "Retry-After": "25" } }) },
  }));
  const result = await harness.query("auth/me");
  assert.equal(result.error.retryAfterSeconds, 25);
});

test("a second 401 after retry clears auth without another refresh", async () => {
  const harness = createHarness(
    { accessToken: "expired", refreshToken: "refresh-secret" },
    (args) => {
      const url = typeof args === "string" ? args : args.url;
      return url === "auth/refresh"
        ? { data: { accessToken: "new-access", refreshToken: "rotated-refresh" } }
        : { error: { status: 401 } };
    },
  );
  await harness.query("auth/me");
  assert.equal(harness.calls.length, 3);
  assert.equal(harness.getTokens(), null);
  assert.equal(harness.actions.some((action) => action.type === "auth/clearAuth"), true);
  assert.equal(harness.actions.at(-1).type, "api/reset");
});

test("auth state initializes from stored tokens and clears after recovery failure", () => {
  const source = fs.readFileSync(path.join(testDirectory, "../src/auth/store/authSlice.ts"), "utf8");
  const compiled = ts.transpileModule(source, {
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2020 },
  }).outputText;
  const compiledModule = { exports: {} };
  vm.runInNewContext(compiled, {
    module: compiledModule,
    exports: compiledModule.exports,
    require: (name) => localRequire(name),
  });
  const { default: reducer, initializeAuth, clearAuth } = compiledModule.exports;
  const initial = reducer(undefined, { type: "init" });
  assert.equal(initial.isInitialized, false);

  const restored = reducer(initial, initializeAuth({ accessToken: "access", refreshToken: "refresh" }));
  assert.equal(restored.isInitialized, true);
  assert.equal(restored.accessToken, "access");
  assert.equal(restored.refreshToken, "refresh");

  const cleared = reducer(restored, clearAuth());
  assert.equal(cleared.isInitialized, true);
  assert.equal(cleared.accessToken, null);
  assert.equal(cleared.refreshToken, null);
});
