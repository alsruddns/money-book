"use client";

import { useState, type FormEvent } from "react";
import { accountProviderLabel, accountRoleLabel, accountStatusLabel, formatAccountDateTime } from "../accountManagementLabels";
import { useAccountMe } from "../hooks/useAccountMe";
import { useUpdateAccountPassword } from "../hooks/useUpdateAccountPassword";
import { useUpdateAccountProfile } from "../hooks/useUpdateAccountProfile";
import { useWithdrawAccount } from "../hooks/useWithdrawAccount";
import SessionSection from "./SessionSection";

const emptyPassword = { currentPassword: "", newPassword: "", newPasswordConfirm: "" };
const fieldClass = "mt-1 min-h-11 w-full rounded-lg border border-zinc-300 bg-white px-3 py-2.5 text-zinc-900";

export default function AccountManagementView() {
  const { account, isLoading, isError, errorMessage, retry } = useAccountMe();
  if (isLoading) return <p role="status" className="py-12 text-center">계정 정보를 불러오는 중...</p>;
  if (isError || !account) return <section className="rounded-xl border border-red-200 bg-white p-5" role="alert">
    <p className="text-red-800">{errorMessage ?? "계정 정보를 불러오지 못했습니다."}</p>
    <button type="button" onClick={() => void retry()} className="mt-4 min-h-11 rounded-lg border border-zinc-300 px-4 text-sm font-medium">다시 시도</button>
  </section>;
  return <AccountDetailsView key={account.userUid} account={account} />;
}

