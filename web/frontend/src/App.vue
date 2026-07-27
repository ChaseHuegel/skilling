<template>
    <div :class="['app-container', darkMode ? 'app-dark' : '']">
        <AppTopbar v-if="authStore.isAuthenticated" @toggle-dark="darkMode = !darkMode" />
        <router-view />
    </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue';
import { useAuthStore } from './stores/auth';
import AppTopbar from './components/layout/AppTopbar.vue';

const authStore = useAuthStore();
authStore.checkSession();
const darkMode = ref(localStorage.getItem('skilling_dark_mode') === 'true');

watch(darkMode, (val) => {
    localStorage.setItem('skilling_dark_mode', String(val));
});
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
