import type { Metadata } from "next";
import ForgotPasswordForm from "@/auth/components/ForgotPasswordForm";
import { privatePageMetadata } from "@/common/seo/siteMetadata";

export const metadata: Metadata = privatePageMetadata();

export default function ForgotPasswordPage() {
  return <main className="flex flex-1 items-center justify-center bg-zinc-50 px-4 py-10 text-zinc-900"><section className="w-full max-w-lg rounded-xl border border-zinc-200 bg-white p-5 shadow-sm sm:p-8"><h1 className="mb-6 text-center text-2xl font-semibold">비밀번호 찾기</h1><ForgotPasswordForm /></section></main>;
}
