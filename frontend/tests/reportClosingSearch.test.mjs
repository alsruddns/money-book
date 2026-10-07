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
function load(relative, mocks = {}) {
  const source = fs.readFileSync(path.join(sourceRoot, relative), "utf8");
  const code = ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2020, jsx: ts.JsxEmit.ReactJSX } }).outputText;
  const mod = { exports: {} };
  vm.runInNewContext(code, { module: mod, exports: mod.exports, URLSearchParams,
    require: (name) => name in mocks ? mocks[name] : name === "react/jsx-runtime" ? { jsx: () => null, jsxs: () => null, Fragment: "fragment" } : localRequire(name) });
  return mod.exports;
}
const plain = (value) => JSON.parse(JSON.stringify(value));
const builder = { query: (definition) => definition, mutation: (definition) => definition };
const baseApiMock = { baseApi: { injectEndpoints: ({ endpoints }) => endpoints(builder) } };

test("transaction search defaults to the selected month and creates exact backend query options", () => {
  const search = load("transaction/search.ts");
  assert.deepEqual(plain(search.defaultTransactionFilters(2026, 2)), {
    startDate: "2026-02-01", endDate: "2026-02-28", transactionType: "", categoryUid: "", accountUid: "",
    keyword: "", minAmount: "", maxAmount: "", page: 0, size: 20, sort: "DATE_DESC",
  });
  const parsed = search.parseTransactionFilters(new URLSearchParams("startDate=2024-02-01&endDate=2024-02-29&transactionType=EXPENSE&categoryUid=12&accountUid=4&keyword=coffee&minAmount=0&maxAmount=10.25&page=2&size=50&sort=AMOUNT_ASC"), 2026, 2);
  assert.equal(parsed.endDate, "2024-02-29");
  assert.deepEqual(plain(search.filtersToSearchQuery(8, parsed)), {
    moneyBookUid: 8, startDate: "2024-02-01", endDate: "2024-02-29", transactionType: "EXPENSE", categoryUid: 12,
    accountUid: 4, keyword: "coffee", minAmount: 0, maxAmount: 10.25, page: 2, size: 50, sort: "AMOUNT_ASC",
  });
});

test("search query rejects invalid ranges, filters, amount bounds and unsupported pagination", () => {
  const { parseTransactionFilters, defaultTransactionFilters, isValidSearchDateRange, isValidSearchAmount } = load("transaction/search.ts");
  const fallback = defaultTransactionFilters(2026, 10);
  for (const query of ["month=10&year=2026&startDate=2026-10-31&endDate=2026-10-01", "startDate=no&endDate=2026-10-31",
    "startDate=2023-01-01&endDate=2026-10-01", "startDate=2026-10-01&endDate=2026-10-31&transactionType=TRANSFER",
    "startDate=2026-10-01&endDate=2026-10-31&minAmount=-1", "startDate=2026-10-01&endDate=2026-10-31&minAmount=4&maxAmount=3",
    "startDate=2026-10-01&endDate=2026-10-31&page=-1", "startDate=2026-10-01&endDate=2026-10-31&size=200",
    "startDate=2026-10-01&endDate=2026-10-31&sort=RANDOM"]) {
    assert.deepEqual(plain(parseTransactionFilters(new URLSearchParams(query), 2026, 10)), plain(fallback));
  }
  assert.equal(isValidSearchDateRange("2026-10-01", "2026-10-31"), true);
  assert.equal(isValidSearchDateRange("2023-10-01", "2026-10-01"), false);
  assert.equal(isValidSearchAmount("0"), true);
  assert.equal(isValidSearchAmount("-0.01"), false);
  assert.equal(isValidSearchAmount("1.234"), false);
});

test("filter changes reset pagination and preserve shareable query state", () => {
  const { buildTransactionSearchParams, defaultTransactionFilters } = load("transaction/search.ts");
  const filters = { ...defaultTransactionFilters(2026, 10), transactionType: "INCOME", keyword: "salary", page: 4, size: 50 };
  const params = buildTransactionSearchParams(new URLSearchParams("year=2026&month=10&page=4"), filters, true);
  assert.equal(params.get("search"), "1");
  assert.equal(params.get("transactionType"), "INCOME");
  assert.equal(params.get("keyword"), "salary");
  assert.equal(params.get("size"), "50");
  assert.equal(params.has("page"), false);
  const nextPage = buildTransactionSearchParams(params, { ...filters, page: 1 });
  assert.equal(nextPage.get("page"), "1");
});

