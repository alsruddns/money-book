import { resolveLocaleTestImport } from "./localeTestImports.mjs";
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
const testDirectory = path.dirname(fileURLToPath(import.meta.url));

function loadModule(relativePath, mocks = {}, globals = {}) {
  const source = fs.readFileSync(path.join(testDirectory, "../src", relativePath), "utf8");
  const compiled = ts.transpileModule(source, {
    compilerOptions: {
      module: ts.ModuleKind.CommonJS,
      target: ts.ScriptTarget.ES2020,
      jsx: ts.JsxEmit.ReactJSX,
    },
  }).outputText;
  const compiledModule = { exports: {} };
  vm.runInNewContext(compiled, {
    module: compiledModule,
    exports: compiledModule.exports,
    require: (name) => name in mocks ? mocks[name] : name === "@/i18n/messages" ? { translate: (_locale, key) => ({ "navigation.dashboard": "대시보드", "navigation.reports": "리포트", "navigation.book": "가계부", "navigation.calendar": "캘린더", "navigation.transactions": "거래내역", "navigation.transfers": "이체", "navigation.recurring": "정기거래", "navigation.budget": "예산", "navigation.categories": "카테고리", "navigation.accounts": "계좌 / 결제수단", "navigation.activity": "활동내역", "navigation.closing": "월 결산", "navigation.settings": "가계부 설정", "navigation.members": "멤버 관리", "navigation.manage": "관리" })[key] ?? key } : name === "@/i18n/useTranslation" ? { useTranslation: () => ({ locale: "ko", t: (key) => ({ "books.owner": "소유자", "books.administrator": "관리자", "books.backToList": "내 가계부 목록으로", "books.accessChecking": "가계부 접근 권한을 확인하는 중...", "books.accessMissing": "접근 가능한 가계부를 찾을 수 없습니다.", "books.accessDenied": "가계부 조회 권한이 없습니다.", "common.retry": "다시 시도", "navigation.book": "가계부", "navigation.dashboard": "대시보드", "navigation.openMenu": "가계부 메뉴 열기", "navigation.menu": "메뉴", "navigation.mobileMenu": "가계부 메뉴", "common.close": "닫기" })[key] ?? key }) } : name === "@/i18n/config" ? {} : name === "@/common/format/money" ? { formatNumber: (value) => Number(value).toLocaleString("ko-KR"), formatMoney: (value) => `${Number(value).toLocaleString("ko-KR")}${String.fromCharCode(0xC6D0)}`, formatCurrency: (value) => `${Number(value).toLocaleString("ko-KR")}${String.fromCharCode(0xC6D0)}`, formatCount: (value, unit = String.fromCharCode(0xAC74)) => `${Number(value).toLocaleString("ko-KR")}${unit}` } : resolveLocaleTestImport(name, mocks, localRequire),
    ...globals,
  });
  return compiledModule.exports;
}

function asLocal(value) {
  return JSON.parse(JSON.stringify(value));
}

const link = ({ href, children }) => React.createElement("a", { href }, children);

