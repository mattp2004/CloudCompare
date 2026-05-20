import { useEffect, useState,  } from 'react';

//icons 
import awsIcon from '../assets/aws.png';
import azureIcon from '../assets/azure.png';
import gcpIcon from '../assets/gcp.png';

import { getSaved, getComparison, getAiComparison, getPricing, getEmissions, saveOffer} from '../lib/apiClient.js';
import './Compare.css';
import { useNavigate } from 'react-router-dom';
import FilterBar from "../components/Filterbar.jsx";

export default function Compare() {
    const [mode, setMode] = useState('alternatives');
    
    const [saved, setSaved] = useState([]);
    const [selected, setSelected] = useState({ 
        alternatives: [], 
        direct: [], 
        compatibility: [] 
    });
    const [priceList, setpriceList] = useState({});
    const [altResults, setAlternativeOffer] = useState([]);

    const [status, setStatus] = useState({
        loading: false,
        text : '',
        type : 'info', //error/success.
        result: null
    });

    
    function getProviderIcon(provider) {
        const code =(provider || '').toLowerCase();
        if (code.includes("aws")) return awsIcon;
        if (code.includes("azure")) return azureIcon;
        if (code.includes("gcp")) return gcpIcon;

        return "null";
    }
    
    useEffect(function() {
        getSaved().then(function(saved) {
            let list  = [];
            if(Array.isArray(saved)) {
                list = saved;
            }
            setSaved(list);
            list.forEach(function(item) {
                if(item.offerId) {
                    fetchPrice(item.offerId);
                }
            });
        }).catch(function() {
            setSaved([]);
            console.error("service fetch failure");
        });
    } , []);

    async function fetchPrice(offerId) {
        if(offerId ==null) return;
        try{
            const data = await getPricing(offerId);
            if (!data) return;
            const dprice = data.price
            const dunit = data.unit
            if (data) setpriceList(function(prev) { return { ...prev, [offerId]: { price: dprice, unit: dunit } }; });
        }
        catch(error){
            console.error("Failed to fetch price for offer id: ", offerId, error);
        }
    }


    function formatPrice(offerId) {
        const p = priceList[offerId];

        if (!p) return "?";
        if (p.price == null) return "N/A";
        const priceFormat = Number(p.price).toFixed(3) + "/" + p.unit;
        return "$" + priceFormat;
    }
    

    function addToComparison(card) {
        const id = card.offerId; 
        
        setSelected(function(previous) {
            const existsAlready = previous[mode].some(function(entity){
                return entity.id === id;
            }
            );
            if(existsAlready) return { ...previous, [mode]: previous[mode].filter(function(item) { return item.id !== id; }) };
            return { ...previous, [mode]: [...previous[mode], { ...card, id }] };
        });
        fetchPrice(id);
    }

    function deleteItem(id) {
        setSelected(function(previous) {return { ...previous, [mode]: previous[mode].filter(function(item) { return item.id !== id; }) };
        });
    }

    function onDragStart(card) {
        return function(event) {
            event.dataTransfer.setData('text/plain', JSON.stringify(card));
        };
    }

    function onDrop(event) {
        event.preventDefault();
        try {
            const card = JSON.parse(event.dataTransfer.getData('text/plain'));
            addToComparison(card);
        } catch(err) {}
    }

    function allowDrop(event) { event.preventDefault(); }

    function handleFindAlternatives() {

        const selectedOffer = selected.alternatives[0];
        if(!selectedOffer || selectedOffer.id == null) return;
        
        setStatus({ loading:true, text: "Finding alternatives..", type: "info", result: null });
        
        getComparison(selectedOffer.id, 25).then(function(comparison) {
            const list = comparison.comparedOffers;
            const prices = comparison.comparedPrices;

            const mapped = list.map(function(offer, i) {
                const price = prices[i];
                return { id: offer.id, title: offer.name || offer.id, provider: offer.provider || '', region: offer.region || '', serviceId: offer.serviceId || '', price: price ? price.price : null, unit: price ? price.unit : '' };
            });
            
            const mappedWithPrice = list.map((offer,i)=> {
                const t_price = prices[i];
                return{
                    id: offer.id,
                    title: offer.name,
                    provider: offer.provider || "UNKNOWN",
                    region: offer.region || "UNMAPPED",
                    serviceId: offer.serviceId,
                    price: t_price.price,
                    unit: t_price.unit
                }
            });

            const newPrices = mappedWithPrice.reduce(function(a, offer) {
                if(offer.price != null && offer.unit !=null){
                    a[offer.id] = {
                        price: offer.price,
                        unit: offer.unit,
                    };
                    }
                return a;
            }, {});

            setpriceList(function(prev) { return { 
                ...prev
                , ...newPrices 
            }; 
            });

            const num = mapped.length;
            setStatus ({ loading: false, text: "Alternatives found: " + num, type:  "sucess", result: null });
            setAlternativeOffer(mapped);

        }).catch(function() { setAlternativeOffer([]); 
            setStatus({loading: false, text: "Failed to find alternatives.", type: "error", result: null });

        });
    }

    function handleRunCompatibility() {
        if(selected.compatibility.length < 2|| selected.compatibility.length > 2) { setStatus({ loading: false, text: 'Select two services.', type: 'error', result: null }); return; }
        const [c1, c2] = selected.compatibility;
        setStatus({
            loading: true,
            text: "Running compatibility checker..",
            type: "info",
            result: null
        })

        getAiComparison(c1.id, c2.id).then(function(data) {
            setStatus({
                loading: false,
                text: "Compatiblity check completed",
                type: "success",
                result: data
            })
        }).catch(function()
         { 
            setStatus({
                loading: false,
                text: "Compatibility check failed",
                type: "error",
                result: null 
            })
         })
        .finally(function() { setCompatLoading(false); });
    }

    function displayCard(card, onAdd, onRemove, addLabel, draggable) {
        const icon = getProviderIcon(card.provider);
        const id = card.id || card.offerId;
        return (
            <div key={id} className="compare-card"
                draggable={!!draggable}
                onDragStart={draggable ? onDragStart(card) : undefined}>
                <div className="compare-card-title">{card.title || card.offerId || id}</div>
                <div className="compare-card-meta">
                    {icon && <img src={icon} alt={card.provider} className="provider-icon" />}
                    {card.provider}{card.region ? ' • ' + card.region : ''}
                </div>
                <div className="compare-card-price">{formatPrice(id)}</div>
                <div className="compare-card-actions">
                    {onAdd && <button className="compare-btn" onClick={function() { onAdd(card); }}>{addLabel || 'Add'}</button>}
                    {onRemove && <button className="compare-btn remove" onClick={function() { onRemove(id); }}>Remove</button>}
                </div>
            </div>
        );
    }

    function CompareCard({card, draggable=false, children}){
        const id = card.id || card.offerId;
        const icon = getProviderIcon(card.provider);
        return (
            <div className = "compare-card" draggable = {draggable} onDragStart={draggable?onDragStart(card):undefined}>
                {/* <div classname="compare-card-title">{card.title || card.offerId || id}</div> */}
                <h6>{card.title || card.offerId || id}</h6>

                <div className ="compare-card-meta">{icon&& <img src={icon} alt = {card.provider} className="provider-icon"/>}
                    <span>
                        {card.provider}{card.region ? "-"  + card.region : ""}
                    </span>
                </div>
                <div className="compare-card-price">{formatPrice(id)}</div>
                <div className="compare-card-actions">{children}</div>
            </div>
        )
    }

    const prices = selected.direct.map(function(c) { return priceList[c.id] ? Number(priceList[c.id].price) : null; });
    const validPrices = prices.filter(function(price) {
         return price != null; 
        });
    const minPrice = validPrices.length ? Math.min(...validPrices) : null;
    const maxPrice = validPrices.length ? Math.max(...validPrices) : null;

    return (
        <div className="compare-page">
            <aside className="compare-sidebar">
                <h3>Comparison Modes</h3>
                <button className={mode === "alternatives" ? "compare-tab active" : 'compare-tab'} 
                    onClick={function() { setMode("alternatives"); }}>Find Alternatives</button>
                <button className={mode === "direct" ? "compare-tab active" : "compare-tab"}
                    onClick={function() { setMode("direct"); }}>Direct Comparison</button>
                <button className={mode === "compatibility" ? "compare-tab active" : "compare-tab"}
                    onClick={function() { setMode("compatibility"); }}>Compatibility Checker (AI)</button>
            </aside>

            <div className="compare-saved">
                <div className="compare-panel">
                <h4>Saved Services</h4>
                {saved.length === 0 && <p>No saved services.</p>}
                    <div className="compare-card-list">
                        {saved.map(function(card){
                            const id = card.offerId;
                            return(
                                <CompareCard card={card} key={id}draggable={true}>
                                    <button className="compare-btn add" onClick={function() 
                                        { addToComparison(card); }}>
                                            Add
                                    </button>
                                </CompareCard>
                            );
                        })}
                    </div>
                </div>
            </div>

            <section className="compare-main">
                {mode === "alternatives" && (
                    <div className="compare-section">

                        <h3>Find Alternatives</h3>
                        <p>Drag a service from the saved into the box below, then run Find Alternatives.</p>
                        <div className="compare-dropbox" onDragOver={allowDrop} onDrop={onDrop}>
                            {selected.alternatives.length === 0 && <p>Drop a service here</p>}
                            {selected.alternatives.map(function(c){
                                const id = c.id;
                                return(
                                    <CompareCard card={c} key={id} draggable={false}>
                                        <button className="compare-btn remove" onClick={function() 
                                            { deleteItem(id); }}>
                                                Remove
                                        </button>
                                    </CompareCard>
                                )
                            })}
                    </div>
                        <button className="compare-run" onClick={handleFindAlternatives}>Find Alternatives</button>
                        {status.loading && <p>Loading alternatives...</p>}
                        {!status.loading && altResults.length > 0 && (
                            <div className="compare-panel" style={{marginTop: 13}}>
                                <h4>Alternatives</h4>
                                <div className="compare-card-list">
                                    {altResults.map(function(card){
                                        const id = card.id;
                                        return(
                                            <CompareCard card={card} key={id} draggable={false}>
                                                <button className ="compare-btn add" onClick={function()
                                                    { saveOffer(id); }}>
                                                        Save
                                                </button>
                                            </CompareCard>
                                        );
                                    })}
                                </div>
                            </div>
                        )}
                        {!status.loading && altResults.length === 0 && <p>No alternatives yet</p>}
                    </div>
                )}

                {mode === "direct" && (
                    <div className="compare-section">
                        <h3>Direct Comparison</h3>
                        <p>Drag 2-6 services to compare themside by side.</p>
                        <div className="compare-dropbox" onDragOver={allowDrop} onDrop={onDrop}>
                            {selected.direct.length === 0 && <p >Drop services here</p>}
                            {
                                selected.direct.map(function(card) {
                                    const id = card.id;
                                    return(
                                        <CompareCard key={id} card={card} draggable={false}>
                                            <button className="compare-btn remove" onClick={function() {deleteItem(id);}}>Remove</button>
                                        </CompareCard>
                                    );
                                })
                            }
                        </div>
                        {selected.direct.length > 1 && (
                            <div className="compare-panel" style={{marginTop: 13}}>
                                <table className="compare-table">
                                    <thead>
                                        <tr>
                                            <th>Field</th>
                                            {selected.direct.map(function(c) { return <th key={c.id}>{c.title}</th>; })}
                                        </tr>
                                    </thead>
                                    <tbody>
                                        <tr><td>Provider</td>{selected.direct.map(function(c) { return <td key={c.id}>{c.provider || '-'}</td>; })}</tr>
                                        <tr><td>Region</td>{selected.direct.map(function(c) { return <td key={c.id}>{c.region || '-'}</td>; })}</tr>
                                        <tr>
                                            <td>Price</td>
                                            {selected.direct.map(function(c, i) {
                                                const value =prices[i];

                                                const best = value != null && value === minPrice;
                                                const worst = value != null && value === maxPrice;
                                                let classification = best ? 'price-best' :worst ? 'price-worst' : '';
                                                return <td key={c.id} className={classification}>{formatPrice(c.id)}</td>;
                                            })}
                                        </tr>
                                        <tr>
                                            <td>Unit</td>
                                            {selected.direct.map(function(ccount) 
                                            { return <td key={ccount.id}>{priceList[ccount.id]?.unit || '-'}</td>;
                                             })}
                                        </tr>
                                        <tr><td>Service</td>{selected.direct.map(function(c) { return <td key={c.id}>{c.serviceId || '-'}</td>; })}</tr>
                                    </tbody>
                                </table>
                                <p className="compare-hint">Green; lowest price · Red; highest price</p>
                            </div>
                        )}
                    </div>
                )}

                {mode === 'compatibility' && (
                    <div className="compare-section">
                        <h3>Compatibility Checker - (AI)</h3>
                        <p>Drag two services into the box, and then run the checker.</p>
                        <div className="compare-dropbox" onDragOver={allowDrop} onDrop={onDrop}>
                            {selected.compatibility.length === 0 && <p >Drop services here</p>}
                            {selected.compatibility.map(function(card) {
                                const id = card.id;
                                return(
                                    <CompareCard key={id} card={card} draggable={false}>
                                        <button className="compare-btn remove" onClick={function(){deleteItem(id);}}>Remove</button>
                                    </CompareCard>
                                );
                            })}
                        </div>
                        <button className="compare-run" onClick={handleRunCompatibility} disabled={status.loading}>
                            {status.loading ? "Running.." : "Run AI Checker"}
                        </button>
                        {status.error && <p className="compare-error">{status.text}</p>}
                        {status.result && (
                            <div className="compare-ai-result">
                                <h4>Compatibility Report</h4>
                                <div className="ai-scores">
                                    <div className="compare-ai-num-itm">
                                        <span className= "score-label">SBERT Score</span>
                                        <span className="score-value">{status.result.sbert}</span>
                                    </div>
                                    <div className="compare-ai-num-itm">
                                        <span className= "score-label">Similarity</span>
                                        <span className="score-value">{status.result.sbertPercentage}%</span>
                                    </div>
                                </div>
                                <div className="compare-ai-report">{status.result.aiReport || 'No report available.'}</div>
                            </div>
                        )}
                    </div>
                )}

            </section>
        </div>
    );

}