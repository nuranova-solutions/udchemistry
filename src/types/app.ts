export type Role = "admin" | "staff";
export type ThemeMode = "system" | "dark" | "light";

export interface Profile {
  id: string;
  full_name: string;
  email: string;
  username: string;
  role: Role;
  institute_id: string | null;
  phone: string | null;
  status: string;
}

export interface StaffRecord extends Profile {
  institutes?: Pick<Institute, "name"> | null;
}

export interface Institute {
  id: string;
  name: string;
  code: string;
  address: string | null;
  contact_no: string | null;
  status: string;
  created_at?: string;
}

export interface ClassRecord {
  id: string;
  name: string;
  institute_id: string;
  al_year: number;
  monthly_fee: number;
  class_type: "general" | "extra";
  weekday: "monday" | "tuesday" | "wednesday" | "thursday" | "friday" | "saturday" | "sunday";
  start_time: string;
  end_time: string;
  week_of_month: number | null;
  active_from: string;
  active_until: string | null;
  status: "active" | "inactive";
  notes: string | null;
  created_at?: string;
  institutes?: Pick<Institute, "name"> | null;
}

export interface QrCodeRecord {
  id: string;
  student_id: string;
  qr_data: string;
  share_token: string;
  qr_link: string;
  qr_image_path: string;
  qr_image_url: string | null;
  last_shared_at: string | null;
  generated_at: string;
}

export interface Student {
  id: string;
  student_code: string | null;
  full_name: string;
  al_year: number;
  institute_id: string;
  monthly_fee: number;
  qr_code_id: string | null;
  whatsapp_number: string;
  qr_link: string | null;
  joined_date: string;
  status: string;
  institutes?: Pick<Institute, "name"> | null;
  qr_codes?: QrCodeRecord | null;
}

export interface AttendanceRecord {
  id: string;
  student_id: string;
  attendance_date: string;
  status: "present" | "absent" | "late";
  marked_at: string;
  students?: Pick<Student, "full_name" | "institute_id"> | null;
}

export interface PaymentRecord {
  id: string;
  student_id: string;
  payment_month: number;
  payment_year: number;
  amount: number;
  paid: boolean;
  paid_date: string | null;
  students?: Pick<Student, "full_name" | "institute_id"> | null;
}

export interface DashboardMetric {
  label: string;
  value: number;
  hint: string;
}

export interface TrendPoint {
  label: string;
  value: number;
}

export interface DashboardData {
  metrics: DashboardMetric[];
  attendanceTrend: TrendPoint[];
  incomeTrend: TrendPoint[];
  registrationTrend: TrendPoint[];
}

export interface ScanAttendanceResult {
  duplicate: boolean;
  student: Pick<Student, "id" | "full_name" | "whatsapp_number">;
}
