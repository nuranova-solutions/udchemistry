import { cn } from "../../lib/utils/cn";

export function StatusBadge({ label, tone = "neutral" }: { label: string; tone?: "neutral" | "success" | "warning" }) {
  return <span className={cn("status-badge", `status-${tone}`)}>{label}</span>;
}
