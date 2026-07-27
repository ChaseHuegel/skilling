import { defineStore } from 'pinia';
import { ref } from 'vue';
import { api } from '../api/client';

export const useTagsStore = defineStore('tags', () => {
    const tags = ref<Record<string, string[]>>({});
    const loading = ref(false);
    const saving = ref(false);

    async function fetch() {
        loading.value = true;
        try {
            const res = await api.tags.get();
            tags.value = res.tags;
        } catch {
            tags.value = {};
        } finally {
            loading.value = false;
        }
    }

    async function save(data: Record<string, string[]>) {
        saving.value = true;
        try {
            await api.tags.update(data);
            tags.value = data;
        } finally {
            saving.value = false;
        }
    }

    return { tags, loading, saving, fetch, save };
});
