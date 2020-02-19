import axios from 'axios';

export default class HttpClient {

    #backendBaseUrl = process.env.REACT_APP_BACKEND_BASE_URL || '';

    constructor() {
        axios.defaults.withCredentials = true;
    }

    async getAnimals() {
        return axios
            .get(`${this.#backendBaseUrl}/animals`)
            .then(res => res.data);
    }

    async submitAdoptionRequest({animalId, email, notes}) {
        const request = () =>
            axios.post(`${this.#backendBaseUrl}/animals/${animalId}/adoption-requests`,
                {email, notes});
        return this.hackYesGimmeDaCookies()
            .then(request);
    }

    async editAdoptionRequest({animalId, adoptionRequestId, email, notes}) {
        const request = () =>
            axios.put(`${this.#backendBaseUrl}/animals/${animalId}/adoption-requests/${adoptionRequestId}`,
                {email, notes});
        return this.hackYesGimmeDaCookies()
            .then(request);
    }

    async deleteAdoptionRequest({animalId, adoptionRequestId}) {
        const request = () =>
            axios.delete(`${this.#backendBaseUrl}/animals/${animalId}/adoption-requests/${adoptionRequestId}`);
        return this.hackYesGimmeDaCookies()
            .then(request);
    }

    async getUsername() {
        const testURL = `${this.#backendBaseUrl}/whoami`;
        const request = () => axios.get(testURL).then(res => res.data);
        return this.hackYesGimmeDaCookies()
            .then(response => {
                if (response.url.endsWith(testURL)) {
                    return response.text()
                } else {
                    return request();
                }
            });

    }

    async hackYesGimmeDaCookies() {
        const testURL = `${this.#backendBaseUrl}/whoami`;
        const myInit = {
            method: 'GET',
            mode: 'no-cors', // TODO: this should go away once CORS issue is solved on gateway, then we can switch back to axios
            credentials: 'include',
        };
        return fetch(new Request(testURL, myInit))
    }
}
