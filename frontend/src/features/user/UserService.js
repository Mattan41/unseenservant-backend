import axios from '@/lib/axios.js'

const UserService = {
  async fetchCurrentUserInfo() {
    const response = await axios.get(`api/users/me`, {
    });
    return response.data;
  },

  async fetchUser(userId) {
    const response = await axios.get(`api/users/${userId}`, {
    });
    return response.data;
  },

  async updateProfile(userId, data) {
    const response = await axios.put(`api/users/${userId}`, data, {
    });
    return response.data;
  },

  async updateProfileField(userId, field, value) {
    const response = await axios.patch(`api/users/${userId}`, {
      [field]: value,
    }, {
    });

    return response.data;
  },
};

export default UserService;
