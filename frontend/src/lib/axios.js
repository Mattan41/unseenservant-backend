// axios.js
import Axios from 'axios'
import { useAuthStore } from '@/features/auth/authStore'
import { useNotificationStore } from '@/stores/notificationStore'
import router from '@/router'

// Compute a safe base URL (avoid the string "undefined/")
const rawBase = import.meta.env.VITE_API_BASE_URL ?? '';
const normalizedBase = rawBase
  ? (rawBase.endsWith('/') ? rawBase : `${rawBase}/`)
  : '/';

const axios = Axios.create({
  baseURL: normalizedBase,
  timeout: 60000,
  withCredentials: true,
  withXSRFToken: true,
})

// Request interceptor
axios.interceptors.request.use(
  config => {
    // Prepare for JWT: attach Authorization header if a token exists in the auth store
    try {
      const authStore = useAuthStore()
      if (authStore?.token) {
        // Do not overwrite if already explicitly set
        if (!config.headers) config.headers = {}
        if (!config.headers['Authorization']) {
          config.headers['Authorization'] = `Bearer ${authStore.token}`
        }
      }
    } catch (_) {
      // No-op if store not available during app bootstrap
    }
    return config
  },
  error => Promise.reject(error)
);

// Response interceptor
axios.interceptors.response.use(
  response => response,
  error => {
    // Handle HTTP errors
    if (error.response) {
      const { status } = error.response;

      // Handle authentication errors
      if (status === 401) {
        const authStore = useAuthStore();
        if (authStore.isLoggingOut) return Promise.reject(error);

        if (authStore.isLoggedIn) {
          console.warn('Session expired, logging out');
          const notificationStore = useNotificationStore();

          notificationStore.addNotification(
            'Your session has expired. Please log in again.',
            'warning',
            3000
          );
        }

        authStore.isLoggingOut = true;
        authStore.clearUser();

        if (router.currentRoute.value.meta.requiresAuth && router.currentRoute.value.name !== 'login') {
          router.push({ name: 'login' });
        }
      }

      console.error(`API Error (${status}):`, error.response?.data || error.message);
    } else {
      console.error('API Error (network):', error.message);
    }

    return Promise.reject(error);
  }
);

export default axios;
