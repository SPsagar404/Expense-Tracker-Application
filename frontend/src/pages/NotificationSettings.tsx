import { useEffect, useState } from 'react';
import { notificationsApi, type NotificationPreferences } from '../api/client';

export default function NotificationSettings() {
    const [prefs, setPrefs] = useState<NotificationPreferences | null>(null);
    const [loading, setLoading] = useState(true);
    const [saving, setSaving] = useState(false);

    useEffect(() => {
        const load = async () => {
            try {
                const res = await notificationsApi.getPreferences();
                setPrefs(res.data.data);
            } catch (err) {
                console.error('Failed to load notification preferences', err);
            } finally {
                setLoading(false);
            }
        };
        load();
    }, []);

    const handleToggle = (key: keyof NotificationPreferences) => {
        if (!prefs) return;
        setPrefs({ ...prefs, [key]: !prefs[key] });
    };

    const handleSave = async () => {
        if (!prefs) return;
        try {
            setSaving(true);
            const res = await notificationsApi.updatePreferences(prefs);
            setPrefs(res.data.data);
        } catch (err) {
            console.error('Failed to save notification preferences', err);
        } finally {
            setSaving(false);
        }
    };

    return (
        <div className="space-y-6">
            <div>
                <h1 className="text-2xl lg:text-3xl font-bold">Notification Settings</h1>
                <p className="text-text-muted text-sm mt-1">
                    Choose how you want to be notified about budgets, subscriptions, and goals.
                </p>
            </div>

            <div className="card max-w-2xl">
                {loading && (
                    <div className="flex items-center justify-center py-12">
                        <div className="w-8 h-8 border-3 border-primary/30 border-t-primary rounded-full animate-spin" />
                    </div>
                )}

                {!loading && prefs && (
                    <div className="space-y-6">
                        <div>
                            <h2 className="text-sm font-semibold mb-2">Channels</h2>
                            <p className="text-xs text-text-muted mb-3">
                                Control where we send your alerts.
                            </p>
                            <div className="space-y-3">
                                <ToggleRow
                                    label="Email alerts"
                                    description="Receive important alerts in your inbox."
                                    checked={prefs.emailEnabled}
                                    onToggle={() => handleToggle('emailEnabled')}
                                />
                                <ToggleRow
                                    label="In-app notifications"
                                    description="Show alerts inside the app (recommended)."
                                    checked={prefs.inAppEnabled}
                                    onToggle={() => handleToggle('inAppEnabled')}
                                />
                                <ToggleRow
                                    label="Push notifications"
                                    description="Send mobile or web push notifications (future-ready)."
                                    checked={prefs.pushEnabled}
                                    onToggle={() => handleToggle('pushEnabled')}
                                />
                            </div>
                        </div>

                        <div className="pt-4 border-t border-primary/10">
                            <h2 className="text-sm font-semibold mb-2">Alert types</h2>
                            <p className="text-xs text-text-muted mb-3">
                                Choose which smart alerts you care about.
                            </p>
                            <div className="space-y-3">
                                <ToggleRow
                                    label="Budget alerts"
                                    description="Notify when category spending exceeds your budget."
                                    checked={prefs.budgetAlertEnabled}
                                    onToggle={() => handleToggle('budgetAlertEnabled')}
                                />
                                <ToggleRow
                                    label="Subscription alerts"
                                    description="Reminders before upcoming subscription charges."
                                    checked={prefs.subscriptionAlertEnabled}
                                    onToggle={() => handleToggle('subscriptionAlertEnabled')}
                                />
                                <ToggleRow
                                    label="Goal & salary alerts"
                                    description="Alerts when planned goals or salary allocations are overspent."
                                    checked={prefs.goalAlertEnabled}
                                    onToggle={() => handleToggle('goalAlertEnabled')}
                                />
                                <ToggleRow
                                    label="Large expense alerts"
                                    description="Flag unusually large or risky transactions."
                                    checked={prefs.largeExpenseAlertEnabled}
                                    onToggle={() => handleToggle('largeExpenseAlertEnabled')}
                                />
                            </div>
                        </div>

                        <div className="flex justify-end pt-4 border-t border-primary/10">
                            <button
                                onClick={handleSave}
                                disabled={saving}
                                className="btn-primary px-5 py-2 text-sm disabled:opacity-60 disabled:cursor-not-allowed"
                            >
                                {saving ? 'Saving...' : 'Save preferences'}
                            </button>
                        </div>
                    </div>
                )}
            </div>
        </div>
    );
}

interface ToggleRowProps {
    label: string;
    description: string;
    checked: boolean;
    onToggle: () => void;
}

function ToggleRow({ label, description, checked, onToggle }: ToggleRowProps) {
    return (
        <div className="flex items-center justify-between gap-4">
            <div>
                <p className="text-sm font-medium">{label}</p>
                <p className="text-xs text-text-muted">{description}</p>
            </div>
            <button
                type="button"
                onClick={onToggle}
                className={`relative inline-flex h-6 w-11 items-center rounded-full transition ${
                    checked ? 'bg-primary' : 'bg-primary/30'
                }`}
            >
                <span
                    className={`inline-block h-4 w-4 transform rounded-full bg-white transition ${
                        checked ? 'translate-x-5' : 'translate-x-1'
                    }`}
                />
            </button>
        </div>
    );
}

