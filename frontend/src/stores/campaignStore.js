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
    }


  },

});
