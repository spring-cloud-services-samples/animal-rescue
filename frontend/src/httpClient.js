import axios from 'axios';

const backendBaseUrl = process.env.REACT_APP_BACKEND_BASE_URI || '';
const logoutUrl = process.env.REACT_APP_LOGOUT_URI || '/scg-logout';

export async function getAnimals() {
    return axios
        .get(`${backendBaseUrl}/animals`)
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

export async function getUsername() {
    return axios
        .get(`${backendBaseUrl}/whoami`)
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

export async function logoutFromGateway() {
    // Fetch and set the csrf cookie
    await axios(logoutUrl);

    // Pass cookie value in defined Gateway CSRF header
    let csrfToken = document.cookie.replace(/.*\bSCG-XSRF-TOKEN=([^;]+).*/, "$1");

    return axios.post(logoutUrl, null, {
        headers: {
            "X-SCG-XSRF-TOKEN": csrfToken,
        },
    });
}
