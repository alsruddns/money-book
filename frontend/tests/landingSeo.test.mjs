import { resolveLocaleTestImport } from "./localeTestImports.mjs";
import test from "node:test";
import assert from "node:assert/strict";
import fs from "node:fs";
import path from "node:path";
import vm from "node:vm";
import ts from "typescript";
import { fileURLToPath } from "node:url";
import { createRequire } from "node:module";

const root = path.join(path.dirname(fileURLToPath(import.meta.url)), "../src");
const require = createRequire(import.meta.url);
const source = fs.readFileSync(path.join(root, "common/seo/siteMetadata.ts"), "utf8");
function loadSeo() {
  const js = ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2020 } }).outputText;
  const mod = { exports: {} };
  vm.runInNewContext(js, { module: mod, exports: mod.exports, process: { env: {} }, URL });
  return mod.exports;
}

test("MoneyBook uses the locale-first /money landing route", () => {
  const middleware = fs.readFileSync(path.join(root, "middleware.ts"), "utf8");
  const localized = fs.readFileSync(path.join(root, "app/[locale]/money/page.tsx"), "utf8");
  assert.match(middleware, /!isLocale\(locale\)/);
  assert.match(middleware, /section !== "money"/);
  assert.match(localized, /Free Shared Household Budget/);
  assert.match(localized, /Manage your household finances together/);
  assert.ok(localized.includes("/${locale}/money"));
});
test("landing CTA changes to the personal MoneyBook link for an authenticated user", () => {
  const source = fs.readFileSync(path.join(root, "landing/components/LandingActions.tsx"), "utf8");
  const js = ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2020, jsx: ts.JsxEmit.ReactJSX } }).outputText;
  const React = require("react");
  const renderToStaticMarkup = require("react-dom/server").renderToStaticMarkup;
  const Link = ({ href, children, ...props }) => React.createElement("a", { href, ...props }, children);

  function render(isAuthenticated, isLoading = false, component = "default") {
    const mod = { exports: {} };
    vm.runInNewContext(js, {
      module: mod,
      exports: mod.exports,
      require: (name) => name === "next/link" ? { default: Link } : name === "@/auth/hooks/useCurrentUser" ? { useCurrentUser: () => ({ isAuthenticated, isLoading }) } : resolveLocaleTestImport(name, {}, require),
    });
    return renderToStaticMarkup(React.createElement(mod.exports[component]));
  }

  const guestMarkup = render(false);
  assert.match(guestMarkup, /무료로 시작하기/);
  assert.match(guestMarkup, /href="\/ko\/money\/login"/);
  const memberMarkup = render(true);
  assert.match(memberMarkup, /내 가계부로 이동/);
  assert.match(memberMarkup, /href="\/ko\/money\/books"/);
  assert.doesNotMatch(memberMarkup, /무료로 시작하기|href="\/ko\/money\/login"/);
  assert.match(render(false, false, "LandingHeader"), /공개 메뉴/);
  assert.equal(render(false, true, "LandingHeader"), "");
  assert.equal(render(true, false, "LandingHeader"), "");
});

test("public metadata provides canonical, Open Graph, Twitter and optional verification values", () => {
  const seo = loadSeo();
  const metadata = seo.createPublicMetadata("/privacy", "개인정보 처리 안내", "설명", {
    NODE_ENV: "production",
    SITE_URL: "https://money.example",
    GOOGLE_SITE_VERIFICATION: "google-token",
    NAVER_SITE_VERIFICATION: "naver-token",
  });
  assert.equal(metadata.metadataBase.toString(), "https://money.example/");
  assert.equal(metadata.alternates.canonical, "https://money.example/privacy");
  assert.equal(metadata.openGraph.type, "website");
  assert.equal(metadata.openGraph.url, "https://money.example/privacy");
  assert.equal(metadata.openGraph.locale, "ko_KR");
  assert.equal(metadata.openGraph.images[0].url, "https://money.example/moneybook-og.png");
  assert.equal(metadata.twitter.card, "summary_large_image");
  assert.equal(metadata.twitter.images[0], "https://money.example/moneybook-og.png");
  assert.deepEqual(JSON.parse(JSON.stringify(metadata.verification)), {
    google: "google-token",
    other: { "naver-site-verification": "naver-token" },
  });
});

