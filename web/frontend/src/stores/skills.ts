import { defineStore } from 'pinia';
import { ref } from 'vue';
import { api } from '../api/client';

export const useSkillsStore = defineStore('skills', () => {
    const skills = ref<any[]>([]);
    const currentSkill = ref<any | null>(null);
    const loading = ref(false);
    const error = ref<string | null>(null);
    const saving = ref(false);

    async function fetchList() {
        loading.value = true;
        error.value = null;
        try {
            skills.value = await api.skills.list();
        } catch (e: any) {
            error.value = e.message;
        } finally {
            loading.value = false;
        }
    }

    async function fetch(id: string) {
        loading.value = true;
        error.value = null;
        try {
            currentSkill.value = await api.skills.get(id);
        } catch (e: any) {
            error.value = e.message;
        } finally {
            loading.value = false;
        }
    }

    async function save(data: any) {
        saving.value = true;
        error.value = null;
        try {
            if (currentSkill.value?.id) {
                const updated = await api.skills.update(currentSkill.value.id, data);
                currentSkill.value = updated;
                const idx = skills.value.findIndex(s => s.id === updated.id);
                if (idx !== -1) skills.value[idx] = updated;
            } else {
                const created = await api.skills.create(data);
                currentSkill.value = created;
                skills.value.push(created);
            }
        } catch (e: any) {
            error.value = e.message;
            throw e;
        } finally {
            saving.value = false;
        }
    }

    async function remove(id: string) {
        error.value = null;
        try {
            await api.skills.delete(id);
            skills.value = skills.value.filter(s => s.id !== id);
            if (currentSkill.value?.id === id) {
                currentSkill.value = null;
            }
        } catch (e: any) {
            error.value = e.message;
        }
    }

    function resetCurrent() {
        currentSkill.value = null;
    }

    return { skills, currentSkill, loading, error, saving, fetchList, fetch, save, remove, resetCurrent };
});
