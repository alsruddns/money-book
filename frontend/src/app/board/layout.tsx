import AuthGuard from "@/auth/components/AuthGuard";
export default function BoardLayout({ children }: { children: React.ReactNode }) { return <AuthGuard><main className="min-w-0 flex-1 bg-zinc-50 px-4 py-7 text-zinc-900 sm:px-6 sm:py-10"><div className="mx-auto w-full max-w-screen-xl">{children}</div></main></AuthGuard>; }
