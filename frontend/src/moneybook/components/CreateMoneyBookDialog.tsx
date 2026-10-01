"use client";

import { useState, type FormEvent } from "react";
import { useCreateMoneyBook } from "../hooks/useCreateMoneyBook";
import DialogShell from "./DialogShell";

export default function CreateMoneyBookDialog({ onClose }: { onClose: () => void }) {
  const [name, setName] = useState("");
  const { createMoneyBook, isLoading, errorMessage } = useCreateMoneyBook();

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (isLoading) return;
    if (await createMoneyBook(name)) onClose();
  }

  return (
    <DialogShell title="새 가계부 만들기" onClose={onClose}>
      <form onSubmit={handleSubmit} className="space-y-5">
        <div>
          <label htmlFor="money-book-name" className="mb-1 block text-sm font-medium">가계부 이름</label>
          <input id="money-book-name" name="name" required maxLength={100} value={name}
            onChange={(event) => setName(event.target.value)} autoFocus
            className="w-full rounded-lg border border-zinc-300 px-3 py-2.5 outline-none focus:border-blue-600" />
        </div>
        {errorMessage && <p role="alert" className="text-sm text-red-600">{errorMessage}</p>}
        <button type="submit" disabled={isLoading}
          className="min-h-11 w-full rounded-lg bg-blue-600 px-4 font-medium text-white disabled:opacity-60">
          {isLoading ? "만드는 중..." : "만들기"}
        </button>
      </form>
    </DialogShell>
  );
}
