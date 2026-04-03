import { format } from "date-fns";

export function formatCurrency(value: number) {
  return new Intl.NumberFormat("en-LK", {
    style: "currency",
    currency: "LKR",
    maximumFractionDigits: 2,
  }).format(value);
}

export function formatDate(value: string | null | undefined, pattern = "dd MMM yyyy") {
  if (!value) {
    return "-";
  }

  return format(new Date(value), pattern);
}

export function currentMonthLabel() {
  return format(new Date(), "MMMM yyyy");
}
