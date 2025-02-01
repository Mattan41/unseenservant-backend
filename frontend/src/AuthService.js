import axios from 'axios';
import {useAuthStore} from './stores/authStore';
import router from './router'; // Import the router

const API_BASE_URL = `${import.meta.env.VITE_API_BASE_URL}/api/auth`;

class AuthService {

  async login(user) {
    const response = await axios.post(API_BASE_URL + '/login', {
      username: user.username,
      password: user.password
    });
    if (response.data) {
      console.log('response.data:', response.data);
      const authStore = useAuthStore();
      authStore.user = response.data;
      localStorage.setItem('userData', JSON.stringify(response.data));
      window.dispatchEvent(new Event('storage'));
      // Use router to navigate to the home page after successful login
      await router.push('/');
    }
    return response;
  }

  register(user) {
    return axios.post(API_BASE_URL + '/register', user);
  }

}

export default new AuthService();
