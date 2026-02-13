import { useState, useEffect } from 'react';
import { salaryApi } from '../api/client';

interface Allocation {
    category: string;
    allocationType: 'PERCENTAGE' | 'FIXED';
    allocationValue: string;
    allocatedAmount: number;
}

interface SummaryAllocation {
    id: number;
    category: string;
    allocationType: string;
    allocationValue: number;
    allocatedAmount: number;
    actualSpent: number;
    variance: number;
}

interface SalarySummary {
    month: number;
    year: number;
    totalSalary: number;
    totalPlanned: number;
    totalActualSpent: number;
    totalSavings: number;
    unallocatedAmount: number;
    allocations: SummaryAllocation[];
}

const CATEGORIES = [
    'Rent', 'Groceries', 'Food', 'Transportation', 'Utilities', 'Healthcare',
    'EMI', 'Savings', 'Investments', 'Entertainment', 'Shopping', 'Education',
    'Personal', 'Subscriptions', 'Others',
];

export default function SalaryPlanner() {
    const now = new Date();
    const [month, setMonth] = useState(now.getMonth() + 1);
    const [year, setYear] = useState(now.getFullYear());
    const [totalSalary, setTotalSalary] = useState('');
    const [allocations, setAllocations] = useState<Allocation[]>([
        { category: 'Rent', allocationType: 'FIXED', allocationValue: '', allocatedAmount: 0 },
    ]);
    const [summary, setSummary] = useState<SalarySummary | null>(null);
    const [loading, setLoading] = useState(false);
    const [saving, setSaving] = useState(false);
    const [activeTab, setActiveTab] = useState<'plan' | 'summary'>('plan');

    useEffect(() => {
        fetchSummary();
    }, [month, year]);

    const fetchSummary = async () => {
        setLoading(true);
        try {
            const res = await salaryApi.getSummary(year, month);
            setSummary(res.data.data);
            if (res.data.data.totalSalary > 0) {
                setTotalSalary(String(res.data.data.totalSalary));
            }
        } catch { /* empty plan */ }
        setLoading(false);
    };

    const addRow = () => {
        setAllocations([...allocations, { category: '', allocationType: 'FIXED', allocationValue: '', allocatedAmount: 0 }]);
    };

    const removeRow = (idx: number) => {
        setAllocations(allocations.filter((_, i) => i !== idx));
    };

    const updateRow = (idx: number, field: keyof Allocation, value: string) => {
        const updated = [...allocations];
        const row = { ...updated[idx], [field]: value };
        // Recalculate allocated amount
        const salary = parseFloat(totalSalary) || 0;
        const val = parseFloat(row.allocationValue) || 0;
        row.allocatedAmount = row.allocationType === 'PERCENTAGE'
            ? +(salary * val / 100).toFixed(2)
            : val;
        updated[idx] = row;
        setAllocations(updated);
    };

    const totalAllocated = allocations.reduce((s, a) => s + a.allocatedAmount, 0);
    const remaining = (parseFloat(totalSalary) || 0) - totalAllocated;
    const totalPct = allocations.filter(a => a.allocationType === 'PERCENTAGE')
        .reduce((s, a) => s + (parseFloat(a.allocationValue) || 0), 0);

    const savePlan = async () => {
        if (!totalSalary || allocations.length === 0) return;
        setSaving(true);
        try {
            const payload = allocations.map(a => ({
                month, year,
                totalSalary: parseFloat(totalSalary),
                category: a.category,
                allocationType: a.allocationType,
                allocationValue: parseFloat(a.allocationValue) || 0,
            }));
            await salaryApi.savePlan(payload);
            await fetchSummary();
            setActiveTab('summary');
        } catch (err: unknown) {
            const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message || 'Failed to save';
            alert(msg);
        }
        setSaving(false);
    };

    return (
        <div>
            <h1 className="text-2xl font-bold mb-6" style={{ color: '#e2e8f0' }}>💰 Salary Planner</h1>

            {/* Month/Year selector */}
            <div className="flex gap-3 mb-6">
                <select value={month} onChange={e => setMonth(+e.target.value)} className="input-field" style={{ width: '140px' }}>
                    {Array.from({ length: 12 }, (_, i) => (
                        <option key={i + 1} value={i + 1}>
                            {new Date(2000, i).toLocaleString('default', { month: 'long' })}
                        </option>
                    ))}
                </select>
                <select value={year} onChange={e => setYear(+e.target.value)} className="input-field" style={{ width: '100px' }}>
                    {[2024, 2025, 2026, 2027].map(y => <option key={y} value={y}>{y}</option>)}
                </select>
            </div>

            {/* Tabs */}
            <div className="flex gap-2 mb-6">
                <button onClick={() => setActiveTab('plan')}
                    className={activeTab === 'plan' ? 'btn-primary' : 'btn-secondary'}>
                    Plan Allocations
                </button>
                <button onClick={() => setActiveTab('summary')}
                    className={activeTab === 'summary' ? 'btn-primary' : 'btn-secondary'}>
                    Summary & Variance
                </button>
            </div>

            {activeTab === 'plan' && (
                <div className="card" style={{ maxWidth: '900px' }}>
                    {/* Total salary */}
                    <div className="mb-6">
                        <label className="block text-sm font-medium mb-2" style={{ color: '#94a3b8' }}>Monthly Salary</label>
                        <input
                            type="number"
                            value={totalSalary}
                            onChange={e => {
                                setTotalSalary(e.target.value);
                                // Recalculate all allocations
                                const salary = parseFloat(e.target.value) || 0;
                                setAllocations(allocations.map(a => ({
                                    ...a,
                                    allocatedAmount: a.allocationType === 'PERCENTAGE'
                                        ? +(salary * (parseFloat(a.allocationValue) || 0) / 100).toFixed(2)
                                        : parseFloat(a.allocationValue) || 0,
                                })));
                            }}
                            className="input-field"
                            placeholder="e.g. 100000"
                            style={{ maxWidth: '300px' }}
                        />
                    </div>

                    {/* Allocation rows */}
                    <div className="space-y-3 mb-6">
                        <div className="grid grid-cols-12 gap-2 text-xs font-semibold" style={{ color: '#94a3b8' }}>
                            <div className="col-span-3">Category</div>
                            <div className="col-span-2">Type</div>
                            <div className="col-span-2">Value</div>
                            <div className="col-span-3">Allocated</div>
                            <div className="col-span-2"></div>
                        </div>

                        {allocations.map((alloc, idx) => (
                            <div key={idx} className="grid grid-cols-12 gap-2 items-center">
                                <select
                                    value={alloc.category}
                                    onChange={e => updateRow(idx, 'category', e.target.value)}
                                    className="input-field col-span-3"
                                >
                                    <option value="">Select...</option>
                                    {CATEGORIES.map(c => <option key={c} value={c}>{c}</option>)}
                                </select>

                                <select
                                    value={alloc.allocationType}
                                    onChange={e => updateRow(idx, 'allocationType', e.target.value)}
                                    className="input-field col-span-2"
                                >
                                    <option value="FIXED">Fixed ₹</option>
                                    <option value="PERCENTAGE">% of Salary</option>
                                </select>

                                <input
                                    type="number"
                                    value={alloc.allocationValue}
                                    onChange={e => updateRow(idx, 'allocationValue', e.target.value)}
                                    className="input-field col-span-2"
                                    placeholder={alloc.allocationType === 'PERCENTAGE' ? '%' : '₹'}
                                />

                                <div className="col-span-3 text-sm font-semibold" style={{ color: '#818cf8' }}>
                                    ₹{alloc.allocatedAmount.toLocaleString()}
                                </div>

                                <button onClick={() => removeRow(idx)} className="col-span-2 text-sm" style={{ color: '#ef4444' }}>
                                    ✕ Remove
                                </button>
                            </div>
                        ))}
                    </div>

                    <button onClick={addRow} className="btn-secondary text-sm mb-6">+ Add Category</button>

                    {/* Summary bar */}
                    <div className="p-4 rounded-xl mb-4" style={{ background: 'rgba(99,102,241,0.1)', border: '1px solid rgba(99,102,241,0.2)' }}>
                        <div className="flex justify-between text-sm mb-2">
                            <span style={{ color: '#94a3b8' }}>Total Allocated</span>
                            <span className="font-bold" style={{ color: '#818cf8' }}>₹{totalAllocated.toLocaleString()}</span>
                        </div>
                        <div className="flex justify-between text-sm mb-2">
                            <span style={{ color: '#94a3b8' }}>Remaining</span>
                            <span className="font-bold" style={{ color: remaining >= 0 ? '#22c55e' : '#ef4444' }}>
                                ₹{remaining.toLocaleString()}
                            </span>
                        </div>
                        {totalPct > 0 && (
                            <div className="flex justify-between text-sm">
                                <span style={{ color: '#94a3b8' }}>Percentage Used</span>
                                <span className="font-bold" style={{ color: totalPct > 100 ? '#ef4444' : '#94a3b8' }}>
                                    {totalPct}%
                                </span>
                            </div>
                        )}
                        {/* Progress bar */}
                        <div className="progress-bar mt-3">
                            <div className="progress-fill" style={{
                                width: `${Math.min((totalAllocated / (parseFloat(totalSalary) || 1)) * 100, 100)}%`,
                                background: remaining >= 0 ? 'linear-gradient(90deg, #6366f1, #818cf8)' : '#ef4444',
                            }} />
                        </div>
                    </div>

                    <button onClick={savePlan} disabled={saving || !totalSalary}
                        className="btn-primary w-full">
                        {saving ? 'Saving...' : 'Save Plan'}
                    </button>
                </div>
            )}

            {activeTab === 'summary' && (
                <div>
                    {loading ? (
                        <div className="card text-center" style={{ color: '#94a3b8' }}>Loading...</div>
                    ) : !summary || summary.totalSalary === 0 ? (
                        <div className="card text-center" style={{ color: '#94a3b8' }}>
                            No salary plan for {new Date(2000, month - 1).toLocaleString('default', { month: 'long' })} {year}.
                            Switch to "Plan Allocations" to create one.
                        </div>
                    ) : (
                        <>
                            {/* Summary cards */}
                            <div className="grid grid-cols-1 md:grid-cols-4 gap-4 mb-6">
                                {[
                                    { label: 'Total Salary', value: summary.totalSalary, color: '#6366f1' },
                                    { label: 'Planned', value: summary.totalPlanned, color: '#0ea5e9' },
                                    { label: 'Actual Spent', value: summary.totalActualSpent, color: '#f59e0b' },
                                    { label: 'Savings', value: summary.totalSavings, color: summary.totalSavings >= 0 ? '#22c55e' : '#ef4444' },
                                ].map(c => (
                                    <div key={c.label} className="card text-center">
                                        <div className="text-xs mb-1" style={{ color: '#94a3b8' }}>{c.label}</div>
                                        <div className="text-xl font-bold" style={{ color: c.color }}>₹{c.value.toLocaleString()}</div>
                                    </div>
                                ))}
                            </div>

                            {/* Category breakdown table */}
                            <div className="card overflow-x-auto">
                                <h3 className="text-lg font-semibold mb-4" style={{ color: '#e2e8f0' }}>Category Breakdown</h3>
                                <table className="w-full text-sm">
                                    <thead>
                                        <tr style={{ color: '#94a3b8', borderBottom: '1px solid rgba(99,102,241,0.2)' }}>
                                            <th className="text-left py-2">Category</th>
                                            <th className="text-right py-2">Type</th>
                                            <th className="text-right py-2">Allocated</th>
                                            <th className="text-right py-2">Actual</th>
                                            <th className="text-right py-2">Variance</th>
                                            <th className="text-right py-2">Status</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        {summary.allocations.map(a => (
                                            <tr key={a.id} style={{ borderBottom: '1px solid rgba(99,102,241,0.1)' }}>
                                                <td className="py-3 font-medium" style={{ color: '#e2e8f0' }}>{a.category}</td>
                                                <td className="text-right py-3" style={{ color: '#94a3b8' }}>
                                                    {a.allocationType === 'PERCENTAGE' ? `${a.allocationValue}%` : 'Fixed'}
                                                </td>
                                                <td className="text-right py-3 font-semibold" style={{ color: '#818cf8' }}>
                                                    ₹{a.allocatedAmount.toLocaleString()}
                                                </td>
                                                <td className="text-right py-3" style={{ color: '#f59e0b' }}>
                                                    ₹{a.actualSpent.toLocaleString()}
                                                </td>
                                                <td className="text-right py-3 font-semibold"
                                                    style={{ color: a.variance >= 0 ? '#22c55e' : '#ef4444' }}>
                                                    {a.variance >= 0 ? '+' : ''}₹{a.variance.toLocaleString()}
                                                </td>
                                                <td className="text-right py-3">
                                                    <span className="badge" style={{
                                                        background: a.variance >= 0 ? 'rgba(34,197,94,0.15)' : 'rgba(239,68,68,0.15)',
                                                        color: a.variance >= 0 ? '#22c55e' : '#ef4444',
                                                    }}>
                                                        {a.variance >= 0 ? 'Under' : 'Over'}
                                                    </span>
                                                </td>
                                            </tr>
                                        ))}
                                    </tbody>
                                </table>
                            </div>
                        </>
                    )}
                </div>
            )}
        </div>
    );
}
