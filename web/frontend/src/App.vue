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
</style>
