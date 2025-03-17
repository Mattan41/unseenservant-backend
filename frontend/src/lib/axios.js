import Axios from 'axios';

const axios = Axios.create({

  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 60000,
  withCredentials: true,
  withXSRFToken: true,
// todo: define api an use throughout the app, makes it easier to change the api interface if needed
  // todo: is an axios service needed or can we set it here?
  // if no Xsrf token is set, the server will should a 403 error,
  // and we probably get a new token with the response, should we make another request with the new token immediately, at least once?

  // todo: set up error handling for axios, and use it throughout the app. Remember to set upp error in backend as well
});

export default axios;
