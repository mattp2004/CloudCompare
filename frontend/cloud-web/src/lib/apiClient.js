const BASE_URL = "/api";
export const getToken=()=> localStorage.getItem("token");
export const setToken=(token)=> localStorage.setItem("token", token);
export const clearToken = () => localStorage.removeItem('token');

async function request(endpoint, options = {}) {
    const token = getToken();
    const headers = {
        "Content-Type": "application/json",
        ...options.headers
    };

    if(token){
        headers["Authorization"] = `Bearer ${token}`;
    }
    let response;
    try{
        response = await fetch(`${BASE_URL}${endpoint}`, {
            ...options,
            headers});
    }
    catch(e){console.error("API error: ", e); throw new Error("Network error");}

    if(response.status == 401){
        setToken(null);
        window.location.href = '/login';
        throw new Error("Unauthorised.");
    }
    if(!response.ok){
        const errorInfo = await response.json();
        throw new Error(errorInfo.message);
    }
    //nothing
    if(response.status === 204) return null;
    const text = await response.text();
    if (!text || text.trim() === '') return null;
    return JSON.parse(text);
}

export function login(username, password){
    return request("/auth/login", {
        method: "POST",
        body: JSON.stringify({
            username,
            password
        }),
    });
}

export function getServices(){
    return request("/services");
}

//Offers & pricing
export function getOffers(param){
    return request("/offers?" + new URLSearchParams(param));
}

export function getPricing(offerId){
    return request(`/prices/${encodeURIComponent(offerId)}`);
}

//Comparison models 
export function getComparison(offerId, limit=20){
    return request(`/compare/${encodeURIComponent(offerId)}?limit=${limit}`);
}

export function getAiComparison(offerId, offer2Id){
    return request(`/compare/ai/${encodeURIComponent(offerId)}?offer2Id=${encodeURIComponent(offer2Id)}`);
}

//saved / account
export function getSaved(){
    return request("/saved");
}

export function saveOffer(offerId){
    return request(`/saved/${encodeURIComponent(offerId)}`, {
        method:"POST",
    });
}
export function deleteOffer(offerId){
    return request(`/saved/${encodeURIComponent(offerId)}`, {
        method:"DELETE",
    });
}

export function getOffersMap(params){
    return request(`/offers/map?${new URLSearchParams(params)}`);
}

export function getDataTransfer(params){
    return request(`/offers/dt?${new URLSearchParams(params)}`);
}

export function getHealth(){
    return request("/health");
}

export function getAdminStatus(){
    return request("/admin/status");
}

export function runIngestion(provider){
    return request(`/admin/ingest/${provider}`);
}
export function getEmissions(offerId) {
    return request(`/emissions/${encodeURIComponent(offerId)}`);
}
export function getEmissionsMap(params) {
    return request(`/emissions/map?${new URLSearchParams(params)}`);
}