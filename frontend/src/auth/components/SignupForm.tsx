"use client";

import { useState, type FormEvent } from "react";
import Link from "../../common/components/MoneyLink";
import { useSignup } from "../hooks/useSignup";
import { useConfirmEmailVerificationMutation, useGetSecurityQuestionsQuery, useRequestEmailVerificationMutation } from "../controller/passwordRecoveryApi";
import type { SignUpReqDto } from "../dto/req/SignUpReqDto";
import { getApiErrorMessage } from "@/common/api/getApiErrorMessage";
import { copyText } from "@/common/utils/copyText";
import { hasSecurityAnswerBoundaryWhitespace } from "../securityAnswerValidation";
import { useTranslation } from "@/i18n/useTranslation";

const blank: SignUpReqDto = { loginId: "", password: "", passwordConfirm: "", nickname: "", securityQuestionCode: "", securityAnswer: "" };
const inputClass = "mt-1 min-h-11 w-full rounded-lg border border-zinc-300 bg-white px-3 py-2 text-zinc-900 outline-none focus:border-blue-600 focus:ring-2 focus:ring-blue-100";

export default function SignupForm() {
  const { t } = useTranslation();
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
  const [securityAnswerError, setSecurityAnswerError] = useState("");
  const [duplicateEmailError, setDuplicateEmailError] = useState(false);
  const [showUnverifiedEmailError, setShowUnverifiedEmailError] = useState(false);
  const [recoveryCodes, setRecoveryCodes] = useState<string[] | null>(null);
  const [copied, setCopied] = useState(false);
  const [copyError, setCopyError] = useState(false);
  const [done, setDone] = useState(false);

  function updateField(field: keyof SignUpReqDto, value: string) { setForm((current) => ({ ...current, [field]: value })); }
  async function sendCode() {
    setShowUnverifiedEmailError(false); setVerificationUid(null); setCode("");
    setVerificationError(""); setVerificationMessage(""); setVerificationToken("");
    try { const result = await requestCode({ email, purpose: "SIGNUP" }).unwrap(); requestState.reset(); setVerificationUid(result.verificationUid); setVerificationMessage(t("signup.codeSent")); }
    catch (error) { setVerificationError(getApiErrorMessage(error, t("signup.requestFailed"))); requestState.reset(); }
  }
  async function verifyCode() {
    if (verificationUid === null) return;
    setVerificationError("");
    try { const result = await confirmCode({ verificationUid, code }).unwrap(); confirmState.reset(); setCode(""); setVerificationToken(result.verificationToken); setVerificationMessage(t("signup.emailVerified")); }
    catch (error) { setVerificationError(getApiErrorMessage(error, t("signup.verifyFailed"))); confirmState.reset(); }
  }
  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (hasSecurityAnswerBoundaryWhitespace(form.securityAnswer)) {
      setSecurityAnswerError(t("signup.invalidSecurityAnswer"));
      document.getElementById("security-answer")?.focus();
      return;
    }
    setSecurityAnswerError("");
    if (email.trim() && !verificationToken) {
      setVerificationError("");
      setShowUnverifiedEmailError(true);
      document.getElementById("signup-email")?.focus();
      return;
    }
    setShowUnverifiedEmailError(false);
    setDuplicateEmailError(false);
    const result = await signup(
      { ...form, ...(verificationToken ? { emailVerificationToken: verificationToken } : {}) },
      () => {
        setDuplicateEmailError(true);
        document.getElementById("signup-email")?.focus();
      },
    );
    if (result) setRecoveryCodes(result.recoveryCodes ?? []);
  }

  if (done) return <section className="space-y-5 text-center" aria-labelledby="signup-done-title">
    <h2 id="signup-done-title" className="text-xl font-semibold">{t("signup.done")}</h2>
    <p className="text-sm text-zinc-600">{t("signup.loginNext")}</p>
    <Link href="/login" className="inline-flex min-h-11 items-center rounded-lg bg-blue-600 px-5 font-medium text-white">{t("auth.login")}</Link>
  </section>;

  if (recoveryCodes) return <section className="space-y-5" aria-labelledby="recovery-codes-title">
    <h2 id="recovery-codes-title" className="text-xl font-semibold">{t("signup.recoveryTitle")}</h2>
    {recoveryCodes.length ? <ul className="grid gap-2 rounded-lg bg-zinc-50 p-4 font-mono text-sm sm:grid-cols-2">{recoveryCodes.map((value) => <li key={value} className="break-all">{value}</li>)}</ul> : <p role="alert" className="text-sm text-red-700">{t("signup.recoveryUnavailable")}</p>}
    <button type="button" disabled={!recoveryCodes.length} onClick={async () => { const success = await copyText(recoveryCodes.join("\n")); setCopied(success); setCopyError(!success); }} className="min-h-11 w-full rounded-lg border px-4 font-medium">{copied ? t("signup.copied") : t("signup.copyAll")}</button>
    {copyError && <p role="alert" className="text-sm text-red-700">{t("signup.copyFailed")}</p>}
    <div className="space-y-1 text-sm text-zinc-600"><p>{t("signup.oneTime")}</p><p>{t("signup.recoveryHelp")}</p><p>{t("signup.storeCodes")}</p></div>
    <button type="button" onClick={() => { setRecoveryCodes(null); setForm(blank); setVerificationToken(""); reset(); setDone(true); }} className="min-h-11 w-full rounded-lg bg-blue-600 px-4 font-medium text-white">{t("signup.stored")}</button>
  </section>;

  return <form onSubmit={handleSubmit} className="space-y-5">
    {(["loginId", "password", "passwordConfirm", "nickname"] as const).map((key) => {
      const meta = { loginId: [t("auth.loginId"), "text", "username"], password: [t("auth.password"), "password", "new-password"], passwordConfirm: [t("auth.passwordConfirm"), "password", "new-password"], nickname: [t("auth.nickname"), "text", "nickname"] }[key];
      return <div key={key}><label htmlFor={key} className="text-sm font-medium">{meta[0]} *</label><input id={key} name={key} type={meta[1]} autoComplete={meta[2]} required value={form[key]} onChange={(event) => updateField(key, event.target.value)} className={inputClass} /></div>;
    })}
    <fieldset className="space-y-3 rounded-lg border border-zinc-200 p-4">
      <legend className="px-1 text-sm font-semibold">{t("signup.recoveryInfo")}</legend>
      <p className="text-sm text-zinc-600">{t("signup.recoveryInfoHelp")}</p>
      {questions.isLoading ? <p role="status" className="text-sm">{t("signup.questionLoading")}</p> : questions.isError ? <p role="alert" className="text-sm text-red-700">{t("signup.questionError")}</p> : <>
        <div><label htmlFor="security-question" className="text-sm font-medium">{t("signup.securityQuestion")} *</label><select id="security-question" required value={form.securityQuestionCode} onChange={(event) => updateField("securityQuestionCode", event.target.value)} className={inputClass}><option value="">{t("signup.chooseQuestion")}</option>{questions.data?.map((item) => <option key={item.code} value={item.code}>{item.question}</option>)}</select></div>
        <div><label htmlFor="security-answer" className="text-sm font-medium">{t("signup.answer")} *</label><input id="security-answer" name="securityAnswer" type="text" autoComplete="off" required maxLength={128} value={form.securityAnswer} aria-invalid={Boolean(securityAnswerError)} aria-describedby={securityAnswerError ? "security-answer-error" : undefined} onChange={(event) => { updateField("securityAnswer", event.target.value); setSecurityAnswerError(""); }} className={inputClass} />{securityAnswerError && <p id="security-answer-error" role="alert" className="mt-1 text-sm text-red-700">{securityAnswerError}</p>}</div>
      </>}
    </fieldset>
    <fieldset className="space-y-3 rounded-lg border border-zinc-200 p-4">
      <legend className="px-1 text-sm font-semibold">{t("signup.optionalEmail")}</legend>
      <p className="text-sm text-zinc-600">{t("signup.emailHelp")}</p>
      <label htmlFor="signup-email" className="text-sm font-medium">{t("signup.emailAddress")}</label>
      <div className="flex flex-col gap-2 sm:flex-row"><input id="signup-email" type="email" autoComplete="email" value={email} aria-invalid={duplicateEmailError} aria-describedby={duplicateEmailError ? "signup-email-duplicate-error" : undefined} onChange={(event) => { setEmail(event.target.value); setDuplicateEmailError(false); setVerificationToken(""); setVerificationUid(null); setCode(""); setVerificationMessage(""); setVerificationError(""); setShowUnverifiedEmailError(false); }} className={`${inputClass} mt-0 min-w-0 flex-1`} /><button type="button" disabled={!email || requestState.isLoading || Boolean(verificationToken)} onClick={() => void sendCode()} className="min-h-11 shrink-0 rounded-lg border px-3 text-sm font-medium disabled:opacity-50">{requestState.isLoading ? t("signup.requesting") : t("signup.requestCode")}</button></div>
      {duplicateEmailError && <p id="signup-email-duplicate-error" role="alert" className="text-sm text-red-700">{t("signup.duplicateEmail")}</p>}
      {verificationUid !== null && !verificationToken && <div><label htmlFor="signup-email-code" className="text-sm font-medium">{t("signup.verificationCode")}</label><div className="flex flex-col gap-2 sm:flex-row"><input id="signup-email-code" inputMode="numeric" autoComplete="one-time-code" maxLength={6} value={code} onChange={(event) => setCode(event.target.value.replace(/\D/g, "").slice(0, 6))} className={`${inputClass} mt-0 min-w-0 flex-1`} /><button type="button" disabled={code.length !== 6 || confirmState.isLoading} onClick={() => void verifyCode()} className="min-h-11 shrink-0 rounded-lg border px-3 text-sm font-medium disabled:opacity-50">{confirmState.isLoading ? t("signup.verifying") : t("signup.verify")}</button></div></div>}
      {verificationMessage && <p role="status" className="text-sm text-green-700">{verificationMessage}</p>}{verificationError && <p role="alert" className="text-sm text-red-700">{verificationError}</p>}
      {!email ? <p className="text-xs text-zinc-500">{t("signup.noEmailTip")}</p> : showUnverifiedEmailError && !verificationToken && <p role="alert" className="text-sm text-red-700">{t("signup.emailRequired")}</p>}
    </fieldset>
    {errorMessage && !duplicateEmailError && <p role="alert" className="text-sm text-red-600">{errorMessage}</p>}
    <button type="submit" disabled={isLoading || questions.isLoading || questions.isError || Boolean(email && !verificationToken)} className="min-h-11 w-full rounded-lg bg-blue-600 px-4 font-medium text-white hover:bg-blue-700 disabled:cursor-not-allowed disabled:opacity-60">{isLoading ? t("auth.signingUp") : t("signup.signup")}</button>
    <p className="text-center text-sm text-zinc-600">{t("signup.existingAccount")} <Link href="/login" className="font-medium text-blue-700 hover:underline">{t("auth.login")}</Link></p>
  </form>;
}
