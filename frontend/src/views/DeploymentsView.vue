<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { PhArrowClockwise, PhClock, PhGitBranch, PhTerminalWindow } from '@phosphor-icons/vue'
import AppShell from '@/components/AppShell.vue'
import StatusBadge from '@/components/StatusBadge.vue'
import { api, errorMessage } from '@/lib/api'
import { formatDate } from '@/lib/format'
import { useToast } from '@/composables/useToast'
import type { ApiEnvelope, Deployment } from '@/types'

const deployments = ref<Deployment[]>([])
const selected = ref<Deployment | null>(null)
const loading = ref(true)
const toast = useToast()

async function load() {
  try {
    const response = await api.get<ApiEnvelope<Deployment[]>>('/deployments/recent?limit=100')
    deployments.value = response.data.data
    selected.value ||= deployments.value[0] || null
  } catch (error) {
    toast.show(errorMessage(error), 'error')
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <AppShell>
    <section class="page-heading">
      <div><span class="eyebrow">DEPLOYMENTS</span><h1>部署记录.</h1><p>查看构建状态、日志和生产环境发布结果。</p></div>
      <button class="button button--secondary" @click="load"><PhArrowClockwise :size="17" />刷新</button>
    </section>
    <div class="split-workspace">
      <section class="data-panel">
        <header class="data-panel__header"><strong>最近部署</strong><span>{{ deployments.length }} 条记录</span></header>
        <div v-if="loading" class="loading-copy">正在加载部署记录…</div>
        <button
          v-for="deployment in deployments"
          v-else
          :key="deployment.id"
          class="deployment-row"
          :class="{ selected: selected?.id === deployment.id }"
          @click="selected = deployment"
        >
          <StatusBadge :status="deployment.status" />
          <div><strong>{{ deployment.siteName }}</strong><span>{{ deployment.environment }} · {{ deployment.sourceFilename || '项目源文件' }}</span></div>
          <code>{{ deployment.commitHash }}</code>
          <time>{{ formatDate(deployment.createdAt) }}</time>
        </button>
      </section>
      <aside class="inspector-panel">
        <template v-if="selected">
          <header><div><span class="eyebrow">BUILD #{{ selected.id }}</span><h2>{{ selected.siteName }}</h2></div><StatusBadge :status="selected.status" /></header>
          <dl class="inspector-facts">
            <div><dt><PhClock :size="16" />开始时间</dt><dd>{{ formatDate(selected.startedAt) }}</dd></div>
            <div><dt><PhGitBranch :size="16" />来源版本</dt><dd><code>main / {{ selected.commitHash }}</code></dd></div>
            <div><dt><PhTerminalWindow :size="16" />构建耗时</dt><dd>{{ selected.durationSeconds ? `${selected.durationSeconds} 秒` : '进行中' }}</dd></div>
          </dl>
          <div class="build-log">
            <div class="build-log__title"><PhTerminalWindow :size="17" />构建日志</div>
            <pre>{{ selected.buildLog || '正在等待构建日志…' }}</pre>
          </div>
        </template>
        <div v-else class="empty-state"><p>选择一条部署记录查看详情。</p></div>
      </aside>
    </div>
  </AppShell>
</template>

