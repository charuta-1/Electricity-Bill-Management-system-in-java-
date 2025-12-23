import { useEffect, useState } from 'react';
import { NavLink, useNavigate } from 'react-router-dom';
import { Button } from 'react-bootstrap';
import {
  BsBoxArrowRight,
  BsGridFill,
  BsPeopleFill,
  BsReceipt,
  BsGraphUp,
  BsLightningFill,
  BsTagFill,
  BsChevronDoubleLeft,
  BsChevronDoubleRight
} from 'react-icons/bs';
import { FaFileInvoiceDollar } from 'react-icons/fa';
import logo from '../assets/logo-vit-energysuite.png';
import { useAuth } from '../context/AuthContext.js';

const navLinks = {
  ADMIN: [
    { to: '/admin/dashboard', label: 'Dashboard', icon: <BsGridFill /> },
    { to: '/admin/users', label: 'Admins', icon: <BsPeopleFill /> },
    { to: '/admin/customers', label: 'Customers', icon: <BsPeopleFill /> },
    { to: '/admin/accounts', label: 'Accounts', icon: <BsReceipt /> },
    { to: '/admin/readings', label: 'Meter Readings', icon: <BsLightningFill /> },
    { to: '/admin/bills', label: 'Bills', icon: <FaFileInvoiceDollar /> },
    { to: '/admin/complaints', label: 'Complaints', icon: <BsGraphUp /> },
    { to: '/admin/tariffs', label: 'Tariffs', icon: <BsTagFill /> }
  ],
  CUSTOMER: [
    { to: '/customer/dashboard', label: 'Dashboard', icon: <BsGridFill /> },
    { to: '/customer/bills', label: 'My Bills', icon: <FaFileInvoiceDollar /> },
    { to: '/customer/pay', label: 'Pay Bill', icon: <BsReceipt /> },
    { to: '/customer/advance-payment', label: 'Advance Payment', icon: <BsLightningFill /> },
    { to: '/customer/usage', label: 'Usage', icon: <BsGraphUp /> },
    { to: '/customer/complaints', label: 'Complaints', icon: <BsPeopleFill /> }
  ]
};

const Navbar = () => {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [collapsed, setCollapsed] = useState(false);
  const [logoFailed, setLogoFailed] = useState(false);

  useEffect(() => {
    const evaluateCollapse = () => {
      setCollapsed(window.innerWidth < 992);
    };

    evaluateCollapse();
    window.addEventListener('resize', evaluateCollapse);
    return () => window.removeEventListener('resize', evaluateCollapse);
  }, []);

  useEffect(() => {
    const shell = document.querySelector('.app-shell');
    if (!shell) {
      return undefined;
    }
    if (collapsed) {
      shell.classList.add('sidebar-collapsed');
    } else {
      shell.classList.remove('sidebar-collapsed');
    }

    return () => {
      shell.classList.remove('sidebar-collapsed');
    };
  }, [collapsed]);

  if (!user) {
    return null;
  }

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const links = navLinks[user.role] || [];

  return (
    <aside className={`sidebar${collapsed ? ' collapsed' : ''}`}>
      <div className="sidebar-header">
        <button
          type="button"
          className="sidebar-toggle"
          aria-label={collapsed ? 'Expand navigation' : 'Collapse navigation'}
          onClick={() => setCollapsed((prev) => !prev)}
        >
          {collapsed ? <BsChevronDoubleRight /> : <BsChevronDoubleLeft />}
        </button>
        <div className="sidebar-brand text-center">
          {!logoFailed ? (
            <img
              src={logo}
              alt="VIT EnergySuite Logo"
              className="sidebar-brand-logo"
              onError={() => setLogoFailed(true)}
            />
          ) : (
            <div className="sidebar-brand-placeholder">VIT EnergySuite</div>
          )}
          {!collapsed && <span className="sidebar-brand-text">EnergySuite Portal</span>}
        </div>
      </div>
      <nav className="sidebar-nav">
        {links.map((link) => (
          <NavLink
            key={link.to}
            to={link.to}
            className={({ isActive }) => (isActive ? 'sidebar-link active' : 'sidebar-link')}
          >
            <span className="sidebar-link-icon">{link.icon}</span>
            <span className="sidebar-link-label">{link.label}</span>
          </NavLink>
        ))}
      </nav>
      <div className="sidebar-footer">
        {!collapsed && (
          <>
            <div className="user-name">{user.fullName}</div>
            <div className="user-role">{user.role}</div>
          </>
        )}
        <Button variant="outline-light" className="logout-button" onClick={handleLogout}>
          <BsBoxArrowRight />
          {!collapsed && <span>Logout</span>}
        </Button>
      </div>
    </aside>
  );
};

export default Navbar;
