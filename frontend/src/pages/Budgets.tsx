import { useEffect, useState } from 'react';
import { budgetApi } from '../api/client';
import { PlusIcon, TrashIcon, ExclamationTriangleIcon } from '@heroicons/react/24/outline';

interface Budget {
    id: number;
    category: string;
    month: number;
    year: number;
    limitAmount: number;
    spent: number;
    remaining: number;
    utilizationPercentage: number;
}

const categories = ['Food & Groceries', 'Transportation', 'Shopping', 'Entertainment', 'Utilities', 'Healthcare', 'Housing', 'Other'];

export default function Budgets() {
    const [budgets, setBudgets] = useState<Budget[]>([]);
    const [loading, setLoading] = useState(true);
    const [showModal, setShowModal] = useState(false);
    const [form, setForm] = useState({
        category: 'Food & Groceries', month: new Date().getMonth() + 1,
        year: new Date().getFullYear(), limitAmount: '',
    });

    const now = new Date();

    useEffect(() => {
        loadBudgets();
    }, []);

    const loadBudgets = async () => {
        try {
            const res = await budgetApi.list({ month: now.getMonth() + 1, year: now.getFullYear() });
            setBudgets(res.data.data || []);
        } catch (err) {
            console.error('Failed to load budgets', err);
        } finally {
            setLoading(false);
        }
    };

    const handleCreate = async () => {
        try {
            await budgetApi.create({
                category: form.category,
                month: form.month,
                year: form.year,
                limitAmount: parseFloat(form.limitAmount),
            });
            setShowModal(false);
            setForm({ category: 'Food & Groceries', month: now.getMonth() + 1, year: now.getFullYear(), limitAmount: '' });
            loadBudgets();
        } catch (err: any) {
            alert(err.response?.data?.message || 'Failed to create budget');
        }
    };

    const handleDelete = async (id: number) => {
        if (!confirm('Delete this budget?')) return;
        try {
            await budgetApi.delete(id);
            loadBudgets();
        } catch (err) {
            console.error('Delete failed', err);
        }
    };

    const getProgressColor = (pct: number) => {
        if (pct >= 100) return 'bg-danger';
        if (pct >= 80) return 'bg-accent';
        return 'bg-success';
    };

    if (loading) {
        return (
            <div className="flex items-center justify-center h-96">
                <div className="w-10 h-10 border-3 border-primary/30 border-t-primary rounded-full animate-spin" />
            </div>
        );
    }

    return (
        <div className="space-y-6">
            <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
                <div>
                    <h1 className="text-2xl lg:text-3xl font-bold">Budgets</h1>
                    <p className="text-text-muted mt-1">
                        {now.toLocaleString('default', { month: 'long', year: 'numeric' })} Budgets
                    </p>
                </div>
                <button onClick={() => setShowModal(true)} className="btn-primary flex items-center gap-2">
                    <PlusIcon className="w-5 h-5" />
                    New Budget
                </button>
            </div>

            {/* Budget Cards */}
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                {budgets.map((budget) => (
                    <div key={budget.id} className="card-hover relative group">
                        <button
                            onClick={() => handleDelete(budget.id)}
                            className="absolute top-4 right-4 p-2 opacity-0 group-hover:opacity-100 hover:bg-danger/10 rounded-lg transition-all"
                        >
                            <TrashIcon className="w-4 h-4 text-danger" />
                        </button>

                        <div className="flex items-center gap-3 mb-4">
                            <div className="w-10 h-10 bg-primary/10 rounded-xl flex items-center justify-center">
                                <span className="text-primary font-bold text-sm">
                                    {budget.category.charAt(0)}
                                </span>
                            </div>
                            <div>
                                <h3 className="font-semibold">{budget.category}</h3>
                                <p className="text-xs text-text-muted">
                                    {budget.month}/{budget.year}
                                </p>
                            </div>
                        </div>

                        {/* Progress */}
                        <div className="progress-bar mb-3">
                            <div
                                className={`progress-fill ${getProgressColor(budget.utilizationPercentage)}`}
                                style={{ width: `${Math.min(budget.utilizationPercentage, 100)}%` }}
                            />
                        </div>

                        <div className="flex justify-between text-sm">
                            <span className="text-text-muted">
                                ${budget.spent?.toFixed(2)} spent
                            </span>
                            <span className="font-medium">
                                ${budget.limitAmount?.toFixed(2)} limit
                            </span>
                        </div>

                        <div className="flex items-center justify-between mt-3">
                            <span className={`text-sm font-semibold ${budget.utilizationPercentage >= 100 ? 'text-danger' :
                                    budget.utilizationPercentage >= 80 ? 'text-accent' : 'text-success'
                                }`}>
                                {budget.utilizationPercentage?.toFixed(0)}% used
                            </span>
                            {budget.utilizationPercentage >= 100 && (
                                <div className="flex items-center gap-1 text-danger text-xs">
                                    <ExclamationTriangleIcon className="w-4 h-4" />
                                    Over budget!
                                </div>
                            )}
                            {budget.utilizationPercentage >= 80 && budget.utilizationPercentage < 100 && (
                                <div className="flex items-center gap-1 text-accent text-xs">
                                    <ExclamationTriangleIcon className="w-4 h-4" />
                                    Nearing limit
                                </div>
                            )}
                        </div>

                        <div className="mt-3 pt-3 border-t border-primary/10 text-sm text-text-muted">
                            Remaining: <span className={`font-medium ${budget.remaining < 0 ? 'text-danger' : 'text-success'}`}>
                                ${budget.remaining?.toFixed(2)}
                            </span>
                        </div>
                    </div>
                ))}

                {budgets.length === 0 && (
                    <div className="col-span-full text-center py-12 text-text-muted">
                        <p className="text-lg mb-2">No budgets yet</p>
                        <p className="text-sm">Create your first budget to start tracking spending limits.</p>
                    </div>
                )}
            </div>

            {/* Create Modal */}
            {showModal && (
                <div className="modal-overlay" onClick={() => setShowModal(false)}>
                    <div className="modal-content" onClick={(e) => e.stopPropagation()}>
                        <h2 className="text-xl font-bold mb-5">Create Budget</h2>
                        <div className="space-y-4">
                            <div>
                                <label className="block text-sm font-medium text-text-muted mb-1">Category</label>
                                <select
                                    value={form.category}
                                    onChange={(e) => setForm({ ...form, category: e.target.value })}
                                    className="input-field"
                                >
                                    {categories.map((c) => <option key={c} value={c}>{c}</option>)}
                                </select>
                            </div>
                            <div className="grid grid-cols-2 gap-4">
                                <div>
                                    <label className="block text-sm font-medium text-text-muted mb-1">Month</label>
                                    <select
                                        value={form.month}
                                        onChange={(e) => setForm({ ...form, month: parseInt(e.target.value) })}
                                        className="input-field"
                                    >
                                        {Array.from({ length: 12 }, (_, i) => (
                                            <option key={i + 1} value={i + 1}>
                                                {new Date(2000, i, 1).toLocaleString('default', { month: 'long' })}
                                            </option>
                                        ))}
                                    </select>
                                </div>
                                <div>
                                    <label className="block text-sm font-medium text-text-muted mb-1">Year</label>
                                    <input
                                        type="number"
                                        value={form.year}
                                        onChange={(e) => setForm({ ...form, year: parseInt(e.target.value) })}
                                        className="input-field"
                                    />
                                </div>
                            </div>
                            <div>
                                <label className="block text-sm font-medium text-text-muted mb-1">Budget Limit ($)</label>
                                <input
                                    type="number"
                                    step="0.01"
                                    value={form.limitAmount}
                                    onChange={(e) => setForm({ ...form, limitAmount: e.target.value })}
                                    className="input-field"
                                    placeholder="500.00"
                                />
                            </div>
                            <div className="flex gap-3 pt-2">
                                <button onClick={() => setShowModal(false)} className="btn-secondary flex-1">Cancel</button>
                                <button onClick={handleCreate} className="btn-primary flex-1">Create Budget</button>
                            </div>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}
