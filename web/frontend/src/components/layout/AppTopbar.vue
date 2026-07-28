<template>
    <div class="topbar">
        <div class="topbar-left">
            <router-link to="/" class="topbar-brand">
                <svg class="brand-icon" viewBox="0 0 24 24" width="22" height="22" fill="none">
                    <path d="M12 3L3 14h5v7h8v-7h5L12 3z" stroke="currentColor" stroke-width="1.5" stroke-linejoin="round" />
                </svg>
                Skilling
            </router-link>
            <nav class="topbar-nav">
                <div class="nav-dropdown">
                    <router-link to="/" class="nav-link" exact-active-class="router-link-exact-active">Skills</router-link>
                    <div class="dropdown-menu">
                        <div v-if="skills.length === 0" class="dropdown-empty">No skills loaded</div>
                        <router-link
                            v-for="s in skills"
                            :key="s.id"
                            :to="'/skills/' + s.id"
                            class="dropdown-item"
                        >
                            <MinecraftIcon :material="s.icon || 'minecraft:barrier'" :size="28" />
                            {{ s.displayName || s.id }}
                        </router-link>
                    </div>
                </div>
                <router-link to="/abilities" class="nav-link" active-class="router-link-active">Abilities</router-link>
                <router-link to="/tags" class="nav-link" active-class="router-link-active">Tags</router-link>
                <router-link to="/config" class="nav-link" active-class="router-link-active">Config</router-link>
            </nav>
        </div>
        <div class="topbar-right">
            <button class="btn btn-icon" @click="$emit('toggleDark')" :title="darkMode ? 'Switch to light mode' : 'Switch to dark mode'">
                <svg v-if="darkMode" viewBox="0 0 24 24" width="18" height="18" fill="none">
                    <circle cx="12" cy="12" r="4.5" stroke="currentColor" stroke-width="1.5" />
                    <path d="M12 3v2m0 14v2m9-9h-2M5 12H3m15.07-6.07l-1.41 1.41M7.34 16.66l-1.41 1.41m12.73 0l-1.41-1.41M7.34 7.34L5.93 5.93" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" />
                </svg>
                <svg v-else viewBox="0 0 24 24" width="18" height="18" fill="none">
                    <path d="M21 12.79A9 9 0 1111.21 3 7 7 0 0021 12.79z" stroke="currentColor" stroke-width="1.5" stroke-linejoin="round" />
                </svg>
            </button>
            <div class="user-area">
                <svg class="user-icon" viewBox="0 0 24 24" width="16" height="16" fill="none">
                    <circle cx="12" cy="8" r="4" stroke="currentColor" stroke-width="1.5" />
                    <path d="M4 21v-1a6 6 0 016-6h4a6 6 0 016 6v1" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" />
                </svg>
                <span class="user-name">{{ authStore.user }}</span>
            </div>
            <button class="btn btn-ghost" @click="logout">Logout</button>
        </div>
    </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '../../stores/auth';
import { api } from '../../api/client';
import MinecraftIcon from '../common/MinecraftIcon.vue';

defineEmits<{ toggleDark: [] }>();

const skills = ref<any[]>([]);

onMounted(async () => {
    try {
        skills.value = await api.skills.list();
    } catch { /* ignore */ }
});

const authStore = useAuthStore();
const router = useRouter();
const darkMode = computed(() => document.documentElement.classList.contains('app-dark'));

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
    padding: 0 1.5rem;
    height: 52px;
    background: var(--p-content-background);
    border-bottom: 1px solid var(--p-content-border-color);
}

.topbar-left {
    display: flex;
    align-items: center;
    gap: 2rem;
}

.topbar-brand {
    display: flex;
    align-items: center;
    gap: 0.45rem;
    font-size: 1.1rem;
    font-weight: 700;
    color: var(--p-primary-color);
    text-decoration: none;
}

.brand-icon {
    color: var(--p-primary-color);
}

.topbar-nav {
    display: flex;
    gap: 0.25rem;
}
.nav-dropdown {
    position: relative;
    display: flex;
    align-items: center;
}
.nav-dropdown:hover .dropdown-menu {
    display: block;
}
.dropdown-menu {
    display: none;
    position: absolute;
    top: 100%;
    left: 0;
    z-index: 200;
    min-width: 200px;
    margin-top: 4px;
    border: 1px solid var(--p-content-border-color, #ddd);
    border-radius: 6px;
    background: var(--p-content-background, #fff);
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
    overflow: hidden;
}
.dropdown-empty {
    padding: 0.75rem;
    font-size: 0.8rem;
    color: var(--p-form-field-placeholder-color, #888);
    text-align: center;
}
.dropdown-item {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    padding: 0.5rem 0.75rem;
    text-decoration: none;
    color: var(--p-text-color, #000);
    font-size: 0.85rem;
    transition: background 0.1s;
}
.dropdown-item:hover {
    background: var(--p-content-hover-background, #f0f0f0);
}

.nav-link {
    color: var(--p-form-field-placeholder-color);
    text-decoration: none;
    padding: 0.35rem 0.65rem;
    border-radius: 5px;
    font-size: 0.875rem;
    transition: background 0.15s, color 0.15s;
}

.nav-link:hover {
    color: var(--p-text-color);
    background: var(--p-content-hover-background);
}

.nav-link.router-link-active,
.nav-link.router-link-exact-active {
    color: var(--p-primary-color);
    background: color-mix(in srgb, var(--p-primary-color) 15%, transparent);
    font-weight: 600;
}

.app-dark .nav-link.router-link-active,
.app-dark .nav-link.router-link-exact-active {
    background: color-mix(in srgb, var(--p-primary-color) 25%, transparent);
}

.topbar-right {
    display: flex;
    align-items: center;
    gap: 0.75rem;
}

.theme-toggle {
    background: none;
    border: none;
    cursor: pointer;
    padding: 0.35rem;
    border-radius: 5px;
    color: var(--p-form-field-placeholder-color);
    display: flex;
    align-items: center;
    transition: background 0.15s, color 0.15s;
}

.theme-toggle:hover {
    background: var(--p-content-hover-background);
    color: var(--p-text-color);
}

.user-area {
    display: flex;
    align-items: center;
    gap: 0.35rem;
    color: var(--p-form-field-placeholder-color);
}

.user-icon {
    flex-shrink: 0;
    opacity: 0.6;
}

.user-name {
    font-size: 0.8rem;
    color: var(--p-form-field-placeholder-color);
}

.theme-toggle {
    /* btn-icon styles handled by global .btn.btn-icon */
}
</style>
