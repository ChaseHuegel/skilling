import { defineStore } from 'pinia';
import { ref } from 'vue';
import { api } from '../api/client';

export const useStagingStore = defineStore('staging', () => {
    const hasPending = ref(false);
    const fileCount = ref(0);
    const files = ref<string[]>([]);
    const loading = ref(false);
    const applying = ref(false);

    async function fetchStatus() {
        loading.value = true;
        try {
            const status = await api.staging.status();
            hasPending.value = status.hasPendingChanges;
            fileCount.value = status.fileCount;
            files.value = status.files;
        } catch {
            hasPending.value = false;
            fileCount.value = 0;
            files.value = [];
        } finally {
            loading.value = false;
        }
    }

    async function applyAndReload(): Promise<string | null> {
        applying.value = true;
        try {
            const result = await api.reload();
            if (result.success) {
                hasPending.value = false;
                fileCount.value = 0;
                files.value = [];
                return null;
            }
            return result.message;
        } catch (e: any) {
            return e.message;
        } finally {
            applying.value = false;
        }
    }

    async function discard() {
        try {
            await api.staging.clear();
            hasPending.value = false;
            fileCount.value = 0;
            files.value = [];
        } catch { /* ignore */ }
    }

    return { hasPending, fileCount, files, loading, applying, fetchStatus, applyAndReload, discard };
});
