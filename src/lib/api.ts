import QRCode from "qrcode";
import { format, startOfMonth, subDays } from "date-fns";
import { supabase } from "./supabase/client";
import type {
  AttendanceRecord,
  DashboardData,
  Institute,
  PaymentRecord,
  Profile,
  ScanAttendanceResult,
  StaffRecord,
  Student,
} from "../types/app";

export interface InstituteFormValues {
  name: string;
  code: string;
  address: string;
  contact_no: string;
  status: "active" | "inactive";
}

export interface StaffFormValues {
  full_name: string;
  username: string;
  email: string;
  password?: string;
  institute_id: string;
  phone: string;
  status: "active" | "inactive";
}

export interface StudentFormValues {
  student_code: string;
  full_name: string;
  al_year: number;
  institute_id: string;
  whatsapp_number: string;
  joined_date: string;
  status: "active" | "inactive";
}

export interface AttendanceFormValues {
  student_id: string;
  attendance_date: string;
  status: AttendanceRecord["status"];
}

export interface PaymentFormValues {
  student_id: string;
  payment_month: number;
  payment_year: number;
  amount: number;
  paid: boolean;
  paid_date: string | null;
}

const studentSelectFields =
  "id, student_code, full_name, al_year, institute_id, qr_code_id, whatsapp_number, qr_link, joined_date, status, institutes(name), qr_codes(id, student_id, qr_data, share_token, qr_link, qr_image_path, qr_image_url, last_shared_at, generated_at)";

const appUrl =
  (import.meta.env.VITE_APP_URL as string | undefined) ??
  (typeof window !== "undefined" ? window.location.origin : "http://localhost:5173");

function toTrendMap(labels: string[]) {
  return new Map(labels.map((label) => [label, 0]));
}

function firstRelation<T>(value: T | T[] | null | undefined) {
  if (Array.isArray(value)) {
    return value[0] ?? null;
  }

  return value ?? null;
}

function cleanText(value: string | null | undefined) {
  return value?.trim() ?? "";
}

function nullIfBlank(value: string | null | undefined) {
  const nextValue = cleanText(value);
  return nextValue.length ? nextValue : null;
}

function currentAppUrl() {
  return appUrl.replace(/\/+$/, "");
}

async function buildQrRecord(studentId: string) {
  const shareToken = crypto.randomUUID().replace(/-/g, "");
  const qrLink = `${currentAppUrl()}/qr/${shareToken}`;
  const qrImageUrl = await QRCode.toDataURL(studentId, {
    width: 480,
    margin: 2,
    color: {
      dark: "#0C162A",
      light: "#FFFFFF",
    },
  });

  return {
    qr_data: studentId,
    share_token: shareToken,
    qr_link: qrLink,
    qr_image_path: `generated/${shareToken}.png`,
    qr_image_url: qrImageUrl,
  };
}

function mapStudentRow(row: unknown) {
  const record = row as Student & {
    institutes?: { name: string }[] | { name: string } | null;
    qr_codes?: Student["qr_codes"][] | Student["qr_codes"] | null;
  };

  return {
    ...record,
    institutes: firstRelation(record.institutes),
    qr_codes: firstRelation(record.qr_codes),
  } as Student;
}

async function fetchStudentById(studentId: string) {
  const { data, error } = await supabase
    .from("students")
    .select(studentSelectFields)
    .eq("id", studentId)
    .single();

  if (error) {
    throw error;
  }

  return mapStudentRow(data);
}

async function ensureStudentQrRecord(student: Pick<Student, "id" | "qr_code_id" | "qr_link">) {
  if (student.qr_code_id && student.qr_link) {
    return;
  }

  const qrCodeId = student.qr_code_id ?? crypto.randomUUID();
  const qrRecord = await buildQrRecord(student.id);

  const { error: qrError } = await supabase.from("qr_codes").upsert(
    {
      id: qrCodeId,
      student_id: student.id,
      ...qrRecord,
    },
    { onConflict: "student_id" },
  );

  if (qrError) {
    throw qrError;
  }

  const { error: studentError } = await supabase
    .from("students")
    .update({
      qr_code_id: qrCodeId,
      qr_link: qrRecord.qr_link,
    })
    .eq("id", student.id);

  if (studentError) {
    throw studentError;
  }
}

export async function fetchInstitutes(profile: Profile) {
  let query = supabase
    .from("institutes")
    .select("id, name, code, address, contact_no, status, created_at")
    .order("created_at", { ascending: false });

  if (profile.role === "staff" && profile.institute_id) {
    query = query.eq("id", profile.institute_id);
  }

  const { data, error } = await query;

  if (error) {
    throw error;
  }

  return (data ?? []) as Institute[];
}

