import { useEffect, useState } from 'react';
import { reportApi, transactionApi } from '../api/client';
import { PieChart, Pie, Cell, ResponsiveContainer, Tooltip, BarChart, Bar, XAxis, YAxis, CartesianGrid } from 'recharts';
import { PlusIcon, ArrowTrendingUpIcon, ArrowTrendingDownIcon } from '@heroicons/react/24/outline';

const COLORS = ['#6366f1', '#0ea5e9', '#f59e0b', '#22c55e', '#ef4444', '#ec4899', '#8b5cf6', '#14b8a6'];

interface CategoryBreakdown {
    category: string;
    amount: number;
    percentage: number;
}

interface DailyTrend {
    date: string;
    amount: number;
}

interface Transaction {
    id: number;
    amount: number;
    merchant: string;
    category: string;
    transactionDate: string;
    notes: string;
}

export default function Dashboard() {
    const [totalSpent, setTotalSpent] = useState(0);
    const [categoryData, setCategoryData] = useState<CategoryBreakdown[]>([]);
    const [trendData, setTrendData] = useState<DailyTrend[]>([]);
    const [recentTransactions, setRecentTransactions] = useState<Transaction[]>([]);
    const [loading, setLoading] = useState(true);
    const [showQuickAdd, setShowQuickAdd] = useState(false);
    const [quickForm, setQuickForm] = useState({
        amount: '', merchant: '', category: 'Food & Groceries',
        transactionDate: new Date().toISOString().split('T')[0], notes: '',
    });

    const now = new Date();

    useEffect(() => {
        loadDashboard();
    }, []);

    const loadDashboard = async () => {
        try {
            const [reportRes, txnRes] = await Promise.all([
                reportApi.monthly(now.getFullYear(), now.getMonth() + 1),
                transactionApi.list({ size: 5, sort: 'transactionDate,desc' }),
            ]);
            const report = reportRes.data.data;
            setTotalSpent(report.totalSpent || 0);
            setCategoryData(report.categoryBreakdown || []);

            // Aggregate trends into weekly buckets for cleaner chart
            const trends: DailyTrend[] = report.dailyTrends || [];
            const weeklyData: DailyTrend[] = [];
            for (let i = 0; i < trends.length; i += 7) {
                const week = trends.slice(i, i + 7);
                const total = week.reduce((sum: number, d: DailyTrend) => sum + (d.amount || 0), 0);
                weeklyData.push({ date: `Week ${Math.floor(i / 7) + 1}`, amount: total });
            }
            setTrendData(weeklyData);

            setRecentTransactions(txnRes.data.data?.content || []);
        } catch (err) {
            console.error('Failed to load dashboard', err);
        } finally {
            setLoading(false);
        }
    };

    const handleQuickAdd = async () => {
        try {
            await transactionApi.create({
                amount: parseFloat(quickForm.amount),
                merchant: quickForm.merchant,
                category: quickForm.category,
                transactionDate: quickForm.transactionDate,
                notes: quickForm.notes,
            });
            setShowQuickAdd(false);
            setQuickForm({
                amount: '', merchant: '', category: 'Food & Groceries',
                transactionDate: new Date().toISOString().split('T')[0], notes: '',
            });
            loadDashboard();
        } catch (err) {
            console.error('Failed to add expense', err);
        }
    };

    const categories = ['Food & Groceries', 'Transportation', 'Shopping', 'Entertainment', 'Utilities', 'Healthcare', 'Housing', 'Other'];

    if (loading) {
        return (
            <div className="flex items-center justify-center h-96">
                <div className="w-10 h-10 border-3 border-primary/30 border-t-primary rounded-full animate-spin" />
            </div>
        );
    }

    return (
        <div className="space-y-6">
            {/* Header */}
            <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
                <div>
                    <h1 className="text-2xl lg:text-3xl font-bold">Dashboard</h1>
                    <p className="text-text-muted mt-1">
                        {now.toLocaleString('default', { month: 'long', year: 'numeric' })} Overview
                    </p>
                </div>
                <button onClick={() => setShowQuickAdd(true)} className="btn-primary flex items-center gap-2">
                    <PlusIcon className="w-5 h-5" />
                    Quick Add Expense
                </button>
            </div>

            {/* Stats Cards */}
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                <div className="card-hover">
                    <div className="flex items-center justify-between">
                        <div>
                            <p className="text-text-muted text-sm">Total Spent</p>
                            <p className="text-3xl font-bold mt-1 bg-gradient-to-r from-danger to-accent bg-clip-text text-transparent">
                                ${totalSpent.toLocaleString('en-US', { minimumFractionDigits: 2 })}
                            </p>
                        </div>
                        <div className="w-12 h-12 bg-danger/10 rounded-xl flex items-center justify-center">
                            <ArrowTrendingDownIcon className="w-6 h-6 text-danger" />
                        </div>
                    </div>
                </div>
                <div className="card-hover">
                    <div className="flex items-center justify-between">
                        <div>
                            <p className="text-text-muted text-sm">Transactions</p>
                            <p className="text-3xl font-bold mt-1">{recentTransactions.length}+</p>
                        </div>
                        <div className="w-12 h-12 bg-primary/10 rounded-xl flex items-center justify-center">
                            <ArrowTrendingUpIcon className="w-6 h-6 text-primary" />
                        </div>
                    </div>
                </div>
                <div className="card-hover">
                    <div className="flex items-center justify-between">
                        <div>
                            <p className="text-text-muted text-sm">Categories</p>
                            <p className="text-3xl font-bold mt-1">{categoryData.length}</p>
                        </div>
                        <div className="w-12 h-12 bg-secondary/10 rounded-xl flex items-center justify-center">
                            <ArrowTrendingUpIcon className="w-6 h-6 text-secondary" />
                        </div>
                    </div>
                </div>
            </div>

            {/* Charts Row */}
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                {/* Pie Chart */}
                <div className="card">
                    <h2 className="text-lg font-semibold mb-4">Spending by Category</h2>
                    {categoryData.length > 0 ? (
                        <div className="flex flex-col sm:flex-row items-center gap-4">
                            <ResponsiveContainer width="100%" height={220}>
                                <PieChart>
                                    <Pie
                                        data={categoryData}
                                        dataKey="amount"
                                        nameKey="category"
                                        cx="50%"
                                        cy="50%"
                                        outerRadius={90}
                                        innerRadius={50}
                                        paddingAngle={2}
                                        stroke="none"
                                    >
                                        {categoryData.map((_, i) => (
                                            <Cell key={i} fill={COLORS[i % COLORS.length]} />
                                        ))}
                                    </Pie>
                                    <Tooltip
                                        contentStyle={{
                                            background: '#1a1545',
                                            border: '1px solid rgba(99,102,241,0.3)',
                                            borderRadius: '12px',
                                            color: '#e2e8f0',
                                        }}
                                        formatter={(value: number) => `$${value.toFixed(2)}`}
                                    />
                                </PieChart>
                            </ResponsiveContainer>
                            <div className="space-y-2 w-full sm:w-auto">
                                {categoryData.map((cat, i) => (
                                    <div key={cat.category} className="flex items-center gap-2 text-sm">
                                        <div
                                            className="w-3 h-3 rounded-full flex-shrink-0"
                                            style={{ backgroundColor: COLORS[i % COLORS.length] }}
                                        />
                                        <span className="text-text-muted truncate">{cat.category}</span>
                                        <span className="ml-auto font-medium">{cat.percentage?.toFixed(0)}%</span>
                                    </div>
                                ))}
                            </div>
                        </div>
                    ) : (
                        <p className="text-text-muted text-center py-8">No data yet</p>
                    )}
                </div>

                {/* Bar Chart */}
                <div className="card">
                    <h2 className="text-lg font-semibold mb-4">Weekly Spending Trend</h2>
                    {trendData.length > 0 ? (
                        <ResponsiveContainer width="100%" height={220}>
                            <BarChart data={trendData}>
                                <CartesianGrid strokeDasharray="3 3" stroke="#312e81" />
                                <XAxis dataKey="date" stroke="#94a3b8" fontSize={12} />
                                <YAxis stroke="#94a3b8" fontSize={12} tickFormatter={(v) => `$${v}`} />
                                <Tooltip
                                    contentStyle={{
                                        background: '#1a1545',
                                        border: '1px solid rgba(99,102,241,0.3)',
                                        borderRadius: '12px',
                                        color: '#e2e8f0',
                                    }}
                                    formatter={(value: number) => `$${value.toFixed(2)}`}
                                />
                                <Bar dataKey="amount" fill="#6366f1" radius={[6, 6, 0, 0]} />
                            </BarChart>
                        </ResponsiveContainer>
                    ) : (
                        <p className="text-text-muted text-center py-8">No data yet</p>
                    )}
                </div>
            </div>

            {/* Recent Transactions */}
            <div className="card">
                <h2 className="text-lg font-semibold mb-4">Recent Transactions</h2>
                <div className="space-y-3">
                    {recentTransactions.map((txn) => (
                        <div key={txn.id} className="flex items-center justify-between py-3 border-b border-primary/5 last:border-0">
                            <div className="flex items-center gap-3">
                                <div className="w-10 h-10 bg-primary/10 rounded-xl flex items-center justify-center text-sm font-bold text-primary">
                                    {txn.merchant?.charAt(0) || '?'}
                                </div>
                                <div>
                                    <p className="font-medium">{txn.merchant || 'Unknown'}</p>
                                    <p className="text-xs text-text-muted">{txn.category} · {txn.transactionDate}</p>
                                </div>
                            </div>
                            <span className="font-semibold text-danger">
                                -${txn.amount?.toFixed(2)}
                            </span>
                        </div>
                    ))}
                    {recentTransactions.length === 0 && (
                        <p className="text-text-muted text-center py-4">No transactions yet</p>
                    )}
                </div>
            </div>

            {/* Quick Add Modal */}
            {showQuickAdd && (
                <div className="modal-overlay" onClick={() => setShowQuickAdd(false)}>
                    <div className="modal-content" onClick={(e) => e.stopPropagation()}>
                        <h2 className="text-xl font-bold mb-5">Quick Add Expense</h2>
                        <div className="space-y-4">
                            <div>
                                <label className="block text-sm font-medium text-text-muted mb-1">Amount</label>
                                <input
                                    type="number"
                                    step="0.01"
                                    value={quickForm.amount}
                                    onChange={(e) => setQuickForm({ ...quickForm, amount: e.target.value })}
                                    className="input-field"
                                    placeholder="0.00"
                                />
                            </div>
                            <div>
                                <label className="block text-sm font-medium text-text-muted mb-1">Merchant</label>
                                <input
                                    type="text"
                                    value={quickForm.merchant}
                                    onChange={(e) => setQuickForm({ ...quickForm, merchant: e.target.value })}
                                    className="input-field"
                                    placeholder="e.g. Amazon"
                                />
                            </div>
                            <div>
                                <label className="block text-sm font-medium text-text-muted mb-1">Category</label>
                                <select
                                    value={quickForm.category}
                                    onChange={(e) => setQuickForm({ ...quickForm, category: e.target.value })}
                                    className="input-field"
                                >
                                    {categories.map((c) => <option key={c} value={c}>{c}</option>)}
                                </select>
                            </div>
                            <div>
                                <label className="block text-sm font-medium text-text-muted mb-1">Date</label>
                                <input
                                    type="date"
                                    value={quickForm.transactionDate}
                                    onChange={(e) => setQuickForm({ ...quickForm, transactionDate: e.target.value })}
                                    className="input-field"
                                />
                            </div>
                            <div>
                                <label className="block text-sm font-medium text-text-muted mb-1">Notes</label>
                                <input
                                    type="text"
                                    value={quickForm.notes}
                                    onChange={(e) => setQuickForm({ ...quickForm, notes: e.target.value })}
                                    className="input-field"
                                    placeholder="Optional"
                                />
                            </div>
                            <div className="flex gap-3 pt-2">
                                <button onClick={() => setShowQuickAdd(false)} className="btn-secondary flex-1">Cancel</button>
                                <button onClick={handleQuickAdd} className="btn-primary flex-1">Add Expense</button>
                            </div>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}
