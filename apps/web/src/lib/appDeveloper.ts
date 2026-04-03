export const appDeveloper = {
  company: "Nuranova Solutions",
  email: "nuranovasolutions@gmail.com",
  phone: "0782940117",
} as const;

type DeveloperDetail = {
  label: string;
  value: string;
  href?: string;
};

export const developerDetails: DeveloperDetail[] = [
  {
    label: "Company",
    value: appDeveloper.company,
  },
  {
    label: "Email",
    value: appDeveloper.email,
    href: `mailto:${appDeveloper.email}`,
  },
  {
    label: "Phone",
    value: appDeveloper.phone,
    href: `tel:${appDeveloper.phone}`,
  },
] as const;
