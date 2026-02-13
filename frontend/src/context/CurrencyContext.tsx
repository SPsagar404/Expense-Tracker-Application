import { createContext, useContext, useEffect, useState, type ReactNode } from 'react';
import client from '../api/client';

type SupportedCurrency = 'INR' | 'USD' | 'EUR' | 'GBP' | 'AED' | string;

interface CurrencyContextType {
    currency: SupportedCurrency;
    supported: SupportedCurrency[];
    setCurrency: (code: SupportedCurrency) => void;
    convert: (amount: number) => number;
    format: (amount: number) => string;
}

const CurrencyContext = createContext<CurrencyContextType | undefined>(undefined);

const SYMBOLS: Record<string, string> = {
    INR: '₹',
    USD: '$',
    EUR: '€',
    GBP: '£',
    AED: 'د.إ',
};

export function CurrencyProvider({ children }: { children: ReactNode }) {
    const [currency, setCurrencyState] = useState<SupportedCurrency>(
        (localStorage.getItem('preferredCurrency') as SupportedCurrency) || 'INR'
    );
    const [supported, setSupported] = useState<SupportedCurrency[]>(['INR', 'USD', 'EUR', 'GBP', 'AED']);
    const [rates, setRates] = useState<Record<string, number>>({}); // target -> rate from base

    useEffect(() => {
        const load = async () => {
            try {
                const [supportedRes, ratesRes] = await Promise.all([
                    client.get('/currency/supported'),
                    client.get('/currency/rates', { params: { base: 'INR' } }),
                ]);
                setSupported(supportedRes.data.data || supported);
                const list: { baseCurrency: string; targetCurrency: string; rate: number }[] =
                    ratesRes.data.data || [];
                const map: Record<string, number> = {};
                for (const r of list) {
                    map[r.targetCurrency] = r.rate;
                }
                setRates(map);
            } catch {
                // Fallback: keep defaults
            }
        };
        load();
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    const setCurrency = (code: SupportedCurrency) => {
        setCurrencyState(code);
        localStorage.setItem('preferredCurrency', code);
    };

    const convert = (amount: number) => {
        if (!amount) return 0;
        if (currency === 'INR') return amount;
        const rate = rates[currency] ?? 1;
        return amount * rate;
    };

    const format = (amount: number) => {
        const converted = convert(amount);
        const symbol = SYMBOLS[currency] || currency + ' ';
        return `${symbol}${converted.toLocaleString(undefined, { minimumFractionDigits: 2 })}`;
    };

    return (
        <CurrencyContext.Provider
            value={{
                currency,
                supported,
                setCurrency,
                convert,
                format,
            }}
        >
            {children}
        </CurrencyContext.Provider>
    );
}

export function useCurrency() {
    const ctx = useContext(CurrencyContext);
    if (!ctx) throw new Error('useCurrency must be used within CurrencyProvider');
    return ctx;
}

