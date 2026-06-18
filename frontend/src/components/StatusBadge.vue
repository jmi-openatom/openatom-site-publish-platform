<script setup lang="ts">
import { computed } from 'vue'
import { PhCircleNotch } from '@phosphor-icons/vue'
import { statusLabel } from '@/lib/format'

const props = defineProps<{ status: string }>()
const tone = computed(() => {
  if (['ONLINE', 'SUCCESS', 'ACTIVE'].includes(props.status)) return 'success'
  if (props.status === 'BUILDING') return 'building'
  if (props.status === 'FAILED') return 'danger'
  return 'warning'
})
</script>

<template>
  <span class="status-badge" :class="`status-badge--${tone}`">
    <PhCircleNotch v-if="status === 'BUILDING'" class="spin" :size="13" weight="bold" />
    <span v-else class="status-dot" />
    {{ statusLabel(status) }}
  </span>
</template>