test("money book API uses backend paths and invalidates only affected cache tags", () => {
  const builder = {
    query: (definition) => definition,
    mutation: (definition) => definition,
  };
  const { moneyBookApi: api } = loadModule("moneybook/controller/moneyBookApi.ts", {
    "@/common/api/baseApi": {
      baseApi: { injectEndpoints: ({ endpoints }) => endpoints(builder) },
    },
  });
  const key = { moneyBookUid: 7, moneyBookUserUid: 9 };
  assert.equal(api.getMoneyBooks.query(), "money-books");
  assert.equal(api.createMoneyBook.query({ name: "집" }).url, "money-books");
  assert.equal(api.getPendingInvitations.query(), "money-books/invitations");
  assert.equal(api.inviteMoneyBookUser.query({ moneyBookUid: 7, request: {} }).url, "money-books/7/invitations");
  assert.equal(api.acceptInvitation.query(key).url, "money-books/7/invitations/9/accept");
  assert.equal(api.rejectInvitation.query(key).url, "money-books/7/invitations/9/reject");
  assert.equal(api.getMoneyBookMembers.query(7), "money-books/7/members");
  assert.equal(api.updateMoneyBookMemberPermission.query({ ...key, request: {} }).url, "money-books/7/members/9/permissions");
  assert.equal(api.removeMoneyBookMember.query(key).url, "money-books/7/members/9");
  assert.deepEqual(asLocal(api.createMoneyBook.invalidatesTags({}, undefined)), ["MoneyBook", "MoneyBookActivity"]);
  assert.deepEqual(asLocal(api.acceptInvitation.invalidatesTags({}, undefined)), ["MoneyBookInvitation", "MoneyBook", "MoneyBookActivity"]);
  assert.deepEqual(asLocal(api.rejectInvitation.invalidatesTags({}, undefined)), ["MoneyBookInvitation", "MoneyBookActivity"]);
  assert.deepEqual(asLocal(api.getMoneyBookMembers.providesTags([], undefined, 7)), [{ type: "MoneyBookMember", id: 7 }]);
  assert.deepEqual(asLocal(api.updateMoneyBookMemberPermission.invalidatesTags(undefined, undefined, key)), [{ type: "MoneyBookMember", id: 7 }, "MoneyBook", { type: "MoneyBookActivity", id: 7 }]);
  assert.deepEqual(asLocal(api.removeMoneyBookMember.invalidatesTags(undefined, undefined, key)), [{ type: "MoneyBookMember", id: 7 }, "MoneyBook", { type: "MoneyBookActivity", id: 7 }]);
  assert.deepEqual(asLocal(api.transferMoneyBookOwner.query({ moneyBookUid: 7, request: { targetUserUid: 9 } })), {
    url: "money-books/7/owner", method: "PATCH", body: { targetUserUid: 9 },
  });
  assert.deepEqual(asLocal(api.transferMoneyBookOwner.invalidatesTags({}, undefined, { moneyBookUid: 7, request: { targetUserUid: 9 } })), [
    "MoneyBook", { type: "MoneyBookMember", id: 7 }, { type: "MoneyBookActivity", id: 7 },
  ]);
  assert.deepEqual(asLocal(api.transferMoneyBookOwner.invalidatesTags(undefined, { status: 403 }, { moneyBookUid: 7, request: { targetUserUid: 9 } })), []);
  assert.deepEqual(asLocal(api.createMoneyBook.invalidatesTags(undefined, { status: 400 })), []);
});

test("owner transfer hook confirms selected member and sends only targetUserUid", async () => {
  const calls = [];
  const confirmations = [];
  const state = { error: null };
  const { useTransferMoneyBookOwner } = loadModule("moneybook/hooks/useTransferMoneyBookOwner.ts", {
    react: { useState: (initial) => [initial, (next) => { state.error = next; }] },
    "@/common/api/getApiErrorMessage": { getApiErrorMessage: () => "이전 실패" },
    "../controller/moneyBookApi": { useTransferMoneyBookOwnerMutation: () => [(arg) => ({ unwrap: async () => { calls.push(arg); } }), { isLoading: false }] },
  }, { window: { confirm: (message) => { confirmations.push(message); return true; } } });
  const hook = useTransferMoneyBookOwner(7);
  assert.equal(await hook.transferOwner(9, "홍길동"), true);
  assert.deepEqual(asLocal(calls), [{ moneyBookUid: 7, request: { targetUserUid: 9 } }]);
  assert.match(confirmations[0], /홍길동님/);
  assert.match(confirmations[0], /현재 계정은 멤버로 남습니다/);
});

test("book UID parser rejects missing, malformed, and unsafe values", () => {
  const { parseMoneyBookUid } = loadModule("moneybook/parseMoneyBookUid.ts");
  assert.equal(parseMoneyBookUid("42"), 42);
  for (const value of ["", "0", "-1", "1.2", "1e2", "abc", "9007199254740992"]) {
    assert.equal(parseMoneyBookUid(value), null);
  }
});

