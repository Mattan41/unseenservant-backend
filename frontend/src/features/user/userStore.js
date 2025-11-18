import { defineStore } from 'pinia'
import UserService from './UserService.js'
import { useNotificationStore } from '@/stores/notificationStore.js'

export const useUserStore = defineStore('user', {
  state: () => ({
    userInfo: null,
    isLoading: false,
    error: null,
  }),

  actions: {
    // fetch the current user's information
    async fetchCurrentUserInfo() {
      this.isLoading = true
      this.error = null

      try {
        this.userInfo = await UserService.fetchCurrentUserInfo()
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
      const notificationStore = useNotificationStore()
      this.isLoading = true
      this.error = null

      try {
        const updatedUser = await UserService.updateProfileField(this.userInfo.id, field, value)
        this.userInfo = { ...this.userInfo, ...updatedUser }
        // Notify the user about the successful update
        notificationStore.addNotification(`Successfully updated ${field}.`, 'success')
        return true
      } catch (error) {
        // Handle HTTP 409 Conflict (UniqueConstraintViolation) if the field is unique and already taken
        if (error.response?.status === 409) {
          const data = error.response.data
          const errorMessage = data.message || `This ${field} is already taken`
          this.error = errorMessage
          notificationStore.addNotification(errorMessage, 'error')
        } else {
          const errorMessage = `Failed to update ${field}. Please try again.`
          this.error = errorMessage
          notificationStore.addNotification(errorMessage, 'error')
        }
        return false
      } finally {
        this.isLoading = false
      }
    },

    // We might not need this function, since we can update the profile with the updateProfileField function
    // todo remove this function if we don't need it
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
      this.userInfo = null
      this.error = null
      this.isLoading = false
      console.log('User info cleared')
    },
  },

  getters: {
    getDisplayName: (state) =>
      state.userInfo?.displayName || state.userInfo?.username || 'Traveler',
    getRole: (state) => state.userInfo?.role || 'Standard user',
    isLoadingProfile: (state) => state.isLoading,
    getUserId: (state) => state.userInfo?.id || null,
  },
})
