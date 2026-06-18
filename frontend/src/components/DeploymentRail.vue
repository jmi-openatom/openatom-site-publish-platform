<script setup lang="ts">
import { computed } from 'vue'
import { PhArrowRight, PhCaretDown, PhCheckCircle, PhCircleNotch, PhWarning } from '@phosphor-icons/vue'
import type { Deployment } from '@/types'
import { formatDate, statusLabel } from '@/lib/format'

const props = defineProps<{ deployments: Deployment[] }>()
const visible = computed(() => props.deployments.slice(0, 7))
</script>

<template>
  <aside class="activity-rail">
    <header class="activity-header">
      <h2>部署动态</h2>
      <button class="select-button">全部<PhCaretDown :size="14" /></button>
    </header>
    <div v-if="visible.length" class="activity-list">
      <article v-for="deployment in visible" :key="deployment.id" class="activity-item">
        <span class="activity-icon" :class="`activity-icon--${deployment.status.toLowerCase()}`">
          <PhCheckCircle v-if="deployment.status === 'SUCCESS'" :size="21" weight="fill" />
          <PhCircleNotch v-else-if="deployment.status === 'BUILDING'" class="spin" :size="21" weight="bold" />
          <PhWarning v-else :size="21" weight="fill" />
        </span>
        <div class="activity-copy">
          <div class="activity-title">
            <strong>{{ deployment.siteName }}</strong>
            <span :class="`text-${deployment.status.toLowerCase()}`">{{ statusLabel(deployment.status) }}</span>
            <time>{{ formatDate(deployment.createdAt) }}</time>
          </div>
          <div class="activity-detail">
            <span v-if="deployment.status === 'BUILDING'">正在构建（步骤 2/3）</span>
            <span v-else-if="deployment.status === 'FAILED'">构建失败，请检查日志</span>
            <span v-else>部署到生产环境 <code>main</code></span>
            <code>{{ deployment.commitHash }}</code>
          </div>
        </div>
        <PhArrowRight :size="15" />
      </article>
    </div>
    <div v-else class="activity-empty">暂时没有部署记录</div>
    <RouterLink to="/deployments" class="activity-footer">查看全部动态<PhArrowRight :size="16" /></RouterLink>
  </aside>
</template>

