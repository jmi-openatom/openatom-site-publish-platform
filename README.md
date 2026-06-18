# Site Publish Platform

一个面向校园与社团的网站发布平台。前端采用 Vue 3 Composition API + TypeScript，后端采用 Spring Boot、MyBatis-Plus 和 Sa-Token，并接入 OpenAtom OAuth 2.0 / OIDC + PKCE。

## 已实现

- OpenAtom OIDC 授权码登录、PKCE `S256`、`state` 校验和本地 Sa-Token 会话
- OAuth `sub` 用户映射与本地用户自动创建/绑定
- HTML、ZIP、Vue 3、React 项目上传与异步构建
- 模板创建、重新发布、构建日志和部署历史
- 平台默认访问路径、自定义域名绑定与 DNS 检查
- 项目画廊、部署动态、项目详情、域名管理和团队页面
- H2 本地数据库与 MySQL 生产配置
- 完整中文响应式界面

## 项目结构

```text
SitePublishPlatform/
├── backend/             Spring Boot API 与发布服务
├── frontend/            Vue 3 + TypeScript 控制台
├── deploy/              Caddy 与生产部署文档
├── docker-compose.yml   生产容器编排
├── design-reference/    选定的视觉源图
└── design-qa/           桌面与移动端视觉验收证据
```

## 本地启动

要求：Java 21+、Node.js 20+、npm。

后端默认使用文件型 H2：

```bash
cd backend
DEV_LOGIN_ENABLED=true ./mvnw spring-boot:run
```

前端：

```bash
cd frontend
npm install
npm run dev
```

打开 [http://127.0.0.1:5173](http://127.0.0.1:5173)。本地验证可使用“本地开发登录”；生产环境必须保持 `DEV_LOGIN_ENABLED=false`。

本地发布站点通过控制台同源路径访问，例如
`http://127.0.0.1:5173/published/example/`。平台默认域名也可以直接访问，例如
`http://example.sites.localhost:8080/`；本地后端端口不是 `8080` 时，请同时设置
`VITE_API_TARGET` 和 `VITE_SITE_PORT`。

## CNAME 与自定义域名

生产环境设置：

```bash
SITE_PUBLIC_BASE_DOMAIN=sites.jmi-openatom.cn
SITE_CNAME_BASE_DOMAIN=sites.jmi-openatom.cn
```

DNS 服务商处先添加平台通配解析：

```text
*.sites.jmi-openatom.cn  A  你的服务器公网 IP
```

用户为项目 `site-pfpph` 绑定 `www.example.com` 时，平台会要求创建：

```text
www.example.com  CNAME  site-pfpph.sites.jmi-openatom.cn
```

“验证解析”会读取真实 CNAME 记录，只有目标完全匹配时才激活域名。激活后，
后端会根据 HTTP `Host` 自动找到项目并从域名根路径提供首页、静态资源与 SPA 回退。

HTTPS 推荐使用 [deploy/Caddyfile](deploy/Caddyfile)。Caddy 会调用平台的证书授权接口，
只为已上线的平台域名和已验证的自定义域名签发证书：

```bash
ACME_EMAIL=admin@example.com caddy run --config deploy/Caddyfile
```

生产代理必须保留原始 `Host` 请求头，否则平台无法识别自定义域名。

若后端端口不是 `8080`：

```bash
VITE_API_TARGET=http://127.0.0.1:18080 VITE_SITE_PORT=18080 npm run dev
```

## OpenAtom OAuth 配置

在认证中心注册：

- Client ID：`site-platform`
- 回调地址：生产环境必须与 `OIDC_REDIRECT_URI` 完全一致；本地开发若会同时使用
  `localhost` 和 `127.0.0.1`，应在认证中心同时登记对应的前端回调地址。
  不要填写后端接口 `/api/auth/oauth/callback`
- Scopes：`openid profile email roles permissions`
- Grant Types：`authorization_code refresh_token`
- 浏览器公开客户端不配置 `client_secret`，并使用 PKCE `S256`

当前认证中心未登录时会把用户重定向到 `redirect_uri` 所属站点的
`/login`。本项目会将该跳转兼容转发到
`https://oauth.jmi-openatom.cn/login`，避免第三方应用登录页形成循环。

主要环境变量见 [backend/.env.example](backend/.env.example)。

## 使用 MySQL

先执行：

```bash
mysql -u root -p site_publish < backend/src/main/resources/schema-mysql.sql
```

再启动：

```bash
cd backend
SPRING_PROFILES_ACTIVE=mysql \
MYSQL_URL='jdbc:mysql://localhost:3306/site_publish?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai' \
MYSQL_USERNAME=root \
MYSQL_PASSWORD='你的密码' \
./mvnw spring-boot:run
```

## 构建验证

```bash
cd backend && ./mvnw test
cd frontend && npm run build
```

## 发布存储

默认存放于 `backend/data/storage`：

- `uploads/`：上传的源文件
- `work/`：隔离的临时构建目录
- `published/`：可访问的发布产物

ZIP 会进行目录穿越检查。源码项目构建会执行 `npm install` 与 `npm run build`；生产环境建议进一步将构建任务放进受限容器或独立构建节点。

## Docker 与 CI/CD

生产环境可直接使用：

```bash
cp .env.production.example .env
docker compose up -d --build
```

GitHub Actions 会在所有分支执行前端、后端和 Docker 构建检查，并在 `main`
分支检查通过后通过 SSH 自动部署到服务器。完整的服务器准备、DNS 和 GitHub
Environment 配置见 [deploy/README.md](deploy/README.md)。
