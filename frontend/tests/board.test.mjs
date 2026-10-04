import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";

const read = (path) => readFile(new URL(path, import.meta.url), "utf8");

test("global navigation places the shared board between invitations and account", async () => {
  const source = await read("../src/common/components/globalNavigation.ts");
  assert.match(source, /\/books\/invitations[\s\S]*\/board[\s\S]*\/account/);
  assert.match(source, /role === "SYSTEM_ADMIN" \|\| role === "SUPER_ADMIN"/);
  const header = await read("../src/common/components/GlobalHeader.tsx");
  assert.match(header, /hover:bg-zinc-100 hover:text-blue-700/);
  assert.match(header, /focus-visible:ring-2/);
});

test("money book navigation keeps report tabs and removes the duplicate analysis section", async () => {
  const source = await read("../src/moneybook/components/MoneyBookNavigation.tsx");
  assert.match(source, /label: "리포트", href: root/);
  assert.doesNotMatch(source, /label: "분석", items/);
  assert.match(source, /월간 분석/);
  assert.match(source, /ml-5/);
  assert.match(source, /isMoneyBookRouteActive/);
  assert.match(source, /permission\.isOwner \|\| permission\.isAdmin/);
});

test("board frontend uses masked DTO display fields and secret placeholders", async () => {
  const api = await read("../src/board/controller/boardApi.ts");
  const view = await read("../src/board/components/BoardViews.tsx");
  assert.match(api, /authorDisplayName: string/);
  assert.match(view, /post\.secret \? "비밀글입니다\." : post\.title/);
  assert.match(view, /comment\.deleted \? "삭제된 댓글입니다\." : comment\.content/);
  assert.doesNotMatch(view, /comment\.secret \? "비밀 댓글입니다\."/);
  assert.match(view, /삭제된 댓글입니다\./);
  assert.match(view, /SUPER_ADMIN/);
  assert.match(view, /children\(comment\.commentUid\)/);
});
