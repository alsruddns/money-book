import AuthGuard from "@/auth/components/AuthGuard";

export default function AccountLayout({ children }: { children: React.ReactNode }) {
  return <AuthGuard>
    <main className="min-h-screen flex-1 bg-zinc-50 px-4 py-8 text-zinc-900 sm:px-6 sm:py-10">
      {children}
    </main>
  </AuthGuard>;
}
