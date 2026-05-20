import { useEffect, useRef, useState } from "react";
import leaflet from "leaflet";
import { getEmissionsMap, getOffersMap, getDataTransfer, getServices } from "../lib/apiClient.js";
import "./Map.css";
import transferCoords from "../components/TransferCoords.json";
import { REGION_COORDS } from "../components/constants";
import { Overlay } from "react-bootstrap";
import 'leaflet/dist/leaflet.css';
export default function Map() {

    const map = useRef(null);
    const layer = useRef(null);
    const transferLayer = useRef(null);

    const[services, setServices] = useState([]);
    const[serviceId, setServiceId] = useState('');

    const[mode, setMode] = useState('overview');
    const[overlayMode, setOverlayMode] = useState('price');
    const[provider, setProvider] = useState('');

    const[mapData, setMapData] = useState([]);
    const[emissionData, setEmissionData] = useState([]);
    const[transferData, setTransferData] = useState([]);

    const[loadStatus, setloadStatus] = useState('waiting');
    const[dtOrigin, setdtOrigin] = useState('');

    function getTransferCoords(transferLoc) {
        if(!transferLoc) return null;
        return transferCoords[transferLoc.toUpperCase()] || "null";
    }

    function getColour(value, min, max) {
        if(max === min) {
            return 'rgb(133, 226, 109)';
        }
        const total = max - min;
        const percentage = (value - min) / total;
        const red = Math.floor(255 *percentage);

        const green = Math.floor(255 * (1 - percentage));

        const colour = "rgba(" + red + "," + green + ",0,0.5)";
        return colour;
    }

    useEffect(function() {
        getServices().then(function(saved) {
            let list  = [];
            if(Array.isArray(saved)) {
                list = saved;
            }
            setServices(list);
            if(list.length > 0) {
                setServiceId(list[0].id);
            }
        }).catch(function() {
            console.error("service fetch failure");
        });
    } , []);

    useEffect(function() {
        const lmap = leaflet.map('map').setView([20, 0], 2);
        leaflet.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', { attribution: '© - OpenStreetMap' }).addTo(lmap);
        map.current = lmap;
        layer.current = leaflet.layerGroup().addTo(lmap);
        transferLayer.current = leaflet.layerGroup().addTo(lmap);
        return function() {
            lmap.remove();
        }
    },[]);

    //load overview & emissions 
    useEffect(function() {
        if(!serviceId) {return;}
        setloadStatus('loading');
        const apiParam = {serviceId};
        if(provider){ apiParam.provider = provider;}
        Promise.all([
            getOffersMap(apiParam).catch(function() {
                console.error("failed to retrieve offer map"); return [];
            }
            ),
            getEmissionsMap(apiParam).catch(function() {
                console.error("failed to retrieve emissions map."); return [];
            }
            )
        ]).then(function([offers, emissions]) {
            setMapData(offers);
            setEmissionData(emissions);
            layer.current.clearLayers();

        }).finally(function() {
            setloadStatus('loaded');
        })
    },[serviceId, provider]);


    //when serviceid changes load dt 
    useEffect(function() {
        if(!serviceId) {return;}
        setloadStatus('loading');
        getDataTransfer({serviceId}).then(function(data) {
            if(Array.isArray(data)) {
                setTransferData(data);
            }
        }).catch(function() {console.error("error in retrieve data transfer.");
        });

    },[serviceId]);


    //overlay drawing 

    useEffect(function() {
        if(!layer.current) {
            return;
        }
        layer.current.clearLayers();
        if(mode != 'overview') {
            return;
        }

        let data; 
        if(overlayMode === 'price') {
            data = mapData;
        } else if(overlayMode === 'co2') {
            data = emissionData;
        }
        else{
            data = mapData;
        }

        if(!data || data.length === 0) {
            console.log("no data to display");
            return;
        }

        function retrieveValues(item) {
            if(overlayMode === 'price') {
                console.log("avg price", item);
                return item.minPrice || 0;
            } else if(overlayMode === 'co2') {
                console.log("avg co2", item);
                return item.avgCo2KgPerHour || 0;
            }
            else if(overlayMode === 'frequency') {
                console.log("frequency", item);
                return item.offerCount || 0;
            }
            return 0;
        }

        const values = data.map(retrieveValues);
        const maxValue = Math.max(...values);
        const minValue = Math.min(...values);

        const buckets = {};

        data.forEach(function(item) {
            if(!item.region || !REGION_COORDS[item.region] || item.region === "UNKNOWN") {
                return;
            }
            if(!buckets[item.region]){
                buckets[item.region] = item;
            }
        });
        Object.values(buckets).forEach(function(item) {           
            const region = item.region; 
            const coordinates = REGION_COORDS[region];
            if(!coordinates) {return;}
            const value = retrieveValues(item);
            const colour = getColour(value, minValue, maxValue); 

            let label = item.region + "<br>";
            if(overlayMode === 'price') {
                label += "Average Price: $" + value.toFixed(5);
            }
            else if(overlayMode === 'co2') {
                label += "Average Co2: " + value.toFixed(3) + " kg/h";
            }
            else if (overlayMode === 'frequency') {
                label += "Offer Count: " + value;
            }
            const graphic_radius = Math.max(120000, Math.sqrt(value) * 1200);
            const circle = leaflet.circle(coordinates, {
                radius: graphic_radius,
                color: colour,
                fillColor: colour,
                fillOpacity: 0.5,
                opacity : 0.5
            }).bindPopup(label).addTo(layer.current);

        });
    }, [overlayMode, mapData, emissionData,mode]);


    //Draw dt overlay hurrah
    useEffect(function() {
        if(!transferLayer.current) { return;}
        transferLayer.current.clearLayers();
        if(mode != "transfer") { 
            setdtOrigin('');
            return;}
        if(transferData.length === 0) { return;}

        const origins = {};
        transferData.forEach(function(transfer) {
            const from = transfer.from_location;
            if(!from) { return;}
            const coords = getTransferCoords(from);
            if(coords === "null") { return;}
            if(coords){
                origins[from] = coords;
            }
        });

        Object.entries(origins).forEach(function([origin, coords]) {
            const isSelected = dtOrigin &&origin.toUpperCase() ===dtOrigin.toUpperCase();
            const circle = leaflet.circle(coords, {
                radius: isSelected ? 50000: 20000,
                color: isSelected ? "#3767eb" : "#647194",
                fillColor: isSelected ? '#2753ce' : '#70c9f0',
                fillOpacity: 0.5,
                weight: 1.5

            }).bindTooltip(origin, {direction: "top"}).on('click', function() {
                setdtOrigin(origin);
            }).addTo(transferLayer.current);
        });

        if(!dtOrigin) { return;}
        const filteredTransfers = transferData.filter(function(transfer) {
            return transfer.from_location && transfer.from_location.toUpperCase() === dtOrigin.toUpperCase();
            }).slice(0,50).forEach(function(transfer) {
                const fromCoords = getTransferCoords(transfer.from_location)
                const toCoords = getTransferCoords(transfer.to_location)

                const transferMonthly = transfer.avg_monthly_gib_transfer ?? 0;

                const popUpMsg =
                "From " + transfer.from_location + " to " + transfer.to_location + "<br>" +
                "Avg Price: $" + (transfer.avg_price || "N/A") + "<br>" +
                "Avg transfer: " + transferMonthly.toFixed(5) + " GiB/month";

                if(fromCoords === "null" || toCoords === "null" || !fromCoords || !toCoords) { return;}
                leaflet.polyline([fromCoords, toCoords], {
                    color: "#68b4d4",
                    weight: 1,
                    opacity: 0.5
                }).bindPopup(popUpMsg).addTo(transferLayer.current);
            });
                
        }, [transferData,dtOrigin,mode]);

    return(
        <div className="main-page">
            <div className="layout">
                <aside className="map-side">
                    <h4>Map Settings</h4>

                    <h5>Service</h5>
                    <select className="select" value={serviceId} onChange={function(e) {setServiceId(e.target.value);}}>
                        {services.map(function(service) {
                            return <option key={service.id } value={service.id}>{service.name}</option>
                        })}
                    </select>

                    <h5>Provider</h5>
                    <select className="select" value={provider} onChange={function(e) {setProvider(e.target.value);}}>
                        <option value="">All</option>
                        <option value="AWS">AWS</option>
                        <option value="Azure">Azure</option>
                        <option value="GCP">GCP</option>
                    </select>

                    
                    <h5>View</h5>
                    <button className={mode == "overview" ? "button-map-active" : "button-map"}onClick={function() {setMode("overview");}}>
                        Overview
                    </button>
                    <button className={mode == "transfer" ? "button-map-active" : "button-map"}onClick={function() {setMode("transfer");}}>
                        Transfer
                    </button>

                    {mode === "overview" && (
                        <>
                        <h5>Overlay</h5>
                        <button className={overlayMode == "price" ? "button-map-active" : "button-map"}onClick={function() {setOverlayMode("price");}}>
                            Price
                        </button>
                        <button className={overlayMode == "co2" ? "button-map-active" : "button-map"}onClick={function() {setOverlayMode("co2");}}>
                            Co2
                        </button>
                        <button className={overlayMode == "frequency" ? "button-map-active" : "button-map"}onClick={function() {setOverlayMode("frequency");}}>
                            Frequency
                        </button>
                        </>
                    )}

                    {mode === "transfer" && (
                        <>
                        <h5>Data Transfer Options</h5>
                        <select value ={dtOrigin} onChange={function(e) {setdtOrigin(e.target.value);}} className="select">
                            <option value="">Select origin </option>
                            {[...new Set(transferData.map(function(option) {return option.from_location; }).filter(Boolean))].sort().map(function(origin) {
                                return <option key={origin}value={origin}>{origin}</option>
                            })}
                        </select>
                        </>)}
                        {dtOrigin && (
                        <div className="transfer-list">
                            {transferData
                            .filter(function(options) {
                                return options.from_location &&
                                options.from_location.toUpperCase() === dtOrigin.toUpperCase();
                            })
                            .map(function(r, num) {
                                return (
                                    <div key={num}>
                                        <span>{"⮕ " +r.to_location + " " || "?"}</span>
                                        <span>
                                        ${Number(r.price ?? 0).toFixed(5)}
                                        {r.unit ? `/${r.unit}` : ""}
                                        </span>
                                    </div>
                                );
                            })}
                        </div>
                        )}

                    <div className="legend">
                        <div className="legend-content"><span style={{ backgroundColor: "rgb(148, 236, 125)" }}></span>Low</div>
                        <div className="legend-content"><span style={{ backgroundColor: "rgb(247, 247, 11)" }}></span>Medium</div>
                        <div className="legend-content"><span style={{ backgroundColor: "rgb(248, 9, 9)" }}></span>High</div>
                    </div>

                </aside>
                <div id="map" className="map-content"></div>

            </div>
        </div>

    );    
}
