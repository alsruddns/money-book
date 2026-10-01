"use client";

import { useState, type FormEvent } from "react";
import Link from "next/link";
import { useSignup } from "../hooks/useSignup";
import type { SignUpReqDto } from "../dto/req/SignUpReqDto";

export default function SignupForm() {
  const [form, setForm] = useState<SignUpReqDto>({ loginId: "", password: "", passwordConfirm: "", nickname: "" });
  const { signup, isLoading, errorMessage } = useSignup();

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!isLoading) void signup(form);
  }

  function updateField(field: keyof SignUpReqDto, value: string) {
    setForm((current) => ({ ...current, [field]: value }));
  }

  const fields: { key: keyof SignUpReqDto; label: string; type: string; autoComplete: string }[] = [
    { key: "loginId", label: "로그인 ID", type: "text", autoComplete: "username" },
    { key: "password", label: "비밀번호", type: "password", autoComplete: "new-password" },
    { key: "passwordConfirm", label: "비밀번호 확인", type: "password", autoComplete: "new-password" },
    { key: "nickname", label: "닉네임", type: "text", autoComplete: "nickname" },
  ];

  return (
    <form onSubmit={handleSubmit} className="space-y-5">
      {fields.map(({ key, label, type, autoComplete }) => (
        <div key={key}>
          <label htmlFor={key} className="mb-1 block text-sm font-medium">{label}</label>
          <input id={key} name={key} type={type} autoComplete={autoComplete} required value={form[key]}
            onChange={(event) => updateField(key, event.target.value)}
            className="w-full rounded-lg border border-zinc-300 bg-white px-3 py-2 text-zinc-900 outline-none focus:border-blue-600" />
        </div>
      ))}
      {errorMessage && <p role="alert" className="text-sm text-red-600">{errorMessage}</p>}
      <button type="submit" disabled={isLoading}
        className="w-full rounded-lg bg-blue-600 px-4 py-2.5 font-medium text-white hover:bg-blue-700 disabled:cursor-not-allowed disabled:opacity-60">
        {isLoading ? "가입 중..." : "회원가입"}
      </button>
      <p className="text-center text-sm text-zinc-600">
        이미 계정이 있으신가요? <Link href="/login" className="font-medium text-blue-700 hover:underline">로그인</Link>
      </p>
    </form>
  );
}
