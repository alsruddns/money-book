"use client";

import { useState, type FormEvent } from "react";
import { accountProviderLabel, accountRoleLabel, accountStatusLabel, formatAccountDateTime } from "../accountManagementLabels";
import { useAccountMe } from "../hooks/useAccountMe";
import { useUpdateAccountPassword } from "../hooks/useUpdateAccountPassword";
import { useUpdateAccountProfile } from "../hooks/useUpdateAccountProfile";

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
  const [nickname, setNickname] = useState(account.nickname);
  const [profileSuccess, setProfileSuccess] = useState(false);
  const [passwordSuccess, setPasswordSuccess] = useState(false);
  const [passwordValues, setPasswordValues] = useState(emptyPassword);

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
  </div>;
}
