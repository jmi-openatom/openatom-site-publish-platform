export function formatDate(value?: string) {
  if (!value) return '—'
  return new Intl.DateTimeFormat('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  }).format(new Date(value))
}

export function relativeTime(value?: string) {
  if (!value) return '刚刚'
  const seconds = Math.floor((Date.now() - new Date(value).getTime()) / 1000)
  if (seconds < 60) return '刚刚'
  if (seconds < 3600) return `${Math.floor(seconds / 60)} 分钟前`
  if (seconds < 86400) return `${Math.floor(seconds / 3600)} 小时前`
  return `${Math.floor(seconds / 86400)} 天前`
}

export function statusLabel(status: string) {
  return {
    ONLINE: '已上线',
    BUILDING: '构建中',
    FAILED: '部署失败',
    PENDING: '待配置',
    SUCCESS: '部署成功',
    ACTIVE: '已生效',
    SSL_WAITING_DNS: 'SSL 待验证',
    SSL_WAITING_SITE: 'SSL 等待上线',
    SSL_PROVISIONING: 'SSL 申请中',
    SSL_ACTIVE: 'HTTPS 已启用',
    SSL_FAILED: 'SSL 申请失败',
    SSL_DISABLED: '自动 SSL 未启用',
  }[status] || status
}
