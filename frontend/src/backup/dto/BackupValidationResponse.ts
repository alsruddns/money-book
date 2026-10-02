export interface BackupValidationResponse {
  valid: boolean; backupVersion: string | null; moneyBookName: string | null;
  categoryCount: number; accountCount: number; transactionCount: number; transferCount: number;
  recurringTransactionCount: number; budgetCount: number; monthClosingCount: number;
  firstTransactionDate: string | null; lastTransactionDate: string | null; warnings: string[]; errors: string[];
}