test("money book menu groups routes and limits management to readable admins", () => {
  const { getMoneyBookMenu, getDashboardReportTabs, isMoneyBookRouteActive } = loadModule("moneybook/components/MoneyBookNavigation.tsx", {
    react: { useEffect: () => {}, useState: () => [false, () => {}] },
    "next/link": { default: link }, "next/navigation": { usePathname: () => "/books/7/categories", useRouter: () => ({ replace: () => {} }) },
    "@/common/components/advertisement/DesktopAdRail": { default: () => null },
    "@/common/api/getApiErrorMessage": { getApiErrorMessage: () => "error" },
    "@/settings/controller/moneyBookSettingApi": { useGetMoneyBookSettingQuery: () => ({ isLoading: false, isError: false, currentData: {} }) },
    "../hooks/useMoneyBookPermission": { useMoneyBookPermission: () => ({}) },
  });
  const ownerGroups = getMoneyBookMenu(7, { canRead: true, isOwner: true, isAdmin: false });
  assert.equal(ownerGroups.length, 4);
  assert.equal(ownerGroups.some((group) => group.label === "분석"), false);
  assert.equal(ownerGroups[0].items[0].label, "리포트");
  const ownerItems = ownerGroups.flatMap((group) => group.items);
  assert.ok(ownerItems.some((item) => item.href === "/books/7/activities"));
  assert.ok(ownerItems.some((item) => item.href === "/books/7/members"));
  assert.ok(ownerItems.some((item) => item.href === "/books/7/recurring-transactions"));
  const readonlyGroups = getMoneyBookMenu(7, { canRead: true, isOwner: false, isAdmin: false });
  assert.equal(readonlyGroups.flatMap((group) => group.items).some((item) => item.href.endsWith("/members")), false);
  assert.equal(getMoneyBookMenu(7, { canRead: false, isOwner: false, isAdmin: false }).some((group) => group.items.some((item) => item.href.endsWith("/members"))), false);
  assert.equal(isMoneyBookRouteActive("/books/7/categories/12", "/books/7/categories", "/books/7"), true);
  assert.equal(isMoneyBookRouteActive("/books/7/categories", "/books/7", "/books/7"), false);
  assert.deepEqual(JSON.parse(JSON.stringify(getDashboardReportTabs(7, "/books/7/reports/monthly").map((tab) => tab.active))), [false, true, false, false]);
  assert.deepEqual(JSON.parse(JSON.stringify(getDashboardReportTabs(7, "/books/7/reports/yearly").map((tab) => tab.active))), [false, false, true, false]);
  assert.deepEqual(JSON.parse(JSON.stringify(getDashboardReportTabs(7, "/books/7/reports/expense-ranking").map((tab) => tab.active))), [false, false, false, true]);
  assert.deepEqual(JSON.parse(JSON.stringify(getDashboardReportTabs(7, "/books/7/calendar"))), []);
});

test("money book layout keeps shared navigation and content shell", () => {
  const { default: Navigation } = loadModule("moneybook/components/MoneyBookNavigation.tsx", {
    react: React,
    "next/link": { default: ({ href, children, ...props }) => React.createElement("a", { href, ...props }, children) },
    "next/navigation": { usePathname: () => "/books/7/categories", useRouter: () => ({ replace: () => {} }) },
    "@/common/components/advertisement/DesktopAdRail": { default: () => null },
    "@/common/api/getApiErrorMessage": { getApiErrorMessage: () => "error" },
    "@/settings/controller/moneyBookSettingApi": { useGetMoneyBookSettingQuery: () => ({ isLoading: false, isError: false, currentData: {} }) },
    "../hooks/useMoneyBookPermission": { useMoneyBookPermission: () => ({ moneyBook: { moneyBookUid: 7, name: "Book", isOwner: true, isAdmin: true }, isLoading: false, isError: false, canRead: true, isOwner: true, isAdmin: true }) },
  });
  const markup = renderToStaticMarkup(React.createElement(Navigation, { moneyBookUid: 7 }, React.createElement("p", null, "body")));
  assert.match(markup, /href="\/ko\/money\/books\/7\/categories" aria-current="page"/);
  assert.match(markup, /리포트/);
  assert.doesNotMatch(markup, /aria-label="가계부 기능"[^]*분석/);
  assert.match(markup, /body/);
  assert.match(markup, /md:grid-cols-\[15rem_minmax\(0,1fr\)\]/);
});

