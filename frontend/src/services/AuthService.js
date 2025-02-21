import axios from 'axios';
import {useAuthStore} from '../stores/authStore.js';
import router from '../router/index.js';

const API_BASE_URL = `${import.meta.env.VITE_API_BASE_URL}/api/auth`;
const API_GOOGLE_LOGIN_URL = `${import.meta.env.VITE_API_BASE_URL}/oauth2/authorization/google`;

class AuthService {

  async login(user) {
    const response = await axios.post(`${API_BASE_URL}/login`, user);
    return response.data;
  }

  // loginWithGoogle is the one that I am using for now - moving away from form-based login
  async loginWithGoogle() {
    window.location.href = API_GOOGLE_LOGIN_URL;
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
