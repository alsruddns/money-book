import test from "node:test";
import assert from "node:assert/strict";
import fs from "node:fs";
import path from "node:path";
import vm from "node:vm";
import { createRequire } from "node:module";
import { fileURLToPath } from "node:url";
import ts from "typescript";

const root = path.join(path.dirname(fileURLToPath(import.meta.url)), "../src");
const require = createRequire(import.meta.url);
function load(file, mocks = {}) {
  const source = fs.readFileSync(path.join(root, file), "utf8");
  const js = ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2020 } }).outputText;
  const mod = { exports: {} };
  vm.runInNewContext(js, { module: mod, exports: mod.exports, require: (name) => name in mocks ? mocks[name] : require(name) });
  return mod.exports;
}
const plain = (value) => JSON.parse(JSON.stringify(value));
const builder = { query: (definition) => definition, mutation: (definition) => definition };

test("Admin RTK Query uses exact backend paths, filters, request bodies, and targeted invalidation", () => {
  const api = load("admin/controller/adminApi.ts", { "@/common/api/baseApi": { baseApi: { injectEndpoints: ({ endpoints }) => endpoints(builder) } } }).adminApi;
  assert.equal(api.getAdminMe.query(), "admin/me");
  assert.equal(api.getAdminOverview.query(), "admin/overview");
  assert.deepEqual(plain(api.getAdminUsers.query({ page: 2, size: 50, keyword: "kim", status: "ACTIVE", systemRole: "USER" })), { url: "admin/users", params: { page: 2, size: 50, keyword: "kim", status: "ACTIVE", systemRole: "USER" } });
  assert.equal(api.getAdminUser.query(8), "admin/users/8");
  assert.deepEqual(plain(api.changeAdminUserStatus.query({ userUid: 8, status: "BLOCKED" })), { url: "admin/users/8/status", method: "PATCH", body: { status: "BLOCKED" } });
  assert.deepEqual(plain(api.changeAdminSystemRole.query({ userUid: 8, systemRole: "SYSTEM_ADMIN" })), { url: "admin/users/8/system-role", method: "PATCH", body: { systemRole: "SYSTEM_ADMIN" } });
  assert.deepEqual(plain(api.changeAdminSystemRole.invalidatesTags({}, undefined, { userUid: 8 })), [{ type: "AdminUser", id: 8 }, { type: "AdminUserList", id: "LIST" }, "AdminOverview"]);
  assert.deepEqual(plain(api.getAdminMoneyBooks.query({ page: 0, size: 20, keyword: "home", ownerUserUid: "3" })), { url: "admin/money-books", params: { page: 0, size: 20, keyword: "home", ownerUserUid: "3" } });
  assert.equal(api.getAdminMoneyBook.query(2), "admin/money-books/2");
  assert.deepEqual(plain(api.getAdminActivities.query({ page: 0, size: 20, moneyBookUid: "4", activityType: "BUDGET_UPDATED", startDate: "2026-10-01" })), { url: "admin/activities", params: { page: 0, size: 20, moneyBookUid: "4", activityType: "BUDGET_UPDATED", startDate: "2026-10-01" } });
  assert.deepEqual(plain(api.getAdminAuditLogs.query({ page: 1, size: 50, actionType: "USER_STATUS_CHANGED" })), { url: "admin/audit-logs", params: { page: 1, size: 50, actionType: "USER_STATUS_CHANGED" } });
});

test("Admin frontend enforces distinct system roles, protects self and Super Admin, and omits promotion option", () => {
  const source = fs.readFileSync(path.join(root, "admin/components/AdminViews.tsx"), "utf8");
  const shell = fs.readFileSync(path.join(root, "admin/components/AdminShell.tsx"), "utf8");
  assert.match(source, /session\.user\?\.userUid === data\.userUid/);
  assert.match(source, /data\.systemRole === "SUPER_ADMIN"/);
  assert.match(source, /session\.isSuperAdmin && !self/);
  assert.match(source, /<option value="USER">/);
  assert.match(source, /<option value="SYSTEM_ADMIN">/);
  assert.doesNotMatch(source, /<option value="SUPER_ADMIN">/);
  assert.match(shell, /item\.superOnly \|\| session\.isSuperAdmin/);
  assert.match(shell, /useAdminSession/);
  assert.match(shell, /session\.isAdmin/);
});

test("All requested Admin routes exist separately from MoneyBook pages", () => {
  for (const route of ["admin/page.tsx", "admin/users/page.tsx", "admin/users/[userUid]/page.tsx", "admin/money-books/page.tsx", "admin/money-books/[moneyBookUid]/page.tsx", "admin/activities/page.tsx", "admin/audit-logs/page.tsx", "admin/layout.tsx"]) {
    assert.ok(fs.existsSync(path.join(root, "app", route)), `missing route ${route}`);
  }
});
