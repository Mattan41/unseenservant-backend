// axios.js
import Axios from 'axios';
import { useAuthStore } from '@/features/auth/authStore';
import { useNotificationStore } from '@/stores/notificationStore';
import router from '@/router';

const axios = Axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 60000,
  withCredentials: true,
  withXSRFToken: true,
});

// Request interceptor
axios.interceptors.request.use(
  config => {
    // Du kan lägga till headers eller annan logik här vid behov
    return config;
  },
  error => Promise.reject(error)
);

// Response interceptor
axios.interceptors.response.use(
  response => response,
  error => {
    // Hantera olika HTTP felkoder
    if (error.response) {
      const { status } = error.response;

      // Hantera autentiseringsfel
      if (status === 401) {
        console.warn('Unauthorized access detected, logging out');
        const authStore = useAuthStore();
        const notificationStore = useNotificationStore();

        // Rensa autentiseringsdata
        authStore.clearUser();

        // Meddela användaren
        notificationStore.addNotification(
          'Your session has expired. Please log in again.',
          'warning',
          5000
        );

        // Omdirigera till login om vi inte redan är där
        if (router.currentRoute.value.name !== 'login') {
          router.push({ name: 'login' });
        }
      }

      // Logga alla API-fel
      console.error(`API Error (${status}):`, error.response?.data || error.message);
    } else {
      console.error('API Error (network):', error.message);
    }

    return Promise.reject(error);
  }
);

export default axios;
