<template>
    <div class="config-page">
        <div class="page-header">
            <div>
                <h1>Config</h1>
                <p class="page-subtitle">Adjust global plugin settings and server preferences</p>
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

            <ConfigSection title="Crop Grow" description="Natural crop growth event search radius">
                <AppInput v-model.number="config.cropGrow.searchRadius" type="number" label="Search Radius" :min="1" :max="50" />
                <span class="field-note">Radius in blocks to search for nearby players when a crop grows</span>
            </ConfigSection>

            <ConfigSection title="Skills Guide Book" description="Craftable skills reference book">
                <AppInput v-model="config.skillsGuideBook.enabled" type="checkbox" label="Enabled" />
                <span class="field-note">When disabled, the recipe and listener are not registered</span>
            </ConfigSection>

            <ConfigSection title="Branding — Skill Tooltip" description="Legacy &amp; color codes; {placeholders} are filled live">
                <TemplateListInput v-model="config.branding.skillTemplate" label="Skill Template" :rows="8" />
                <span class="field-note">Tokens: {level} {max_level} {bar} {xp_into} {xp_needed} {xp_total} {color} {lore} {abilities}</span>
                <TemplateListInput v-model="config.branding.abilitiesTemplate" label="Abilities Template" :rows="2" />
                <span class="field-note">Repeated per ability; {ability} expands to the locked/unlocked template</span>
                <AppInput v-model.number="config.branding.barTemplate.width" type="number" label="Bar Width" :min="1" :max="200" />
                <AppInput v-model="config.branding.barTemplate.filled" type="text" label="Bar Filled" />
                <AppInput v-model="config.branding.barTemplate.empty" type="text" label="Bar Empty" />
                <AppInput v-model="config.branding.barTemplate.start" type="text" label="Bar Start" />
                <AppInput v-model="config.branding.barTemplate.end" type="text" label="Bar End" />
                <span class="field-note">Per-unit strings may carry their own color codes</span>
            </ConfigSection>

            <ConfigSection title="Branding — Abilities" description="Locked vs unlocked ability lines and Active/Passive type text">
                <AppInput v-model="config.branding.abilityType.active" type="text" label="Active Type" />
                <AppInput v-model="config.branding.abilityType.passive" type="text" label="Passive Type" />
                <TemplateListInput v-model="config.branding.abilityLockedTemplate" label="Locked Template" :rows="2" />
                <span class="field-note">Tokens: {name} {level} {type} {lore}</span>
                <TemplateListInput v-model="config.branding.abilityUnlockedTemplate" label="Unlocked Template" :rows="2" />
            </ConfigSection>

            <ConfigSection title="Branding — Level Up" description="Level-up titles and chat messages">
                <AppInput v-model="config.branding.levelUp.title" type="text" label="Title" />
                <AppInput v-model="config.branding.levelUp.subtitle" type="text" label="Subtitle" />
                <AppInput v-model="config.branding.levelUp.message" type="text" label="Message" />
                <AppInput v-model="config.branding.levelUp.maxedMessage" type="text" label="Maxed Message" />
                <span class="field-note">Tokens: {name} {level} {player} {color}</span>
            </ConfigSection>

            <ConfigSection title="Branding — Ability Unlock &amp; Ready" description="Ability-unlock announcements and cooldown-ready feedback">
                <AppInput v-model="config.branding.abilityUnlock.title" type="text" label="Title" />
                <AppInput v-model="config.branding.abilityUnlock.subtitle" type="text" label="Subtitle" />
                <AppInput v-model="config.branding.abilityUnlock.message" type="text" label="Message" />
                <span class="field-note">Tokens: {name} {type}</span>
                <AppInput v-model="config.branding.abilityFeedback.readyMessage" type="text" label="Ready Message" />
                <span class="field-note">Tokens: {name} {color}</span>
            </ConfigSection>

            <ConfigSection title="Branding — GUI" description="Chest title, navigation, and skill icon names">
                <AppInput v-model="config.branding.gui.title" type="text" label="Chest Title" />
                <AppInput v-model="config.branding.gui.prevPage" type="text" label="Previous Page Arrow" />
                <AppInput v-model="config.branding.gui.nextPage" type="text" label="Next Page Arrow" />
                <AppInput v-model="config.branding.gui.pageCount" type="text" label="Page Count" />
                <AppInput v-model="config.branding.gui.skillNameUnlocked" type="text" label="Unlocked Skill Name" />
                <AppInput v-model="config.branding.gui.skillNameLocked" type="text" label="Locked Skill Name" />
                <span class="field-note">Tokens: {name} {color} {count}</span>
            </ConfigSection>

            <ConfigSection title="Branding — Guide Book" description="The craftable Skills Guide item">
                <AppInput v-model="config.branding.guideBook.name" type="text" label="Name" />
                <AppInput v-model="config.branding.guideBook.lore" type="text" label="Lore" />
            </ConfigSection>

            <ConfigSection title="Branding — Boss Bar" description="XP bar title text and default color/style">
                <AppInput v-model="config.branding.bossBar.titleFormat" type="text" label="Title Format" />
                <span class="field-note">Tokens: {color} {name} {level} {into} {needed}</span>
                <AppInput v-model="config.branding.bossBar.defaultColor" type="text" label="Default BarColor" />
                <AppInput v-model="config.branding.bossBar.defaultStyle" type="text" label="Default BarStyle" />
            </ConfigSection>

            <ConfigSection title="Branding — Commands" description="/skills command feedback">
                <AppInput v-model="config.branding.command.header" type="text" label="Header" />
                <AppInput v-model="config.branding.command.command" type="text" label="Command Line" />
                <AppInput v-model="config.branding.command.description" type="text" label="Description" />
                <AppInput v-model="config.branding.command.usage" type="text" label="Usage" />
                <AppInput v-model="config.branding.command.success" type="text" label="Success" />
                <AppInput v-model="config.branding.command.error" type="text" label="Error" />
                <AppInput v-model="config.branding.command.info" type="text" label="Info" />
                <span class="field-note">Tokens: {message} {command} {description} {usage} {title}</span>
            </ConfigSection>

            <ConfigSection title="Web Server" description="Built-in administration interface">
                <AppInput v-model="config.web.enabled" type="checkbox" label="Enabled" />
                <span class="field-note">Requires server restart to take effect</span>
                <AppInput v-model.number="config.web.port" type="number" label="Port" :min="1025" :max="65535" />
                <span class="field-note">Changing the port requires editing config.yml and restarting the server</span>
                <AppInput v-model="config.web.username" type="text" label="Username" />
                <span class="field-note">Changing the username requires editing config.yml and restarting the server</span>
                <AppInput v-model="config.web.password" type="password" label="Password" />
                <span class="field-note">Leave blank to keep the current password; changing it requires editing config.yml and restarting the server</span>
            </ConfigSection>
        </div>

        <StickyActionBanner :visible="isDirty" :saving="saving" @save="saveConfig" @cancel="confirmCancel" />

        <!-- Cancel confirm dialog -->
        <div v-if="showCancelDialog" class="modal-overlay" @click.self="showCancelDialog = false">
            <div class="modal">
                <h3>Discard changes?</h3>
                <p>Any unsaved changes to your configuration will be lost.</p>
                <div class="modal-actions">
                    <button class="btn btn-secondary" @click="showCancelDialog = false">Keep Editing</button>
                    <button class="btn btn-danger" @click="discardConfig">Discard</button>
                </div>
            </div>
        </div>

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

    </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue';
