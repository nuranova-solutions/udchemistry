import { useLocation, useNavigate } from "react-router-dom";
import { LogOut, MoonStar, SunMedium } from "lucide-react";
import { useAuth } from "../../features/auth/useAuth";
import { useTheme } from "../../features/theme/ThemeProvider";

const routeTitles: Record<string, string> = {
  "/dashboard": "Dashboard",
  "/institutes": "Institutes",
  "/staff": "Staff",
  "/classes": "Classes",
  "/students": "Students",
  "/attendance": "Attendance",
  "/payments": "Payments",
  "/reports": "Summary",
  "/help": "Help",
  "/profile": "Settings",
};

export function TopBar() {
  const location = useLocation();
  const navigate = useNavigate();
  const { profile, signOut } = useAuth();
  const { resolvedTheme, toggleThemeMode } = useTheme();

  const title = routeTitles[location.pathname] ?? "Chemistry Class Manager";

  return (
    <header className="topbar">
      <div>
        <p className="eyebrow">Chemistry class QR attendance system</p>
        <h2>{title}</h2>
      </div>

      <div className="topbar-actions">
        <button
          className="button ghost small-button icon-button theme-toggle-button"
          type="button"
          onClick={toggleThemeMode}
        >
          {resolvedTheme === "dark" ? <SunMedium size={16} /> : <MoonStar size={16} />}
          {resolvedTheme === "dark" ? "Light mode" : "Dark mode"}
        </button>

        <div className="topbar-user">
          <strong>{profile?.full_name ?? "User"}</strong>
          <span>{profile?.role === "admin" ? "Admin" : "Institute staff"}</span>
        </div>

        <button
          className="button secondary"
          type="button"
          onClick={async () => {
            await signOut();
            navigate("/login", { replace: true });
          }}
        >
          <LogOut size={16} />
          Sign out
        </button>
      </div>
    </header>
  );
}
