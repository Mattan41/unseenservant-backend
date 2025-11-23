import Axios from 'axios'
import { useAuthStore } from '@/features/auth/authStore'
import { useNotificationStore } from '@/stores/notificationStore'

const rawBase = import.meta.env.VITE_API_BASE_URL ?? ''
const normalizedBase = rawBase ? (rawBase.endsWith('/') ? rawBase : `${rawBase}/`) : '/'

const axios = Axios.create({
  baseURL: normalizedBase,
  timeout: 60000,
  withCredentials: true,
  withXSRFToken: true,
})

// Request interceptor
axios.interceptors.request.use(
  (config) => {
    // Prepare for JWT: attach Authorization header if a token exists in the auth store
    try {
      const authStore = useAuthStore()
      if (authStore?.token) {
        if (!config.headers) config.headers = {}
        if (!config.headers['Authorization']) {
          config.headers['Authorization'] = `Bearer ${authStore.token}`
        }
      }
    } catch (e) {
      if (!(e instanceof ReferenceError)) {
        console.error('Error in request interceptor:', e)
      }
    }
    return config
  },
  (error) => Promise.reject(error),
)

// Response interceptor
axios.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      try {
        const authStore = useAuthStore()
        const notificationStore = useNotificationStore()

        if (authStore.isAuthenticated) {
          notificationStore.addNotification(
            'Your session has expired. Please log in again.',
            'warning',
            3000,
          )
        }

        authStore.clearAuth()
      } catch (e) {
        if (!(e instanceof ReferenceError)) {
          console.error('Error handling 401 in response interceptor:', e)
        }
      }
    }

    return Promise.reject(error)
  },
)

export default axios