import { onBeforeRouteLeave } from 'vue-router';
import { api } from '../api/client';
import ConfigSection from '../components/config/ConfigSection.vue';
import TemplateListInput from '../components/config/TemplateListInput.vue';
import AppInput from '../components/common/AppInput.vue';
import StickyActionBanner from '../components/common/StickyActionBanner.vue';

const loading = ref(true);
const saving = ref(false);
const error = ref<string | null>(null);
const showCancelDialog = ref(false);
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
    cropGrow: { searchRadius: 10 },
    skillsGuideBook: { enabled: true },
    branding: {
        skillTemplate: ['&aLevel {level} / {max_level}', '{bar}', '&aXP: {xp_into} / {xp_needed}', '{color}Total XP: {xp_total}', '&7▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔', '{lore}', '', '{abilities}'],
        barTemplate: { width: 20, filled: '&a█', empty: '&8█', start: '&7[', end: '&7]' },
        abilitiesTemplate: ['{ability}', ''],
        abilityType: { active: '&8Active', passive: '&8Passive' },
        abilityLockedTemplate: ['&c❌ {level} &8· {name} &8· {type}', '{lore}'],
        abilityUnlockedTemplate: ['&a✔ {name} &8· {type}', '{lore}'],
        levelUp: {
            title: '&6Level up!',
            subtitle: '{color}{name} &aincreased to {level}',
            message: '&fYou leveled up &a[{name} {level}]',
            maxedMessage: '&f{player} has reached max level {color}[{name}]',
        },
        abilityUnlock: {
            title: '&6Unlocked!',
            subtitle: '&a✔ {name} &8· {type}',
            message: '&fYou unlocked the ability &a[{name} &8· {type}&a]',
        },
        abilityFeedback: { readyMessage: '&a✦ {color}{name} &ais ready!' },
        gui: {
            title: '&6Skills',
            prevPage: '&6◀ Prev Page',
            nextPage: '&6Next Page ▶',
            pageCount: '&7{count} skill(s)',
            skillNameUnlocked: '&a{name}',
            skillNameLocked: '&7{name} &8· Locked',
        },
        guideBook: { name: '&6Skills Guide', lore: '&7Right-click to open your skills' },
        bossBar: { titleFormat: '{color}{name} &7- &f{level}', defaultColor: 'white', defaultStyle: 'solid' },
        command: {
            header: '&6=== {title} ===',
            command: '&e{command}',
            description: '&f{description}',
            usage: '&eUsage: {usage}',
            success: '&a{message}',
            error: '&c{message}',
            info: '&7{message}',
        },
    },
    web: { enabled: false, port: 8082, username: 'admin', password: '' },
});