export async function createInstitute(values: InstituteFormValues) {
  const { error } = await supabase.from("institutes").insert({
    name: cleanText(values.name),
    code: cleanText(values.code),
    address: nullIfBlank(values.address),
    contact_no: nullIfBlank(values.contact_no),
    status: values.status,
  });

  if (error) {
    throw error;
  }
}

export async function updateInstitute(instituteId: string, values: InstituteFormValues) {
  const { error } = await supabase
    .from("institutes")
    .update({
      name: cleanText(values.name),
      code: cleanText(values.code),
      address: nullIfBlank(values.address),
      contact_no: nullIfBlank(values.contact_no),
      status: values.status,
    })
    .eq("id", instituteId);

  if (error) {
    throw error;
  }
}

export async function deleteInstitute(instituteId: string) {
  const { error } = await supabase.from("institutes").delete().eq("id", instituteId);

  if (error) {
    throw error;
  }
}

export async function fetchStaff(profile: Profile) {
  let query = supabase
    .from("profiles")
    .select("id, full_name, email, username, role, institute_id, phone, status, institutes(name)")
    .eq("role", "staff")
    .order("full_name");

  if (profile.role === "staff" && profile.institute_id) {
    query = query.eq("institute_id", profile.institute_id);
  }

  const { data, error } = await query;

  if (error) {
    throw error;
  }

  return ((data ?? []) as unknown[]).map((row) => {
    const record = row as StaffRecord & { institutes?: { name: string }[] | { name: string } | null };
    return {
      ...record,
      institutes: firstRelation(record.institutes),
    };
  }) as StaffRecord[];
}

export async function createStaff(values: StaffFormValues) {
  const { error } = await supabase.rpc("admin_create_staff", {
    p_full_name: cleanText(values.full_name),
    p_username: cleanText(values.username),
    p_email: cleanText(values.email).toLowerCase(),
    p_password: cleanText(values.password),
    p_institute_id: values.institute_id,
    p_phone: nullIfBlank(values.phone),
    p_status: values.status,
  });

  if (error) {
    throw error;
  }
}

export async function updateStaff(staffId: string, values: StaffFormValues) {
  const { error } = await supabase.rpc("admin_update_staff", {
    p_staff_id: staffId,
    p_full_name: cleanText(values.full_name),
    p_username: cleanText(values.username),
    p_email: cleanText(values.email).toLowerCase(),
    p_password: nullIfBlank(values.password),
    p_institute_id: values.institute_id,
    p_phone: nullIfBlank(values.phone),
    p_status: values.status,
  });

  if (error) {
    throw error;
  }
}

export async function deleteStaff(staffId: string) {
  const { error } = await supabase.rpc("admin_delete_staff", {
    p_staff_id: staffId,
  });

  if (error) {
    throw error;
  }
}

export async function fetchStudents(profile: Profile) {
  let query = supabase
    .from("students")
    .select(studentSelectFields)
    .order("created_at", { ascending: false });

  if (profile.role === "staff" && profile.institute_id) {
    query = query.eq("institute_id", profile.institute_id);
  }

  const { data, error } = await query;

  if (error) {
    throw error;
  }

  return ((data ?? []) as unknown[]).map(mapStudentRow);
}

export async function createStudent(profile: Profile, values: StudentFormValues) {
  const studentId = crypto.randomUUID();
  const qrCodeId = crypto.randomUUID();
  const qrRecord = await buildQrRecord(studentId);

  const { error: studentError } = await supabase.from("students").insert({
    id: studentId,
    student_code: nullIfBlank(values.student_code),
    full_name: cleanText(values.full_name),
    al_year: values.al_year,
    institute_id: values.institute_id,
    qr_code_id: qrCodeId,
    whatsapp_number: cleanText(values.whatsapp_number),
    qr_link: qrRecord.qr_link,
    joined_date: values.joined_date,
    status: values.status,
    created_by: profile.id,
  });

  if (studentError) {
    throw studentError;
  }

  const { error: qrError } = await supabase.from("qr_codes").insert({
    id: qrCodeId,
    student_id: studentId,
    ...qrRecord,
  });

  if (qrError) {
    await supabase.from("students").delete().eq("id", studentId);
    throw qrError;
  }

  return fetchStudentById(studentId);
}

export async function updateStudent(student: Student, values: StudentFormValues) {
  const { error } = await supabase
    .from("students")
    .update({
      student_code: nullIfBlank(values.student_code),
      full_name: cleanText(values.full_name),
      al_year: values.al_year,
      institute_id: values.institute_id,
      whatsapp_number: cleanText(values.whatsapp_number),
      joined_date: values.joined_date,
      status: values.status,
    })
    .eq("id", student.id);

  if (error) {
    throw error;
  }

  await ensureStudentQrRecord(student);
  return fetchStudentById(student.id);
}

