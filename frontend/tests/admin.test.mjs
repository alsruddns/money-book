import { resolveLocaleTestImport } from "./localeTestImports.mjs";
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
  vm.runInNewContext(js, { module: mod, exports: mod.exports, require: (name) => name in mocks ? mocks[name] : resolveLocaleTestImport(name, {}, require) });
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
  assert.deepEqual(plain(api.getAdminUserActivities.query({ userUid: 8, page: 0, size: 10 })), { url: "admin/users/8/activities", params: { page: 0, size: 10 } });
  assert.deepEqual(plain(api.revokeAdminUserSessions.query(8)), { url: "admin/users/8/sessions/revoke-all", method: "POST" });
  assert.deepEqual(plain(api.revokeAdminUserSessions.invalidatesTags(2, undefined, 8)), [{ type: "AdminUser", id: 8 }, "AdminAuditLog"]);
  assert.deepEqual(plain(api.changeAdminUserStatus.query({ userUid: 8, status: "BLOCKED" })), { url: "admin/users/8/status", method: "PATCH", body: { status: "BLOCKED" } });
  assert.deepEqual(plain(api.changeAdminSystemRole.query({ userUid: 8, systemRole: "SYSTEM_ADMIN" })), { url: "admin/users/8/system-role", method: "PATCH", body: { systemRole: "SYSTEM_ADMIN" } });
  assert.deepEqual(plain(api.changeAdminSystemRole.invalidatesTags({}, undefined, { userUid: 8 })), [{ type: "AdminUser", id: 8 }, { type: "AdminUserList", id: "LIST" }, "AdminOverview", "AdminAuditLog"]);
  assert.deepEqual(plain(api.getAdminMoneyBooks.query({ page: 0, size: 20, keyword: "home", ownerUserUid: "3" })), { url: "admin/money-books", params: { page: 0, size: 20, keyword: "home", ownerUserUid: "3" } });
  assert.equal(api.getAdminMoneyBook.query(2), "admin/money-books/2");
  assert.deepEqual(plain(api.getAdminMoneyBookMembers.query({ moneyBookUid: 2, page: 0, size: 20 })), { url: "admin/money-books/2/members", params: { page: 0, size: 20 } });
  assert.deepEqual(plain(api.getAdminActivities.query({ page: 0, size: 20, moneyBookUid: "4", activityType: "BUDGET_UPDATED", startDate: "2026-10-01" })), { url: "admin/activities", params: { page: 0, size: 20, moneyBookUid: "4", activityType: "BUDGET_UPDATED", startDate: "2026-10-01" } });
  assert.deepEqual(plain(api.getAdminAuditLogs.query({ page: 1, size: 50, actorUserUid: "3", targetUserUid: "8", actionType: "USER_STATUS_CHANGED", targetType: "USER", startDate: "2026-10-01", endDate: "2026-10-03" })), { url: "admin/audit-logs", params: { page: 1, size: 50, actorUserUid: "3", targetUserUid: "8", actionType: "USER_STATUS_CHANGED", targetType: "USER", startDate: "2026-10-01", endDate: "2026-10-03" } });
});

test("Admin V2 uses the implemented operational DTOs, session revoke, member and audit filters", () => {
  const views = fs.readFileSync(path.join(root, "admin/components/AdminViews.tsx"), "utf8");
  const dto = fs.readFileSync(path.join(root, "admin/dto/AdminDtos.ts"), "utf8");
  const shell = fs.readFileSync(path.join(root, "admin/components/AdminShell.tsx"), "utf8");
  assert.match(views, /최근 성장/);
  assert.match(views, /활성 세션/);
  assert.match(views, /data\.withdrawnUsers/);
  assert.match(views, /data\.averageMembersPerMoneyBook/);
  assert.match(views, /data\.authProviders/);
  assert.match(views, /data\.activeSessionCount/);
  assert.match(views, /revokeMutation\.revokeSessions/);
  assert.match(views, /USER_SESSIONS_REVOKED/);
  assert.match(views, /data\.adminMemberCount/);
  assert.match(views, /data\.incomeTransactionCount/);
  assert.match(views, /data\.expenseTransactionCount/);
  assert.match(views, /data\.recurringRuleCount/);
  assert.match(views, /admin\/audit-logs\?targetUserUid=/);
  assert.match(views, /members\.data\.content/);
  assert.match(views, /사용자가 수행한 가계부 활동/);
  assert.match(views, /moneyBookUid=\$\{data\.moneyBookUid\}/);
  assert.match(dto, /targetUserUid\?: string/);
  assert.match(dto, /interface AdminMoneyBookMemberResponse/);
  assert.match(shell, /superOnly: true/);
  assert.match(views, /SYSTEM_ADMIN은 SUPER_ADMIN의 세션을 종료할 수 없습니다/);
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
    assert.ok(fs.existsSync(path.join(root, "app", "[locale]", "money", route)), `missing route ${route}`);
  }
});
