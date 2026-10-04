"use client";

import { useEffect, useId, useRef, type ReactNode } from "react";
import { acquireBodyScrollLock } from "./bodyScrollLock";

interface DialogShellProps {
  title: string;
  description?: string;
  onClose: () => void;
  children: ReactNode;
  size?: "default" | "wide";
}

export default function DialogShell({ title, description, onClose, children, size = "default" }: DialogShellProps) {
  const dialogRef = useRef<HTMLElement>(null);
  const closeButtonRef = useRef<HTMLButtonElement>(null);
  const onCloseRef = useRef(onClose);
  const titleId = useId();

  useEffect(() => {
    onCloseRef.current = onClose;
  }, [onClose]);

  useEffect(() => {
    const previouslyFocused = document.activeElement instanceof HTMLElement ? document.activeElement : null;
    const releaseScrollLock = acquireBodyScrollLock();
    closeButtonRef.current?.focus();

    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === "Escape") {
        event.preventDefault();
        onCloseRef.current();
        return;
      }
      if (event.key !== "Tab" || !dialogRef.current) return;
      const focusable = dialogRef.current.querySelectorAll<HTMLElement>(
        'a[href], button:not([disabled]), input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])',
      );
      if (!focusable.length) {
        event.preventDefault();
        dialogRef.current.focus();
        return;
      }
      const first = focusable[0];
      const last = focusable[focusable.length - 1];
      if (event.shiftKey && document.activeElement === first) {
        event.preventDefault();
        last.focus();
      } else if (!event.shiftKey && document.activeElement === last) {
        event.preventDefault();
        first.focus();
      }
    }

    document.addEventListener("keydown", handleKeyDown);
    return () => {
      document.removeEventListener("keydown", handleKeyDown);
      releaseScrollLock();
      previouslyFocused?.focus();
    };
  }, []);

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4" onMouseDown={(event) => { if (event.target === event.currentTarget) onCloseRef.current(); }}>
      <section ref={dialogRef} role="dialog" aria-modal="true" aria-labelledby={titleId} aria-describedby={description ? `${titleId}-description` : undefined} tabIndex={-1}
        className={`max-h-[90dvh] w-full ${size === "wide" ? "max-w-3xl" : "max-w-md"} overflow-y-auto rounded-xl bg-white p-5 text-zinc-900 shadow-xl sm:p-6`}>
        <div className="mb-5 flex items-center justify-between gap-3">
          <div><h2 id={titleId} className="text-xl font-semibold">{title}</h2>{description && <p id={`${titleId}-description`} className="mt-2 text-sm text-zinc-600">{description}</p>}</div>
          <button ref={closeButtonRef} type="button" onClick={onClose} aria-label="닫기"
            className="min-h-11 shrink-0 rounded-lg px-3 py-2 text-zinc-600 hover:bg-zinc-100 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-600">닫기</button>
        </div>
        {children}
      </section>
    </div>
  );
}