export async function deleteStudent(studentId: string) {
  const { error } = await supabase.from("students").delete().eq("id", studentId);

  if (error) {
    throw error;
  }
}

export async function fetchAttendance(profile: Profile) {
  let query = supabase
    .from("attendance")
    .select("id, student_id, attendance_date, status, marked_at, students!inner(full_name, institute_id)")
    .order("marked_at", { ascending: false });

  if (profile.role === "staff" && profile.institute_id) {
    query = query.eq("students.institute_id", profile.institute_id);
  }

  const { data, error } = await query;

  if (error) {
    throw error;
  }

  return ((data ?? []) as unknown[]).map((row) => {
    const record = row as AttendanceRecord & {
      students?: AttendanceRecord["students"][] | AttendanceRecord["students"] | null;
    };

    return {
      ...record,
      students: firstRelation(record.students),
    };
  }) as AttendanceRecord[];
}

export async function createAttendance(profile: Profile, values: AttendanceFormValues) {
  const { error } = await supabase.from("attendance").insert({
    student_id: values.student_id,
    attendance_date: values.attendance_date,
    status: values.status,
    marked_by: profile.id,
  });

  if (error) {
    throw error;
  }
}

export async function updateAttendance(
  profile: Profile,
  attendanceId: string,
  values: AttendanceFormValues,
) {
  const { error } = await supabase
    .from("attendance")
    .update({
      student_id: values.student_id,
      attendance_date: values.attendance_date,
      status: values.status,
      marked_by: profile.id,
      marked_at: new Date().toISOString(),
    })
    .eq("id", attendanceId);

  if (error) {
    throw error;
  }
}

export async function deleteAttendance(attendanceId: string) {
  const { error } = await supabase.from("attendance").delete().eq("id", attendanceId);

  if (error) {
    throw error;
  }
}

export async function fetchPayments(profile: Profile) {
  let query = supabase
    .from("payments")
    .select("id, student_id, payment_month, payment_year, amount, paid, paid_date, students!inner(full_name, institute_id)")
    .order("payment_year", { ascending: false })
    .order("payment_month", { ascending: false });

  if (profile.role === "staff" && profile.institute_id) {
    query = query.eq("students.institute_id", profile.institute_id);
  }

  const { data, error } = await query;

  if (error) {
    throw error;
  }

  return ((data ?? []) as unknown[]).map((row) => {
    const record = row as PaymentRecord & {
      students?: PaymentRecord["students"][] | PaymentRecord["students"] | null;
    };

    return {
      ...record,
      students: firstRelation(record.students),
    };
  }) as PaymentRecord[];
}

export async function createPayment(profile: Profile, values: PaymentFormValues) {
  const { error } = await supabase.from("payments").insert({
    student_id: values.student_id,
    payment_month: values.payment_month,
    payment_year: values.payment_year,
    amount: values.amount,
    paid: values.paid,
    paid_date: values.paid ? values.paid_date ?? format(new Date(), "yyyy-MM-dd") : null,
    marked_by: profile.id,
  });

  if (error) {
    throw error;
  }
}

export async function updatePayment(profile: Profile, paymentId: string, values: PaymentFormValues) {
  const { error } = await supabase
    .from("payments")
    .update({
      student_id: values.student_id,
      payment_month: values.payment_month,
      payment_year: values.payment_year,
      amount: values.amount,
      paid: values.paid,
      paid_date: values.paid ? values.paid_date ?? format(new Date(), "yyyy-MM-dd") : null,
      marked_by: profile.id,
    })
    .eq("id", paymentId);

  if (error) {
    throw error;
  }
}

export async function deletePayment(paymentId: string) {
  const { error } = await supabase.from("payments").delete().eq("id", paymentId);

  if (error) {
    throw error;
  }
}

