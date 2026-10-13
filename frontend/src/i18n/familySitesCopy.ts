export const familySitesCopy = {
  ko: { label: "패밀리사이트", portal: "포탈", calculator: "계산기", tools: "온라인 도구", moneyBook: "무료 공유 가계부", newTab: "선택한 서비스가 새 탭에서 열립니다." },
  en: { label: "Family Sites", portal: "Portal", calculator: "Calculator", tools: "Online Tools", moneyBook: "Free Shared Money Book", newTab: "The selected service opens in a new tab." },
  ja: { label: "関連サービス", portal: "ポータル", calculator: "計算機", tools: "オンラインツール", moneyBook: "無料共有家計簿", newTab: "選択したサービスは新しいタブで開きます。" },
  zh: { label: "相关服务", portal: "门户", calculator: "计算器", tools: "在线工具", moneyBook: "免费共享记账本", newTab: "所选服务将在新标签页打开。" },
} as const;

export type FamilySitesCopy = (typeof familySitesCopy)[keyof typeof familySitesCopy];
