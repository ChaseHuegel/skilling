import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import { api, setCredentials, clearCredentials } from '../api/client';

export const useAuthStore = defineStore('auth', () => {
    const user = ref<string | null>(null);
    const loading = ref(false);
    const error = ref<string | null>(null);

    const isAuthenticated = computed(() => user.value !== null);

    async function login(username: string, password: string): Promise<boolean> {
        loading.value = true;
        error.value = null;
        try {
            const res = await api.auth.check(username, password);
            if (res.ok) {
                const data = await res.json();
                user.value = data.user;
                setCredentials(username, password);
                return true;
            } else {
                error.value = 'Invalid credentials';
                return false;
            }
        } catch (e: any) {
            error.value = e.message || 'Connection failed';
            return false;
        } finally {
            loading.value = false;
        }
    }

    function logout() {
        user.value = null;
        clearCredentials();
    }

    function checkSession() {
        const creds = sessionStorage.getItem('skilling_credentials');
        if (creds) {
            const decoded = atob(creds);
            const [u] = decoded.split(':');
            user.value = u || null;
        }
    }

    return { user, loading, error, isAuthenticated, login, logout, checkSession };
});
