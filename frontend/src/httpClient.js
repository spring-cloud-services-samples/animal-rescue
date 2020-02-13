import axios from 'axios';

export default class HttpClient {

    #backendBaseUrl = process.env.REACT_APP_BACKEND_BASE_URL || '';

    async getAnimals() {
        return axios
            .get(`${this.#backendBaseUrl}/animals`)
            .then(res => res.data);
    }

    async submitAdoptionRequest({animalId, email, notes}) {
        const testURL = `${this.#backendBaseUrl}/animals/${animalId}/adoption-requests`;
        const myInit = {
            method: 'POST',
            body: JSON.stringify({email, notes}),
            mode: 'no-cors',
            withCredentials: true,
        };
        return fetch(new Request(testURL, myInit));
        // return axios
        //     .post(`${this.#backendBaseUrl}/animals/${animalId}/adoption-requests`, {
        //         email,
        //         notes,
        //     }, {withCredentials: true});
    }

    async signIn() {
        const testURL = `${this.#backendBaseUrl}/whoami`;
        const myInit = {
            method: 'GET',
            mode: 'no-cors',
            credentials: 'include',
            // withCredentials: true,
        };
        return fetch(new Request(testURL, myInit));
        // return axios
        //     .get(`${this.#backendBaseUrl}/whoami`, {withCredentials: true});
    }
}
