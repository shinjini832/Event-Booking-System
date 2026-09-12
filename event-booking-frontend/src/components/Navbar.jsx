import React from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

const Navbar = () => {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <nav className="navbar">
      <div className="nav-brand">
        <Link to="/">
          <span className="brand-icon">🎟️</span>
          <span className="brand-text">EventPass</span>
        </Link>
      </div>

      <div className="nav-links">
        <Link to="/" className="nav-link">Explore Events</Link>
        {user && (
          <Link to="/my-bookings" className="nav-link">My Tickets</Link>
        )}
      </div>

      <div className="nav-auth">
        {user ? (
          <div className="user-profile">
            <span className="user-email">{user.email}</span>
            <span className={`role-badge ${user.role?.toLowerCase()}`}>{user.role}</span>
            <button className="btn btn-outline" onClick={handleLogout}>Logout</button>
          </div>
        ) : (
          <div className="auth-buttons">
            <Link to="/login" className="btn btn-outline">Log In</Link>
            <Link to="/register" className="btn btn-primary">Sign Up</Link>
          </div>
        )}
      </div>
    </nav>
  );
};

export default Navbar;
