<script setup lang="ts">
import { PhCheckCircle, PhInfo, PhWarningCircle } from '@phosphor-icons/vue'
import { useToast } from '@/composables/useToast'

const { toasts } = useToast()
</script>

<template>
  <div class="toast-host" aria-live="polite">
    <div v-for="toast in toasts" :key="toast.id" class="toast" :class="`toast--${toast.tone}`">
      <PhCheckCircle v-if="toast.tone === 'success'" :size="18" weight="fill" />
      <PhWarningCircle v-else-if="toast.tone === 'error'" :size="18" weight="fill" />
      <PhInfo v-else :size="18" weight="fill" />
      <span>{{ toast.message }}</span>
    </div>
  </div>
</template>

