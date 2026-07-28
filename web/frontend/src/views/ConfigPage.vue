<template>
    <div class="config-page">
        <div class="page-header">
            <h1>Configuration</h1>
            <div class="header-actions">
                <button class="btn btn-secondary" @click="fetchConfig">Reset</button>
                <button class="btn btn-primary" :disabled="saving" @click="saveConfig">
                    {{ saving ? 'Saving...' : 'Save Changes' }}
                </button>
            </div>
        </div>

        <div v-if="error" class="error-banner">{{ error }}</div>
        <div v-if="loading" class="loading">Loading config...</div>

        <div v-else class="config-sections">
            <ConfigSection title="Database" description="HikariCP connection pool settings">
                <AppInput v-model.number="config.database.poolSize" type="number" label="Pool Size" :min="1" :max="100" />
                <AppInput v-model="config.database.walMode" type="checkbox" label="WAL Mode" />
                <span class="field-note">Write-Ahead Logging; requires restart to change</span>
            </ConfigSection>

            <ConfigSection title="Boss Bar" description="XP progress bar display settings">
                <AppInput v-model.number="config.bossbar.maxActive" type="number" label="Max Active" :min="1" :max="10" />
                <AppInput v-model.number="config.bossbar.fadeTicks" type="number" label="Fade Ticks" :min="0" :max="200" />
            </ConfigSection>

            <ConfigSection title="Debouncer" description="Minimum interval between repeated feedback messages">
                <AppInput v-model.number="config.debouncer.intervalMs" type="number" label="Interval (ms)" :min="100" :max="5000" />
            </ConfigSection>

            <ConfigSection title="Debug" description="Verbose console logging">
                <AppInput v-model="config.debugLogging" type="checkbox" label="Debug Logging" />
                <span class="field-note">WARNING: significant log output</span>
            </ConfigSection>

            <ConfigSection title="Titles" description="Level-up title display settings">
                <AppInput v-model.number="config.titles.stayDuration" type="number" label="Stay Duration (ms)" :min="1000" :max="30000" />
            </ConfigSection>

            <ConfigSection title="XP" description="Global XP modifier">
                <AppInput v-model.number="config.globalXpModifier" type="number" label="Global XP Modifier" :min="0.1" :max="100" :step="0.1" />
            </ConfigSection>

            <ConfigSection title="Web Server" description="Built-in administration interface">
                <AppInput v-model="config.web.enabled" type="checkbox" label="Enabled" />
                <span class="field-note">Requires server restart to take effect</span>
                <AppInput v-model.number="config.web.port" type="number" label="Port" :min="1025" :max="65535" />
                <AppInput v-model="config.web.username" type="text" label="Username" />
                <AppInput v-model="config.web.password" type="password" label="Password" />
            </ConfigSection>
        </div>
    </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { api } from '../api/client';
import ConfigSection from '../components/config/ConfigSection.vue';
import AppInput from '../components/common/AppInput.vue';

const loading = ref(true);
const saving = ref(false);
const error = ref<string | null>(null);

const config = reactive({
    database: { poolSize: 10, walMode: true },
    bossbar: { maxActive: 2, fadeTicks: 40 },
    debouncer: { intervalMs: 500 },
    debugLogging: false,
    titles: { stayDuration: 5000 },
    globalXpModifier: 1.0,
    web: { enabled: false, port: 8082, username: 'admin', password: 'skilling' },
});

onMounted(fetchConfig);

async function fetchConfig() {
    loading.value = true;
    error.value = null;
    try {
        const data = await api.config.get();
        Object.assign(config, data);
    } catch (e: any) {
        error.value = e.message || 'Failed to load config';
    } finally {
        loading.value = false;
    }
}

async function saveConfig() {
    saving.value = true;
    error.value = null;
    try {
        await api.config.update({ ...config });
    } catch (e: any) {
        error.value = e.message || 'Failed to save config';
    } finally {
        saving.value = false;
    }
}
</script>

<style scoped>
.config-page {
    max-width: 700px;
    margin: 0 auto;
    padding: 1.5rem;
}
.page-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 1.5rem;
}
.page-header h1 {
    margin: 0;
    font-size: 1.5rem;
}
.header-actions {
    display: flex;
    gap: 0.5rem;
}
.btn {
    padding: 0.5rem 1rem;
    border: none;
    border-radius: 4px;
    cursor: pointer;
    font-size: 0.875rem;
}
.btn:disabled {
    opacity: 0.6;
    cursor: not-allowed;
}
.btn-primary {
    background: var(--p-primary-color, #3b82f6);
    color: white;
}
.btn-secondary {
    background: var(--p-surface-border, #e5e7eb);
    color: var(--p-text-color, #333);
}
.error-banner {
    background: var(--p-red-100, #fee2e2);
    color: var(--p-red-800, #991b1b);
    padding: 0.75rem;
    border-radius: 4px;
    margin-bottom: 1rem;
    font-size: 0.875rem;
}
.loading {
    text-align: center;
    padding: 2rem;
    color: var(--p-text-muted-color, #888);
}
.config-sections {
    display: flex;
    flex-direction: column;
}
.field-note {
    font-size: 0.8rem;
    color: var(--p-text-muted-color, #888);
    font-style: italic;
}
</style>