const isDirty = computed(() => JSON.stringify(config) !== cleanConfig.value);

onBeforeRouteLeave((_to, _from, next) => {
    if (!isDirty.value) {
        next();
        return;
    }
    showLeaveDialog.value = true;
    pendingNavigation = () => next();
});

onMounted(fetchConfig);

function confirmCancel() {
    showCancelDialog.value = true;
}

function discardConfig() {
    showCancelDialog.value = false;
    cleanConfig.value = JSON.stringify(config);
    fetchConfig();
}

async function leaveSave() {
    showLeaveDialog.value = false;
    saving.value = true;
    error.value = null;
    try {
        await api.config.update({ ...config });
        saving.value = false;
        pendingNavigation?.();
        pendingNavigation = null;
    } catch (e: any) {
        // A failed save must never silently navigate away: surface the error
        // and re-open the leave dialog so the admin can retry or discard.
        saving.value = false;
        error.value = e.message || 'Failed to save changes';
        showLeaveDialog.value = true;
    }
}

function leaveDiscard() {
    showLeaveDialog.value = false;
    pendingNavigation?.();
    pendingNavigation = null;
}

async function fetchConfig() {
    loading.value = true;
    error.value = null;
    try {
        const data = await api.config.get();
        Object.assign(config, data);
        // The backend redacts the stored password; blank means "keep current".
        config.web.password = '';
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

.page-subtitle {
    margin: 0.15rem 0 0;
    font-size: 0.8rem;
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
