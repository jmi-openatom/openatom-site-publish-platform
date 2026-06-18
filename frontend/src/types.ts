export interface ApiEnvelope<T> {
  success: boolean
  data: T
  message?: string
}

export interface User {
  id: number
  username: string
  displayName: string
  email: string
  avatar?: string
  roles: string[]
}

export interface LoginResponse {
  tokenName: string
  tokenValue: string
  expiresIn: number
  user: User
}

export type SiteStatus = 'ONLINE' | 'BUILDING' | 'FAILED' | 'PENDING'
export type DeploymentStatus = 'SUCCESS' | 'BUILDING' | 'FAILED'

export interface Deployment {
  id: number
  siteId: number
  siteName?: string
  siteSlug?: string
  status: DeploymentStatus
  environment: string
  commitHash: string
  sourceFilename?: string
  buildLog?: string
  durationSeconds?: number
  startedAt?: string
  finishedAt?: string
  createdAt: string
}

export interface Site {
  id: number
  name: string
  slug: string
  framework: string
  description?: string
  status: SiteStatus
  defaultDomain: string
  cnameTarget: string
  customDomain?: string
  publicUrl: string
  previewImage?: string
  branchName: string
  sourceFilename?: string
  createdAt: string
  updatedAt: string
  latestDeployment?: Deployment
}

export interface DomainBinding {
  id: number
  siteId: number
  siteName: string
  domain: string
  type: string
  status: 'ACTIVE' | 'PENDING'
  verificationToken: string
  cnameTarget: string
  sslStatus: 'SSL_WAITING_DNS' | 'SSL_WAITING_SITE' | 'SSL_PROVISIONING' | 'SSL_ACTIVE' | 'SSL_FAILED' | 'SSL_DISABLED'
  sslMessage: string
  sslExpiresAt?: string
  createdAt: string
  updatedAt: string
}

export interface AdminSummary {
  users: number
  sites: number
  onlineSites: number
  deployments: number
  successfulDeployments: number
  failedDeployments: number
  domains: number
  activeDomains: number
}

export interface AdminUser {
  id: number
  username?: string
  displayName?: string
  email?: string
  avatar?: string
  roles: string[]
  admin: boolean
  siteCount: number
  createdAt: string
  updatedAt: string
}

export interface AdminSite {
  id: number
  name: string
  slug: string
  framework: string
  status: SiteStatus
  defaultDomain: string
  customDomain?: string
  ownerId: number
  ownerName: string
  ownerEmail?: string
  updatedAt: string
}

export interface AdminDomain {
  id: number
  siteId: number
  siteName?: string
  ownerId: number
  ownerName: string
  domain: string
  status: 'ACTIVE' | 'PENDING'
  cnameTarget: string
  sslStatus: DomainBinding['sslStatus']
  sslMessage: string
  updatedAt: string
}

export interface AdminDeployment {
  id: number
  siteId: number
  siteName?: string
  ownerId: number
  ownerName: string
  status: DeploymentStatus
  environment?: string
  commitHash?: string
  sourceFilename?: string
  durationSeconds?: number
  createdAt: string
}

export interface AdminDashboard {
  currentUserId: number
  summary: AdminSummary
  users: AdminUser[]
  sites: AdminSite[]
  domains: AdminDomain[]
  deployments: AdminDeployment[]
}
