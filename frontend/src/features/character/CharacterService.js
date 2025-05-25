import axios from '@/lib/axios.js'

const CharacterService = {

  // create a character
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

  async updateCharacter(characterId, data) {
    const response = await axios.patch(`api/characters/${characterId}`, data, {
    });
    return response.data;
  },

  async updateCharacterField(characterId, field, value) {
    const response = await axios.patch(`api/characters/${characterId}`, {
      [field]: value,
    }, {
    });

    return response.data;
  },

  async uploadCharacterImage(characterId, imageFile) {

    const formData = new FormData();
    formData.append('file', imageFile);

    const response = await axios.post(`api/characters/${characterId}/image`, formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
    return response.data;
  },

  // delete a character
  async deleteCharacter(characterId) {
    const response = await axios.delete(`api/characters/${characterId}`, {
    });
    return response.data;
  },
}
export default CharacterService;
