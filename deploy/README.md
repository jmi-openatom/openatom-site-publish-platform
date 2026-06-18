# Docker 与 GitHub Actions 部署

生产环境包含：

- `frontend`：Vue 控制台与 `/api`、`/published` 反向代理。
- `backend`：Spring Boot API、Host 路由和网站构建服务，镜像内包含 Node.js 20。
- `mysql`：生产数据库。
- `caddy`：可选公网入口，仅在 `INGRESS_MODE=caddy` 时启动。

MySQL、发布文件和 Caddy 证书均保存在 Docker volume 中，更新容器不会清空数据。

## 一、服务器首次准备

要求服务器已安装 Git、Docker Engine 和 Docker Compose，部署用户拥有执行 Docker
的权限，并且服务器已经配置好读取此 GitHub 仓库的凭据。

```bash
mkdir -p /www/wwwroot
cd /www/wwwroot
git clone git@github.com:ArivenHe/JMI-OPENATOM-Site-Publish-Platform.git site-publish-platform
cd site-publish-platform
cp .env.production.example .env
```

编辑 `.env` 后执行：

```bash
docker compose config --quiet
docker compose up -d --build
docker compose ps
```

### 已有 Nginx、OpenResty 或宝塔

默认使用 `INGRESS_MODE=external`。容器只监听本机：

```text
控制台：127.0.0.1:28081
发布站点：127.0.0.1:28080
```

在现有代理中将控制台域名反代到 `28081`，将站点通配域名反代到 `28080`，
并保留原始 `Host`。示例见
[nginx-external.conf.example](nginx-external.conf.example)。

> **自定义域名注意事项：** CNAME 只负责把域名解析到服务器，不会把 HTTP
> `Host` 改成 CNAME 目标。使用宝塔或共享 Nginx 时，还必须把每个自定义域名
> 加入站点的域名列表，反向代理到 `127.0.0.1:28080`，并传递原始 `Host`。
> 否则请求会落到宝塔默认站点并显示“没有找到站点”。HTTPS 证书也需要在入口
> Web 服务中为该自定义域名配置。

### 服务器没有其他 Web 入口

设置：

```env
INGRESS_MODE=caddy
```

Caddy 将占用 `80/443` 并提供按需 HTTPS。服务器防火墙和云安全组必须开放
TCP `80/443` 与 UDP `443`。

生产服务器上的 Caddy 使用 host 网络，以便继续访问宝塔原有的本机反向代理
上游。`deploy/Caddyfile` 已包含当前 `oauth`、官网、LMS、活动、Bot 等域名路由；
新增宝塔站点时也应同步补充 Caddy 路由。

这是平台“验证域名后自动绑定并申请 SSL”的推荐且完整模式。域名验证通过后，
后端会通过内部 TLS 握手触发 Caddy 签发证书；申请状态会显示在域名管理页面。
证书由 Caddy 保存到 `caddy_data` volume，并自动续期。

后端通过 `host.docker.internal:443` 探测宿主机上的 Caddy。Linux Docker Compose
使用 `host-gateway` 自动建立该地址；不要再把 `SSL_PROXY_HOST` 设置为 `caddy`，
因为 host 网络模式下 Caddy 不在应用的 bridge 网络中。

若服务器当前由宝塔 Nginx 占用 `80/443`，切换前必须先在宝塔停止这两个端口的
监听，然后将 GitHub Environment 变量 `INGRESS_MODE` 改为 `caddy` 并重新部署。
Caddy 会同时承接控制台域名、平台通配域名和已验证的用户自定义域名。不要让
Nginx 与 Caddy 同时监听公网 `80/443`。

## 二、DNS

假设控制台域名是 `publish.example.com`，站点基础域名是 `sites.example.com`：

```text
publish.example.com    A    服务器公网 IP
*.sites.example.com    A    服务器公网 IP
```

用户自定义域名应创建 CNAME：

```text
www.customer.com       CNAME    project-slug.sites.example.com
```

若使用外部 Nginx/宝塔，还需新增：

