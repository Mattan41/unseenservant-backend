import axios from '@/lib/axios.js';

const apiBase = import.meta.env.VITE_API_BASE_URL || '';
const API_GOOGLE_LOGIN_URL = `${apiBase}/oauth2/authorization/google`;
const API_GITHUB_LOGIN_URL = `${apiBase}/oauth2/authorization/github`;

class AuthService {
  async loginWithGoogle() {
    window.location.href = API_GOOGLE_LOGIN_URL;
  }

  async loginWithGithub() {
    window.location.href = API_GITHUB_LOGIN_URL;
  }

  async getCurrentUser() {
    try {
      const response = await axios.get(`/api/auth/me`);
      if (response.data) {
        // Store in localStorage for persistence across tabs
        localStorage.setItem('userData', JSON.stringify(response.data));
      }
      return response.data;
    } catch (error) {
      console.error('Error fetching current user:', error);
      return null;
    }
  }

  async logoutAPI() {
    try {
      await axios.post(`/logout`, {});
      return true;
    } catch (error) {
      console.error('Error logging out:', error);
      return false;
    }
  }
}

export default new AuthService();
