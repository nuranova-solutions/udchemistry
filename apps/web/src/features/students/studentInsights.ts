import { format, subMonths } from "date-fns";
import type { AttendanceRecord, PaymentRecord, Student, TrendPoint } from "../../types/app";

export interface StudentMonthOption {
  key: string;
  label: string;
}

const RECENT_MONTH_COUNT = 6;

function monthStartFromKey(monthKey: string) {
  return new Date(`${monthKey}-01T00:00:00`);
}

function attendanceMonthKey(record: AttendanceRecord) {
  return record.attendance_date.slice(0, 7);
}

function paymentMonthKey(record: PaymentRecord) {
  return `${record.payment_year}-${String(record.payment_month).padStart(2, "0")}`;
}

function recentMonthKeys(count = RECENT_MONTH_COUNT) {
  return Array.from({ length: count }, (_, index) =>
    format(subMonths(new Date(), count - index - 1), "yyyy-MM"),
  );
}

export function currentMonthKey() {
  return format(new Date(), "yyyy-MM");
}

export function formatMonthKeyLabel(monthKey: string, pattern = "MMMM yyyy") {
  return format(monthStartFromKey(monthKey), pattern);
}

export function buildStudentMonthOptions(
  student: Student,
  attendance: AttendanceRecord[],
  payments: PaymentRecord[],
): StudentMonthOption[] {
  const keys = new Set<string>(recentMonthKeys());

  keys.add(currentMonthKey());
  keys.add(student.joined_date.slice(0, 7));

  attendance.forEach((record) => keys.add(attendanceMonthKey(record)));
  payments.forEach((record) => keys.add(paymentMonthKey(record)));

  return Array.from(keys)
    .sort((left, right) => right.localeCompare(left))
    .map((key) => ({
      key,
      label: formatMonthKeyLabel(key),
    }));
}

export function studentAttendanceTrend(attendance: AttendanceRecord[]): TrendPoint[] {
  return recentMonthKeys().map((monthKey) => ({
    label: formatMonthKeyLabel(monthKey, "MMM yy"),
    value: attendance.filter(
      (record) => attendanceMonthKey(record) === monthKey && record.status !== "absent",
    ).length,
  }));
}

export function studentPaymentTrend(payments: PaymentRecord[]): TrendPoint[] {
  return recentMonthKeys().map((monthKey) => ({
    label: formatMonthKeyLabel(monthKey, "MMM yy"),
    value: payments.some((record) => paymentMonthKey(record) === monthKey && record.paid) ? 1 : 0,
  }));
}

export function attendanceForMonth(attendance: AttendanceRecord[], monthKey: string) {
  return [...attendance]
    .filter((record) => attendanceMonthKey(record) === monthKey)
    .sort((left, right) => right.attendance_date.localeCompare(left.attendance_date));
}

export function paymentsForMonth(payments: PaymentRecord[], monthKey: string) {
  return [...payments]
    .filter((record) => paymentMonthKey(record) === monthKey)
    .sort((left, right) => {
      const leftKey = `${left.payment_year}-${String(left.payment_month).padStart(2, "0")}`;
      const rightKey = `${right.payment_year}-${String(right.payment_month).padStart(2, "0")}`;
      return rightKey.localeCompare(leftKey);
    });
}

export function countAttendedClasses(attendance: AttendanceRecord[]) {
  return attendance.filter((record) => record.status !== "absent").length;
}
