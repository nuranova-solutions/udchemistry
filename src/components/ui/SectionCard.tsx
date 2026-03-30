import type { ReactNode } from "react";
import { motion } from "framer-motion";

interface SectionCardProps {
  title: string;
  description?: string;
  children: ReactNode;
  actions?: ReactNode;
}

export function SectionCard({ title, description, children, actions }: SectionCardProps) {
  return (
    <motion.section
      className="section-card"
      initial={{ opacity: 0, y: 14 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.35 }}
    >
      <div className="section-card-header">
        <div>
          <h3>{title}</h3>
          {description ? <p>{description}</p> : null}
        </div>
        {actions ? <div>{actions}</div> : null}
      </div>

      {children}
    </motion.section>
  );
}
