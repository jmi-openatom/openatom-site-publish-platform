<script setup lang="ts">
import { PhImageSquare } from '@phosphor-icons/vue'
import { computed, ref, watch } from 'vue'
import { siteVisitUrl } from '@/lib/siteUrl'
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
const livePreviewUrl = computed(() => {
  if (props.site.status !== 'ONLINE') return undefined
  const stableSiteUrl = siteVisitUrl({ defaultDomain: props.site.defaultDomain })
  return `${stableSiteUrl}?site-preview=${encodeURIComponent(props.site.updatedAt)}`
})

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
  <iframe
    v-else-if="livePreviewUrl"
    class="site-live-preview"
    :src="livePreviewUrl"
    :title="alt"
    loading="lazy"
    referrerpolicy="no-referrer"
    sandbox="allow-same-origin allow-scripts"
    tabindex="-1"
  />
  <div v-else class="site-preview-placeholder" role="img" :aria-label="`${site.name} 暂无真实网站截图`">
    <PhImageSquare :size="28" />
    <span>真实预览待生成</span>
  </div>
</template>
