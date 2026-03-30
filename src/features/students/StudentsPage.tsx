import { useEffect, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { DataTable } from "../../components/ui/DataTable";
import { PageHeader } from "../../components/ui/PageHeader";
import { SectionCard } from "../../components/ui/SectionCard";
import { StatusBadge } from "../../components/ui/StatusBadge";
import {
  createStudent,
  deleteStudent,
  fetchAttendance,
  fetchInstitutes,
  fetchPayments,
  fetchStudents,
  type StudentFormValues,
  updateStudent,
} from "../../lib/api";
import { formatDate } from "../../lib/utils/formatters";
import type { Student } from "../../types/app";
import { useAuth } from "../auth/useAuth";
import { QrLinkButton } from "./QrLinkButton";
import { QrShareButton } from "./QrShareButton";
import { StudentDetailPanel } from "./StudentDetailPanel";

function todayString() {
  return new Date().toISOString().slice(0, 10);
}

function emptyForm(instituteId = ""): StudentFormValues {
  return {
    student_code: "",
    full_name: "",
    al_year: new Date().getFullYear(),
    institute_id: instituteId,
    whatsapp_number: "",
    joined_date: todayString(),
    status: "active",
  };
}

export function StudentsPage() {
  const { profile } = useAuth();
  const queryClient = useQueryClient();
  const [editingStudent, setEditingStudent] = useState<Student | null>(null);
  const [formValues, setFormValues] = useState<StudentFormValues>(emptyForm(profile?.institute_id ?? ""));
  const [feedback, setFeedback] = useState<string | null>(null);
  const [instituteFilter, setInstituteFilter] = useState("all");
  const [yearFilter, setYearFilter] = useState("all");
  const [selectedStudentId, setSelectedStudentId] = useState<string | null>(null);

  const studentsQuery = useQuery({
    queryKey: ["students", profile?.id, profile?.institute_id],
    queryFn: () => fetchStudents(profile!),
    enabled: Boolean(profile),
  });

  const institutesQuery = useQuery({
    queryKey: ["institutes", profile?.id, profile?.institute_id],
    queryFn: () => fetchInstitutes(profile!),
    enabled: Boolean(profile),
  });

  const attendanceQuery = useQuery({
    queryKey: ["attendance", "student-insight", profile?.id, profile?.institute_id],
    queryFn: () => fetchAttendance(profile!),
    enabled: Boolean(profile),
  });

  const paymentsQuery = useQuery({
    queryKey: ["payments", "student-insight", profile?.id, profile?.institute_id],
    queryFn: () => fetchPayments(profile!),
    enabled: Boolean(profile),
  });

  useEffect(() => {
    if (editingStudent) {
      return;
    }

    if (profile?.role === "staff" && profile.institute_id) {
      setFormValues((currentValues) => ({
        ...currentValues,
        institute_id: profile.institute_id ?? "",
      }));
      return;
    }

    if (!formValues.institute_id && institutesQuery.data?.[0]?.id) {
      setFormValues((currentValues) => ({
        ...currentValues,
        institute_id: institutesQuery.data?.[0]?.id ?? "",
      }));
    }
  }, [editingStudent, formValues.institute_id, institutesQuery.data, profile]);

  const saveMutation = useMutation({
    mutationFn: async (values: StudentFormValues) => {
      if (editingStudent) {
        await updateStudent(editingStudent, values);
        return;
      }

      await createStudent(profile!, values);
    },
    onSuccess: async () => {
      setFeedback(editingStudent ? "Student updated successfully." : "Student added successfully.");
      setEditingStudent(null);
      setFormValues(emptyForm(profile?.role === "staff" ? profile.institute_id ?? "" : institutesQuery.data?.[0]?.id ?? ""));
      await queryClient.invalidateQueries({ queryKey: ["students"] });
      await queryClient.invalidateQueries({ queryKey: ["dashboard"] });
      await queryClient.invalidateQueries({ queryKey: ["reports"] });
    },
    onError: (error) => {
      setFeedback(error instanceof Error ? error.message : "Unable to save the student.");
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (studentId: string) => deleteStudent(studentId),
    onSuccess: async () => {
      setFeedback("Student deleted successfully.");
      if (editingStudent) {
        setEditingStudent(null);
      }
      setFormValues(emptyForm(profile?.role === "staff" ? profile.institute_id ?? "" : institutesQuery.data?.[0]?.id ?? ""));
      await queryClient.invalidateQueries({ queryKey: ["students"] });
      await queryClient.invalidateQueries({ queryKey: ["attendance"] });
      await queryClient.invalidateQueries({ queryKey: ["payments"] });
      await queryClient.invalidateQueries({ queryKey: ["dashboard"] });
      await queryClient.invalidateQueries({ queryKey: ["reports"] });
    },
    onError: (error) => {
      setFeedback(error instanceof Error ? error.message : "Unable to delete the student.");
    },
  });

  function handleChange<K extends keyof StudentFormValues>(key: K, value: StudentFormValues[K]) {
    setFormValues((currentValues) => ({
      ...currentValues,
      [key]: value,
    }));
  }

  function handleEdit(student: Student) {
    setEditingStudent(student);
    setFormValues({
      student_code: student.student_code ?? "",
      full_name: student.full_name,
      al_year: student.al_year,
      institute_id: student.institute_id,
      whatsapp_number: student.whatsapp_number,
      joined_date: student.joined_date,
      status: student.status as StudentFormValues["status"],
    });
    setFeedback(null);
  }

  function handleReset() {
    setEditingStudent(null);
    setFormValues(emptyForm(profile?.role === "staff" ? profile.institute_id ?? "" : institutesQuery.data?.[0]?.id ?? ""));
    setFeedback(null);
  }

  const allStudents = studentsQuery.data ?? [];
  const availableYears = Array.from(new Set(allStudents.map((student) => String(student.al_year)))).sort();
  const selectedStudent = allStudents.find((student) => student.id === selectedStudentId) ?? null;
  const visibleStudents = allStudents.filter((student) => {
    const matchesInstitute =
      instituteFilter === "all" || student.institute_id === instituteFilter;
    const matchesYear = yearFilter === "all" || String(student.al_year) === yearFilter;

    return matchesInstitute && matchesYear;
  });

  return (
    <div className="page-stack">
      <PageHeader
        title="Students"
        description="Admins can change student details, while each registration still creates one unique QR and shareable link."
        actions={
          <button className="button secondary" type="button" onClick={handleReset}>
            Add student
          </button>
        }
      />

      <SectionCard
        title={editingStudent ? "Edit student" : "Add student"}
        description="Saving a new student keeps their QR link ready for open, download, or WhatsApp sharing."
      >
        <form
          className="management-form"
          onSubmit={(event) => {
            event.preventDefault();
            saveMutation.mutate(formValues);
          }}
        >
          <div className="form-grid two-columns">
            <div>
              <label className="field-label" htmlFor="student_code">
                Student code
              </label>
              <input
                id="student_code"
                className="input"
                value={formValues.student_code}
                onChange={(event) => handleChange("student_code", event.target.value)}
              />
            </div>

            <div>
              <label className="field-label" htmlFor="student_name">
                Student name
              </label>
              <input
                id="student_name"
                className="input"
                value={formValues.full_name}
                onChange={(event) => handleChange("full_name", event.target.value)}
                required
              />
            </div>

            <div>
              <label className="field-label" htmlFor="student_year">
                A/L year
              </label>
              <input
                id="student_year"
                className="input"
                type="number"
                value={formValues.al_year}
                onChange={(event) => handleChange("al_year", Number(event.target.value))}
                required
              />
            </div>

            <div>
              <label className="field-label" htmlFor="student_institute">
                Institute
              </label>
              <select
                id="student_institute"
                className="input"
                value={formValues.institute_id}
                onChange={(event) => handleChange("institute_id", event.target.value)}
                disabled={profile?.role === "staff"}
                required
              >
                <option value="" disabled>
                  Select institute
                </option>
                {(institutesQuery.data ?? []).map((institute) => (
                  <option key={institute.id} value={institute.id}>
                    {institute.name}
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label className="field-label" htmlFor="student_whatsapp">
                WhatsApp number
              </label>
              <input
                id="student_whatsapp"
                className="input"
                value={formValues.whatsapp_number}
                onChange={(event) => handleChange("whatsapp_number", event.target.value)}
                required
              />
            </div>

            <div>
              <label className="field-label" htmlFor="student_joined">
                Joined date
              </label>
              <input
                id="student_joined"
                className="input"
                type="date"
                value={formValues.joined_date}
                onChange={(event) => handleChange("joined_date", event.target.value)}
                required
              />
            </div>

            <div>
              <label className="field-label" htmlFor="student_status">
                Status
              </label>
              <select
                id="student_status"
                className="input"
                value={formValues.status}
                onChange={(event) =>
                  handleChange("status", event.target.value as StudentFormValues["status"])
                }
              >
                <option value="active">Active</option>
                <option value="inactive">Inactive</option>
              </select>
            </div>
          </div>

          {feedback ? (
            <p className={feedback.toLowerCase().includes("unable") ? "error-text" : "helper-text"}>
              {feedback}
            </p>
          ) : null}

          <div className="inline-actions">
            <button className="button" type="submit" disabled={saveMutation.isPending}>
              {saveMutation.isPending ? "Saving..." : editingStudent ? "Save changes" : "Save student"}
            </button>
            <button className="button ghost" type="button" onClick={handleReset}>
              Clear
            </button>
          </div>
        </form>
      </SectionCard>

      <SectionCard
        title="Student roster"
        description="Admins can filter by institute and A/L year, then edit, delete, or share each student's QR."
        actions={
          <div className="filter-row">
            {profile?.role === "admin" ? (
              <select
                className="input filter-input"
                value={instituteFilter}
                onChange={(event) => setInstituteFilter(event.target.value)}
              >
                <option value="all">All institutes</option>
                {(institutesQuery.data ?? []).map((institute) => (
                  <option key={institute.id} value={institute.id}>
                    {institute.name}
                  </option>
                ))}
              </select>
            ) : null}

            <select
              className="input filter-input"
              value={yearFilter}
              onChange={(event) => setYearFilter(event.target.value)}
            >
              <option value="all">All years</option>
              {availableYears.map((year) => (
                <option key={year} value={year}>
                  {year}
                </option>
              ))}
            </select>
          </div>
        }
      >
        <DataTable
          rows={visibleStudents}
          getRowKey={(row) => row.id}
          columns={[
            { header: "Code", render: (row) => row.student_code ?? "-" },
            {
              header: "Student",
              render: (row) => (
                <button
                  className="student-link-button"
                  type="button"
                  onClick={() => setSelectedStudentId(row.id)}
                >
                  {row.full_name}
                </button>
              ),
            },
            { header: "A/L year", render: (row) => row.al_year },
            { header: "Institute", render: (row) => row.institutes?.name ?? "-" },
            { header: "WhatsApp", render: (row) => row.whatsapp_number },
            { header: "Joined", render: (row) => formatDate(row.joined_date) },
            {
              header: "Status",
              render: (row) => (
                <StatusBadge
                  label={row.status}
                  tone={row.status === "active" ? "success" : "warning"}
                />
              ),
            },
            { header: "QR", render: (row) => <QrLinkButton student={row} /> },
            {
              header: "Share",
              render: (row) => <QrShareButton student={row} />,
            },
            {
              header: "Actions",
              render: (row) => (
                <div className="table-actions">
                  <button className="button ghost small-button" type="button" onClick={() => handleEdit(row)}>
                    Edit
                  </button>
                  <button
                    className="button ghost danger-button small-button"
                    type="button"
                    onClick={() => {
                      const confirmed = window.confirm(`Delete ${row.full_name}?`);

                      if (confirmed) {
                        deleteMutation.mutate(row.id);
                      }
                    }}
                    disabled={deleteMutation.isPending}
                  >
                    Delete
                  </button>
                </div>
              ),
            },
          ]}
        />
      </SectionCard>

      <StudentDetailPanel
        student={selectedStudent}
        instituteName={selectedStudent?.institutes?.name ?? "-"}
        attendance={attendanceQuery.data ?? []}
        payments={paymentsQuery.data ?? []}
        loading={attendanceQuery.isLoading || paymentsQuery.isLoading}
        onClose={() => setSelectedStudentId(null)}
      />
    </div>
  );
}
