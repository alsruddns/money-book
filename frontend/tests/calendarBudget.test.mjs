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
function loadModule(relativePath, mocks = {}, globals = {}) {
  const source = fs.readFileSync(path.join(sourceRoot, relativePath), "utf8");
  const compiled = ts.transpileModule(source, {
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2020, jsx: ts.JsxEmit.ReactJSX },
  }).outputText;
  const compiledModule = { exports: {} };
  vm.runInNewContext(compiled, {
    module: compiledModule, exports: compiledModule.exports,
    require: (name) => name in mocks ? mocks[name] : localRequire(name), ...globals,
  });
  return compiledModule.exports;
}
const plain = (value) => JSON.parse(JSON.stringify(value));
const link = ({ href, children, ...props }) => React.createElement("a", { href, ...props }, children);
const formatMoney = (value) => `${Number(value).toLocaleString("ko-KR")}원`;
const builder = { query: (definition) => definition, mutation: (definition) => definition };
const apiMock = { baseApi: { injectEndpoints: ({ endpoints }) => endpoints(builder) } };
const day = (date, dayOfWeek, extras = {}) => ({
  date, dayOfWeek, weekend: dayOfWeek === "SUNDAY" || dayOfWeek === "SATURDAY",
  holiday: false, holidayName: null, incomeAmount: 0, expenseAmount: 0,
  transferInAmount: 0, transferOutAmount: 0, transactionCount: 0, transferCount: 0,
  hasRecurringGeneratedTransaction: false, ...extras,
});
const budget = {
  configured: true, budgetUid: 2, moneyBookUid: 7, year: 2026, month: 10,
  totalBudget: 100000, totalExpense: 125000, remainingBudget: -25000,
  usageRate: 125, overBudget: true,
  categories: [{ categoryUid: 3, categoryName: "식비", budgetAmount: 50000, expenseAmount: 60000,
    remainingAmount: -10000, usageRate: 120, overBudget: true }],
};

test("calendar and budget API match backend paths, methods, and scoped cache tags", () => {
  const calendar = loadModule("calendar/controller/calendarApi.ts", { "@/common/api/baseApi": apiMock }).calendarApi;
  const budgetApi = loadModule("budget/controller/budgetApi.ts", { "@/common/api/baseApi": apiMock }).budgetApi;
  const month = { moneyBookUid: 7, year: 2026, month: 10 };
  assert.deepEqual(plain(calendar.getMonthlyCalendar.query(month)), {
    url: "money-books/7/calendar", params: { year: 2026, month: 10 },
  });
  assert.equal(calendar.getCalendarDay.query({ moneyBookUid: 7, date: "2026-10-02" }), "money-books/7/calendar/2026-10-02");
  assert.deepEqual(plain(calendar.getMonthlyCalendar.providesTags({}, undefined, month)), [
    { type: "Calendar", id: 7 }, { type: "Calendar", id: "7-2026-10" },
  ]);
  assert.equal(budgetApi.getMonthlyBudget.query(month), "money-books/7/budgets/2026/10");
  const save = { ...month, request: { totalBudget: null, categories: [{ categoryUid: 3, amount: 50000 }] } };
  assert.deepEqual(plain(budgetApi.saveMonthlyBudget.query(save)), {
    url: "money-books/7/budgets/2026/10", method: "PUT", body: save.request,
  });
  assert.deepEqual(plain(budgetApi.saveMonthlyBudget.invalidatesTags({}, undefined, save)), [{ type: "Budget", id: "7-2026-10" }, { type: "Report", id: 7 }]);
  assert.deepEqual(plain(budgetApi.saveMonthlyBudget.invalidatesTags(undefined, { status: 403 }, save)), []);
});

