import { useState, useEffect } from 'react';
import { subscriptionApi } from '../api/client';

interface Sub {
    id: number;
    merchant: string;
    amount: number;
    category: string;
    interval: string;
    nextBillingDate: string | null;
    lastBilledDate: string | null;
    autoGenerateTransaction: boolean;
    active: boolean;
}

interface Summary {
    totalMonthlyCommitment: number;
    totalYearlyCommitment: number;
    activeSubscriptions: number;
    upcomingIn7Days: Sub[];
}

interface WasteAnalysis {
    warnings: string[];
}

const EMPTY_FORM = {
    merchant: '', amount: '', category: '', interval: 'MONTHLY',
    nextBillingDate: '', autoGenerateTransaction: false, active: true,
};

const CATEGORIES = [
    'Entertainment', 'Food', 'Utilities', 'Healthcare', 'Shopping',
    'Education', 'Transportation', 'Subscriptions', 'Others',
];

export default function Subscriptions() {
    const [subs, setSubs] = useState<Sub[]>([]);
    const [summary, setSummary] = useState<Summary | null>(null);
    const [waste, setWaste] = useState<WasteAnalysis | null>(null);
    const [showModal, setShowModal] = useState(false);
    const [editId, setEditId] = useState<number | null>(null);
    const [form, setForm] = useState(EMPTY_FORM);
    const [loading, setLoading] = useState(true);

    useEffect(() => { fetchAll(); }, []);

    const fetchAll = async () => {
        setLoading(true);
        try {
            const [subsRes, summaryRes, wasteRes] = await Promise.all([
                subscriptionApi.list(),
                subscriptionApi.summary(),
                subscriptionApi.wasteAnalysis(),
            ]);
            setSubs(subsRes.data.data);
            setSummary(summaryRes.data.data);
            setWaste(wasteRes.data.data);
        } catch { /* ignore */ }
        setLoading(false);
    };

    const openAdd = () => {
        setEditId(null);
        setForm(EMPTY_FORM);
        setShowModal(true);
    };

    const openEdit = (sub: Sub) => {
        setEditId(sub.id);
        setForm({
            merchant: sub.merchant,
            amount: String(sub.amount),
            category: sub.category || '',
            interval: sub.interval,
            nextBillingDate: sub.nextBillingDate || '',
            autoGenerateTransaction: sub.autoGenerateTransaction,
            active: sub.active,
        });
        setShowModal(true);
    };

    const handleSave = async () => {
        const payload = {
            merchant: form.merchant,
            amount: parseFloat(form.amount),
            category: form.category,
            interval: form.interval,
            nextBillingDate: form.nextBillingDate || null,
            autoGenerateTransaction: form.autoGenerateTransaction,
            active: form.active,
        };
        try {
            if (editId) {
                await subscriptionApi.update(editId, payload);
            } else {
                await subscriptionApi.create(payload);
            }
            setShowModal(false);
            fetchAll();
        } catch (err: unknown) {
            const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message || 'Failed';
            alert(msg);
        }
    };

    const handleDelete = async (id: number) => {
        if (!confirm('Delete this subscription?')) return;
        await subscriptionApi.delete(id);
        fetchAll();
    };

    const toggleActive = async (sub: Sub) => {
        await subscriptionApi.update(sub.id, {
            merchant: sub.merchant, amount: sub.amount, category: sub.category,
            interval: sub.interval, nextBillingDate: sub.nextBillingDate,
            autoGenerateTransaction: sub.autoGenerateTransaction,
            active: !sub.active,
        });
        fetchAll();
    };

    if (loading) return <div style={{ color: '#94a3b8' }}>Loading...</div>;

    return (
        <div>
            <div className="flex justify-between items-center mb-6">
                <h1 className="text-2xl font-bold" style={{ color: '#e2e8f0' }}>🔄 Subscriptions</h1>
                <button onClick={openAdd} className="btn-primary">+ Add Subscription</button>
            </div>

            {/* Summary cards */}
            {summary && (
                <div className="grid grid-cols-1 md:grid-cols-4 gap-4 mb-6">
                    <div className="card text-center">
                        <div className="text-xs mb-1" style={{ color: '#94a3b8' }}>Monthly</div>
                        <div className="text-xl font-bold" style={{ color: '#6366f1' }}>₹{summary.totalMonthlyCommitment.toLocaleString()}</div>
                    </div>
                    <div className="card text-center">
                        <div className="text-xs mb-1" style={{ color: '#94a3b8' }}>Yearly</div>
                        <div className="text-xl font-bold" style={{ color: '#0ea5e9' }}>₹{summary.totalYearlyCommitment.toLocaleString()}</div>
                    </div>
                    <div className="card text-center">
                        <div className="text-xs mb-1" style={{ color: '#94a3b8' }}>Active</div>
                        <div className="text-xl font-bold" style={{ color: '#22c55e' }}>{summary.activeSubscriptions}</div>
                    </div>
                    <div className="card text-center">
                        <div className="text-xs mb-1" style={{ color: '#94a3b8' }}>Due in 7 Days</div>
                        <div className="text-xl font-bold" style={{ color: '#f59e0b' }}>{summary.upcomingIn7Days.length}</div>
                    </div>
                </div>
            )}

            {/* Waste Analysis */}
            {waste && waste.warnings.length > 0 && (
                <div className="card mb-6" style={{ border: '1px solid rgba(239,68,68,0.3)', background: 'rgba(239,68,68,0.05)' }}>
                    <h3 className="font-semibold mb-3" style={{ color: '#ef4444' }}>⚠️ Waste Analysis</h3>
                    <ul className="space-y-1">
                        {waste.warnings.map((w, i) => (
                            <li key={i} className="text-sm" style={{ color: '#fca5a5' }}>• {w}</li>
                        ))}
                    </ul>
                </div>
            )}

            {/* Subscriptions list */}
            <div className="card overflow-x-auto">
                <table className="w-full text-sm">
                    <thead>
                        <tr style={{ color: '#94a3b8', borderBottom: '1px solid rgba(99,102,241,0.2)' }}>
                            <th className="text-left py-2">Merchant</th>
                            <th className="text-right py-2">Amount</th>
                            <th className="text-left py-2">Category</th>
                            <th className="text-left py-2">Interval</th>
                            <th className="text-left py-2">Next Billing</th>
                            <th className="text-center py-2">Auto-Bill</th>
                            <th className="text-center py-2">Status</th>
                            <th className="text-right py-2">Actions</th>
                        </tr>
                    </thead>
                    <tbody>
                        {subs.map(sub => (
                            <tr key={sub.id} style={{
                                borderBottom: '1px solid rgba(99,102,241,0.1)',
                                opacity: sub.active ? 1 : 0.5,
                            }}>
                                <td className="py-3 font-medium" style={{ color: '#e2e8f0' }}>{sub.merchant}</td>
                                <td className="text-right py-3 font-semibold" style={{ color: '#818cf8' }}>
                                    ₹{sub.amount.toLocaleString()}
                                </td>
                                <td className="py-3" style={{ color: '#94a3b8' }}>{sub.category || '-'}</td>
                                <td className="py-3" style={{ color: '#94a3b8' }}>{sub.interval}</td>
                                <td className="py-3" style={{ color: '#94a3b8' }}>
                                    {sub.nextBillingDate || '-'}
                                </td>
                                <td className="text-center py-3">
                                    {sub.autoGenerateTransaction
                                        ? <span className="badge" style={{ background: 'rgba(34,197,94,0.15)', color: '#22c55e' }}>ON</span>
                                        : <span className="badge" style={{ background: 'rgba(148,163,184,0.15)', color: '#94a3b8' }}>OFF</span>
                                    }
                                </td>
                                <td className="text-center py-3">
                                    <button onClick={() => toggleActive(sub)} className="badge cursor-pointer"
                                        style={{
                                            background: sub.active ? 'rgba(34,197,94,0.15)' : 'rgba(239,68,68,0.15)',
                                            color: sub.active ? '#22c55e' : '#ef4444',
                                        }}>
                                        {sub.active ? 'Active' : 'Inactive'}
                                    </button>
                                </td>
                                <td className="text-right py-3">
                                    <button onClick={() => openEdit(sub)} className="mr-2" style={{ color: '#818cf8' }}>Edit</button>
                                    <button onClick={() => handleDelete(sub.id)} style={{ color: '#ef4444' }}>Delete</button>
                                </td>
                            </tr>
                        ))}
                        {subs.length === 0 && (
                            <tr>
                                <td colSpan={8} className="text-center py-8" style={{ color: '#94a3b8' }}>
                                    No subscriptions yet. Click "+ Add Subscription" to get started.
                                </td>
                            </tr>
                        )}
                    </tbody>
                </table>
            </div>

            {/* Modal */}
            {showModal && (
                <div className="modal-overlay" onClick={() => setShowModal(false)}>
                    <div className="modal-content" onClick={e => e.stopPropagation()}>
                        <h2 className="text-xl font-bold mb-4" style={{ color: '#e2e8f0' }}>
                            {editId ? 'Edit Subscription' : 'Add Subscription'}
                        </h2>

                        <div className="space-y-4">
                            <div>
                                <label className="block text-sm mb-1" style={{ color: '#94a3b8' }}>Merchant</label>
                                <input className="input-field" value={form.merchant}
                                    onChange={e => setForm({ ...form, merchant: e.target.value })} placeholder="e.g. Netflix" />
                            </div>
                            <div>
                                <label className="block text-sm mb-1" style={{ color: '#94a3b8' }}>Amount</label>
                                <input className="input-field" type="number" value={form.amount}
                                    onChange={e => setForm({ ...form, amount: e.target.value })} placeholder="699" />
                            </div>
                            <div>
                                <label className="block text-sm mb-1" style={{ color: '#94a3b8' }}>Category</label>
                                <select className="input-field" value={form.category}
                                    onChange={e => setForm({ ...form, category: e.target.value })}>
                                    <option value="">Select...</option>
                                    {CATEGORIES.map(c => <option key={c} value={c}>{c}</option>)}
                                </select>
                            </div>
                            <div>
                                <label className="block text-sm mb-1" style={{ color: '#94a3b8' }}>Interval</label>
                                <select className="input-field" value={form.interval}
                                    onChange={e => setForm({ ...form, interval: e.target.value })}>
                                    <option value="WEEKLY">Weekly</option>
                                    <option value="MONTHLY">Monthly</option>
                                    <option value="YEARLY">Yearly</option>
                                </select>
                            </div>
                            <div>
                                <label className="block text-sm mb-1" style={{ color: '#94a3b8' }}>Next Billing Date</label>
                                <input className="input-field" type="date" value={form.nextBillingDate}
                                    onChange={e => setForm({ ...form, nextBillingDate: e.target.value })} />
                            </div>
                            <div className="flex items-center gap-3">
                                <input type="checkbox" id="autoBill" checked={form.autoGenerateTransaction}
                                    onChange={e => setForm({ ...form, autoGenerateTransaction: e.target.checked })} />
                                <label htmlFor="autoBill" className="text-sm" style={{ color: '#94a3b8' }}>
                                    Auto-generate transaction on billing date
                                </label>
                            </div>
                        </div>

                        <div className="flex gap-3 mt-6">
                            <button onClick={handleSave} className="btn-primary flex-1">
                                {editId ? 'Update' : 'Create'}
                            </button>
                            <button onClick={() => setShowModal(false)} className="btn-secondary flex-1">
                                Cancel
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}
