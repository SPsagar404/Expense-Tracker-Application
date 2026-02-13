import { useState, useRef, useEffect } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import {
    UserIcon,
    BellIcon,
    ArrowRightOnRectangleIcon,
    ChevronDownIcon
} from '@heroicons/react/24/outline';

export default function UserProfileDropdown() {
    const { user, logout } = useAuth();
    const navigate = useNavigate();
    const [isOpen, setIsOpen] = useState(false);
    const dropdownRef = useRef<HTMLDivElement>(null);

    useEffect(() => {
        function handleClickOutside(event: MouseEvent) {
            if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
                setIsOpen(false);
            }
        }
        document.addEventListener('mousedown', handleClickOutside);
        return () => document.removeEventListener('mousedown', handleClickOutside);
    }, []);

    const handleLogout = () => {
        logout();
        navigate('/login');
    };

    if (!user) return null;

    return (
        <div className="relative" ref={dropdownRef}>
            <button
                onClick={() => setIsOpen(!isOpen)}
                className="flex items-center gap-2 p-1 rounded-full hover:bg-white/5 transition-colors focus:outline-none"
            >
                <div className="w-8 h-8 rounded-full bg-gradient-to-br from-primary to-accent flex items-center justify-center text-white font-bold text-sm border border-white/10 shadow-lg">
                    {user.name?.charAt(0).toUpperCase()}
                </div>
                <ChevronDownIcon className={`w-4 h-4 text-text-muted transition-transform duration-200 ${isOpen ? 'rotate-180' : ''}`} />
            </button>

            {/* Dropdown Menu */}
            {isOpen && (
                <div className="absolute right-0 mt-2 w-64 bg-bg-card border border-primary/20 rounded-xl shadow-2xl py-2 z-50 animate-in fade-in slide-in-from-top-2 duration-200">
                    {/* User Info */}
                    <div className="px-4 py-3 border-b border-primary/10">
                        <p className="text-sm font-semibold text-white truncate">{user.name}</p>
                        <p className="text-xs text-text-muted truncate mt-0.5">{user.email}</p>
                    </div>

                    {/* Menu Items */}
                    <div className="py-1">
                        <Link
                            to="/profile"
                            onClick={() => setIsOpen(false)}
                            className="flex items-center gap-3 px-4 py-2 text-sm text-text-muted hover:text-white hover:bg-white/5 transition-colors"
                        >
                            <UserIcon className="w-4 h-4" />
                            <span>Profile</span>
                        </Link>
                        <Link
                            to="/settings/notifications"
                            onClick={() => setIsOpen(false)}
                            className="flex items-center gap-3 px-4 py-2 text-sm text-text-muted hover:text-white hover:bg-white/5 transition-colors"
                        >
                            <BellIcon className="w-4 h-4" />
                            <span>Notification Settings</span>
                        </Link>
                    </div>

                    {/* Footer / Logout */}
                    <div className="border-t border-primary/10 mt-1 pt-1">
                        <button
                            onClick={handleLogout}
                            className="flex items-center gap-3 w-full px-4 py-2 text-sm text-danger hover:bg-danger/10 transition-colors"
                        >
                            <ArrowRightOnRectangleIcon className="w-4 h-4" />
                            <span>Logout</span>
                        </button>
                    </div>
                </div>
            )}
        </div>
    );
}