test("MoneyBook access guard redirects forbidden and missing resources without trapping loading", () => {
  function renderAccessState(accessState, permissionState) {
    const effects = [];
    const routes = [];
    const { default: Navigation } = loadModule("moneybook/components/MoneyBookNavigation.tsx", {
      react: {
        useEffect: (callback) => effects.push(callback),
        useRef: (current) => ({ current }),
        useState: (initial) => [initial, () => {}],
      },
      "next/link": { default: link },
      "next/navigation": { usePathname: () => "/books/7", useRouter: () => ({ replace: (route) => routes.push(route) }) },
      "@/common/components/advertisement/DesktopAdRail": { default: () => null },
      "@/common/api/getApiErrorMessage": { getApiErrorMessage: () => "접근 오류" },
      "@/settings/controller/moneyBookSettingApi": { useGetMoneyBookSettingQuery: () => accessState },
      "../hooks/useMoneyBookPermission": { useMoneyBookPermission: () => permissionState },
    });
    const markup = renderToStaticMarkup(React.createElement(Navigation, { moneyBookUid: 7 }, React.createElement("p", null, "authorized content")));
    effects[1]?.();
    return { markup, routes };
  }

  const permission = { moneyBook: null, isLoading: false, isError: false, canRead: false, isOwner: false, isAdmin: false };
  const forbidden = renderAccessState({ isLoading: false, isError: true, error: { status: 403 } }, permission);
  assert.deepEqual(forbidden.routes, ["/ko/money/forbidden"]);
  assert.match(forbidden.markup, /접근 권한을 확인하는 중/);

  const missing = renderAccessState({ isLoading: false, isError: true, error: { status: 404 } }, permission);
  assert.deepEqual(missing.routes, ["/ko/money/not-found"]);

  const authorized = renderAccessState(
    { isLoading: false, isFetching: true, isError: false, currentData: { moneyBookUid: 7 } },
    { moneyBook: { moneyBookUid: 7, name: "Book", isOwner: true, isAdmin: true }, isLoading: false, isError: false, canRead: true, isOwner: true, isAdmin: true },
  );
  assert.deepEqual(authorized.routes, []);
  assert.match(authorized.markup, /authorized content/);
  assert.doesNotMatch(authorized.markup, /접근 권한을 확인하는 중/);
});

test("mobile drawer opens, closes from overlay and navigation, and handles Escape", () => {
  let open = false;
  const effects = [];
  let keyHandler;
  const documentMock = {
    body: { style: { overflow: "" } },
    addEventListener: (_name, handler) => { keyHandler = handler; },
    removeEventListener: () => {},
  };
  const { default: Navigation } = loadModule("moneybook/components/MoneyBookNavigation.tsx", {
    react: { useEffect: (callback) => { effects.push(callback); }, useRef: (current) => ({ current }), useState: () => [open, (value) => { open = value; }] },
    "next/link": { default: link },
    "next/navigation": { usePathname: () => "/books/7/transactions", useRouter: () => ({ replace: () => {} }) },
    "@/common/components/advertisement/DesktopAdRail": { default: () => null },
    "@/common/api/getApiErrorMessage": { getApiErrorMessage: () => "error" },
    "@/settings/controller/moneyBookSettingApi": { useGetMoneyBookSettingQuery: () => ({ isLoading: false, isError: false, currentData: {} }) },
    "../hooks/useMoneyBookPermission": { useMoneyBookPermission: () => ({
      moneyBook: { moneyBookUid: 7, name: "집", isOwner: false, isAdmin: true }, canRead: true,
    }) },
  }, { document: documentMock });
  const navigationProps = { moneyBookUid: 7, children: null };
  const render = () => Navigation(navigationProps);
  const children = (element) => React.Children.toArray(element.props.children);
  let page = render();
  const header = children(page)[0];
  const menuButton = children(header)[0];
  assert.equal(menuButton.props["aria-expanded"], false);
  menuButton.props.onClick();
  page = render();
  assert.equal(children(page).length, 3);
  const drawer = children(page)[2];
  const overlay = children(drawer)[0];
  overlay.props.onClick();
  assert.equal(open, false);

  menuButton.props.onClick();
  page = render();
  const mobileAside = children(children(page)[2])[1];
  const sidebar = children(mobileAside)[1];
  sidebar.props.onNavigate();
  assert.equal(open, false);

  menuButton.props.onClick();
  render();
  const cleanup = effects.at(-2)();
  assert.equal(documentMock.body.style.overflow, "hidden");
  keyHandler({ key: "Escape", preventDefault: () => {} });
  assert.equal(open, false);
  cleanup();
  assert.equal(documentMock.body.style.overflow, "");
});

