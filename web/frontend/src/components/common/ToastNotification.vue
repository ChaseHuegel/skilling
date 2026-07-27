<template>
    <div v-if="visible" :class="['toast', type]">
        <span class="toast-msg">{{ message }}</span>
        <button class="toast-close" @click="dismiss">&times;</button>
    </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue';

const props = defineProps<{
    message: string | null;
    type?: 'success' | 'error' | 'info';
}>();

const emit = defineEmits<{ dismiss: [] }>();

const visible = ref(false);
const autoDismissTimer = ref<ReturnType<typeof setTimeout> | null>(null);

watch(() => props.message, (val) => {
    if (val) {
        visible.value = true;
        if (autoDismissTimer.value) clearTimeout(autoDismissTimer.value);
        if (props.type !== 'error') {
            autoDismissTimer.value = setTimeout(() => {
                visible.value = false;
                emit('dismiss');
            }, 4000);
        }
    } else {
        visible.value = false;
    }
});

function dismiss() {
    visible.value = false;
    if (autoDismissTimer.value) clearTimeout(autoDismissTimer.value);
    emit('dismiss');
}
</script>

<style scoped>
.toast {
    position: fixed;
    top: 1rem;
    right: 1rem;
    padding: 0.75rem 1rem;
    border-radius: 6px;
    color: white;
    font-size: 0.875rem;
    z-index: 2000;
    display: flex;
    align-items: center;
    gap: 0.75rem;
    box-shadow: 0 2px 8px rgba(0,0,0,0.2);
    animation: slideIn 0.2s ease-out;
}
@keyframes slideIn {
    from { transform: translateX(100%); opacity: 0; }
    to { transform: translateX(0); opacity: 1; }
}
.toast.success { background: #16a34a; }
.toast.error { background: #dc2626; }
.toast.info { background: #2563eb; }
.toast-close {
    background: none;
    border: none;
    color: white;
    font-size: 1.25rem;
    cursor: pointer;
    opacity: 0.8;
    padding: 0;
    line-height: 1;
}
.toast-close:hover { opacity: 1; }
</style>
