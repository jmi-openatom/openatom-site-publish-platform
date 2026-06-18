# Docker 与 GitHub Actions 部署

生产环境由四个容器组成：

- `caddy`：唯一公网入口，占用 `80/443`，自动签发 HTTPS 证书。
- `frontend`：Vue 控制台与 `/api`、`/published` 反向代理。
- `backend`：Spring Boot API、Host 路由和网站构建服务，镜像内包含 Node.js 20。
- `mysql`：生产数据库。

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

服务器防火墙和云安全组必须开放 TCP `80/443` 与 UDP `443`。

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
| `SITE_PUBLIC_BASE_DOMAIN` | `sites.example.com` |
| `SITE_CNAME_BASE_DOMAIN` | `sites.example.com` |
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
4. 部署完成后检查后端、前端、Caddy 配置和公网 HTTPS 健康接口。

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
