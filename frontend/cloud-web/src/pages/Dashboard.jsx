import './Dashboard.css';

import { useEffect, useState } from 'react';
import {Bar, BarChart, XAxis, YAxis, ResponsiveContainer, Cell, PieChart, Pie, Legend, Tooltip} from 'recharts';
import { useNavigate } from 'react-router-dom';

import { getSaved, deleteOffer, getEmissions} from '../lib/apiClient.js';


export default function Dashboard() {
    const COLOURS = ['#70aaec', '#eb9b1b', '#2fc26c', '#a551c7', '#e45c4d'];
    const[savedService, setSavedService] = useState([]);
    const[serviceEmissions , setServiceEmissions] = useState({}); 
    useEffect(function() {
        getSaved().then(function(saved) {
            let list  = [];
            if(Array.isArray(saved)) {
                list = saved;
            }
            //set service && fetch emissions per service.
            setSavedService(list);
            list.forEach(function(offer) {
                getEmissions(offer.offerId).then(function(emissions) {
                    if(emissions){
                        setServiceEmissions(function(prev) {
                            const newEmissions = { 
                                ...prev, [offer.offerId]: emissions 
                            };
                            return newEmissions;
                        });
                    }
            });
            });
        }).catch(function() {
            console.error("service fetch failure");
        });
    } , []);

    function handleDel(id){
        deleteOffer(id).then(function() {
            setSavedService(function(previous) {    
                return previous.filter(function(offer){
                    return offer.offerId !== id;
                });
            });
        }).catch(function() {
            console.error("service delete failure");
        });
    }

    function fetchOfferData(offer) {
        const providerCount = {};
        const priceCount = {};
        const priceTotal =  {};
        const regionCount = {};

        savedService.forEach(function(offer) {
            const provider = offer.provider;
            const region = offer.region;

            const price = Number(offer.price);

            //increment count or set to 1 if not exist
            providerCount[provider] = providerCount[provider] ? providerCount[provider] + 1:1;
            regionCount[region] = regionCount[region] ? regionCount[region] + 1:1;

            if(!isNaN(price)) {
                if(price>0) {
                    priceTotal[provider] = (priceTotal[provider] || 0) + price;
                    priceCount[provider] = (priceCount[provider] || 0) + 1;
                }
            }
        });
        const providerData = objectLoad(providerCount);
        const regionData = objectLoad(regionCount);
        const priceData = Object.keys(priceTotal).map(function(provider) {
            return{name:provider, average: Number((priceTotal[provider]/priceCount[provider]).toFixed(3))};
        });

        return{
            providerData, 
            regionData,
            priceData
        }
    }

    function objectLoad(input){
        return Object.entries(input).map(function(entry) {
            return {name: entry[0], value: entry[1]};
        });
    }

    function loadCo2Data() {
        const co2Total ={};
        
        //extyract service emissions out of parsed obj
        Object.values(serviceEmissions).forEach(function(emission) {
            const provider = emission.provider;
            const val = emission.co2KgPerMonth || emission.co2Kg || 0;
            co2Total[provider] = (co2Total[provider] || 0) +val;
        });
        const co2Data = Object.keys(co2Total).map(function(provider) {
            return{name:provider, value: Number(co2Total[provider].toFixed(3))};
        });
        
        return co2Data;

    }

    const {priceData, providerData, regionData} = fetchOfferData(savedService);
    const co2Data = loadCo2Data();

    const prices = savedService.map(function(offer) {
        return Number(offer.price);
    });

    const filteredPrices = prices.filter(function(price) {
        return Number.isFinite(price) && price > 0;
    });

    const averagePrice = filteredPrices.length ? filteredPrices.reduce((sum,price) => sum + price,0)/filteredPrices.length : 0;

    const minPrice = Math.min(...prices);
    const maxPrice = Math.max(...prices);

    return (
        <div className = "dashboard-main">
            <h2>Dashboard</h2>
            <div className="stats-cont">
                <div className="stat">
                    <div className="stat-l">Total Services</div>
                    <div className="stat-v">{savedService.length}</div></div>
                <div className="stat">
                    <div className="stat-l">Average Price</div>
                    <div className="stat-v">{averagePrice.toFixed(3)}</div></div>
                <div className="stat">
                    <div className="stat-l">Min Price</div>
                    <div className="stat-v">{minPrice.toFixed(3)}</div></div>                    
                <div className="stat">
                    <div className="stat-l">Max Price</div>
                    <div className="stat-v">{maxPrice.toFixed(3)}</div></div>    
            </div>
            <div className= "diagrams-main">
                <div className="diagram">
                    <h4>Provider Distribution</h4>
                    <ResponsiveContainer width="100%" height={320}>
                        <PieChart>
                            <Pie data={providerData} 
                            dataKey="value"
                            nameKey="name"
                            outerRadius={90}
                            innerRadius={70}
                            startAngle={90}
                            endAngle={-270}
                            >
                            {providerData.map((entry, index) => (
                                <Cell fill={COLOURS[index%COLOURS.length]} key={entry.name}/>
                            ))}
                            </Pie>
                            <Legend />
                            <Tooltip />
                        </PieChart>
                    </ResponsiveContainer>
                    </div>
                
                <div className="diagram">
                    <h4>Region Distribution</h4>
                    <ResponsiveContainer width="100%" height={320}>
                        <BarChart data = {regionData}>
                            <XAxis dataKey="name" />
                            <YAxis allowDecimals={false}/>
                            <Tooltip />
                            <Bar dataKey="value" fill="#71a9e9">
                            </Bar>
                        </BarChart>
                    </ResponsiveContainer>
                </div>

                <div className="diagram">
                    <h4>Average Price Per Provider($)</h4>
                    <ResponsiveContainer width="100%" height={320}>
                        <BarChart data = {priceData}>
                            <XAxis dataKey="name" />
                            <YAxis allowDecimals={false}/>
                            <Tooltip />
                            <Bar dataKey="average" fill="#e77cc7">
                            </Bar>
                        </BarChart>
                    </ResponsiveContainer>
                </div>

                <div className="diagram">
                    <h4>Average Emissions Per Provider(kg CO2 per month)</h4>
                    <ResponsiveContainer width="100%" height={320}>
                        <BarChart data = {co2Data}>
                            <XAxis dataKey="name" />
                            <YAxis allowDecimals={false}/>
                            <Tooltip />
                            <Bar dataKey="value" fill="#93c576">
                            </Bar>
                        </BarChart>
                    </ResponsiveContainer>
                </div>
            </div>
            <div className="services-saved">
                <h2>Saved Services</h2>
                <table className='service-table'>
                    <thead>
                        <tr>
                            <th>Offer Id</th>
                            <th>Provider</th>
                            <th>Region</th>
                            <th>Price</th>
                            <th>Unit</th>
                            <th>Co2 Kg/ph</th>
                            <th></th>
                        </tr>
                    </thead>
                    <tbody>
                        {savedService.map(function(offer) {
                            const emission = serviceEmissions[offer.offerId];
                            return(
                                <tr key={offer.offerId}>
                                <td>{offer.offerId}</td>
                                <td>{offer.provider}</td>
                                <td>{offer.region}</td>
                                <td>{offer.price}</td>
                                <td>{offer.unit}</td>
                                <td>{emission ? emission.co2KgPerHour : ''}</td>
                                <td><button onClick={function() {handleDel(offer.offerId)}} className='delete-button'>Delete</button>
                                
                                </td>
                                </tr>
                            );
                        })}
                    </tbody>
                </table>
            </div>
        </div>

                            

    );

}