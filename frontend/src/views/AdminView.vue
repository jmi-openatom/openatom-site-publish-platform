<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import {
  PhArrowClockwise,
  PhFolderOpen,
  PhGlobe,
  PhMagnifyingGlass,
  PhRocketLaunch,
  PhShieldCheck,
  PhTrash,
  PhUsers,
} from '@phosphor-icons/vue'
import AppShell from '@/components/AppShell.vue'
import BaseModal from '@/components/BaseModal.vue'
import StatusBadge from '@/components/StatusBadge.vue'
import { api, errorMessage } from '@/lib/api'
import { formatDate } from '@/lib/format'
import { useToast } from '@/composables/useToast'
import type {
  AdminDashboard,
  AdminDeployment,
  AdminDomain,
  AdminSite,
  AdminUser,
  ApiEnvelope,
} from '@/types'

type Section = 'users' | 'sites' | 'domains' | 'deployments'

const toast = useToast()
const dashboard = ref<AdminDashboard | null>(null)
const loading = ref(true)
const busyUserId = ref<number | null>(null)
const deleteTarget = ref<AdminSite | null>(null)
const deleting = ref(false)
const activeSection = ref<Section>('users')
const search = ref('')

const normalizedSearch = computed(() => search.value.trim().toLowerCase())

function matches(values: Array<string | number | undefined>) {
  const query = normalizedSearch.value
  return !query || values.some((value) => String(value || '').toLowerCase().includes(query))
}

const filteredUsers = computed(() => (dashboard.value?.users || []).filter((user) =>
  matches([user.displayName, user.username, user.email, ...user.roles]),
))

const filteredSites = computed(() => (dashboard.value?.sites || []).filter((site) =>
  matches([site.name, site.slug, site.framework, site.ownerName, site.ownerEmail, site.customDomain, site.defaultDomain]),
))

const filteredDomains = computed(() => (dashboard.value?.domains || []).filter((domain) =>
  matches([domain.domain, domain.siteName, domain.ownerName, domain.status, domain.sslStatus]),
))

const filteredDeployments = computed(() => (dashboard.value?.deployments || []).filter((deployment) =>
  matches([deployment.siteName, deployment.ownerName, deployment.commitHash, deployment.sourceFilename, deployment.status]),
))

async function load(silent = false) {
  if (!silent) loading.value = true
  try {
    const response = await api.get<ApiEnvelope<AdminDashboard>>('/admin/dashboard')
    dashboard.value = response.data.data
  } catch (error) {
    toast.show(errorMessage(error), 'error')
  } finally {
    loading.value = false
  }
}

async function setAdmin(user: AdminUser, enabled: boolean) {
  busyUserId.value = user.id
  try {
    const response = await api.put<ApiEnvelope<AdminUser>>(`/admin/users/${user.id}/admin`, { enabled })
    const index = dashboard.value?.users.findIndex((item) => item.id === user.id) ?? -1
    if (dashboard.value && index >= 0) dashboard.value.users[index] = response.data.data
    toast.show(enabled ? '已授予管理员权限' : '已取消管理员权限')
  } catch (error) {
    toast.show(errorMessage(error), 'error')
  } finally {
    busyUserId.value = null
  }
}

async function removeSite() {
  if (!deleteTarget.value) return
  deleting.value = true
  try {
    await api.delete(`/admin/sites/${deleteTarget.value.id}`)
    toast.show(`项目“${deleteTarget.value.name}”已删除`)
    deleteTarget.value = null
    await load(true)
  } catch (error) {
    toast.show(errorMessage(error), 'error')
  } finally {
    deleting.value = false
  }
}

function ownerInitial(name?: string) {
  return name?.trim().slice(0, 1) || '用'
}

function setSection(section: Section) {
  activeSection.value = section
  search.value = ''
}

onMounted(load)
</script>

