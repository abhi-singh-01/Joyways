export function AdminTableStatusRow({
  colSpan,
  message,
}: Readonly<{ colSpan: number; message: string }>) {
  return (
    <tr>
      <td colSpan={colSpan} className="px-4 py-8 text-center text-charcoal-muted">
        {message}
      </td>
    </tr>
  );
}
