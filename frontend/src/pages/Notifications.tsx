import { useEffect, useState } from 'react';
import { notificationsApi } from '../api/client';

interface Notification {
    id: number;
    type: string;
    title: string;
    message: string;
    createdAt: string;
    read: boolean;
}

interface NotificationPage {
    content: Notification[];
    totalPages: number;
    number: number;
    totalElements: number;
}

export default function Notifications() {
    const [pageData, setPageData] = useState<NotificationPage | null>(null);
    const [page, setPage] = useState(0);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        loadPage(page);
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [page]);

    const loadPage = async (pageIndex: number) => {
        try {
            setLoading(true);
            const res = await notificationsApi.list({ page: pageIndex, size: 10 });
            setPageData(res.data.data);
        } catch (err) {
            console.error('Failed to load notifications', err);
        } finally {
            setLoading(false);
        }
    };

    const handleMarkRead = async (id: number) => {
        try {
            await notificationsApi.markRead(id);
            setPageData((prev) =>
                prev
                    ? {
                          ...prev,
                          content: prev.content.map((n) =>
                              n.id === id ? { ...n, read: true } : n
                          ),
                      }
                    : prev
            );
        } catch (err) {
            console.error('Failed to mark notification as read', err);
        }
    };

    const notifications = pageData?.content ?? [];

    return (
        <div className="space-y-6">
            <div className="flex items-center justify-between">
                <div>
                    <h1 className="text-2xl lg:text-3xl font-bold">Notifications</h1>
                    <p className="text-text-muted text-sm mt-1">
                        Stay on top of budgets, subscriptions, and important reminders.
                    </p>
                </div>
            </div>

            <div className="card">
                {loading && (
                    <div className="flex items-center justify-center py-12">
                        <div className="w-8 h-8 border-3 border-primary/30 border-t-primary rounded-full animate-spin" />
                    </div>
                )}

                {!loading && notifications.length === 0 && (
                    <p className="text-sm text-text-muted text-center py-8">
                        No notifications yet. You&apos;ll see smart financial alerts here.
                    </p>
                )}

                {!loading && notifications.length > 0 && (
                    <div className="divide-y divide-primary/5">
                        {notifications.map((n) => (
                            <div
                                key={n.id}
                                className={`flex items-start justify-between gap-4 py-4 ${
                                    n.read ? 'opacity-80' : 'bg-primary/5'
                                } px-3 rounded-lg`}
                            >
                                <div>
                                    <p className="text-xs uppercase tracking-wide text-text-muted mb-1">
                                        {n.type.replace(/_/g, ' ')}
                                    </p>
                                    <p className="text-sm font-semibold">{n.title}</p>
                                    <p className="text-sm text-text-muted mt-1 whitespace-pre-line">
                                        {n.message}
                                    </p>
                                    <p className="text-[11px] text-text-muted mt-1">
                                        {new Date(n.createdAt).toLocaleString()}
                                    </p>
                                </div>
                                {!n.read && (
                                    <button
                                        onClick={() => handleMarkRead(n.id)}
                                        className="text-xs text-primary hover:text-primary-light font-medium"
                                    >
                                        Mark as read
                                    </button>
                                )}
                            </div>
                        ))}
                    </div>
                )}

                {pageData && pageData.totalPages > 1 && (
                    <div className="flex items-center justify-between mt-4 pt-3 border-t border-primary/10 text-xs">
                        <span className="text-text-muted">
                            Page {pageData.number + 1} of {pageData.totalPages}
                        </span>
                        <div className="space-x-2">
                            <button
                                disabled={page === 0}
                                onClick={() => setPage((p) => Math.max(0, p - 1))}
                                className="btn-secondary disabled:opacity-40 disabled:cursor-not-allowed px-3 py-1 text-xs"
                            >
                                Previous
                            </button>
                            <button
                                disabled={page >= pageData.totalPages - 1}
                                onClick={() =>
                                    setPage((p) =>
                                        pageData ? Math.min(pageData.totalPages - 1, p + 1) : p
                                    )
                                }
                                className="btn-secondary disabled:opacity-40 disabled:cursor-not-allowed px-3 py-1 text-xs"
                            >
                                Next
                            </button>
                        </div>
                    </div>
                )}
            </div>
        </div>
    );
}

