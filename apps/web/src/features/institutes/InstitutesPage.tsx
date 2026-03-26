import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { DataTable } from "../../components/ui/DataTable";
import { PageHeader } from "../../components/ui/PageHeader";
import { SectionCard } from "../../components/ui/SectionCard";
import { StatusBadge } from "../../components/ui/StatusBadge";
import {
  createInstitute,
  deleteInstitute,
  fetchInstitutes,
  type InstituteFormValues,
  updateInstitute,
} from "../../lib/api";
import { formatDate } from "../../lib/utils/formatters";
import type { Institute } from "../../types/app";
import { useAuth } from "../auth/useAuth";

const emptyForm: InstituteFormValues = {
  name: "",
  code: "",
  address: "",
  contact_no: "",
  status: "active",
};

export function InstitutesPage() {
  const { profile } = useAuth();
  const queryClient = useQueryClient();
  const [editingInstitute, setEditingInstitute] = useState<Institute | null>(null);
  const [formValues, setFormValues] = useState<InstituteFormValues>(emptyForm);
  const [feedback, setFeedback] = useState<string | null>(null);

  const institutesQuery = useQuery({
    queryKey: ["institutes", profile?.id, profile?.institute_id],
    queryFn: () => fetchInstitutes(profile!),
    enabled: Boolean(profile),
  });

  const saveMutation = useMutation({
    mutationFn: async (values: InstituteFormValues) => {
      if (editingInstitute) {
        await updateInstitute(editingInstitute.id, values);
        return;
      }

      await createInstitute(values);
    },
    onSuccess: async () => {
      setFeedback(editingInstitute ? "Institute updated successfully." : "Institute created successfully.");
      setEditingInstitute(null);
      setFormValues(emptyForm);
      await queryClient.invalidateQueries({ queryKey: ["institutes"] });
    },
    onError: (error) => {
      setFeedback(error instanceof Error ? error.message : "Unable to save the institute.");
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (instituteId: string) => deleteInstitute(instituteId),
    onSuccess: async () => {
      setFeedback("Institute deleted successfully.");
      if (editingInstitute) {
        setEditingInstitute(null);
        setFormValues(emptyForm);
      }
      await queryClient.invalidateQueries({ queryKey: ["institutes"] });
      await queryClient.invalidateQueries({ queryKey: ["students"] });
      await queryClient.invalidateQueries({ queryKey: ["staff"] });
      await queryClient.invalidateQueries({ queryKey: ["dashboard"] });
    },
    onError: (error) => {
      setFeedback(error instanceof Error ? error.message : "Unable to delete the institute.");
    },
  });

  function handleChange<K extends keyof InstituteFormValues>(key: K, value: InstituteFormValues[K]) {
    setFormValues((currentValues) => ({
      ...currentValues,
      [key]: value,
    }));
  }

  function handleEdit(institute: Institute) {
    setEditingInstitute(institute);
    setFormValues({
      name: institute.name,
      code: institute.code,
      address: institute.address ?? "",
      contact_no: institute.contact_no ?? "",
      status: institute.status as InstituteFormValues["status"],
    });
    setFeedback(null);
  }

  function handleReset() {
    setEditingInstitute(null);
    setFormValues(emptyForm);
    setFeedback(null);
  }

  return (
    <div className="page-stack">
      <PageHeader
        title="Institutes"
        description="Admins can add, edit, save, and delete institute details from one management workspace."
        actions={
          <button className="button secondary" type="button" onClick={handleReset}>
            Add institute
          </button>
        }
      />

      <SectionCard
        title={editingInstitute ? "Edit institute" : "Add institute"}
        description="Update institute details here, then save them directly to Supabase."
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
              <label className="field-label" htmlFor="institute_name">
                Institute name
              </label>
              <input
                id="institute_name"
                className="input"
                value={formValues.name}
                onChange={(event) => handleChange("name", event.target.value)}
                required
              />
            </div>

            <div>
              <label className="field-label" htmlFor="institute_code">
                Code
              </label>
              <input
                id="institute_code"
                className="input"
                value={formValues.code}
                onChange={(event) => handleChange("code", event.target.value)}
                required
              />
            </div>

            <div>
              <label className="field-label" htmlFor="institute_contact">
                Contact number
              </label>
              <input
                id="institute_contact"
                className="input"
                value={formValues.contact_no}
                onChange={(event) => handleChange("contact_no", event.target.value)}
              />
            </div>

            <div>
              <label className="field-label" htmlFor="institute_status">
                Status
              </label>
              <select
                id="institute_status"
                className="input"
                value={formValues.status}
                onChange={(event) =>
                  handleChange("status", event.target.value as InstituteFormValues["status"])
                }
              >
                <option value="active">Active</option>
                <option value="inactive">Inactive</option>
              </select>
            </div>
          </div>

          <div>
            <label className="field-label" htmlFor="institute_address">
              Address
            </label>
            <textarea
              id="institute_address"
              className="input textarea"
              value={formValues.address}
              onChange={(event) => handleChange("address", event.target.value)}
              rows={3}
            />
          </div>

          {feedback ? (
            <p className={feedback.toLowerCase().includes("unable") ? "error-text" : "helper-text"}>
              {feedback}
            </p>
          ) : null}

          <div className="inline-actions">
            <button className="button" type="submit" disabled={saveMutation.isPending}>
              {saveMutation.isPending ? "Saving..." : editingInstitute ? "Save changes" : "Save institute"}
            </button>
            <button className="button ghost" type="button" onClick={handleReset}>
              Clear
            </button>
          </div>
        </form>
      </SectionCard>

      <SectionCard
        title="Institute directory"
        description="Editing or deleting an institute immediately updates the live admin data."
      >
        <DataTable
          rows={institutesQuery.data ?? []}
          getRowKey={(row) => row.id}
          columns={[
            { header: "Code", render: (row) => row.code },
            { header: "Institute", render: (row) => row.name },
            { header: "Contact", render: (row) => row.contact_no ?? "-" },
            { header: "Created", render: (row) => formatDate(row.created_at) },
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
                      const confirmed = window.confirm(
                        `Delete ${row.name}? This also removes linked students, attendance, and payment records.`,
                      );

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
