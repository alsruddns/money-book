import LoginForm from "@/auth/components/LoginForm";
import GuestGuard from "@/auth/components/GuestGuard";

const loginNotices: Record<string, string> = {
  "password-changed": "비밀번호가 변경되었습니다. 다시 로그인해주세요.",
  "logout-incomplete": "이 기기에서는 로그아웃했습니다. 서버 연결 문제로 원격 세션 종료는 확인되지 않았습니다.",
  "logged-out": "로그아웃했습니다.",
  "sessions-ended": "모든 기기에서 로그아웃했습니다.",
};

export default async function LoginPage({ searchParams }: { searchParams?: Promise<{ reason?: string | string[] }> }) {
  const params = await searchParams;
  const reason = typeof params?.reason === "string" ? params.reason : "";
  return (
    <GuestGuard>
      <main className="flex flex-1 items-center justify-center bg-zinc-50 px-4 py-10 text-zinc-900">
        <section className="w-full max-w-sm rounded-xl border border-zinc-200 bg-white p-6 shadow-sm sm:p-8">
          <h1 className="mb-6 text-center text-2xl font-semibold">로그인</h1>
          <LoginForm notice={loginNotices[reason]} />
        </section>
      </main>
    </GuestGuard>
  );
}
