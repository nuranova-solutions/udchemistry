import assert from "node:assert/strict";
import { renderToStaticMarkup } from "react-dom/server";
import { DataTable } from "../components/ui/DataTable";
import { PageHeader } from "../components/ui/PageHeader";
import { ScanResultPanel } from "../features/attendance/ScanResultPanel";
import { QrShareButton } from "../features/students/QrShareButton";
import { StudentDetailPanel } from "../features/students/StudentDetailPanel";
import { resolveStudentQrLink } from "../features/students/qrHelpers";
import { currentMonthLabel, formatCurrency } from "../lib/utils/formatters";
import type {
  AttendanceRecord,
  ClassRecord,
  PaymentRecord,
  Student,
} from "../types/app";

const student: Student = {
  id: "stu-1",
  student_code: "UD-001",
  full_name: "Akeel Fernando",
  al_year: 2027,
  institute_id: "inst-1",
  monthly_fee: 1200,
  qr_code_id: "qr-1",
  whatsapp_number: "0771000001",
  qr_link: "https://example.com/qr/token-1",
  joined_date: "2026-01-05",
  status: "active",
  institutes: { name: "UD Chemistry Colombo" },
  qr_codes: {
    id: "qr-1",
    student_id: "stu-1",
    qr_data: "stu-1",
    share_token: "token-1",
    qr_link: "https://example.com/qr/token-1",
    qr_image_path: "generated/token-1.png",
    qr_image_url: "data:image/png;base64,ZmFrZQ==",
    last_shared_at: null,
    generated_at: "2026-04-03T10:00:00",
  },
};

const classes: ClassRecord[] = [
  {
    id: "class-1",
    name: "2027 Theory",
    institute_id: "inst-1",
    al_year: 2027,
    monthly_fee: 1200,
    class_type: "general",
    weekday: "monday",
    start_time: "09:00:00",
    end_time: "11:00:00",
    week_of_month: null,
    active_from: "2026-01-01",
    active_until: null,
    status: "active",
    notes: "Main weekly batch",
    created_at: "2026-01-01T09:00:00",
    institutes: { name: "UD Chemistry Colombo" },
  },
  {
    id: "class-2",
    name: "First Week Extra",
    institute_id: "inst-1",
    al_year: 2027,
    monthly_fee: 0,
    class_type: "extra",
    weekday: "tuesday",
    start_time: "16:00:00",
    end_time: "18:00:00",
    week_of_month: 1,
    active_from: "2026-02-01",
    active_until: "2026-04-30",
    status: "active",
    notes: "Dummy extra class",
    created_at: "2026-02-01T09:00:00",
    institutes: { name: "UD Chemistry Colombo" },
  },
];

const attendance: AttendanceRecord[] = [
  {
    id: "att-1",
    student_id: "stu-1",
    attendance_date: "2026-04-03",
    status: "present",
    marked_at: "2026-04-03T09:10:00",
    students: { full_name: student.full_name, institute_id: student.institute_id },
  },
  {
    id: "att-2",
    student_id: "stu-1",
    attendance_date: "2026-03-27",
    status: "late",
    marked_at: "2026-03-27T09:15:00",
    students: { full_name: student.full_name, institute_id: student.institute_id },
  },
];

const payments: PaymentRecord[] = [
  {
    id: "pay-1",
    student_id: "stu-1",
    payment_month: 4,
    payment_year: 2026,
    amount: 1200,
    paid: true,
    paid_date: "2026-04-03",
    students: { full_name: student.full_name, institute_id: student.institute_id },
  },
];

function runDummySmoke() {
  const headerMarkup = renderToStaticMarkup(
    <PageHeader
      title="Classes"
      description="Dummy data smoke test for the premium web shell."
    />,
  );
  assert.match(headerMarkup, /Classes/);
  assert.match(headerMarkup, /Dummy data smoke test/i);

  const classTableMarkup = renderToStaticMarkup(
    <DataTable
      rows={classes}
      getRowKey={(row) => row.id}
      columns={[
        { header: "Class", render: (row) => row.name },
        { header: "Type", render: (row) => row.class_type },
        { header: "Fee", render: (row) => formatCurrency(row.monthly_fee) },
      ]}
    />,
  );
  assert.match(classTableMarkup, /2027 Theory/);
  assert.match(classTableMarkup, /First Week Extra/);
  assert.match(classTableMarkup, /LKR/);

  const detailMarkup = renderToStaticMarkup(
    <StudentDetailPanel
      student={student}
      instituteName={student.institutes?.name ?? "-"}
      attendance={attendance}
      payments={payments}
      loading={false}
      onClose={() => undefined}
    />,
  );
  assert.match(detailMarkup, /Student Insight/);
  assert.match(detailMarkup, /Monthly fee/);
  assert.match(detailMarkup, new RegExp(formatCurrency(student.monthly_fee).replace(/[.*+?^${}()|[\]\\]/g, "\\$&")));
  assert.match(detailMarkup, /Paid/);

  const scanMarkup = renderToStaticMarkup(
    <ScanResultPanel
      result={{
        duplicate: false,
        student: {
          id: student.id,
          full_name: student.full_name,
          whatsapp_number: student.whatsapp_number,
        },
      }}
      paymentStatus="idle"
      paymentLoading={false}
      onMarkPaid={() => undefined}
      onSkip={() => undefined}
      onDone={() => undefined}
    />,
  );
  assert.match(scanMarkup, /Attendance marked/);
  assert.match(scanMarkup, new RegExp(currentMonthLabel()));

  const qrLink = resolveStudentQrLink(student);
  assert.equal(qrLink, "https://example.com/qr/token-1");

  const shareMarkup = renderToStaticMarkup(<QrShareButton student={student} />);
  assert.match(shareMarkup, /wa\.me\/0771000001|wa\.me\/771000001/);
  assert.match(shareMarkup, /2027/);

  console.log("Web dummy smoke passed:");
  console.log("- Page header rendered");
  console.log("- Class roster rendered");
  console.log("- Student detail panel rendered");
  console.log("- Scan result panel rendered");
  console.log("- QR share URL resolved with student year");
}

runDummySmoke();
