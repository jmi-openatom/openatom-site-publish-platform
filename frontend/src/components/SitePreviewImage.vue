<script setup lang="ts">
import { PhImageSquare } from '@phosphor-icons/vue'
import { computed, ref, watch } from 'vue'
import type { Site } from '@/types'

const props = defineProps<{
  site: Site
  alt: string
}>()

const legacyPlaceholderImages = new Set([
  '/previews/lab.png',
  '/previews/recruit.png',
  '/previews/wall.png',
  '/previews/mirror.png',
])
const sourceIndex = ref(0)
const sources = computed(() => Array.from(new Set([
  `/previews/sites/${encodeURIComponent(props.site.slug)}.png?v=${encodeURIComponent(props.site.updatedAt)}`,
  props.site.previewImage && !legacyPlaceholderImages.has(props.site.previewImage)
    ? props.site.previewImage
    : undefined,
].filter((source): source is string => Boolean(source)))))
const source = computed(() => sources.value[sourceIndex.value])

watch(
  () => [props.site.slug, props.site.updatedAt, props.site.previewImage],
  () => {
    sourceIndex.value = 0
  },
)

function useFallback() {
  sourceIndex.value += 1
}
</script>

<template>
  <img v-if="source" :src="source" :alt="alt" @error="useFallback">
  <div v-else class="site-preview-placeholder" role="img" :aria-label="`${site.name} 暂无真实网站截图`">
    <PhImageSquare :size="28" />
    <span>真实预览待生成</span>
  </div>
</template>
