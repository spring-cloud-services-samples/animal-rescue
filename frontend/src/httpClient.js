import axios from 'axios';

export default class HttpClient {

    #backendBaseUrl = process.env.REACT_APP_BACKEND_BASE_URL || '';

    async getAnimals() {
        return axios
            .get(`${this.#backendBaseUrl}/animals`)
            .then(res => res.data);
    }

    async submitAdoptionRequest({animalId, email, notes}) {
        let postRequest = axios
            .post(`${this.#backendBaseUrl}/animals/${animalId}/adoption-requests`, {
                email,
                notes,
            }, {withCredentials: true});
        return postRequest
            .catch(e => {
                console.info('in catch', e);
                return this.signIn().then(() => postRequest)
            });
    }

    async signIn() {
        return axios
            .get(`${this.#backendBaseUrl}/whoami`)
            .then(res => {
                if (res && res.request && res.request.responseURL) {
                    console.log('whoami', res);
                    window.location = res.request.responseURL;
                }
                return res;
            });
    }
}
