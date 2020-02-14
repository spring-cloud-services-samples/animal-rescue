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
            mode: 'no-cors', // TODO: this should go away once CORS issue is solved on gateway, then no need to use fetch,
            credentials: 'include',
        };
        return fetch(new Request(testURL, myInit)) // #TODO: this is a hack, we make a request to set session cookie for this domain.
            .then(response => {
                console.log(response);
                return axios
                    .post(`${this.#backendBaseUrl}/animals/${animalId}/adoption-requests`, {
                        email,
                        notes,
                    })
            });
    }

    async signIn() {
        const testURL = `${this.#backendBaseUrl}/whoami`;
        const myInit = {
            method: 'GET',
            mode: 'no-cors', // TODO: this should go away once CORS issue is solved on gateway, then we can switch back to axios
            credentials: 'include'
        };
        return fetch(new Request(testURL, myInit))
            .then(response => {
                console.log(response);
                return axios
                    .get(`${this.#backendBaseUrl}/whoami`, {withCredentials: true});
            });

    }
}
