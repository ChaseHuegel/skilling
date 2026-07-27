<template>
    <div class="topbar">
        <div class="topbar-left">
            <router-link to="/" class="topbar-title">Skilling</router-link>
            <nav class="topbar-nav">
                <router-link to="/" class="nav-link">Dashboard</router-link>
                <router-link to="/tags" class="nav-link">Tags</router-link>
                <router-link to="/config" class="nav-link">Config</router-link>
            </nav>
        </div>
        <div class="topbar-right">
            <button class="theme-toggle" @click="$emit('toggleDark')">
                {{ darkIcon }}
            </button>
            <span class="user-name">{{ authStore.user }}</span>
            <button class="logout-btn" @click="logout">Logout</button>
        </div>
    </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '../../stores/auth';

defineEmits<{ toggleDark: [] }>();

const authStore = useAuthStore();
const router = useRouter();
const darkIcon = computed(() => document.documentElement.classList.contains('app-dark') ? '☀️' : '🌙');

function logout() {
    authStore.logout();
    router.push('/login');
}
</script>

<style scoped>
.topbar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 0.75rem 1.5rem;
    background: var(--p-surface-section);
    border-bottom: 1px solid var(--p-surface-border);
}
.topbar-left {
    display: flex;
    align-items: center;
    gap: 2rem;
}
.topbar-title {
    font-size: 1.25rem;
    font-weight: 700;
    color: var(--p-primary-color);
    text-decoration: none;
}
.topbar-nav {
    display: flex;
    gap: 1rem;
}
.nav-link {
    color: var(--p-text-muted-color);
    text-decoration: none;
    padding: 0.25rem 0.5rem;
    border-radius: 4px;
}
.nav-link:hover {
    color: var(--p-text-color);
    background: var(--p-surface-hover);
}
.topbar-right {
    display: flex;
    align-items: center;
    gap: 1rem;
}
.user-name {
    color: var(--p-text-muted-color);
    font-size: 0.875rem;
}
.theme-toggle, .logout-btn {
    background: none;
    border: 1px solid var(--p-surface-border);
    padding: 0.375rem 0.75rem;
    border-radius: 4px;
    cursor: pointer;
    color: var(--p-text-color);
}
.logout-btn:hover {
    background: var(--p-surface-hover);
}
</style>
