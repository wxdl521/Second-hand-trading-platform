const demoAliasMap: Record<string, string> = {
  '/uploads/default-avatar.png': '/uploads/default-avatar.svg',
  '/uploads/demo-created.png': '/uploads/demo-created.svg',
  '/uploads/demo-chanel.png': '/uploads/demo-chanel.svg',
  '/uploads/demo-chanel-detail.png': '/uploads/demo-chanel-detail.svg',
  '/uploads/demo-rolex.png': '/uploads/demo-rolex.svg',
  '/uploads/demo-rolex-detail.png': '/uploads/demo-rolex-detail.svg',
  '/uploads/demo-leica.png': '/uploads/demo-leica.svg',
  '/uploads/demo-leica-detail.png': '/uploads/demo-leica-detail.svg'
}

const defaultAssets = {
  goods: '/uploads/demo-created.svg',
  avatar: '/uploads/default-avatar.svg'
} as const

type AssetType = keyof typeof defaultAssets

const normalizePath = (value: string) => value.replace(/^https?:\/\/[^/]+/i, '').replace(/\\/g, '/')
const isRemoteUrl = (value: string) => /^https?:\/\//i.test(value)
const assetPrefixes = ['/uploads/', '/files/']

const resolveAssetOrigin = () => {
  const apiBase = import.meta.env.VITE_API_BASE_URL?.trim()

  if (apiBase && isRemoteUrl(apiBase)) {
    try {
      return new URL(apiBase).origin
    } catch (error) {
      console.warn('Unable to parse asset origin from VITE_API_BASE_URL.', error)
    }
  }

  if (typeof window !== 'undefined') {
    return window.location.origin
  }

  return ''
}

const withOrigin = (origin: string, path: string) => (origin ? new URL(path, origin).toString() : path)

export const resolveAssetUrl = (value?: string | null, type: AssetType = 'goods') => {
  if (!value) {
    return withOrigin(resolveAssetOrigin(), defaultAssets[type])
  }

  if (value.startsWith('data:') || value.startsWith('blob:')) {
    return value
  }

  const normalized = normalizePath(value)
  const mappedPath = demoAliasMap[normalized] ?? normalized

  if (isRemoteUrl(value)) {
    const origin = new URL(value).origin
    return mappedPath.startsWith('/') ? withOrigin(origin, mappedPath) : value
  }

  if (assetPrefixes.some((prefix) => mappedPath.startsWith(prefix))) {
    return withOrigin(resolveAssetOrigin(), mappedPath)
  }

  return demoAliasMap[normalized] ?? value
}

export const applyImageFallback = (event: Event, type: AssetType = 'goods') => {
  const target = event.target as HTMLImageElement | null
  if (!target) return

  const fallback = resolveAssetUrl(defaultAssets[type], type)
  if (target.src.endsWith(fallback)) return

  target.onerror = null
  target.src = fallback
}
