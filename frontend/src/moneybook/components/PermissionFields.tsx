"use client";

import { normalizePermissions, permissionLabels, type MemberPermissions } from "../permissions";

interface PermissionFieldsProps {
  value: MemberPermissions;
  onChange: (value: MemberPermissions) => void;
  idPrefix: string;
}

export default function PermissionFields({ value, onChange, idPrefix }: PermissionFieldsProps) {
  return (
    <fieldset className="space-y-3">
      <legend className="mb-2 text-sm font-medium">권한</legend>
      <label className="flex min-h-11 items-center gap-3 rounded-lg border border-zinc-200 px-3 py-2">
        <input id={`${idPrefix}-admin`} type="checkbox" checked={value.isAdmin}
          onChange={(event) => onChange(normalizePermissions({ ...value, isAdmin: event.target.checked }))} />
        <span>관리자 <span className="text-xs text-zinc-500">(모든 권한 포함)</span></span>
      </label>
      <div className="grid grid-cols-2 gap-2">
        {permissionLabels.map(([key, label]) => (
          <label key={key} className="flex min-h-11 items-center gap-2 rounded-lg border border-zinc-200 px-3 py-2">
            <input id={`${idPrefix}-${key}`} type="checkbox" checked={value[key]}
              disabled={value.isAdmin}
              onChange={(event) => onChange({ ...value, [key]: event.target.checked })} />
            <span>{label}</span>
          </label>
        ))}
      </div>
    </fieldset>
  );
}
