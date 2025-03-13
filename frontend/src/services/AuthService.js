import axios from 'axios';
import {useAuthStore} from '../stores/authStore.js';
import router from '../router/index.js';

const API_BASE_URL = `${import.meta.env.VITE_API_BASE_URL}/api/auth`;
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
      const response = await axios.get(`${API_BASE_URL}/me`, {
        withCredentials: true, //  session-cookies is automatically sent with the request
      });
      return response.data;
    } catch (error) {
      console.error('Error fetching current user:', error);
      return null;
    }
  }

  async logout() {
    return axios
      .post(`${import.meta.env.VITE_API_BASE_URL}/logout`, {}, {
        withCredentials: true,
      })
      .then(() => {
        const authStore = useAuthStore();
        authStore.user = null;
        localStorage.removeItem('userData');
        sessionStorage.removeItem('userData');
        window.dispatchEvent(new Event('storage'));
        router.push('/');
      });
  }
}

export default new AuthService();
