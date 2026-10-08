import test from "node:test";
import assert from "node:assert/strict";
import fs from "node:fs";
import path from "node:path";
import vm from "node:vm";
import ts from "typescript";
import { fileURLToPath } from "node:url";

const testDirectory = path.dirname(fileURLToPath(import.meta.url));
const source = fs.readFileSync(path.join(testDirectory, "../src/auth/session/idleSession.ts"), "utf8");
const compiled = ts.transpileModule(source, {
  compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2020 },
}).outputText;

function loadIdleSession() {
  const values = new Map();
  const compiledModule = { exports: {} };
  vm.runInNewContext(compiled, {
    module: compiledModule,
    exports: compiledModule.exports,
    window: { localStorage: {
      getItem: (key) => values.get(key) ?? null,
      setItem: (key, value) => values.set(key, value),
      removeItem: (key) => values.delete(key),
    } },
  });
  return compiledModule.exports;
}

test("idle policy warns five minutes before the six-hour timeout and expires at the boundary", () => {
  const idle = loadIdleSession();
  const start = 1_800_000_000_000;
  assert.equal(idle.IDLE_TIMEOUT_MS, 6 * 60 * 60 * 1000);
  assert.equal(idle.IDLE_WARNING_MS, 5 * 60 * 60 * 1000 + 55 * 60 * 1000);
  assert.equal(idle.isIdleExpired(start, start + idle.IDLE_TIMEOUT_MS - 1), false);
  assert.equal(idle.isIdleExpired(start, start + idle.IDLE_TIMEOUT_MS), true);
});

test("last interaction timestamp persists between page loads and can be cleared on logout", () => {
  const idle = loadIdleSession();
  idle.writeLastActivityAt(1_800_000_000_123);
  assert.equal(idle.readLastActivityAt(), 1_800_000_000_123);
  idle.clearLastActivityAt();
  assert.equal(idle.readLastActivityAt(), null);
});

test("fake timers schedule the warning at 5h55m and timeout at exactly six hours", () => {
  const idle = loadIdleSession();
  const scheduled = [];
  const cancelled = [];
  const callbacks = [];
  const start = 1_800_000_000_000;
  const cleanup = idle.scheduleIdleTimers(start, () => callbacks.push("warning"), () => callbacks.push("timeout"), start,
    (callback, delay) => { scheduled.push({ callback, delay }); return scheduled.length; },
    (timer) => cancelled.push(timer));
  assert.deepEqual(scheduled.map((timer) => timer.delay), [idle.IDLE_WARNING_MS, idle.IDLE_TIMEOUT_MS]);
  scheduled.forEach((timer) => timer.callback());
  assert.deepEqual(callbacks, ["warning", "timeout"]);
  cleanup();
  assert.deepEqual(cancelled, [1, 2]);
});
