"use client";

import { useMoneyRouter } from "../../common/components/useMoneyRouter";

import { useEffect, useState, type FormEvent } from "react";

import { useCurrentUser } from "../hooks/useCurrentUser";
import { useUpdateAccountPassword } from "@/account/hooks/useUpdateAccountPassword";

export default function RequiredPasswordChangeForm() {
  const router = useMoneyRouter();
  const auth = useCurrentUser();
  const password = useUpdateAccountPassword();
  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [newPasswordConfirm, setNewPasswordConfirm] = useState("");
  useEffect(() => {
    if (!auth.isLoading && !auth.isAuthenticated) router.replace("/login");
    else if (!auth.isLoading && auth.isAuthenticated && !auth.currentUser?.passwordChangeRequired) router.replace("/books");
  }, [auth.isLoading, auth.isAuthenticated, auth.currentUser?.passwordChangeRequired, router]);
  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    await password.updatePassword({ currentPassword, newPassword, newPasswordConfirm });
  }
  if (auth.isLoading || !auth.isAuthenticated || !auth.currentUser?.passwordChangeRequired) return <p role="status" className="text-center">계정 상태를 확인하고 있습니다...</p>;
  return <form onSubmit={submit} className="space-y-4">
    <p className="text-sm text-zinc-600">임시 비밀번호로 로그인했습니다. 서비스를 계속 사용하려면 새 비밀번호를 설정해 주세요.</p>
    <div><label htmlFor="temporary-password" className="text-sm font-medium">현재 임시 비밀번호</label><input id="temporary-password" type="password" autoComplete="current-password" maxLength={72} required value={currentPassword} onChange={(e) => setCurrentPassword(e.target.value)} className="mt-1 min-h-11 w-full rounded-lg border border-zinc-300 px-3 py-2" /></div>
    <div><label htmlFor="required-new-password" className="text-sm font-medium">새 비밀번호</label><input id="required-new-password" type="password" autoComplete="new-password" maxLength={72} required value={newPassword} onChange={(e) => setNewPassword(e.target.value)} className="mt-1 min-h-11 w-full rounded-lg border border-zinc-300 px-3 py-2" /></div>
    <div><label htmlFor="required-new-password-confirm" className="text-sm font-medium">새 비밀번호 확인</label><input id="required-new-password-confirm" type="password" autoComplete="new-password" maxLength={72} required value={newPasswordConfirm} onChange={(e) => setNewPasswordConfirm(e.target.value)} className="mt-1 min-h-11 w-full rounded-lg border border-zinc-300 px-3 py-2" /></div>
    {password.errorMessage && <p role="alert" className="text-sm text-red-700">{password.errorMessage}</p>}
    <button type="submit" disabled={password.isLoading} className="min-h-11 w-full rounded-lg bg-blue-600 px-4 font-medium text-white disabled:opacity-50">{password.isLoading ? "변경 중..." : "비밀번호 변경"}</button>
  </form>;
}
