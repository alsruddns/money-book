import AuthGuard from "@/auth/components/AuthGuard";

export default function BooksLayout({ children }: { children: React.ReactNode }) {
  return (
    <AuthGuard>
      <div className="min-w-0 flex-1 bg-zinc-50 text-zinc-900">
        <main className="mx-auto w-full max-w-screen-2xl min-w-0 px-4 py-7 sm:px-6 sm:py-10">{children}</main>
      </div>
    </AuthGuard>
  );
}
