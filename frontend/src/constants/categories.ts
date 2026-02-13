export const CATEGORIES = [
    'Housing',
    'Food & Groceries',
    'Transportation',
    'Shopping',
    'Utilities',
    'Entertainment',
    'Healthcare',
    'Education',
    'EMI',
    'Savings',
    'Investments',
    'Subscriptions',
    'Personal',
    'Others'
] as const;

export type Category = (typeof CATEGORIES)[number];
