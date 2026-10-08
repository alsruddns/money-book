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
  assert.match(source, /label: label\("reports"\), href: root/);
  assert.doesNotMatch(source, /label: "분석", items/);
  assert.match(source, /reports\.monthly/);
  assert.match(source, /ml-5/);
  assert.match(source, /isMoneyBookRouteActive/);
  assert.match(source, /permission\.isOwner \|\| permission\.isAdmin/);
});

test("board frontend uses masked DTO display fields and secret placeholders", async () => {
  const api = await read("../src/board/controller/boardApi.ts");
  const view = await read("../src/board/components/BoardViews.tsx");
  assert.match(api, /authorDisplayName: string/);
  assert.match(api, /regTime: string/);
  assert.match(api, /replies: BoardComment\[\]/);
  assert.match(view, /post\.title/);
  assert.match(view, /comment\.content/);
  assert.match(view, /comment\.deleted \? "italic text-zinc-500"/);
  assert.doesNotMatch(view, /comment\.secret \? "비밀 댓글입니다\."/);
  assert.match(view, /comment\.replies\.map\(\(child\) => item\(child, true\)\)/);
  assert.match(view, /!nested && <button[^]*답글/);
  assert.doesNotMatch(view, /post\.createdAt|comment\.createdAt|post\.updatedAt/);
  assert.match(view, /SUPER_ADMIN/);
});

test("Board RTK endpoints match Backend paths, methods, pagination and narrow invalidation", async () => {
  const api = await read("../src/board/controller/boardApi.ts");
  assert.match(api, /board\/categories.*includeInactive: true/);
  assert.match(api, /method: "PATCH"/);
  assert.match(api, /board\/comments\/\$\{commentUid\}/);
  assert.match(api, /board\/posts\/\$\{uid\}\/notice/);
  assert.match(api, /method: "POST"/);
  assert.match(api, /method: "DELETE"/);
  assert.match(api, /board\/posts\/\$\{uid\}\/comments/);
  assert.match(api, /method: "PATCH", body/);
  assert.doesNotMatch(api, /admin\/board/);
  assert.match(api, /totalPages: number; number: number; totalElements: number/);
  assert.match(api, /\{ type: "BoardPost", id: uid \}/);
  assert.match(api, /invalidatesTags: \["BoardCategory"\]/);
  const view = await read("../src/board/components/BoardViews.tsx");
  assert.match(view, /query\.page \+ 1/);
  assert.match(view, /query\.page - 1/);
  assert.match(view, /role === "SUPER_ADMIN" && <Link[^]*board\/categories/);
  assert.match(view, /role !== "SUPER_ADMIN"/);
  assert.match(view, /getApiErrorMessage\(error/);
});

test("board list opens the reusable post detail dialog without navigating away", async () => {
  const view = await read("../src/board/components/BoardViews.tsx");
  const shell = await read("../src/common/components/DialogShell.tsx");
  assert.match(view, /onClick=\{\(\) => setSelectedPostUid\(post\.postUid\)\}/);
  assert.match(view, /selectedPostUid !== null && <DialogShell[\s\S]*size="wide"[\s\S]*<PostDetailView uid=\{selectedPostUid\}/);
  assert.match(view, /onDeleted=\{\(\) => \{ setSelectedPostUid\(null\);/);
  assert.match(view, /posts\.data\?\.content\.length === 1 && query\.page > 0/);
  assert.match(shell, /size\?: "default" \| "wide"/);
  assert.match(shell, /max-h-\[90dvh\][\s\S]*max-w-3xl/);
  assert.match(view, /onClose \? <button type="button" className=\{button\} onClick=\{onClose\}>/);
  assert.match(view, /post\.canDelete \|\| admin/);
  assert.match(view, /onDeleted\) onDeleted\(\); else router\.push\("\/board"\)/);
  assert.match(view, /status === 403 \?/);
  assert.match(view, /status === 404 \?/);
  assert.match(view, /comment\.replies\.map\(\(child\) => item\(child, true\)\)/);
  assert.match(view, /whitespace-pre-wrap break-words/);
  assert.match(view, /className=\{`block w-full p-4 text-left/);
  assert.match(view, /href=\{`\/board\/\$\{uid\}\/edit`\}/);
  assert.match(view, /router\.push\("\/board"\)/);
});
