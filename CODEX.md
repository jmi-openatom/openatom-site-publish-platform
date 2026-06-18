下面给你一份**完整 Codex 可直接开工版本（OAuth + 网站发布平台整合版）**，已经帮你把：

- 网站发布平台

- OpenAtom OAuth（OIDC）

- PKCE 登录

- JWT 本地登录体系

- 用户绑定逻辑

- API 结构

- 数据库结构

- 前后端流程

全部统一成**工程级可实现规范**

---

# 🚀 Codex 开发文档：网站发布平台 + OpenAtom OAuth 集成版

---

# 一、项目名称

```text
Site Publish Platform (with OpenAtom OAuth)
```

---

# 二、系统目标

构建一个支持以下能力的平台：

### 🌐 网站发布

- HTML / Vue / React 上传发布

- 模板创建网站

- 自动构建 + 部署

- 默认域名 + CNAME 自定义域名

### 🔐 登录系统

- 使用 OpenAtom OAuth（OIDC）

- PKCE 安全授权

- 自动创建用户

- 本地 JWT 登录体系

---

# 三、系统架构

```text
Frontend (Vue / React)
        ↓
OpenAtom OAuth (OIDC Server)
        ↓
/oauth/authorize
        ↓
/auth/callback (Platform Backend)
        ↓
/oauth/token + /userinfo
        ↓
User Mapping Service
        ↓
Local User DB
        ↓
JWT Token Issued
        ↓
Platform Dashboard
```

---

# 四、OAuth 认证流程（核心）

## 4.1 登录流程

```text
用户点击登录
↓
跳转 OpenAtom OAuth
↓
授权登录
↓
回调 /auth/callback?code=xxx
↓
后端换 token
↓
获取 userinfo
↓
映射本地用户
↓
签发 platform JWT
↓
进入系统
```

---

## 4.2 OAuth 关键参数

- issuer:

```text
https://oauth.jmi-openatom.cn/api/v1
```

- client_id:

```text
site-platform
```

- scope:

```text
openid profile email roles permissions
```

---

# 五、前端实现（Codex 可直接写）

## 5.1 登录按钮

```js
const issuer = "https://oauth.jmi-openatom.cn/api/v1"
const clientId = "site-platform"
const redirectUri = "https://your-domain.com/auth/callback"
```

---

## 5.2 PKCE 生成

```js
function base64Url(buffer) {
  return btoa(String.fromCharCode(...buffer))
    .replace(/\+/g, "-")
    .replace(/\//g, "_")
    .replace(/=+$/, "")
}

async function createPkce() {
  const random = crypto.getRandomValues(new Uint8Array(32))
  const verifier = base64Url(random)

  const digest = await crypto.subtle.digest(
    "SHA-256",
    new TextEncoder().encode(verifier)
  )

  const challenge = base64Url(new Uint8Array(digest))

  return { verifier, challenge }
}
```

---

## 5.3 跳转登录

```js
async function login() {
  const { verifier, challenge } = await createPkce()
  const state = crypto.randomUUID()

  sessionStorage.setItem("verifier", verifier)
  sessionStorage.setItem("state", state)

  const params = new URLSearchParams({
    response_type: "code",
    client_id: clientId,
    redirect_uri: redirectUri,
    scope: "openid profile email roles permissions",
    state,
    code_challenge: challenge,
    code_challenge_method: "S256",
  })

  window.location.href =
    `${issuer}/oauth/authorize?${params.toString()}`
}
```

---

## 5.4 callback 页面

```js
async function callback() {
  const url = new URL(window.location.href)

  const code = url.searchParams.get("code")
  const state = url.searchParams.get("state")

  if (state !== sessionStorage.getItem("state")) {
    throw new Error("state invalid")
  }

  const verifier = sessionStorage.getItem("verifier")

  const res = await fetch(
    "https://oauth.jmi-openatom.cn/api/v1/oauth/token",
    {
      method: "POST",
      headers: {
        "Content-Type": "application/x-www-form-urlencoded",
      },
      body: new URLSearchParams({
        grant_type: "authorization_code",
        client_id: clientId,
        code,
        redirect_uri: redirectUri,
        code_verifier: verifier,
      }),
    }
  )

  const data = await res.json()

  localStorage.setItem("token", data.access_token)
}
```

