<script setup lang="ts">
import { PhX } from '@phosphor-icons/vue'

defineProps<{
  open: boolean
  title: string
  description?: string
  wide?: boolean
}>()

defineEmits<{ close: [] }>()
</script>

<template>
  <Teleport to="body">
    <Transition name="modal">
      <div v-if="open" class="modal-backdrop" @mousedown.self="$emit('close')">
        <section class="modal-panel" :class="{ 'modal-panel--wide': wide }" role="dialog" aria-modal="true">
          <header class="modal-header">
            <div>
              <h2>{{ title }}</h2>
              <p v-if="description">{{ description }}</p>
            </div>
            <button class="icon-button" aria-label="关闭" @click="$emit('close')">
              <PhX :size="18" />
            </button>
          </header>
          <div class="modal-body">
            <slot />
          </div>
          <footer v-if="$slots.footer" class="modal-footer">
            <slot name="footer" />
          </footer>
        </section>
      </div>
    </Transition>
  </Teleport>
</template>

