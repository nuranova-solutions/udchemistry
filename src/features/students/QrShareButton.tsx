import type { MouseEvent } from "react";
import type { Student } from "../../types/app";
import { resolveStudentQrImage, resolveStudentQrLink } from "./qrHelpers";

async function dataUrlToFile(dataUrl: string, filename: string) {
  const response = await fetch(dataUrl);
  const blob = await response.blob();
  return new File([blob], filename, { type: blob.type || "image/png" });
}

export function QrShareButton({
  student,
}: {
  student: Pick<Student, "full_name" | "al_year" | "whatsapp_number" | "qr_link" | "qr_codes">;
}) {
  const qrLink = resolveStudentQrLink(student);
  const qrImageUrl = resolveStudentQrImage(student);

  if (!qrLink) {
    return (
      <button className="button secondary small-button" type="button" disabled>
        Share QR
      </button>
    );
  }

  const messageText = `Hello ${student.full_name}, here is your UD Chemistry QR for ${student.al_year}: ${qrLink}`;
  const message = encodeURIComponent(messageText);
  const targetUrl = `https://wa.me/${student.whatsapp_number.replace(/\D/g, "")}?text=${message}`;

  async function handleShare(event: MouseEvent<HTMLAnchorElement>) {
    if (!qrImageUrl || typeof navigator.share !== "function") {
      return;
    }

    try {
      const qrFile = await dataUrlToFile(
        qrImageUrl,
        `${student.full_name.replace(/\s+/g, "-").toLowerCase()}-${student.al_year}-qr.png`,
      );

      if (!navigator.canShare?.({ files: [qrFile] })) {
        return;
      }

      event.preventDefault();
      await navigator.share({
        title: `${student.full_name} QR`,
        text: messageText,
        files: [qrFile],
      });
    } catch {
      // Fall back to the WhatsApp web link if the browser cancels or blocks file sharing.
    }
  }

  return (
    <a className="button secondary small-button" href={targetUrl} target="_blank" rel="noreferrer" onClick={handleShare}>
      Share QR
    </a>
  );
}
