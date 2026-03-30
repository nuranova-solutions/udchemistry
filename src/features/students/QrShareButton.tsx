import type { Student } from "../../types/app";
import { resolveStudentQrLink } from "./qrHelpers";

export function QrShareButton({
  student,
}: {
  student: Pick<Student, "full_name" | "whatsapp_number" | "qr_link" | "qr_codes">;
}) {
  const qrLink = resolveStudentQrLink(student);

  if (!qrLink) {
    return (
      <button className="button secondary small-button" type="button" disabled>
        Share QR
      </button>
    );
  }

  const message = encodeURIComponent(
    `Hello ${student.full_name}, here is your Chemistry class QR link: ${qrLink}`,
  );
  const targetUrl = `https://wa.me/${student.whatsapp_number.replace(/\D/g, "")}?text=${message}`;

  return (
    <a className="button secondary small-button" href={targetUrl} target="_blank" rel="noreferrer">
      Share QR
    </a>
  );
}
