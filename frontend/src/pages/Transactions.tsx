import { useEffect, useState } from 'react';
import { transactionApi } from '../api/client';
import { PlusIcon, PencilIcon, TrashIcon, MagnifyingGlassIcon, FunnelIcon } from '@heroicons/react/24/outline';

interface Transaction {
    id: number;
    amount: number;
    currency: string;
    merchant: string;
    category: string;
    transactionDate: string;
    notes: string;
    accountId: number | null;
}

const categories = ['Food & Groceries', 'Transportation', 'Shopping', 'Entertainment', 'Utilities', 'Healthcare', 'Housing', 'Other'];

export default function Transactions() {
    const [transactions, setTransactions] = useState<Transaction[]>([]);
    const [totalPages, setTotalPages] = useState(0);
    const [page, setPage] = useState(0);
    const [loading, setLoading] = useState(true);

    // Filters
    const [filterCategory, setFilterCategory] = useState('');
    const [filterStartDate, setFilterStartDate] = useState('');
    const [filterEndDate, setFilterEndDate] = useState('');
    const [searchMerchant, setSearchMerchant] = useState('');
    const [showFilters, setShowFilters] = useState(false);

    // Modal
    const [showModal, setShowModal] = useState(false);
    const [editingTxn, setEditingTxn] = useState<Transaction | null>(null);
    const [form, setForm] = useState({
        amount: '', merchant: '', category: 'Food & Groceries',
        transactionDate: new Date().toISOString().split('T')[0], notes: '',
    });

    useEffect(() => {
        loadTransactions();
    }, [page, filterCategory, filterStartDate, filterEndDate]);

    const loadTransactions = async () => {
        setLoading(true);
        try {
            const params: Record<string, string | number> = {
                page, size: 15, sort: 'transactionDate,desc',
            };
            if (filterCategory) params.category = filterCategory;
            if (filterStartDate) params.startDate = filterStartDate;
            if (filterEndDate) params.endDate = filterEndDate;
            if (searchMerchant) params.merchant = searchMerchant;

            const res = await transactionApi.list(params);
            setTransactions(res.data.data?.content || []);
            setTotalPages(res.data.data?.totalPages || 0);
        } catch (err) {
            console.error('Failed to load transactions', err);
        } finally {
            setLoading(false);
        }
    };

    const handleSearch = () => {
        setPage(0);
        loadTransactions();
    };

    const openCreate = () => {
        setEditingTxn(null);
        setForm({
            amount: '', merchant: '', category: 'Food & Groceries',
            transactionDate: new Date().toISOString().split('T')[0], notes: '',
        });
        setShowModal(true);
    };

    const openEdit = (txn: Transaction) => {
        setEditingTxn(txn);
        setForm({
            amount: txn.amount.toString(),
            merchant: txn.merchant || '',
            category: txn.category,
            transactionDate: txn.transactionDate,
            notes: txn.notes || '',
        });
        setShowModal(true);
    };

    const handleSave = async () => {
        const data = {
            amount: parseFloat(form.amount),
            merchant: form.merchant,
            category: form.category,
            transactionDate: form.transactionDate,
            notes: form.notes,
        };

        try {
            if (editingTxn) {
                await transactionApi.update(editingTxn.id, data);
            } else {
                await transactionApi.create(data);
            }
            setShowModal(false);
            loadTransactions();
        } catch (err) {
            console.error('Save failed', err);
        }
    };

    const handleDelete = async (id: number) => {
        if (!confirm('Are you sure you want to delete this transaction?')) return;
        try {
            await transactionApi.delete(id);
            loadTransactions();
        } catch (err) {
            console.error('Delete failed', err);
        }
    };

    return (
        <div className="space-y-6">
            {/* Header */}
            <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
                <div>
                    <h1 className="text-2xl lg:text-3xl font-bold">Transactions</h1>
                    <p className="text-text-muted mt-1">Manage your expenses</p>
                </div>
                <div className="flex gap-3">
                    <button onClick={() => setShowFilters(!showFilters)} className="btn-secondary flex items-center gap-2">
                        <FunnelIcon className="w-5 h-5" />
                        Filters
                    </button>
                    <button onClick={openCreate} className="btn-primary flex items-center gap-2">
                        <PlusIcon className="w-5 h-5" />
                        Add
                    </button>
                </div>
            </div>

            {/* Filters */}
            {showFilters && (
                <div className="card grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-4">
                    <div className="lg:col-span-2 relative">
                        <MagnifyingGlassIcon className="w-5 h-5 absolute left-3 top-3.5 text-text-muted" />
                        <input
                            type="text"
                            value={searchMerchant}
                            onChange={(e) => setSearchMerchant(e.target.value)}
                            onKeyDown={(e) => e.key === 'Enter' && handleSearch()}
                            className="input-field pl-10"
                            placeholder="Search merchant..."
                        />
                    </div>
                    <select
                        value={filterCategory}
                        onChange={(e) => { setFilterCategory(e.target.value); setPage(0); }}
                        className="input-field"
                    >
                        <option value="">All Categories</option>
                        {categories.map((c) => <option key={c} value={c}>{c}</option>)}
                    </select>
                    <input
                        type="date"
                        value={filterStartDate}
                        onChange={(e) => { setFilterStartDate(e.target.value); setPage(0); }}
                        className="input-field"
                        placeholder="Start date"
                    />
                    <input
                        type="date"
                        value={filterEndDate}
                        onChange={(e) => { setFilterEndDate(e.target.value); setPage(0); }}
                        className="input-field"
                        placeholder="End date"
                    />
                </div>
            )}

            {/* Table */}
            <div className="card overflow-x-auto">
                {loading ? (
                    <div className="flex justify-center py-12">
                        <div className="w-8 h-8 border-3 border-primary/30 border-t-primary rounded-full animate-spin" />
                    </div>
                ) : (
                    <table className="w-full text-sm">
                        <thead>
                            <tr className="border-b border-primary/10">
                                <th className="text-left text-text-muted font-medium py-3 px-4">Date</th>
                                <th className="text-left text-text-muted font-medium py-3 px-4">Merchant</th>
                                <th className="text-left text-text-muted font-medium py-3 px-4">Category</th>
                                <th className="text-right text-text-muted font-medium py-3 px-4">Amount</th>
                                <th className="text-left text-text-muted font-medium py-3 px-4 hidden md:table-cell">Notes</th>
                                <th className="text-right text-text-muted font-medium py-3 px-4">Actions</th>
                            </tr>
                        </thead>
                        <tbody>
                            {transactions.map((txn) => (
                                <tr key={txn.id} className="border-b border-primary/5 hover:bg-surface-light/20 transition-colors">
                                    <td className="py-3 px-4 text-text-muted">{txn.transactionDate}</td>
                                    <td className="py-3 px-4 font-medium">{txn.merchant || '—'}</td>
                                    <td className="py-3 px-4">
                                        <span className="badge bg-primary/10 text-primary-light">{txn.category}</span>
                                    </td>
                                    <td className="py-3 px-4 text-right font-semibold text-danger">
                                        -${txn.amount?.toFixed(2)}
                                    </td>
                                    <td className="py-3 px-4 text-text-muted hidden md:table-cell truncate max-w-[200px]">
                                        {txn.notes || '—'}
                                    </td>
                                    <td className="py-3 px-4 text-right">
                                        <div className="flex justify-end gap-1">
                                            <button
                                                onClick={() => openEdit(txn)}
                                                className="p-2 hover:bg-primary/10 rounded-lg transition-colors"
                                            >
                                                <PencilIcon className="w-4 h-4 text-primary-light" />
                                            </button>
                                            <button
                                                onClick={() => handleDelete(txn.id)}
                                                className="p-2 hover:bg-danger/10 rounded-lg transition-colors"
                                            >
                                                <TrashIcon className="w-4 h-4 text-danger" />
                                            </button>
                                        </div>
                                    </td>
                                </tr>
                            ))}
                            {transactions.length === 0 && (
                                <tr>
                                    <td colSpan={6} className="text-center py-12 text-text-muted">
                                        No transactions found
                                    </td>
                                </tr>
                            )}
                        </tbody>
                    </table>
                )}

                {/* Pagination */}
                {totalPages > 1 && (
                    <div className="flex items-center justify-center gap-2 pt-4 border-t border-primary/10 mt-4">
                        <button
                            onClick={() => setPage(Math.max(0, page - 1))}
                            disabled={page === 0}
                            className="btn-secondary text-sm py-2 px-4 disabled:opacity-50"
                        >
                            Previous
                        </button>
                        <span className="text-sm text-text-muted">
                            Page {page + 1} of {totalPages}
                        </span>
                        <button
                            onClick={() => setPage(Math.min(totalPages - 1, page + 1))}
                            disabled={page >= totalPages - 1}
                            className="btn-secondary text-sm py-2 px-4 disabled:opacity-50"
                        >
                            Next
                        </button>
                    </div>
                )}
            </div>

            {/* Add/Edit Modal */}
            {showModal && (
                <div className="modal-overlay" onClick={() => setShowModal(false)}>
                    <div className="modal-content" onClick={(e) => e.stopPropagation()}>
                        <h2 className="text-xl font-bold mb-5">
                            {editingTxn ? 'Edit Transaction' : 'Add Transaction'}
                        </h2>
                        <div className="space-y-4">
                            <div>
                                <label className="block text-sm font-medium text-text-muted mb-1">Amount</label>
                                <input
                                    type="number"
                                    step="0.01"
                                    value={form.amount}
                                    onChange={(e) => setForm({ ...form, amount: e.target.value })}
                                    className="input-field"
                                    placeholder="0.00"
                                />
                            </div>
                            <div>
                                <label className="block text-sm font-medium text-text-muted mb-1">Merchant</label>
                                <input
                                    type="text"
                                    value={form.merchant}
                                    onChange={(e) => setForm({ ...form, merchant: e.target.value })}
                                    className="input-field"
                                    placeholder="e.g. Amazon"
                                />
                            </div>
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
                            <div>
                                <label className="block text-sm font-medium text-text-muted mb-1">Date</label>
                                <input
                                    type="date"
                                    value={form.transactionDate}
                                    onChange={(e) => setForm({ ...form, transactionDate: e.target.value })}
                                    className="input-field"
                                />
                            </div>
                            <div>
                                <label className="block text-sm font-medium text-text-muted mb-1">Notes</label>
                                <input
                                    type="text"
                                    value={form.notes}
                                    onChange={(e) => setForm({ ...form, notes: e.target.value })}
                                    className="input-field"
                                    placeholder="Optional"
                                />
                            </div>
                            <div className="flex gap-3 pt-2">
                                <button onClick={() => setShowModal(false)} className="btn-secondary flex-1">Cancel</button>
                                <button onClick={handleSave} className="btn-primary flex-1">
                                    {editingTxn ? 'Update' : 'Add'}
                                </button>
                            </div>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}
