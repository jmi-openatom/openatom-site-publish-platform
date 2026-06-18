MERGE INTO site_users (id, username, display_name, email, avatar, oauth_sub, roles, created_at, updated_at)
KEY(id) VALUES (1, 'dev', '林同学', 'dev@jmi-openatom.cn', NULL, 'dev-user', 'member,site_admin', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

MERGE INTO sites (id, user_id, name, slug, framework, description, status, default_domain, custom_domain, preview_image, branch_name, source_filename, source_path, latest_deployment_id, created_at, updated_at)
KEY(id) VALUES
(1, 1, '实验室官网', 'lab', 'Vue 3', '计算机协会实验室门户与项目展示站点', 'ONLINE', 'lab.sites.localhost', 'lab.csucc.club', '/previews/lab.png', 'main', 'lab-site.zip', NULL, 1, DATEADD('DAY', -31, CURRENT_TIMESTAMP), DATEADD('MINUTE', -18, CURRENT_TIMESTAMP)),
(2, 1, '招新专题页', 'join', 'React', '2026 秋季招新活动专题页面', 'ONLINE', 'join.sites.localhost', 'join.csucc.club', '/previews/recruit.png', 'main', 'recruit.zip', NULL, 2, DATEADD('DAY', -22, CURRENT_TIMESTAMP), DATEADD('HOUR', -4, CURRENT_TIMESTAMP)),
(3, 1, '活动签到墙', 'wall', 'HTML', '线下活动现场签到与留言展示', 'BUILDING', 'wall.sites.localhost', 'wall.csucc.club', '/previews/wall.png', 'main', 'wall.zip', NULL, 3, DATEADD('DAY', -12, CURRENT_TIMESTAMP), DATEADD('MINUTE', -8, CURRENT_TIMESTAMP)),
(4, 1, '开源镜像站', 'mirror', 'HTML', '社团开源软件镜像与下载服务入口', 'PENDING', 'mirror.sites.localhost', 'mirror.csucc.club', '/previews/mirror.png', 'main', 'mirror.html', NULL, 4, DATEADD('DAY', -8, CURRENT_TIMESTAMP), DATEADD('DAY', -1, CURRENT_TIMESTAMP));

MERGE INTO deployments (id, site_id, user_id, status, environment, commit_hash, source_filename, output_path, build_log, duration_seconds, started_at, finished_at, created_at)
KEY(id) VALUES
(1, 1, 1, 'SUCCESS', 'production', '9f3c2a1', 'lab-site.zip', NULL, '✓ 安装依赖\n✓ 构建完成\n✓ 发布成功', 42, DATEADD('MINUTE', -19, CURRENT_TIMESTAMP), DATEADD('MINUTE', -18, CURRENT_TIMESTAMP), DATEADD('MINUTE', -19, CURRENT_TIMESTAMP)),
(2, 2, 1, 'SUCCESS', 'production', 'a7d1b9e', 'recruit.zip', NULL, '✓ 安装依赖\n✓ 构建完成\n✓ 发布成功', 36, DATEADD('HOUR', -4, CURRENT_TIMESTAMP), DATEADD('MINUTE', -239, CURRENT_TIMESTAMP), DATEADD('HOUR', -4, CURRENT_TIMESTAMP)),
(3, 3, 1, 'BUILDING', 'production', 'c2a9d3f', 'wall.zip', NULL, '✓ 已接收项目\n✓ 正在构建（步骤 2/3）', NULL, DATEADD('MINUTE', -8, CURRENT_TIMESTAMP), NULL, DATEADD('MINUTE', -8, CURRENT_TIMESTAMP)),
(4, 4, 1, 'SUCCESS', 'production', '3c4d5e6', 'mirror.html', NULL, '✓ 静态资源检查\n✓ 发布成功', 4, DATEADD('DAY', -1, CURRENT_TIMESTAMP), DATEADD('SECOND', 4, DATEADD('DAY', -1, CURRENT_TIMESTAMP)), DATEADD('DAY', -1, CURRENT_TIMESTAMP)),
(5, 1, 1, 'FAILED', 'production', '7b8a1c2', 'lab-site.zip', NULL, '✕ 构建失败：TypeScript 类型错误', 18, DATEADD('DAY', -1, CURRENT_TIMESTAMP), DATEADD('SECOND', 18, DATEADD('DAY', -1, CURRENT_TIMESTAMP)), DATEADD('DAY', -1, CURRENT_TIMESTAMP)),
(6, 3, 1, 'SUCCESS', 'production', 'd4e5f6a', 'wall.zip', NULL, '✓ 构建完成\n✓ 发布成功', 28, DATEADD('DAY', -2, CURRENT_TIMESTAMP), DATEADD('SECOND', 28, DATEADD('DAY', -2, CURRENT_TIMESTAMP)), DATEADD('DAY', -2, CURRENT_TIMESTAMP));

MERGE INTO site_domains (id, site_id, user_id, domain, type, status, verification_token, created_at, updated_at)
KEY(id) VALUES
(1, 1, 1, 'lab.csucc.club', 'CUSTOM', 'ACTIVE', 'site-verify-lab', DATEADD('DAY', -20, CURRENT_TIMESTAMP), DATEADD('DAY', -19, CURRENT_TIMESTAMP)),
(2, 2, 1, 'join.csucc.club', 'CUSTOM', 'ACTIVE', 'site-verify-join', DATEADD('DAY', -15, CURRENT_TIMESTAMP), DATEADD('DAY', -15, CURRENT_TIMESTAMP)),
(3, 3, 1, 'wall.csucc.club', 'CUSTOM', 'PENDING', 'site-verify-wall', DATEADD('DAY', -2, CURRENT_TIMESTAMP), DATEADD('DAY', -2, CURRENT_TIMESTAMP));

