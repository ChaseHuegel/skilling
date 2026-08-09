import { defineStore } from 'pinia';
import { ref } from 'vue';
import { api } from '../api/client';
import {
    MATERIAL_SUGGESTIONS,
    TAG_SUGGESTIONS_BASE,
    ENTITY_SUGGESTIONS,
    SOUND_SUGGESTIONS,
    PARTICLE_SUGGESTIONS,
} from '../components/common/recommendedLists';

function unique(values: string[]): string[] {
    return Array.from(new Set(values));
}

export const useRegistriesStore = defineStore('registries', () => {
    const mechanicKeys = ref<string[]>([]);
    const mechanicParams = ref<Record<string, string[]>>({});
    const triggers = ref<string[]>([]);
    const stateFilters = ref<string[]>([]);
    const baseAbilities = ref<Record<string, { id: string; displayName: string; trigger: string; unlockLevel: number }>>({});
    const materials = ref<string[]>([...MATERIAL_SUGGESTIONS]);
    const tags = ref<string[]>([...TAG_SUGGESTIONS_BASE]);
    const entities = ref<string[]>([...ENTITY_SUGGESTIONS]);
    const sounds = ref<string[]>([...SOUND_SUGGESTIONS]);
    const particles = ref<string[]>([...PARTICLE_SUGGESTIONS]);
    const loaded = ref(false);

    async function fetch() {
        if (loaded.value) return;
        try {
            const [mechRes, trigRes, sfRes, abRes, matRes, soundRes, particleRes, entRes, tagRes] = await Promise.all([
                api.mechanics.list(),
                api.triggers.list(),
                api.stateFilters.list(),
                api.abilities.list(),
                api.recommended.materials(),
                api.recommended.sounds(),
                api.recommended.particles(),
                api.recommended.entities(),
                api.recommended.tags(),
            ]);
            mechanicParams.value = mechRes.mechanics;
            mechanicKeys.value = Object.keys(mechRes.mechanics);
            triggers.value = trigRes.triggers;
            stateFilters.value = sfRes.stateFilters;
            baseAbilities.value = {};
            for (const ab of abRes.abilities) {
                baseAbilities.value[ab.id] = ab;
            }
            materials.value = unique([...matRes.materials, ...MATERIAL_SUGGESTIONS]);
            sounds.value = unique([...soundRes.sounds, ...SOUND_SUGGESTIONS]);
            particles.value = unique([...particleRes.particles, ...PARTICLE_SUGGESTIONS]);
            entities.value = unique([...entRes.entities, ...ENTITY_SUGGESTIONS]);
            tags.value = unique([...tagRes.tags.map(t => (t.startsWith('#') ? t : '#' + t)), ...TAG_SUGGESTIONS_BASE]);
            loaded.value = true;
        } catch {
            // Keep defaults on error
        }
    }

    return { mechanicKeys, mechanicParams, triggers, stateFilters, baseAbilities, materials, tags, entities, sounds, particles, loaded, fetch };
});
