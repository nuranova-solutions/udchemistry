import type { ReactNode } from "react";

interface PageHeaderProps {
  title: string;
  description: string;
  actions?: ReactNode;
}

export function PageHeader({ title, description, actions }: PageHeaderProps) {
  return (
    <div className="page-header">
      <div>
        <p className="eyebrow">Web admin panel</p>
        <h1>{title}</h1>
        <p>{description}</p>
      </div>

      {actions ? <div className="page-header-actions">{actions}</div> : null}
    </div>
  );
}
