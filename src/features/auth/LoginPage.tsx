import { useState } from "react";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { zodResolver } from "@hookform/resolvers/zod";
import { Beaker, Eye, EyeOff } from "lucide-react";
import { useAuth } from "./useAuth";

const loginSchema = z.object({
  identifier: z.string().min(2, "Enter your username."),
  password: z.string().min(6, "Password must be at least 6 characters."),
});

type LoginFormValues = z.infer<typeof loginSchema>;

export function LoginPage() {
  const { signIn } = useAuth();
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [showPassword, setShowPassword] = useState(false);

  const form = useForm<LoginFormValues>({
    resolver: zodResolver(loginSchema),
    defaultValues: {
      identifier: "",
      password: "",
    },
  });

  const onSubmit = form.handleSubmit(async (values) => {
    setErrorMessage(null);

    try {
      await signIn(values.identifier, values.password);
    } catch (error) {
      if (error instanceof TypeError && error.message.includes("Failed to fetch")) {
        setErrorMessage("Cannot reach Supabase from this deployment. Redeploy the latest version or check the production connection settings.");
        return;
      }

      if (error instanceof Error) {
        setErrorMessage(error.message);
        return;
      }

      if (typeof error === "object" && error !== null && "message" in error) {
        setErrorMessage(String(error.message));
        return;
      }

      setErrorMessage("Unable to sign in.");
    }
  });

  return (
    <div className="auth-page">
      <div className="auth-card simple">
        <form className="auth-form simple" onSubmit={onSubmit}>
          <div className="auth-form-simple-header">
            <div className="login-class-title">
              <Beaker size={22} />
              <span className="login-class-ud">UD</span>
              <span className="login-class-chemistry">chemistry</span>
            </div>
            <h1>Login</h1>
            <p className="muted-text">Sign in to the class management system.</p>
          </div>

          <div>
            <label className="field-label" htmlFor="identifier">
              Username
            </label>
            <input
              id="identifier"
              className="input"
              placeholder="UDchemistry"
              {...form.register("identifier")}
            />
            {form.formState.errors.identifier ? (
              <p className="error-text">{form.formState.errors.identifier.message}</p>
            ) : null}
          </div>

          <div>
            <label className="field-label" htmlFor="password">
              Password
            </label>
            <div className="input-wrap">
              <input
                id="password"
                type={showPassword ? "text" : "password"}
                className="input with-action"
                placeholder="Enter password"
                {...form.register("password")}
              />
              <button
                className="input-eye"
                type="button"
                onClick={() => setShowPassword((current) => !current)}
                aria-label={showPassword ? "Hide password" : "Show password"}
              >
                {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
              </button>
            </div>
            {form.formState.errors.password ? (
              <p className="error-text">{form.formState.errors.password.message}</p>
            ) : null}
          </div>

          {errorMessage ? <p className="error-text">{errorMessage}</p> : null}

          <button className="button" type="submit" disabled={form.formState.isSubmitting}>
            {form.formState.isSubmitting ? "Signing in..." : "Sign in"}
          </button>
        </form>
      </div>
    </div>
  );
}
