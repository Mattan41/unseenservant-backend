import axios from '@/lib/axios.js'

const apiBase = import.meta.env.VITE_API_BASE_URL || ''
const API_GOOGLE_LOGIN_URL = `${apiBase}/oauth2/authorization/google`
const API_GITHUB_LOGIN_URL = `${apiBase}/oauth2/authorization/github`

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
