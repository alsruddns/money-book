export interface PreparedDownload { objectUrl: string; filename: string }

export function parseDownloadFilename(disposition: string | null, fallback: string): string {
  if (!disposition) return fallback;
  const encoded = /filename\*=UTF-8''([^;]+)/i.exec(disposition)?.[1];
  const plain = /filename="?([^";]+)"?/i.exec(disposition)?.[1];
  let filename = plain?.trim();
  if (encoded) { try { filename = decodeURIComponent(encoded.trim()); } catch { filename = plain?.trim(); } }
  if (!filename) return fallback;
  filename = filename.split(/[\\/]/).pop()?.replace(/[\u0000-\u001f<>:"|?*]/g, "_") ?? "";
  return filename || fallback;
}

export async function prepareDownload(response: Response, fallback: string): Promise<PreparedDownload | { message: string }> {
  if (!response.ok) {
    const message = await response.text();
    return { message: message || `요청에 실패했습니다. (${response.status})` };
  }
  return { objectUrl: URL.createObjectURL(await response.blob()), filename: parseDownloadFilename(response.headers.get("Content-Disposition"), fallback) };
}

export function startDownload(file: PreparedDownload): void {
  const link = document.createElement("a");
  link.href = file.objectUrl;
  link.download = file.filename;
  document.body.appendChild(link);
  link.click();
  link.remove();
  window.setTimeout(() => URL.revokeObjectURL(file.objectUrl), 0);
}
