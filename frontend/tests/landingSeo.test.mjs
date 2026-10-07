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

test("public root renders a server-readable landing page with one H1 and working CTAs", () => {
  const page = fs.readFileSync(path.join(root, "app/page.tsx"), "utf8");
  const controls = fs.readFileSync(path.join(root, "landing/components/LandingActions.tsx"), "utf8");
  assert.doesNotMatch(page, /redirect\(/);
  assert.doesNotMatch(page, /^\s*"use client"/);
  assert.equal((page.match(/<h1\b/g) ?? []).length, 1);
  assert.match(page, /공유 가계부/);
  assert.match(page, /캘린더/);
  assert.match(page, /예산/);
  assert.match(controls, /href="\/signup"/);
  assert.match(controls, /href="\/login"/);
  assert.match(controls, /href="\/books"/);
  assert.match(page, /href="\/privacy"/);
  assert.match(page, /href="\/terms"/);
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
      require: (name) => name === "next/link" ? { default: Link } : name === "@/auth/hooks/useCurrentUser" ? { useCurrentUser: () => ({ isAuthenticated, isLoading }) } : require(name),
    });
    return renderToStaticMarkup(React.createElement(mod.exports[component]));
  }

  const guestMarkup = render(false);
  assert.match(guestMarkup, /무료로 시작하기/);
  assert.match(guestMarkup, /href="\/login"/);
  const memberMarkup = render(true);
  assert.match(memberMarkup, /내 가계부로 이동/);
  assert.match(memberMarkup, /href="\/books"/);
  assert.doesNotMatch(memberMarkup, /무료로 시작하기|href="\/login"/);
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
  assert.equal(metadata.alternates.canonical, "https://money.example/money/privacy");
  assert.equal(metadata.openGraph.type, "website");
  assert.equal(metadata.openGraph.url, "https://money.example/money/privacy");
  assert.equal(metadata.openGraph.locale, "ko_KR");
  assert.equal(metadata.openGraph.images[0].url, "https://money.example/money/moneybook-og.png");
  assert.equal(metadata.twitter.card, "summary_large_image");
  assert.equal(metadata.twitter.images[0], "https://money.example/money/moneybook-og.png");
  assert.deepEqual(JSON.parse(JSON.stringify(metadata.verification)), {
    google: "google-token",
    other: { "naver-site-verification": "naver-token" },
  });
});

test("production defaults to the configured public domain and basePath", () => {
  const seo = loadSeo();
  const env = { NODE_ENV: "production" };
  const metadata = seo.createPublicMetadata("/", "MoneyBook", "설명", env);
  assert.equal(metadata.metadataBase.toString(), "https://www.woori.today/");
  assert.equal(metadata.alternates.canonical, "https://www.woori.today/money");
  assert.equal(metadata.openGraph.url, "https://www.woori.today/money");
  const sitemapUrls = JSON.parse(JSON.stringify(seo.buildPublicSitemap(env).map((item) => item.url)));
  assert.ok(sitemapUrls.every((url) => new URL(url).pathname === "/money" || new URL(url).pathname.startsWith("/money/")));
  assert.ok(sitemapUrls.every((url) => url !== "https://money.example/"));
  const privatePaths = ["/login", "/signup", "/account", "/admin", "/board", "/books"];
  assert.ok(sitemapUrls.every((url) => privatePaths.every((path) => !new URL(url).pathname.startsWith(`/money${path}`))));
  assert.deepEqual(sitemapUrls, [
    "https://www.woori.today/money",
    "https://www.woori.today/money/privacy",
    "https://www.woori.today/money/terms",
  ]);
  assert.equal(seo.buildWebApplicationJsonLd(env).url, "https://www.woori.today/money");
});

test("local development uses localhost only as a development canonical base", () => {
  const seo = loadSeo();
  assert.equal(seo.getSiteUrl({ NODE_ENV: "development" }).toString(), "http://localhost:3000/");
  assert.equal(seo.createPublicMetadata("/terms", "이용 안내", "설명", { NODE_ENV: "development" }).alternates.canonical, "http://localhost:3000/money/terms");
});

test("robots blocks private routes and sitemap includes only public pages", () => {
  const seo = loadSeo();
  const env = { NODE_ENV: "production", SITE_URL: "https://money.example" };
  const robots = seo.buildRobots(env);
  const disallow = robots.rules.disallow;
  assert.deepEqual(Array.from(disallow), ["/api/"]);
  assert.equal(robots.sitemap, "https://money.example/money/sitemap.xml");
  assert.deepEqual(JSON.parse(JSON.stringify(seo.buildPublicSitemap(env).map((item) => item.url))), [
    "https://money.example/money",
    "https://money.example/money/privacy",
    "https://money.example/money/terms",
  ]);
});

test("private page metadata is noindex and each private route applies the shared policy", () => {
  const seo = loadSeo();
  assert.deepEqual(JSON.parse(JSON.stringify(seo.privatePageMetadata().robots)), {
    index: false,
    follow: false,
    googleBot: { index: false, follow: false },
  });
  for (const relativePath of ["app/login/page.tsx", "app/signup/page.tsx", "app/board/layout.tsx", "app/books/layout.tsx", "app/account/layout.tsx", "app/admin/layout.tsx"]) {
    assert.match(fs.readFileSync(path.join(root, relativePath), "utf8"), /privatePageMetadata/);
  }
});

test("privacy and terms routes have real content, metadata and navigation back home", () => {
  for (const route of ["privacy", "terms"]) {
    const page = fs.readFileSync(path.join(root, `app/${route}/page.tsx`), "utf8");
    assert.match(page, /createPublicMetadata/);
    assert.match(page, /<h1\b/);
    assert.match(page, /href="\/"/);
    assert.doesNotMatch(page, /href="#"/);
  }
});

test("JSON-LD is a minimal WebApplication schema without invented prices or reviews", () => {
  const app = loadSeo().buildWebApplicationJsonLd({ NODE_ENV: "production", SITE_URL: "https://money.example" });
  assert.equal(app["@context"], "https://schema.org");
  assert.equal(app["@type"], "WebApplication");
  assert.equal(app.name, "MoneyBook");
  assert.equal(app.operatingSystem, "Web");
  assert.equal(app.url, "https://money.example/money");
  assert.equal(app.offers, undefined);
  assert.equal(app.aggregateRating, undefined);
});
