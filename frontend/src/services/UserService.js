import axios from 'axios';

const API_BASE_URL = `${import.meta.env.VITE_API_BASE_URL}/api/users`;

const UserService = {
  async fetchCurrentUser() {
    const response = await axios.get(`${API_BASE_URL}/me`, {
      withCredentials: true, // session-cookies is automatically sent with the request
    });
    return response.data;
  },

  async fetchUser(userId) {
    const response = await axios.get(`${API_BASE_URL}/${userId}`, {
      withCredentials: true,
    });
    return response.data;
  },

  async updateProfile(userId, data) {
    const response = await axios.put(`${API_BASE_URL}/${userId}`, data, {
      withCredentials: true,
    });
    return response.data;
  },

  async updateProfileField(userId, field, value) {
    const response = await axios.patch(`${API_BASE_URL}/${userId}`, {
      [field]: value,
    }, {
      withCredentials: true,
    });

    return response.data;
  },
};

export default UserService;
