import LoginForm from "@/auth/components/LoginForm";
import GuestGuard from "@/auth/components/GuestGuard";
import { privatePageMetadata } from "@/common/seo/siteMetadata";
import type { Metadata } from "next";
import TranslationText from "@/i18n/TranslationText";
import { translate } from "@/i18n/messages";
import { isLocale } from "@/i18n/config";

export const metadata: Metadata = privatePageMetadata();

const loginNotices: Record<string, string> = {
  "password-changed": "비밀번호가 변경되었습니다. 다시 로그인해주세요.",
  "logout-incomplete": "이 기기에서는 로그아웃했습니다. 서버 연결 문제로 원격 세션 종료는 확인되지 않았습니다.",
  "logged-out": "로그아웃했습니다.",
  "sessions-ended": "모든 기기에서 로그아웃했습니다.",
};

export default async function LoginPage({ params, searchParams }: { params: Promise<{ locale: string }>; searchParams?: Promise<{ reason?: string | string[] }> }) {
  const route = await params;
  const queryParams = await searchParams;
  const reason = typeof queryParams?.reason === "string" ? queryParams.reason : "";
  const locale = isLocale(route.locale) ? route.locale : "ko";
  const notice = reason === "idle" ? translate(locale, "idleSession.logoutReason")
    : reason === "session-expired" ? translate(locale, "idleSession.sessionExpired")
      : loginNotices[reason];
  return (
    <GuestGuard>
      <main className="flex flex-1 items-center justify-center bg-zinc-50 px-4 py-10 text-zinc-900">
        <section className="w-full max-w-sm rounded-xl border border-zinc-200 bg-white p-6 shadow-sm sm:p-8">
          <h1 className="mb-6 text-center text-2xl font-semibold"><TranslationText messageKey="auth.login" /></h1>
          <LoginForm notice={notice} />
        </section>
      </main>
    </GuestGuard>
  );
}
