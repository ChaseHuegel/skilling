<template>
    <div class="config-page">
        <div class="page-header">
            <h1>Config</h1>
            <div class="header-actions">
                <button v-if="staging.hasFileChanges('config.yml')" class="btn btn-secondary" @click="cancelConfig">Cancel</button>
                <button v-if="staging.hasFileChanges('config.yml')" class="btn btn-danger" @click="showResetDialog = true">Reset</button>
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

        <StickyActionBanner :visible="isDirty" :saving="saving" @save="saveConfig" @cancel="fetchConfig" />

        <!-- Leave confirm dialog -->
        <div v-if="showLeaveDialog" class="modal-overlay" @click.self="showLeaveDialog = false">
            <div class="modal">
                <h3>Unsaved changes</h3>
                <p>Would you like to save your changes before leaving?</p>
                <div class="modal-actions">
                    <button class="btn btn-secondary" @click="showLeaveDialog = false">Cancel</button>
                    <button class="btn btn-danger" @click="leaveDiscard">Discard</button>
                    <button class="btn btn-primary" @click="leaveSave">Save & Leave</button>
                </div>
            </div>
        </div>

        <!-- Web disable confirm dialog -->
        <div v-if="showWebDisableDialog" class="modal-overlay" @click.self="showWebDisableDialog = false">
            <div class="modal">
                <h3>Disable web interface?</h3>
                <p>This page will no longer be accessible once saved. To re-enable, you must edit <code>config.yml</code> directly on the server.</p>
                <div class="modal-actions">
                    <button class="btn btn-secondary" @click="showWebDisableDialog = false">Keep Enabled</button>
                    <button class="btn btn-danger" @click="confirmWebDisable">Disable</button>
                </div>
            </div>
        </div>

        <!-- Reset confirm dialog -->
        <div v-if="showResetDialog" class="modal-overlay" @click.self="showResetDialog = false">
            <div class="modal">
                <h3>Discard config changes?</h3>
                <p>Any unsaved changes to your configuration will be lost.</p>
                <div class="modal-actions">
                    <button class="btn btn-secondary" @click="showResetDialog = false">Keep Editing</button>
                    <button class="btn btn-danger" @click="confirmReset">Discard</button>
                </div>
            </div>
        </div>
    </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue';
import { onBeforeRouteLeave } from 'vue-router';
import { api } from '../api/client';
import { useStagingStore } from '../stores/staging';
import ConfigSection from '../components/config/ConfigSection.vue';
import AppInput from '../components/common/AppInput.vue';
import StickyActionBanner from '../components/common/StickyActionBanner.vue';

const staging = useStagingStore();
const loading = ref(true);
const saving = ref(false);
const error = ref<string | null>(null);
const showResetDialog = ref(false);
const showWebDisableDialog = ref(false);
const showLeaveDialog = ref(false);
let pendingNavigation: (() => void) | null = null;
const cleanConfig = ref('');

const config = reactive({
    database: { poolSize: 10, walMode: true },
    bossbar: { maxActive: 2, fadeTicks: 40 },
    debouncer: { intervalMs: 500 },
    debugLogging: false,
    titles: { stayDuration: 5000 },
    globalXpModifier: 1.0,
    web: { enabled: false, port: 8082, username: 'admin', password: 'skilling' },
});

const isDirty = computed(() => JSON.stringify(config) !== cleanConfig.value);

onBeforeRouteLeave((to, from, next) => {
    if (!isDirty.value) {
        next();
        return;
    }
    showLeaveDialog.value = true;
    pendingNavigation = () => next();
});

onMounted(fetchConfig);

async function cancelConfig() {
    await staging.discard();
    window.location.reload();
}

async function leaveSave() {
    showLeaveDialog.value = false;
    saving.value = true;
    try {
        await api.config.update({ ...config });
    } catch { /* navigate anyway */ }
    saving.value = false;
    pendingNavigation?.();
    pendingNavigation = null;
}

function leaveDiscard() {
    showLeaveDialog.value = false;
    pendingNavigation?.();
    pendingNavigation = null;
}

function confirmReset() {
    showResetDialog.value = false;
    fetchConfig();
}

async function fetchConfig() {
    loading.value = true;
    error.value = null;
    try {
        const data = await api.config.get();
        Object.assign(config, data);
        cleanConfig.value = JSON.stringify(config);
    } catch (e: any) {
        error.value = e.message || 'Failed to load config';
    } finally {
        loading.value = false;
    }
}

async function saveConfig() {
    if (!config.web.enabled) {
        showWebDisableDialog.value = true;
        return;
    }
    await doSaveConfig();
}

function confirmWebDisable() {
    showWebDisableDialog.value = false;
    doSaveConfig();
}

async function doSaveConfig() {
    saving.value = true;
    error.value = null;
    try {
        await api.config.update({ ...config });
        window.location.reload();
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
    padding-bottom: 4rem;
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
.modal-overlay {
    position: fixed;
    inset: 0;
    background: rgba(0, 0, 0, 0.5);
    display: flex;
    align-items: center;
    justify-content: center;
    z-index: 1000;
}
.modal {
    background: var(--p-content-background);
    border: 1px solid var(--p-content-border-color);
    border-radius: 8px;
    padding: 1.5rem;
    max-width: 400px;
    width: 90%;
    box-shadow: 0 4px 24px rgba(0, 0, 0, 0.15);
}
.modal h3 {
    margin: 0 0 0.5rem;
    font-size: 1.05rem;
    color: var(--p-text-color);
}
.modal p {
    margin: 0 0 1.25rem;
    color: var(--p-text-muted-color, #888);
    font-size: 0.875rem;
    line-height: 1.4;
}
.modal-actions {
    display: flex;
    justify-content: flex-end;
    gap: 0.5rem;
}
</style>
