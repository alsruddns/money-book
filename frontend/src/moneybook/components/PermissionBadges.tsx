import { permissionSummary, type MemberPermissions } from "../permissions";

export default function PermissionBadges({ permissions }: { permissions: MemberPermissions }) {
  return (
    <div className="flex flex-wrap gap-1.5" aria-label="권한">
      {permissionSummary(permissions).map((label) => (
        <span key={label} className="rounded-full bg-blue-50 px-2.5 py-1 text-xs font-medium text-blue-800">
          {label}
        </span>
      ))}
    </div>
  );
}
