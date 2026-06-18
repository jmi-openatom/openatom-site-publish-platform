import type { Site } from '@/types'

function isLocalDomain(domain: string) {
  return domain === 'localhost' || domain.endsWith('.localhost')
}

function hostUrl(domain: string) {
  if (isLocalDomain(domain)) {
    const port = import.meta.env.VITE_SITE_PORT ?? '8080'
    return `http://${domain}${port ? `:${port}` : ''}/`
  }
  return `https://${domain}/`
}

export function siteVisitUrl(site: Pick<Site, 'customDomain' | 'defaultDomain'>) {
  return hostUrl(site.customDomain || site.defaultDomain)
}
