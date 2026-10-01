import AuthGuard from "@/auth/components/AuthGuard";

export default function BooksPage() {
  return (
    <AuthGuard>
      <main className="mx-auto w-full max-w-4xl flex-1 px-4 py-10 text-zinc-900">
        <h1 className="text-2xl font-semibold">가계부</h1>
      </main>
    </AuthGuard>
  );
}
