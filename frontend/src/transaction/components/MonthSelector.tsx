"use client";
import { localeIntl } from "@/i18n/config";
import { useTranslation } from "@/i18n/useTranslation";

export default function MonthSelector({ year, month, onPrevious, onNext, allowPrevious, allowNext }: {
  year: number; month: number; onPrevious: () => void; onNext: () => void; allowPrevious?: boolean; allowNext?: boolean;
}) {
  const { locale, t } = useTranslation();
  const selectedMonth = new Intl.DateTimeFormat(localeIntl[locale], { year: "numeric", month: "long" }).format(new Date(year, month - 1, 1));
  return (
    <div className="flex items-center gap-2" aria-label={t("calendar.selectMonth")}>
      <button type="button" onClick={onPrevious} disabled={allowPrevious === false || year === 1 && month === 1}
        aria-label={t("calendar.previousMonth")} className="min-h-11 rounded-lg border border-zinc-300 bg-white px-3 text-sm disabled:opacity-50">〈</button>
      <span className="min-w-28 text-center font-semibold">{selectedMonth}</span>
      <button type="button" onClick={onNext} disabled={allowNext === false || year === 9999 && month === 12}
        aria-label={t("calendar.nextMonth")} className="min-h-11 rounded-lg border border-zinc-300 bg-white px-3 text-sm disabled:opacity-50">〉</button>
    </div>
  );
}
