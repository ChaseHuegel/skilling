import { useAuthStore } from '../stores/auth';

function getCredentials(): string | null {
    return sessionStorage.getItem('skilling_credentials');
}

export function setCredentials(user: string, pass: string): void {
    sessionStorage.setItem('skilling_credentials', btoa(`${user}:${pass}`));
}

export function clearCredentials(): void {
    sessionStorage.removeItem('skilling_credentials');
}

export async function apiFetch<T>(path: string, options?: RequestInit): Promise<T> {
    const creds = getCredentials();
    const headers: Record<string, string> = {
        'Content-Type': 'application/json',
        ...(creds ? { Authorization: `Basic ${creds}` } : {}),
        ...(options?.headers as Record<string, string> || {}),
    };
    const res = await fetch(path, { ...options, headers });

    if (res.status === 401) {
        // Session expired: log the Pinia store out (clears user + credentials)
        // so the router guard, topbar, and pending-changes banner all react,
        // then force the redirect to the login page.
        useAuthStore().logout();
        window.location.hash = '#/login';
        throw new Error('Session expired');
    }

    if (!res.ok) {
        const body = await res.json().catch(() => ({}));
        throw new Error(body.message || `HTTP ${res.status}`);
    }

    return res.json();
}

// API functions
export const api = {
    auth: {
        check: (user: string, pass: string) => {
            const encoded = btoa(`${user}:${pass}`);
            return fetch('/api/auth/check', {
                headers: { Authorization: `Basic ${encoded}` },
            });
        },
    },
    skills: {
        list: () => apiFetch<any[]>('/api/skills'),
        get: (id: string) => apiFetch<any>(`/api/skills/${id}`),
        create: (data: any) => apiFetch<any>('/api/skills', { method: 'POST', body: JSON.stringify(data) }),
        update: (id: string, data: any) => apiFetch<any>(`/api/skills/${id}`, { method: 'PUT', body: JSON.stringify(data) }),
        delete: (id: string) => apiFetch<any>(`/api/skills/${id}`, { method: 'DELETE' }),
    },
    tags: {
        get: () => apiFetch<{ tags: Record<string, string[]>; entityTags: Record<string, string[]> }>('/api/tags'),
        update: (tags: Record<string, string[]>, entityTags: Record<string, string[]>) => apiFetch<any>('/api/tags', { method: 'PUT', body: JSON.stringify({ tags, entityTags }) }),
    },
    config: {
        get: () => apiFetch<any>('/api/config'),
        update: (data: any) => apiFetch<any>('/api/config', { method: 'PUT', body: JSON.stringify(data) }),
    },
    staging: {
        status: () => apiFetch<{ hasPendingChanges: boolean; fileCount: number; files: string[] }>('/api/staging/status'),
        clear: () => apiFetch<any>('/api/staging', { method: 'DELETE' }),
    },
    reload: () => apiFetch<{ success: boolean; message: string; errors: string[] }>('/api/reload', { method: 'POST', body: JSON.stringify({ confirm: true }) }),
    guiLayout: {
        get: () => apiFetch<any>('/api/gui-layout'),
        update: (data: any) => apiFetch<any>('/api/gui-layout', { method: 'PUT', body: JSON.stringify(data) }),
    },
    mechanics: {
        list: () => apiFetch<{ mechanics: Record<string, string[]> }>('/api/mechanics'),
    },
    triggers: {
        list: () => apiFetch<{ triggers: string[] }>('/api/triggers'),
    },
    stateFilters: {
        list: () => apiFetch<{ stateFilters: string[] }>('/api/state-filters'),
    },
    abilities: {
        list: () => apiFetch<{ abilities: { id: string; displayName: string; trigger: string; unlockLevel: number }[] }>('/api/abilities'),
    },
};
