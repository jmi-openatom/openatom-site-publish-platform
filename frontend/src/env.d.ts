/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_SITE_PORT?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
