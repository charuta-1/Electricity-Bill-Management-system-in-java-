import { useEffect, useMemo, useState } from 'react';
import { Alert, Badge, Button, ButtonGroup, Card, CloseButton, Col, Form, Row, Table } from 'react-bootstrap';
import api from '../../api/axiosConfig.js';
import { useAuth } from '../../context/AuthContext.js';
import AdminTopBar from '../../components/AdminTopBar.js';

const defaultForm = {
  fullName: '',
  username: '',
  email: '',
  phoneNumber: '',
  password: '',
  confirmPassword: ''
};

const ManageAdmins = () => {
  const { user } = useAuth();
  const [admins, setAdmins] = useState([]);
  const [form, setForm] = useState(defaultForm);
  const [submitting, setSubmitting] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [showCreateForm, setShowCreateForm] = useState(false);

  useEffect(() => {
    fetchAdmins();
  }, []);

  const activeAdminsCount = useMemo(
    () => admins.filter((admin) => admin.active).length,
    [admins]
  );

  const fetchAdmins = async () => {
    try {
      setLoading(true);
      const { data } = await api.get('/admin/users');
      setAdmins(data);
    } catch (err) {
      console.error('Failed to fetch admins', err);
      setError('Unable to load administrators');
    } finally {
      setLoading(false);
    }
  };

  const handleChange = ({ target }) => {
    const { name, value } = target;
    setError('');
    setSuccess('');
    setForm((prev) => ({ ...prev, [name]: value }));
  };

  const toggleCreateForm = () => {
    setError('');
    setSuccess('');
    setShowCreateForm((prev) => !prev);
  };

  const handleCreate = async (event) => {
    event.preventDefault();
    setError('');
    setSuccess('');

    if (form.password !== form.confirmPassword) {
      setError('Passwords do not match');
      return;
    }

    setSubmitting(true);
    try {
      const payload = {
        fullName: form.fullName,
        username: form.username,
        email: form.email,
        phoneNumber: form.phoneNumber,
        password: form.password
      };

      const { data } = await api.post('/admin/users', payload);
      setAdmins((prev) => [data, ...prev]);
      setForm(defaultForm);
      setSuccess('Administrator account created successfully');
      setShowCreateForm(false);
    } catch (err) {
      console.error('Failed to create admin', err);
      const message = err.response?.data || 'Unable to create administrator';
      setError(typeof message === 'string' ? message : 'Unable to create administrator');
    } finally {
      setSubmitting(false);
    }
  };

  const handleToggle = async (adminId, active) => {
    setError('');
    setSuccess('');

    if (!active && user?.userId === adminId) {
      setError('You cannot deactivate your own administrator account');
      return;
    }

    try {
      const { data } = await api.patch(`/admin/users/${adminId}/status`, {}, {
        params: { active }
      });
      setAdmins((prev) => prev.map((admin) => (admin.userId === adminId ? data : admin)));
      setSuccess(`Administrator ${active ? 'activated' : 'deactivated'} successfully`);
    } catch (err) {
      console.error('Failed to update admin status', err);
      const message = err.response?.data || 'Unable to update administrator status';
      setError(typeof message === 'string' ? message : 'Unable to update administrator status');
    }
  };

  return (
    <div className="container py-4">
      <AdminTopBar
        title="Administrator Management"
        subtitle="Create new admin accounts and manage their access."
        extra={(
          <div className="d-flex align-items-center gap-2">
            <Badge bg="primary" className="bg-primary-subtle text-primary-emphasis">
              Active admins: {activeAdminsCount}
            </Badge>
            <Button variant="primary" size="sm" onClick={toggleCreateForm}>
              {showCreateForm ? 'Close form' : 'New admin'}
            </Button>
          </div>
        )}
      />

      {!showCreateForm && success && (
        <Alert variant="success">{success}</Alert>
      )}
      {!showCreateForm && error && (
        <Alert variant="danger">{error}</Alert>
      )}

      <div className="row g-4">
        {showCreateForm && (
          <div className="col-lg-5">
            <Card className="border-0 shadow-sm h-100">
              <Card.Body className="p-4">
                <div className="d-flex justify-content-between align-items-start mb-3">
                  <div>
                    <h2 className="h5 mb-1">Create new admin</h2>
                    <p className="text-muted small mb-0">
                      Admins get full access to billing configuration, customer management, and reports.
                    </p>
                  </div>
                  <CloseButton onClick={toggleCreateForm} aria-label="Close" />
                </div>

                {error && <Alert variant="danger">{error}</Alert>}
                {success && <Alert variant="success">{success}</Alert>}

                <Form onSubmit={handleCreate} noValidate>
                  <Form.Group className="mb-3" controlId="adminFullName">
                    <Form.Label>Full name</Form.Label>
                    <Form.Control
                      type="text"
                      name="fullName"
                      value={form.fullName}
                      onChange={handleChange}
                      required
                    />
                  </Form.Group>
                  <Form.Group className="mb-3" controlId="adminUsername">
                    <Form.Label>Username</Form.Label>
                    <Form.Control
                      type="text"
                      name="username"
                      value={form.username}
                      onChange={handleChange}
                      required
                    />
                  </Form.Group>
                  <Form.Group className="mb-3" controlId="adminEmail">
                    <Form.Label>Email</Form.Label>
                    <Form.Control
                      type="email"
                      name="email"
                      value={form.email}
                      onChange={handleChange}
                      required
                    />
                  </Form.Group>
                  <Form.Group className="mb-3" controlId="adminPhone">
                    <Form.Label>Phone number</Form.Label>
                    <Form.Control
                      type="tel"
                      name="phoneNumber"
                      value={form.phoneNumber}
                      onChange={handleChange}
                    />
                  </Form.Group>
                  <Row className="g-3">
                    <Form.Group as={Col} sm={6} controlId="adminPassword">
                      <Form.Label>Password</Form.Label>
                      <Form.Control
                        type="password"
                        name="password"
                        value={form.password}
                        onChange={handleChange}
                        required
                      />
                    </Form.Group>
                    <Form.Group as={Col} sm={6} controlId="adminConfirmPassword">
                      <Form.Label>Confirm password</Form.Label>
                      <Form.Control
                        type="password"
                        name="confirmPassword"
                        value={form.confirmPassword}
                        onChange={handleChange}
                        required
                      />
                    </Form.Group>
                  </Row>

                  <Button className="mt-4 w-100" variant="primary" type="submit" disabled={submitting}>
                    {submitting ? 'Creating admin...' : 'Create admin'}
                  </Button>
                </Form>
              </Card.Body>
            </Card>
          </div>
        )}

        <div className={showCreateForm ? 'col-lg-7' : 'col-12'}>
          <Card className="border-0 shadow-sm h-100">
            <Card.Body className="p-0">
              <Table responsive hover borderless className="align-middle mb-0">
                <thead className="table-light">
                  <tr>
                    <th>Name</th>
                    <th>Username</th>
                    <th>Email</th>
                    <th>Status</th>
                    <th className="text-end">Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {loading ? (
                    <tr>
                      <td colSpan={5} className="text-center py-5">
                        Loading administrators...
                      </td>
                    </tr>
                  ) : admins.length === 0 ? (
                    <tr>
                      <td colSpan={5} className="text-center py-5 text-muted">
                        No administrators found yet.
                      </td>
                    </tr>
                  ) : (
                    admins.map((admin) => (
                      <tr key={admin.userId}>
                        <td>
                          <div className="fw-semibold">{admin.fullName}</div>
                          <div className="text-muted small">
                            {admin.createdAt ? `Created ${new Date(admin.createdAt).toLocaleDateString()}` : 'Creation date unavailable'}
                          </div>
                        </td>
                        <td>{admin.username}</td>
                        <td>{admin.email}</td>
                        <td>
                          <Badge
                            pill
                            bg={admin.active ? 'success' : 'secondary'}
                            className={admin.active ? 'bg-success-subtle text-success-emphasis' : 'bg-secondary-subtle text-secondary-emphasis'}
                          >
                            {admin.active ? 'Active' : 'Inactive'}
                          </Badge>
                        </td>
                        <td className="text-end">
                          <ButtonGroup size="sm">
                            <Button
                              variant="outline-secondary"
                              disabled={!admin.active || activeAdminsCount <= 1 || admin.userId === user?.userId}
                              onClick={() => handleToggle(admin.userId, false)}
                            >
                              Deactivate
                            </Button>
                            <Button
                              variant="outline-primary"
                              disabled={admin.active}
                              onClick={() => handleToggle(admin.userId, true)}
                            >
                              Activate
                            </Button>
                          </ButtonGroup>
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
              </Table>
            </Card.Body>
          </Card>
        </div>
      </div>
    </div>
  );
};

export default ManageAdmins;