test("admin permission forces create, read, update, and delete", () => {
  const { normalizePermissions, permissionSummary } = loadModule("moneybook/permissions.ts");
  const requested = { isAdmin: true, canCreate: false, canRead: false, canUpdate: false, canDelete: false };
  const normalized = normalizePermissions(requested);
  assert.deepEqual(asLocal(normalized), {
    isAdmin: true, canCreate: true, canRead: true, canUpdate: true, canDelete: true,
  });
  assert.deepEqual(asLocal(permissionSummary(normalized)), ["관리자", "생성", "조회", "수정", "삭제"]);
  const { default: PermissionFields } = loadModule("moneybook/components/PermissionFields.tsx", {
    react: React,
    "../permissions": { normalizePermissions, permissionLabels: loadModule("moneybook/permissions.ts").permissionLabels },
  });
  const markup = renderToStaticMarkup(React.createElement(PermissionFields, {
    value: normalized, onChange: () => {}, idPrefix: "test",
  }));
  assert.equal((markup.match(/disabled=""/g) ?? []).length, 4);
  assert.equal((markup.match(/checked=""/g) ?? []).length, 5);
});

test("money book list presents loading, error, empty, and populated states", () => {
  const baseMocks = {
    react: React,
    "@/i18n/useTranslation": { useTranslation: () => ({ locale: "ko", t: (key) => ({ "books.title": "가계부", "books.description": "참여 중인 가계부를 선택하세요.", "books.create": "새 가계부 만들기", "books.loading": "가계부를 불러오는 중...", "books.empty": "아직 참여 중인 가계부가 없습니다.", "books.emptyHint": "새 가계부를 만들어 시작해 보세요." })[key] ?? key }) },
    "next/link": { default: link },
    "../hooks/usePendingInvitations": { usePendingInvitations: () => ({ invitations: [] }) },
    "./MoneyBookCard": { default: ({ moneyBook }) => React.createElement("div", null, moneyBook.name) },
    "./CreateMoneyBookDialog": { default: () => null },
  };
  function render(state) {
    const { default: List } = loadModule("moneybook/components/MoneyBookList.tsx", {
      ...baseMocks,
      "../hooks/useMoneyBookList": { useMoneyBookList: () => state },
    });
    return renderToStaticMarkup(React.createElement(List));
  }
  assert.match(render({ moneyBooks: [], isLoading: true }), /불러오는 중/);
  assert.match(render({ moneyBooks: [], isLoading: false, isError: true, errorMessage: "오류" }), /오류/);
  assert.match(render({ moneyBooks: [], isLoading: false, isError: false }), /아직 참여 중인 가계부가 없습니다/);
  assert.match(render({ moneyBooks: [{ moneyBookUid: 1, name: "우리 집" }], isLoading: false, isError: false }), /우리 집/);
  const populatedMarkup = render({ moneyBooks: [{ moneyBookUid: 1, name: "Book" }], isLoading: false, isError: false });
  assert.equal((populatedMarkup.match(/<button/g) ?? []).length, 1);
  assert.doesNotMatch(populatedMarkup, /href="\/books\/invitations"/);
});

test("invitation list presents pending invitations and an empty state", () => {
  const mocks = {
    "next/link": { default: link },
    "./InvitationCard": { default: ({ invitation }) => React.createElement("article", null, invitation.moneyBookName) },
  };
  const empty = loadModule("moneybook/components/InvitationList.tsx", {
    ...mocks,
    "../hooks/usePendingInvitations": { usePendingInvitations: () => ({ invitations: [], isLoading: false, isError: false }) },
  }).default;
  assert.match(renderToStaticMarkup(React.createElement(empty)), /대기 중인 초대가 없습니다/);
  const populated = loadModule("moneybook/components/InvitationList.tsx", {
    ...mocks,
    "../hooks/usePendingInvitations": { usePendingInvitations: () => ({ invitations: [{ moneyBookUserUid: 2, moneyBookName: "여행" }], isLoading: false, isError: false }) },
  }).default;
  assert.match(renderToStaticMarkup(React.createElement(populated)), /여행/);
});

