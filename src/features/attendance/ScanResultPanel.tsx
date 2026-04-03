import { currentMonthLabel } from "../../lib/utils/formatters";
import type { ScanAttendanceResult } from "../../types/app";

interface ScanResultPanelProps {
  result: ScanAttendanceResult;
  paymentStatus: "idle" | "paid" | "skipped";
  paymentLoading: boolean;
  onMarkPaid: () => void;
  onSkip: () => void;
  onDone: () => void;
}

export function ScanResultPanel({
  result,
  paymentStatus,
  paymentLoading,
  onMarkPaid,
  onSkip,
  onDone,
}: ScanResultPanelProps) {
  return (
    <div className="scan-result">
      <p className="eyebrow">{result.duplicate ? "Attendance already exists" : "Attendance marked"}</p>
      <h3>{result.student.full_name}</h3>
      <p>
        Billing period: <strong>{currentMonthLabel()}</strong>
      </p>

      <div className="scan-actions">
        <button
          className="button success"
          type="button"
          onClick={onMarkPaid}
          disabled={paymentLoading || paymentStatus === "paid"}
        >
          {paymentLoading ? "Saving..." : paymentStatus === "paid" ? "Paid" : "Mark as Paid"}
        </button>

        <button
          className="button secondary"
          type="button"
          onClick={onSkip}
          disabled={paymentLoading}
        >
          {paymentStatus === "skipped" ? "Skipped" : "Skip"}
        </button>

        <button className="button ghost" type="button" onClick={onDone}>
          Done
        </button>
      </div>
    </div>
  );
}
