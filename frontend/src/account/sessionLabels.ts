export function describeSessionDevice(userAgent: string | null): string {
  if (!userAgent?.trim()) return "알 수 없는 기기";
  const value = userAgent.toLowerCase();
  const browser = value.includes("edg/") ? "Edge"
    : value.includes("firefox/") ? "Firefox"
      : value.includes("chrome/") || value.includes("chromium/") ? "Chrome"
        : value.includes("safari/") && value.includes("version/") ? "Safari" : null;
  const platform = /iphone|ipad|ipod/.test(value) ? "iPhone/iPad"
    : value.includes("android") ? "Android"
      : value.includes("windows") ? "Windows"
        : value.includes("macintosh") || value.includes("mac os") ? "macOS"
          : value.includes("linux") ? "Linux" : null;
  if (!browser && !platform) return "알 수 없는 기기";
  if (!browser) return platform ?? "알 수 없는 기기";
  return platform ? `${browser} · ${platform}` : browser;
}

export function formatSessionDateTime(value: string | null): string {
  if (!value) return "확인 불가";
  const normalized = /(?:Z|[+-]\d{2}:?\d{2})$/i.test(value) ? value : `${value}Z`;
  const date = new Date(normalized);
  if (Number.isNaN(date.getTime())) return "확인 불가";
  return new Intl.DateTimeFormat("ko-KR", {
    timeZone: "Asia/Seoul", year: "numeric", month: "2-digit", day: "2-digit",
    hour: "2-digit", minute: "2-digit", hourCycle: "h23",
  }).format(date);
}