test("owner and ordinary users never receive owner edit or removal controls", () => {
  const { default: MemberRow } = loadModule("moneybook/components/MemberRow.tsx", {
    react: React,
    "../hooks/useRemoveMoneyBookMember": { useRemoveMoneyBookMember: () => ({ isLoading: false, errorMessage: null }) },
    "./PermissionBadges": { default: () => null },
    "./MemberPermissionDialog": { default: () => null },
  });
  const member = { moneyBookUserUid: 3, nickname: "테스터", isOwner: true };
  const ownerMarkup = renderToStaticMarkup(React.createElement(MemberRow, { member, moneyBookUid: 1, canManage: true }));
  assert.match(ownerMarkup, /소유자/);
  assert.doesNotMatch(ownerMarkup, /권한 변경|제거/);
  const ordinaryMarkup = renderToStaticMarkup(React.createElement(MemberRow, { member: { ...member, isOwner: false }, moneyBookUid: 1, canManage: false }));
  assert.doesNotMatch(ordinaryMarkup, /권한 변경|제거/);
  const managerMarkup = renderToStaticMarkup(React.createElement(MemberRow, { member: { ...member, isOwner: false }, moneyBookUid: 1, canManage: true }));
  assert.match(managerMarkup, /권한 변경/);
  assert.match(managerMarkup, /제거/);
});

test("member list shows management only to owner or admin, with loading and empty states", () => {
  const member = { moneyBookUserUid: 2, nickname: "멤버" };
  function render(book, memberState = { members: [member], isLoading: false, isError: false }) {
    const { default: MemberList } = loadModule("moneybook/components/MemberList.tsx", {
      react: React,
      "next/link": { default: link },
      "../hooks/useMoneyBookDetail": { useMoneyBookDetail: () => ({ moneyBook: book, isLoading: false, isError: false }) },
      "../hooks/useMoneyBookMembers": { useMoneyBookMembers: () => memberState },
      "../hooks/useTransferMoneyBookOwner": { useTransferMoneyBookOwner: () => ({ isLoading: false, errorMessage: null, transferOwner: async () => true }) },
      "./InviteMemberDialog": { default: () => null },
      "./MemberRow": { default: ({ member: row }) => React.createElement("div", null, row.nickname) },
    });
    return renderToStaticMarkup(React.createElement(MemberList, { moneyBookUid: 1 }));
  }
  const book = { moneyBookUid: 1, name: "집", isOwner: false, isAdmin: false };
  assert.doesNotMatch(render(book), /사용자 초대/);
  assert.match(render({ ...book, isOwner: true }), /사용자 초대/);
  const ownerMarkup = render({ ...book, isOwner: true }, { members: [
    { userUid: 1, nickname: "현재 소유자", isOwner: true },
    { userUid: 2, nickname: "이전 후보", isOwner: false },
  ], isLoading: false, isError: false });
  assert.match(ownerMarkup, /소유권 이전/);
  assert.match(ownerMarkup, /<option value="2">이전 후보/);
  assert.doesNotMatch(ownerMarkup, /<option value="1">현재 소유자/);
  assert.doesNotMatch(render({ ...book, isAdmin: true }), /소유권 이전/);
  assert.match(render({ ...book, isAdmin: true }), /사용자 초대/);
  assert.match(render(book, { members: [], isLoading: true }), /멤버를 불러오는 중/);
  assert.match(render(book, { members: [], isLoading: false, isError: false }), /가입한 멤버가 없습니다/);
});

