import axios from 'axios';

const backendBaseUrl = process.env.REACT_APP_BACKEND_BASE_URI || '';
const chatServerUrl = process.env.REACT_APP_CHAT_SERVER_URI || '/chat';

export async function getAnimals(page = 0, size = 3) {
    return axios
        .get(`${backendBaseUrl}/animals`, {
            params: { page, size },
            headers: { 'API-Version': '2.0' },
        })
        .then(res => res.data);
}

export async function submitAdoptionRequest({animalId, email, notes}) {
    return axios.post(`${backendBaseUrl}/animals/${animalId}/adoption-requests`, {email, notes});
}

export async function editAdoptionRequest({animalId, adoptionRequestId, email, notes}) {
    return axios.put(`${backendBaseUrl}/animals/${animalId}/adoption-requests/${adoptionRequestId}`,
        {email, notes});
}

export async function deleteAdoptionRequest({animalId, adoptionRequestId}) {
    return axios.delete(`${backendBaseUrl}/animals/${animalId}/adoption-requests/${adoptionRequestId}`);
}

export function sendChatMessage({message, history, username}) {
    const headers = {'Content-Type': 'application/json'};
    if (username) {
        headers['X-Authenticated'] = 'true';
    }
    return fetch(`${chatServerUrl}`, {
        method: 'POST',
        headers,
        credentials: 'include',
        body: JSON.stringify({message, history}),
    });
}

export async function getUsername() {
    return axios
        .get(`${backendBaseUrl}/whoami`, { transformResponse: [data  => data] } ) // Transform data to string. Google auth username is in form of bigint so frontend may have trouble handling it without this transformation.
        .then(res => {
            if (res.request.responseURL && !res.request.responseURL.endsWith('whoami')) {
                return '';
            }
            return res.data;
        })
        .catch(err => {
            console.error('Failed to fetched username', err);
            return '';
        });
}
