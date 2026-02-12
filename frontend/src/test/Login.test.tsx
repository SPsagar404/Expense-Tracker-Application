import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import Login from '../pages/Login';

// Mock AuthContext
vi.mock('../context/AuthContext', () => ({
    useAuth: () => ({
        login: vi.fn(),
        isAuthenticated: false,
        loading: false,
    }),
}));

describe('Login', () => {
    it('renders login form', () => {
        render(
            <BrowserRouter>
                <Login />
            </BrowserRouter>
        );

        expect(screen.getByText('Welcome Back')).toBeInTheDocument();
        expect(screen.getByPlaceholderText('you@example.com')).toBeInTheDocument();
        expect(screen.getByPlaceholderText('••••••••')).toBeInTheDocument();
        expect(screen.getByText('Sign In')).toBeInTheDocument();
    });

    it('renders demo credentials', () => {
        render(
            <BrowserRouter>
                <Login />
            </BrowserRouter>
        );

        expect(screen.getByText('Demo Account')).toBeInTheDocument();
        expect(screen.getByText('Email: demo@expensemanager.com')).toBeInTheDocument();
    });

    it('renders signup link', () => {
        render(
            <BrowserRouter>
                <Login />
            </BrowserRouter>
        );

        expect(screen.getByText('Sign up')).toBeInTheDocument();
    });
});
