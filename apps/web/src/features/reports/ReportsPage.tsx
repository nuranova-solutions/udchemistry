import { useQuery } from "@tanstack/react-query";
import jsPDF from "jspdf";
import * as XLSX from "xlsx";
import { DataTable } from "../../components/ui/DataTable";
import { PageHeader } from "../../components/ui/PageHeader";
import { SectionCard } from "../../components/ui/SectionCard";
import { fetchAttendance, fetchPayments, fetchStudents } from "../../lib/api";
import { formatDate } from "../../lib/utils/formatters";
import { useAuth } from "../auth/useAuth";

export function ReportsPage() {
  const { profile } = useAuth();
  const reportsQuery = useQuery({
    queryKey: ["reports", profile?.id, profile?.institute_id],
    queryFn: async () => {
      const [students, attendance, payments] = await Promise.all([
        fetchStudents(profile!),
        fetchAttendance(profile!),
        fetchPayments(profile!),
      ]);

      return { students, attendance, payments };
    },
    enabled: Boolean(profile),
  });

  const data = reportsQuery.data;

  return (
    <div className="page-stack">
      <PageHeader
        title="Reports"
        description="Export operational data to Excel or generate a quick PDF summary for administration."
        actions={
          <div className="inline-actions">
            <button
              className="button"
              type="button"
              onClick={() => {
                if (!data) {
                  return;
                }

                const workbook = XLSX.utils.book_new();
                XLSX.utils.book_append_sheet(
                  workbook,
                  XLSX.utils.json_to_sheet(
                    data.students.map((student) => ({
                      name: student.full_name,
                      al_year: student.al_year,
                      whatsapp_number: student.whatsapp_number,
                      institute: student.institutes?.name ?? "",
                      status: student.status,
                      joined_date: student.joined_date,
                    })),
                  ),
                  "Students",
                );
                XLSX.writeFile(workbook, "students-report.xlsx");
              }}
            >
              Export Students
            </button>

            <button
              className="button secondary"
              type="button"
              onClick={() => {
                if (!data) {
                  return;
                }

                const workbook = XLSX.utils.book_new();
                XLSX.utils.book_append_sheet(
                  workbook,
                  XLSX.utils.json_to_sheet(
                    data.payments.map((payment) => ({
                      student: payment.students?.full_name ?? "",
                      month: payment.payment_month,
                      year: payment.payment_year,
                      paid: payment.paid,
                      paid_date: payment.paid_date,
                    })),
                  ),
                  "Payments",
                );
                XLSX.writeFile(workbook, "payments-report.xlsx");
              }}
            >
              Export Payments
            </button>

            <button
              className="button ghost"
              type="button"
              onClick={() => {
                if (!data) {
                  return;
                }

                const doc = new jsPDF();
                doc.setFontSize(18);
                doc.text("Chemistry Class Summary", 14, 20);
                doc.setFontSize(12);
                doc.text(`Generated: ${formatDate(new Date().toISOString(), "dd MMM yyyy, hh:mm a")}`, 14, 32);
                doc.text(`Students: ${data.students.length}`, 14, 46);
                doc.text(`Attendance rows: ${data.attendance.length}`, 14, 56);
                doc.text(`Payments rows: ${data.payments.length}`, 14, 66);
                doc.save("chemistry-summary.pdf");
              }}
            >
              Export Summary PDF
            </button>
          </div>
        }
      />

      <SectionCard title="Recent attendance export preview" description="Use this table to verify what will be included in exports.">
        <DataTable
          rows={data?.attendance ?? []}
          getRowKey={(row) => row.id}
          columns={[
            { header: "Student", render: (row) => row.students?.full_name ?? "-" },
            { header: "Date", render: (row) => formatDate(row.attendance_date) },
            { header: "Status", render: (row) => row.status },
          ]}
        />
      </SectionCard>
    </div>
  );
}