test("search endpoint forwards date filters and results remain scoped to the transaction tag", () => {
  const { transactionApi } = load("transaction/controller/transactionApi.ts", { "@/common/api/baseApi": baseApiMock });
  const query = { moneyBookUid: 4, startDate: "2026-10-01", endDate: "2026-10-31", page: 0, size: 20, sort: "DATE_DESC" };
  assert.deepEqual(plain(transactionApi.searchTransactions.query(query)), { url: "money-books/4/transactions/search", params: {
    startDate: query.startDate, endDate: query.endDate, page: 0, size: 20, sort: "DATE_DESC",
  } });
  assert.deepEqual(plain(transactionApi.searchTransactions.providesTags({}, undefined, query)), [{ type: "Transaction", id: 4 }]);
});

test("report endpoints mirror backend monthly, yearly and period statistics paths", () => {
  const { reportApi } = load("report/controller/reportApi.ts", { "@/common/api/baseApi": baseApiMock });
  assert.deepEqual(plain(reportApi.getMonthlyReport.query({ moneyBookUid: 5, year: 2026, month: 10 })), {
    url: "money-books/5/reports/monthly", params: { year: 2026, month: 10 },
  });
  assert.deepEqual(plain(reportApi.getYearlyReport.query({ moneyBookUid: 5, year: 2026 })), {
    url: "money-books/5/reports/yearly", params: { year: 2026 },
  });
  assert.deepEqual(plain(reportApi.getCategoryStatistics.query({ moneyBookUid: 5, startDate: "2026-10-01", endDate: "2026-10-31", transactionType: "EXPENSE" })), {
    url: "money-books/5/reports/categories", params: { startDate: "2026-10-01", endDate: "2026-10-31", transactionType: "EXPENSE" },
  });
  assert.deepEqual(plain(reportApi.getAccountStatistics.query({ moneyBookUid: 5, startDate: "2026-10-01", endDate: "2026-10-31" })), {
    url: "money-books/5/reports/accounts", params: { startDate: "2026-10-01", endDate: "2026-10-31" },
  });
});

test("closing endpoints use the implemented month-closings path and invalidate only related snapshot/report data", () => {
  const { closingApi } = load("closing/controller/closingApi.ts", { "@/common/api/baseApi": baseApiMock });
  const key = { moneyBookUid: 11, year: 2026, month: 10 };
  assert.equal(closingApi.getMonthClosing.query(key), "money-books/11/month-closings/2026/10");
  assert.deepEqual(plain(closingApi.closeMonth.query(key)), { url: "money-books/11/month-closings/2026/10", method: "POST" });
  assert.deepEqual(plain(closingApi.cancelMonthClosing.query(key)), { url: "money-books/11/month-closings/2026/10", method: "DELETE" });
  const tags = [{ type: "Closing", id: 11 }, { type: "Closing", id: "11-2026-10" }, { type: "Report", id: 11 }, { type: "MoneyBookActivity", id: 11 }];
  assert.deepEqual(plain(closingApi.closeMonth.invalidatesTags({}, undefined, key)), tags);
  assert.deepEqual(plain(closingApi.cancelMonthClosing.invalidatesTags(undefined, { status: 403 }, key)), []);
});

test("money book API 404 states do not redirect a basePath page to the Next not-found route", () => {
  const source = fs.readFileSync(path.join(sourceRoot, "common/api/baseApi.ts"), "utf8");
  assert.match(source, /startsWith\("money-books\/"\)\) return;/);
  assert.match(source, /Money book resources use 404 to represent valid empty states/);
});

test("MoneyBook sidebar exposes report and monthly closing routes with read access", () => {
  const source = fs.readFileSync(path.join(sourceRoot, "moneybook/components/MoneyBookNavigation.tsx"), "utf8");
  const { getMoneyBookMenu } = load("moneybook/components/MoneyBookNavigation.tsx", {
    react: { useEffect() {}, useState: (value) => [value, () => {}] },
    "next/link": { default: "a" }, "next/navigation": { usePathname: () => "/", useRouter: () => ({ replace: () => {} }) },
    "@/common/components/advertisement/DesktopAdRail": { default: () => null },
    "@/common/api/getApiErrorMessage": { getApiErrorMessage: () => "error" },
    "@/settings/controller/moneyBookSettingApi": { useGetMoneyBookSettingQuery: () => ({ isLoading: false, isError: false, currentData: {} }) },
    "../hooks/useMoneyBookPermission": { useMoneyBookPermission: () => ({}) },
  });
  const routes = getMoneyBookMenu(7, { canRead: true, isOwner: false, isAdmin: false }).flatMap((group) => group.items.map((item) => item.href));
  assert.ok(routes.includes("/books/7/closings"));
  assert.ok(routes.includes("/books/7/closings"));
  assert.match(source, /DesktopAdRail/);
});
