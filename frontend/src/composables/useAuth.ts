import { computed, reactive } from 'vue'
import { api } from '@/lib/api'
import type { ApiEnvelope, LoginResponse, User } from '@/types'

interface AuthConfig {
  issuer: string
  clientId: string
  scope: string
  redirectUri: string
  devLoginEnabled: boolean
}

const storedUser = localStorage.getItem('site_publish_user')
const adminRoles = new Set(['site_admin', 'admin', 'super_admin'])
const state = reactive<{
  user: User | null
  initialized: boolean
  config: AuthConfig | null
}>({
  user: storedUser ? JSON.parse(storedUser) : null,
  initialized: false,
  config: null,
})

function base64Url(bytes: Uint8Array) {
  let binary = ''
  bytes.forEach((byte) => (binary += String.fromCharCode(byte)))
  return btoa(binary).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '')
}

async function createPkce() {
  const bytes = crypto.getRandomValues(new Uint8Array(32))
  const codeVerifier = base64Url(bytes)
  const digest = await crypto.subtle.digest('SHA-256', new TextEncoder().encode(codeVerifier))
  return { codeVerifier, codeChallenge: base64Url(new Uint8Array(digest)) }
}

function saveLogin(login: LoginResponse) {
  localStorage.setItem('site_publish_token', login.tokenValue)
  localStorage.setItem('site_publish_user', JSON.stringify(login.user))
  state.user = login.user
}

function centralLoginUrl(authorizeUrl: string, issuer: string) {
  const authorize = new URL(authorizeUrl)
  const authority = new URL(issuer)
  const issuerPath = authority.pathname.replace(/\/$/, '')
  const expectedAuthorizePath = `${issuerPath}/oauth/authorize`
  if (authorize.origin !== authority.origin || authorize.pathname !== expectedAuthorizePath) {
    throw new Error('OAuth 授权地址与认证中心配置不一致')
  }
  return `${authority.origin}/login?redirect=${encodeURIComponent(authorize.toString())}`
}

export function useAuth() {
  const initialize = async () => {
    if (state.initialized) return
    try {
      const configResponse = await api.get<ApiEnvelope<AuthConfig>>('/auth/config')
      state.config = configResponse.data.data
      if (localStorage.getItem('site_publish_token')) {
        const response = await api.get<ApiEnvelope<User>>('/me')
        state.user = response.data.data
        localStorage.setItem('site_publish_user', JSON.stringify(state.user))
      }
    } catch {
      state.user = null
    } finally {
      state.initialized = true
    }
  }

  const loginWithOAuth = async () => {
    if (!state.config) await initialize()
    if (!state.config) throw new Error('无法读取 OAuth 配置')
    const redirectUri = `${window.location.origin}/auth/callback`
    const { codeVerifier, codeChallenge } = await createPkce()
    const nonce = crypto.randomUUID()
    const response = await api.post<ApiEnvelope<{ authorizeUrl: string; state: string }>>(
      '/auth/oauth/start',
      { codeChallenge, redirectUri, nonce },
    )
    sessionStorage.setItem('oauth_code_verifier', codeVerifier)
    sessionStorage.setItem('oauth_state', response.data.data.state)
    window.location.assign(centralLoginUrl(response.data.data.authorizeUrl, state.config.issuer))
  }

  const finishOAuth = async (code: string, stateValue: string) => {
    if (stateValue !== sessionStorage.getItem('oauth_state')) {
      throw new Error('OAuth state 校验失败')
    }
    const response = await api.post<ApiEnvelope<LoginResponse>>('/auth/oauth/callback', {
      code,
      state: stateValue,
      codeVerifier: sessionStorage.getItem('oauth_code_verifier'),
      redirectUri: `${window.location.origin}/auth/callback`,
    })
    sessionStorage.removeItem('oauth_code_verifier')
    sessionStorage.removeItem('oauth_state')
    saveLogin(response.data.data)
  }

  const devLogin = async () => {
    const response = await api.post<ApiEnvelope<LoginResponse>>('/auth/dev-login')
    saveLogin(response.data.data)
  }

  const logout = async () => {
    try {
      await api.post('/auth/logout')
    } finally {
      localStorage.removeItem('site_publish_token')
      localStorage.removeItem('site_publish_user')
      state.user = null
    }
  }

  return {
    user: computed(() => state.user),
    config: computed(() => state.config),
    isAuthenticated: computed(() => Boolean(state.user && localStorage.getItem('site_publish_token'))),
    isAdmin: computed(() => Boolean(
      state.user?.roles.some((role) => adminRoles.has(role.trim().toLowerCase())),
    )),
    initialize,
    loginWithOAuth,
    finishOAuth,
    devLogin,
    logout,
  }
}
