import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Button, Card, Col, Form, Row } from 'react-bootstrap';
import { useAuth } from '../context/AuthContext.js';
import logo from '../assets/logo-vit-energysuite.png';

const Login = () => {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();
  const { login, user } = useAuth();

  useEffect(() => {
    if (user) {
      const landing = user.role === 'ADMIN' ? '/admin/dashboard' : '/customer/dashboard';
      navigate(landing, { replace: true });
    }
  }, [user, navigate]);

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError('');
    setLoading(true);
    const result = await login(username, password);

    if (result.success) {
      const stored = localStorage.getItem('user');
      const sessionUser = stored ? JSON.parse(stored) : null;
      if (sessionUser?.role === 'ADMIN') {
        navigate('/admin/dashboard', { replace: true });
      } else if (sessionUser?.role === 'CUSTOMER') {
        navigate('/customer/dashboard', { replace: true });
      } else {
        navigate('/unauthorized', { replace: true });
      }
    } else {
      setError(result.message);
    }

    setLoading(false);
  };

  return (
    <div className="auth-wrapper">
      <Row className="w-100 align-items-stretch" style={{ maxWidth: '1100px' }}>
        <Col md={6} className="mb-4 mb-md-0">
          <div className="auth-hero">
            <img src={logo} alt="VIT EnergySuite" style={{ width: '160px', margin: '0 auto' }} />
            <h1>Welcome back</h1>
            <small>Secure access to the VIT EnergySuite billing portal.</small>
          </div>
        </Col>
        <Col md={6}>
          <Card className="auth-card border-0">
            <div className="text-center mb-4">
              <img src={logo} alt="VIT EnergySuite Logo" style={{ width: '180px' }} />
            </div>
            <h3 className="text-center mb-2">Admin & Customer Login</h3>
            <p className="text-center text-muted mb-4">Sign in with your VIT EnergySuite credentials.</p>

            {error && <div className="alert alert-danger">{error}</div>}

            <Form onSubmit={handleSubmit} className="d-flex flex-column gap-3">
              <Form.Group controlId="username">
                <Form.Label>Username</Form.Label>
                <Form.Control
                  type="text"
                  placeholder="Enter username"
                  value={username}
                  onChange={(event) => setUsername(event.target.value)}
                  required
                />
              </Form.Group>

              <Form.Group controlId="password">
                <Form.Label>Password</Form.Label>
                <Form.Control
                  type="password"
                  placeholder="Enter password"
                  value={password}
                  onChange={(event) => setPassword(event.target.value)}
                  required
                />
              </Form.Group>

              <Button type="submit" variant="primary" className="w-100" size="lg" disabled={loading}>
                {loading ? 'Signing in…' : 'Login'}
              </Button>
            </Form>

            <div className="text-center text-muted mt-4">
              Don&apos;t have an account? <Link to="/signup">Sign up</Link>
            </div>
          </Card>
        </Col>
      </Row>
    </div>
  );
};

export default Login;
