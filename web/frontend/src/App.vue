<template>
    <div class="app-container">
        <AppTopbar v-if="authStore.isAuthenticated" @toggle-dark="toggleDark" />
        <PendingChangesBanner v-if="authStore.isAuthenticated" />
        <router-view />
    </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import { useAuthStore } from './stores/auth';
import { useStagingStore } from './stores/staging';
import { useRegistriesStore } from './stores/registries';
import AppTopbar from './components/layout/AppTopbar.vue';
import PendingChangesBanner from './components/layout/PendingChangesBanner.vue';

const authStore = useAuthStore();
const stagingStore = useStagingStore();
const registriesStore = useRegistriesStore();
const route = useRoute();
authStore.checkSession();
// Fetch registry catalogs (mechanics, triggers, state filters) and staging
// status only once authenticated — on first load they would 401 and never
// retry, leaving the UI on fallback suggestion lists.
watch(() => authStore.isAuthenticated, (auth) => {
    if (auth) {
        stagingStore.fetchStatus();
        registriesStore.fetch();
    }
}, { immediate: true });
watch(() => route.path, () => {
    if (authStore.isAuthenticated) stagingStore.fetchStatus();
});

const darkMode = ref(document.documentElement.classList.contains('app-dark'));

function toggleDark() {
    darkMode.value = !darkMode.value;
    document.documentElement.classList.toggle('app-dark', darkMode.value);
    localStorage.setItem('skilling_dark_mode', String(darkMode.value));
}
</script>

<style>
body {
    margin: 0;
    font-family: var(--font-family);
}
.app-container {
    min-height: 100vh;
}

/* ---- Global Button System ---- */
.btn {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    gap: 0.35rem;
    padding: 0.5rem 1rem;
    border-radius: 6px;
    font-size: 0.875rem;
    font-weight: 500;
    cursor: pointer;
    border: 1px solid transparent;
    transition: background 0.15s, border-color 0.15s, transform 0.1s, filter 0.15s;
    line-height: 1.2;
    text-decoration: none;
    white-space: nowrap;
}

.btn:active { transform: scale(0.97); }
.btn:disabled { opacity: 0.5; cursor: not-allowed; transform: none; }

/* Primary — filled accent */
.btn-primary {
    background: var(--p-primary-color);
    color: #fff;
    border-color: var(--p-primary-color);
}
.app-dark .btn-primary {
    color: #000;
}
.btn-primary:hover { filter: brightness(1.1); }

/* Secondary — bordered, adapts to theme */
.btn-secondary {
    background: transparent;
    color: var(--p-text-color);
    border-color: var(--p-content-border-color);
}
.btn-secondary:hover {
    background: var(--p-content-hover-background);
}

/* Danger — for destructive actions */
.btn-danger {
    background: transparent;
    color: var(--p-red-500, #ef4444);
    border-color: var(--p-red-500, #ef4444);
}
.btn-danger:hover {
    background: color-mix(in srgb, var(--p-red-500, #ef4444) 10%, transparent);
}

/* Ghost — minimal text link */
.btn-ghost {
    background: transparent;
    color: var(--p-text-color);
    border-color: transparent;
    padding: 0.3rem 0.5rem;
}
.btn-ghost:hover {
    background: var(--p-content-hover-background);
}

/* Reduced motion */
@media (prefers-reduced-motion: reduce) {
    *, *::before, *::after {
        animation-duration: 0.01ms !important;
        animation-iteration-count: 1 !important;
        transition-duration: 0.01ms !important;
    }
}

/* Small variant */
.btn-sm {
    padding: 0.3rem 0.6rem;
    font-size: 0.8rem;
    border-radius: 4px;
}

/* Icon-only (square) */
.btn-icon {
    padding: 0.35rem;
    border-radius: 5px;
    line-height: 1;
}
</style>
