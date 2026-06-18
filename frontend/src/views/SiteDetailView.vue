<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  PhArrowLeft,
  PhArrowSquareOut,
  PhCopy,
  PhFileArrowUp,
  PhGlobe,
  PhRocketLaunch,
  PhSpinnerGap,
  PhTrash,
} from '@phosphor-icons/vue'
import AppShell from '@/components/AppShell.vue'
import StatusBadge from '@/components/StatusBadge.vue'
import BaseModal from '@/components/BaseModal.vue'
import { api, errorMessage } from '@/lib/api'
import { formatDate } from '@/lib/format'
import { siteVisitUrl } from '@/lib/siteUrl'
import { useToast } from '@/composables/useToast'
import type { ApiEnvelope, Deployment, Site } from '@/types'

const route = useRoute()
const router = useRouter()
const toast = useToast()
const site = ref<Site | null>(null)
const deployments = ref<Deployment[]>([])
const file = ref<File | null>(null)
const uploadBusy = ref(false)
const deleteOpen = ref(false)
const domainOpen = ref(false)
const domain = ref('')
let pollTimer: number | undefined

const activeUrl = computed(() => {
  if (!site.value) return '#'
  return siteVisitUrl(site.value)
})
const displayAddress = computed(() => site.value?.customDomain || site.value?.defaultDomain || '')
const cnameTarget = computed(() => site.value?.cnameTarget || site.value?.defaultDomain || '')

async function load() {
  const id = route.params.id
  try {
    const [siteResponse, deploymentResponse] = await Promise.all([
      api.get<ApiEnvelope<Site>>(`/sites/${id}`),
      api.get<ApiEnvelope<Deployment[]>>(`/sites/${id}/deployments`),
    ])
    site.value = siteResponse.data.data
    deployments.value = deploymentResponse.data.data
    domain.value = site.value.customDomain || ''
  } catch (error) {
    toast.show(errorMessage(error), 'error')
  }
}

function chooseFile(event: Event) {
  file.value = (event.target as HTMLInputElement).files?.[0] || null
}

async function upload() {
  if (!file.value || !site.value) return
  uploadBusy.value = true
  try {
    const data = new FormData()
    data.append('file', file.value)
    await api.post(`/sites/${site.value.id}/deploy`, data)
    toast.show('项目已上传，正在构建')
    file.value = null
    await load()
  } catch (error) {
    toast.show(errorMessage(error), 'error')
  } finally {
    uploadBusy.value = false
  }
}

async function redeploy() {
  if (!site.value) return
  try {
    await api.post(`/sites/${site.value.id}/redeploy`)
    toast.show('已开始重新发布')
    await load()
  } catch (error) {
    toast.show(errorMessage(error), 'error')
  }
}

async function saveDomain() {
  if (!site.value || !domain.value) return
  try {
    await api.post(`/sites/${site.value.id}/domains`, { domain: domain.value })
    toast.show('域名已保存，请完成 CNAME 配置')
    domainOpen.value = false
    await load()
  } catch (error) {
    toast.show(errorMessage(error), 'error')
  }
}

async function remove() {
  if (!site.value) return
  try {
    await api.delete(`/sites/${site.value.id}`)
    toast.show('项目已删除')
    await router.push('/')
  } catch (error) {
    toast.show(errorMessage(error), 'error')
  }
}

function copy(value: string) {
  navigator.clipboard.writeText(value)
  toast.show('已复制')
}

onMounted(() => {
  load()
  pollTimer = window.setInterval(load, 4000)
})
onUnmounted(() => window.clearInterval(pollTimer))
</script>

