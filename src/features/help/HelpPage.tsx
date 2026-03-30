import { useAuth } from "../auth/useAuth";
import { PageHeader } from "../../components/ui/PageHeader";
import { SectionCard } from "../../components/ui/SectionCard";

const pageGuides = [
  {
    title: "Dashboard",
    description: "View the main activity for your workspace.",
  },
  {
    title: "Students",
    description: "Register students, open QR codes, and share QR images.",
  },
  {
    title: "Attendance",
    description: "Scan QR codes or correct attendance records manually.",
  },
  {
    title: "Payments",
    description: "Add, edit, and review monthly payment records.",
  },
  {
    title: "Summary",
    description: "Export CSV and PDF reports when needed.",
  },
  {
    title: "Settings",
    description: "Update your account details and password.",
  },
];

export function HelpPage() {
  const { profile } = useAuth();
  const roleLabel = profile?.role === "admin" ? "Admin" : "Staff";

  return (
    <div className="page-stack">
      <PageHeader
        title="Help and how to use the app"
        description="Use this page to learn the basic flow for sign-in, dashboard use, QR scanning, and monthly records."
        actions={
          <div className="help-chip-row">
            <span className="info-chip">{roleLabel}</span>
            <span className="info-chip">QR attendance</span>
            <span className="info-chip">Payments + students</span>
          </div>
        }
      />

      <SectionCard
        title="Start here"
        description="The quickest way to begin using the app."
      >
        <div className="help-list">
          <div className="help-list-item">1. Sign in with your username and password.</div>
          <div className="help-list-item">
            2. Open the dashboard to see attendance, unpaid students, and shortcuts.
          </div>
          <div className="help-list-item">
            3. Use the attendance page to scan a student QR code or add attendance manually.
          </div>
        </div>
      </SectionCard>

      <SectionCard
        title="Main pages"
        description="What each important page is used for."
      >
        <div className="help-grid">
          {pageGuides.map((guide) => (
            <div key={guide.title} className="help-card">
              <strong>{guide.title}</strong>
              <p>{guide.description}</p>
            </div>
          ))}
        </div>
      </SectionCard>

      <SectionCard
        title="Good workflow"
        description="A simple daily routine for staff and admins."
      >
        <div className="help-list">
          <div className="help-list-item">Check the dashboard first.</div>
          <div className="help-list-item">Scan students as they arrive.</div>
          <div className="help-list-item">Review unpaid students and payment updates.</div>
          <div className="help-list-item">Open settings when you need to update your profile.</div>
        </div>
      </SectionCard>
    </div>
  );
}
