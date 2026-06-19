<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { PhArrowRight, PhCheckCircle, PhCode, PhGlobe, PhSpinnerGap, PhStack } from '@phosphor-icons/vue'
import { useAuth } from '@/composables/useAuth'
import { errorMessage } from '@/lib/api'
import { useToast } from '@/composables/useToast'

const auth = useAuth()
const router = useRouter()
const toast = useToast()
const busy = ref(false)

onMounted(async () => {
  await auth.initialize()
})

async function oauthLogin() {
  busy.value = true
  try {
    await auth.loginWithOAuth()
  } catch (error) {
    toast.show(errorMessage(error), 'error')
    busy.value = false
  }
}

async function devLogin() {
  busy.value = true
  try {
    await auth.devLogin()
    await router.push('/')
  } catch (error) {
    toast.show(errorMessage(error), 'error')
  } finally {
    busy.value = false
  }
}
</script>

<template>
  <main class="login-page">
    <section class="login-intro">
      <div class="login-brand"><img src="../../public/logo.png" width="50" alt="">JMI-OPENATOM 发布平台</div>
      <div class="login-copy">
        <span class="eyebrow">OPENATOM / WEBSITE DELIVERY</span>
        <h1>把网站交付，<br />变成一件小事.</h1>
        <p>上传 HTML、Vue 或 React 项目，平台会替你完成构建、发布和域名管理。</p>
        <ul>
          <li><PhCheckCircle :size="19" weight="fill" />JMI-OPENATOM 统一身份认证</li>
          <li><PhCode :size="19" />自动识别并构建前端项目</li>
          <li><PhGlobe :size="19" />默认域名与自定义 CNAME</li>
        </ul>
      </div>
      <div class="login-gradient" />
    </section>
    <section class="login-form-side">
      <div class="login-card">
        <div class=""><img src="../../public/logo.png" width="50" alt=""></div>
        <h2>登录 发布平台</h2>
        <p>使用你的 JMI-OPENATOM 账号继续。</p>
        <button class="button button--primary button--large button--full" :disabled="busy" @click="oauthLogin">
          <PhSpinnerGap v-if="busy" class="spin" :size="18" />
          使用 JMI-OPENATOM 登录
          <PhArrowRight v-if="!busy" :size="18" />
        </button>
        <small>登录即表示你同意平台仅使用授权范围内的账号信息。</small>
      </div>
    </section>
  </main>
</template>
