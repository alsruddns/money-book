export type AdPlacement = "moneybook-right" | "dashboard-inline" | "transaction-bottom" | "mobile-inline";

// Future ad integrations can render by placement. No slot reserves space until enabled.
export default function AdSlot({ placement }: { placement: AdPlacement }) {
  void placement;
  return null;
}
