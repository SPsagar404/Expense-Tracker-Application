import axios from 'axios';

const API_BASE_URL = '/api/v1';

const client = axios.create({
    baseURL: API_BASE_URL,
    headers: { 'Content-Type': 'application/json' },
});

// Request interceptor: attach JWT
client.interceptors.request.use((config) => {
    const token = localStorage.getItem('accessToken');
    if (token) {
        config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
});

// Response interceptor: handle 401
client.interceptors.response.use(
    (response) => response,
    (error) => {
        if (error.response?.status === 401) {
            localStorage.removeItem('accessToken');
            localStorage.removeItem('refreshToken');
            localStorage.removeItem('user');
            window.location.href = '/login';
        }
        return Promise.reject(error);
    }
);

// Auth API
export const authApi = {
    signup: (data: { name: string; email: string; password: string }) =>
        client.post('/auth/signup', data),
    login: (data: { email: string; password: string }) =>
        client.post('/auth/login', data),
    refresh: (refreshToken: string) =>
        client.post('/auth/refresh', { refreshToken }),
};

// Transaction API
export const transactionApi = {
    list: (params?: Record<string, string | number>) =>
        client.get('/transactions', { params }),
    create: (data: Record<string, unknown>) =>
        client.post('/transactions', data),
    update: (id: number, data: Record<string, unknown>) =>
        client.put(`/transactions/${id}`, data),
    delete: (id: number) =>
        client.delete(`/transactions/${id}`),
    importCsv: (file: File) => {
        const formData = new FormData();
        formData.append('file', file);
        return client.post('/transactions/import-csv', formData, {
            headers: { 'Content-Type': 'multipart/form-data' },
        });
    },
};

// Budget API
export const budgetApi = {
    list: (params?: Record<string, number>) =>
        client.get('/budgets', { params }),
    create: (data: Record<string, unknown>) =>
        client.post('/budgets', data),
    update: (id: number, data: Record<string, unknown>) =>
        client.put(`/budgets/${id}`, data),
    delete: (id: number) =>
        client.delete(`/budgets/${id}`),
};

// Report API
export const reportApi = {
    monthly: (year: number, month: number) =>
        client.get('/reports/monthly', { params: { year, month } }),
};

// Insight API
export const insightApi = {
    summary: () => client.get('/insights/summary'),
};

// Salary Allocation API
export const salaryApi = {
    savePlan: (data: Record<string, unknown>[]) =>
        client.post('/salary', data),
    addAllocation: (id: number, data: Record<string, unknown>) =>
        client.post(`/salary/${id}/allocate`, data),
    getAllocations: (year: number, month: number) =>
        client.get(`/salary/${year}/${month}`),
    getSummary: (year: number, month: number) =>
        client.get('/reports/salary-summary', { params: { year, month } }),
};

// Subscription API
export const subscriptionApi = {
    list: () => client.get('/subscriptions'),
    create: (data: Record<string, unknown>) =>
        client.post('/subscriptions', data),
    update: (id: number, data: Record<string, unknown>) =>
        client.put(`/subscriptions/${id}`, data),
    delete: (id: number) => client.delete(`/subscriptions/${id}`),
    summary: () => client.get('/subscriptions/summary'),
    wasteAnalysis: () => client.get('/subscriptions/waste-analysis'),
};

// Notification API
export interface NotificationPreferences {
    emailEnabled: boolean;
    pushEnabled: boolean;
    inAppEnabled: boolean;
    budgetAlertEnabled: boolean;
    subscriptionAlertEnabled: boolean;
    goalAlertEnabled: boolean;
    largeExpenseAlertEnabled: boolean;
}

export const notificationsApi = {
    list: (params?: Record<string, string | number>) =>
        client.get('/notifications', { params }),
    unreadCount: () =>
        client.get('/notifications/unread-count'),
    markRead: (id: number) =>
        client.put(`/notifications/${id}/mark-read`),
    createCustom: (data: { title: string; message: string; scheduledAt: string }) =>
        client.post('/notifications/custom', data),
    getPreferences: () =>
        client.get('/notification-preferences'),
    updatePreferences: (data: NotificationPreferences) =>
        client.put('/notification-preferences', data),
};

export const userApi = {
    me: () => client.get('/users/me'),
};

export default client;
