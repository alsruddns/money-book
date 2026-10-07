import test from "node:test";
import assert from "node:assert/strict";
import fs from "node:fs";
import path from "node:path";
import vm from "node:vm";
import { createRequire } from "node:module";
import { fileURLToPath } from "node:url";
import ts from "typescript";
const require = createRequire(import.meta.url); const root = path.join(path.dirname(fileURLToPath(import.meta.url)), "../src/..");
function loadConfig(env) {
  const source=fs.readFileSync(path.join(root,"next.config.ts"),"utf8");
  const js=ts.transpileModule(source,{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2020}}).outputText;
  const mod={exports:{}}; vm.runInNewContext(js,{module:mod,exports:mod.exports,require,process:{env},URL}); return mod.exports.default;
}
function loadSource(relativePath, mocks = {}) {
  const source=fs.readFileSync(path.join(root,"src",relativePath),"utf8");
  const js=ts.transpileModule(source,{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2020}}).outputText;
  const mod={exports:{}}; vm.runInNewContext(js,{module:mod,exports:mod.exports,require:(name)=>name in mocks?mocks[name]:require(name),URL,URLSearchParams}); return mod.exports;
}

test("Next proxy always maps same-origin /api paths to backend /api paths",async()=>{
  const local=await loadConfig({NODE_ENV:"development"}).rewrites();
  assert.deepEqual(JSON.parse(JSON.stringify(local)),[{source:"/api/:path*",destination:"http://localhost:8080/api/:path*"}]);
  const production=await loadConfig({NODE_ENV:"production",BACKEND_API_URL:"https://api.example.test"}).rewrites();
  assert.deepEqual(JSON.parse(JSON.stringify(production)),[{source:"/api/:path*",destination:"https://api.example.test/api/:path*"}]);
});

test("production proxy requires a server-only origin and rejects an API path that would duplicate /api",async()=>{
  await assert.rejects(()=>loadConfig({NODE_ENV:"production"}).rewrites(),/BACKEND_API_URL is required/);
  await assert.rejects(()=>loadConfig({NODE_ENV:"development",BACKEND_API_URL:"http://localhost:8080/api"}).rewrites(),/must be an HTTP\(S\) origin/);
});

test("repository config uses same-origin API base and removes old public backend base variable",()=>{
  const baseApi=fs.readFileSync(path.join(root,"src/common/api/baseApi.ts"),"utf8");
  const nextConfig=fs.readFileSync(path.join(root,"next.config.ts"),"utf8");
  assert.match(baseApi,/baseUrl:\s*["']\/api["']/);
  assert.doesNotMatch(baseApi,/NEXT_PUBLIC_API_BASE_URL|localhost:8080/);
  assert.doesNotMatch(nextConfig,/NEXT_PUBLIC_API_BASE_URL/);
});

test("auth, MoneyBook, CSV, and backup endpoints compose with /api exactly once",()=>{
  const builder={query:(definition)=>definition,mutation:(definition)=>definition};
  const baseApi={injectEndpoints:({endpoints})=>endpoints(builder)};
  const auth=loadSource("auth/controller/authApi.ts",{"@/common/api/baseApi":{baseApi}}).authApi;
  const books=loadSource("moneybook/controller/moneyBookApi.ts",{"@/common/api/baseApi":{baseApi}}).moneyBookApi;
  const exports=loadSource("export/controller/transactionExportApi.ts",{"@/common/api/baseApi":{baseApi},"@/common/download/fileDownload":{prepareDownload:()=>({})}}).transactionExportApi;
  const backup=loadSource("backup/controller/moneyBookBackupApi.ts",{"@/common/api/baseApi":{baseApi},"@/common/download/fileDownload":{prepareDownload:()=>({})}}).moneyBookBackupApi;
  const paths=[auth.signup.query({}).url,auth.login.query({}).url,auth.refreshToken.query({}).url,auth.getCurrentUser.query(),books.getMoneyBooks.query(),exports.exportTransactions.query({moneyBookUid:3,format:"csv",startDate:"2026-01-01",endDate:"2026-01-31"}).url,backup.exportMoneyBookBackup.query(3).url];
  assert.deepEqual(paths.map((path)=>`/api/${path}`),["/api/auth/signup","/api/auth/login","/api/auth/refresh","/api/auth/me","/api/money-books","/api/money-books/3/exports/transactions.csv?startDate=2026-01-01&endDate=2026-01-31","/api/money-books/3/backups/export"]);
  assert.equal(paths.some((path)=>path.startsWith("/api/")),false);
});
