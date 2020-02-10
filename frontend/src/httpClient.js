import axios from 'axios';

export default class HttpClient {

    #backendBaseUrl = process.env.REACT_APP_BACKEND_BASE_URL || '';

    async getAnimals() {
        return axios.get(`${this.#backendBaseUrl}/animals`)
                    .then(res => res.data);
    }

    async submitAdoptionRequest({animalId, email, notes}) {
        return axios.post(`${this.#backendBaseUrl}/animals/${animalId}/adoption-requests`, {
            email,
            notes,
        })
    }
}
