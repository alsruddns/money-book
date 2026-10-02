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
    require: (name) => name in mocks ? mocks[name] : localRequire(name),
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

test("money book menu keeps readable pages visible without edit rights and avoids dead links", () => {
  const { getMoneyBookMenu, isMoneyBookRouteActive } = loadModule("moneybook/components/MoneyBookNavigation.tsx", {
    react: { useEffect: () => {}, useState: () => [false, () => {}] },
    "next/link": { default: link },
    "next/navigation": { usePathname: () => "/books/7/categories" },
    "@/common/components/advertisement/DesktopAdRail": { default: () => null },
    "../hooks/useMoneyBookPermission": { useMoneyBookPermission: () => ({}) },
  });
  const groups = getMoneyBookMenu(7, true);
  const items = groups.flatMap((group) => group.items);
  assert.equal(items.find((item) => item.label === "카테고리").disabled, undefined);
  assert.equal(items.find((item) => item.label === "캘린더").disabled, undefined);
  assert.equal(items.find((item) => item.label === "예산").disabled, undefined);
  assert.equal(items.find((item) => item.label === "정기 수입/지출").disabled, undefined);
  assert.equal(items.find((item) => item.label === "이체").disabled, undefined);
  assert.equal(items.find((item) => item.label === "활동내역").href, "/books/7/activities");
  assert.equal(getMoneyBookMenu(7, false).flatMap((group) => group.items).some((item) => item.label === "활동내역"), false);
  assert.equal(getMoneyBookMenu(7, false).flatMap((group) => group.items).some((item) => item.label === "카테고리"), false);
  assert.equal(isMoneyBookRouteActive("/books/7/categories/12", "/books/7/categories", "/books/7"), true);
  assert.equal(isMoneyBookRouteActive("/books/7/categories", "/books/7", "/books/7"), false);
  assert.equal(isMoneyBookRouteActive("/books/7/accounts", "/books/7/categories", "/books/7"), false);
  assert.equal(isMoneyBookRouteActive("/books/7/transfers/9", "/books/7/transfers", "/books/7"), true);
  assert.equal(isMoneyBookRouteActive("/books/7/recurring-transactions", "/books/7/recurring-transactions", "/books/7"), true);
});

test("money book sidebar renders name, role, active route, and disabled entries", () => {
  const book = { moneyBookUid: 7, name: "우리 집", isOwner: true, isAdmin: true };
  const { default: Navigation } = loadModule("moneybook/components/MoneyBookNavigation.tsx", {
    react: { useEffect: () => {}, useState: () => [false, () => {}] },
    "next/link": { default: ({ href, children, ...props }) => React.createElement("a", { href, ...props }, children) },
    "next/navigation": { usePathname: () => "/books/7/categories" },
    "@/common/components/advertisement/DesktopAdRail": { default: () => null },
    "../hooks/useMoneyBookPermission": { useMoneyBookPermission: () => ({ moneyBook: book, canRead: true }) },
  });
  const markup = renderToStaticMarkup(React.createElement(Navigation, { moneyBookUid: 7 }, React.createElement("p", null, "본문")));
  assert.match(markup, /우리 집/);
  assert.match(markup, /소유자/);
  assert.match(markup, /href="\/books\/7\/categories" aria-current="page"/);
  assert.match(markup, /href="\/books"/);
  assert.match(markup, /href="\/books\/7\/transfers"/);
  assert.match(markup, /href="\/books\/7\/budgets"/);
  assert.match(markup, /href="\/books\/7\/recurring-transactions"/);
  assert.match(markup, /md:grid-cols-\[15rem_minmax\(0,1fr\)\]/);
  assert.match(markup, /overflow-x-auto/);
  assert.doesNotMatch(markup, /aria-label="광고"/);
});

test("mobile drawer opens, closes from overlay and navigation, and handles Escape", () => {
  let open = false;
  let effect;
  let keyHandler;
  const documentMock = {
    body: { style: { overflow: "" } },
    addEventListener: (_name, handler) => { keyHandler = handler; },
    removeEventListener: () => {},
  };
  const { default: Navigation } = loadModule("moneybook/components/MoneyBookNavigation.tsx", {
    react: { useEffect: (callback) => { effect = callback; }, useState: () => [open, (value) => { open = value; }] },
    "next/link": { default: link },
    "next/navigation": { usePathname: () => "/books/7/transactions" },
    "@/common/components/advertisement/DesktopAdRail": { default: () => null },
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
  const cleanup = effect();
  assert.equal(documentMock.body.style.overflow, "hidden");
  keyHandler({ key: "Escape" });
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
