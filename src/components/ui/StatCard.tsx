import { motion } from "framer-motion";

interface StatCardProps {
  label: string;
  value: number;
  hint: string;
}

export function StatCard({ label, value, hint }: StatCardProps) {
  return (
    <motion.div
      className="stat-card"
      initial={{ opacity: 0, y: 18 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.35 }}
    >
      <p>{label}</p>
      <strong>{value}</strong>
      <span>{hint}</span>
    </motion.div>
  );
}
