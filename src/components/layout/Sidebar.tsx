import { NavLink } from "react-router-dom";
import {
  ChartNoAxesCombined,
  CircleDollarSign,
  ClipboardCheck,
  CircleHelp,
  FileSpreadsheet,
  GraduationCap,
  School,
  Settings2,
  Users,
} from "lucide-react";
import { useAuth } from "../../features/auth/useAuth";
import { cn } from "../../lib/utils/cn";
import type { Role } from "../../types/app";

const navItems: Array<{
  to: string;
  label: string;
  icon: typeof ChartNoAxesCombined;
  roles: Role[];
}> = [
  { to: "/dashboard", label: "Dashboard", icon: ChartNoAxesCombined, roles: ["admin", "staff"] },
  { to: "/institutes", label: "Institutes", icon: School, roles: ["admin"] },
  { to: "/staff", label: "Staff", icon: Users, roles: ["admin"] },
  { to: "/students", label: "Students", icon: GraduationCap, roles: ["admin", "staff"] },
  { to: "/attendance", label: "Attendance", icon: ClipboardCheck, roles: ["admin", "staff"] },
  { to: "/payments", label: "Payments", icon: CircleDollarSign, roles: ["admin", "staff"] },
  { to: "/reports", label: "Reports", icon: FileSpreadsheet, roles: ["admin", "staff"] },
  { to: "/help", label: "Help", icon: CircleHelp, roles: ["admin", "staff"] },
  { to: "/profile", label: "Profile", icon: Settings2, roles: ["admin", "staff"] },
];

export function Sidebar() {
  const { profile } = useAuth();

  return (
    <aside className="sidebar">
      <div className="sidebar-brand">
        <span className="sidebar-mark">NC</span>
        <div>
          <strong>UD chemistry</strong>
          <p>{profile?.role === "admin" ? "Admin workspace" : "Institute workspace"}</p>
        </div>
      </div>

      <nav className="sidebar-nav">
        {navItems
          .filter((item) => item.roles.includes(profile?.role ?? "staff"))
          .map((item) => {
            const Icon = item.icon;

            return (
              <NavLink
                key={item.to}
                to={item.to}
                className={({ isActive }) => cn("nav-link", isActive && "active")}
              >
                <Icon size={18} />
                <span>{item.label}</span>
              </NavLink>
            );
          })}
      </nav>

      <div className="sidebar-footer">
        <p className="eyebrow">Institute scope</p>
        <strong>{profile?.role === "admin" ? "All institutes" : profile?.institute_id ?? "Assigned by admin"}</strong>
      </div>
    </aside>
  );
}
