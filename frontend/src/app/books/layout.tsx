import Link from "next/link";
import AuthGuard from "@/auth/components/AuthGuard";

export default function BooksLayout({ children }: { children: React.ReactNode }) {
  return (
    <AuthGuard>
      <div className="min-h-screen flex-1 bg-zinc-50 text-zinc-900">
        <header className="border-b border-zinc-200 bg-white">
          <nav aria-label="가계부 메뉴" className="mx-auto flex w-full max-w-6xl flex-wrap items-center justify-between gap-3 px-4 py-4 sm:px-6">
            <Link href="/books" className="text-lg font-semibold">가계부</Link>
            <div className="flex gap-4 text-sm font-medium">
              <Link href="/books" className="hover:text-blue-700">내 가계부</Link>
              <Link href="/books/invitations" className="hover:text-blue-700">받은 초대</Link>
            </div>
          </nav>
        </header>
        <main className="mx-auto w-full max-w-screen-2xl min-w-0 px-4 py-7 sm:px-6 sm:py-10">{children}</main>
      </div>
    </AuthGuard>
  );
}
