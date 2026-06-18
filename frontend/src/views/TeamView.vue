<script setup lang="ts">
import { computed } from 'vue'
import { PhCrown, PhShieldCheck, PhUserCircle, PhUsers } from '@phosphor-icons/vue'
import AppShell from '@/components/AppShell.vue'
import { useAuth } from '@/composables/useAuth'

const auth = useAuth()
const roleText = computed(() => auth.user.value?.roles.includes('site_admin') ? '平台管理员' : '项目成员')
</script>

<template>
  <AppShell>
    <section class="page-heading">
      <div><span class="eyebrow">TEAM</span><h1>团队空间.</h1><p>当前空间成员通过 OpenAtom 账号完成身份认证。</p></div>
    </section>
    <div class="team-grid">
      <section class="team-overview">
        <span class="large-feature-icon"><PhUsers :size="28" /></span>
        <h2>计算机协会</h2>
        <p>用于社团官网、招新专题和内部项目的统一发布空间。</p>
        <dl>
          <div><dt>当前成员</dt><dd>1</dd></div>
          <div><dt>认证方式</dt><dd>OpenAtom OIDC</dd></div>
          <div><dt>项目权限</dt><dd>空间内隔离</dd></div>
        </dl>
      </section>
      <section class="members-panel">
        <header class="data-panel__header"><strong>成员</strong><span>1 位</span></header>
        <article class="member-row">
          <span class="member-avatar"><PhUserCircle :size="32" weight="fill" /></span>
          <div><strong>{{ auth.user.value?.displayName }}</strong><span>{{ auth.user.value?.email }}</span></div>
          <span class="member-role"><PhCrown :size="16" />{{ roleText }}</span>
          <span class="member-security"><PhShieldCheck :size="16" weight="fill" />已认证</span>
        </article>
      </section>
    </div>
  </AppShell>
</template>

