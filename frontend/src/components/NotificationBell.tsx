import { useEffect, useState } from 'react';
import { BellIcon, BellAlertIcon } from '@heroicons/react/24/outline';
import { notificationsApi } from '../api/client';

interface Notification {
    id: number;
    type: string;
    title: string;
    message: string;
    createdAt: string;
    read: boolean;
}

export default function NotificationBell() {
    const [open, setOpen] = useState(false);
    const [loading, setLoading] = useState(false);
    const [unreadCount, setUnreadCount] = useState(0);
    const [notifications, setNotifications] = useState<Notification[]>([]);

    useEffect(() => {
        loadNotifications();
        const interval = setInterval(loadNotifications, 60000);
        return () => clearInterval(interval);
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    const loadNotifications = async () => {
        try {
            setLoading(true);
            const [countRes, listRes] = await Promise.all([
                notificationsApi.unreadCount(),
                notificationsApi.list({ size: 5 }),
            ]);
            setUnreadCount(countRes.data.data ?? 0);
            setNotifications(listRes.data.data?.content ?? []);
        } catch (err) {
            console.error('Failed to load notifications', err);
        } finally {
            setLoading(false);
        }
    };

    const handleToggle = () => {
        setOpen((prev) => !prev);
    };

    const handleMarkRead = async (id: number) => {
        try {
            await notificationsApi.markRead(id);
            setNotifications((prev) =>
                prev.map((n) => (n.id === id ? { ...n, read: true } : n))
            );
            setUnreadCount((prev) => (prev > 0 ? prev - 1 : 0));
        } catch (err) {
            console.error('Failed to mark notification as read', err);
        }
    };

    return (
        <div className="relative">
            <button
                type="button"
                onClick={handleToggle}
                className="relative p-2 rounded-full hover:bg-primary/10 text-text-muted hover:text-white transition"
            >
                {unreadCount > 0 ? (
                    <BellAlertIcon className="w-6 h-6" />
                ) : (
                    <BellIcon className="w-6 h-6" />
                )}
                {unreadCount > 0 && (
                    <span className="absolute -top-1 -right-1 min-w-[18px] h-4 px-1 rounded-full bg-danger text-[10px] font-bold text-white flex items-center justify-center">
                        {unreadCount > 9 ? '9+' : unreadCount}
                    </span>
                )}
            </button>

            {open && (
                <div className="absolute right-0 mt-3 w-80 bg-bg-card border border-primary/20 rounded-xl shadow-xl z-40">
                    <div className="px-4 py-3 border-b border-primary/10 flex items-center justify-between">
                        <p className="text-sm font-semibold">Notifications</p>
                        {loading && (
                            <span className="text-[11px] text-text-muted">Refreshing...</span>
                        )}
                    </div>
                    <div className="max-h-80 overflow-y-auto">
                        {notifications.length === 0 && (
                            <p className="text-xs text-text-muted text-center py-4">
                                You&apos;re all caught up!
                            </p>
                        )}
                        {notifications.map((n) => (
                            <div
                                key={n.id}
                                className={`px-4 py-3 border-b border-primary/5 last:border-0 ${
                                    n.read ? 'bg-transparent' : 'bg-primary/5'
                                }`}
                            >
                                <div className="flex items-start justify-between gap-3">
                                    <div>
                                        <p className="text-xs uppercase text-text-muted mb-1">
                                            {n.type.replace(/_/g, ' ')}
                                        </p>
                                        <p className="text-sm font-semibold">{n.title}</p>
                                        <p className="text-xs text-text-muted mt-1 line-clamp-2">
                                            {n.message}
                                        </p>
                                        <p className="text-[11px] text-text-muted mt-1">
                                            {new Date(n.createdAt).toLocaleString()}
                                        </p>
                                    </div>
                                    {!n.read && (
                                        <button
                                            onClick={() => handleMarkRead(n.id)}
                                            className="text-[11px] text-primary hover:text-primary-light font-medium"
                                        >
                                            Mark read
                                        </button>
                                    )}
                                </div>
                            </div>
                        ))}
                    </div>
                    <div className="px-4 py-2 border-t border-primary/10 text-xs text-right">
                        <a
                            href="/notifications"
                            className="text-primary hover:text-primary-light font-medium"
                        >
                            View all notifications
                        </a>
                    </div>
                </div>
            )}
        </div>
    );
}

