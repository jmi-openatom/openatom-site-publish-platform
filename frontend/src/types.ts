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
  createdAt: string
  updatedAt: string
}