---

# 六、后端实现（Node/NestJS 可用）

---

## 6.1 OAuth userinfo 获取

```http
GET https://oauth.jmi-openatom.cn/api/v1/oauth/userinfo
Authorization: Bearer ACCESS_TOKEN
```

---

## 6.2 用户映射逻辑（核心）

```js
async function loginWithOAuth(userinfo) {
  const sub = userinfo.sub

  let user = await db.users.findOne({ oauth_sub: sub })

  if (!user) {
    const emailUser = await db.users.findOne({
      email: userinfo.email
    })

    if (emailUser) {
      emailUser.oauth_sub = sub
      user = await db.users.update(emailUser)
    } else {
      user = await db.users.create({
        username: userinfo.username || userinfo.sub,
        email: userinfo.email,
        avatar: userinfo.avatar,
        oauth_sub: sub,
      })
    }
  }

  return user
}
```

---

## 6.3 签发平台 JWT

```js
const token = jwt.sign({
  userId: user.id,
  oauthSub: user.oauth_sub,
  role: "user"
}, "JWT_SECRET", { expiresIn: "7d" })
```

---

# 七、数据库设计

---

## 7.1 users 表

```sql
CREATE TABLE users (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  username TEXT,
  email TEXT,
  password TEXT,

  oauth_sub TEXT UNIQUE,
  avatar TEXT,

  created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

---

## 7.2 sites 表

```sql
CREATE TABLE sites (
  id INTEGER PRIMARY KEY,
  user_id INTEGER,
  name TEXT,
  slug TEXT UNIQUE,
  type TEXT,
  status TEXT,
  default_domain TEXT,
  custom_domain TEXT,
  created_at DATETIME
);
```

---

## 7.3 oauth_bindings（可选）

```sql
CREATE TABLE oauth_bindings (
  id INTEGER PRIMARY KEY,
  user_id INTEGER,
  provider TEXT,
  provider_sub TEXT,
  created_at DATETIME
);
```

---

# 八、后端 API

---

## 8.1 OAuth 回调接口

```http
GET /auth/callback
```

流程：

```text
code → token → userinfo → user mapping → JWT
```

---

## 8.2 当前用户

```http
GET /api/me
Authorization: Bearer token
```

---

## 8.3 网站接口

```http
POST /api/sites
GET /api/sites
DELETE /api/sites/:id
POST /api/sites/:id/deploy
```

---

# 九、安全规范（必须实现）

---

## 9.1 state 校验

```text
防 CSRF 必须
```

---

## 9.2 PKCE

```text
S256 强制使用
```

---

## 9.3 token 分层

```text
OAuth token（外部）
    ↓
Platform JWT（内部）
```

---

## 9.4 user 唯一标识

```text
sub = 唯一用户ID（禁止用 email）
```

---

# 十、完整流程（Codex理解版）

```text
[1] 点击登录
↓
[2] OAuth authorize
↓
[3] callback
↓
[4] token exchange
↓
[5] userinfo
↓
[6] db user mapping
↓
[7] create/login user
↓
[8] issue JWT
↓
[9] 进入 dashboard
```

---

# 十一、MVP开发顺序（强烈建议）

### Phase 1（必须先做）

- OAuth 登录（OpenAtom）

- callback

- JWT 登录

- 用户表

---

### Phase 2

- 网站上传 HTML

- 发布系统

---

### Phase 3

- Vue/React 构建

- Docker builder

---

### Phase 4

- CNAME 域名绑定

- HTTPS

---

# 十二、最终效果

你会得到一个类似：

```text
Vercel + Netlify + GitHub Pages + OAuth SSO
```

的校园/社团级平台。

---

# 如果你下一步要继续，我可以直接帮你生成：

### 🔥 1. NestJS 完整项目结构（可运行）

### 🔥 2. Vue 登录 + Dashboard 全套代码

### 🔥 3. Docker 构建服务（Vue/React 自动部署）

### 🔥 4. Caddy 域名 + HTTPS 自动配置

只要你说一句：

> “给我可运行版本”

我可以直接帮你生成整个工程代码。
