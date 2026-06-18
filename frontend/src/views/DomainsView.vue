<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { PhCheckCircle, PhCopy, PhGlobe, PhPlus, PhShieldCheck } from '@phosphor-icons/vue'
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
    toast.show(`请将 CNAME 指向 ${response.data.data.cnameTarget}`)
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
    toast.show('域名解析验证通过，请确认入口代理已接入该域名')
    await load()
  } catch (error) {
    toast.show(errorMessage(error), 'error')
  }
}

function copy(value: string) {
  navigator.clipboard.writeText(value)
  toast.show('记录值已复制')
}

onMounted(load)
</script>

<template>
  <AppShell>
    <section class="page-heading">
      <div><span class="eyebrow">DOMAINS</span><h1>域名管理.</h1><p>为项目配置自定义域名，并检查 DNS 解析状态。</p></div>
      <button class="button button--primary" @click="modalOpen = true"><PhPlus :size="17" />添加域名</button>
    </section>

    <section class="domain-panel">
      <header class="data-panel__header"><strong>自定义域名</strong><span>{{ domains.length }} 个绑定</span></header>
      <article v-for="domain in domains" :key="domain.id" class="domain-row">
        <span class="domain-icon"><PhGlobe :size="21" /></span>
        <div class="domain-main"><strong>{{ domain.domain }}</strong><span>{{ domain.siteName }} · 添加于 {{ formatDate(domain.createdAt) }}</span></div>
        <StatusBadge :status="domain.status" />
        <div class="dns-record">
          <span>CNAME</span><code>{{ domain.cnameTarget }}</code>
          <button class="icon-button icon-button--ghost" @click="copy(domain.cnameTarget)"><PhCopy :size="16" /></button>
        </div>
        <button v-if="domain.status !== 'ACTIVE'" class="button button--secondary button--small" @click="verify(domain)">
          <PhShieldCheck :size="16" />验证解析
        </button>
        <span v-else class="verified-label"><PhCheckCircle :size="17" weight="fill" />解析正常</span>
      </article>
      <div v-if="!domains.length" class="empty-state"><h2>还没有自定义域名</h2><p>添加域名后，按照提示配置 CNAME 即可。</p></div>
    </section>

    <BaseModal
      :open="modalOpen"
      title="添加自定义域名"
      description="保存后请配置 CNAME；使用宝塔或外部 Nginx 时，还需在入口代理中绑定该域名。"
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
