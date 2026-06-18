<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  PhBell,
  PhCaretDown,
  PhCommand,
  PhList,
  PhMagnifyingGlass,
  PhSignOut,
  PhShieldCheck,
  PhSquaresFour,
  PhStack,
  PhX,
} from '@phosphor-icons/vue'
import { useAuth } from '@/composables/useAuth'

const props = withDefaults(
  defineProps<{
    search?: string
    searchPlaceholder?: string
  }>(),
  { search: '', searchPlaceholder: '搜索项目、域名或部署…' },
)

const emit = defineEmits<{ 'update:search': [value: string] }>()
const route = useRoute()
const router = useRouter()
const auth = useAuth()
const userMenuOpen = ref(false)
const mobileMenuOpen = ref(false)

const navigation = computed(() => [
  { label: '项目', to: '/' },
  { label: '部署', to: '/deployments' },
  { label: '域名', to: '/domains' },
  ...(auth.isAdmin.value ? [{ label: '管理', to: '/admin' }] : []),
])

const initials = computed(() => auth.user.value?.displayName?.slice(0, 1) || 'S')

function active(to: string) {
  return to === '/' ? route.path === '/' || route.path.startsWith('/sites/') : route.path.startsWith(to)
}

async function logout() {
  await auth.logout()
  await router.push('/login')
}
</script>

<template>
  <div class="app-shell">
    <header class="topbar">
      <div class="topbar__brand">
        <RouterLink to="/" class="brand-mark" aria-label="Site Publish 首页">
          <span class="brand-icon"><PhStack :size="23" weight="fill" /></span>
          <strong>Site Publish</strong>
        </RouterLink>
      </div>

      <nav class="topbar__nav" aria-label="主导航">
        <RouterLink
          v-for="item in navigation"
          :key="item.to"
          :to="item.to"
          :class="{ active: active(item.to) }"
        >
          {{ item.label }}
        </RouterLink>
      </nav>

      <div class="topbar__actions">
        <label class="global-search">
          <PhMagnifyingGlass :size="17" />
          <input
            :value="props.search"
            :placeholder="searchPlaceholder"
            @input="emit('update:search', ($event.target as HTMLInputElement).value)"
          />
          <span class="shortcut"><PhCommand :size="12" />K</span>
        </label>
        <button class="topbar-icon" aria-label="通知"><PhBell :size="20" /></button>
        <div class="user-menu">
          <button class="user-trigger" @click="userMenuOpen = !userMenuOpen">
            <img v-if="auth.user.value?.avatar" :src="auth.user.value.avatar" alt="" />
            <span v-else class="avatar-fallback">{{ initials }}</span>
            <strong>{{ auth.user.value?.displayName || '用户' }}</strong>
            <PhCaretDown :size="14" />
          </button>
          <Transition name="popover">
            <div v-if="userMenuOpen" class="popover-menu">
              <div class="popover-profile">
                <strong>{{ auth.user.value?.displayName }}</strong>
                <span>{{ auth.user.value?.email }}</span>
              </div>
              <RouterLink v-if="auth.isAdmin.value" to="/admin" @click="userMenuOpen = false">
                <PhShieldCheck :size="17" />管理后台
              </RouterLink>
              <button @click="logout"><PhSignOut :size="17" />退出登录</button>
            </div>
          </Transition>
        </div>
        <button
          class="mobile-menu-button"
          :aria-label="mobileMenuOpen ? '关闭导航菜单' : '打开导航菜单'"
          @click="mobileMenuOpen = !mobileMenuOpen"
        >
          <PhX v-if="mobileMenuOpen" :size="22" />
          <PhList v-else :size="22" />
        </button>
      </div>
    </header>

    <Transition name="mobile-nav">
      <nav v-if="mobileMenuOpen" class="mobile-navigation">
        <RouterLink v-for="item in navigation" :key="item.to" :to="item.to" @click="mobileMenuOpen = false">
          <PhSquaresFour :size="18" />{{ item.label }}
        </RouterLink>
      </nav>
    </Transition>

    <main class="shell-content">
      <slot />
    </main>
  </div>
</template>
