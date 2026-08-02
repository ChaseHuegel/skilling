import { defineStore } from 'pinia';
import { ref } from 'vue';
import { api } from '../api/client';

export const useSkillsStore = defineStore('skills', () => {
    const skills = ref<any[]>([]);

    async function fetchList() {
        skills.value = await api.skills.list();
    }

    return { skills, fetchList };
});
