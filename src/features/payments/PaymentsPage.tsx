import { useEffect, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { DataTable } from "../../components/ui/DataTable";
import { PageHeader } from "../../components/ui/PageHeader";
import { SectionCard } from "../../components/ui/SectionCard";
import { StatusBadge } from "../../components/ui/StatusBadge";
import {
  createPayment,
  deletePayment,
  fetchPayments,
  fetchStudents,
  type PaymentFormValues,
  updatePayment,
} from "../../lib/api";
import { formatCurrency, formatDate } from "../../lib/utils/formatters";
import type { PaymentRecord } from "../../types/app";
import { useAuth } from "../auth/useAuth";

function todayString() {
  return new Date().toISOString().slice(0, 10);
}

function emptyForm(): PaymentFormValues {
  const now = new Date();

  return {
    student_id: "",
    payment_month: now.getMonth() + 1,
    payment_year: now.getFullYear(),
    amount: 0,
    paid: true,
    paid_date: todayString(),
  };
}

export function PaymentsPage() {
  const { profile } = useAuth();
  const queryClient = useQueryClient();
  const [editingPayment, setEditingPayment] = useState<PaymentRecord | null>(null);
  const [formValues, setFormValues] = useState<PaymentFormValues>(emptyForm());
  const [feedback, setFeedback] = useState<string | null>(null);
  const [monthFilter, setMonthFilter] = useState("all");
  const [yearFilter, setYearFilter] = useState("all");

  const paymentsQuery = useQuery({
    queryKey: ["payments", profile?.id, profile?.institute_id],
    queryFn: () => fetchPayments(profile!),
    enabled: Boolean(profile),
  });

  const studentsQuery = useQuery({
    queryKey: ["students", profile?.id, profile?.institute_id],
    queryFn: () => fetchStudents(profile!),
    enabled: Boolean(profile),
  });

  useEffect(() => {
    if (editingPayment) {
      return;
    }

    if (!formValues.student_id && studentsQuery.data?.[0]?.id) {
      setFormValues((currentValues) => ({
        ...currentValues,
        student_id: studentsQuery.data?.[0]?.id ?? "",
      }));
    }
  }, [editingPayment, formValues.student_id, studentsQuery.data]);

  const saveMutation = useMutation({
    mutationFn: async (values: PaymentFormValues) => {
      if (editingPayment) {
        await updatePayment(profile!, editingPayment.id, values);
        return;
      }

      await createPayment(profile!, values);
    },
    onSuccess: async () => {
      setFeedback(editingPayment ? "Payment updated successfully." : "Payment record added successfully.");
      setEditingPayment(null);
      setFormValues({
        ...emptyForm(),
        student_id: studentsQuery.data?.[0]?.id ?? "",
      });
      await queryClient.invalidateQueries({ queryKey: ["payments"] });
      await queryClient.invalidateQueries({ queryKey: ["dashboard"] });
      await queryClient.invalidateQueries({ queryKey: ["reports"] });
    },
    onError: (error) => {
      setFeedback(error instanceof Error ? error.message : "Unable to save the payment.");
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (paymentId: string) => deletePayment(paymentId),
    onSuccess: async () => {
      setFeedback("Payment deleted successfully.");
      setEditingPayment(null);
      setFormValues({
        ...emptyForm(),
        student_id: studentsQuery.data?.[0]?.id ?? "",
      });
      await queryClient.invalidateQueries({ queryKey: ["payments"] });
      await queryClient.invalidateQueries({ queryKey: ["dashboard"] });
      await queryClient.invalidateQueries({ queryKey: ["reports"] });
    },
    onError: (error) => {
      setFeedback(error instanceof Error ? error.message : "Unable to delete the payment.");
    },
  });

  function handleChange<K extends keyof PaymentFormValues>(key: K, value: PaymentFormValues[K]) {
    setFormValues((currentValues) => ({
      ...currentValues,
      [key]: value,
    }));
  }

  function handleEdit(record: PaymentRecord) {
    setEditingPayment(record);
    setFormValues({
      student_id: record.student_id,
      payment_month: record.payment_month,
      payment_year: record.payment_year,
      amount: Number(record.amount),
      paid: record.paid,
      paid_date: record.paid_date,
    });
    setFeedback(null);
  }

  function handleReset() {
    setEditingPayment(null);
    setFormValues({
      ...emptyForm(),
      student_id: studentsQuery.data?.[0]?.id ?? "",
    });
    setFeedback(null);
  }

  const paymentRows = paymentsQuery.data ?? [];
  const visiblePayments = paymentRows.filter((payment) => {
    const matchesMonth = monthFilter === "all" || String(payment.payment_month) === monthFilter;
    const matchesYear = yearFilter === "all" || String(payment.payment_year) === yearFilter;

    return matchesMonth && matchesYear;
  });

  return (
    <div className="page-stack">
      <PageHeader
        title="Payments"
        description="Admins can add, edit, save, and delete monthly payment records without leaving the web panel."
      />

      <SectionCard
        title={editingPayment ? "Edit payment" : "Add payment"}
        description="Payment actions from attendance scans still work, and this form handles manual corrections."
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
              <label className="field-label" htmlFor="payment_student">
                Student
              </label>
              <select
                id="payment_student"
                className="input"
                value={formValues.student_id}
                onChange={(event) => handleChange("student_id", event.target.value)}
                required
              >
                <option value="" disabled>
                  Select student
                </option>
                {(studentsQuery.data ?? []).map((student) => (
                  <option key={student.id} value={student.id}>
                    {student.full_name}
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label className="field-label" htmlFor="payment_amount">
                Amount
              </label>
              <input
                id="payment_amount"
                className="input"
                type="number"
                min="0"
                step="0.01"
                value={formValues.amount}
                onChange={(event) => handleChange("amount", Number(event.target.value))}
                required
              />
            </div>

            <div>
              <label className="field-label" htmlFor="payment_month">
                Month
              </label>
              <input
                id="payment_month"
                className="input"
                type="number"
                min="1"
                max="12"
                value={formValues.payment_month}
                onChange={(event) => handleChange("payment_month", Number(event.target.value))}
                required
              />
            </div>

            <div>
              <label className="field-label" htmlFor="payment_year">
                Year
              </label>
              <input
                id="payment_year"
                className="input"
                type="number"
                min="2024"
                value={formValues.payment_year}
                onChange={(event) => handleChange("payment_year", Number(event.target.value))}
                required
              />
            </div>

            <div>
              <label className="field-label" htmlFor="payment_status">
                Payment status
              </label>
              <select
                id="payment_status"
                className="input"
                value={formValues.paid ? "paid" : "unpaid"}
                onChange={(event) => handleChange("paid", event.target.value === "paid")}
              >
                <option value="paid">Paid</option>
                <option value="unpaid">Unpaid</option>
              </select>
            </div>

            <div>
              <label className="field-label" htmlFor="payment_date">
                Paid date
              </label>
              <input
                id="payment_date"
                className="input"
                type="date"
                value={formValues.paid_date ?? ""}
                onChange={(event) => handleChange("paid_date", event.target.value || null)}
                disabled={!formValues.paid}
              />
            </div>
          </div>

          {feedback ? (
            <p className={feedback.toLowerCase().includes("unable") ? "error-text" : "helper-text"}>
              {feedback}
            </p>
          ) : null}

          <div className="inline-actions">
            <button className="button" type="submit" disabled={saveMutation.isPending}>
              {saveMutation.isPending ? "Saving..." : editingPayment ? "Save changes" : "Save payment"}
            </button>
            <button className="button ghost" type="button" onClick={handleReset}>
              Clear
            </button>
          </div>
        </form>
      </SectionCard>

      <SectionCard
        title="Payment records"
        description="Filter recent records by month or year, then edit or remove entries as needed."
        actions={
          <div className="filter-row">
            <select
              className="input filter-input"
              value={monthFilter}
              onChange={(event) => setMonthFilter(event.target.value)}
            >
              <option value="all">All months</option>
              {Array.from(new Set(paymentRows.map((payment) => String(payment.payment_month)))).map((month) => (
                <option key={month} value={month}>
                  Month {month}
                </option>
              ))}
            </select>

            <select
              className="input filter-input"
              value={yearFilter}
              onChange={(event) => setYearFilter(event.target.value)}
            >
              <option value="all">All years</option>
              {Array.from(new Set(paymentRows.map((payment) => String(payment.payment_year)))).map((year) => (
                <option key={year} value={year}>
                  {year}
                </option>
              ))}
            </select>
          </div>
        }
      >
        <DataTable
          rows={visiblePayments}
          getRowKey={(row) => row.id}
          columns={[
            { header: "Student", render: (row) => row.students?.full_name ?? "-" },
            { header: "Month", render: (row) => `${row.payment_month}/${row.payment_year}` },
            { header: "Amount", render: (row) => formatCurrency(Number(row.amount)) },
            {
              header: "Paid",
              render: (row) => (
                <StatusBadge label={row.paid ? "Paid" : "Unpaid"} tone={row.paid ? "success" : "warning"} />
              ),
            },
            { header: "Paid date", render: (row) => formatDate(row.paid_date) },
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
                      const confirmed = window.confirm("Delete this payment record?");

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
    </div>
  );
}
