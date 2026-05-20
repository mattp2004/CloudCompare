import {useState} from 'react';    
import{login,setToken} from '../lib/apiClient';
import './Login.css';
import { useNavigate } from 'react-router-dom';

export default function Login({onLogin}) {

    const[username, setUsername] = useState('');
    const[password, setPassword] = useState('');
    
    const[error, setError] = useState('');
    const[status, setStatus] = useState('');

    const navigate = useNavigate();

    const handleSubmission = async (e) => {
        e.preventDefault();
        setError('');
        setStatus('Attempting')

        login(username, password).then((data) => {
            setToken(data.token)
            setStatus("Success")

            onLogin(true);
            navigate('/');
        }).catch((e) => {
            setStatus("Failed")
            setError(e.message);
        });
    }


    return(
        <div className="login-main">
            <form className="login-form" onSubmit={async (e) => {handleSubmission(e)}}>
                <h2>Cloud Compare</h2>
                <p>Login to your account</p>
                <label>Username</label>
                <input 
                    type="text"
                    value={username}
                    placeholder="username"
                    onChange={(e) => setUsername(e.target.value)}
                />
                <label>Password</label>
                <input 
                    type="password"
                    value={password}
                    placeholder="password"
                    onChange={(e) =>setPassword(e.target.value)}
                />
                {status && <p className="status">{status}</p>}
                {error && <p className="error">{error}</p>}
                <button type="submit">Login</button>
            </form>
        </div>
    );


}