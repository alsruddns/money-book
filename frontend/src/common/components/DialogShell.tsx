"use client";

import type { ReactNode } from "react";

interface DialogShellProps {
  title: string;
  onClose: () => void;
  children: ReactNode;
}

export default function DialogShell({ title, onClose, children }: DialogShellProps) {
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
      <section role="dialog" aria-modal="true" aria-label={title}
        className="max-h-[90vh] w-full max-w-md overflow-y-auto rounded-xl bg-white p-5 text-zinc-900 shadow-xl sm:p-6">
        <div className="mb-5 flex items-center justify-between gap-3">
          <h2 className="text-xl font-semibold">{title}</h2>
          <button type="button" onClick={onClose} aria-label="닫기"
            className="rounded-lg px-3 py-2 text-zinc-600 hover:bg-zinc-100">닫기</button>
        </div>
        {children}
      </section>
    </div>
  );
}
