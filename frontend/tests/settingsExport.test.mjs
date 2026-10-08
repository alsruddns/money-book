import { resolveLocaleTestImport } from "./localeTestImports.mjs";
import test from "node:test";
import assert from "node:assert/strict";
import fs from "node:fs";
import path from "node:path";
import vm from "node:vm";
import { createRequire } from "node:module";
import { fileURLToPath } from "node:url";
import ts from "typescript";

const localRequire = createRequire(import.meta.url);
const sourceRoot = path.join(path.dirname(fileURLToPath(import.meta.url)), "../src");
function load(relativePath, mocks = {}, globals = {}) {
  const source = fs.readFileSync(path.join(sourceRoot, relativePath), "utf8");
  const compiled = ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2020, jsx: ts.JsxEmit.ReactJSX } }).outputText;
  const mod = { exports: {} };
  vm.runInNewContext(compiled, { module: mod, exports: mod.exports, require: (name) => name in mocks ? mocks[name] : resolveLocaleTestImport(name, mocks, localRequire), URLSearchParams, FormData: TestFormData, ...globals });
  return mod.exports;
}
const plain = (value) => JSON.parse(JSON.stringify(value));
class TestFormData { values = new Map(); set(key, value) { this.values.set(key, value); } get(key) { return this.values.get(key); } }
const builder = { query: (definition) => definition, mutation: (definition) => definition };
const apiMock = { baseApi: { injectEndpoints: ({ endpoints }) => endpoints(builder) } };

test("settings endpoint uses actual URI, method, and invalidates calendar after a successful change", () => {
  const api = load("settings/controller/moneyBookSettingApi.ts", { "@/common/api/baseApi": apiMock }).moneyBookSettingApi;
  assert.equal(api.getMoneyBookSetting.query(12), "money-books/12/settings");
  const arg = { moneyBookUid: 12, request: { weekStartDay: "MONDAY" } };
  assert.deepEqual(plain(api.updateMoneyBookSetting.query(arg)), { url: "money-books/12/settings", method: "PUT", body: arg.request });
  assert.deepEqual(plain(api.updateMoneyBookSetting.invalidatesTags({}, undefined, arg)), [{ type: "MoneyBookSetting", id: 12 }, { type: "Calendar", id: 12 }, { type: "MoneyBookActivity", id: 12 }]);
});

test("export query sends only backend-supported filters and keeps bearer-authenticated RTK request path", () => {
  const api = load("export/controller/transactionExportApi.ts", { "@/common/api/baseApi": apiMock, "@/common/download/fileDownload": { prepareDownload: () => ({}) } }).transactionExportApi;
  const query = api.exportTransactions.query({ moneyBookUid: 12, format: "csv", startDate: "2026-01-01", endDate: "2026-10-02", transactionType: "EXPENSE", categoryUid: 4, accountUid: 7, keyword: " lunch " });
  assert.equal(query.url, "money-books/12/exports/transactions.csv?startDate=2026-01-01&endDate=2026-10-02&transactionType=EXPENSE&categoryUid=4&accountUid=7&keyword=+lunch+");
  const xlsx = api.exportTransactions.query({ moneyBookUid: 12, format: "xlsx", startDate: "2026-01-01", endDate: "2026-10-02" });
  assert.match(xlsx.url, /transactions\.xlsx\?/);
  assert.deepEqual(api.exportTransactions.invalidatesTags, undefined);
});

test("download filename parser accepts server name, prevents path traversal, and uses fallback", () => {
  const { parseDownloadFilename } = load("common/download/fileDownload.ts", {});
  assert.equal(parseDownloadFilename("attachment; filename*=UTF-8''%ED%85%8C%EC%8A%A4%ED%8A%B8.csv", "fallback.csv"), "테스트.csv");
  assert.equal(parseDownloadFilename('attachment; filename="..\\secret.csv"', "fallback.csv"), "secret.csv");
  assert.equal(parseDownloadFilename(null, "fallback.xlsx"), "fallback.xlsx");
});

test("backup uses implemented routes, multipart file contract, and invalidates MoneyBook list after restore", () => {
  const api = load("backup/controller/moneyBookBackupApi.ts", { "@/common/api/baseApi": apiMock, "@/common/download/fileDownload": { prepareDownload: () => ({}) } }).moneyBookBackupApi;
  assert.equal(api.exportMoneyBookBackup.query(5).url, "money-books/5/backups/export");
  const file = { name: "book.json" };
  const validQuery = api.validateMoneyBookBackup.query({ file });
  assert.equal(validQuery.url, "money-books/backups/validate"); assert.equal(validQuery.method, "POST"); assert.equal(validQuery.body.get("file"), file);
  const restoreQuery = api.restoreMoneyBookBackup.query({ file });
  assert.equal(restoreQuery.url, "money-books/backups/restore"); assert.equal(restoreQuery.body.get("file"), file);
  assert.deepEqual(plain(api.restoreMoneyBookBackup.invalidatesTags({}, undefined, { file })), ["MoneyBook", "MoneyBookActivity"]);
  assert.deepEqual(plain(api.restoreMoneyBookBackup.invalidatesTags(undefined, { status: 400 }, { file })), []);
});
