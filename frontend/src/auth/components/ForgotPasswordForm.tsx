"use client";

import { useState, type FormEvent } from "react";
import Link from "next/link";
import { getApiErrorMessage } from "@/common/api/getApiErrorMessage";
import { useConfirmEmailVerificationMutation, useGetSecurityQuestionsQuery, useRequestPasswordResetEmailMutation, useResetPasswordByEmailMutation, useResetPasswordByQuestionMutation, useResetPasswordByRecoveryCodeMutation } from "../controller/passwordRecoveryApi";

type Method = "email" | "question" | "code";
const inputClass = "mt-1 min-h-11 w-full rounded-lg border border-zinc-300 bg-white px-3 py-2 text-zinc-900 outline-none focus:border-blue-600 focus:ring-2 focus:ring-blue-100";
const genericRecoveryError = "입력한 계정 또는 복구 정보가 일치하지 않습니다.";

export default function ForgotPasswordForm() {
  const [method, setMethod] = useState<Method>("email");
  const [loginId, setLoginId] = useState("");
  const [questionCode, setQuestionCode] = useState("");
  const [answer, setAnswer] = useState("");
  const [recoveryCode, setRecoveryCode] = useState("");
  const [email, setEmail] = useState("");
  const [verificationUid, setVerificationUid] = useState<number | null>(null);
  const [verificationToken, setVerificationToken] = useState("");
  const [code, setCode] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [newPasswordConfirm, setNewPasswordConfirm] = useState("");
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");
  const [complete, setComplete] = useState(false);
  const questions = useGetSecurityQuestionsQuery();
  const [requestEmail, requestState] = useRequestPasswordResetEmailMutation();
  const [confirmEmail, confirmState] = useConfirmEmailVerificationMutation();
  const [resetEmail, resetEmailState] = useResetPasswordByEmailMutation();
  const [resetQuestion, resetQuestionState] = useResetPasswordByQuestionMutation();
  const [resetCode, resetCodeState] = useResetPasswordByRecoveryCodeMutation();

  function resetNotices(next: Method) { setMethod(next); setError(""); setMessage(""); setVerificationUid(null); setVerificationToken(""); setCode(""); }
  async function requestVerification() {
    setError("");
    try { const response = await requestEmail({ loginId, email }).unwrap(); requestState.reset(); setVerificationUid(response.verificationUid); setMessage("입력한 정보와 일치하는 인증 이메일이 있는 경우 인증번호를 전송했습니다."); }
    catch (reason) { setError(getApiErrorMessage(reason, "입력한 정보와 일치하는 인증 이메일이 있는 경우 인증번호를 전송했습니다.")); requestState.reset(); }
  }
  async function confirmVerification() {
    if (verificationUid === null) return;
    setError("");
    try { const result = await confirmEmail({ verificationUid, code }).unwrap(); confirmState.reset(); setCode(""); setVerificationToken(result.verificationToken); setMessage("이메일 인증이 완료되었습니다."); }
    catch (reason) { setError(getApiErrorMessage(reason, genericRecoveryError)); confirmState.reset(); }
  }
  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); setError(""); setMessage("");
    if (newPassword !== newPasswordConfirm) { setError("새 비밀번호와 확인 값이 일치하지 않습니다."); return; }
    try {
      if (method === "question") { await resetQuestion({ loginId, questionCode, answer, newPassword, newPasswordConfirm }).unwrap(); resetQuestionState.reset(); }
      else if (method === "code") { await resetCode({ loginId, recoveryCode, newPassword, newPasswordConfirm }).unwrap(); resetCodeState.reset(); }
      else {
        if (!verificationToken) { setError("이메일 인증을 먼저 완료해 주세요."); return; }
        await resetEmail({ verificationToken, newPassword, newPasswordConfirm }).unwrap(); resetEmailState.reset();
      }
      setAnswer(""); setRecoveryCode(""); setCode(""); setVerificationToken(""); setNewPassword(""); setNewPasswordConfirm("");
      setComplete(true);
    } catch (reason) { setError(getApiErrorMessage(reason, genericRecoveryError)); resetEmailState.reset(); resetQuestionState.reset(); resetCodeState.reset(); }
  }

  if (complete) return <div className="space-y-4 text-center"><p role="status" className="text-sm text-green-800">비밀번호를 변경했습니다. 새 비밀번호로 다시 로그인해주세요.</p><Link href="/login" className="inline-flex min-h-11 items-center rounded-lg bg-blue-600 px-5 font-medium text-white">로그인</Link></div>;

  const busy = resetEmailState.isLoading || resetQuestionState.isLoading || resetCodeState.isLoading;
  return <div className="space-y-5">
    <div role="tablist" aria-label="비밀번호 복구 방법" className="grid grid-cols-3 rounded-lg bg-zinc-100 p-1">
      {([["email", "이메일"], ["question", "보안 질문"], ["code", "복구 코드"]] as const).map(([key, label]) => <button key={key} role="tab" aria-selected={method === key} type="button" onClick={() => resetNotices(key)} className={`min-h-10 rounded-md px-1 text-xs font-medium sm:text-sm ${method === key ? "bg-white text-blue-800 shadow-sm" : "text-zinc-600 hover:text-zinc-900"}`}>{label}</button>)}
    </div>
    <form onSubmit={submit} className="space-y-4">
      {method === "email" ? <>
        <div><label htmlFor="recovery-login-id" className="text-sm font-medium">아이디</label><input id="recovery-login-id" autoComplete="username" required value={loginId} onChange={(e) => setLoginId(e.target.value)} className={inputClass} /></div>
        <div><label htmlFor="recovery-email" className="text-sm font-medium">인증된 이메일</label><input id="recovery-email" type="email" autoComplete="email" required value={email} onChange={(e) => setEmail(e.target.value)} className={inputClass} /></div>
        <button type="button" disabled={!loginId || !email || requestState.isLoading} onClick={() => void requestVerification()} className="min-h-11 w-full rounded-lg border border-zinc-300 px-3 text-sm font-medium disabled:opacity-50">{requestState.isLoading ? "요청 중..." : "인증번호 받기"}</button>
        <p role="status" aria-live="polite" className="text-sm text-zinc-600">{message || "입력한 정보와 일치하는 인증 이메일이 있는 경우 인증번호를 전송합니다."}</p>
        {verificationUid !== null && !verificationToken && <div><label htmlFor="recovery-email-code" className="text-sm font-medium">인증번호</label><div className="flex flex-col gap-2 sm:flex-row"><input id="recovery-email-code" inputMode="numeric" autoComplete="one-time-code" maxLength={6} required value={code} onChange={(e) => setCode(e.target.value.replace(/\D/g, "").slice(0, 6))} className={`${inputClass} mt-0 min-w-0 flex-1`} /><button type="button" disabled={code.length !== 6 || confirmState.isLoading} onClick={() => void confirmVerification()} className="min-h-11 rounded-lg border px-3 text-sm font-medium disabled:opacity-50">{confirmState.isLoading ? "확인 중..." : "인증하기"}</button></div></div>}
      </> : <>
        <div><label htmlFor="recovery-login-id" className="text-sm font-medium">아이디</label><input id="recovery-login-id" autoComplete="username" required value={loginId} onChange={(e) => setLoginId(e.target.value)} className={inputClass} /></div>
        {method === "question" ? <>
          <div><label htmlFor="recovery-question" className="text-sm font-medium">보안 질문</label><select id="recovery-question" required value={questionCode} onChange={(e) => setQuestionCode(e.target.value)} className={inputClass}><option value="">질문을 선택하세요</option>{questions.data?.map((q) => <option key={q.code} value={q.code}>{q.question}</option>)}</select></div>
          <div><label htmlFor="recovery-answer" className="text-sm font-medium">답변</label><input id="recovery-answer" autoComplete="off" required maxLength={128} value={answer} onChange={(e) => setAnswer(e.target.value)} className={inputClass} /></div>
          <p className="text-xs text-zinc-600">보안 질문의 답변은 가입 시 입력한 내용과 정확하게 일치해야 합니다.</p>
        </> : <>
          <div><label htmlFor="recovery-code" className="text-sm font-medium">복구코드</label><input id="recovery-code" autoComplete="off" required value={recoveryCode} onChange={(e) => setRecoveryCode(e.target.value)} className={`${inputClass} font-mono`} /></div>
          <p className="text-xs text-zinc-600">복구코드는 발급 시 한 번만 확인할 수 있으며, 사용한 코드는 다시 사용할 수 없습니다.</p>
        </>}
      </>}
      {(method !== "email" || verificationToken) && <>
        <div><label htmlFor="recovery-new-password" className="text-sm font-medium">새 비밀번호</label><input id="recovery-new-password" type="password" autoComplete="new-password" maxLength={72} required value={newPassword} onChange={(e) => setNewPassword(e.target.value)} className={inputClass} /></div>
        <div><label htmlFor="recovery-new-password-confirm" className="text-sm font-medium">새 비밀번호 확인</label><input id="recovery-new-password-confirm" type="password" autoComplete="new-password" maxLength={72} required value={newPasswordConfirm} onChange={(e) => setNewPasswordConfirm(e.target.value)} className={inputClass} /></div>
        <button type="submit" disabled={busy} className="min-h-11 w-full rounded-lg bg-blue-600 px-4 font-medium text-white disabled:opacity-50">{busy ? "변경 중..." : "비밀번호 재설정"}</button>
      </>}
      {error && <p role="alert" className="text-sm text-red-700">{error}</p>}
    </form>
    <p className="text-sm text-zinc-600">위 방법으로 복구할 수 없는 경우 서비스 관리자에게 문의해주세요.</p>
  </div>;
}
