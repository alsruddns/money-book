"use client";

export default function MonthSelector({ year, month, onPrevious, onNext, allowPrevious, allowNext }: {
  year: number; month: number; onPrevious: () => void; onNext: () => void; allowPrevious?: boolean; allowNext?: boolean;
}) {
  return (
    <div className="flex items-center gap-2" aria-label="조회 월 선택">
      <button type="button" onClick={onPrevious} disabled={allowPrevious === false || year === 1 && month === 1}
        aria-label="이전 달" className="min-h-11 rounded-lg border border-zinc-300 bg-white px-3 text-sm disabled:opacity-50">〈 이전</button>
      <span className="min-w-28 text-center font-semibold">{year}년 {month}월</span>
      <button type="button" onClick={onNext} disabled={allowNext === false || year === 9999 && month === 12}
        aria-label="다음 달" className="min-h-11 rounded-lg border border-zinc-300 bg-white px-3 text-sm disabled:opacity-50">다음 〉</button>
    </div>
  );
}
