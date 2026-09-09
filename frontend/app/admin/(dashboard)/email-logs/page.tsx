"use client";

import { useEffect, useState } from "react";
import { listAdminEmailLogs, type AdminEmailLog } from "@/lib/api/adminOpsLogsApi";
import { ErrorState } from "@/components/ui/ErrorState";
import { formatDate } from "@/lib/format";

export default function AdminEmailLogsPage() {
  const [logs, setLogs] = useState<AdminEmailLog[]>([]);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);

  useEffect(() => {
    let cancelled = false;
    listAdminEmailLogs(page)
      .then((result) => {
        if (cancelled) return;
        setLogs(result.content);
        setTotalPages(result.totalPages);
        setError("");
      })
      .catch((err) => {
        if (cancelled) return;
        setError(err instanceof Error ? err.message : "Failed to load email logs");
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [page]);

  if (loading) return <p className="text-charcoal-muted">Loading email logs...</p>;
  if (error) return <ErrorState title="Unable to load email logs" message={error} />;

  return (
    <div>
      <h1 className="font-display text-3xl font-semibold text-charcoal">Email log</h1>
      <p className="mt-1 text-charcoal-muted">Outbound mail status, retries, and failures</p>

      <div className="mt-8 overflow-x-auto rounded-2xl border border-border bg-white">
        <table className="min-w-full text-left text-sm">
          <thead className="border-b border-border bg-surface text-charcoal-muted">
            <tr>
              <th className="px-4 py-3 font-medium">When</th>
              <th className="px-4 py-3 font-medium">Recipient</th>
              <th className="px-4 py-3 font-medium">Template</th>
              <th className="px-4 py-3 font-medium">Status</th>
              <th className="px-4 py-3 font-medium">Attempts</th>
            </tr>
          </thead>
          <tbody>
            {logs.length === 0 ? (
              <tr>
                <td colSpan={5} className="px-4 py-8 text-center text-charcoal-muted">
                  No email logs yet
                </td>
              </tr>
            ) : (
              logs.map((log) => (
                <tr key={log.id} className="border-b border-border last:border-0">
                  <td className="px-4 py-3 whitespace-nowrap">{formatDate(log.createdAt)}</td>
                  <td className="px-4 py-3">{log.recipient}</td>
                  <td className="px-4 py-3">{log.template}</td>
                  <td className="px-4 py-3">{log.status}</td>
                  <td className="px-4 py-3">
                    {log.attemptCount}
                    {log.failureReason && (
                      <p className="text-xs text-red-700">{log.failureReason}</p>
                    )}
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {totalPages > 1 && (
        <div className="mt-4 flex items-center justify-end gap-2">
          <button
            type="button"
            disabled={page === 0}
            onClick={() => {
              setLoading(true);
              setPage((p) => p - 1);
            }}
            className="rounded-lg border border-border px-3 py-1.5 text-sm disabled:opacity-50"
          >
            Previous
          </button>
          <span className="text-sm text-charcoal-muted">
            Page {page + 1} of {totalPages}
          </span>
          <button
            type="button"
            disabled={page + 1 >= totalPages}
            onClick={() => {
              setLoading(true);
              setPage((p) => p + 1);
            }}
            className="rounded-lg border border-border px-3 py-1.5 text-sm disabled:opacity-50"
          >
            Next
          </button>
        </div>
      )}
    </div>
  );
}
