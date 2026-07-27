<template>
    <div class="login-wrapper">
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

            <button class="login-btn" :disabled="authStore.loading" @click="submit">
                {{ authStore.loading ? 'Signing in...' : 'Sign In' }}
            </button>
        </div>
    </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '../stores/auth';

const router = useRouter();
const authStore = useAuthStore();
const username = ref('admin');
const password = ref('');

async function submit() {
    const ok = await authStore.login(username.value, password.value);
    if (ok) router.push('/');
}
</script>

<style scoped>
.login-wrapper {
    display: flex;
    align-items: center;
    justify-content: center;
    min-height: 100vh;
    background: var(--p-surface-ground);
}
.login-card {
    background: var(--p-surface-section);
    border: 1px solid var(--p-surface-border);
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
    color: var(--p-text-muted-color);
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
    border: 1px solid var(--p-surface-border);
    border-radius: 4px;
    background: var(--p-surface-input);
    color: var(--p-text-color);
    box-sizing: border-box;
}
.error-msg {
    color: var(--p-red-600);
    font-size: 0.875rem;
    margin: 0.5rem 0;
}
.login-btn {
    width: 100%;
    padding: 0.625rem;
    background: var(--p-primary-color);
    color: white;
    border: none;
    border-radius: 4px;
    cursor: pointer;
    font-size: 1rem;
}
.login-btn:disabled {
    opacity: 0.6;
    cursor: not-allowed;
}
</style>
