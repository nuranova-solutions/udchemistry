import { Outlet } from "react-router-dom";
import { Sidebar } from "./Sidebar";
import { TopBar } from "./TopBar";

export function AppShell() {
  return (
    <div className="app-shell">
      <Sidebar />
      <div className="content-panel">
        <TopBar />
        <main className="page-content">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
