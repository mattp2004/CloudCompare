import { useState, useEffect } from "react";
import "./Catalogue.css";

import { getToken } from "../lib/apiClient.js";

//icons
import awsIcon from "../assets/aws.png";
import azureIcon from "../assets/azure.png";
import gcpIcon from "../assets/gcp.png";

import FilterBar from "../components/Filterbar.jsx";
import {getServices,getOffers,getPricing,saveOffer,getSaved } from "../lib/apiClient.js";

export default function Catalogue() {
    const [services, setServices] = useState([]);
    const [selectedService, setSelectedService] = useState(null);

    const [offers, setOffers] = useState([]);
    const [prices, setPrices] = useState({});

    const [savedIds, setSavedIds] = useState([]);
    const [totalCount, setTotalCount] = useState(0);
    const [currentPage, setCurrentPage] = useState(1);
    const [pageSize, setPageSize] = useState(50);

    const [search, setSearch] = useState("");

    const [provider, setProvider] = useState("");
    const [region, setRegion] = useState("");
    const [unit, setUnit] = useState("");
    const [offerType, setOfferType] = useState("");

    const [orderBy, setOrderBy] = useState("");
    const [saveMsg, setSaveMsg] = useState("");

    useEffect(function() {
        getServices().then(function(data) {
            let list;
            if(Array.isArray(data)) list = data;
            else{
                list = [];
            }
            setServices(list);

            if(list != null){
                setSelectedService(list[0].id);
                fetchOffers(list[0].id, 1);
            }

        }).catch(function(error) { 
            console.error(error); 
            setServices([]);
        });

        getSaved().then(function(data) {
            let list;
            if(Array.isArray(data)) list = data;
            
            setSavedIds(list.map(function(item) {
                 return item.offerId; 
                }));
        }).catch(function() { 
            setSavedIds([]);
         });
    }, []);

    
    function fetchOffers(serviceId, page) {
        const params = { serviceId, page, size: pageSize };
        if (search){
            params.q = search;
        }
        if (provider){
            params.provider = provider;   
        }
        if (region){
            params.region = region;
        } 
        if (unit){
            params.unit = unit;
        }
        if (offerType){
            params.type = offerType;
        }
        if (orderBy){
            params.orderBy = orderBy;
        }
        getOffers(params).then(function(data) {
            let offerList;
            offerList = data.offers;

            setOffers(offerList);
            setTotalCount(data.total);

            setCurrentPage(page);

            offerList.forEach(function(offer) {
                 fetchPrice(offer.id);
            });
        }).catch(function(error) { console.error(error); });
    }
      


    async function fetchPrice(offerId) {
        if(offerId ==null) return;
        try{
            const data = await getPricing(offerId);
            if (!data) return;
            const dprice = data.price
            const dunit = data.unit
            if (data) setPrices(function(prev) { return { ...prev, [offerId]: { price: dprice, unit: dunit } }; });
        }
        catch(error){
            console.error("Failed to fetch price for offer", offerId, error);
        }
    }

    function handleSave(offer) {
        console.log("token:", getToken());
        saveOffer(offer.id).then(function() {
            setSavedIds(function(prev) { return [...prev, offer.id]; });
            setSaveMsg("Saved Service!");
            setTimeout(function() { setSaveMsg(""); }, 2500);
        }).catch(function(error) { setSaveMsg(error.message || "Failed to save"); });
    }

    function formatPrice(offerId) {
        const p = prices[offerId];

        if (!p) return "..";
        if (p.price == null) return "N/A";
        const priceFormat = Number(p.price).toFixed(4) + "/" + p.unit;
        return "$" + priceFormat;
    }

    function getProviderIcon(provider) {
        const key =(provider || '').toLowerCase();
        if (key.includes("aws")) return awsIcon;
        if (key.includes("azure")) return azureIcon;
        if (key.includes("gcp")) return gcpIcon;
        return null;
    }

    const totalPages = Math.ceil(totalCount / pageSize);
    const currentServiceName = services.find(function(s) { return s.id === selectedService; })?.name || "";

    return (
        <div className="catalogue-page">
            <div className="catalogue-layout">
                <div className="service-sidebar">
                    {services.map(function(service) {
                        return (
                            <button key={service.id}
                                className={selectedService === service.id ? "service-button active" : "service-button"}
                                onClick={
                                    function() { 
                                        setSelectedService(service.id); 
                                        fetchOffers(service.id, 1); 
                                        }}>
                                <div className="service-button-title">
                                    {service.name?.toLowerCase().includes("virtual") ? "🖥️" : ""}

                                    {service.name?.toLowerCase().includes("storage") ? "🗄️ " : ""}

                                    {service.name?.toLowerCase().includes("database") ? "🗃️ " : ""}

                                    {service.name}
                                </div>
                                <div className="service-button-desc">{service.description}</div>
                            </button>
                        );
                    })}
                    <div className="page-info">Page {currentPage}/{totalPages}</div>
                </div>

                <div className="catalogue-content">
                    <FilterBar
                        searchValue={search} provider={provider} region={region} unit={unit} offerType={offerType} orderBy={orderBy} onSearchChange={setSearch} onProviderChange={setProvider} onRegionChange={setRegion} onUnitChange= {setUnit} onOfferTypeChange={setOfferType} onOrderByChange={setOrderBy}
                            onSearch={function() { if(selectedService) fetchOffers(selectedService, 1); }}
                    />

                    <div className="catalogue-controls">
                        <span className="catalogue-info">
                            Showing results for <strong>{currentServiceName}</strong> - {totalCount} offers • {totalPages} pages • {pageSize} per page
                        </span>
                        <div className="catalogue-actions">
                            <select className="page-size-select" value={pageSize}
                                onChange= {function(e) { setPageSize(Number(e.target.value)); fetchOffers(selectedService, 1); }}>
                                <option value={25}>25</option>
                                <option value={50}>50</option>
                                <option value={100}>100</option>
                            </select>

                            {saveMsg && <span className="save-msg">{saveMsg}</span>}
                            <button className="page-button" disabled={currentPage === 1}
                                onClick={function() { fetchOffers(selectedService,currentPage - 1); }}>Previous</button>
                            <span>Page {currentPage} of {totalPages}</span>
                            <button className="page-button" disabled={currentPage===totalPages}
                                onClick={function() { fetchOffers(selectedService, currentPage+ 1); }}>Next</button>
                        </div>
                    </div>

                    <table className="offers-table">
                        <thead>
                            <tr>
                                <th>ID</th>
                                <th>Name</th>
                                <th>Provider</th>
                                <th>Region</th>
                                <th>Type</th>
                                <th>Price</th>
                                <th>Actions</th>
                            </tr>
                        </thead>
                        <tbody>
                            {offers.map(function(offer, i) {
                                const icon = getProviderIcon(offer.provider);
                                return (
                                    <tr key={offer.id}>
                                        <td><span className="offer-id">{offer.id}</span></td>
                                        <td>{offer.name}</td>
                                        <td>
                                            {icon && <img src={icon} alt={offer.provider} className="provider-icon" />}
                                            {offer.provider}
                                        </td>
                                        <td>{offer.region}</td>
                                        <td>{offer.offerType}</td>
                                        <td>{formatPrice(offer.id)}</td>
                                        <td>
                                            <button className="save-button"
                                                onClick={function() { handleSave(offer); }}
                                                //disable button if saved already
                                                disabled={savedIds.includes(offer.id)}>
                                                {savedIds.includes(offer.id) ? "Saved" : "Save"}

                                            </button>
                                        </td>
                                    </tr>
                                    
                                );
                            })}
                        </tbody>
                    </table>
                </div>

            </div>
        </div>
    );

}