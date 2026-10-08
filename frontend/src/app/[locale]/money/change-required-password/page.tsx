import type { Metadata } from "next";
import RequiredPasswordChangeForm from "@/auth/components/RequiredPasswordChangeForm";
import { privatePageMetadata } from "@/common/seo/siteMetadata";

export const metadata: Metadata = privatePageMetadata();

export default function RequiredPasswordChangePage() {
  return <main className="flex flex-1 items-center justify-center bg-zinc-50 px-4 py-10 text-zinc-900"><section className="w-full max-w-md rounded-xl border border-zinc-200 bg-white p-6 shadow-sm sm:p-8"><h1 className="mb-6 text-center text-2xl font-semibold">비밀번호 변경</h1><RequiredPasswordChangeForm /></section></main>;
}
