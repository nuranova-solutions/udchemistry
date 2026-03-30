import { useEffect, useMemo, useState } from "react";
import {
  Bar,
  BarChart,
  CartesianGrid,
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import { AnimatePresence, motion } from "framer-motion";
import { X } from "lucide-react";
import { SectionCard } from "../../components/ui/SectionCard";
import { StatusBadge } from "../../components/ui/StatusBadge";
import { formatCurrency, formatDate } from "../../lib/utils/formatters";
import type { AttendanceRecord, PaymentRecord, Student } from "../../types/app";
import { QrLinkButton } from "./QrLinkButton";
import { QrShareButton } from "./QrShareButton";
import {
  attendanceForMonth,
  buildStudentMonthOptions,
  countAttendedClasses,
  currentMonthKey,
  paymentsForMonth,
  studentAttendanceTrend,
  studentPaymentTrend,
} from "./studentInsights";

interface StudentDetailPanelProps {
  student: Student | null;
  instituteName: string;
  attendance: AttendanceRecord[];
  payments: PaymentRecord[];
  loading: boolean;
  onClose: () => void;
}

export function StudentDetailPanel({
  student,
  instituteName,
  attendance,
  payments,
  loading,
  onClose,
}: StudentDetailPanelProps) {
  const studentAttendance = useMemo(
    () => attendance.filter((record) => record.student_id === student?.id),
    [attendance, student?.id],
  );
  const studentPayments = useMemo(
    () => payments.filter((record) => record.student_id === student?.id),
    [payments, student?.id],
  );
  const monthOptions = useMemo(() => {
    if (!student) {
      return [];
    }

    return buildStudentMonthOptions(student, studentAttendance, studentPayments);
  }, [student, studentAttendance, studentPayments]);

  const defaultMonthKey =
    monthOptions.find((option) => option.key === currentMonthKey())?.key ?? monthOptions[0]?.key ?? "";

  const [selectedMonthKey, setSelectedMonthKey] = useState(defaultMonthKey);

  useEffect(() => {
    if (!student) {
      return;
    }

    setSelectedMonthKey(defaultMonthKey);
  }, [defaultMonthKey, student]);

  useEffect(() => {
    if (!student) {
      return;
    }

    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === "Escape") {
        onClose();
      }
    }

    document.body.style.overflow = "hidden";
    window.addEventListener("keydown", handleKeyDown);

    return () => {
      document.body.style.overflow = "";
      window.removeEventListener("keydown", handleKeyDown);
    };
  }, [onClose, student]);

  const monthlyAttendance = useMemo(
    () => attendanceForMonth(studentAttendance, selectedMonthKey),
    [selectedMonthKey, studentAttendance],
  );
  const monthlyPayments = useMemo(
    () => paymentsForMonth(studentPayments, selectedMonthKey),
    [selectedMonthKey, studentPayments],
  );
  const attendanceTrend = useMemo(
    () => studentAttendanceTrend(studentAttendance),
    [studentAttendance],
  );
  const paymentTrend = useMemo(() => studentPaymentTrend(studentPayments), [studentPayments]);

  const totalAttendance = countAttendedClasses(studentAttendance);
  const monthlyAttendanceCount = countAttendedClasses(monthlyAttendance);
  const lateCount = monthlyAttendance.filter((record) => record.status === "late").length;
  const paidMonthsCount = studentPayments.filter((record) => record.paid).length;
  const selectedMonthPaid = monthlyPayments.some((record) => record.paid);

  return (
    <AnimatePresence>
      {student ? (
        <motion.div
          className="detail-overlay"
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
          onClick={onClose}
        >
          <motion.aside
            className="detail-panel"
            initial={{ opacity: 0, y: 28, scale: 0.98 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0, y: 28, scale: 0.98 }}
            transition={{ duration: 0.22 }}
            onClick={(event) => event.stopPropagation()}
          >
            <div className="detail-header">
              <div className="detail-header-copy">
                <p className="eyebrow">Student Insight</p>
                <h2>{student.full_name}</h2>
                <p>
                  Open the student profile, monthly attendance dates, and payment tracking without
                  leaving the student list.
                </p>
              </div>

              <div className="detail-header-actions">
                <QrLinkButton qrLink={student.qr_link} />
                <QrShareButton
                  qrLink={student.qr_link}
                  whatsappNumber={student.whatsapp_number}
                  studentName={student.full_name}
                />
                <button className="button ghost small-button icon-button" type="button" onClick={onClose}>
                  <X size={18} />
                  Close
                </button>
              </div>
            </div>

            <div className="detail-grid">
              <SectionCard
                title="Overview"
                description="Core student information and this month's quick performance view."
              >
                <div className="detail-metrics-grid">
                  <div className="detail-metric-card">
                    <span>Total attended</span>
                    <strong>{totalAttendance}</strong>
                    <small>All recorded classes</small>
                  </div>

                  <div className="detail-metric-card">
                    <span>This month</span>
                    <strong>{monthlyAttendanceCount}</strong>
                    <small>{lateCount} late marks</small>
                  </div>

                  <div className="detail-metric-card">
                    <span>Paid months</span>
                    <strong>{paidMonthsCount}</strong>
                    <small>Completed payment records</small>
                  </div>

                  <div className="detail-metric-card">
                    <span>Selected month</span>
                    <strong>{selectedMonthPaid ? "Paid" : "Pending"}</strong>
                    <small>Payment status</small>
                  </div>
                </div>

                <div className="detail-info-grid">
                  <div className="detail-info-item">
                    <span>Student code</span>
                    <strong>{student.student_code ?? "-"}</strong>
                  </div>
                  <div className="detail-info-item">
                    <span>Institute</span>
                    <strong>{instituteName}</strong>
                  </div>
                  <div className="detail-info-item">
                    <span>A/L year</span>
                    <strong>{student.al_year}</strong>
                  </div>
                  <div className="detail-info-item">
                    <span>WhatsApp</span>
                    <strong>{student.whatsapp_number}</strong>
                  </div>
                  <div className="detail-info-item">
                    <span>Joined date</span>
                    <strong>{formatDate(student.joined_date)}</strong>
                  </div>
                  <div className="detail-info-item">
                    <span>Status</span>
                    <strong>
                      <StatusBadge
                        label={student.status}
                        tone={student.status === "active" ? "success" : "warning"}
                      />
                    </strong>
                  </div>
                </div>
              </SectionCard>

              <SectionCard
                title="Monthly attendance"
                description="Each recorded attendance date for the selected month."
                actions={
                  <select
                    className="input detail-select"
                    value={selectedMonthKey}
                    onChange={(event) => setSelectedMonthKey(event.target.value)}
                  >
                    {monthOptions.map((option) => (
                      <option key={option.key} value={option.key}>
                        {option.label}
                      </option>
                    ))}
                  </select>
                }
              >
                {loading ? (
                  <div className="empty-state">Loading student attendance and payment history...</div>
                ) : monthlyAttendance.length ? (
                  <div className="detail-chip-list">
                    {monthlyAttendance.map((record) => (
                      <div className="detail-chip" key={record.id}>
                        <strong>{formatDate(record.attendance_date, "dd MMM")}</strong>
                        <StatusBadge
                          label={record.status}
                          tone={record.status === "present" ? "success" : record.status === "late" ? "neutral" : "warning"}
                        />
                      </div>
                    ))}
                  </div>
                ) : (
                  <div className="empty-state">No attendance was recorded for this month yet.</div>
                )}
              </SectionCard>

              <SectionCard
                title="Attendance graph"
                description="Classes attended across the latest six months."
              >
                <div className="chart-shell detail-chart-shell">
                  <ResponsiveContainer width="100%" height={240}>
                    <LineChart data={attendanceTrend}>
                      <CartesianGrid strokeDasharray="3 3" vertical={false} />
                      <XAxis dataKey="label" />
                      <YAxis allowDecimals={false} />
                      <Tooltip />
                      <Line type="monotone" dataKey="value" stroke="#8C63FF" strokeWidth={3} />
                    </LineChart>
                  </ResponsiveContainer>
                </div>
              </SectionCard>

              <SectionCard
                title="Payment graph"
                description="A bar value of 1 means the month is already paid."
              >
                <div className="chart-shell detail-chart-shell">
                  <ResponsiveContainer width="100%" height={240}>
                    <BarChart data={paymentTrend}>
                      <CartesianGrid strokeDasharray="3 3" vertical={false} />
                      <XAxis dataKey="label" />
                      <YAxis allowDecimals={false} ticks={[0, 1]} domain={[0, 1]} />
                      <Tooltip formatter={(value) => (Number(value) > 0 ? "Paid" : "Pending")} />
                      <Bar dataKey="value" fill="#1F2A48" radius={[12, 12, 0, 0]} />
                    </BarChart>
                  </ResponsiveContainer>
                </div>

                <div className="detail-subgrid">
                  {loading ? (
                    <div className="empty-state">Loading student attendance and payment history...</div>
                  ) : monthlyPayments.length ? (
                    monthlyPayments.map((record) => (
                      <div className="detail-subcard" key={record.id}>
                        <div>
                          <strong>{record.paid ? "Paid" : "Pending"}</strong>
                          <p>
                            {record.payment_month}/{record.payment_year}
                          </p>
                        </div>
                        <div>
                          <span>Paid date</span>
                          <strong>{formatDate(record.paid_date)}</strong>
                          <p>{formatCurrency(record.amount)}</p>
                        </div>
                      </div>
                    ))
                  ) : (
                    <div className="empty-state">No payment record was added for this month.</div>
                  )}
                </div>
              </SectionCard>
            </div>
          </motion.aside>
        </motion.div>
      ) : null}
    </AnimatePresence>
  );
}
