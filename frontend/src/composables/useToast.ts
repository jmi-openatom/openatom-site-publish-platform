import { reactive } from 'vue'

export interface ToastItem {
  id: number
  message: string
  tone: 'success' | 'error' | 'info'
}

const state = reactive<{ items: ToastItem[] }>({ items: [] })
let counter = 0

export function useToast() {
  const show = (message: string, tone: ToastItem['tone'] = 'success') => {
    const id = ++counter
    state.items.push({ id, message, tone })
    window.setTimeout(() => {
      const index = state.items.findIndex((item) => item.id === id)
      if (index >= 0) state.items.splice(index, 1)
    }, 3200)
  }

  return { toasts: state.items, show }
}

