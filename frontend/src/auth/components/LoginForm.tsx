"use client";

import { useState, type FormEvent } from "react";
import Link from "next/link";
import { useLogin } from "../hooks/useLogin";

export default function LoginForm({ notice }: { notice?: string } = {}) {
  const [loginId, setLoginId] = useState("");
  const [password, setPassword] = useState("");
  const { login, isLoading, errorMessage } = useLogin();

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!isLoading) void login({ loginId, password });
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-5">
      {notice && <p role="status" className="rounded-lg bg-blue-50 p-3 text-sm text-blue-900">{notice}</p>}
      <div>
        <label htmlFor="loginId" className="mb-1 block text-sm font-medium">로그인 ID</label>
        <input id="loginId" name="loginId" autoComplete="username" required value={loginId}
          onChange={(event) => setLoginId(event.target.value)}
          className="w-full rounded-lg border border-zinc-300 bg-white px-3 py-2 text-zinc-900 outline-none focus:border-blue-600" />
      </div>
      <div>
        <label htmlFor="password" className="mb-1 block text-sm font-medium">비밀번호</label>
        <input id="password" name="password" type="password" autoComplete="current-password" required value={password}
          onChange={(event) => setPassword(event.target.value)}
          className="w-full rounded-lg border border-zinc-300 bg-white px-3 py-2 text-zinc-900 outline-none focus:border-blue-600" />
      </div>
      {errorMessage && <p role="alert" className="text-sm text-red-600">{errorMessage}</p>}
      <button type="submit" disabled={isLoading}
        className="w-full rounded-lg bg-blue-600 px-4 py-2.5 font-medium text-white hover:bg-blue-700 disabled:cursor-not-allowed disabled:opacity-60">
        {isLoading ? "로그인 중..." : "로그인"}
      </button>
      <p className="text-center text-sm"><Link href="/forgot-password" className="font-medium text-blue-700 hover:underline">비밀번호를 잊으셨나요?</Link></p>
      <p className="text-center text-sm text-zinc-600">
        계정이 없으신가요? <Link href="/signup" className="font-medium text-blue-700 hover:underline">회원가입</Link>
      </p>
    </form>
  );
}
