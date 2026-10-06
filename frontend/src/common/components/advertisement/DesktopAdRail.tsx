import AdSlot from "./AdSlot";

export default function DesktopAdRail() {
  // Keep the rail absent until an ad is available; the content keeps the full column.
  const hasAd = false;
  if (!hasAd) return null;
  return <aside aria-label="광고" className="hidden w-72 2xl:block">
    <AdSlot placement="moneybook-right" />
  </aside>;
}
