import axios from '@/lib/axios.js'

const CharacterService = {

  // create a character - we will wait to implement this until we have the character creation form. start with viewing a character
  async createCharacter(data) {
    const response = await axios.post('api/characters', data, {
    });
    return response.data;
  },

  // fetch all characters for the current user
  async fetchAllCharactersForCurrentUser() {
    const response = await axios.get('api/characters/me', {
    });
    return response.data;
  },
  async fetchCharactersWithoutCampaign()
  {
    const response = await axios.get('api/characters/without-campaign', {
    });
    return response.data;
  },

  // is this redundant? - we can use the fetchAllCharactersForCurrentUser to get all characters and then filter by id on the getter??
  async fetchCharacter(characterId) {
    const response = await axios.get(`api/characters/${characterId}`, {
    });
    return response.data;
  },

  // we will wait to implement this until we have the character creation form. start with viewing a character
  async updateCharacter(characterId, data) {
    const response = await axios.put(`api/characters/${characterId}`, data, {
    });
    return response.data;
  },

  // wait to implement this until we have the character creation form. start with viewing a character
  async updateCharacterField(characterId, field, value) {
    const response = await axios.patch(`api/characters/${characterId}`, {
      [field]: value,
    }, {
    });

    return response.data;
  },

  // delete a character
  async deleteCharacter(characterId) {
    const response = await axios.delete(`api/characters/${characterId}`, {
    });
    return response.data;
  },

  // add to campaign
  async addCharacterToCampaign(characterId, campaignId) {
    const response = await axios.post(`api/characters/${characterId}/campaigns/${campaignId}`,
      {}, {});
    return response.data;

    },

  // remove from campaign
  async removeCharacterFromCampaign(characterId, campaignId) {
    const response = await axios.patch(`api/characters/${characterId}/campaigns`, {campaignId},
      {});
    return response.data;
    },

}
export default CharacterService;
