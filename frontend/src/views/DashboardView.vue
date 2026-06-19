<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import {
  PhCaretDown,
  PhFolder,
  PhFunnel,
  PhGridFour,
  PhListBullets,
  PhPlus,
  PhSortAscending,
} from '@phosphor-icons/vue'
import AppShell from '@/components/AppShell.vue'
import SiteCard from '@/components/SiteCard.vue'
import DeploymentRail from '@/components/DeploymentRail.vue'
import CreateSiteModal from '@/components/CreateSiteModal.vue'
import BaseModal from '@/components/BaseModal.vue'
import { api, errorMessage } from '@/lib/api'
import { useToast } from '@/composables/useToast'
import type { ApiEnvelope, Deployment, Site } from '@/types'

const toast = useToast()
const sites = ref<Site[]>([])
const deployments = ref<Deployment[]>([])
const loading = ref(true)
const search = ref('')
const status = ref('')
const framework = ref('')
const createOpen = ref(false)
const deleteTarget = ref<Site | null>(null)
const busy = ref(false)
let pollTimer: number | undefined

const filteredSites = computed(() => {
  const query = search.value.trim().toLowerCase()
  return sites.value.filter((site) => {
    const searchMatches = !query || [site.name, site.slug, site.customDomain, site.defaultDomain]
      .some((value) => value?.toLowerCase().includes(query))
    return searchMatches
      && (!status.value || site.status === status.value)
      && (!framework.value || site.framework === framework.value)
  })
})

async function load(silent = false) {
  if (!silent) loading.value = true
  try {
    const [siteResponse, deploymentResponse] = await Promise.all([
      api.get<ApiEnvelope<Site[]>>('/sites'),
      api.get<ApiEnvelope<Deployment[]>>('/deployments/recent?limit=12'),
    ])
    sites.value = siteResponse.data.data
    deployments.value = deploymentResponse.data.data
  } catch (error) {
    toast.show(errorMessage(error), 'error')
  } finally {
    loading.value = false
  }
}

function copy(value: string) {
  navigator.clipboard.writeText(value)
  toast.show('访问地址已复制')
}

async function redeploy(site: Site) {
  try {
    await api.post(`/sites/${site.id}/redeploy`)
    toast.show(`${site.name} 已进入重新发布队列`)
    await load(true)
  } catch (error) {
    toast.show(errorMessage(error), 'error')
  }
}

async function removeSite() {
  if (!deleteTarget.value) return
  busy.value = true
  try {
    await api.delete(`/sites/${deleteTarget.value.id}`)
    toast.show('项目已删除')
    deleteTarget.value = null
    await load(true)
  } catch (error) {
    toast.show(errorMessage(error), 'error')
  } finally {
    busy.value = false
  }
}

onMounted(() => {
  load()
  pollTimer = window.setInterval(() => load(true), 5000)
})
onUnmounted(() => window.clearInterval(pollTimer))
</script>

<template>
  <AppShell v-model:search="search">
    <section class="dashboard-hero">
      <div>
        <h1>你的项目.</h1>
      </div>
      <button class="button button--blue button--large" @click="createOpen = true">
        新建项目<PhPlus :size="19" weight="bold" />
      </button>
    </section>
    <br>

    <div class="dashboard-layout">
      <section class="projects-panel">
        <div class="project-toolbar">
          <div class="toolbar-filters">
            <button class="select-button"><PhFolder :size="17" />全部项目<PhCaretDown :size="14" /></button>
            <label class="select-control">
              <PhFunnel :size="17" />
              <select v-model="status">
                <option value="">全部状态</option>
                <option value="ONLINE">已上线</option>
                <option value="BUILDING">构建中</option>
                <option value="FAILED">部署失败</option>
                <option value="PENDING">待配置</option>
              </select>
              <PhCaretDown :size="14" />
            </label>
            <label class="select-control">
              <PhGridFour :size="17" />
              <select v-model="framework">
                <option value="">全部框架</option>
                <option>Vue 3</option>
                <option>React</option>
                <option>HTML</option>
              </select>
              <PhCaretDown :size="14" />
            </label>
          </div>
          <div class="toolbar-layout">
            <button class="select-button"><PhSortAscending :size="17" />排序：最近部署<PhCaretDown :size="14" /></button>
            <button class="view-button active" aria-label="网格视图"><PhGridFour :size="18" weight="fill" /></button>
            <button class="view-button" aria-label="列表视图"><PhListBullets :size="20" /></button>
          </div>
        </div>

        <div v-if="loading" class="project-grid">
          <div v-for="item in 4" :key="item" class="site-card skeleton-card" />
        </div>
        <div v-else-if="filteredSites.length" class="project-grid">
          <SiteCard
            v-for="site in filteredSites"
            :key="site.id"
            :site="site"
            @open="$router.push(`/sites/${$event.id}`)"
            @copy="copy"
            @redeploy="redeploy"
            @delete="deleteTarget = $event"
          />
        </div>
        <div v-else class="empty-state">
          <h2>没有符合条件的项目</h2>
          <p>调整筛选条件，或者创建一个新项目。</p>
          <button class="button button--primary" @click="createOpen = true"><PhPlus :size="17" />新建项目</button>
        </div>
        <footer class="projects-count">共 {{ filteredSites.length }} 个项目</footer>
      </section>

      <DeploymentRail :deployments="deployments" />
    </div>

    <CreateSiteModal :open="createOpen" @close="createOpen = false" @created="load(true)" />
    <BaseModal
      :open="Boolean(deleteTarget)"
      title="删除项目"
      :description="`确认删除“${deleteTarget?.name || ''}”？项目配置和部署记录会一并移除。`"
      @close="deleteTarget = null"
    >
      <div class="danger-callout">此操作不可撤销，已发布文件不会再通过当前项目访问。</div>
      <template #footer>
        <span class="modal-footer__spacer" />
        <button class="button button--secondary" @click="deleteTarget = null">取消</button>
        <button class="button button--danger" :disabled="busy" @click="removeSite">确认删除</button>
      </template>
    </BaseModal>
  </AppShell>
</template>
