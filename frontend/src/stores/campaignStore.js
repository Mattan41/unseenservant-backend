import {defineStore} from 'pinia';
import CampaignService from '../services/CampaignService';

export const useCampaignStore = defineStore('campaign', {
  state: () => ({
    campaigns: [],
    isLoading: false,
    error: null,
  }),

  actions: {
    async fetchAllCampaigns() {
      this.isLoading = true;
      this.error = null;

      try {
        this.campaigns = await CampaignService.fetchAllCampaigns();
      } catch (error) {
        console.error('Failed to fetch campaigns:', error);
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

    async createCampaign(name, description) {
      this.isLoading = true;
      this.error = null;

      try {
        const newCampaign = await CampaignService.createCampaign(name, description);
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

    // use this method to update a campaign field, such as name, description, etc.
    async updateCampaignField(campaignId, field, value) {
      console.log('Updating campaign field:', campaignId, field, value);
      try {
        await CampaignService.updateCampaignField(campaignId, field, value);
      } catch (error) {
        console.error('Failed to update campaign field:', error);
        throw error;
      }
    },

    async updateCampaignInfo(campaignId, campaignData) {
      try {
        return await CampaignService.updateCampaignInfo(campaignId, campaignData);
      } catch (error) {
        console.error('Failed to update campaign info:', error);
        throw error;
      }
    },

    // manage participants in a campaign
    async addParticipantsToCampaign(campaignId, participantsToAdd) {
      console.log('Adding participants to campaign:', campaignId, participantsToAdd);
      try {
        return await CampaignService.addParticipants(campaignId, participantsToAdd);
      } catch (error) {
        console.error('Failed to add participants to campaign:', error);
        throw error;
      }
    },

    async removeParticipantsFromCampaign(campaignId, participantIdsToRemove) {
      console.log('Removing participants from campaign:', campaignId, participantIdsToRemove);
      try {
        return await CampaignService.removeParticipants(campaignId, participantIdsToRemove);
      } catch (error) {
        console.error('Failed to remove participants from campaign:', error);
        throw error;
      }
    },

    async updateParticipantNickname(campaignId, participantId, nickname) {
      try {
        await CampaignService.updateParticipantNickname(campaignId, participantId, nickname);
      } catch (error) {
        console.error('Failed to update participant nickname:', error);
        throw error;
      }
    },

    async searchUsers(query) {
      try {
        return await CampaignService.searchUsers(query);
      } catch (error) {
        console.error('Failed to search users:', error);
        throw error;
      }
    },

    async updateParticipantRole(campaignId, participantId, role) {
      try {
        return await CampaignService.updateParticipantRole(campaignId, participantId, role);
      } catch (error) {
        console.error('Failed to update participant role:', error);
        throw error;
      }
    },

  },

  getters: {
    getCampaignById: (state) => (id) => state.campaigns.find(campaign => campaign.id === id),
    //Fetch the owner of a campaign based on ID
    ownerId: (state) => (id) => {
      const campaign = state.campaigns.find(c => c.id === id);
      return campaign ? campaign.ownerId : null;
    }
  },
});
