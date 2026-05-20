import { Link } from 'react-router-dom';
import './Home.css';
import homeBg from '../assets/home-bg.png';

export default function Home(){
    return (
        <div>
            <div className="main-section" style={{ backgroundImage: `url(${homeBg})` }}>
                <div className="main-content">
                    <h1>Compare Cloud Services and Migration Planner</h1>
                    <p>Search, map, and compare AWS, Azure, and GCP services by region and price.</p>
                    <div className="main-buttons">
                        <Link className="button-browse" to="/catalogue">🔎 Browse Catalogue</Link>
                        <Link className="button-compare" to="/compare">📊 Compare Services</Link>
                        <Link className="button-saved" to="/dashboard">💾 My Saved Services</Link>
                    </div>
                </div>
            </div>
            <div className="home-divider" />

            <section className="home-info">
                <div className="home-info-inner">
                    <div className="home-info-header">
                        <h2>Cloud Service Comparison</h2>
                        <p>Use our tools to find, compare, save and analyse your cloud services!</p>
                    </div>
                    <div className="home-info-1">
                        <div className="home-cards">
                            <div className="home-info-icon">📌</div>
                            <h3>1. Find a service</h3>
                            <p>Browse in the catalogue and save your services.</p>
                        </div>
                        <div className="home-cards">
                            <div className="home-info-icon">📊</div>
                            <h3>2. Compare side by side</h3>
                            <p>Use comparison tools to find suitable alternatives.</p>
                        </div>
                        <div className="home-cards">
                            <div className="home-info-icon">🗂️</div>
                            <h3>3. Save your services</h3>
                            <p>Save offers and view them in your dashboard for analysis.</p>
                        </div>
                    </div>
                </div>
            </section>
        </div>
    );
}