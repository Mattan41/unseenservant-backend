import axios from 'axios';
import {useAuthStore} from './stores/authStore';
import router from './router';

const API_BASE_URL = `${import.meta.env.VITE_API_BASE_URL}/api/auth`;
const API_GOOGLE_LOGIN_URL = `${import.meta.env.VITE_API_BASE_URL}/oauth2/authorization/google`;

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
      await router.push('/');
    }
    return response;
  }

  // async loginWithGoogle(idToken) {
  //   try {
  //     const response = await axios.post(API_GOOGLE_LOGIN_URL, { idToken }, {
  //       withCredentials: true,
  //     });
  //     if (response.data) {
  //       console.log('response.data:', response.data);
  //       const authStore = useAuthStore();
  //       authStore.user = response.data;
  //       localStorage.setItem('userData', JSON.stringify(response.data));
  //       window.dispatchEvent(new Event('storage'));
  //       await router.push('/');
  //     }
  //   } catch (error) {
  //     console.error('Google login failed', error);
  //   }
  // }

  async loginWithGoogle() {
    window.location.href = API_GOOGLE_LOGIN_URL;
  }


  register(user) {
    return axios.post(API_BASE_URL + '/register', user);
  }

}

export default new AuthService();