function AccountDetailsView({ account }: { account: NonNullable<ReturnType<typeof useAccountMe>["account"]> }) {
  const profile = useUpdateAccountProfile();
  const password = useUpdateAccountPassword();
  const withdrawal = useWithdrawAccount();
  const [nickname, setNickname] = useState(account.nickname);
  const [profileSuccess, setProfileSuccess] = useState(false);
  const [passwordSuccess, setPasswordSuccess] = useState(false);
  const [passwordValues, setPasswordValues] = useState(emptyPassword);
  const [isWithdrawalOpen, setWithdrawalOpen] = useState(false);
  const [withdrawalPassword, setWithdrawalPassword] = useState("");

  async function submitProfile(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (profile.isLoading || !account || nickname.trim() === account.nickname) return;
    setProfileSuccess(false);
    if (await profile.updateNickname(nickname)) {
      setNickname(nickname.trim());
      setProfileSuccess(true);
    }
  }

  async function submitPassword(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (password.isLoading) return;
    setPasswordSuccess(false);
    if (await password.updatePassword(passwordValues)) {
      setPasswordValues({ ...emptyPassword });
      setPasswordSuccess(true);
    }
  }

  async function submitWithdrawal(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (withdrawal.isLoading || account.systemRole === "SUPER_ADMIN" || !hasLocalProvider) return;
    if (await withdrawal.withdraw(withdrawalPassword)) {
      setWithdrawalPassword("");
      setWithdrawalOpen(false);
    }
  }

  const hasLocalProvider = account.providers.includes("LOCAL");
  const nicknameUnchanged = nickname.trim() === account.nickname;

  return <div className="mx-auto w-full max-w-3xl space-y-6">
    <header>
      <h1 className="text-2xl font-semibold">계정 관리</h1>
      <p className="mt-1 text-sm text-zinc-600">내 계정 정보를 확인하고 닉네임과 비밀번호를 관리합니다.</p>
    </header>

    <section aria-labelledby="account-info-title" className="rounded-xl border border-zinc-200 bg-white p-5 sm:p-6">
      <h2 id="account-info-title" className="text-lg font-semibold">계정 정보</h2>
      <dl className="mt-4 grid gap-x-6 gap-y-4 sm:grid-cols-2">
        <div><dt className="text-sm text-zinc-600">사용자 UID</dt><dd className="mt-1 font-medium">{account.userUid}</dd></div>
        <div><dt className="text-sm text-zinc-600">닉네임</dt><dd className="mt-1 font-medium">{account.nickname}</dd></div>
        <div><dt className="text-sm text-zinc-600">로그인 ID</dt><dd className="mt-1 break-all font-medium">{account.loginId ?? "로컬 로그인 ID 없음"}</dd></div>
        <div><dt className="text-sm text-zinc-600">계정 상태</dt><dd className="mt-1 font-medium">{accountStatusLabel(account.status)}</dd></div>
        <div><dt className="text-sm text-zinc-600">시스템 역할</dt><dd className="mt-1 font-medium">{accountRoleLabel(account.systemRole)}</dd></div>
        <div><dt className="text-sm text-zinc-600">가입일</dt><dd className="mt-1 font-medium">{formatAccountDateTime(account.regTime)}</dd></div>
        <div><dt className="text-sm text-zinc-600">최근 수정</dt><dd className="mt-1 font-medium">{formatAccountDateTime(account.modTime)}</dd></div>
        <div className="sm:col-span-2"><dt className="text-sm text-zinc-600">인증 제공자</dt>
          <dd className="mt-2 flex flex-wrap gap-2">{account.providers.map((provider) => <span key={provider} className="rounded-full bg-zinc-100 px-3 py-1 text-sm">{accountProviderLabel(provider)}</span>)}</dd>
        </div>
      </dl>
    </section>

    <section aria-labelledby="nickname-title" className="rounded-xl border border-zinc-200 bg-white p-5 sm:p-6">
      <h2 id="nickname-title" className="text-lg font-semibold">닉네임 변경</h2>
      <form className="mt-4 space-y-4" onSubmit={submitProfile}>
        <div>
          <label htmlFor="account-nickname" className="text-sm font-medium">닉네임</label>
          <input id="account-nickname" name="nickname" autoComplete="nickname" maxLength={50} required value={nickname}
            onChange={(event) => { setNickname(event.target.value); setProfileSuccess(false); }}
            aria-describedby={profile.errorMessage ? "nickname-error" : undefined} className={fieldClass} />
        </div>
        {profile.errorMessage && <p id="nickname-error" role="alert" className="text-sm text-red-700">{profile.errorMessage}</p>}
        {profileSuccess && <p role="status" className="text-sm text-green-700">닉네임을 변경했습니다.</p>}
        <button type="submit" disabled={profile.isLoading || nicknameUnchanged}
          className="min-h-11 rounded-lg bg-blue-600 px-4 text-sm font-medium text-white disabled:cursor-not-allowed disabled:opacity-50">
          {profile.isLoading ? "변경 중..." : "닉네임 변경"}
        </button>
      </form>
    </section>

    <section aria-labelledby="password-title" className="rounded-xl border border-zinc-200 bg-white p-5 sm:p-6">
      <h2 id="password-title" className="text-lg font-semibold">비밀번호 변경</h2>
      {!hasLocalProvider ? <p className="mt-3 text-sm text-zinc-600">소셜 로그인 계정은 이 화면에서 비밀번호를 변경할 수 없습니다.</p> :
        <form className="mt-4 space-y-4" onSubmit={submitPassword}>
          <div><label htmlFor="current-password" className="text-sm font-medium">현재 비밀번호</label>
            <input id="current-password" name="currentPassword" type="password" autoComplete="current-password" maxLength={72} required value={passwordValues.currentPassword}
              onChange={(event) => { setPasswordValues((current) => ({ ...current, currentPassword: event.target.value })); setPasswordSuccess(false); }}
              aria-describedby={password.errorMessage ? "password-error" : undefined} className={fieldClass} /></div>
          <div><label htmlFor="new-password" className="text-sm font-medium">새 비밀번호</label>
            <input id="new-password" name="newPassword" type="password" autoComplete="new-password" maxLength={72} required value={passwordValues.newPassword}
              onChange={(event) => { setPasswordValues((current) => ({ ...current, newPassword: event.target.value })); setPasswordSuccess(false); }} className={fieldClass} /></div>
          <div><label htmlFor="new-password-confirm" className="text-sm font-medium">새 비밀번호 확인</label>
            <input id="new-password-confirm" name="newPasswordConfirm" type="password" autoComplete="new-password" maxLength={72} required value={passwordValues.newPasswordConfirm}
              onChange={(event) => { setPasswordValues((current) => ({ ...current, newPasswordConfirm: event.target.value })); setPasswordSuccess(false); }}
              aria-describedby={password.errorMessage ? "password-error" : undefined} className={fieldClass} /></div>
          {password.errorMessage && <p id="password-error" role="alert" className="text-sm text-red-700">{password.errorMessage}</p>}
          {passwordSuccess && <p role="status" className="text-sm text-green-700">비밀번호를 변경했습니다.</p>}
          <button type="submit" disabled={password.isLoading}
            className="min-h-11 rounded-lg bg-blue-600 px-4 text-sm font-medium text-white disabled:cursor-not-allowed disabled:opacity-50">
            {password.isLoading ? "변경 중..." : "비밀번호 변경"}
          </button>
        </form>}
    </section>

    <SessionSection />

    <section aria-labelledby="withdrawal-title" className="rounded-xl border border-red-300 bg-red-50 p-5 sm:p-6">
      <h2 id="withdrawal-title" className="text-lg font-semibold text-red-950">회원 탈퇴</h2>
      <p className="mt-2 text-sm text-red-900">탈퇴하면 로그인할 수 없고 가계부 멤버십과 초대가 정리됩니다. 소유 중인 가계부가 있으면 탈퇴 전에 다른 멤버에게 소유권을 이전해야 합니다.</p>
      {account.systemRole === "SUPER_ADMIN" ? <p className="mt-3 text-sm font-medium text-red-900">최고 관리자는 이 화면에서 탈퇴할 수 없습니다.</p> : !hasLocalProvider ?
        <p className="mt-3 text-sm text-red-900">현재 소셜 로그인 계정은 이 화면에서 탈퇴할 수 없습니다.</p> : <>
          <button type="button" onClick={() => { setWithdrawalPassword(""); setWithdrawalOpen(true); }} className="mt-4 min-h-11 rounded-lg border border-red-700 px-4 text-sm font-semibold text-red-900 hover:bg-red-100">회원 탈퇴 진행</button>
          {withdrawal.errorMessage && <p role="alert" className="mt-3 text-sm text-red-800">{withdrawal.errorMessage}</p>}
        </>}
    </section>

    {isWithdrawalOpen && hasLocalProvider && account.systemRole !== "SUPER_ADMIN" && <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4" role="presentation">
      <section role="dialog" aria-modal="true" aria-labelledby="withdrawal-confirm-title" className="w-full max-w-lg rounded-xl bg-white p-5 shadow-xl sm:p-6">
        <h2 id="withdrawal-confirm-title" className="text-xl font-semibold text-red-900">회원 탈퇴를 확인해주세요</h2>
        <p className="mt-3 text-sm text-zinc-700">계정은 탈퇴 처리되고 LOCAL 인증정보와 가계부 멤버십 및 초대가 삭제됩니다. 소유 중인 가계부가 있으면 탈퇴가 거절되므로 먼저 소유권을 이전해주세요. 활동 기록은 서버 정책에 따라 보존됩니다.</p>
        <form className="mt-5 space-y-4" onSubmit={submitWithdrawal}>
          <div>
            <label htmlFor="withdrawal-current-password" className="text-sm font-medium">현재 비밀번호</label>
            <input id="withdrawal-current-password" name="currentPassword" type="password" autoComplete="current-password" maxLength={72} required value={withdrawalPassword}
              onChange={(event) => setWithdrawalPassword(event.target.value)} className={fieldClass} />
            <p className="mt-1 text-xs text-zinc-600">현재 비밀번호로 본인 확인이 필요합니다.</p>
          </div>
          {withdrawal.errorMessage && <p role="alert" className="text-sm text-red-700">{withdrawal.errorMessage}</p>}
          <div className="flex flex-col-reverse gap-2 sm:flex-row sm:justify-end">
            <button type="button" disabled={withdrawal.isLoading} onClick={() => { setWithdrawalOpen(false); setWithdrawalPassword(""); }} className="min-h-11 rounded-lg border border-zinc-300 px-4 text-sm font-medium">취소</button>
            <button type="submit" disabled={withdrawal.isLoading || !withdrawalPassword.trim() || new TextEncoder().encode(withdrawalPassword).length > 72} className="min-h-11 rounded-lg bg-red-700 px-4 text-sm font-semibold text-white disabled:opacity-50">{withdrawal.isLoading ? "탈퇴 처리 중..." : "확인 후 탈퇴"}</button>
          </div>
        </form>
      </section>
    </div>}
  </div>;
}
