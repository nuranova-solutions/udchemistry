import { useQuery } from "@tanstack/react-query";
import {
  Bar,
  BarChart,
  CartesianGrid,
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import { PageHeader } from "../../components/ui/PageHeader";
import { SectionCard } from "../../components/ui/SectionCard";
import { StatCard } from "../../components/ui/StatCard";
import { fetchDashboardData } from "../../lib/api";
import { formatCurrency } from "../../lib/utils/formatters";
import { useAuth } from "../auth/useAuth";

export function DashboardPage() {
  const { profile } = useAuth();
  const dashboardQuery = useQuery({
    queryKey: ["dashboard", profile?.id, profile?.institute_id],
    queryFn: () => fetchDashboardData(profile!),
    enabled: Boolean(profile),
  });

  const data = dashboardQuery.data;

  return (
    <div className="page-stack">
      <PageHeader
        title="Operations dashboard"
        description="Track attendance, payments, and new registrations in the same premium admin workspace."
      />

      <div className="stats-grid">
        {(data?.metrics ?? []).map((metric) => (
          <StatCard key={metric.label} label={metric.label} value={metric.value} hint={metric.hint} />
        ))}
      </div>

      <div className="chart-grid">
        <SectionCard title="Attendance trend" description="Daily attendance activity from your latest scan windows.">
          <div className="chart-shell">
            <ResponsiveContainer width="100%" height={280}>
              <LineChart data={data?.attendanceTrend ?? []}>
                <CartesianGrid strokeDasharray="3 3" vertical={false} />
                <XAxis dataKey="label" />
                <YAxis allowDecimals={false} />
                <Tooltip />
                <Line type="monotone" dataKey="value" stroke="#8C63FF" strokeWidth={3} />
              </LineChart>
            </ResponsiveContainer>
          </div>
        </SectionCard>

        <SectionCard title="Income trend" description="Paid monthly records grouped by month.">
          <div className="chart-shell">
            <ResponsiveContainer width="100%" height={280}>
              <BarChart data={data?.incomeTrend ?? []}>
                <CartesianGrid strokeDasharray="3 3" vertical={false} />
                <XAxis dataKey="label" />
                <YAxis />
                <Tooltip formatter={(value) => formatCurrency(Number(value ?? 0))} />
                <Bar dataKey="value" fill="#1F2A48" radius={[10, 10, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </SectionCard>
      </div>

      <SectionCard title="New registration trend" description="A/L intake growth for the most recent months.">
        <div className="chart-shell">
          <ResponsiveContainer width="100%" height={280}>
            <BarChart data={data?.registrationTrend ?? []}>
              <CartesianGrid strokeDasharray="3 3" vertical={false} />
              <XAxis dataKey="label" />
              <YAxis allowDecimals={false} />
              <Tooltip />
              <Bar dataKey="value" fill="#A47FFF" radius={[10, 10, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </div>
      </SectionCard>
    </div>
  );
}
