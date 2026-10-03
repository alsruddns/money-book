import AuthGuard from "@/auth/components/AuthGuard";
import Link from "next/link";
import { privatePageMetadata } from "@/common/seo/siteMetadata";
import type { Metadata } from "next";

export const metadata: Metadata = privatePageMetadata();

export default function AccountLayout({ children }: { children: React.ReactNode }) {
  return <AuthGuard>
    <main className="min-h-screen flex-1 bg-zinc-50 px-4 py-8 text-zinc-900 sm:px-6 sm:py-10">
      <div className="mx-auto w-full max-w-5xl">
        <Link href="/books" className="mb-5 inline-flex min-h-11 items-center rounded-lg border border-zinc-300 bg-white px-4 text-sm font-medium hover:bg-zinc-100">내 가계부로 돌아가기</Link>
        {children}
      </div>
    </main>
  </AuthGuard>;
}
