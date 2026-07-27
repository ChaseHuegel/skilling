import { defineStore } from 'pinia';
import { ref } from 'vue';
import { api } from '../api/client';

export const useConfigStore = defineStore('config', () => {
    const config = ref<any>(null);
    const loading = ref(false);
    const saving = ref(false);

    async function fetch() {
        loading.value = true;
        try {
            config.value = await api.config.get();
        } catch {
            config.value = null;
        } finally {
            loading.value = false;
        }
    }

    async function save(data: any) {
        saving.value = true;
        try {
            config.value = await api.config.update(data);
        } finally {
            saving.value = false;
        }
    }

    return { config, loading, saving, fetch, save };
});
