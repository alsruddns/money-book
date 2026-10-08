"use client";

import Link from "./MoneyLink";
import { useTranslation } from "@/i18n/useTranslation";

export default function NotFoundPageContent() {
  const { t } = useTranslation();
  return (
    <main className="flex min-h-[50vh] flex-1 items-center justify-center bg-zinc-50 px-4 py-12 text-zinc-900">
      <section className="w-full max-w-lg rounded-2xl border border-zinc-200 bg-white p-7 text-center shadow-sm sm:p-10">
        <p className="text-sm font-semibold text-zinc-500">404</p>
        <h1 className="mt-2 text-2xl font-bold">{t("errors.notFound")}</h1>
        <p className="mt-3 text-sm leading-6 text-zinc-600">{t("errors.notFoundDescription")}</p>
        <Link href="/books" className="mt-7 inline-flex min-h-11 items-center justify-center rounded-lg bg-blue-700 px-5 text-sm font-semibold text-white hover:bg-blue-800 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-600 focus-visible:ring-offset-2">
          {t("errors.backToBooks")}
        </Link>
      </section>
    </main>
  );
}
