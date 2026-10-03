import test from "node:test";
import assert from "node:assert/strict";
import fs from "node:fs";
import path from "node:path";
import vm from "node:vm";
import { createRequire } from "node:module";
import { fileURLToPath } from "node:url";
import React from "react";
import { renderToStaticMarkup } from "react-dom/server";
import ts from "typescript";

const localRequire = createRequire(import.meta.url);
const sourceRoot = path.join(path.dirname(fileURLToPath(import.meta.url)), "../src");
function loadModule(relativePath, mocks = {}) {
  const source = fs.readFileSync(path.join(sourceRoot, relativePath), "utf8");
  const compiled = ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2020, jsx: ts.JsxEmit.ReactJSX } }).outputText;
  const compiledModule = { exports: {} };
  vm.runInNewContext(compiled, { module: compiledModule, exports: compiledModule.exports,
    URLSearchParams, require: (name) => name in mocks ? mocks[name] : localRequire(name) });
  return compiledModule.exports;
}
const apiBuilder = { query: (definition) => definition, mutation: (definition) => definition };
const apiMock = { baseApi: { injectEndpoints: ({ endpoints }) => endpoints(apiBuilder) } };
const link = ({ href, children, ...props }) => React.createElement("a", { href, ...props }, children);

test("Dashboard V2 uses the backend route, query parameters, and book-scoped cache", () => {
  const api = loadModule("dashboard/controller/dashboardApi.ts", { "@/common/api/baseApi": apiMock }).dashboardApi;
  const key = { moneyBookUid: 7, year: 2026, month: 10 };
  assert.deepEqual(JSON.parse(JSON.stringify(api.getDashboard.query(key))), {
    url: "money-books/7/dashboard", params: { year: 2026, month: 10 },
  });
  assert.deepEqual(JSON.parse(JSON.stringify(api.getDashboard.providesTags({}, undefined, key))), [{ type: "Dashboard", id: 7 }]);
});

test("transaction create, update, and delete refresh the matching Dashboard cache", () => {
  const api = loadModule("transaction/controller/transactionApi.ts", { "@/common/api/baseApi": apiMock }).transactionApi;
  const arg = { moneyBookUid: 7, transactionUid: 3, request: {} };
  for (const endpoint of [api.createTransaction, api.updateTransaction, api.deleteTransaction]) {
    assert.ok(endpoint.invalidatesTags({}, undefined, arg).some((tag) => tag.type === "Dashboard" && tag.id === 7));
    assert.deepEqual(JSON.parse(JSON.stringify(endpoint.invalidatesTags(undefined, { status: 403 }, arg))), []);
  }
});

test("expense ranking API sends MONTH and YEAR contract parameters and scoped report tag", () => {
  const api = loadModule("report/controller/reportApi.ts", { "@/common/api/baseApi": apiMock }).reportApi;
  const month = { moneyBookUid: 7, periodType: "MONTH", year: 2026, month: 10 };
  const year = { moneyBookUid: 7, periodType: "YEAR", year: 2026 };
  assert.deepEqual(JSON.parse(JSON.stringify(api.getExpenseRanking.query(month))), {
    url: "money-books/7/reports/expense-ranking", params: { periodType: "MONTH", year: 2026, month: 10 },
  });
  assert.deepEqual(JSON.parse(JSON.stringify(api.getExpenseRanking.query(year))), {
    url: "money-books/7/reports/expense-ranking", params: { periodType: "YEAR", year: 2026 },
  });
  assert.deepEqual(JSON.parse(JSON.stringify(api.getExpenseRanking.providesTags([], undefined, month))), [{ type: "Report", id: 7 }]);
});

test("ranking UI displays the desktop table and mobile list with formatted values", () => {
  const view = loadModule("report/components/ExpenseRankingView.tsx", {
    "next/link": { default: link }, "next/navigation": { useRouter: () => ({ push: () => {} }), useSearchParams: () => new URLSearchParams("periodType=MONTH&year=2026&month=10") },
    "@/common/format/money": { formatMoney: (value) => `${Number(value).toLocaleString("ko-KR")}원` },
    "../hooks/useExpenseRanking": { useExpenseRanking: () => ({ permission: { canRead: true }, isLoading: false, isFetching: false, isError: false, ranking: [
      { rank: 1, transactionUid: 88, transactionDate: "2026-10-03", categoryName: "보험", memo: "자동차 보험", accountName: "신한카드", amount: 520000 },
    ] }) },
  }).default;
  const markup = renderToStaticMarkup(React.createElement(view, { moneyBookUid: 7 }));
  for (const value of ["1위", "자동차 보험", "보험", "2026-10-03", "신한카드", "520,000원", "md:block", "md:hidden"]) assert.match(markup, new RegExp(value));
});

test("ranking UI shows a readable empty state", () => {
  const view = loadModule("report/components/ExpenseRankingView.tsx", {
    "next/link": { default: link }, "next/navigation": { useRouter: () => ({ push: () => {} }), useSearchParams: () => new URLSearchParams("periodType=YEAR&year=2026") },
    "@/common/format/money": { formatMoney: String },
    "../hooks/useExpenseRanking": { useExpenseRanking: () => ({ permission: { canRead: true }, isLoading: false, isFetching: false, isError: false, ranking: [] }) },
  }).default;
  const markup = renderToStaticMarkup(React.createElement(view, { moneyBookUid: 7 }));
  assert.match(markup, /선택한 기간에 지출 내역이 없습니다/);
  assert.match(markup, /조회 연도/);
});

test("percentage helpers keep fraction and percentage-point units distinct", () => {
  const { formatFractionPercent, formatPercentPoints } = loadModule("common/format/percent.ts");
  assert.equal(formatFractionPercent(0.34), "34.0%");
  assert.equal(formatPercentPoints(34), "34.0%");
  assert.equal(formatPercentPoints(null), "-");
});
