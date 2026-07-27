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
                <div class="field-row">
                    <label>Pool Size</label>
                    <input type="number" v-model.number="config.database.poolSize" min="1" max="100" class="input" />
                </div>
                <div class="field-row">
                    <label>WAL Mode</label>
                    <input type="checkbox" v-model="config.database.walMode" />
                    <span class="field-note">Write-Ahead Logging; requires restart to change</span>
                </div>
            </ConfigSection>

            <ConfigSection title="Boss Bar" description="XP progress bar display settings">
                <div class="field-row">
                    <label>Max Active</label>
                    <input type="number" v-model.number="config.bossbar.maxActive" min="1" max="10" class="input" />
                </div>
                <div class="field-row">
                    <label>Fade Ticks</label>
                    <input type="number" v-model.number="config.bossbar.fadeTicks" min="0" max="200" class="input" />
                </div>
            </ConfigSection>

            <ConfigSection title="Debouncer" description="Minimum interval between repeated feedback messages">
                <div class="field-row">
                    <label>Interval (ms)</label>
                    <input type="number" v-model.number="config.debouncer.intervalMs" min="100" max="5000" class="input" />
                </div>
            </ConfigSection>

            <ConfigSection title="Debug" description="Verbose console logging">
                <div class="field-row">
                    <label>Debug Logging</label>
                    <input type="checkbox" v-model="config.debugLogging" />
                    <span class="field-note">WARNING: significant log output</span>
                </div>
            </ConfigSection>

            <ConfigSection title="Titles" description="Level-up title display settings">
                <div class="field-row">
                    <label>Stay Duration (ms)</label>
                    <input type="number" v-model.number="config.titles.stayDuration" min="1000" max="30000" class="input" />
                </div>
            </ConfigSection>

            <ConfigSection title="XP" description="Global XP modifier">
                <div class="field-row">
                    <label>Global XP Modifier</label>
                    <input type="number" v-model.number="config.globalXpModifier" min="0.1" max="100" step="0.1" class="input" />
                </div>
            </ConfigSection>

            <ConfigSection title="Web Server" description="Built-in administration interface">
                <div class="field-row">
                    <label>Enabled</label>
                    <input type="checkbox" v-model="config.web.enabled" />
                    <span class="field-note">Requires server restart to take effect</span>
                </div>
                <div class="field-row">
                    <label>Port</label>
                    <input type="number" v-model.number="config.web.port" min="1025" max="65535" class="input" />
                </div>
                <div class="field-row">
                    <label>Username</label>
                    <input type="text" v-model="config.web.username" class="input" />
                </div>
                <div class="field-row">
                    <label>Password</label>
                    <input type="password" v-model="config.web.password" class="input" />
                </div>
            </ConfigSection>
        </div>
    </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { api } from '../api/client';
import ConfigSection from '../components/config/ConfigSection.vue';

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
.field-row {
    display: flex;
    align-items: center;
    gap: 0.75rem;
}
.field-row label {
    min-width: 120px;
    font-size: 0.875rem;
}
.input {
    padding: 0.375rem 0.5rem;
    border: 1px solid var(--p-surface-border, #ddd);
    border-radius: 4px;
    background: var(--p-surface-input, #fff);
    color: var(--p-text-color, #333);
    width: 120px;
}
.field-note {
    font-size: 0.8rem;
    color: var(--p-text-muted-color, #888);
    font-style: italic;
}
</style>
