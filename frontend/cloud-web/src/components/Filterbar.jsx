import { REGIONS, UNITS } from '../components/constants.js';
import './FilterBar.css';

export default function FilterBar({
    searchValue = '',
    provider = '',
    region = '',
    unit = '',
    offerType = '',
    orderBy = '',
    onSearchChange,
    onProviderChange,
    onRegionChange,
    onUnitChange,
    onOfferTypeChange,
    onOrderByChange,
    onSearch,
}) {
    return (
        <div className="filter-bar">
            <input
                className="filter-search"
                type="text"
                placeholder="Search offers"
                value={searchValue}
                onChange={function(e) { if (onSearchChange) onSearchChange(e.target.value); }}
                onKeyDown={function(e) { if (e.key === 'Enter' && onSearch) onSearch(); }}
            />

            <select className="filter-select" value={provider}
                onChange={function(e) { if (onProviderChange) onProviderChange(e.target.value); }}>
                <option value="">Provider</option>
                <option value="AWS">AWS</option>
                <option value="AZURE">AZURE</option>
                <option value="GCP">GCP</option>
            </select>

            <select className="filter-select" value={region}
                onChange={function(e) { if (onRegionChange) onRegionChange(e.target.value); }}>
                <option value="">Region</option>
                {REGIONS.map(function(r) { return <option key={r} value={r}>{r}</option>; })}
            </select>

            <select className="filter-select" value={unit}
                onChange={function(e) { if (onUnitChange) onUnitChange(e.target.value); }}>
                <option value="">Unit</option>
                {UNITS.map(function(u) { return <option key={u} value={u}>{u}</option>; })}
            </select>

            <select className="filter-select" value={offerType}
                onChange={function(e) { if (onOfferTypeChange) onOfferTypeChange(e.target.value); }}>
                <option value="">Type</option>
                <option value="ON_DEMAND">ON_DEMAND</option>
                <option value="RESERVED">RESERVED</option>
                <option value="DATA_TRANSFER">DATA_TRANSFER</option>
            </select>

            <div className="filter-sort">
                <span className="filter-sort-label">Sort</span>
                <button type="button"
                    className={orderBy === '' ? 'filter-sort-btn active' : 'filter-sort-btn'}
                    onClick={function() { if (onOrderByChange) onOrderByChange(''); }}>
                    Default
                </button>
                <button type="button"
                    className={orderBy === 'price' ? 'filter-sort-btn active' : 'filter-sort-btn'}
                    onClick={function() { if (onOrderByChange) onOrderByChange('price'); }}>
                    Price
                </button>
                <button type="button"
                    className={orderBy === 'region' ? 'filter-sort-btn active' : 'filter-sort-btn'}
                    onClick={function() { if (onOrderByChange) onOrderByChange('region'); }}>
                    Region
                </button>
                <button type="button"
                    className={orderBy === 'provider' ? 'filter-sort-btn active' : 'filter-sort-btn'}
                    onClick={function() { if (onOrderByChange) onOrderByChange('provider'); }}>
                    Provider
                </button>
            </div>

            <button className="filter-search-btn"
                onClick={function() { if (onSearch) onSearch(); }}>
                Search
            </button>
        </div>
    );
}