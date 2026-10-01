import SignupForm from "@/auth/components/SignupForm";

export default function SignupPage() {
  return (
    <main className="flex flex-1 items-center justify-center bg-zinc-50 px-4 py-10 text-zinc-900">
      <section className="w-full max-w-sm rounded-xl border border-zinc-200 bg-white p-6 shadow-sm sm:p-8">
        <h1 className="mb-6 text-center text-2xl font-semibold">회원가입</h1>
        <SignupForm />
      </section>
    </main>
  );
}
