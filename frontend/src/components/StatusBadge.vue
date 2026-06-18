<script setup lang="ts">
import { computed } from 'vue'
import { PhCircleNotch } from '@phosphor-icons/vue'
import { statusLabel } from '@/lib/format'

const props = defineProps<{ status: string }>()
const tone = computed(() => {
  if (['ONLINE', 'SUCCESS', 'ACTIVE', 'SSL_ACTIVE'].includes(props.status)) return 'success'
  if (['BUILDING', 'SSL_PROVISIONING'].includes(props.status)) return 'building'
  if (['FAILED', 'SSL_FAILED'].includes(props.status)) return 'danger'
  return 'warning'
})
</script>

<template>
  <span class="status-badge" :class="`status-badge--${tone}`">
    <PhCircleNotch v-if="['BUILDING', 'SSL_PROVISIONING'].includes(status)" class="spin" :size="13" weight="bold" />
    <span v-else class="status-dot" />
    {{ statusLabel(status) }}
  </span>
</template>
