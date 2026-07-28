import { ref, type Ref } from 'vue'

export function useDragReorder<T>(items: Ref<T[]>) {
    const dragIndex = ref<number | null>(null)

    function onDragStart(index: number) {
        dragIndex.value = index
    }

    function onDragOver(e: DragEvent, index: number) {
        e.preventDefault()
        if (dragIndex.value === null || dragIndex.value === index) return
        const newItems = [...items.value]
        const [removed] = newItems.splice(dragIndex.value, 1)
        newItems.splice(index, 0, removed)
        items.value = newItems
        dragIndex.value = index
    }

    function onDragEnd() {
        dragIndex.value = null
    }

    return { dragIndex, onDragStart, onDragOver, onDragEnd }
}
