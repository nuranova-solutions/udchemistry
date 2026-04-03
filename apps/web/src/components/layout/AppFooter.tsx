import { appDeveloper } from "../../lib/appDeveloper";

export function AppFooter() {
  return (
    <footer className="app-footer">
      <div className="app-footer-copy">
        <strong>{appDeveloper.company}</strong>
        <span>2026 All rights reserved</span>
      </div>

      <div className="app-footer-links">
        <a href={`mailto:${appDeveloper.email}`}>{appDeveloper.email}</a>
        <a href={`tel:${appDeveloper.phone}`}>{appDeveloper.phone}</a>
      </div>
    </footer>
  );
}