test("calendar grid aligns first weekday and handles 28, 29, 30, and 31 day months", () => {
  const { buildCalendarGrid } = loadModule("calendar/calendarGrid.ts");
  for (const [year, month, length] of [[2026, 2, 28], [2024, 2, 29], [2026, 4, 30], [2026, 10, 31]]) {
    const first = new Date(year, month - 1, 1).getDay();
    const weekdays = ["SUNDAY", "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY"];
    const days = Array.from({ length }, (_, index) => day(
      `${year}-${String(month).padStart(2, "0")}-${String(index + 1).padStart(2, "0")}`,
      weekdays[(first + index) % 7],
    ));
    const cells = buildCalendarGrid(days);
    assert.equal(cells.length % 7, 0);
    assert.equal(cells.filter(Boolean).length, length);
    assert.equal(cells[first].date, days[0].date);
    assert.equal(cells[first + length - 1].date, days[length - 1].date);
    assert.equal(cells.slice(0, first).every((cell) => cell === null), true);
  }
});

test("calendar hook aggregates income, expense, and transfers without mixing them", () => {
  const calendarResponse = { days: [
    day("2026-10-01", "THURSDAY", { incomeAmount: 300000, expenseAmount: 50000, transferInAmount: 10000, transferOutAmount: 10000 }),
    day("2026-10-02", "FRIDAY", { incomeAmount: 2000, expenseAmount: 5000 }),
  ] };
  const { useMonthlyCalendar } = loadModule("calendar/hooks/useMonthlyCalendar.ts", {
    react: { useMemo: (fn) => fn() },
    "@/common/api/getApiErrorMessage": { getApiErrorMessage: () => "오류" },
    "../controller/calendarApi": { useGetMonthlyCalendarQuery: () => ({ currentData: calendarResponse }) },
  });
  assert.deepEqual(plain(useMonthlyCalendar(7, 2026, 10).totals), {
    income: 302000, expense: 55000, transferIn: 10000, transferOut: 10000,
  });
});

test("calendar renders weekend, holiday, amounts, transfers, and empty dates", () => {
  const days = [day("2026-10-03", "SATURDAY", { holiday: true, holidayName: "개천절", incomeAmount: 3000, expenseAmount: 1000,
    transferOutAmount: 5000, transferCount: 1 }), day("2026-10-04", "SUNDAY")];
  const grid = loadModule("calendar/calendarGrid.ts");
  const View = loadModule("calendar/components/CalendarView.tsx", {
    "@/common/format/money": { formatMoney },
    "@/moneybook/hooks/useMoneyBookPermission": { useMoneyBookPermission: () => ({ canRead: true }) },
    "@/transaction/hooks/useMonthNavigation": { useMonthNavigation: () => ({ year: 2026, month: 10, moveMonth: () => {}, goToToday: () => {} }) },
    "@/transaction/components/MonthSelector": { default: () => React.createElement("span", null, "2026년 10월") },
    "../calendarGrid": grid,
    "../hooks/useMonthlyCalendar": { useMonthlyCalendar: () => ({ calendar: { days }, isLoading: false, isError: false }) },
    "./CalendarDayDetailDialog": { default: () => null },
  }).default;
  const markup = renderToStaticMarkup(React.createElement(View, { moneyBookUid: 7 }));
  assert.match(markup, /grid-cols-7/);
  assert.match(markup, /개천절/);
  assert.match(markup, /공휴일/);
  assert.match(markup, /수입 \+3,000원/);
  assert.match(markup, /지출 −1,000원/);
  assert.match(markup, /이체 1건/);
  assert.match(markup, /2026-10-04 상세 보기/);
});

