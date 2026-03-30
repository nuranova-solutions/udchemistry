import { Navigate, Outlet, Route, Routes } from "react-router-dom";
import { AppShell } from "../components/layout/AppShell";
import { useAuth } from "../features/auth/useAuth";
import { LoginPage } from "../features/auth/LoginPage";
import { DashboardPage } from "../features/dashboard/DashboardPage";
import { InstitutesPage } from "../features/institutes/InstitutesPage";
import { StaffPage } from "../features/staff/StaffPage";
import { StudentsPage } from "../features/students/StudentsPage";
import { AttendancePage } from "../features/attendance/AttendancePage";
import { PaymentsPage } from "../features/payments/PaymentsPage";
import { ReportsPage } from "../features/reports/ReportsPage";
import { HelpPage } from "../features/help/HelpPage";
import { ProfilePage } from "../features/profile/ProfilePage";
import { PublicQrViewerPage } from "../features/students/PublicQrViewerPage";
import type { Role } from "../types/app";

function LoadingScreen() {
  return (
    <div className="fullscreen-state">
      <div className="state-card">
        <p className="eyebrow">Preparing workspace</p>
        <h2>Loading your chemistry class dashboard...</h2>
      </div>
    </div>
  );
}

function ProfilePendingScreen() {
  return (
    <div className="fullscreen-state">
      <div className="state-card">
        <p className="eyebrow">Profile required</p>
        <h2>Your auth account exists, but the `profiles` record is missing.</h2>
        <p>Create the profile row in Supabase, then reload the app.</p>
      </div>
    </div>
  );
}

function ProtectedRoute() {
  const { user, profile, isLoading } = useAuth();

  if (isLoading) {
    return <LoadingScreen />;
  }

  if (!user) {
    return <Navigate to="/login" replace />;
  }

  if (!profile) {
    return <ProfilePendingScreen />;
  }

  return <Outlet />;
}

function RoleGuard({ roles }: { roles: Role[] }) {
  const { profile } = useAuth();

  if (!profile) {
    return <Navigate to="/login" replace />;
  }

  if (!roles.includes(profile.role)) {
    return <Navigate to="/dashboard" replace />;
  }

  return <Outlet />;
}

export function AppRoutes() {
  const { user, isLoading } = useAuth();

  if (isLoading) {
    return <LoadingScreen />;
  }

  return (
    <Routes>
      <Route
        path="/login"
        element={user ? <Navigate to="/dashboard" replace /> : <LoginPage />}
      />
      <Route path="/qr/:shareToken" element={<PublicQrViewerPage />} />

      <Route element={<ProtectedRoute />}>
        <Route element={<AppShell />}>
          <Route index element={<Navigate to="/dashboard" replace />} />
          <Route path="/dashboard" element={<DashboardPage />} />

          <Route element={<RoleGuard roles={["admin"]} />}>
            <Route path="/institutes" element={<InstitutesPage />} />
            <Route path="/staff" element={<StaffPage />} />
          </Route>

          <Route path="/students" element={<StudentsPage />} />
          <Route path="/attendance" element={<AttendancePage />} />
          <Route path="/payments" element={<PaymentsPage />} />
          <Route path="/reports" element={<ReportsPage />} />
          <Route path="/help" element={<HelpPage />} />
          <Route path="/profile" element={<ProfilePage />} />
        </Route>
      </Route>

      <Route
        path="*"
        element={<Navigate to={user ? "/dashboard" : "/login"} replace />}
      />
    </Routes>
  );
}