```nginx
server {
    listen 80;
    server_name www.customer.com;

    location / {
        proxy_pass http://127.0.0.1:28080;
        proxy_set_header Host $host;
        proxy_set_header X-Forwarded-Host $host;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

宝塔中对应操作为：将 `www.customer.com` 添加到网站域名列表，反向代理目标填写
`http://127.0.0.1:28080`，发送域名选择 `$host`，然后为该域名申请证书并重载
Nginx。若希望域名验证后自动接入并自动签发 HTTPS，应使用 Caddy 模式。

当前生产域名 `sites.jmi-openatom.cn` 的宝塔拆分配置可直接参考
[nginx-sites.jmi-openatom.cn.conf](nginx-sites.jmi-openatom.cn.conf)。该配置使用
正则 `server_name` 接住未单独配置的自定义域名 HTTP 请求，但不会用平台通配证书
冒充自定义域名证书。

## 三、GitHub Environment

在仓库 `Settings → Environments` 创建 `SERVER` 环境。

Environment secrets：

| 名称 | 用途 |
| --- | --- |
| `SERVER_HOST` | 服务器地址 |
| `SERVER_USER` | SSH 用户 |
| `SERVER_PORT` | SSH 端口，可省略 |
| `SERVER_PASSWORD` | SSH 密码；与 SSH Key 二选一 |
| `SERVER_SSH_KEY` | SSH 私钥；与密码二选一 |
| `SERVER_FINGERPRINT` | SSH 主机指纹，建议配置 |
| `SERVER_PATH` | 部署目录，默认 `/www/wwwroot/site-publish-platform` |
| `ACME_EMAIL` | HTTPS 证书邮箱 |
| `MYSQL_PASSWORD` | 应用数据库密码 |
| `MYSQL_ROOT_PASSWORD` | MySQL root 密码 |
| `OIDC_CLIENT_SECRET` | OAuth 客户端密钥；公开客户端可留空 |

Environment variables：

| 名称 | 示例 |
| --- | --- |
| `APP_DOMAIN` | `publish.example.com` |
| `INGRESS_MODE` | 已有 Nginx 填 `external`；独占端口填 `caddy` |
| `FRONTEND_BIND_PORT` | `28081` |
| `BACKEND_BIND_PORT` | `28080` |
| `SITE_PUBLIC_BASE_DOMAIN` | `sites.example.com` |
| `SITE_CNAME_BASE_DOMAIN` | `sites.example.com` |
| `AUTO_SSL_ENABLED` | 由部署脚本按入口模式自动设置 |
| `MYSQL_DATABASE` | `site_publish` |
| `MYSQL_USER` | `site_publish` |
| `OIDC_ISSUER` | `https://oauth.jmi-openatom.cn/api/v1` |
| `OIDC_CLIENT_ID` | `site-platform` |
| `BUILD_TIMEOUT_SECONDS` | `180` |

## 四、自动部署

工作流位于 `.github/workflows/deploy.yml`：

1. 所有分支和 PR 运行前端构建、后端测试与 Docker 镜像构建。
2. `main` 分支通过所有检查后，通过 SSH 更新服务器代码。
3. 工作流生成仅服务器可读的 `.env`，构建并更新容器。
4. Caddy 模式检查公网 HTTPS；外部代理模式输出两个本机上游地址。

在 Caddy 模式下，自定义域名流程为：

1. 用户保存自定义域名，并按提示设置 CNAME。
2. 用户点击“验证解析”。
3. 后端激活域名并授权 Caddy 签发该域名证书。
4. 后端自动触发 TLS 握手，页面显示“SSL 申请中”。
5. 签发成功后页面显示“HTTPS 已启用”；失败任务每 5 分钟自动重试。

生产 OAuth 回调地址会自动设置为：

```text
https://APP_DOMAIN/auth/callback
```

该地址必须同步登记到 OpenAtom OAuth 客户端允许列表。

MySQL 密码只会在数据库卷首次创建时初始化。后续若修改 GitHub 中的数据库密码，
需要先在 MySQL 内同步修改对应账号密码，不能只修改 Secret。

## 五、常用运维命令

```bash
docker compose ps
docker compose logs -f --tail=200
docker compose restart backend
docker compose exec mysql mysql -usite_publish -p site_publish
docker compose down
```

`docker compose down` 不会删除数据卷；不要使用 `down -v`，除非确定要清空数据库、发布文件和证书。
