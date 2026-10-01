import LoginForm from "@/auth/components/LoginForm";
import GuestGuard from "@/auth/components/GuestGuard";

export default function LoginPage() {
  return (
    <GuestGuard>
      <main className="flex flex-1 items-center justify-center bg-zinc-50 px-4 py-10 text-zinc-900">
        <section className="w-full max-w-sm rounded-xl border border-zinc-200 bg-white p-6 shadow-sm sm:p-8">
          <h1 className="mb-6 text-center text-2xl font-semibold">로그인</h1>
          <LoginForm />
        </section>
      </main>
    </GuestGuard>
  );
}
