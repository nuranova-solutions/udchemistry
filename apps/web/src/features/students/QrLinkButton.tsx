import { useState } from "react";
import type { Student } from "../../types/app";
import { hasStudentQr, resolveStudentQrImage, resolveStudentQrLink } from "./qrHelpers";
import { StudentQrPreviewDialog } from "./StudentQrPreviewDialog";

export function QrLinkButton({
  student,
}: {
  student: Pick<Student, "full_name" | "qr_link" | "qr_codes">;
}) {
  const [showPreview, setShowPreview] = useState(false);
  const qrLink = resolveStudentQrLink(student);
  const qrImageUrl = resolveStudentQrImage(student);

  if (!hasStudentQr(student)) {
    return <span className="muted-text">Not generated</span>;
  }

  return (
    <>
      <button
        className="button ghost small-button"
        type="button"
        onClick={() => setShowPreview(true)}
      >
        Open QR
      </button>

      {showPreview ? (
        <StudentQrPreviewDialog
          studentName={student.full_name}
          qrLink={qrLink}
          qrImageUrl={qrImageUrl}
          onClose={() => setShowPreview(false)}
        />
      ) : null}
    </>
  );
}
