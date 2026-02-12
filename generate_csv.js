const fs = require('fs');

const merchants = [
    'Amazon', 'Walmart', 'Starbucks', 'Netflix', 'Uber', 'Target', 'Costco',
    'McDonalds', 'Spotify', 'Apple Store', 'Google Play', 'Shell Gas', 'Whole Foods',
    'Trader Joes', 'Home Depot', 'Nike', 'Zara', 'H&M', 'Chipotle', 'Dominos',
    'Grubhub', 'DoorDash', 'Lyft', 'Delta Airlines', 'Hilton Hotels', 'Airbnb',
    'CVS Pharmacy', 'Walgreens', 'Best Buy', 'IKEA', 'Sephora', 'Nordstrom',
    'T-Mobile', 'AT&T', 'Verizon', 'Adobe', 'Microsoft', 'Dropbox', 'Planet Fitness',
    'Electric Co', 'Water Utility', 'Comcast Internet', 'State Farm Insurance',
    'Blue Cross Health', 'Rent Payment', 'Mortgage Co', 'City Tax Office',
    'Dental Care', 'City Hospital', 'PetSmart'
];

const categories = [
    'Food', 'Transportation', 'Shopping', 'Entertainment', 'Utilities',
    'Healthcare', 'Housing', 'Subscriptions', 'Travel', 'Education',
    'Personal', 'Groceries'
];

const noteOptions = [
    'Weekly purchase', 'Monthly bill', 'One-time expense', 'Regular payment',
    'Impulse buy', 'Planned purchase', 'Emergency expense', 'Gift',
    'Recurring charge', ''
];

const lines = ['amount,merchant,category,date,notes'];

for (let i = 0; i < 500; i++) {
    const amount = (Math.random() * 490 + 5).toFixed(2);
    const merchant = merchants[Math.floor(Math.random() * merchants.length)];
    const category = categories[Math.floor(Math.random() * categories.length)];
    const month = String(Math.floor(Math.random() * 12) + 1).padStart(2, '0');
    const day = String(Math.floor(Math.random() * 28) + 1).padStart(2, '0');
    const year = Math.random() > 0.3 ? '2025' : '2024';
    const date = `${year}-${month}-${day}`;
    const notes = noteOptions[Math.floor(Math.random() * noteOptions.length)];
    lines.push(`${amount},${merchant},${category},${date},${notes}`);
}

fs.writeFileSync('sample_transactions.csv', lines.join('\n'), 'utf8');
console.log('Generated 500 records in sample_transactions.csv');
