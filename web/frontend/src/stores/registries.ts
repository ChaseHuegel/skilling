import { defineStore } from 'pinia';
import { ref } from 'vue';
import { api } from '../api/client';

export const useRegistriesStore = defineStore('registries', () => {
    const mechanicKeys = ref<string[]>([]);
    const mechanicParams = ref<Record<string, string[]>>({});
    const triggers = ref<string[]>([]);
    const stateFilters = ref<string[]>([]);
    const loaded = ref(false);

    async function fetch() {
        if (loaded.value) return;
        try {
            const [mechRes, trigRes, sfRes] = await Promise.all([
                api.mechanics.list(),
                api.triggers.list(),
                api.stateFilters.list(),
            ]);
            mechanicParams.value = mechRes.mechanics;
            mechanicKeys.value = Object.keys(mechRes.mechanics);
            triggers.value = trigRes.triggers;
            stateFilters.value = sfRes.stateFilters;
            loaded.value = true;
        } catch {
            // Keep defaults on error
        }
    }

    return { mechanicKeys, mechanicParams, triggers, stateFilters, loaded, fetch };
});
