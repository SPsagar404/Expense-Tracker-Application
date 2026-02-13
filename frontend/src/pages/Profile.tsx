import { useAuth } from '../context/AuthContext';
import { UserIcon, EnvelopeIcon, KeyIcon } from '@heroicons/react/24/outline';

export default function Profile() {
    const { user } = useAuth();

    return (
        <div className="space-y-6">
            <header>
                <h2 className="text-2xl font-bold bg-gradient-to-r from-white to-text-muted bg-clip-text text-transparent">
                    User Profile
                </h2>
                <p className="text-text-muted text-sm mt-1">Manage your account settings and preferences</p>
            </header>

            <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
                <div className="lg:col-span-2 space-y-6">
                    <div className="card">
                        <h3 className="text-lg font-semibold mb-6 flex items-center gap-2">
                            <UserIcon className="w-5 h-5 text-primary" />
                            Personal Information
                        </h3>

                        <div className="space-y-4">
                            <div>
                                <label className="block text-xs font-medium text-text-muted mb-1 uppercase tracking-wider">
                                    Full Name
                                </label>
                                <div className="p-3 bg-white/5 border border-primary/10 rounded-lg text-white">
                                    {user?.name}
                                </div>
                            </div>

                            <div>
                                <label className="block text-xs font-medium text-text-muted mb-1 uppercase tracking-wider">
                                    Email Address
                                </label>
                                <div className="flex items-center gap-2 p-3 bg-white/5 border border-primary/10 rounded-lg text-white">
                                    <EnvelopeIcon className="w-4 h-4 text-text-muted" />
                                    {user?.email}
                                </div>
                            </div>
                        </div>
                    </div>

                    <div className="card">
                        <h3 className="text-lg font-semibold mb-6 flex items-center gap-2">
                            <KeyIcon className="w-5 h-5 text-secondary" />
                            Security
                        </h3>
                        <p className="text-sm text-text-muted mb-4">
                            Password management and two-factor authentication will be available in a future update.
                        </p>
                        <button disabled className="btn-primary opacity-50 cursor-not-allowed">
                            Change Password
                        </button>
                    </div>
                </div>

                <div className="space-y-6">
                    <div className="card text-center py-10">
                        <div className="w-24 h-24 bg-gradient-to-br from-primary to-accent rounded-full flex items-center justify-center text-white text-4xl font-bold mx-auto mb-4 border-2 border-primary/20 shadow-xl">
                            {user?.name?.charAt(0).toUpperCase()}
                        </div>
                        <h4 className="text-xl font-bold">{user?.name}</h4>
                        <p className="text-sm text-text-muted">{user?.email}</p>
                        <div className="mt-6 inline-flex items-center gap-2 px-3 py-1 bg-primary/10 text-primary rounded-full text-xs font-medium">
                            Premium Member
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
}
