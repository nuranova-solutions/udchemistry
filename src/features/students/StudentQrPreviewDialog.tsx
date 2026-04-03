import { useEffect } from "react";
import { X } from "lucide-react";
import { createPortal } from "react-dom";

interface StudentQrPreviewDialogProps {
  studentName: string;
  qrLink: string | null;
  qrImageUrl: string | null;
  onClose: () => void;
}

export function StudentQrPreviewDialog({
  studentName,
  qrLink,
  qrImageUrl,
  onClose,
}: StudentQrPreviewDialogProps) {
  useEffect(() => {
    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === "Escape") {
        onClose();
      }
    }

    document.body.style.overflow = "hidden";
    window.addEventListener("keydown", handleKeyDown);

    return () => {
      document.body.style.overflow = "";
      window.removeEventListener("keydown", handleKeyDown);
    };
  }, [onClose]);

  if (typeof document === "undefined") {
    return null;
  }

  return createPortal(
    <div className="qr-preview-overlay" onClick={onClose}>
      <div
        className="qr-preview-card"
        role="dialog"
        aria-modal="true"
        aria-label={`${studentName} QR preview`}
        onClick={(event) => event.stopPropagation()}
      >
        <div className="qr-preview-header">
          <div>
            <p className="eyebrow">Student QR</p>
            <h3>{studentName}</h3>
            <p className="muted-text">
              Open the QR page, preview the code image, or download it directly.
            </p>
          </div>

          <button className="button ghost small-button icon-button" type="button" onClick={onClose}>
            <X size={18} />
            Close
          </button>
        </div>

        {qrImageUrl ? (
          <img className="qr-image" src={qrImageUrl} alt={`${studentName} QR`} />
        ) : (
          <div className="scan-preview">QR image unavailable</div>
        )}

        <div className="qr-preview-actions">
          {qrLink ? (
            <a className="button" href={qrLink} target="_blank" rel="noreferrer">
              Open page
            </a>
          ) : (
            <button className="button" type="button" disabled>
              Open page
            </button>
          )}

          {qrImageUrl ? (
            <a className="button ghost" href={qrImageUrl} download>
              Download QR
            </a>
          ) : (
            <button className="button ghost" type="button" disabled>
              Download QR
            </button>
          )}
        </div>
      </div>
    </div>,
    document.body,
  );
}