<template>
  <AppShell>
    <div class="admin-page">
      <section class="page-heading admin-heading">
        <div>
          <span class="eyebrow">ADMIN CONSOLE</span>
          <h1>管理后台.</h1>
          <p>集中管理平台用户、项目、域名与部署记录。</p>
        </div>
        <button class="button button--secondary" :disabled="loading" @click="load()">
          <PhArrowClockwise :size="17" />刷新数据
        </button>
      </section>

      <section class="admin-metrics" aria-label="平台概览">
        <article class="admin-metric">
          <span class="admin-metric__icon"><PhUsers :size="21" /></span>
          <div><strong>{{ dashboard?.summary.users ?? '—' }}</strong><span>平台用户</span></div>
        </article>
        <article class="admin-metric">
          <span class="admin-metric__icon"><PhFolderOpen :size="21" /></span>
          <div><strong>{{ dashboard?.summary.sites ?? '—' }}</strong><span>全部项目</span></div>
          <small>{{ dashboard?.summary.onlineSites ?? 0 }} 个已上线</small>
        </article>
        <article class="admin-metric">
          <span class="admin-metric__icon"><PhRocketLaunch :size="21" /></span>
          <div><strong>{{ dashboard?.summary.deployments ?? '—' }}</strong><span>累计部署</span></div>
          <small>{{ dashboard?.summary.successfulDeployments ?? 0 }} 次成功</small>
        </article>
        <article class="admin-metric">
          <span class="admin-metric__icon"><PhGlobe :size="21" /></span>
          <div><strong>{{ dashboard?.summary.domains ?? '—' }}</strong><span>自定义域名</span></div>
          <small>{{ dashboard?.summary.activeDomains ?? 0 }} 个已生效</small>
        </article>
      </section>

      <section class="admin-workspace">
        <header class="admin-workspace__toolbar">
          <nav class="admin-tabs" aria-label="管理分类">
            <button :class="{ active: activeSection === 'users' }" @click="setSection('users')">
              用户 <span>{{ dashboard?.users.length ?? 0 }}</span>
            </button>
            <button :class="{ active: activeSection === 'sites' }" @click="setSection('sites')">
              项目 <span>{{ dashboard?.sites.length ?? 0 }}</span>
            </button>
            <button :class="{ active: activeSection === 'domains' }" @click="setSection('domains')">
              域名 <span>{{ dashboard?.domains.length ?? 0 }}</span>
            </button>
            <button :class="{ active: activeSection === 'deployments' }" @click="setSection('deployments')">
              部署 <span>{{ dashboard?.deployments.length ?? 0 }}</span>
            </button>
          </nav>
          <label class="admin-search">
            <PhMagnifyingGlass :size="17" />
            <input v-model="search" :placeholder="`搜索${activeSection === 'users' ? '用户' : activeSection === 'sites' ? '项目' : activeSection === 'domains' ? '域名' : '部署'}…`">
          </label>
        </header>

        <div v-if="loading" class="loading-copy admin-loading">正在加载管理数据…</div>

        <div v-else class="admin-table-wrap">
          <table v-if="activeSection === 'users'" class="admin-table">
            <thead><tr><th>用户</th><th>角色</th><th>项目</th><th>加入时间</th><th class="align-right">权限操作</th></tr></thead>
            <tbody>
              <tr v-for="user in filteredUsers" :key="user.id">
                <td>
                  <div class="admin-identity">
                    <img v-if="user.avatar" :src="user.avatar" alt="">
                    <span v-else>{{ ownerInitial(user.displayName || user.username) }}</span>
                    <div><strong>{{ user.displayName || user.username || `用户 #${user.id}` }}</strong><small>{{ user.email || '未填写邮箱' }}</small></div>
                  </div>
                </td>
                <td>
                  <span v-if="user.admin" class="admin-role-badge"><PhShieldCheck :size="14" weight="fill" />管理员</span>
                  <span v-else class="muted-copy">普通成员</span>
                </td>
                <td>{{ user.siteCount }} 个</td>
                <td>{{ formatDate(user.createdAt) }}</td>
                <td class="align-right">
                  <button
                    v-if="user.id === dashboard?.currentUserId"
                    class="button button--secondary button--small"
                    disabled
                  >当前管理员</button>
                  <button
                    v-else
                    class="button button--small"
                    :class="user.admin ? 'button--secondary' : 'button--primary'"
                    :disabled="busyUserId === user.id"
                    @click="setAdmin(user, !user.admin)"
                  >{{ user.admin ? '取消管理员' : '设为管理员' }}</button>
                </td>
              </tr>
            </tbody>
          </table>

          <table v-else-if="activeSection === 'sites'" class="admin-table">
            <thead><tr><th>项目</th><th>所有者</th><th>框架</th><th>状态</th><th>访问域名</th><th>更新时间</th><th></th></tr></thead>
            <tbody>
              <tr v-for="site in filteredSites" :key="site.id">
                <td><div class="admin-primary-cell"><strong>{{ site.name }}</strong><code>{{ site.slug }}</code></div></td>
                <td><div class="admin-primary-cell"><strong>{{ site.ownerName }}</strong><small>{{ site.ownerEmail || `用户 #${site.ownerId}` }}</small></div></td>
                <td><span class="framework-badge">{{ site.framework }}</span></td>
                <td><StatusBadge :status="site.status" /></td>
                <td><code class="admin-domain-code">{{ site.customDomain || site.defaultDomain }}</code></td>
                <td>{{ formatDate(site.updatedAt) }}</td>
                <td class="align-right"><button class="icon-button admin-delete" aria-label="删除项目" @click="deleteTarget = site"><PhTrash :size="17" /></button></td>
              </tr>
            </tbody>
          </table>

          <table v-else-if="activeSection === 'domains'" class="admin-table">
            <thead><tr><th>域名</th><th>项目</th><th>所有者</th><th>解析状态</th><th>HTTPS</th><th>更新时间</th></tr></thead>
            <tbody>
              <tr v-for="domain in filteredDomains" :key="domain.id">
                <td><div class="admin-primary-cell"><strong>{{ domain.domain }}</strong><code>CNAME {{ domain.cnameTarget }}</code></div></td>
                <td>{{ domain.siteName || `项目 #${domain.siteId}` }}</td>
                <td>{{ domain.ownerName }}</td>
                <td><StatusBadge :status="domain.status" /></td>
                <td><div class="admin-status-stack"><StatusBadge :status="domain.sslStatus" /><small>{{ domain.sslMessage }}</small></div></td>
                <td>{{ formatDate(domain.updatedAt) }}</td>
              </tr>
            </tbody>
          </table>

          <table v-else class="admin-table">
            <thead><tr><th>部署</th><th>项目</th><th>所有者</th><th>来源</th><th>状态</th><th>耗时</th><th>创建时间</th></tr></thead>
            <tbody>
              <tr v-for="deployment in filteredDeployments" :key="deployment.id">
                <td><code>#{{ deployment.id }}</code></td>
                <td>{{ deployment.siteName || `项目 #${deployment.siteId}` }}</td>
                <td>{{ deployment.ownerName }}</td>
                <td><div class="admin-primary-cell"><code>{{ deployment.commitHash || '—' }}</code><small>{{ deployment.sourceFilename || '项目源文件' }}</small></div></td>
                <td><StatusBadge :status="deployment.status" /></td>
                <td>{{ deployment.durationSeconds ? `${deployment.durationSeconds} 秒` : '—' }}</td>
                <td>{{ formatDate(deployment.createdAt) }}</td>
              </tr>
            </tbody>
          </table>

          <div
            v-if="(activeSection === 'users' && !filteredUsers.length)
              || (activeSection === 'sites' && !filteredSites.length)
              || (activeSection === 'domains' && !filteredDomains.length)
              || (activeSection === 'deployments' && !filteredDeployments.length)"
            class="empty-state admin-empty"
          >
            <h2>没有找到匹配记录</h2>
            <p>换一个关键词再试试。</p>
          </div>
        </div>
      </section>
    </div>

    <BaseModal
      :open="Boolean(deleteTarget)"
      title="删除用户项目"
      :description="`确认删除“${deleteTarget?.name || ''}”？该项目的域名绑定与部署记录也会一并删除。`"
      @close="deleteTarget = null"
    >
      <div class="danger-callout">
        这是管理员操作且不可撤销。项目所有者将无法再访问该项目。
      </div>
      <template #footer>
        <span class="modal-footer__spacer" />
        <button class="button button--secondary" @click="deleteTarget = null">取消</button>
        <button class="button button--danger" :disabled="deleting" @click="removeSite">确认删除</button>
      </template>
    </BaseModal>
  </AppShell>
</template>