test("calendar day detail shows transactions, transfers, and an empty state", () => {
  const selected = day("2026-10-03", "SATURDAY", { holiday: true, holidayName: "개천절" });
  const baseMocks = {
    react: { useEffect: () => {} },
    "@/common/components/DialogShell": { default: ({ title, children }) => React.createElement("section", { "aria-label": title }, children) },
    "@/common/format/money": { formatMoney },
  };
  function render(detail) {
    const Detail = loadModule("calendar/components/CalendarDayDetailDialog.tsx", {
      ...baseMocks, "../hooks/useCalendarDayDetail": { useCalendarDayDetail: () => ({ detail }) },
    }).default;
    return renderToStaticMarkup(React.createElement(Detail, { moneyBookUid: 7, day: selected, onClose: () => {} }));
  }
  assert.match(render({ transactions: [], transfers: [] }), /등록된 거래가 없습니다/);
  const markup = render({ holiday: true, holidayName: "개천절", transactions: [{ transactionUid: 1, transactionType: "EXPENSE", amount: 1000,
    categoryName: "식비", accountName: "현금", memo: "점심" }], transfers: [{ transferUid: 2,
    fromAccountName: "현금", toAccountName: "은행", amount: 5000, memo: "저축" }] });
  assert.match(markup, /토요일/);
  assert.match(markup, /개천절/);
  assert.match(markup, /식비/);
  assert.match(markup, /점심/);
  assert.match(markup, /현금 → 은행/);
  assert.match(markup, /5,000원/);
});

test("budget form validates amounts, null totals, and category sum", () => {
  const { validateBudgetForm } = loadModule("budget/budgetForm.ts");
  assert.deepEqual(plain(validateBudgetForm("", [{ categoryUid: 3, amount: "5,000.50" }]).request), {
    totalBudget: null, categories: [{ categoryUid: 3, amount: 5000.5 }],
  });
  assert.deepEqual(plain(validateBudgetForm("1000", [{ categoryUid: 3, amount: "" }]).request), {
    totalBudget: 1000, categories: [],
  });
  assert.match(validateBudgetForm("-1", []).error, /총 예산/);
  assert.match(validateBudgetForm("10", [{ categoryUid: 3, amount: "-1" }]).error, /카테고리/);
  assert.match(validateBudgetForm("10", [{ categoryUid: 3, amount: "11" }]).error, /초과/);
  assert.match(validateBudgetForm("1.001", []).error, /총 예산/);
});

test("budget save hook sends the validated final list to PUT", async () => {
  const calls = [];
  const form = loadModule("budget/budgetForm.ts");
  const { useSaveBudget } = loadModule("budget/hooks/useSaveBudget.ts", {
    react: { useState: (initial) => [initial, () => {}] },
    "@/common/api/getApiErrorMessage": { getApiErrorMessage: () => "저장 오류" },
    "../controller/budgetApi": { useSaveMonthlyBudgetMutation: () => [(value) => ({
      unwrap: async () => { calls.push(value); },
    }), { isLoading: false }] },
    "../budgetForm": form,
  });
  const save = useSaveBudget(7, 2026, 10);
  assert.equal(await save.save("", [{ categoryUid: 3, amount: "50,000" }, { categoryUid: 4, amount: "" }]), true);
  assert.deepEqual(plain(calls), [{
    moneyBookUid: 7, year: 2026, month: 10,
    request: { totalBudget: null, categories: [{ categoryUid: 3, amount: 50000 }] },
  }]);
});

test("budget form requests EXPENSE categories and omits income fields", () => {
  let requestedType;
  const Form = loadModule("budget/components/BudgetForm.tsx", {
    "next/link": { default: link },
    "@/category/hooks/useCategoryList": { useCategoryList: (_uid, type) => {
      requestedType = type;
      return { categories: [{ categoryUid: 3, name: "식비" }], isLoading: false, isError: false };
    } },
    "../hooks/useSaveBudget": { useSaveBudget: () => ({ save: async () => true, isLoading: false }) },
  }).default;
  const markup = renderToStaticMarkup(React.createElement(Form, {
    moneyBookUid: 7, year: 2026, month: 10, budget,
    onSaved: () => {}, onCancel: () => {},
  }));
  assert.equal(requestedType, "EXPENSE");
  assert.match(markup, /식비/);
  assert.match(markup, /value="50000"/);
});

