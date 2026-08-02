<template>
  <div class="gui-layout-page">
    <div class="page-header">
      <div>
        <h1 class="page-title">Layout</h1>
        <p class="page-subtitle">Arrange skills into a visual chest grid for the in-game menu</p>
      </div>
    </div>

    <div v-if="store.error" class="error-banner">
      {{ store.error }}
    </div>

    <div v-if="!store.layout" class="loading-skeleton">
      <div class="skeleton-grid"></div>
    </div>

    <template v-else>
      <PageTabs
        :pages="store.layout.pages"
        :active-index="activePage"
        @select="activePage = $event"
        @add="onAddPage"
        @remove="onRemovePage"
        @rename="(idx: number, label: string) => store.renamePage(idx, label)"
        @duplicate="(idx: number) => store.duplicatePage(idx)"
        @clear-slots="(idx: number) => store.clearPageSlots(idx)"
        @move-left="(idx: number) => store.movePage(idx, -1)"
        @move-right="(idx: number) => store.movePage(idx, 1)"
      />

      <div class="layout-main">
          <ChestGrid
            v-if="currentPage !== null"
            :rows="store.layout.rows"
            :page="currentPage"
            :page-index="activePage"
            :total-pages="store.layout.pages.length"
            :skill-map="skillMap"
            @assign="(pageIndex: number, slot: number, skillId: string) => store.setSlot(pageIndex, slot, skillId)"
            @swap="(pageIndex: number, fromSlot: number, toSlot: number) => store.swapSlots(pageIndex, fromSlot, toSlot)"
            @remove="(pageIndex: number, slot: number) => store.clearSlot(pageIndex, slot)"
            @prev-page="activePage = Math.max(0, activePage - 1)"
            @next-page="activePage = Math.min(store.layout.pages.length - 1, activePage + 1)"
          />

        <SkillPalette
          :skills="paletteSkills"
        />
      </div>
    </template>

    <StickyActionBanner :visible="isDirty" :saving="store.saving" @save="saveToStaging" @cancel="confirmCancel" />

    <div v-if="showCancelDialog" class="modal-overlay" @click.self="showCancelDialog = false">
      <div class="modal">
        <h3>Discard changes?</h3>
        <p>Any unsaved changes to your GUI layout will be lost.</p>
        <div class="modal-actions">
          <button class="btn btn-secondary" @click="showCancelDialog = false">Keep Editing</button>
          <button class="btn btn-danger" @click="discardChanges">Discard</button>
        </div>
      </div>
    </div>

    <div v-if="showLeaveDialog" class="modal-overlay" @click.self="showLeaveDialog = false">
      <div class="modal">
        <h3>Unsaved changes</h3>
        <p>Would you like to save your changes before leaving?</p>
        <div class="modal-actions">
          <button class="btn btn-secondary" @click="showLeaveDialog = false">Cancel</button>
          <button class="btn btn-danger" @click="leaveDiscard">Discard</button>
          <button class="btn btn-primary" @click="leaveSave">Save & Leave</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, provide } from 'vue'
import { onBeforeRouteLeave } from 'vue-router'
import { useGuiLayoutStore } from '../stores/gui-layout'
import { useStagingStore } from '../stores/staging'
import ChestGrid from '../components/layout/ChestGrid.vue'
import PageTabs from '../components/layout/PageTabs.vue'
import SkillPalette from '../components/layout/SkillPalette.vue'
import StickyActionBanner from '../components/common/StickyActionBanner.vue'

const store = useGuiLayoutStore()
const stagingStore = useStagingStore()

const activePage = ref(0)
const selectedSkillId = ref<string | null>(null)
const cleanSnapshot = ref('')
const showCancelDialog = ref(false)
const showLeaveDialog = ref(false)
let pendingNavigation: (() => void) | null = null

provide('selectedSkillId', selectedSkillId)

const currentPage = computed(() => {
  if (!store.layout) return null
  return store.layout.pages[activePage.value] || null
})

const isDirty = computed(() => {
  return store.layout !== null && JSON.stringify(store.layout) !== cleanSnapshot.value
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

function takeSnapshot() {
  cleanSnapshot.value = store.layout ? JSON.stringify(store.layout) : ''
}

onBeforeRouteLeave((_to, _from, next) => {
  if (!isDirty.value) {
    next()
    return
  }
  showLeaveDialog.value = true
  pendingNavigation = () => next()
})

onMounted(async () => {
  await store.fetch()
  takeSnapshot()
  await stagingStore.fetchStatus()
})

async function saveToStaging() {
  try {
    await store.save()
    await stagingStore.fetchStatus()
    takeSnapshot()
  } catch {
    // error is set in store
  }
}

function confirmCancel() {
  showCancelDialog.value = true
}

function discardChanges() {
  showCancelDialog.value = false
  store.fetch().then(takeSnapshot)
}

function onAddPage(label: string, icon = 'minecraft:book', customModelData = 0) {
  store.addPage(label, icon, customModelData)
  activePage.value = store.layout!.pages.length - 1
}

function onRemovePage(index: number) {
  if (store.layout && store.layout.pages.length <= 1) return
  store.removePage(index)
  if (activePage.value >= (store.layout?.pages.length || 0)) {
    activePage.value = Math.max(0, (store.layout?.pages.length || 1) - 1)
  }
}

async function leaveSave() {
  showLeaveDialog.value = false
  try {
    await store.save()
    await stagingStore.fetchStatus()
  } catch {
    // A failed save must never silently navigate away: the store already set
    // store.error; re-open the leave dialog so the admin can retry or discard.
    showLeaveDialog.value = true
    return
  }
  pendingNavigation?.()
  pendingNavigation = null
}

function leaveDiscard() {
  showLeaveDialog.value = false
  pendingNavigation?.()
  pendingNavigation = null
}
</script>

<style scoped>
.gui-layout-page {
  padding: 1.5rem;
  max-width: 740px;
  margin: 0 auto;
  padding-bottom: 4rem;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 1.5rem;
}

.page-title {
  font-size: 1.5rem;
  font-weight: 700;
  margin: 0;
  color: var(--p-text-color, #fff);
}

.page-subtitle {
  margin: 0.15rem 0 0;
  font-size: 0.8rem;
  color: var(--p-text-muted-color, #888);
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
  flex-direction: column;
  gap: 1rem;
  margin-top: 1rem;
  align-items: center;
}

.loading-skeleton {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  align-items: center;
}

.skeleton-grid {
  width: 100%;
  max-width: 700px;
  height: 466px;
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

/* Modal system (matches TagsPage/ConfigPage) */
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal {
  background: var(--p-content-background);
  border: 1px solid var(--p-content-border-color);
  border-radius: 8px;
  padding: 1.5rem;
  max-width: 400px;
  width: 90%;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.15);
}

.modal h3 {
  margin: 0 0 0.5rem;
  font-size: 1.05rem;
  color: var(--p-text-color);
}

.modal p {
  margin: 0 0 1.25rem;
  color: var(--p-text-muted-color, #888);
  font-size: 0.875rem;
  line-height: 1.4;
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.5rem;
}
</style>
