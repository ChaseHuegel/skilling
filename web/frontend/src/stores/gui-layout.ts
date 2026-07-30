import { ref } from 'vue'
import { defineStore } from 'pinia'
import { api } from '../api/client'

export interface GuiPageDTO {
  label: string
  slots: Record<number, string>
  icon?: string
  customModelData?: number
}

export interface GuiLayoutDTO {
  title: string
  rows: number
  pages: GuiPageDTO[]
  version: number
}

export interface SkillSummary {
  id: string
  displayName: string
  icon: string
  color: string
  maxLevel: number
  abilityCount: number
  xpSourceCount: number
  xpSourceTriggers: string[]
  abilityIds: string[]
  abilityNames: string[]
}

export const useGuiLayoutStore = defineStore('guiLayout', () => {
  const layout = ref<GuiLayoutDTO | null>(null)
  const allSkills = ref<SkillSummary[]>([])
  const loading = ref(false)
  const saving = ref(false)
  const error = ref<string | null>(null)

  async function fetch() {
    loading.value = true
    error.value = null
    try {
      const [layoutData, skillsData] = await Promise.all([
        api.guiLayout.get(),
        api.skills.list(),
      ])
      layout.value = layoutData
      allSkills.value = skillsData
    } catch (e: any) {
      error.value = e.message
    } finally {
      loading.value = false
    }
  }

  async function save() {
    if (!layout.value) return
    saving.value = true
    error.value = null
    try {
      await api.guiLayout.update(layout.value)
    } catch (e: any) {
      error.value = e.message
      throw e
    } finally {
      saving.value = false
    }
  }

  function addPage(label: string, icon = 'minecraft:book', customModelData = 0) {
    if (!layout.value) return
    layout.value = {
      ...layout.value,
      pages: [...layout.value.pages, { label, slots: {}, icon, customModelData }],
    }
  }

  function removePage(index: number) {
    if (!layout.value) return
    const copy = [...layout.value.pages]
    copy.splice(index, 1)
    layout.value = { ...layout.value, pages: copy }
  }

  function renamePage(index: number, label: string) {
    if (!layout.value) return
    const copy = [...layout.value.pages]
    copy[index] = { ...copy[index], label }
    layout.value = { ...layout.value, pages: copy }
  }

  function setSlot(pageIndex: number, slot: number, skillId: string | null) {
    if (!layout.value) return
    const copy = [...layout.value.pages]
    const page = { ...copy[pageIndex] }
    const slots = { ...page.slots }
    if (skillId === null) {
      delete slots[slot]
    } else {
      slots[slot] = skillId
    }
    page.slots = slots
    copy[pageIndex] = page
    layout.value = { ...layout.value, pages: copy }
  }

  function swapSlots(pageIndex: number, a: number, b: number) {
    if (!layout.value) return
    const copy = [...layout.value.pages]
    const page = { ...copy[pageIndex] }
    const slots = { ...page.slots }
    const temp = slots[a]
    slots[a] = slots[b]
    slots[b] = temp
    page.slots = slots
    copy[pageIndex] = page
    layout.value = { ...layout.value, pages: copy }
  }

  function clearSlot(pageIndex: number, slot: number) {
    setSlot(pageIndex, slot, null)
  }

  function duplicatePage(index: number) {
    if (!layout.value) return
    const source = layout.value.pages[index]
    const copy = [...layout.value.pages]
    copy.splice(index + 1, 0, {
      label: source.label + ' (copy)',
      slots: { ...source.slots },
      icon: source.icon,
      customModelData: source.customModelData,
    })
    layout.value = { ...layout.value, pages: copy }
  }

  function clearPageSlots(index: number) {
    if (!layout.value) return
    const copy = [...layout.value.pages]
    copy[index] = { ...copy[index], slots: {} }
    layout.value = { ...layout.value, pages: copy }
  }

  function movePage(index: number, direction: -1 | 1) {
    if (!layout.value) return
    const target = index + direction
    if (target < 0 || target >= layout.value.pages.length) return
    const copy = [...layout.value.pages]
    const temp = copy[index]
    copy[index] = copy[target]
    copy[target] = temp
    layout.value = { ...layout.value, pages: copy }
  }

  return {
    layout, allSkills, loading, saving, error,
    fetch, save, addPage, removePage, renamePage,
    setSlot, swapSlots, clearSlot,
    duplicatePage, clearPageSlots, movePage,
  }
})