test("month navigation uses query values and returns to the browser month", () => {
  const pushes = [];
  const { useMonthNavigation } = loadModule("transaction/hooks/useMonthNavigation.ts", {
    "next/navigation": {
      useRouter: () => ({ push: (url) => pushes.push(url) }),
      usePathname: () => "/books/7/calendar",
      useSearchParams: () => new URLSearchParams("year=2026&month=12"),
    },
    "../month": loadModule("transaction/month.ts"),
  });
  const navigation = useMonthNavigation();
  assert.deepEqual([navigation.year, navigation.month], [2026, 12]);
  navigation.moveMonth(1);
  assert.equal(pushes[0], "/books/7/calendar?year=2027&month=1");
  navigation.goToToday();
  const today = new Date();
  assert.equal(pushes[1], `/books/7/calendar?year=${today.getFullYear()}&month=${today.getMonth() + 1}`);
});

test("budget overview shows total, expense, overage, rate, and category", () => {
  const Progress = loadModule("budget/components/BudgetProgress.tsx").default;
  const Overview = loadModule("budget/components/BudgetOverview.tsx", {
    "@/common/format/money": { formatMoney }, "./BudgetProgress": { default: Progress },
  }).default;
  const markup = renderToStaticMarkup(React.createElement(Overview, { budget }));
  for (const expected of ["100,000원", "125,000원", "25,000원 초과", "125%", "예산 초과", "식비", "10,000원 초과"]) {
    assert.match(markup, new RegExp(expected));
  }
  assert.match(markup, /width:100%/);
});

test("budget view keeps setup control behind U permission", () => {
  function render(canUpdate) {
    const View = loadModule("budget/components/BudgetView.tsx", {
      "@/moneybook/hooks/useMoneyBookPermission": { useMoneyBookPermission: () => ({ canRead: true, canUpdate }) },
      "@/transaction/hooks/useMonthNavigation": { useMonthNavigation: () => ({ year: 2026, month: 10, moveMonth: () => {}, goToToday: () => {} }) },
      "@/transaction/components/MonthSelector": { default: () => null },
      "@/common/format/money": { formatMoney },
      "../hooks/useMonthlyBudget": { useMonthlyBudget: () => ({ budget: { ...budget, configured: false }, isLoading: false, isError: false }) },
      "./BudgetOverview": { default: () => null }, "./BudgetForm": { default: () => null },
    }).default;
    return renderToStaticMarkup(React.createElement(View, { moneyBookUid: 7 }));
  }
  assert.match(render(false), /예산이 아직 설정되지 않았습니다/);
  assert.doesNotMatch(render(false), /예산 설정<\/button>/);
  assert.match(render(true), /예산 설정<\/button>/);
});

test("dashboard shows monthly totals, budget summary, and quick links", () => {
  const Dashboard = loadModule("moneybook/components/MoneyBookDetail.tsx", {
    "next/link": { default: link }, "@/common/format/money": { formatMoney },
    "@/budget/components/BudgetProgress": { default: () => React.createElement("span", null, "125% 예산 초과") },
    "@/dashboard/hooks/useMonthlyDashboard": { useMonthlyDashboard: () => ({
      year: 2026, month: 10, isMonthReady: true, permission: { moneyBook: { name: "우리 집" }, canRead: true },
      calendar: { calendar: { days: [] }, totals: { income: 300000, expense: 125000, transferOut: 5000 } },
      budget: { budget }, activeDays: [],
    }) },
  }).default;
  const markup = renderToStaticMarkup(React.createElement(Dashboard, { moneyBookUid: 7 }));
  for (const expected of ["300,000원", "125,000원", "175,000원", "100,000원", "25,000원 초과"]) assert.match(markup, new RegExp(expected));
  assert.match(markup, /href="\/books\/7\/calendar\?year=2026&amp;month=10"/);
  assert.match(markup, /href="\/books\/7\/transactions\?year=2026&amp;month=10"/);
  assert.match(markup, /href="\/books\/7\/budgets\?year=2026&amp;month=10"/);
});
