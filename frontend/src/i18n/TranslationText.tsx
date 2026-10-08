"use client";

import { useTranslation } from "./useTranslation";

export default function TranslationText({ messageKey }: { messageKey: string }) {
  const { t } = useTranslation();
  return t(messageKey);
}
