import {defineStore} from 'pinia';
import CampaignService from '../services/CampaignService';
import {useNotificationStore} from "@/stores/notificationStore.js";

export const useCampaignStore = defineStore('campaign', {
  state: () => ({
    campaigns: [],
    isLoading: false,
    error: null,
  }),

  actions: {

    async createCampaign(name, description) {
      const notificationStore = useNotificationStore();
      this.isLoading = true;
      this.error = null;

      try {
        const newCampaign = await CampaignService.createCampaign(name, description);
        notificationStore.addNotification("Campaign created successfully!", "success");
        await this.fetchAllCampaignsForCurrentUser();
        return newCampaign;
      } catch (error) {
        console.error('Failed to create campaign:', error);
        this.error = 'Could not create campaign.';
        throw error;
      } finally {
        this.isLoading = false;
      }
    },

    async fetchAllCampaigns() {
      this.isLoading = true;
      this.error = null;
      try {
        this.campaigns = await CampaignService.fetchAllCampaigns();
      } catch (error) {
        this.error = 'Could not fetch campaigns.';
      } finally {
        this.isLoading = false;
      }
    },

    async fetchAllCampaignsForCurrentUser() {
      this.isLoading = true;
      this.error = null;

      try {
        this.campaigns = await CampaignService.fetchAllCampaignsForCurrentUser();
      } catch (error) {
        console.error('Failed to fetch campaigns:', error);
        this.error = 'Could not fetch campaigns.';
      } finally {
        this.isLoading = false;
      }
    },

    async fetchCampaign(id) {
      this.isLoading = true;
      this.error = null;

      try {
        return await CampaignService.fetchCampaign(id);
      } catch (error) {
        console.error('Failed to fetch campaign:', error);
        this.error = 'Could not fetch campaign.';
        throw error;
      } finally {
        this.isLoading = false;
      }
    },

    async updateCampaignInfo(campaignId, campaignData) {
      const notificationStore = useNotificationStore();
      try {
        await CampaignService.updateCampaignInfo(campaignId, campaignData);
        notificationStore.addNotification("Campaign updated successfully!", "success");
        return true;
      } catch (error) {
        console.error('Failed to update campaign info:', error);
        notificationStore.addNotification(error.message || "Failed to update campaign info", "error");
        throw error;
      }
    },

    async updateCampaignImage(campaignId, imageUrl) {
      const notificationStore = useNotificationStore();
      console.log('Updating campaign image:', campaignId, imageUrl);
      try {
        return await CampaignService.updateCampaignImage(campaignId, imageUrl);
      } catch (error) {
        notificationStore.addNotification(error.message || "Failed to update campaign image", "error");
        console.error('Failed to update campaign image:', error);
        throw error;
      }
    },

    async deleteCampaign(campaignId) {
      const notificationStore = useNotificationStore();

      try {
        await CampaignService.deleteCampaign(campaignId);
        notificationStore.addNotification("Campaign deleted successfully!", "success");
      } catch (error) {
        notificationStore.addNotification(error.message || "Failed to delete campaign", "error");
        console.error("Error deleting campaign:", error);
        throw error;
      }
    },

    // transfer ownership of the campaign
    async transferCampaignOwnership(campaignId, newOwnerId) {
      const notificationStore = useNotificationStore();
      console.log('Transferring campaign ownership:', campaignId, newOwnerId);
      try {
        await CampaignService.transferCampaignOwnership(campaignId, newOwnerId);
        notificationStore.addNotification("Campaign ownership transferred successfully!", "success");
        await this.fetchAllCampaignsForCurrentUser();
      } catch (error) {
        notificationStore.addNotification(error.message || "Failed to transfer campaign ownership", "error");
        console.error('Failed to transfer campaign ownership:', error);
        throw error;
      }
    },


    // manage participants in a campaign
    async addParticipantsToCampaign(campaignId, participantsToAdd) {
      const notificationStore = useNotificationStore();
      console.log('Adding participants to campaign:', campaignId, participantsToAdd);
      try {
        await CampaignService.addParticipants(campaignId, participantsToAdd);
        notificationStore.addNotification(`Participant added successfully!`, "success");
        return true;

      } catch (error) {
        console.error('Failed to add participants to campaign:', error);
        notificationStore.addNotification(error.message || "Failed to add participants", "error");
        throw error;
      }
    },

    async removeParticipantsFromCampaign(campaignId, participantIdsToRemove) {
      const notificationStore = useNotificationStore();
      console.log('Removing participants from campaign:', campaignId, participantIdsToRemove);
      try {
        await CampaignService.removeParticipants(campaignId, participantIdsToRemove);
        notificationStore.addNotification("Participants removed successfully!", "success");
        return true;
      } catch (error) {
        console.error('Failed to remove participants from campaign:', error);
        notificationStore.addNotification(error.message || "Failed to remove participants", "error");
        throw error;
      }
    },

    async updateParticipantNickname(campaignId, participantId, nickname) {
      const notificationStore = useNotificationStore();
      try {
        await CampaignService.updateParticipantNickname(campaignId, participantId, nickname);
        notificationStore.addNotification("Participant nickname updated successfully to: " + nickname, "success");
      } catch (error) {
        console.error('Failed to update participant nickname:', error);
        notificationStore.addNotification(error.message || "Failed to update participant nickname", "error");
        throw error;
      }
    },

    async searchUsers(query) {
      try {
        return await CampaignService.searchUsers(query);
      } catch (error) {
        const notificationStore = useNotificationStore();
        notificationStore.addNotification(error.message || "Failed to search users", "error");
        console.error('Failed to search users:', error);
        throw error;
      }
    },

    async updateParticipantRole(campaignId, participantId, role) {
      const notificationStore = useNotificationStore();
      try {
        await CampaignService.updateParticipantRole(campaignId, participantId, role);
        notificationStore.addNotification("Participant role updated successfully!", "success");
        return true;

      } catch (error) {
        notificationStore.addNotification(error.message || "Failed to update participant role", "error");
        console.error('Failed to update participant role:', error);
        throw error;
      }
    },

  },

  getters: {
    getCampaignById: (state) => (id) => state.campaigns.find(campaign => campaign.id === id),

    ownerId: (state) => (id) => {
      const campaign = state.campaigns.find(c => c.id === id);
      return campaign ? campaign.ownerId : null;
    },

    getCampaignDescription: (state) => (id) => {
      const campaign = state.campaigns.find(campaign => campaign.id === id);
      return campaign?.description || "This campaign has no description.";
    },

    getCampaignTitle: (state) => (id) => {
      const campaign = state.campaigns.find(campaign => campaign.id === id);
      return campaign.name || "This is a generic campaign title";
    },
    getCampaignImageUrl: (state) => (id) => {
      const campaign = state.campaigns.find(campaign => campaign.id === id);
      return campaign?.imageUrl || '/src/assets/default-campaign.svg';
    }
  },
});
