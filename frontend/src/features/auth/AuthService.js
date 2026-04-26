import axios from '@/lib/axios.js'

// OAuth redirects need the full URL because Cloudflare Pages can't proxy OAuth properly
// For local dev and RPI, this will be empty (relative URLs work via Vite proxy/nginx)
// For Cloudflare Pages, this points to the main backend
const oauthBase = import.meta.env.VITE_OAUTH_BASE_URL || ''

const API_GOOGLE_LOGIN_URL = `${oauthBase}/oauth2/authorization/google`
const API_GITHUB_LOGIN_URL = `${oauthBase}/oauth2/authorization/github`

class AuthService {
  async loginWithGoogle() {
    window.location.href = API_GOOGLE_LOGIN_URL
  }

  async loginWithGithub() {
    window.location.href = API_GITHUB_LOGIN_URL
  }

  async getCurrentUser() {
    const response = await axios.get(`/api/auth/me`)
    return response.data
  }
}
export default new AuthService()
