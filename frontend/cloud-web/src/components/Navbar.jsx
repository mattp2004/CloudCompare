import { Container, Nav, Navbar, NavDropdown } from "react-bootstrap";
import { Link, NavLink } from "react-router-dom";
import "./Navbar.css";
export default function NavBar({ onLogout }) {
    return (
        <Navbar className="navbar-custom" expand="md">
            <Container fluid>
                <Navbar.Brand as={Link} to="/">
                    <span className="brand-logo">☁️</span> Cloud Compare</Navbar.Brand>

                <Navbar.Toggle aria-controls="basic-navbar-nav" />
                <Navbar.Collapse id="basic-navbar-nav">
                    <Nav>
                        <Nav.Link as={NavLink} to='/' end>Home</Nav.Link>
                        <Nav.Link as={NavLink} to="/catalogue">Catalogue</Nav.Link>
                        <Nav.Link as={NavLink} to="/compare">Compare</Nav.Link>
                        <Nav.Link as={NavLink} to="/map">Map</Nav.Link>
                        <Nav.Link as={NavLink} to="/dashboard">Dashboard</Nav.Link>
                    </Nav>
                    
                    <Nav className="ms-auto">
                        <NavDropdown title="Account" id="account-dropdown" className="navbar-dropdown">
                            <NavDropdown.Item as={NavLink} to="/admin">Admin</NavDropdown.Item>
                            <NavDropdown.Divider />
                            <NavDropdown.Item onClick={onLogout} className="logout-item">Logout</NavDropdown.Item>
                        </NavDropdown>
                    </Nav>
                </Navbar.Collapse>
            </Container>
        </Navbar>
    );
}