export async function fetchDashboardData(profile: Profile): Promise<DashboardData> {
  const [students, attendance, payments] = await Promise.all([
    fetchStudents(profile),
    fetchAttendance(profile),
    fetchPayments(profile),
  ]);

  const today = format(new Date(), "yyyy-MM-dd");
  const month = new Date().getMonth() + 1;
  const year = new Date().getFullYear();

  const attendanceLabels = Array.from({ length: 7 }, (_, index) =>
    format(subDays(new Date(), 6 - index), "MMM dd"),
  );
  const attendanceMap = toTrendMap(attendanceLabels);

  attendance.forEach((record) => {
    const label = format(new Date(record.attendance_date), "MMM dd");
    attendanceMap.set(label, (attendanceMap.get(label) ?? 0) + 1);
  });

  const registrationLabels = Array.from({ length: 6 }, (_, index) =>
    format(subDays(startOfMonth(new Date()), (5 - index) * 30), "MMM yyyy"),
  );
  const registrationMap = toTrendMap(registrationLabels);

  students.forEach((student) => {
    const label = format(new Date(student.joined_date), "MMM yyyy");
    registrationMap.set(label, (registrationMap.get(label) ?? 0) + 1);
  });

  const incomeLabels = registrationLabels;
  const incomeMap = toTrendMap(incomeLabels);

  payments
    .filter((payment) => payment.paid)
    .forEach((payment) => {
      const label = format(
        new Date(payment.paid_date ?? `${payment.payment_year}-${payment.payment_month}-01`),
        "MMM yyyy",
      );
      incomeMap.set(label, (incomeMap.get(label) ?? 0) + payment.amount);
    });

  return {
    metrics: [
      {
        label: "Total students",
        value: students.length,
        hint: "Active roster across your scope",
      },
      {
        label: "Attendance today",
        value: attendance.filter((record) => record.attendance_date === today).length,
        hint: "Students marked from QR or manual entry",
      },
      {
        label: "Paid this month",
        value: payments.filter(
          (payment) =>
            payment.paid &&
            payment.payment_month === month &&
            payment.payment_year === year,
        ).length,
        hint: "Monthly fee records already settled",
      },
      {
        label: "Unpaid this month",
        value:
          students.length -
          payments.filter(
            (payment) =>
              payment.paid &&
              payment.payment_month === month &&
              payment.payment_year === year,
          ).length,
        hint: "Students still awaiting current month payment",
      },
    ],
    attendanceTrend: attendanceLabels.map((label) => ({
      label,
      value: attendanceMap.get(label) ?? 0,
    })),
    incomeTrend: incomeLabels.map((label) => ({
      label,
      value: incomeMap.get(label) ?? 0,
    })),
    registrationTrend: registrationLabels.map((label) => ({
      label,
      value: registrationMap.get(label) ?? 0,
    })),
  };
}

export async function scanAttendance(profile: Profile, qrData: string): Promise<ScanAttendanceResult> {
  let studentQuery = supabase
    .from("students")
    .select("id, full_name, whatsapp_number, institute_id")
    .eq("id", qrData);

  if (profile.role === "staff" && profile.institute_id) {
    studentQuery = studentQuery.eq("institute_id", profile.institute_id);
  }

  const { data: student, error: studentError } = await studentQuery.single();

  if (studentError || !student) {
    throw new Error("Student QR was not found in your access scope.");
  }

  const attendanceDate = format(new Date(), "yyyy-MM-dd");
  const { error: attendanceError } = await supabase.from("attendance").insert({
    student_id: student.id,
    attendance_date: attendanceDate,
    status: "present",
    marked_by: profile.id,
  });

  if (attendanceError && attendanceError.code === "23505") {
    return {
      duplicate: true,
      student,
    };
  }

  if (attendanceError) {
    throw attendanceError;
  }

  return {
    duplicate: false,
    student,
  };
}

export async function markCurrentMonthPaid(profile: Profile, studentId: string) {
  const now = new Date();
  const paymentMonth = now.getMonth() + 1;
  const paymentYear = now.getFullYear();

  const { error } = await supabase.from("payments").upsert(
    {
      student_id: studentId,
      payment_month: paymentMonth,
      payment_year: paymentYear,
      amount: 0,
      paid: true,
      paid_date: format(now, "yyyy-MM-dd"),
      marked_by: profile.id,
    },
    { onConflict: "student_id,payment_month,payment_year" },
  );

  if (error) {
    throw error;
  }
}

export async function updateProfile(profileId: string, values: Pick<Profile, "full_name" | "phone">) {
  const { error } = await supabase
    .from("profiles")
    .update({
      full_name: cleanText(values.full_name),
      phone: nullIfBlank(values.phone),
    })
    .eq("id", profileId);

  if (error) {
    throw error;
  }
}

export async function updateOwnPassword(password: string) {
  const { error } = await supabase.auth.updateUser({
    password: cleanText(password),
  });

  if (error) {
    throw error;
  }
}

export async function fetchPublicQr(shareToken: string) {
  const { data, error } = await supabase.rpc("get_public_qr_by_token", {
    p_share_token: shareToken,
  });

  if (error) {
    throw error;
  }

  const rows = Array.isArray(data) ? data : [];
  return rows[0] as { qr_image_url: string | null; qr_link: string } | undefined;
}
