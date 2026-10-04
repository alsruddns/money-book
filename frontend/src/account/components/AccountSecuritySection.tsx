"use client";

import { useState, type FormEvent } from "react";
import { getApiErrorMessage } from "@/common/api/getApiErrorMessage";
import { useApplyAccountEmailMutation, useConfirmEmailVerificationMutation, useGetAccountSecurityQuery, useGetSecurityQuestionsQuery, useRegenerateRecoveryCodesMutation, useRemoveAccountEmailMutation, useRequestEmailVerificationMutation, useUpdateSecurityQuestionMutation } from "@/auth/controller/passwordRecoveryApi";

const field = "mt-1 min-h-11 w-full rounded-lg border border-zinc-300 bg-white px-3 py-2 text-zinc-900";

export default function AccountSecuritySection() {
  const security = useGetAccountSecurityQuery();
  const questions = useGetSecurityQuestionsQuery();
  const [updateQuestion, questionState] = useUpdateSecurityQuestionMutation();
  const [regenerate, regenerateState] = useRegenerateRecoveryCodesMutation();
  const [requestCode, requestState] = useRequestEmailVerificationMutation();
  const [confirmCode, confirmState] = useConfirmEmailVerificationMutation();
  const [applyEmail, applyState] = useApplyAccountEmailMutation();
  const [removeEmail, removeState] = useRemoveAccountEmailMutation();
  const [questionOpen, setQuestionOpen] = useState(false);
  const [regenerateOpen, setRegenerateOpen] = useState(false);
  const [questionCode, setQuestionCode] = useState("");
  const [answer, setAnswer] = useState("");
  const [currentPassword, setCurrentPassword] = useState("");
  const [regeneratePassword, setRegeneratePassword] = useState("");
  const [recoveryCodes, setRecoveryCodes] = useState<string[] | null>(null);
  const [emailOpen, setEmailOpen] = useState(false);
  const [email, setEmail] = useState("");
  const [emailCode, setEmailCode] = useState("");
  const [verificationUid, setVerificationUid] = useState<number | null>(null);
  const [verificationToken, setVerificationToken] = useState("");
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  if (security.isLoading) return <section className="rounded-xl border bg-white p-5" aria-labelledby="security-title"><h2 id="security-title" className="text-lg font-semibold">보안</h2><p role="status" className="mt-3 text-sm">보안 설정을 불러오는 중...</p></section>;
  if (security.isError || !security.data) return <section className="rounded-xl border bg-white p-5" aria-labelledby="security-title"><h2 id="security-title" className="text-lg font-semibold">보안</h2><p role="alert" className="mt-3 text-sm text-red-700">{getApiErrorMessage(security.error, "보안 설정을 불러오지 못했습니다.")}</p><button type="button" onClick={() => void security.refetch()} className="mt-3 min-h-11 rounded-lg border px-4 text-sm">다시 시도</button></section>;

  async function submitQuestion(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); setError(""); setSuccess("");
    try { await updateQuestion({ questionCode, answer, currentPassword }).unwrap(); questionState.reset(); setQuestionOpen(false); setQuestionCode(""); setAnswer(""); setCurrentPassword(""); setSuccess("보안 질문을 저장했습니다."); }
    catch (reason) { setError(getApiErrorMessage(reason, "보안 질문을 저장하지 못했습니다.")); questionState.reset(); }
  }
  async function submitRegenerate(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); setError(""); setSuccess("");
    try { const result = await regenerate({ currentPassword: regeneratePassword }).unwrap(); regenerateState.reset(); setRecoveryCodes(result.recoveryCodes); setRegeneratePassword(""); setRegenerateOpen(false); }
    catch (reason) { setError(getApiErrorMessage(reason, "복구코드를 발급하지 못했습니다.")); regenerateState.reset(); }
  }
  async function requestEmailCode() {
    setError(""); setVerificationToken("");
    try { const result = await requestCode({ email, purpose: "ACCOUNT_EMAIL" }).unwrap(); requestState.reset(); setVerificationUid(result.verificationUid); setSuccess("인증번호를 전송했습니다."); }
    catch (reason) { setError(getApiErrorMessage(reason, "인증번호를 요청하지 못했습니다.")); }
  }
  async function confirmEmailCode() {
    if (verificationUid === null) return;
    setError("");
    try { const result = await confirmCode({ verificationUid, code: emailCode }).unwrap(); confirmState.reset(); setEmailCode(""); setVerificationToken(result.verificationToken); setSuccess("이메일 인증이 완료되었습니다."); }
    catch (reason) { setError(getApiErrorMessage(reason, "인증번호를 확인하지 못했습니다.")); }
  }
  async function saveEmail() {
    if (!verificationToken) return;
    setError("");
    try { await applyEmail({ verificationToken }).unwrap(); applyState.reset(); setEmailOpen(false); setVerificationToken(""); setEmail(""); setSuccess("복구 이메일을 등록했습니다."); }
    catch (reason) { setError(getApiErrorMessage(reason, "복구 이메일을 저장하지 못했습니다.")); applyState.reset(); }
  }
  async function deleteEmail() {
    if (!window.confirm("복구 이메일을 삭제하시겠습니까?")) return;
    const password = window.prompt("현재 비밀번호를 입력해 주세요.");
    if (!password) return;
    setError("");
    try { await removeEmail({ currentPassword: password }).unwrap(); removeState.reset(); setSuccess("복구 이메일을 삭제했습니다."); }
    catch (reason) { setError(getApiErrorMessage(reason, "복구 이메일을 삭제하지 못했습니다.")); removeState.reset(); }
  }

  return <section aria-labelledby="security-title" className="rounded-xl border border-zinc-200 bg-white p-5 sm:p-6">
    <h2 id="security-title" className="text-lg font-semibold">보안</h2>
    <dl className="mt-4 grid gap-4 sm:grid-cols-3">
      <div><dt className="text-sm text-zinc-600">보안 질문</dt><dd className="mt-1 font-medium">{security.data.securityQuestionConfigured ? "설정됨" : "설정 안 됨"}</dd>{security.data.securityQuestionConfigured && <p className="mt-1 text-xs text-zinc-500">{questions.data?.find((item) => item.code === security.data.securityQuestionCode)?.question}</p>}</div>
      <div><dt className="text-sm text-zinc-600">복구 코드</dt><dd className="mt-1 font-medium">사용 가능한 코드 {security.data.recoveryCodesRemaining}개</dd></div>
      <div><dt className="text-sm text-zinc-600">복구 이메일</dt><dd className="mt-1 font-medium">{security.data.emailVerified ? `${security.data.maskedEmail} · 인증 완료` : "등록 안 됨"}</dd></div>
    </dl>
    <div className="mt-4 flex flex-wrap gap-2">
      <button type="button" onClick={() => { setQuestionOpen(!questionOpen); setError(""); }} className="min-h-11 rounded-lg border px-3 text-sm font-medium">{security.data.securityQuestionConfigured ? "보안 질문 변경" : "보안 질문 설정"}</button>
      <button type="button" onClick={() => { if (window.confirm("새 복구코드를 발급하면 기존 복구코드는 모두 사용할 수 없게 됩니다.")) { setRecoveryCodes(null); setRegenerateOpen(true); setError(""); } }} className="min-h-11 rounded-lg border px-3 text-sm font-medium">새 복구코드 발급</button>
      <button type="button" onClick={() => { setEmailOpen(!emailOpen); setError(""); }} className="min-h-11 rounded-lg border px-3 text-sm font-medium">{security.data.emailVerified ? "복구 이메일 변경" : "복구 이메일 등록"}</button>
      {security.data.emailVerified && <button type="button" onClick={() => void deleteEmail()} disabled={removeState.isLoading} className="min-h-11 rounded-lg border border-red-200 px-3 text-sm font-medium text-red-700">이메일 삭제</button>}
    </div>
    {questionOpen && <form onSubmit={submitQuestion} className="mt-5 grid gap-3 rounded-lg bg-zinc-50 p-4 sm:grid-cols-2">
      <div className="sm:col-span-2"><label htmlFor="account-security-question" className="text-sm font-medium">새 보안 질문</label><select id="account-security-question" required value={questionCode} onChange={(e) => setQuestionCode(e.target.value)} className={field}><option value="">질문을 선택하세요</option>{questions.data?.map((item) => <option key={item.code} value={item.code}>{item.question}</option>)}</select></div>
      <div><label htmlFor="account-security-answer" className="text-sm font-medium">새 답변</label><input id="account-security-answer" autoComplete="off" maxLength={128} required value={answer} onChange={(e) => setAnswer(e.target.value)} className={field} /></div>
      <div><label htmlFor="account-security-password" className="text-sm font-medium">현재 비밀번호</label><input id="account-security-password" type="password" autoComplete="current-password" maxLength={72} required value={currentPassword} onChange={(e) => setCurrentPassword(e.target.value)} className={field} /></div>
      <button type="submit" disabled={questionState.isLoading} className="min-h-11 rounded-lg bg-blue-600 px-4 text-sm font-medium text-white sm:col-span-2">{questionState.isLoading ? "저장 중..." : "보안 질문 저장"}</button>
    </form>}
    {regenerateOpen && !questionOpen && !recoveryCodes && <form onSubmit={submitRegenerate} className="mt-5 flex flex-col gap-3 rounded-lg bg-amber-50 p-4 sm:flex-row sm:items-end"><div className="min-w-0 flex-1"><label htmlFor="regenerate-current-password" className="text-sm font-medium">현재 비밀번호</label><input id="regenerate-current-password" type="password" autoComplete="current-password" maxLength={72} required value={regeneratePassword} onChange={(e) => setRegeneratePassword(e.target.value)} className={field} /><p className="mt-1 text-xs text-zinc-600">기존 복구코드는 모두 폐기됩니다.</p></div><button type="submit" disabled={regenerateState.isLoading} className="min-h-11 rounded-lg bg-blue-600 px-4 text-sm font-medium text-white">{regenerateState.isLoading ? "발급 중..." : "발급 확인"}</button></form>}
    {recoveryCodes && <div className="mt-5 space-y-3 rounded-lg border border-blue-200 bg-blue-50 p-4"><h3 className="font-semibold">새 복구코드 · 지금 한 번만 표시됩니다</h3><ul className="grid gap-2 font-mono text-sm sm:grid-cols-2">{recoveryCodes.map((value) => <li className="break-all" key={value}>{value}</li>)}</ul><button type="button" onClick={() => void navigator.clipboard.writeText(recoveryCodes.join("\n"))} className="min-h-11 rounded-lg border bg-white px-4 text-sm">전체 복사</button><button type="button" onClick={() => { setRecoveryCodes(null); regenerateState.reset(); }} className="min-h-11 rounded-lg bg-blue-600 px-4 text-sm text-white">저장했습니다</button></div>}
    {emailOpen && <div className="mt-5 space-y-3 rounded-lg bg-zinc-50 p-4"><label htmlFor="account-recovery-email" className="text-sm font-medium">새 인증 이메일</label><div className="flex flex-col gap-2 sm:flex-row"><input id="account-recovery-email" type="email" autoComplete="email" required value={email} onChange={(e) => setEmail(e.target.value)} className={`${field} mt-0 min-w-0 flex-1`} /><button type="button" disabled={!email || requestState.isLoading || Boolean(verificationToken)} onClick={() => void requestEmailCode()} className="min-h-11 rounded-lg border px-3 text-sm">인증번호 받기</button></div>
      {verificationUid !== null && !verificationToken && <div><label htmlFor="account-email-code" className="text-sm font-medium">인증번호</label><div className="flex flex-col gap-2 sm:flex-row"><input id="account-email-code" inputMode="numeric" autoComplete="one-time-code" maxLength={6} value={emailCode} onChange={(e) => setEmailCode(e.target.value.replace(/\D/g, "").slice(0, 6))} className={`${field} mt-0 min-w-0 flex-1`} /><button type="button" disabled={emailCode.length !== 6 || confirmState.isLoading} onClick={() => void confirmEmailCode()} className="min-h-11 rounded-lg border px-3 text-sm">인증하기</button></div></div>}
      {verificationToken && <button type="button" disabled={applyState.isLoading} onClick={() => void saveEmail()} className="min-h-11 rounded-lg bg-blue-600 px-4 text-sm text-white">인증된 이메일 저장</button>}</div>}
    {error && <p role="alert" className="mt-3 text-sm text-red-700">{error}</p>}{success && <p role="status" className="mt-3 text-sm text-green-700">{success}</p>}
  </section>;
}
