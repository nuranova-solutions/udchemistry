import { useEffect, useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { DataTable } from "../../components/ui/DataTable";
import { PageHeader } from "../../components/ui/PageHeader";
import { SectionCard } from "../../components/ui/SectionCard";
import { StatCard } from "../../components/ui/StatCard";
import { StatusBadge } from "../../components/ui/StatusBadge";
import {
  createClass,
  deleteClass,
  fetchClasses,
  fetchInstitutes,
  type ClassFormValues,
  updateClass,
} from "../../lib/api";
import { formatCurrency } from "../../lib/utils/formatters";
import type { ClassRecord } from "../../types/app";
import { useAuth } from "../auth/useAuth";

const classTypeOptions = [
  { value: "general", label: "General" },
  { value: "extra", label: "Extra" },
] as const;

const weekdayOptions = [
  { value: "monday", label: "Monday" },
  { value: "tuesday", label: "Tuesday" },
  { value: "wednesday", label: "Wednesday" },
  { value: "thursday", label: "Thursday" },
  { value: "friday", label: "Friday" },
  { value: "saturday", label: "Saturday" },
  { value: "sunday", label: "Sunday" },
] as const;

const weekOfMonthOptions = [
  { value: 1, label: "First week" },
  { value: 2, label: "Second week" },
  { value: 3, label: "Third week" },
  { value: 4, label: "Fourth week" },
  { value: 5, label: "Fifth week" },
] as const;

function todayString() {
  return new Date().toISOString().slice(0, 10);
}

function emptyForm(instituteId = ""): ClassFormValues {
  return {
    name: "",
    institute_id: instituteId,
    al_year: new Date().getFullYear(),
    monthly_fee: 0,
    class_type: "general",
    weekday: "monday",
    start_time: "09:00:00",
    end_time: "11:00:00",
    week_of_month: 1,
    active_from: todayString(),
    active_until: null,
    status: "active",
    notes: "",
  };
}

function labelForClassType(value: string) {
  return classTypeOptions.find((option) => option.value === value)?.label ?? value;
}

function labelForWeekday(value: string) {
  return weekdayOptions.find((option) => option.value === value)?.label ?? value;
}

function labelForWeekOfMonth(value: number | null) {
  return weekOfMonthOptions.find((option) => option.value === value)?.label ?? "-";
}

function displayTime(value: string) {
  return value?.slice(0, 5) ?? value;
}

function normalizeTime(value: string) {
  const trimmed = value.trim();
  if (!trimmed) {
    return "09:00:00";
  }

  if (trimmed.length === 5) {
    return `${trimmed}:00`;
  }

  return trimmed;
}

function classScheduleLabel(classRecord: ClassRecord) {
  const weekdayLabel = labelForWeekday(classRecord.weekday);
  if (classRecord.class_type === "extra" && classRecord.week_of_month) {
    return `${labelForWeekOfMonth(classRecord.week_of_month)} ${weekdayLabel}`;
  }

  return `Every ${weekdayLabel}`;
}

export function ClassesPage() {
  const { profile } = useAuth();
  const queryClient = useQueryClient();
  const [editingClass, setEditingClass] = useState<ClassRecord | null>(null);
  const [feedback, setFeedback] = useState<string | null>(null);
  const [instituteFilter, setInstituteFilter] = useState("all");
  const [yearFilter, setYearFilter] = useState("all");
  const [typeFilter, setTypeFilter] = useState("all");
  const [weekdayFilter, setWeekdayFilter] = useState("all");
  const [formValues, setFormValues] = useState<ClassFormValues>(emptyForm(profile?.institute_id ?? ""));

  const classesQuery = useQuery({
    queryKey: ["classes", profile?.id, profile?.institute_id],
    queryFn: () => fetchClasses(profile!),
    enabled: Boolean(profile),
  });

  const institutesQuery = useQuery({
    queryKey: ["institutes", profile?.id, profile?.institute_id],
    queryFn: () => fetchInstitutes(profile!),
    enabled: Boolean(profile),
  });

  useEffect(() => {
    if (editingClass) {
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
  }, [editingClass, formValues.institute_id, institutesQuery.data, profile]);

  const saveMutation = useMutation({
    mutationFn: async (values: ClassFormValues) => {
      if (editingClass) {
        await updateClass(editingClass.id, values);
        return;
      }

      await createClass(profile!, values);
    },
    onSuccess: async () => {
      setFeedback(editingClass ? "Class updated successfully." : "Class saved successfully.");
      setEditingClass(null);
      setFormValues(emptyForm(profile?.role === "staff" ? profile.institute_id ?? "" : institutesQuery.data?.[0]?.id ?? ""));
      await queryClient.invalidateQueries({ queryKey: ["classes"] });
      await queryClient.invalidateQueries({ queryKey: ["dashboard"] });
      await queryClient.invalidateQueries({ queryKey: ["reports"] });
    },
    onError: (error) => {
      setFeedback(error instanceof Error ? error.message : "Unable to save the class.");
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (classId: string) => deleteClass(classId),
    onSuccess: async () => {
      setFeedback("Class deleted successfully.");
      setEditingClass(null);
      setFormValues(emptyForm(profile?.role === "staff" ? profile.institute_id ?? "" : institutesQuery.data?.[0]?.id ?? ""));
      await queryClient.invalidateQueries({ queryKey: ["classes"] });
      await queryClient.invalidateQueries({ queryKey: ["dashboard"] });
      await queryClient.invalidateQueries({ queryKey: ["reports"] });
    },
    onError: (error) => {
      setFeedback(error instanceof Error ? error.message : "Unable to delete the class.");
    },
  });

  function handleChange<K extends keyof ClassFormValues>(key: K, value: ClassFormValues[K]) {
    setFormValues((currentValues) => ({
      ...currentValues,
      [key]: value,
    }));
  }

  function handleEdit(classRecord: ClassRecord) {
    setEditingClass(classRecord);
    setFormValues({
      name: classRecord.name,
      institute_id: classRecord.institute_id,
      al_year: classRecord.al_year,
      monthly_fee: Number(classRecord.monthly_fee ?? 0),
      class_type: classRecord.class_type,
      weekday: classRecord.weekday,
      start_time: classRecord.start_time,
      end_time: classRecord.end_time,
      week_of_month: classRecord.week_of_month,
      active_from: classRecord.active_from,
      active_until: classRecord.active_until,
      status: classRecord.status,
      notes: classRecord.notes ?? "",
    });
    setFeedback(null);
  }

  function handleReset() {
    setEditingClass(null);
    setFormValues(emptyForm(profile?.role === "staff" ? profile.institute_id ?? "" : institutesQuery.data?.[0]?.id ?? ""));
    setFeedback(null);
  }

  const allClasses = classesQuery.data ?? [];
  const visibleClasses = useMemo(() => allClasses.filter((classRecord) => {
    const matchesInstitute =
      instituteFilter === "all" || classRecord.institute_id === instituteFilter;
    const matchesYear = yearFilter === "all" || String(classRecord.al_year) === yearFilter;
    const matchesType = typeFilter === "all" || classRecord.class_type === typeFilter;
    const matchesWeekday = weekdayFilter === "all" || classRecord.weekday === weekdayFilter;

    return matchesInstitute && matchesYear && matchesType && matchesWeekday;
  }), [allClasses, instituteFilter, typeFilter, weekdayFilter, yearFilter]);

  const activeClasses = allClasses.filter((classRecord) => classRecord.status === "active").length;
  const generalClasses = allClasses.filter((classRecord) => classRecord.class_type === "general").length;
  const extraClasses = allClasses.filter((classRecord) => classRecord.class_type === "extra").length;
  const yearOptions = Array.from(new Set(allClasses.map((classRecord) => String(classRecord.al_year)))).sort();

  return (
    <div className="page-stack">
      <PageHeader
        title="Classes"
        description="Manage weekly general classes and patterned extra classes from the same premium workspace used on mobile."
        actions={(
          <button className="button secondary" type="button" onClick={handleReset}>
            Add class
          </button>
        )}
      />

      <div className="stats-grid stats-grid-compact">
        <StatCard label="Active classes" value={activeClasses} hint="Live schedules now running" />
        <StatCard label="General classes" value={generalClasses} hint="Weekly recurring batches" />
        <StatCard label="Extra classes" value={extraClasses} hint="Patterned add-on sessions" />
      </div>

      <SectionCard
        title={editingClass ? "Edit class" : "Add class"}
        description="Start with the normal weekly class, then add extra classes only when you need a specific monthly week pattern."
      >
        <form
          className="management-form"
          onSubmit={(event) => {
            event.preventDefault();
            saveMutation.mutate({
              ...formValues,
              start_time: normalizeTime(formValues.start_time),
              end_time: normalizeTime(formValues.end_time),
              active_until: formValues.active_until || null,
              week_of_month: formValues.class_type === "extra" ? formValues.week_of_month : null,
            });
          }}
        >
          <div className="form-grid two-columns">
            <div>
              <label className="field-label" htmlFor="class_name">
                Class name
              </label>
              <input
                id="class_name"
                className="input"
                value={formValues.name}
                onChange={(event) => handleChange("name", event.target.value)}
                required
              />
            </div>

            <div>
              <label className="field-label" htmlFor="class_institute">
                Institute
              </label>
              <select
                id="class_institute"
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
              <label className="field-label" htmlFor="class_year">
                A/L year
              </label>
              <input
                id="class_year"
                className="input"
                type="number"
                value={formValues.al_year}
                onChange={(event) => handleChange("al_year", Number(event.target.value))}
                required
              />
            </div>

            <div>
              <label className="field-label" htmlFor="class_fee">
                Monthly fee (LKR)
              </label>
              <input
                id="class_fee"
                className="input"
                type="number"
                min="0"
                step="0.01"
                value={formValues.monthly_fee}
                onChange={(event) => handleChange("monthly_fee", Number(event.target.value))}
                required
              />
            </div>

            <div>
              <label className="field-label" htmlFor="class_type">
                Class type
              </label>
              <select
                id="class_type"
                className="input"
                value={formValues.class_type}
                onChange={(event) => handleChange("class_type", event.target.value as ClassFormValues["class_type"])}
              >
                {classTypeOptions.map((option) => (
                  <option key={option.value} value={option.value}>
                    {option.label}
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label className="field-label" htmlFor="class_weekday">
                Weekday
              </label>
              <select
                id="class_weekday"
                className="input"
                value={formValues.weekday}
                onChange={(event) => handleChange("weekday", event.target.value as ClassFormValues["weekday"])}
              >
                {weekdayOptions.map((option) => (
                  <option key={option.value} value={option.value}>
                    {option.label}
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label className="field-label" htmlFor="class_start">
                Start time
              </label>
              <input
                id="class_start"
                className="input"
                type="time"
                value={displayTime(formValues.start_time)}
                onChange={(event) => handleChange("start_time", normalizeTime(event.target.value))}
                required
              />
            </div>

            <div>
              <label className="field-label" htmlFor="class_end">
                End time
              </label>
              <input
                id="class_end"
                className="input"
                type="time"
                value={displayTime(formValues.end_time)}
                onChange={(event) => handleChange("end_time", normalizeTime(event.target.value))}
                required
              />
            </div>

            {formValues.class_type === "extra" ? (
              <div>
                <label className="field-label" htmlFor="class_week_of_month">
                  Week of month
                </label>
                <select
                  id="class_week_of_month"
                  className="input"
                  value={String(formValues.week_of_month ?? 1)}
                  onChange={(event) => handleChange("week_of_month", Number(event.target.value))}
                >
                  {weekOfMonthOptions.map((option) => (
                    <option key={option.value} value={option.value}>
                      {option.label}
                    </option>
                  ))}
                </select>
              </div>
            ) : null}

            <div>
              <label className="field-label" htmlFor="class_active_from">
                Active from
              </label>
              <input
                id="class_active_from"
                className="input"
                type="date"
                value={formValues.active_from}
                onChange={(event) => handleChange("active_from", event.target.value)}
                required
              />
            </div>

            <div>
              <label className="field-label" htmlFor="class_active_until">
                Active until
              </label>
              <input
                id="class_active_until"
                className="input"
                type="date"
                value={formValues.active_until ?? ""}
                onChange={(event) => handleChange("active_until", event.target.value || null)}
              />
            </div>

            <div>
              <label className="field-label" htmlFor="class_status">
                Status
              </label>
              <select
                id="class_status"
                className="input"
                value={formValues.status}
                onChange={(event) => handleChange("status", event.target.value as ClassFormValues["status"])}
              >
                <option value="active">Active</option>
                <option value="inactive">Inactive</option>
              </select>
            </div>

            <div className="form-span-two">
              <label className="field-label" htmlFor="class_notes">
                Notes
              </label>
              <textarea
                id="class_notes"
                className="input textarea"
                value={formValues.notes}
                onChange={(event) => handleChange("notes", event.target.value)}
                placeholder="Optional notes for admin or staff"
              />
            </div>
          </div>

          {feedback ? (
            <p className={feedback.toLowerCase().includes("unable") || feedback.toLowerCase().includes("migration") ? "error-text" : "helper-text"}>
              {feedback}
            </p>
          ) : (
            <p className="helper-text">
              Past class days should stay unchanged when schedules change. This page handles the class setup layer first.
            </p>
          )}

          <div className="inline-actions">
            <button className="button" type="submit" disabled={saveMutation.isPending}>
              {saveMutation.isPending ? "Saving..." : editingClass ? "Save changes" : "Save class"}
            </button>
            <button className="button ghost" type="button" onClick={handleReset}>
              Clear
            </button>
          </div>
        </form>
      </SectionCard>

      <SectionCard
        title="Class roster"
        description="Filter by institute, year, type, or weekday, then edit the schedule details in place."
        actions={(
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
              {yearOptions.map((year) => (
                <option key={year} value={year}>
                  {year}
                </option>
              ))}
            </select>

            <select
              className="input filter-input"
              value={typeFilter}
              onChange={(event) => setTypeFilter(event.target.value)}
            >
              <option value="all">All types</option>
              {classTypeOptions.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>

            <select
              className="input filter-input"
              value={weekdayFilter}
              onChange={(event) => setWeekdayFilter(event.target.value)}
            >
              <option value="all">All weekdays</option>
              {weekdayOptions.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
          </div>
        )}
      >
        <DataTable
          rows={visibleClasses}
          getRowKey={(row) => row.id}
          columns={[
            {
              header: "Class",
              render: (row) => (
                <div className="table-primary-cell">
                  <strong>{row.name}</strong>
                  <span>{row.notes || `${row.al_year} batch`}</span>
                </div>
              ),
            },
            { header: "Type", render: (row) => labelForClassType(row.class_type) },
            { header: "Schedule", render: (row) => `${classScheduleLabel(row)} · ${displayTime(row.start_time)} - ${displayTime(row.end_time)}` },
            { header: "Institute", render: (row) => row.institutes?.name ?? "-" },
            { header: "Fee", render: (row) => formatCurrency(Number(row.monthly_fee ?? 0)) },
            { header: "Dates", render: (row) => `${row.active_from}${row.active_until ? ` to ${row.active_until}` : ""}` },
            {
              header: "Status",
              render: (row) => (
                <StatusBadge
                  label={row.status}
                  tone={row.status === "active" ? "success" : "warning"}
                />
              ),
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
                      const confirmed = window.confirm(`Delete ${row.name}?`);

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
          emptyMessage="No classes match the current filters yet."
        />
      </SectionCard>
    </div>
  );
}
