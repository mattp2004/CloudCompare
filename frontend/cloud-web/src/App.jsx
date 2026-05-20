import { useState } from 'react'
import './App.css'
import { clearToken, getToken } from './lib/apiClient'
import { Navigate, Route, Routes } from 'react-router-dom';

import Home from './pages/Home';
import Catalogue from './pages/Catalogue';
import Compare from './pages/Compare';
import Dashboard from './pages/Dashboard';
import Map from './pages/Map';
import Admin from './pages/Admin';
import Login from './pages/Login';

import NavBar from './components/Navbar.jsx';


function PrivateRoute ({ children }) {
  const token = getToken();
  if(!token) return <Navigate to="/login" />;
  return children;
}

function App() {
  const[loggedIn, setLoggedIn] = useState(!!getToken());

  function updateLoginStatus(status){
    if(!status) clearToken();
    setLoggedIn(status);
  }

    return(
        <div>
          {loggedIn && <NavBar onLogout={() => updateLoginStatus(false)} />}
          <div className="page-layout">  
            <Routes>
                <Route path="/login" element={<Login onLogin={updateLoginStatus}/>} />
                <Route path="/" element={
                  <PrivateRoute> <Home /></PrivateRoute>
                  }/>
                <Route path="/catalogue" element={
                  <PrivateRoute> <Catalogue /></PrivateRoute>
                  }/>
                <Route path="/compare" element={
                  <PrivateRoute> <Compare /></PrivateRoute>
                  }/>
                <Route path="/dashboard" element={  
                  <PrivateRoute> <Dashboard /></PrivateRoute>
                  }/>
                <Route path="/map" element={
                  <PrivateRoute> <Map /></PrivateRoute>
                  }/>
                <Route path="/admin" element={
                  <PrivateRoute> <Admin /></PrivateRoute>
                  }/>
                <Route path="*" element={<Navigate to="/" />}/>
              </Routes>
          </div>
        </div>
    )
}
export default App
