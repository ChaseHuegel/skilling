<template>
    <div class="level-up-commands-section">
        <div class="commands-list" v-if="localCommands.length > 0">
            <div
                v-for="(cmd, i) in localCommands"
                :key="i"
                class="command-row"
            >
                <input
                    type="text"
                    :value="cmd"
                    @input="updateCommand(i, ($event.target as HTMLInputElement).value)"
                    placeholder="/give {player} minecraft:diamond 1"
                    class="command-input"
                />
                <button class="btn-remove" @click="removeCommand(i)" title="Remove command">&times;</button>
            </div>
        </div>
        <div v-else class="commands-empty">
            No level-up commands configured.
        </div>

        <div class="placeholder-chips">
            <span class="placeholder-label">Insert placeholder:</span>
            <button
                v-for="ph in PLACEHOLDERS"
                :key="ph"
                class="chip"
                @click="insertPlaceholder(ph)"
            >{{ ph }}</button>
        </div>

        <button class="btn-add" @click="addCommand">+ Add Command</button>
    </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue';

const PLACEHOLDERS = ['{player}', '{level}', '{skill_id}', '{skill_name}'];

const props = defineProps<{
    modelValue: string[];
}>();

const emit = defineEmits<{
    'update:modelValue': [value: string[]];
}>();

const localCommands = ref<string[]>([...props.modelValue]);

watch(() => props.modelValue, (val) => {
    localCommands.value = [...val];
});

function emitUpdate() {
    emit('update:modelValue', [...localCommands.value]);
}

function addCommand() {
    localCommands.value.push('');
    emitUpdate();
}

function removeCommand(index: number) {
    localCommands.value.splice(index, 1);
    emitUpdate();
}

function updateCommand(index: number, value: string) {
    localCommands.value[index] = value;
    emitUpdate();
}

function insertPlaceholder(placeholder: string) {
    localCommands.value.push(placeholder);
    emitUpdate();
}
</script>

<style scoped>
.level-up-commands-section {
    display: flex;
    flex-direction: column;
    gap: 0.75rem;
}
.commands-list {
    display: flex;
    flex-direction: column;
    gap: 0.5rem;
}
.command-row {
    display: flex;
    gap: 0.5rem;
    align-items: center;
}
.command-input {
    flex: 1;
    padding: 0.5rem;
    border: 1px solid var(--p-content-border-color, #ccc);
    border-radius: 4px;
    background: var(--p-form-field-background, #fff);
    color: var(--p-text-color, #333);
    font-family: monospace;
    font-size: 0.85rem;
}
.command-input:focus {
    outline: none;
    border-color: var(--p-primary-color, #3b82f6);
    box-shadow: 0 0 0 1px var(--p-primary-color, #3b82f6);
}
.btn-remove {
    background: transparent;
    border: 1px solid var(--p-content-border-color, #ccc);
    border-radius: 4px;
    cursor: pointer;
    color: var(--p-red-500, #ef4444);
    font-size: 1.1rem;
    width: 2rem;
    height: 2rem;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
}
.btn-remove:hover {
    background: color-mix(in srgb, var(--p-red-500, #ef4444) 10%, transparent);
    border-color: var(--p-red-500, #ef4444);
}
.commands-empty {
    color: var(--p-text-muted-color, #888);
    font-style: italic;
    font-size: 0.85rem;
}
.placeholder-chips {
    display: flex;
    flex-wrap: wrap;
    gap: 0.35rem;
    align-items: center;
}
.placeholder-label {
    font-size: 0.8rem;
    color: var(--p-text-muted-color, #888);
    margin-right: 0.25rem;
}
.chip {
    background: var(--p-primary-color, #3b82f6);
    color: #fff;
    border: none;
    border-radius: 4px;
    padding: 0.2rem 0.5rem;
    font-size: 0.75rem;
    cursor: pointer;
    font-family: monospace;
}
.chip:hover {
    opacity: 0.85;
}
.btn-add {
    align-self: flex-start;
    background: transparent;
    border: 1px dashed var(--p-content-border-color, #ccc);
    border-radius: 4px;
    padding: 0.4rem 0.75rem;
    cursor: pointer;
    color: var(--p-primary-color, #3b82f6);
    font-size: 0.85rem;
}
.btn-add:hover {
    background: color-mix(in srgb, var(--p-primary-color, #3b82f6) 5%, transparent);
    border-color: var(--p-primary-color, #3b82f6);
}
</style>