test("production defaults to the configured public domain and locale paths", () => {
  const seo = loadSeo();
  const env = { NODE_ENV: "production" };
  const metadata = seo.createPublicMetadata("/", "MoneyBook", "설명", env);
  assert.equal(metadata.metadataBase.toString(), "https://www.woori.today/");
  assert.equal(metadata.alternates.canonical, "https://www.woori.today/");
  assert.equal(metadata.openGraph.url, "https://www.woori.today/");
  const sitemapUrls = JSON.parse(JSON.stringify(seo.buildPublicSitemap(env).map((item) => item.url)));
  assert.ok(sitemapUrls.every((url) => /^\/(ko|en|ja|zh)(\/|$)/.test(new URL(url).pathname)));
  assert.ok(sitemapUrls.every((url) => url !== "https://money.example/"));
  const privatePaths = ["/login", "/signup", "/account", "/admin", "/board", "/books"];
  assert.ok(sitemapUrls.every((url) => privatePaths.every((path) => !new URL(url).pathname.startsWith(`/money${path}`))));
  assert.deepEqual(sitemapUrls, ["", "/privacy", "/terms"].flatMap((suffix) => ["ko", "en", "ja", "zh"].map((locale) => `https://www.woori.today/${locale}/money${suffix}`)));
  assert.equal(seo.buildWebApplicationJsonLd(env).url, "https://www.woori.today/ko/money");
});

test("local development uses localhost only as a development canonical base", () => {
  const seo = loadSeo();
  assert.equal(seo.getSiteUrl({ NODE_ENV: "development" }).toString(), "http://localhost:3000/");
  assert.equal(seo.createPublicMetadata("/terms", "이용 안내", "설명", { NODE_ENV: "development" }).alternates.canonical, "http://localhost:3000/terms");
});

test("robots blocks private routes and sitemap includes only public pages", () => {
  const seo = loadSeo();
  const env = { NODE_ENV: "production", SITE_URL: "https://money.example" };
  const robots = seo.buildRobots(env);
  const disallow = robots.rules.disallow;
  assert.equal(disallow[0], "/api/");
  assert.ok(disallow.includes("/en/money/admin/"));
  assert.equal(robots.sitemap, "https://money.example/money-sitemap.xml");
  assert.deepEqual(JSON.parse(JSON.stringify(seo.buildPublicSitemap(env).map((item) => item.url))), ["", "/privacy", "/terms"].flatMap((suffix) => ["ko", "en", "ja", "zh"].map((locale) => `https://money.example/${locale}/money${suffix}`)));
});

test("private page metadata is noindex and each private route applies the shared policy", () => {
  const seo = loadSeo();
  assert.deepEqual(JSON.parse(JSON.stringify(seo.privatePageMetadata().robots)), {
    index: false,
    follow: false,
    googleBot: { index: false, follow: false },
  });
  for (const relativePath of ["app/[locale]/money/login/page.tsx", "app/[locale]/money/signup/page.tsx", "app/[locale]/money/board/layout.tsx", "app/[locale]/money/books/layout.tsx", "app/[locale]/money/account/layout.tsx", "app/[locale]/money/admin/layout.tsx"]) {
    assert.match(fs.readFileSync(path.join(root, relativePath), "utf8"), /privatePageMetadata/);
  }
});

test("privacy and terms routes have real content, metadata and navigation back home", () => {
  for (const route of ["privacy", "terms"]) {
    const page = fs.readFileSync(path.join(root, `app/[locale]/money/${route}/page.tsx`), "utf8");
    assert.match(page, /createLocalizedMetadata/);
    assert.match(page, /<h1\b/);
    assert.match(page, /href=\{`\/\$\{raw\}`\}/);
    assert.doesNotMatch(page, /href="#"/);
  }
});

test("JSON-LD is a minimal WebApplication schema without invented prices or reviews", () => {
  const app = loadSeo().buildWebApplicationJsonLd({ NODE_ENV: "production", SITE_URL: "https://money.example" });
  assert.equal(app["@context"], "https://schema.org");
  assert.equal(app["@type"], "WebApplication");
  assert.equal(app.name, "\uBB34\uB8CC \uACF5\uC720 \uAC00\uACC4\uBD80");
  assert.equal(app.operatingSystem, "Web");
    assert.equal(app.url, "https://money.example/ko/money");
    assert.equal(app.inLanguage, "ko");
  assert.equal(app.offers, undefined);
  assert.equal(app.aggregateRating, undefined);
});
