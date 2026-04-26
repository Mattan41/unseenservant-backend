import axios from '@/lib/axios.js'

// OAuth redirects need the full URL because Cloudflare Pages can't proxy OAuth properly
// For local dev and RPI, this will be empty (relative URLs work via Vite proxy/nginx)
// For Cloudflare Pages, this points to the main backend
const oauthBase = import.meta.env.VITE_OAUTH_BASE_URL || ''

class AuthService {
  async loginWithGoogle() {
    const origin = window.location.origin
    window.location.href = `${oauthBase}/api/auth/oauth-init?provider=google&origin=${encodeURIComponent(origin)}`
  }

  async loginWithGithub() {
    const origin = window.location.origin
    window.location.href = `${oauthBase}/api/auth/oauth-init?provider=github&origin=${encodeURIComponent(origin)}`
  }

  async getCurrentUser() {
    const response = await axios.get(`/api/auth/me`)
    return response.data
  }
}
export default new AuthService()
