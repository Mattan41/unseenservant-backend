import {defineStore} from 'pinia';
import {useNotificationStore} from "@/stores/notificationStore.js";
import CharacterService from "@/services/CharacterService.js";

export const useCharacterStore = defineStore('character', {
  state: () => ({
    characters: [],
    currentCharacter: null,
    isLoading: false,
    error: null,
  }),

  actions: {

    async createCharacter(data) {
      const notificationStore = useNotificationStore();
      this.isLoading = true;

      try {
        const newCharacter = await CharacterService.createCharacter(data);
        this.characters.push(newCharacter);
        notificationStore.addNotification("Character created successfully!", "success");
        return newCharacter;
      } catch (error) {
        console.error('Failed to create character:', error);
        notificationStore.addNotification("Failed to create character.", "error");
        this.error = error.message;
      } finally {
        this.isLoading = false;
      }
    },

    async fetchCharacter(characterId) {
      const notificationStore = useNotificationStore();
      this.isLoading = true;

      const existingCharacter = this.characters.find(char => char.id === characterId);

      if (existingCharacter) {
        this.currentCharacter = existingCharacter;
        this.isLoading = false;
        return existingCharacter;
      }

      try {
        const character = await CharacterService.fetchCharacter(characterId);
        this.currentCharacter = character;

        // Om karaktären inte finns i characters-arrayen, lägg till den
        if (!this.characters.some(char => char.id === characterId)) {
          this.characters.push(character);
        }

        return character;
      } catch (error) {
        console.error('Failed to fetch character:', error);
        notificationStore.addNotification("Failed to fetch character.", "error");
        this.error = error.message;
      } finally {
        this.isLoading = false;
      }
    },

    async fetchAllCharactersForCurrentUser() {
      const notificationStore = useNotificationStore();
      this.isLoading = true;
      try {
        const characters = await CharacterService.fetchAllCharactersForCurrentUser();
        this.characters = characters;
        return characters;
      } catch (error) {
        console.error('Failed to fetch characters:', error);
        notificationStore.addNotification("Failed to fetch characters.", "error");
        this.error = error.message;
      } finally {
        this.isLoading = false;
      }
    },


    async fetchCharactersWithoutCampaign() {
      const notificationStore = useNotificationStore();
      try {
        this.isLoading = true;
        const characters = await CharacterService.fetchCharactersWithoutCampaign();
        console.log('Characters returned from service:', characters);

        return Array.isArray(characters) ? characters : [];
      } catch (error) {
        console.error('Failed to fetch characters without campaign:', error);
        notificationStore.addNotification("Failed to fetch characters without campaign.", "error");
        return [];
      } finally {
        this.isLoading = false;
      }
    },

    async updateCharacter(characterId, data) {
      const notificationStore = useNotificationStore();
      this.isLoading = true;

      try {
        const updatedCharacter = await CharacterService.updateCharacter(characterId, data);

        // update the character in the characters array
        const index = this.characters.findIndex(char => char.id === characterId);
        if (index !== -1) {
          this.characters[index] = updatedCharacter;
        }

        // Update currentCharacter if it is the one to be updated
        if (this.currentCharacter && this.currentCharacter.id === characterId) {
          this.currentCharacter = updatedCharacter;
        }

        notificationStore.addNotification("Character updated successfully!", "success");
        return updatedCharacter;
      } catch (error) {
        console.error('Failed to update character:', error);
        notificationStore.addNotification("Failed to update character.", "error");
        this.error = error.message;
      } finally {
        this.isLoading = false;
      }
    },

    async updateCharacterField(characterId, field, value) {
      const notificationStore = useNotificationStore();
      this.isLoading = true;

      try {
        const updatedCharacter = await CharacterService.updateCharacterField(characterId, field, value);

        // update the character in the characters array
        const index = this.characters.findIndex(char => char.id === characterId);
        if (index !== -1) {
          this.characters[index] = updatedCharacter;
        }

        // Update currentCharacter if it is the one to be updated
        if (this.currentCharacter && this.currentCharacter.id === characterId) {
          this.currentCharacter = updatedCharacter;
        }

        notificationStore.addNotification(`Character ${field} updated successfully!`, "success");
        return updatedCharacter;
      } catch (error) {
        console.error('Failed to update character field:', error);
        notificationStore.addNotification(`Failed to update ${field}.`, "error");
        this.error = error.message;
      } finally {
        this.isLoading = false;
      }
    },

    async deleteCharacter(characterId) {
      const notificationStore = useNotificationStore();
      this.isLoading = true;

      try {
        await CharacterService.deleteCharacter(characterId);

        // remove character form the characters array
        this.characters = this.characters.filter(char => char.id !== characterId);

        // remove currentCharacter if it matches the deleted character
        if (this.currentCharacter && this.currentCharacter.id === characterId) {
          this.currentCharacter = null;
        }

        notificationStore.addNotification("Character deleted successfully!", "success");
        return true;
      } catch (error) {
        console.error('Failed to delete character:', error);
        notificationStore.addNotification("Failed to delete character.", "error");
        this.error = error.message;
        return false;
      } finally {
        this.isLoading = false;
      }
    },

    async addCharacterToCampaign(characterId, campaignId) {
      const notificationStore = useNotificationStore();
      try {
        const response = await CharacterService.addCharacterToCampaign(characterId, campaignId);
         return response.data;
      } catch (error) {
        notificationStore.addNotification("Failed to add character to campaign.", error);
      }
    },

    // todo: is this needed? we use the removeCharacterFromCampaign method in the CampaignStore
    async removeCharacterFromCampaign(characterId) {
      try {
        const response = await CharacterService.removeCharacterFromCampaign(characterId);
        return response.data;
      } catch (error) {
        const notificationStore = useNotificationStore();
        notificationStore.addNotification("Failed to remove character from campaign.", error);
      }
    }

  },

  getters: {
    getCharacterById: (state) => (id) => {
      return state.characters.find(character => character.id === parseInt(id) || character.id === id);
    }
  }
});
