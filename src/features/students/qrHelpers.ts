import type { QrCodeRecord, Student } from "../../types/app";

function normalizeUrl(value: string | null | undefined) {
  const trimmed = value?.trim();

  if (!trimmed) {
    return null;
  }

  return trimmed.replace(/\/+$/, "");
}

function extractShareToken(qrLink: string | null | undefined) {
  if (!qrLink) {
    return null;
  }

  try {
    const url = new URL(qrLink);
    const segments = url.pathname.split("/").filter(Boolean);
    const qrIndex = segments.findIndex((segment) => segment === "qr");

    if (qrIndex >= 0) {
      return segments[qrIndex + 1] ?? null;
    }
  } catch {
    const match = qrLink.match(/\/qr\/([^/?#]+)/i);
    return match?.[1] ?? null;
  }

  return null;
}

function currentQrBaseUrl() {
  const runtimeOrigin =
    typeof window !== "undefined" ? normalizeUrl(window.location.origin) : null;
  const configuredOrigin = normalizeUrl(import.meta.env.VITE_APP_URL as string | undefined);

  return runtimeOrigin ?? configuredOrigin;
}

function resolveShareToken(
  qrCode: Pick<QrCodeRecord, "share_token"> | null | undefined,
  qrLink: string | null | undefined,
) {
  return qrCode?.share_token ?? extractShareToken(qrLink);
}

export function resolveStudentQrLink(
  student: Pick<Student, "qr_link" | "qr_codes">,
) {
  const shareToken = resolveShareToken(student.qr_codes, student.qr_link);
  const baseUrl = currentQrBaseUrl();

  if (shareToken && baseUrl) {
    return `${baseUrl}/qr/${shareToken}`;
  }

  return student.qr_link;
}

export function resolveStudentQrImage(
  student: Pick<Student, "qr_codes">,
) {
  return student.qr_codes?.qr_image_url ?? null;
}

export function hasStudentQr(
  student: Pick<Student, "qr_link" | "qr_codes">,
) {
  return Boolean(resolveStudentQrLink(student) || resolveStudentQrImage(student));
}
