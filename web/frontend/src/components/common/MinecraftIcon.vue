<template>
    <span
        class="mc-icon"
        :class="{ 'image-loaded': imageLoaded, 'image-errored': errored }"
        :style="{
            width: size + 'px',
            height: size + 'px',
            '--accent': color,
        }"
    >
        <img
            v-if="!errored"
            :src="imageUrl"
            :alt="material"
            :class="{ loaded: imageLoaded }"
            class="mc-image"
            @error="onError"
            @load="onLoad"
            referrerpolicy="no-referrer"
        />
        <span v-else class="mc-fallback" :style="{ background: color + '22', borderColor: color }">
            {{ fallbackLetter }}
        </span>
    </span>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';

const props = withDefaults(defineProps<{
    material: string;
    color?: string;
    size?: number;
}>(), {
    color: '#fff',
    size: 40,
});

const errored = ref(false);
const imageLoaded = ref(false);

const CDN_BASE = 'https://raw.githubusercontent.com/InventivetalentDev/minecraft-assets';
const VERSION = import.meta.env.VITE_MINECRAFT_ASSETS_VERSION || '1.21.4';

const itemName = computed(() => {
    const clean = props.material.replace(/^minecraft:/, '');
    return clean;
});

const imageUrl = computed(() => {
    return `${CDN_BASE}/${VERSION}/assets/minecraft/textures/item/${itemName.value}.png`;
});

const fallbackLetter = computed(() => {
    const name = itemName.value;
    if (!name) return '?';
    // Pick first letter of the first meaningful word
    return name.charAt(0).toUpperCase();
});

function onError() {
    errored.value = true;
}

function onLoad() {
    imageLoaded.value = true;
}
</script>

<style scoped>
.mc-icon {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    border-radius: 8px;
    background: linear-gradient(135deg, #1a1a2e 0%, #16213e 100%);
    border: 1px solid rgba(255, 255, 255, 0.08);
    box-shadow:
        inset 0 0 0 1px rgba(255, 255, 255, 0.04),
        inset 0 0 12px rgba(0, 0, 0, 0.3),
        0 0 0 1px var(--accent, #fff) 33;
    position: relative;
    overflow: hidden;
    flex-shrink: 0;
}

/* Color accent glow ring */
.mc-icon::before {
    content: '';
    position: absolute;
    inset: -1px;
    border-radius: 9px;
    border: 1.5px solid color-mix(in srgb, var(--accent, #fff) 35%, transparent);
    pointer-events: none;
}

/* Loading shimmer — hidden once image loads or errors */
.mc-icon::after {
    content: '';
    position: absolute;
    inset: 0;
    border-radius: 8px;
    background: linear-gradient(
        90deg,
        transparent 25%,
        rgba(255, 255, 255, 0.06) 50%,
        transparent 75%
    );
    background-size: 200% 100%;
    animation: mcShimmer 1.5s infinite;
    pointer-events: none;
    opacity: 1;
    transition: opacity 0.3s;
}

.mc-icon.image-loaded::after,
.mc-icon.image-errored::after {
    opacity: 0;
}

.mc-image {
    width: 100%;
    height: 100%;
    object-fit: contain;
    image-rendering: pixelated;
    image-rendering: crisp-edges;
    opacity: 0;
    transition: opacity 0.25s ease;
    z-index: 1;
    position: relative;
    padding: 4px;
    box-sizing: border-box;
}

.mc-image.loaded {
    opacity: 1;
}

.mc-image.loaded ~ .mc-fallback {
    display: none;
}

.mc-fallback {
    width: 100%;
    height: 100%;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 8px;
    font-size: 1.2em;
    font-weight: 700;
    color: var(--accent, #fff);
    border: 2px solid;
    box-sizing: border-box;
    z-index: 1;
    position: relative;
}

@keyframes mcShimmer {
    0% { background-position: 200% 0; }
    100% { background-position: -200% 0; }
}
</style>
