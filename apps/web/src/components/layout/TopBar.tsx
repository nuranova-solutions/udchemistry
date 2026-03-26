import { useLocation, useNavigate } from "react-router-dom";
import { LogOut } from "lucide-react";
import { useAuth } from "../../features/auth/useAuth";

const routeTitles: Record<string, string> = {
  "/dashboard": "Dashboard",
  "/institutes": "Institutes",
  "/staff": "Staff",
  "/students": "Students",
  "/attendance": "Attendance",
  "/payments": "Payments",
  "/reports": "Reports",
  "/profile": "Profile",
};

export function TopBar() {
  const location = useLocation();
  const navigate = useNavigate();
  const { profile, signOut } = useAuth();

  const title = routeTitles[location.pathname] ?? "Chemistry Class Manager";

  return (
    <header className="topbar">
      <div>
        <p className="eyebrow">Chemistry class QR attendance system</p>
        <h2>{title}</h2>
      </div>

      <div className="topbar-actions">
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
