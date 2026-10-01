export function validateNamedItem(name: string, sortOrder: number): string | null {
  if (!name.trim()) return "이름을 입력해 주세요.";
  if (!Number.isSafeInteger(sortOrder) || sortOrder < 0) return "정렬 순서는 0 이상의 정수여야 합니다.";
  return null;
}
