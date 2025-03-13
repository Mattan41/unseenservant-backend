import { defineStore } from 'pinia'
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

    //fetch user information by user id, could be used to fetch other users' information
    async fetchUserInfo(userId) {
      this.isLoading = true
      this.error = null

      try {
        const user = await UserService.fetchUser(userId)
        this.userInfo = user
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
        const updatedUser = await UserService.updateProfile(this.userInfo.id, data)
        this.userInfo = updatedUser
      } catch (error) {
        console.error('Failed to update profile:', error)
        this.error = 'Failed to update profile.'
      } finally {
        this.isLoading = false
      }
    },
  },

  getters: {
    getDisplayName: (state) => state.userInfo?.displayName || state.userInfo?.username || 'Guest',

    isLoadingProfile: (state) => state.isLoading,
  },
})
