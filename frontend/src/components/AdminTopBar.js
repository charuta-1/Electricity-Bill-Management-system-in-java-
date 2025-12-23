import { useEffect, useMemo, useRef, useState } from 'react';
import PropTypes from 'prop-types';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext.js';
import logo from '../assets/logo-vit-energysuite.png';

const AdminTopBar = ({ title, subtitle, extra }) => {
  const navigate = useNavigate();
  const { user, logout } = useAuth();
  const [menuOpen, setMenuOpen] = useState(false);
  const menuRef = useRef(null);

  useEffect(() => {
    if (!menuOpen) {
      return;
    }

    const handleClickOutside = (event) => {
      if (menuRef.current && !menuRef.current.contains(event.target)) {
        setMenuOpen(false);
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, [menuOpen]);

  const initials = useMemo(() => {
    if (!user?.fullName) {
      return 'AD';
    }
    return user.fullName
      .split(' ')
      .filter(Boolean)
      .slice(0, 2)
      .map((segment) => segment.charAt(0).toUpperCase())
      .join('') || 'AD';
  }, [user]);

  if (!user) {
    return null;
  }

  const handleLogout = () => {
    setMenuOpen(false);
    logout();
    navigate('/login');
  };

  const toggleMenu = () => {
    setMenuOpen((prev) => !prev);
  };

  return (
    <header className="d-flex flex-wrap align-items-center gap-3 mb-4 topbar">
      <div className="topbar-brand">
        <img src={logo} alt="VIT EnergySuite" />
        <div className="brand-text">
          <span className="brand-title">VIT EnergySuite</span>
          <span className="brand-subtitle">Billing Portal</span>
        </div>
      </div>
      <div className="topbar-page flex-grow-1">
        <h1 className="h4 mb-1">{title}</h1>
        {subtitle && <p className="text-muted mb-0">{subtitle}</p>}
      </div>
      <div className="topbar-actions ms-auto">
        {extra && <div className="d-flex align-items-center">{extra}</div>}
        <div className="position-relative" ref={menuRef}>
          <button
            type="button"
            className="btn btn-light border d-flex align-items-center gap-2 shadow-sm"
            onClick={toggleMenu}
          >
            <span
              className="rounded-circle bg-primary text-white fw-semibold d-inline-flex align-items-center justify-content-center"
              style={{ width: 36, height: 36 }}
            >
              {initials}
            </span>
            <span className="text-start d-none d-sm-flex flex-column">
              <div className="fw-semibold text-primary-emphasis">{user.fullName}</div>
              <small className="text-muted text-uppercase">{user.role}</small>
            </span>
            <span className="text-muted">▾</span>
          </button>
          {menuOpen && (
            <div className="dropdown-menu dropdown-menu-end show shadow-sm border-0 mt-2" style={{ minWidth: '14rem' }}>
              <div className="px-3 py-2 text-muted small">
                Signed in as
                <div className="fw-semibold text-body">{user.username || user.fullName}</div>
              </div>
              <div className="dropdown-divider" />
              <button type="button" className="dropdown-item text-danger" onClick={handleLogout}>
                Logout
              </button>
            </div>
          )}
        </div>
      </div>
    </header>
  );
};

AdminTopBar.propTypes = {
  title: PropTypes.string.isRequired,
  subtitle: PropTypes.string,
  extra: PropTypes.node
};

AdminTopBar.defaultProps = {
  subtitle: '',
  extra: null
};

export default AdminTopBar;
