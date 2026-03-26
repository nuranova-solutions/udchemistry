export function QrShareButton({
  qrLink,
  whatsappNumber,
  studentName,
}: {
  qrLink: string | null;
  whatsappNumber: string;
  studentName: string;
}) {
  if (!qrLink) {
    return (
      <button className="button secondary small-button" type="button" disabled>
        Share QR
      </button>
    );
  }

  const message = encodeURIComponent(
    `Hello ${studentName}, here is your Chemistry class QR link: ${qrLink}`,
  );
  const targetUrl = `https://wa.me/${whatsappNumber.replace(/\D/g, "")}?text=${message}`;

  return (
    <a className="button secondary small-button" href={targetUrl} target="_blank" rel="noreferrer">
      Share QR
    </a>
  );
}
