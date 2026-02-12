import { useState, useRef } from 'react';
import { transactionApi } from '../api/client';
import { ArrowUpTrayIcon, CheckCircleIcon, XMarkIcon } from '@heroicons/react/24/outline';

interface ParsedRow {
    amount: string;
    merchant: string;
    category: string;
    date: string;
    notes: string;
}

export default function CsvImport() {
    const [file, setFile] = useState<File | null>(null);
    const [preview, setPreview] = useState<ParsedRow[]>([]);
    const [importing, setImporting] = useState(false);
    const [result, setResult] = useState<{ success: boolean; message: string } | null>(null);
    const fileRef = useRef<HTMLInputElement>(null);

    const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const selected = e.target.files?.[0];
        if (!selected) return;
        setFile(selected);
        setResult(null);
        parsePreview(selected);
    };

    const parsePreview = (file: File) => {
        const reader = new FileReader();
        reader.onload = (e) => {
            const text = e.target?.result as string;
            const lines = text.split('\n').filter((l) => l.trim());
            const rows: ParsedRow[] = [];

            // Skip header
            for (let i = 1; i < Math.min(lines.length, 11); i++) {
                const cols = lines[i].split(',').map((c) => c.trim().replace(/^"|"$/g, ''));
                if (cols.length >= 4) {
                    rows.push({
                        amount: cols[0],
                        merchant: cols[1],
                        category: cols[2],
                        date: cols[3],
                        notes: cols[4] || '',
                    });
                }
            }
            setPreview(rows);
        };
        reader.readAsText(file);
    };

    const handleImport = async () => {
        if (!file) return;
        setImporting(true);
        setResult(null);

        try {
            const res = await transactionApi.importCsv(file);
            const count = res.data.data?.length || 0;
            setResult({ success: true, message: `Successfully imported ${count} transactions!` });
            setFile(null);
            setPreview([]);
            if (fileRef.current) fileRef.current.value = '';
        } catch (err: any) {
            setResult({
                success: false,
                message: err.response?.data?.message || 'Import failed. Please check your CSV format.',
            });
        } finally {
            setImporting(false);
        }
    };

    const handleDrop = (e: React.DragEvent) => {
        e.preventDefault();
        const droppedFile = e.dataTransfer.files[0];
        if (droppedFile?.name.endsWith('.csv')) {
            setFile(droppedFile);
            setResult(null);
            parsePreview(droppedFile);
        }
    };

    return (
        <div className="space-y-6">
            <div>
                <h1 className="text-2xl lg:text-3xl font-bold">Import CSV</h1>
                <p className="text-text-muted mt-1">Upload a CSV file to import transactions</p>
            </div>

            {/* Format info */}
            <div className="card">
                <h3 className="font-semibold mb-3">Expected CSV Format</h3>
                <div className="bg-surface/50 rounded-xl p-4 overflow-x-auto">
                    <code className="text-sm text-primary-light">
                        amount,merchant,category,date,notes<br />
                        45.99,Whole Foods,Food & Groceries,2026-01-15,Weekly groceries<br />
                        12.50,Starbucks,Food & Groceries,2026-01-16,Coffee
                    </code>
                </div>
                <p className="text-xs text-text-muted mt-2">
                    Supported date formats: YYYY-MM-DD, MM/DD/YYYY, DD/MM/YYYY
                </p>
            </div>

            {/* Upload area */}
            <div
                className="card border-2 border-dashed border-primary/30 hover:border-primary/50 transition-colors cursor-pointer text-center py-12"
                onDrop={handleDrop}
                onDragOver={(e) => e.preventDefault()}
                onClick={() => fileRef.current?.click()}
            >
                <input
                    ref={fileRef}
                    type="file"
                    accept=".csv"
                    className="hidden"
                    onChange={handleFileChange}
                />
                <ArrowUpTrayIcon className="w-12 h-12 mx-auto text-primary/50 mb-4" />
                <p className="text-lg font-medium">
                    {file ? file.name : 'Drop CSV file here or click to upload'}
                </p>
                <p className="text-sm text-text-muted mt-1">
                    {file ? `${(file.size / 1024).toFixed(1)} KB` : 'Maximum file size: 10MB'}
                </p>
            </div>

            {/* Result message */}
            {result && (
                <div className={`card flex items-center gap-3 ${result.success ? 'border-success/30 bg-success/5' : 'border-danger/30 bg-danger/5'
                    }`}>
                    {result.success ? (
                        <CheckCircleIcon className="w-6 h-6 text-success flex-shrink-0" />
                    ) : (
                        <XMarkIcon className="w-6 h-6 text-danger flex-shrink-0" />
                    )}
                    <p className={result.success ? 'text-success' : 'text-danger'}>{result.message}</p>
                </div>
            )}

            {/* Preview */}
            {preview.length > 0 && (
                <div className="card">
                    <div className="flex items-center justify-between mb-4">
                        <h3 className="font-semibold">Preview (first 10 rows)</h3>
                        <button
                            onClick={handleImport}
                            disabled={importing}
                            className="btn-primary flex items-center gap-2"
                        >
                            {importing ? (
                                <div className="w-5 h-5 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                            ) : (
                                <>
                                    <CheckCircleIcon className="w-5 h-5" />
                                    Confirm Import
                                </>
                            )}
                        </button>
                    </div>

                    <div className="overflow-x-auto">
                        <table className="w-full text-sm">
                            <thead>
                                <tr className="border-b border-primary/10">
                                    <th className="text-left text-text-muted font-medium py-2 px-3">Amount</th>
                                    <th className="text-left text-text-muted font-medium py-2 px-3">Merchant</th>
                                    <th className="text-left text-text-muted font-medium py-2 px-3">Category</th>
                                    <th className="text-left text-text-muted font-medium py-2 px-3">Date</th>
                                    <th className="text-left text-text-muted font-medium py-2 px-3">Notes</th>
                                </tr>
                            </thead>
                            <tbody>
                                {preview.map((row, i) => (
                                    <tr key={i} className="border-b border-primary/5">
                                        <td className="py-2 px-3 font-medium">${row.amount}</td>
                                        <td className="py-2 px-3">{row.merchant}</td>
                                        <td className="py-2 px-3">
                                            <span className="badge bg-primary/10 text-primary-light">{row.category}</span>
                                        </td>
                                        <td className="py-2 px-3 text-text-muted">{row.date}</td>
                                        <td className="py-2 px-3 text-text-muted">{row.notes}</td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                </div>
            )}
        </div>
    );
}
