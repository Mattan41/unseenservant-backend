import {defineStore} from 'pinia'
import UserService from '../services/UserService' // Importera UserService

export const useUserStore = defineStore('user', {
  state: () => ({
    userInfo: null,
    isLoading: false,
    error: null,
  }),

  actions: {
    // fetch the current user's information
    async fetchCurrentUser() {
      this.isLoading = true
      this.error = null

      try {
        this.userInfo = await UserService.fetchCurrentUser()
      } catch (error) {
        console.error('Failed to fetch current user info:', error)
        this.error = 'Could not fetch user information.'
        this.userInfo = null
      } finally {
        this.isLoading = false
      }
    },

    //fetch user information by user id, could be used to fetch other users' information such as invites to campaigns, or by admin to view/edit user information
    async fetchUserInfo(userId) {
      this.isLoading = true
      this.error = null

      try {
        this.userInfo = await UserService.fetchUser(userId)
      } catch (error) {
        console.error('Failed to fetch user info:', error)
        this.error = 'Failed to fetch user.'
      } finally {
        this.isLoading = false
      }
    },

    async updateProfileField(field, value) {
      this.isLoading = true
      this.error = null

      try {
        const updatedUser = await UserService.updateProfileField(this.userInfo.id, field, value)
        this.userInfo = { ...this.userInfo, ...updatedUser }
      } catch (error) {
        console.error('Failed to update profile field:', error)
        this.error = 'Failed to update profile.'
      } finally {
        this.isLoading = false
      }
    },

    async updateProfile(data) {
      this.isLoading = true
      this.error = null

      try {
        this.userInfo = await UserService.updateProfile(this.userInfo.id, data)
      } catch (error) {
        console.error('Failed to update profile:', error)
        this.error = 'Failed to update profile.'
      } finally {
        this.isLoading = false
      }
    },
    clearUserInfo() {
      this.userInfo = null;
      this.error = null;
      this.isLoading = false;
      localStorage.removeItem('userData');
      console.log('User info cleared and localStorage cleaned.');
    },
  },

  getters: {
    getDisplayName: (state) => state.userInfo?.displayName || state.userInfo?.username || 'Traveler',
    getRole: (state) => state.userInfo?.role || 'Standard user',
    isLoadingProfile: (state) => state.isLoading,
  },
})
