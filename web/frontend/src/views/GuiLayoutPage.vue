<template>
  <div class="gui-layout-page">
    <div class="page-header">
      <h1 class="page-title">GUI Layout</h1>
      <div class="header-actions">
        <button
          class="btn btn-primary"
          :disabled="!isDirty || store.saving"
          @click="applyAndReload"
        >
          {{ store.saving ? 'Saving...' : 'Apply & Reload' }}
        </button>
        <button
          class="btn btn-ghost"
          :disabled="!isDirty"
          @click="resetLayout"
        >
          Reset
        </button>
      </div>
    </div>

    <div v-if="store.error" class="error-banner">
      {{ store.error }}
    </div>

    <div v-if="!store.layout" class="loading-skeleton">
      <div class="skeleton-grid"></div>
      <div class="skeleton-palette"></div>
    </div>

    <template v-else>
      <PageTabs
        :pages="store.layout.pages"
        :active-index="activePage"
        @select="activePage = $event; markDirty()"
        @add="onAddPage"
        @remove="onRemovePage"
        @rename="(idx: number, label: string) => { store.renamePage(idx, label); markDirty() }"
        @duplicate="(idx: number) => { store.duplicatePage(idx); markDirty() }"
        @clear-slots="(idx: number) => { store.clearPageSlots(idx); markDirty() }"
        @move-left="(idx: number) => { store.movePage(idx, -1); activePage = Math.max(0, activePage - 1); markDirty() }"
        @move-right="(idx: number) => { store.movePage(idx, 1); activePage = Math.min(store.layout!.pages.length - 1, activePage + 1); markDirty() }"
      />

      <div class="layout-main">
        <div class="grid-area">
          <ChestGrid
            v-if="currentPage !== null"
            :title="store.layout.title"
            :rows="store.layout.rows"
            :page="currentPage"
            :page-index="activePage"
            :total-pages="store.layout.pages.length"
            :skill-map="skillMap"
            @assign="(pageIndex: number, slot: number, skillId: string) => { store.setSlot(pageIndex, slot, skillId); markDirty() }"
            @swap="(pageIndex: number, fromSlot: number, toSlot: number) => { store.swapSlots(pageIndex, fromSlot, toSlot); markDirty() }"
            @remove="(pageIndex: number, slot: number) => { store.clearSlot(pageIndex, slot); markDirty() }"
            @prev-page="activePage = Math.max(0, activePage - 1)"
            @next-page="activePage = Math.min(store.layout.pages.length - 1, activePage + 1)"
          />
          <div class="pending-indicator" v-if="isDirty">
            <span class="pending-dot"></span>
            Unsaved changes
          </div>
        </div>

        <SkillPalette
          :skills="paletteSkills"
        />
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, provide, reactive } from 'vue'
import { useGuiLayoutStore } from '../stores/gui-layout'
import { useStagingStore } from '../stores/staging'
import ChestGrid from '../components/layout/ChestGrid.vue'
import PageTabs from '../components/layout/PageTabs.vue'
import SkillPalette from '../components/layout/SkillPalette.vue'

const store = useGuiLayoutStore()
const stagingStore = useStagingStore()

const activePage = ref(0)
const dirtyFlag = ref(false)
const selectedSkillId = ref<string | null>(null)

provide('selectedSkillId', selectedSkillId)

const currentPage = computed(() => {
  if (!store.layout) return null
  return store.layout.pages[activePage.value] || null
})

const isDirty = computed(() => {
  return dirtyFlag.value
})

const skillMap = computed(() => {
  const map: Record<string, any> = {}
  for (const s of store.allSkills) {
    map[s.id] = {
      id: s.id,
      displayName: s.displayName,
      icon: s.icon,
      color: s.color,
      abilities: s.abilityIds.map((id, i) => ({
        name: s.abilityNames[i] || id,
        unlockLevel: 0,
      })),
    }
  }
  return map
})

const paletteSkills = computed(() => {
  return store.allSkills.map(s => ({
    id: s.id,
    displayName: s.displayName,
    icon: s.icon,
    color: s.color,
    abilities: s.abilityIds.map((id, i) => ({
      name: s.abilityNames[i] || id,
      unlockLevel: 0,
    })),
  }))
})

function markDirty() {
  dirtyFlag.value = true
}

onMounted(async () => {
  await store.fetch()
  await stagingStore.fetchStatus()
})

async function applyAndReload() {
  try {
    await store.save()
    await stagingStore.fetchStatus()
    if (stagingStore.hasPending) {
      await stagingStore.applyAndReload()
      await store.fetch()
    }
    dirtyFlag.value = false
  } catch {
    // error is set in store
  }
}

function resetLayout() {
  dirtyFlag.value = false
  store.fetch()
}

function onAddPage(label: string) {
  store.addPage(label)
  activePage.value = store.layout!.pages.length - 1
  markDirty()
}

function onRemovePage(index: number) {
  if (store.layout && store.layout.pages.length <= 1) return
  store.removePage(index)
  if (activePage.value >= (store.layout?.pages.length || 0)) {
    activePage.value = Math.max(0, (store.layout?.pages.length || 1) - 1)
  }
  markDirty()
}
</script>

<style scoped>
.gui-layout-page {
  padding: 1.5rem;
  max-width: 900px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 1rem;
}

.page-title {
  font-size: 1.3rem;
  font-weight: 700;
  margin: 0;
  color: var(--p-text-color, #fff);
}

.header-actions {
  display: flex;
  gap: 0.5rem;
}

.error-banner {
  padding: 0.75rem;
  margin-bottom: 1rem;
  background: color-mix(in srgb, var(--p-red-500, #ef4444) 15%, transparent);
  border: 1px solid var(--p-red-500, #ef4444);
  border-radius: 6px;
  color: var(--p-red-500, #ef4444);
  font-size: 0.85rem;
}

.layout-main {
  display: flex;
  gap: 1.5rem;
  margin-top: 1rem;
  align-items: flex-start;
}

.grid-area {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.pending-indicator {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 0.75rem;
  color: var(--p-form-field-placeholder-color, #888);
}

.pending-dot {
  width: 6px;
  height: 6px;
  background: var(--p-primary-color, #3b82f6);
  border-radius: 50%;
  animation: pulse 1.5s infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.3; }
}

.loading-skeleton {
  display: flex;
  gap: 1.5rem;
  align-items: flex-start;
}

.skeleton-grid {
  width: 480px;
  height: 360px;
  background: var(--p-skeleton-background, var(--p-content-background, #1a1a2e));
  border: 1px solid var(--p-content-border-color, #333);
  border-radius: 8px;
  animation: shimmer 1.5s infinite;
}

.skeleton-palette {
  width: 280px;
  height: 360px;
  background: var(--p-skeleton-background, var(--p-content-background, #1a1a2e));
  border: 1px solid var(--p-content-border-color, #333);
  border-radius: 8px;
  animation: shimmer 1.5s infinite;
}

@keyframes shimmer {
  0% { opacity: 1; }
  50% { opacity: 0.4; }
  100% { opacity: 1; }
}

@media (max-width: 900px) {
  .layout-main {
    flex-direction: column;
    align-items: center;
  }
}
</style>
