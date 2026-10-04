"use client";

import { useState, type FormEvent } from "react";
import Link from "next/link";
import { useSignup } from "../hooks/useSignup";
import { useConfirmEmailVerificationMutation, useGetSecurityQuestionsQuery, useRequestEmailVerificationMutation } from "../controller/passwordRecoveryApi";
import type { SignUpReqDto } from "../dto/req/SignUpReqDto";
import { getApiErrorMessage } from "@/common/api/getApiErrorMessage";

const blank: SignUpReqDto = { loginId: "", password: "", passwordConfirm: "", nickname: "", securityQuestionCode: "", securityAnswer: "" };
const inputClass = "mt-1 min-h-11 w-full rounded-lg border border-zinc-300 bg-white px-3 py-2 text-zinc-900 outline-none focus:border-blue-600 focus:ring-2 focus:ring-blue-100";

export default function SignupForm() {
  const [form, setForm] = useState<SignUpReqDto>(blank);
  const { signup, isLoading, errorMessage, reset } = useSignup();
  const questions = useGetSecurityQuestionsQuery();
  const [requestCode, requestState] = useRequestEmailVerificationMutation();
  const [confirmCode, confirmState] = useConfirmEmailVerificationMutation();
  const [email, setEmail] = useState("");
  const [code, setCode] = useState("");
  const [verificationUid, setVerificationUid] = useState<number | null>(null);
  const [verificationToken, setVerificationToken] = useState("");
  const [verificationMessage, setVerificationMessage] = useState("");
  const [verificationError, setVerificationError] = useState("");
  const [recoveryCodes, setRecoveryCodes] = useState<string[] | null>(null);
  const [copied, setCopied] = useState(false);
  const [done, setDone] = useState(false);

  function updateField(field: keyof SignUpReqDto, value: string) { setForm((current) => ({ ...current, [field]: value })); }
  async function sendCode() {
    setVerificationError(""); setVerificationMessage(""); setVerificationToken("");
    try { const result = await requestCode({ email, purpose: "SIGNUP" }).unwrap(); requestState.reset(); setVerificationUid(result.verificationUid); setVerificationMessage("인증번호를 전송했습니다. 10분 안에 인증을 완료해 주세요."); }
    catch (error) { setVerificationError(getApiErrorMessage(error, "인증번호를 요청하지 못했습니다.")); requestState.reset(); }
  }
  async function verifyCode() {
    if (verificationUid === null) return;
    setVerificationError("");
    try { const result = await confirmCode({ verificationUid, code }).unwrap(); confirmState.reset(); setCode(""); setVerificationToken(result.verificationToken); setVerificationMessage("이메일 인증이 완료되었습니다."); }
    catch (error) { setVerificationError(getApiErrorMessage(error, "인증번호를 확인하지 못했습니다.")); confirmState.reset(); }
  }
  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (email.trim() && !verificationToken) { setVerificationError("이메일을 입력했다면 먼저 이메일 인증을 완료해 주세요."); return; }
    const result = await signup({ ...form, ...(verificationToken ? { emailVerificationToken: verificationToken } : {}) });
    if (result) setRecoveryCodes(result.recoveryCodes ?? []);
  }

  if (done) return <section className="space-y-5 text-center" aria-labelledby="signup-done-title">
    <h2 id="signup-done-title" className="text-xl font-semibold">회원가입이 완료되었습니다.</h2>
    <p className="text-sm text-zinc-600">로그인 화면에서 새 계정으로 로그인해 주세요.</p>
    <Link href="/login" className="inline-flex min-h-11 items-center rounded-lg bg-blue-600 px-5 font-medium text-white">로그인</Link>
  </section>;

  if (recoveryCodes) return <section className="space-y-5" aria-labelledby="recovery-codes-title">
    <h2 id="recovery-codes-title" className="text-xl font-semibold">계정 복구코드를 저장해주세요</h2>
    {recoveryCodes.length ? <ul className="grid gap-2 rounded-lg bg-zinc-50 p-4 font-mono text-sm sm:grid-cols-2">{recoveryCodes.map((value) => <li key={value} className="break-all">{value}</li>)}</ul> : <p role="alert" className="text-sm text-red-700">복구코드를 표시할 수 없습니다. 계정 복구 설정을 확인해 주세요.</p>}
    <button type="button" disabled={!recoveryCodes.length} onClick={() => { void navigator.clipboard.writeText(recoveryCodes.join("\n")).then(() => setCopied(true)); }} className="min-h-11 w-full rounded-lg border px-4 font-medium">{copied ? "복사했습니다" : "전체 복사"}</button>
    <div className="space-y-1 text-sm text-zinc-600"><p>복구코드는 지금 한 번만 표시됩니다.</p><p>보안 질문 답변을 잊었거나 다른 복구 방법을 사용할 수 없을 때 필요합니다.</p><p>별도로 보관하지 않으면 기존 코드를 다시 확인할 수 없습니다.</p></div>
    <button type="button" onClick={() => { setRecoveryCodes(null); setForm(blank); setVerificationToken(""); reset(); setDone(true); }} className="min-h-11 w-full rounded-lg bg-blue-600 px-4 font-medium text-white">복구코드를 저장했습니다</button>
  </section>;

  return <form onSubmit={handleSubmit} className="space-y-5">
    {(["loginId", "password", "passwordConfirm", "nickname"] as const).map((key) => {
      const meta = { loginId: ["로그인 ID", "text", "username"], password: ["비밀번호", "password", "new-password"], passwordConfirm: ["비밀번호 확인", "password", "new-password"], nickname: ["닉네임", "text", "nickname"] }[key];
      return <div key={key}><label htmlFor={key} className="text-sm font-medium">{meta[0]} *</label><input id={key} name={key} type={meta[1]} autoComplete={meta[2]} required value={form[key]} onChange={(event) => updateField(key, event.target.value)} className={inputClass} /></div>;
    })}
    <fieldset className="space-y-3 rounded-lg border border-zinc-200 p-4">
      <legend className="px-1 text-sm font-semibold">계정 복구 정보</legend>
      <p className="text-sm text-zinc-600">보안 질문과 답변은 비밀번호를 분실했을 때 본인 확인에 사용됩니다. 입력한 답변을 정확하게 기억해주세요.</p>
      {questions.isLoading ? <p role="status" className="text-sm">보안 질문을 불러오는 중...</p> : questions.isError ? <p role="alert" className="text-sm text-red-700">보안 질문을 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.</p> : <>
        <div><label htmlFor="security-question" className="text-sm font-medium">보안 질문 *</label><select id="security-question" required value={form.securityQuestionCode} onChange={(event) => updateField("securityQuestionCode", event.target.value)} className={inputClass}><option value="">질문을 선택하세요</option>{questions.data?.map((item) => <option key={item.code} value={item.code}>{item.question}</option>)}</select></div>
        <div><label htmlFor="security-answer" className="text-sm font-medium">답변 *</label><input id="security-answer" name="securityAnswer" type="text" autoComplete="off" required maxLength={128} value={form.securityAnswer} onChange={(event) => updateField("securityAnswer", event.target.value)} className={inputClass} /></div>
      </>}
    </fieldset>
    <fieldset className="space-y-3 rounded-lg border border-zinc-200 p-4">
      <legend className="px-1 text-sm font-semibold">이메일 (선택)</legend>
      <p className="text-sm text-zinc-600">이메일 등록은 선택사항입니다. 인증된 이메일이 있으면 비밀번호 분실 시 이메일 인증을 통해 직접 재설정할 수 있습니다. 이메일을 등록하지 않아도 서비스 이용에는 문제가 없습니다.</p>
      <label htmlFor="signup-email" className="text-sm font-medium">이메일 주소</label>
      <div className="flex flex-col gap-2 sm:flex-row"><input id="signup-email" type="email" autoComplete="email" value={email} onChange={(event) => { setEmail(event.target.value); setVerificationToken(""); setVerificationUid(null); setVerificationMessage(""); }} className={`${inputClass} mt-0 min-w-0 flex-1`} /><button type="button" disabled={!email || requestState.isLoading || Boolean(verificationToken)} onClick={() => void sendCode()} className="min-h-11 shrink-0 rounded-lg border px-3 text-sm font-medium disabled:opacity-50">{requestState.isLoading ? "요청 중..." : "인증번호 받기"}</button></div>
      {verificationUid !== null && !verificationToken && <div><label htmlFor="signup-email-code" className="text-sm font-medium">인증번호</label><div className="flex flex-col gap-2 sm:flex-row"><input id="signup-email-code" inputMode="numeric" autoComplete="one-time-code" maxLength={6} value={code} onChange={(event) => setCode(event.target.value.replace(/\D/g, "").slice(0, 6))} className={`${inputClass} mt-0 min-w-0 flex-1`} /><button type="button" disabled={code.length !== 6 || confirmState.isLoading} onClick={() => void verifyCode()} className="min-h-11 shrink-0 rounded-lg border px-3 text-sm font-medium disabled:opacity-50">{confirmState.isLoading ? "확인 중..." : "인증하기"}</button></div></div>}
      {verificationMessage && <p role="status" className="text-sm text-green-700">{verificationMessage}</p>}{verificationError && <p role="alert" className="text-sm text-red-700">{verificationError}</p>}
      {!email && <p className="text-xs text-zinc-500">이메일 없이 가입하려면 이 항목을 비워두세요.</p>}
    </fieldset>
    {errorMessage && <p role="alert" className="text-sm text-red-600">{errorMessage}</p>}
    <button type="submit" disabled={isLoading || questions.isLoading || questions.isError || Boolean(email && !verificationToken)} className="min-h-11 w-full rounded-lg bg-blue-600 px-4 font-medium text-white hover:bg-blue-700 disabled:cursor-not-allowed disabled:opacity-60">{isLoading ? "가입 중..." : "회원가입"}</button>
    <p className="text-center text-sm text-zinc-600">이미 계정이 있으신가요? <Link href="/login" className="font-medium text-blue-700 hover:underline">로그인</Link></p>
  </form>;
}
