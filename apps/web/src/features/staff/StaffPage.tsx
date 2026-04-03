import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { DataTable } from "../../components/ui/DataTable";
import { PageHeader } from "../../components/ui/PageHeader";
import { SectionCard } from "../../components/ui/SectionCard";
import { StatusBadge } from "../../components/ui/StatusBadge";
import {
  createStaff,
  deleteStaff,
  fetchInstitutes,
  fetchStaff,
  type StaffFormValues,
  updateStaff,
} from "../../lib/api";
import type { StaffRecord } from "../../types/app";
import { useAuth } from "../auth/useAuth";

const emptyForm: StaffFormValues = {
  full_name: "",
  username: "",
  email: "",
  password: "",
  institute_id: "",
  phone: "",
  status: "active",
};

export function StaffPage() {
  const { profile } = useAuth();
  const queryClient = useQueryClient();
  const [editingStaff, setEditingStaff] = useState<StaffRecord | null>(null);
  const [formValues, setFormValues] = useState<StaffFormValues>(emptyForm);
  const [feedback, setFeedback] = useState<string | null>(null);

  const staffQuery = useQuery({
    queryKey: ["staff", profile?.id, profile?.institute_id],
    queryFn: () => fetchStaff(profile!),
    enabled: Boolean(profile),
  });

  const institutesQuery = useQuery({
    queryKey: ["institutes", profile?.id],
    queryFn: () => fetchInstitutes(profile!),
    enabled: Boolean(profile),
  });

  const saveMutation = useMutation({
    mutationFn: async (values: StaffFormValues) => {
      if (editingStaff) {
        await updateStaff(editingStaff.id, values);
        return;
      }

      await createStaff(values);
    },
    onSuccess: async () => {
      setFeedback(editingStaff ? "Staff account updated successfully." : "Staff account created successfully.");
      setEditingStaff(null);
      setFormValues({
        ...emptyForm,
        institute_id: institutesQuery.data?.[0]?.id ?? "",
      });
      await queryClient.invalidateQueries({ queryKey: ["staff"] });
    },
    onError: (error) => {
      setFeedback(error instanceof Error ? error.message : "Unable to save the staff account.");
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (staffId: string) => deleteStaff(staffId),
    onSuccess: async () => {
      setFeedback("Staff account deleted successfully.");
      if (editingStaff) {
        setEditingStaff(null);
      }
      setFormValues({
        ...emptyForm,
        institute_id: institutesQuery.data?.[0]?.id ?? "",
      });
      await queryClient.invalidateQueries({ queryKey: ["staff"] });
    },
    onError: (error) => {
      setFeedback(error instanceof Error ? error.message : "Unable to delete the staff account.");
    },
  });

  function handleChange<K extends keyof StaffFormValues>(key: K, value: StaffFormValues[K]) {
    setFormValues((currentValues) => ({
      ...currentValues,
      [key]: value,
    }));
  }

  function handleEdit(staff: StaffRecord) {
    setEditingStaff(staff);
    setFormValues({
      full_name: staff.full_name,
      username: staff.username,
      email: staff.email,
      password: "",
      institute_id: staff.institute_id ?? "",
      phone: staff.phone ?? "",
      status: staff.status as StaffFormValues["status"],
    });
    setFeedback(null);
  }

  function handleReset() {
    setEditingStaff(null);
    setFormValues({
      ...emptyForm,
      institute_id: institutesQuery.data?.[0]?.id ?? "",
    });
    setFeedback(null);
  }

  const institutes = institutesQuery.data ?? [];

  return (
    <div className="page-stack">
      <PageHeader
        title="Staff"
        description="Admins can create, edit, save, and delete institute staff accounts here."
        actions={
          <button className="button secondary" type="button" onClick={handleReset}>
            Add staff
          </button>
        }
      />

      <SectionCard
        title={editingStaff ? "Edit staff account" : "Add staff account"}
        description="Passwords are required for new staff and optional during edits."
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
              <label className="field-label" htmlFor="staff_full_name">
                Full name
              </label>
              <input
                id="staff_full_name"
                className="input"
                value={formValues.full_name}
                onChange={(event) => handleChange("full_name", event.target.value)}
                required
              />
            </div>

            <div>
              <label className="field-label" htmlFor="staff_username">
                Username
              </label>
              <input
                id="staff_username"
                className="input"
                value={formValues.username}
                onChange={(event) => handleChange("username", event.target.value)}
                required
              />
            </div>

            <div>
              <label className="field-label" htmlFor="staff_email">
                Email
              </label>
              <input
                id="staff_email"
                className="input"
                type="email"
                value={formValues.email}
                onChange={(event) => handleChange("email", event.target.value)}
                required
              />
            </div>

            <div>
              <label className="field-label" htmlFor="staff_password">
                Password
              </label>
              <input
                id="staff_password"
                className="input"
                type="text"
                value={formValues.password ?? ""}
                onChange={(event) => handleChange("password", event.target.value)}
                placeholder={editingStaff ? "Leave blank to keep current password" : "Enter staff password"}
                required={!editingStaff}
              />
            </div>

            <div>
              <label className="field-label" htmlFor="staff_institute">
                Institute
              </label>
              <select
                id="staff_institute"
                className="input"
                value={formValues.institute_id}
                onChange={(event) => handleChange("institute_id", event.target.value)}
                required
              >
                <option value="" disabled>
                  Select institute
                </option>
                {institutes.map((institute) => (
                  <option key={institute.id} value={institute.id}>
                    {institute.name}
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label className="field-label" htmlFor="staff_phone">
                Phone
              </label>
              <input
                id="staff_phone"
                className="input"
                value={formValues.phone}
                onChange={(event) => handleChange("phone", event.target.value)}
              />
            </div>

            <div>
              <label className="field-label" htmlFor="staff_status">
                Status
              </label>
              <select
                id="staff_status"
                className="input"
                value={formValues.status}
                onChange={(event) =>
                  handleChange("status", event.target.value as StaffFormValues["status"])
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
              {saveMutation.isPending ? "Saving..." : editingStaff ? "Save changes" : "Save staff"}
            </button>
            <button className="button ghost" type="button" onClick={handleReset}>
              Clear
            </button>
          </div>
        </form>
      </SectionCard>

      <SectionCard title="Staff directory" description="Each staff account stays tied to one institute at a time.">
        <DataTable
          rows={staffQuery.data ?? []}
          getRowKey={(row) => row.id}
          columns={[
            { header: "Name", render: (row) => row.full_name },
            { header: "Username", render: (row) => row.username },
            { header: "Email", render: (row) => row.email },
            { header: "Institute", render: (row) => row.institutes?.name ?? "-" },
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
    </div>
  );
}
