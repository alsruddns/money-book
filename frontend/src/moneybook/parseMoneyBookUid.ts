export function parseMoneyBookUid(value: string): number | null {
  if (!/^[1-9]\d*$/.test(value)) return null;
  const uid = Number(value);
  return Number.isSafeInteger(uid) ? uid : null;
}
