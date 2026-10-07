import test from "node:test";
import assert from "node:assert/strict";
import fs from "node:fs";
import path from "node:path";
import vm from "node:vm";
import ts from "typescript";
import { fileURLToPath } from "node:url";

const sourceRoot = path.join(path.dirname(fileURLToPath(import.meta.url)), "../src");
function loadConfig() {
  const source = fs.readFileSync(path.join(sourceRoot, "i18n/config.ts"), "utf8");
  const compiled = ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2020 } }).outputText;
  const commonJsModule = { exports: {} };
  vm.runInNewContext(compiled, { module: commonJsModule, exports: commonJsModule.exports });
  return commonJsModule.exports;
}

test("locale config validates supported languages and preserves path, query, and hash", () => {
  const { supportedLocales, defaultLocale, isLocale, withLocale, replaceLocale, getLocaleFromPathname } = loadConfig();
  assert.deepEqual(Array.from(supportedLocales), ["ko", "en", "ja", "zh"]);
  assert.equal(defaultLocale, "ko");
  assert.equal(isLocale("fr"), false);
  assert.equal(getLocaleFromPathname("/ja/books/123/calendar"), "ja");
  assert.equal(withLocale("en", "/books/123?year=2026&month=10#today"), "/en/books/123?year=2026&month=10#today");
  assert.equal(replaceLocale("/ko/board?page=2", "zh"), "/zh/board?page=2");
});

test("all app pages are nested under the locale segment and APIs remain outside it", () => {
  const middleware = fs.readFileSync(path.join(sourceRoot, "middleware.ts"), "utf8");
  assert.ok(fs.existsSync(path.join(sourceRoot, "app/[locale]")));
  assert.match(fs.readFileSync(path.join(sourceRoot, "app/[locale]/layout.tsx"), "utf8"), /isLocale\(locale\)/);
  assert.match(middleware, /api\//);
  assert.match(middleware, /moneybook-locale/);
});