test("create and invitation hooks pass exact mutation arguments on success", async () => {
  const calls = [];
  const mutation = (response) => (argument) => ({
    unwrap: async () => { calls.push(argument); return response; },
  });
  const reactMock = { useState: (initial) => [initial, () => {}] };
  const { useCreateMoneyBook } = loadModule("moneybook/hooks/useCreateMoneyBook.ts", {
    react: reactMock,
    "@/common/api/getApiErrorMessage": { getApiErrorMessage: () => "오류" },
    "../controller/moneyBookApi": { useCreateMoneyBookMutation: () => [mutation({ moneyBookUid: 1 }), { isLoading: false }] },
  });
  assert.equal(await useCreateMoneyBook().createMoneyBook("  우리 집  "), true);
  assert.deepEqual(asLocal(calls.shift()), { name: "우리 집" });

  const { normalizePermissions } = loadModule("moneybook/permissions.ts");
  const { useInviteMoneyBookUser } = loadModule("moneybook/hooks/useInviteMoneyBookUser.ts", {
    react: reactMock,
    "@/common/api/getApiErrorMessage": { getApiErrorMessage: () => "오류" },
    "../permissions": { normalizePermissions },
    "../controller/moneyBookApi": { useInviteMoneyBookUserMutation: () => [mutation({ moneyBookUserUid: 2 }), { isLoading: false }] },
  });
  const request = { loginId: "  friend  ", isAdmin: true, canCreate: false, canRead: false, canUpdate: false, canDelete: false };
  assert.equal(await useInviteMoneyBookUser(1).inviteMoneyBookUser(request), true);
  assert.deepEqual(asLocal(calls.shift()), {
    moneyBookUid: 1,
    request: { loginId: "friend", isAdmin: true, canCreate: true, canRead: true, canUpdate: true, canDelete: true },
  });

  const { useAcceptInvitation } = loadModule("moneybook/hooks/useAcceptInvitation.ts", {
    react: reactMock,
    "@/common/api/getApiErrorMessage": { getApiErrorMessage: () => "오류" },
    "../controller/moneyBookApi": { useAcceptInvitationMutation: () => [mutation({}), { isLoading: false }] },
  });
  await useAcceptInvitation().acceptInvitation(1, 2);
  assert.deepEqual(asLocal(calls.shift()), { moneyBookUid: 1, moneyBookUserUid: 2 });

  const { useRejectInvitation } = loadModule("moneybook/hooks/useRejectInvitation.ts", {
    react: reactMock,
    "@/common/api/getApiErrorMessage": { getApiErrorMessage: () => "오류" },
    "../controller/moneyBookApi": { useRejectInvitationMutation: () => [mutation({}), { isLoading: false }] },
  });
  await useRejectInvitation().rejectInvitation(1, 2);
  assert.deepEqual(asLocal(calls.shift()), { moneyBookUid: 1, moneyBookUserUid: 2 });
});

test("member permission hook normalizes admin rights and removal requires confirmation", async () => {
  const calls = [];
  const mutation = (argument) => ({ unwrap: async () => { calls.push(argument); } });
  const reactMock = { useState: (initial) => [initial, () => {}] };
  const { normalizePermissions } = loadModule("moneybook/permissions.ts");
  const { useUpdateMemberPermission } = loadModule("moneybook/hooks/useUpdateMemberPermission.ts", {
    react: reactMock,
    "@/common/api/getApiErrorMessage": { getApiErrorMessage: () => "오류" },
    "../permissions": { normalizePermissions },
    "../controller/moneyBookApi": { useUpdateMoneyBookMemberPermissionMutation: () => [mutation, { isLoading: false }] },
  });
  await useUpdateMemberPermission(1, 2).updateMemberPermission({
    isAdmin: true, canCreate: false, canRead: false, canUpdate: false, canDelete: false,
  });
  assert.deepEqual(asLocal(calls.shift()), {
    moneyBookUid: 1, moneyBookUserUid: 2,
    request: { isAdmin: true, canCreate: true, canRead: true, canUpdate: true, canDelete: true },
  });

  function removeHook(confirmed) {
    return loadModule("moneybook/hooks/useRemoveMoneyBookMember.ts", {
      react: reactMock,
      "@/common/api/getApiErrorMessage": { getApiErrorMessage: () => "오류" },
      "../controller/moneyBookApi": { useRemoveMoneyBookMemberMutation: () => [mutation, { isLoading: false }] },
    }, { window: { confirm: () => confirmed } }).useRemoveMoneyBookMember(1);
  }
  await removeHook(false).removeMoneyBookMember(2, "멤버");
  assert.equal(calls.length, 0);
  await removeHook(true).removeMoneyBookMember(2, "멤버");
  assert.deepEqual(asLocal(calls.shift()), { moneyBookUid: 1, moneyBookUserUid: 2 });
});
