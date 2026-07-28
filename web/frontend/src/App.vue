<template>
    <div class="app-container">
        <AppTopbar v-if="authStore.isAuthenticated" @toggle-dark="toggleDark" />
        <router-view />
    </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue';
import { useAuthStore } from './stores/auth';
import AppTopbar from './components/layout/AppTopbar.vue';

const authStore = useAuthStore();
authStore.checkSession();

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
    background: var(--p-surface-ground);
    color: var(--p-text-color);
}
.app-container {
    min-height: 100vh;
}

/* Dark mode border contrast fix.
   PrimeVue's --p-surface-border resolves to a near-invisible
   color in the dark theme. These overrides add a subtle inner
   glow/shadow that works regardless of the element's current
   border color, keeping cards, inputs, and panels visually
   distinct without clobbering color accents (e.g. skill card
   top borders). */
.app-dark .skill-card,
.app-dark .section,
.app-dark .config-section,
.app-dark .filter-entry,
.app-dark .ability-card {
    box-shadow: inset 0 0 0 1px color-mix(in srgb, var(--p-surface-border) 50%, rgb(255 255 255 / 0.15));
}

.app-dark .app-input-field,
.app-dark .evaluator-type-select,
.app-dark .field-input,
.app-dark .filter-input,
.app-dark .app-input .checkbox-toggle {
    border-color: color-mix(in srgb, var(--p-surface-border) 40%, rgb(255 255 255 / 0.25));
}
</style>
