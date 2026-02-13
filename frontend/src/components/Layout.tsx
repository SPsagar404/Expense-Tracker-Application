import { NavLink, Outlet } from 'react-router-dom';
import {
    HomeIcon,
    CreditCardIcon,
    ChartBarIcon,
    ArrowUpTrayIcon,
    Bars3Icon,
    XMarkIcon,
    BanknotesIcon,
    ArrowPathIcon,
    BellIcon,
} from '@heroicons/react/24/outline';
import { useState } from 'react';
import NotificationBell from './NotificationBell';
import UserProfileDropdown from './UserProfileDropdown';
import { useCurrency } from '../context/CurrencyContext';

const navItems = [
    { to: '/', icon: HomeIcon, label: 'Dashboard' },
    { to: '/transactions', icon: CreditCardIcon, label: 'Transactions' },
    { to: '/budgets', icon: ChartBarIcon, label: 'Budgets' },
    { to: '/salary', icon: BanknotesIcon, label: 'Salary Planner' },
    { to: '/subscriptions', icon: ArrowPathIcon, label: 'Subscriptions' },
    { to: '/import', icon: ArrowUpTrayIcon, label: 'Import CSV' },
    { to: '/notifications', icon: BellIcon, label: 'Notifications' },
];

export default function Layout() {
    const [sidebarOpen, setSidebarOpen] = useState(false);
    const { currency, supported, setCurrency } = useCurrency();

    return (
        <div className="flex h-screen overflow-hidden">
            {/* Mobile overlay */}
            {sidebarOpen && (
                <div
                    className="fixed inset-0 bg-black/50 z-40 lg:hidden"
                    onClick={() => setSidebarOpen(false)}
                />
            )}

            {/* Sidebar */}
            <aside
                className={`fixed lg:static inset-y-0 left-0 z-50 w-72 bg-bg-card border-r border-primary/10
          transform transition-transform duration-300 ease-in-out flex flex-col
          ${sidebarOpen ? 'translate-x-0' : '-translate-x-full lg:translate-x-0'}`}
            >
                {/* Logo */}
                <div className="flex items-center gap-3 px-6 py-6 border-b border-primary/10">
                    <div className="w-10 h-10 bg-gradient-to-br from-primary to-secondary rounded-xl flex items-center justify-center">
                        <span className="text-white text-lg font-bold">₹</span>
                    </div>
                    <div>
                        <h1 className="text-lg font-bold bg-gradient-to-r from-primary-light to-secondary bg-clip-text text-transparent">
                            ExpenseTracker
                        </h1>
                        <p className="text-xs text-text-muted">Personal Finance</p>
                    </div>
                    <button
                        onClick={() => setSidebarOpen(false)}
                        className="ml-auto lg:hidden text-text-muted hover:text-white"
                    >
                        <XMarkIcon className="w-6 h-6" />
                    </button>
                </div>

                {/* Navigation */}
                <nav className="flex-1 px-4 py-6 space-y-1">
                    {navItems.map(({ to, icon: Icon, label }) => (
                        <NavLink
                            key={to}
                            to={to}
                            end={to === '/'}
                            onClick={() => setSidebarOpen(false)}
                            className={({ isActive }) =>
                                isActive ? 'sidebar-link-active' : 'sidebar-link'
                            }
                        >
                            <Icon className="w-5 h-5" />
                            <span>{label}</span>
                        </NavLink>
                    ))}
                </nav>

                {/* Sidebar Footer */}
                <div className="px-4 py-4 border-t border-primary/10">
                    <p className="text-[10px] text-text-muted text-center uppercase tracking-widest font-semibold opacity-50">
                        v1.0.0
                    </p>
                </div>
            </aside>

            {/* Main content */}
            <main className="flex-1 overflow-auto">
                {/* Mobile header */}
                <header className="lg:hidden sticky top-0 z-30 bg-bg-card/80 backdrop-blur-lg border-b border-primary/10 px-4 py-3">
                    <div className="flex items-center gap-3">
                        <button
                            onClick={() => setSidebarOpen(true)}
                            className="text-text-muted hover:text-white"
                        >
                            <Bars3Icon className="w-6 h-6" />
                        </button>
                        <h1 className="text-lg font-bold bg-gradient-to-r from-primary-light to-secondary bg-clip-text text-transparent">
                            ExpenseTracker
                        </h1>
                        <div className="ml-auto flex items-center gap-2">
                            <select
                                value={currency}
                                onChange={(e) => setCurrency(e.target.value)}
                                className="bg-bg-card border border-primary/20 text-[11px] rounded-lg px-2 py-1 text-text-muted focus:outline-none focus:ring-1 focus:ring-primary/60"
                            >
                                {supported.map((c) => (
                                    <option key={c} value={c}>
                                        {c}
                                    </option>
                                ))}
                            </select>
                            <NotificationBell />
                            <UserProfileDropdown />
                        </div>
                    </div>
                </header>

                {/* Desktop header bar */}
                <div className="hidden lg:flex items-center justify-end gap-4 px-8 pt-6">
                    {/* Currency selector */}
                    <select
                        value={currency}
                        onChange={(e) => setCurrency(e.target.value)}
                        className="bg-bg-card border border-primary/20 text-xs rounded-lg px-3 py-1.5 text-text-muted focus:outline-none focus:ring-2 focus:ring-primary/60"
                    >
                        {supported.map((c) => (
                            <option key={c} value={c}>
                                {c}
                            </option>
                        ))}
                    </select>
                    <NotificationBell />
                    <UserProfileDropdown />
                </div>

                <div className="p-4 lg:p-8 max-w-7xl mx-auto">
                    <Outlet />
                </div>
            </main>
        </div>
    );
}
