import { useQuery } from "@tanstack/react-query";
import { useParams } from "react-router-dom";
import { fetchPublicQr } from "../../lib/api";

export function PublicQrViewerPage() {
  const { shareToken } = useParams();
  const qrQuery = useQuery({
    queryKey: ["public-qr", shareToken],
    queryFn: () => fetchPublicQr(shareToken!),
    enabled: Boolean(shareToken),
  });

  return (
    <div className="fullscreen-state">
      <div className="state-card qr-public-card">
        <p className="eyebrow">Student QR</p>
        <h2>Open, view, and download the QR code.</h2>

        {qrQuery.data?.qr_image_url ? (
          <img className="qr-image" src={qrQuery.data.qr_image_url} alt="Student QR" />
        ) : (
          <div className="scan-preview">QR image unavailable</div>
        )}

        {qrQuery.data?.qr_image_url ? (
          <a className="button" href={qrQuery.data.qr_image_url} download>
            Download QR
          </a>
        ) : null}
      </div>
    </div>
  );
}
