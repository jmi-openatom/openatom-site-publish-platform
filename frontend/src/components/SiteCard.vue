<script setup lang="ts">
import { computed, ref } from 'vue'
import {
  PhArrowClockwise,
  PhCopy,
  PhDotsThreeVertical,
  PhGitBranch,
  PhGlobe,
  PhTrash,
} from '@phosphor-icons/vue'
import type { Site } from '@/types'
import { formatDate } from '@/lib/format'
import { siteVisitUrl } from '@/lib/siteUrl'
import SitePreviewImage from './SitePreviewImage.vue'
import StatusBadge from './StatusBadge.vue'

const props = defineProps<{ site: Site }>()
const emit = defineEmits<{
  open: [site: Site]
  redeploy: [site: Site]
  delete: [site: Site]
  copy: [value: string]
}>()

const menuOpen = ref(false)
const address = computed(() => siteVisitUrl(props.site))
const addressLabel = computed(() => props.site.customDomain || props.site.defaultDomain)
</script>

<template>
  <article class="site-card" @click="emit('open', site)">
    <div class="site-preview">
      <SitePreviewImage :site="site" :alt="`${site.name} 网站预览`" />
    </div>
    <div class="site-card__body">
      <div class="site-title-row">
        <div class="site-title">
          <span class="site-type-icon"><PhGlobe :size="20" /></span>
          <div>
            <div class="site-name-line">
              <h3>{{ site.name }}</h3>
              <span class="framework-badge">{{ site.framework }}</span>
            </div>
            <button class="domain-link" @click.stop="emit('copy', address)">
              <span class="domain-lock">●</span>{{ addressLabel }}<PhCopy :size="15" />
            </button>
          </div>
        </div>
        <div class="site-actions" @click.stop>
          <button class="icon-button icon-button--ghost" aria-label="更多操作" @click="menuOpen = !menuOpen">
            <PhDotsThreeVertical :size="20" weight="bold" />
          </button>
          <Transition name="popover">
            <div v-if="menuOpen" class="popover-menu card-menu">
              <button @click="emit('redeploy', site); menuOpen = false">
                <PhArrowClockwise :size="16" />重新发布
              </button>
              <button class="danger-action" @click="emit('delete', site); menuOpen = false">
                <PhTrash :size="16" />删除项目
              </button>
            </div>
          </Transition>
        </div>
      </div>
      <div class="site-meta-row">
        <span>最后部署：{{ formatDate(site.latestDeployment?.createdAt || site.updatedAt) }}</span>
        <span class="branch"><PhGitBranch :size="15" />{{ site.branchName || 'main' }}</span>
        <code v-if="site.latestDeployment?.commitHash">{{ site.latestDeployment.commitHash }}</code>
        <StatusBadge :status="site.status" />
      </div>
    </div>
  </article>
</template>
