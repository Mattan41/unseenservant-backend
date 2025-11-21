import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import UserService from './UserService.js'
import { useNotificationStore } from '@/stores/notificationStore.js'

export const useUserStore = defineStore('user', () => {
  // State
  const currentUser = ref(null)
  const isLoading = ref(false)
  const error = ref(null)

  // Computed/Getters
  const displayName = computed(
    () => currentUser.value?.displayName || currentUser.value?.username || 'Traveler',
  )

  const userRole = computed(() => currentUser.value?.role || 'Standard user')

  const userId = computed(() => currentUser.value?.id || null)

  const isUserLoaded = computed(() => currentUser.value !== null)

  // Actions
  async function fetchCurrentUser() {
    isLoading.value = true
    error.value = null

    try {
      const userData = await UserService.fetchCurrentUserInfo()
      currentUser.value = userData
      return userData
    } catch (err) {
      console.error('Failed to fetch current user:', err)
      error.value = 'Could not fetch user information.'
      currentUser.value = null
      throw err
    } finally {
      isLoading.value = false
    }
  }

  async function fetchUserById(userId) {
    isLoading.value = true
    error.value = null

    try {
      const userData = await UserService.fetchUser(userId)
      // Obs: Detta uppdaterar currentUser - anpassa om du vill hantera andra users annorlunda
      currentUser.value = userData
      return userData
    } catch (err) {
      console.error('Failed to fetch user:', err)
      error.value = 'Failed to fetch user.'
      throw err
    } finally {
      isLoading.value = false
    }
  }

  async function updateUserField(field, value) {
    if (!currentUser.value?.id) {
      throw new Error('No user loaded')
    }

    const notificationStore = useNotificationStore()
    isLoading.value = true
    error.value = null

    try {
      const updatedUser = await UserService.updateProfileField(currentUser.value.id, field, value)

      // Uppdatera endast ändrade fält
      currentUser.value = { ...currentUser.value, ...updatedUser }

      notificationStore.addNotification(`Successfully updated ${field}.`, 'success')
      return true
    } catch (err) {
      let errorMessage = `Failed to update ${field}. Please try again.`

      if (err.response?.status === 409) {
        errorMessage = err.response.data.message || `This ${field} is already taken`
      }

      error.value = errorMessage
      notificationStore.addNotification(errorMessage, 'error')
      return false
    } finally {
      isLoading.value = false
    }
  }

  function clearUser() {
    currentUser.value = null
    error.value = null
    isLoading.value = false
  }

  return {
    // State
    currentUser,
    isLoading,
    error,
    // Computed
    displayName,
    userRole,
    userId,
    isUserLoaded,
    // Actions
    fetchCurrentUser,
    fetchUserById,
    updateUserField,
    clearUser,
  }
})
