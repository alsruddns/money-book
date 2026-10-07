import Link from "../../common/components/MoneyLink";
import type { MoneyBookListResponse } from "../dto/res/MoneyBookListResponse";
import PermissionBadges from "./PermissionBadges";

export default function MoneyBookCard({ moneyBook }: { moneyBook: MoneyBookListResponse }) {
  return (
    <Link href={`/books/${moneyBook.moneyBookUid}`}
      className="block rounded-xl border border-zinc-200 bg-white p-5 shadow-sm transition hover:border-blue-300 hover:shadow-md">
      <div className="mb-3 flex items-start justify-between gap-3">
        <h2 className="text-lg font-semibold break-words">{moneyBook.name}</h2>
        {moneyBook.isOwner && <span className="shrink-0 rounded-full bg-amber-100 px-2.5 py-1 text-xs font-medium text-amber-900">소유자</span>}
      </div>
      <PermissionBadges permissions={moneyBook} />
      <span className="mt-4 block text-sm font-medium text-blue-700">가계부 열기 →</span>
    </Link>
  );
}
