<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { PhCheckCircle, PhSpinnerGap, PhWarningCircle } from '@phosphor-icons/vue'
import { useAuth } from '@/composables/useAuth'
import { errorMessage } from '@/lib/api'

const route = useRoute()
const router = useRouter()
const auth = useAuth()
const state = ref<'loading' | 'success' | 'error'>('loading')
const message = ref('正在完成 OpenAtom 登录…')

onMounted(async () => {
  const code = String(route.query.code || '')
  const oauthState = String(route.query.state || '')
  if (!code || !oauthState) {
    state.value = 'error'
    message.value = 'OAuth 回调缺少授权码或 state'
    return
  }
  try {
    await auth.finishOAuth(code, oauthState)
    state.value = 'success'
    message.value = '登录成功，正在进入控制台…'
    window.setTimeout(() => router.replace('/'), 600)
  } catch (error) {
    state.value = 'error'
    message.value = errorMessage(error)
  }
})
</script>

<template>
  <main class="callback-page">
    <section class="callback-card">
      <PhSpinnerGap v-if="state === 'loading'" class="spin" :size="34" />
      <PhCheckCircle v-else-if="state === 'success'" class="callback-success" :size="34" weight="fill" />
      <PhWarningCircle v-else class="callback-error" :size="34" weight="fill" />
      <h1>{{ state === 'error' ? '登录没有完成' : '正在验证身份' }}</h1>
      <p>{{ message }}</p>
      <RouterLink v-if="state === 'error'" class="button button--primary" to="/login">返回登录</RouterLink>
    </section>
  </main>
</template>

