export function QrLinkButton({ qrLink }: { qrLink: string | null }) {
  if (!qrLink) {
    return <span className="muted-text">Not generated</span>;
  }

  return (
    <a className="button ghost small-button" href={qrLink} target="_blank" rel="noreferrer">
      Open QR
    </a>
  );
}
