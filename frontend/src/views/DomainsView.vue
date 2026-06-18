<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue'
import { PhArrowClockwise, PhCopy, PhGlobe, PhLockKey, PhPlus, PhShieldCheck } from '@phosphor-icons/vue'
import AppShell from '@/components/AppShell.vue'
import StatusBadge from '@/components/StatusBadge.vue'
import BaseModal from '@/components/BaseModal.vue'
import { api, errorMessage } from '@/lib/api'
import { formatDate } from '@/lib/format'
import { useToast } from '@/composables/useToast'
import type { ApiEnvelope, DomainBinding, Site } from '@/types'

const domains = ref<DomainBinding[]>([])
const sites = ref<Site[]>([])
const modalOpen = ref(false)
const busy = ref(false)
const form = ref({ siteId: '', domain: '' })
const toast = useToast()
let refreshTimer: number | undefined

async function load() {
  try {
    const [domainResponse, siteResponse] = await Promise.all([
      api.get<ApiEnvelope<DomainBinding[]>>('/domains'),
      api.get<ApiEnvelope<Site[]>>('/sites'),
    ])
    domains.value = domainResponse.data.data
    sites.value = siteResponse.data.data
    if (!form.value.siteId && sites.value.length) form.value.siteId = String(sites.value[0].id)
  } catch (error) {
    toast.show(errorMessage(error), 'error')
  }
}

async function bind() {
  if (!form.value.siteId || !form.value.domain) return
  busy.value = true
  try {
    const response = await api.post<ApiEnvelope<DomainBinding>>(
      `/sites/${form.value.siteId}/domains`,
      { domain: form.value.domain },
    )
    toast.show(`请将 CNAME 指向 ${response.data.data.cnameTarget}，验证后平台会自动申请 SSL`)
    modalOpen.value = false
    form.value.domain = ''
    await load()
  } catch (error) {
    toast.show(errorMessage(error), 'error')
  } finally {
    busy.value = false
  }
}

async function verify(domain: DomainBinding) {
  try {
    await api.post(`/domains/${domain.id}/verify`)
    toast.show('域名解析验证通过，正在自动申请 SSL 证书')
    await load()
  } catch (error) {
    toast.show(errorMessage(error), 'error')
  }
}

async function requestSsl(domain: DomainBinding) {
  try {
    await api.post(`/domains/${domain.id}/ssl`)
    toast.show('SSL 证书申请已重新提交')
    await load()
  } catch (error) {
    toast.show(errorMessage(error), 'error')
  }
}

function copy(value: string) {
  navigator.clipboard.writeText(value)
  toast.show('记录值已复制')
}

onMounted(() => {
  load()
  refreshTimer = window.setInterval(load, 5000)
})

onUnmounted(() => {
  if (refreshTimer) window.clearInterval(refreshTimer)
})
</script>

<template>
  <AppShell>
    <section class="page-heading">
      <div><span class="eyebrow">DOMAINS</span><h1>域名管理.</h1><p>验证 CNAME 后自动绑定域名并申请 HTTPS 证书。</p></div>
      <button class="button button--primary" @click="modalOpen = true"><PhPlus :size="17" />添加域名</button>
    </section>

    <section class="domain-panel">
      <header class="data-panel__header"><strong>自定义域名</strong><span>{{ domains.length }} 个绑定</span></header>
      <article v-for="domain in domains" :key="domain.id" class="domain-row">
        <span class="domain-icon"><PhGlobe :size="21" /></span>
        <div class="domain-main">
          <strong>{{ domain.domain }}</strong>
          <span>
            {{ domain.siteName }} · 添加于 {{ formatDate(domain.createdAt) }}
            <template v-if="domain.sslExpiresAt"> · 证书到期 {{ formatDate(domain.sslExpiresAt) }}</template>
          </span>
        </div>
        <div class="domain-statuses">
          <StatusBadge :status="domain.status" />
          <StatusBadge :status="domain.sslStatus" />
        </div>
        <div class="dns-record">
          <span>CNAME</span><code>{{ domain.cnameTarget }}</code>
          <button class="icon-button icon-button--ghost" @click="copy(domain.cnameTarget)"><PhCopy :size="16" /></button>
        </div>
        <button v-if="domain.status !== 'ACTIVE'" class="button button--secondary button--small" @click="verify(domain)">
          <PhShieldCheck :size="16" />验证解析
        </button>
        <a
          v-else-if="domain.sslStatus === 'SSL_ACTIVE'"
          class="button button--secondary button--small"
          :href="`https://${domain.domain}/`"
          target="_blank"
          rel="noopener noreferrer"
        >
          <PhLockKey :size="16" />HTTPS 访问
        </a>
        <button
          v-else-if="domain.sslStatus === 'SSL_FAILED'"
          class="button button--secondary button--small"
          @click="requestSsl(domain)"
        >
          <PhArrowClockwise :size="16" />重试 SSL
        </button>
        <span v-else class="ssl-hint">{{ domain.sslMessage }}</span>
      </article>
      <div v-if="!domains.length" class="empty-state"><h2>还没有自定义域名</h2><p>添加域名后，按照提示配置 CNAME 即可。</p></div>
    </section>

    <BaseModal
      :open="modalOpen"
      title="添加自定义域名"
      description="保存后配置 CNAME，验证通过后平台会自动绑定域名并申请 SSL 证书。"
      @close="modalOpen = false"
    >
      <div class="form-grid">
        <label class="field field--span"><span>绑定项目</span><select v-model="form.siteId"><option v-for="site in sites" :key="site.id" :value="String(site.id)">{{ site.name }}</option></select></label>
        <label class="field field--span"><span>域名</span><input v-model="form.domain" placeholder="www.example.com" /></label>
      </div>
      <template #footer>
        <span class="modal-footer__spacer" />
        <button class="button button--secondary" @click="modalOpen = false">取消</button>
        <button class="button button--primary" :disabled="busy || !form.siteId || !form.domain" @click="bind">保存域名</button>
      </template>
    </BaseModal>
  </AppShell>
</template>
