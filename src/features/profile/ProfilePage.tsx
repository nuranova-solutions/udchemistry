import { useEffect, useState } from "react";
import { useMutation } from "@tanstack/react-query";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { zodResolver } from "@hookform/resolvers/zod";
import { PageHeader } from "../../components/ui/PageHeader";
import { SectionCard } from "../../components/ui/SectionCard";
import { updateOwnPassword, updateProfile } from "../../lib/api";
import { useAuth } from "../auth/useAuth";

const profileSchema = z.object({
  full_name: z.string().min(2, "Name is required."),
  phone: z.string().optional(),
  password: z.string().optional(),
});

type ProfileFormValues = z.infer<typeof profileSchema>;

export function ProfilePage() {
  const { profile, refreshProfile } = useAuth();
  const [message, setMessage] = useState<string | null>(null);
  const form = useForm<ProfileFormValues>({
    resolver: zodResolver(profileSchema),
    defaultValues: {
      full_name: profile?.full_name ?? "",
      phone: profile?.phone ?? "",
      password: "",
    },
  });

  useEffect(() => {
    form.reset({
      full_name: profile?.full_name ?? "",
      phone: profile?.phone ?? "",
      password: "",
    });
  }, [form, profile]);

  const updateMutation = useMutation({
    mutationFn: (values: ProfileFormValues) =>
      Promise.all([
        updateProfile(profile!.id, {
          full_name: values.full_name,
          phone: values.phone ?? null,
        }),
        values.password?.trim() ? updateOwnPassword(values.password) : Promise.resolve(),
      ]),
    onSuccess: async () => {
      await refreshProfile();
      form.reset({
        full_name: form.getValues("full_name"),
        phone: form.getValues("phone"),
        password: "",
      });
      setMessage("Profile updated successfully.");
    },
  });

  return (
    <div className="page-stack">
      <PageHeader
        title="Profile"
        description="Users can edit only their own profile details. Password changes stay inside Supabase Auth."
      />

      <SectionCard title="Profile details" description="Update your display name and optional phone number.">
        <form
          className="profile-form"
          onSubmit={form.handleSubmit((values) => updateMutation.mutate(values))}
        >
          <div>
            <label className="field-label" htmlFor="full_name">
              Full name
            </label>
            <input id="full_name" className="input" {...form.register("full_name")} />
            {form.formState.errors.full_name ? (
              <p className="error-text">{form.formState.errors.full_name.message}</p>
            ) : null}
          </div>

          <div>
            <label className="field-label" htmlFor="phone">
              Phone
            </label>
            <input id="phone" className="input" {...form.register("phone")} />
          </div>

          <div>
            <label className="field-label" htmlFor="password">
              New password
            </label>
            <input
              id="password"
              className="input"
              type="text"
              placeholder="Leave blank to keep your current password"
              {...form.register("password")}
            />
          </div>

          <div className="profile-meta">
            <p>
              <strong>Email:</strong> {profile?.email}
            </p>
            <p>
              <strong>Role:</strong> {profile?.role}
            </p>
          </div>

          {message ? <p className="helper-text">{message}</p> : null}

          <button className="button" type="submit" disabled={updateMutation.isPending}>
            {updateMutation.isPending ? "Saving..." : "Save changes"}
          </button>
        </form>
      </SectionCard>
    </div>
  );
}
