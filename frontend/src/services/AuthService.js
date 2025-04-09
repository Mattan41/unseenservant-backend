import axios from '@/lib/axios.js';
import {useAuthStore} from '../stores/authStore.js';
import router from '../router/index.js';
import {useUserStore} from "@/stores/userStore.js";

const API_GOOGLE_LOGIN_URL = `${import.meta.env.VITE_API_BASE_URL}/oauth2/authorization/google`;
const API_GITHUB_LOGIN_URL = `${import.meta.env.VITE_API_BASE_URL}/oauth2/authorization/github`;

class AuthService {

  async loginWithGoogle() {
    window.location.href = API_GOOGLE_LOGIN_URL;
  }

  async loginWithGithub() {
    window.location.href = API_GITHUB_LOGIN_URL;
  }

  async getCurrentUser() {
    try {
      const response = await axios.get(`/api/auth/me`, {
      });
      return response.data;
    } catch (error) {
      console.error('Error fetching current user:', error);
      return null;
    }
  }

  async logout() {
    return axios
      .post(`/logout`, {}, {})
      .then(() => {
        const authStore = useAuthStore();
        const userStore = useUserStore();

        // Clear the user data from the stores
        authStore.user = null; // todo: clearUser() method in authStore ? something is interfering with the logout
        authStore.isAuthenticated = false;
        userStore.clearUserInfo();

        // Clear the user data from the local storage
        localStorage.removeItem('userData');
        sessionStorage.removeItem('userData');

        // Trigger the storage event to notify all tabs
        window.dispatchEvent(new Event('storage'));

        // Redirect the user to the home page
        router.push('/');
      });
  }
}

export default new AuthService();