<template>
  <AppShell>
    <template v-if="site">
      <section class="site-detail-heading">
        <button class="back-link" @click="$router.push('/')"><PhArrowLeft :size="17" />返回项目</button>
        <div class="site-detail-title">
          <div><span class="eyebrow">PROJECT / {{ site.slug }}</span><h1>{{ site.name }}.</h1><p>{{ site.description || '暂无项目说明' }}</p></div>
          <div class="site-detail-actions">
            <a v-if="site.status === 'ONLINE'" class="button button--secondary" :href="activeUrl" target="_blank"><PhArrowSquareOut :size="17" />访问站点</a>
            <button class="button button--primary" @click="redeploy"><PhRocketLaunch :size="17" />重新发布</button>
          </div>
        </div>
      </section>

      <div class="site-detail-grid">
        <section class="site-overview-panel">
          <div class="detail-preview"><img :src="site.previewImage || '/previews/mirror.png'" :alt="`${site.name} 预览`" /></div>
          <div class="overview-facts">
            <div><span>当前状态</span><StatusBadge :status="site.status" /></div>
            <div><span>框架</span><strong>{{ site.framework }}</strong></div>
            <div><span>{{ site.customDomain ? '自定义域名' : '默认域名' }}</span><button class="inline-copy" @click="copy(displayAddress)">{{ displayAddress }}<PhCopy :size="14" /></button></div>
            <div><span>打开已发布站点</span><a :href="activeUrl" target="_blank">{{ activeUrl }}</a></div>
          </div>
          <div class="detail-section">
            <header><div><h2>上传新版本</h2><p>支持静态 HTML、ZIP 构建产物或 Vue / React 源码。</p></div></header>
            <label class="compact-upload">
              <PhFileArrowUp :size="24" />
              <span><strong>{{ file?.name || '选择项目文件' }}</strong><small>HTML / HTM / ZIP，最大 100 MB</small></span>
              <input type="file" accept=".html,.htm,.zip" @change="chooseFile" />
            </label>
            <button class="button button--primary" :disabled="!file || uploadBusy" @click="upload">
              <PhSpinnerGap v-if="uploadBusy" class="spin" :size="17" />上传并发布
            </button>
          </div>
          <div class="detail-section danger-zone">
            <header><div><h2>危险操作</h2><p>删除项目配置、域名绑定和全部部署记录。</p></div><button class="button button--danger" @click="deleteOpen = true"><PhTrash :size="17" />删除项目</button></header>
          </div>
        </section>

        <aside class="site-side-panel">
          <section>
            <header><h2>域名</h2><button @click="domainOpen = true">配置</button></header>
            <div class="domain-summary">
              <PhGlobe :size="19" />
              <div>
                <strong>{{ activeUrl }}</strong>
                <span>{{ site.customDomain ? '自定义域名访问地址' : '平台访问地址' }}</span>
              </div>
              <button class="domain-copy" title="复制访问地址" @click="copy(activeUrl)"><PhCopy :size="15" /></button>
            </div>
            <div class="cname-summary">
              <div class="cname-summary__label"><span>CNAME</span><small>自定义域名请指向</small></div>
              <button title="复制 CNAME 目标" @click="copy(cnameTarget)">
                <code>{{ cnameTarget }}</code><PhCopy :size="15" />
              </button>
            </div>
          </section>
          <section>
            <header><h2>部署记录</h2><RouterLink to="/deployments">查看全部</RouterLink></header>
            <article v-for="deployment in deployments.slice(0, 6)" :key="deployment.id" class="mini-deployment">
              <StatusBadge :status="deployment.status" />
              <div><strong>{{ deployment.commitHash }}</strong><span>{{ formatDate(deployment.createdAt) }}</span></div>
            </article>
          </section>
        </aside>
      </div>
    </template>

    <BaseModal :open="domainOpen" title="配置自定义域名" description="保存后请配置 CNAME；使用宝塔或外部 Nginx 时，还需在入口代理中绑定该域名。" @close="domainOpen = false">
      <label class="field"><span>自定义域名</span><input v-model="domain" placeholder="www.example.com" /></label>
      <div class="cname-callout">
        <span>DNS 记录类型：CNAME</span>
        <button title="复制 CNAME 目标" @click="copy(cnameTarget)"><code>{{ cnameTarget }}</code><PhCopy :size="15" /></button>
      </div>
      <template #footer><span class="modal-footer__spacer" /><button class="button button--secondary" @click="domainOpen = false">取消</button><button class="button button--primary" @click="saveDomain">保存</button></template>
    </BaseModal>
    <BaseModal :open="deleteOpen" title="删除项目" :description="`确认永久删除“${site?.name || ''}”？`" @close="deleteOpen = false">
      <div class="danger-callout">此操作不可撤销。</div>
      <template #footer><span class="modal-footer__spacer" /><button class="button button--secondary" @click="deleteOpen = false">取消</button><button class="button button--danger" @click="remove">确认删除</button></template>
    </BaseModal>
  </AppShell>
</template>
