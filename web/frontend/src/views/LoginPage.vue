<template>
    <div class="login-wrapper">
        <button class="theme-toggle" @click="toggleDark" :title="darkMode ? 'Switch to light mode' : 'Switch to dark mode'">
            <svg v-if="darkMode" viewBox="0 0 24 24" width="18" height="18" fill="none">
                <circle cx="12" cy="12" r="4.5" stroke="currentColor" stroke-width="1.5" />
                <path d="M12 3v2m0 14v2m9-9h-2M5 12H3m15.07-6.07l-1.41 1.41M7.34 16.66l-1.41 1.41m12.73 0l-1.41-1.41M7.34 7.34L5.93 5.93" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" />
            </svg>
            <svg v-else viewBox="0 0 24 24" width="18" height="18" fill="none">
                <path d="M21 12.79A9 9 0 1111.21 3 7 7 0 0021 12.79z" stroke="currentColor" stroke-width="1.5" stroke-linejoin="round" />
            </svg>
        </button>
        <div class="login-card">
            <h1 class="login-title">Skilling</h1>
            <p class="login-subtitle">Web Administration Interface</p>

            <div class="field">
                <label for="username">Username</label>
                <input id="username" v-model="username" type="text" class="input" placeholder="admin" @keyup.enter="submit" />
            </div>
            <div class="field">
                <label for="password">Password</label>
                <input id="password" v-model="password" type="password" class="input" placeholder="••••••••" @keyup.enter="submit" />
            </div>

            <p v-if="authStore.error" class="error-msg">{{ authStore.error }}</p>

            <button class="btn btn-primary" :disabled="authStore.loading" @click="submit">
                {{ authStore.loading ? 'Signing in...' : 'Sign In' }}
            </button>
        </div>
    </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useAuthStore } from '../stores/auth';

const darkMode = computed(() => document.documentElement.classList.contains('app-dark'));

function toggleDark() {
    const next = !darkMode.value;
    document.documentElement.classList.toggle('app-dark', next);
    localStorage.setItem('skilling_dark_mode', String(next));
}

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();
const username = ref('admin');
const password = ref('');

async function submit() {
    const ok = await authStore.login(username.value, password.value);
    if (ok) {
        // The route guard bounced an authenticated-required navigation here; land
        // back on the originally requested route after login rather than always
        // returning to the dashboard.
        const intended = route.redirectedFrom?.fullPath || '/';
        router.push(intended);
    }
}
</script>

<style scoped>
.login-wrapper {
    display: flex;
    align-items: center;
    justify-content: center;
    min-height: 100vh;
    background: var(--p-surface-ground);
    position: relative;
}
.login-card {
    background: var(--p-content-background);
    border: 1px solid var(--p-content-border-color);
    border-radius: 8px;
    padding: 2rem;
    width: 100%;
    max-width: 360px;
}
.login-title {
    margin: 0 0 0.25rem;
    font-size: 1.5rem;
    text-align: center;
}
.login-subtitle {
    margin: 0 0 1.5rem;
    text-align: center;
    color: var(--p-form-field-placeholder-color);
    font-size: 0.875rem;
}
.field {
    margin-bottom: 1rem;
}
.field label {
    display: block;
    margin-bottom: 0.375rem;
    font-size: 0.875rem;
}
.input {
    width: 100%;
    padding: 0.5rem;
    border: 1px solid var(--p-content-border-color);
    border-radius: 4px;
    background: var(--p-form-field-background);
    color: var(--p-text-color);
    box-sizing: border-box;
}
.error-msg {
    color: var(--p-red-600);
    font-size: 0.875rem;
    margin: 0.5rem 0;
}
.theme-toggle {
    position: absolute;
    top: 0.75rem;
    right: 0.75rem;
    background: none;
    border: none;
    cursor: pointer;
    padding: 0.35rem;
    border-radius: 5px;
    color: var(--p-form-field-placeholder-color, #888);
    display: flex;
    align-items: center;
    transition: background 0.15s, color 0.15s;
}
.theme-toggle:hover {
    background: var(--p-content-hover-background, #eee);
    color: var(--p-text-color, #000);
}

</style>
