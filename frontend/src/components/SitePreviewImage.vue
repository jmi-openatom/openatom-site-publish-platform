<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import type { Site } from '@/types'

const props = defineProps<{
  site: Site
  alt: string
}>()

const sourceIndex = ref(0)
const sources = computed(() => Array.from(new Set([
  `/previews/sites/${encodeURIComponent(props.site.slug)}.png?v=${encodeURIComponent(props.site.updatedAt)}`,
  props.site.previewImage,
  '/previews/mirror.png',
].filter((source): source is string => Boolean(source)))))
const source = computed(() => sources.value[Math.min(sourceIndex.value, sources.value.length - 1)])

watch(
  () => [props.site.slug, props.site.updatedAt, props.site.previewImage],
  () => {
    sourceIndex.value = 0
  },
)

function useFallback() {
  if (sourceIndex.value < sources.value.length - 1) {
    sourceIndex.value += 1
  }
}
</script>

<template>
  <img :src="source" :alt="alt" @error="useFallback" />
</template>
