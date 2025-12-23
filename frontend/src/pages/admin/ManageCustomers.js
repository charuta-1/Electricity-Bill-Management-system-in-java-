import { useEffect, useMemo, useState, useCallback } from 'react';
import { Alert, Button, Card, Col, Form, Modal, Row, Table } from 'react-bootstrap';
import { FaPlus, FaRegEdit, FaTrashAlt, FaUserShield } from 'react-icons/fa';
import api from '../../api/axiosConfig.js';
import AdminTopBar from '../../components/AdminTopBar.js';

const emptyForm = {
  username: '',
  fullName: '',
  email: '',
  phoneNumber: '',
  address: '',
  city: '',
  state: 'Maharashtra',
  pincode: '',
  aadharNumber: '',
  password: '',
  confirmPassword: ''
};

const PASSWORD_RULE = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&#])[A-Za-z\d@$!%*?&#]{8,}$/;

const ManageCustomers = () => {
  const [customers, setCustomers] = useState([]);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [loading, setLoading] = useState(true);
  const [formData, setFormData] = useState({ ...emptyForm });
  const [submitting, setSubmitting] = useState(false);
  const [showForm, setShowForm] = useState(false);
  const [editingCustomerId, setEditingCustomerId] = useState(null);
  const [deletingId, setDeletingId] = useState(null);

  const resolveErrorMessage = (err, fallback) => {
    const data = err?.response?.data;
    if (typeof data === 'string') {
      return data;
    }
    return data?.message || fallback;
  };

  const fetchCustomers = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const response = await api.get('/admin/customers');
      setCustomers(response.data || []);
    } catch (err) {
      setError(resolveErrorMessage(err, 'Failed to load customers'));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchCustomers();
  }, [fetchCustomers]);

  const handleOpenForm = (customer = null) => {
    setError('');
    setSuccess('');
    if (customer) {
      setFormData({
        username: customer.username || '',
        fullName: customer.fullName || '',
        email: customer.email || '',
        phoneNumber: customer.phoneNumber || '',
        address: customer.address || '',
        city: customer.city || '',
        state: customer.state || 'Maharashtra',
        pincode: customer.pincode || '',
        aadharNumber: customer.aadharNumber || '',
        password: '',
        confirmPassword: ''
      });
      setEditingCustomerId(customer.customerId);
    } else {
      setFormData({ ...emptyForm });
      setEditingCustomerId(null);
    }
    setShowForm(true);
  };

  const handleCloseForm = () => {
    if (submitting) {
      return;
    }
    setShowForm(false);
    setFormData({ ...emptyForm });
    setEditingCustomerId(null);
  };

  const handleChange = (event) => {
    const { name, value } = event.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
  };

  const normalizedDetails = useMemo(() => ({
    fullName: formData.fullName.trim(),
    email: formData.email.trim(),
    phoneNumber: formData.phoneNumber.trim(),
    address: formData.address.trim(),
    city: formData.city.trim(),
    state: formData.state.trim() || 'Maharashtra',
    pincode: formData.pincode.trim(),
    aadharNumber: formData.aadharNumber.trim() || null
  }), [formData]);

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError('');
    setSuccess('');
    setSubmitting(true);

    try {
      const requiredFields = [
        { value: normalizedDetails.fullName, message: 'Full name is required' },
        { value: normalizedDetails.phoneNumber, message: 'Phone number is required' },
        { value: normalizedDetails.address, message: 'Address is required' },
        { value: normalizedDetails.city, message: 'City is required' },
        { value: normalizedDetails.pincode, message: 'Pincode is required' }
      ];

      if (!editingCustomerId) {
        requiredFields.push(
          { value: normalizedDetails.email, message: 'Email is required' }
        );
      }

      const missing = requiredFields.find((field) => !field.value);
      if (missing) {
        setError(missing.message);
        setSubmitting(false);
        return;
      }

      if (editingCustomerId) {
        await api.put(`/admin/customers/${editingCustomerId}`, normalizedDetails);
        setSuccess('Customer updated successfully.');
      } else {
        const username = formData.username.trim().toLowerCase();
        if (!username) {
          setError('Username is required');
          setSubmitting(false);
          return;
        }
        if (!formData.password) {
          setError('Password is required');
          setSubmitting(false);
          return;
        }
        if (formData.password !== formData.confirmPassword) {
          setError('Passwords do not match');
          setSubmitting(false);
          return;
        }
        if (!PASSWORD_RULE.test(formData.password)) {
          setError('Password must be at least 8 characters and include upper, lower, number, and special character.');
          setSubmitting(false);
          return;
        }

        const createPayload = {
          username,
          password: formData.password,
          ...normalizedDetails
        };

        await api.post('/admin/customers', createPayload);
        setSuccess('Customer created successfully.');
      }
      await fetchCustomers();
      handleCloseForm();
    } catch (err) {
      setError(resolveErrorMessage(err, 'Failed to save customer'));
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = async (customerId) => {
    if (!window.confirm('Are you sure you want to delete this customer? This cannot be undone.')) {
      return;
    }
    setDeletingId(customerId);
    setError('');
    setSuccess('');
    try {
      await api.delete(`/admin/customers/${customerId}`);
      setSuccess('Customer deleted successfully.');
      await fetchCustomers();
    } catch (err) {
      setError(resolveErrorMessage(err, 'Failed to delete customer'));
    } finally {
      setDeletingId(null);
    }
  };

  // Advance modal state and handlers
  const [advanceModalShow, setAdvanceModalShow] = useState(false);
  const [advanceAmount, setAdvanceAmount] = useState('');
  const [advanceTargetCustomer, setAdvanceTargetCustomer] = useState(null);
  const [advSubmitting, setAdvSubmitting] = useState(false);

  const handleOpenAdvanceModal = (customer) => {
    setError('');
    setSuccess('');
    setAdvanceAmount('');
    setAdvanceTargetCustomer(customer);
    setAdvanceModalShow(true);
  };

  const handleCloseAdvanceModal = () => {
    if (advSubmitting) return;
    setAdvanceModalShow(false);
    setAdvanceTargetCustomer(null);
    setAdvanceAmount('');
  };

  const handleAdvanceSubmit = async (e) => {
    e.preventDefault();
    if (!advanceTargetCustomer) return;
    const amount = parseFloat(advanceAmount);
    if (!amount || amount <= 0) {
      setError('Please enter a valid amount');
      return;
    }
    setAdvSubmitting(true);
    setError('');
    setSuccess('');
    try {
      await api.post(`/admin/customers/${advanceTargetCustomer.customerId}/advance-payment`, { amount });
      setSuccess(`Added ₹${amount.toFixed(2)} to ${advanceTargetCustomer.fullName}'s advance balance.`);
      setAdvanceModalShow(false);
      await fetchCustomers();
    } catch (err) {
      setError(resolveErrorMessage(err, 'Failed to add advance'));
    } finally {
      setAdvSubmitting(false);
    }
  };

  return (
    <div className="container py-4">
      <AdminTopBar
        title="Customers"
        subtitle="Manage registered consumers and their service accounts."
        extra={(
          <div className="d-flex gap-2">
            <Button variant="outline-secondary" onClick={() => window.open('/admin/users', '_self')}>
              <FaUserShield className="me-2" /> Manage Admins
            </Button>
            <Button variant="primary" onClick={() => handleOpenForm()}>
              <FaPlus className="me-2" /> New Customer
            </Button>
          </div>
        )}
      />

      {loading && <Alert variant="info">Loading customers...</Alert>}
      {error && !showForm && <Alert variant="danger">{error}</Alert>}
      {success && <Alert variant="success">{success}</Alert>}

      <Card className="card-shadow">
        <Card.Body className="p-0">
          <Table responsive hover borderless className="align-middle mb-0">
            <thead className="table-light">
              <tr>
                <th>Customer #</th>
                <th>Name</th>
                <th>Email</th>
                <th>Phone</th>
                <th>City</th>
                <th className="text-end">Actions</th>
              </tr>
            </thead>
            <tbody>
              {customers.map((customer) => (
                <tr key={customer.customerId}>
                  <td>{customer.customerNumber}</td>
                  <td>{customer.fullName}</td>
                  <td>{customer.email || '—'}</td>
                  <td>{customer.phoneNumber}</td>
                  <td>{customer.city || '—'}</td>
                  <td className="text-end">
                    <div className="d-inline-flex gap-2">
                      <Button
                        size="sm"
                        variant="outline-secondary"
                        onClick={() => handleOpenForm(customer)}
                      >
                        <FaRegEdit className="me-1" /> Edit
                      </Button>
                      <Button
                        size="sm"
                        variant="outline-success"
                        onClick={() => handleOpenAdvanceModal(customer)}
                      >
                        <FaPlus className="me-1" /> Advance
                      </Button>
                      <Button
                        size="sm"
                        variant="outline-danger"
                        onClick={() => handleDelete(customer.customerId)}
                        disabled={deletingId === customer.customerId}
                      >
                        {deletingId === customer.customerId ? 'Deleting…' : (<><FaTrashAlt className="me-1" /> Delete</>)}
                      </Button>
                    </div>
                  </td>
                </tr>
              ))}
              {customers.length === 0 && !loading && (
                <tr>
                  <td colSpan="6" className="text-center py-4 text-muted">
                    No customers found. Use the new customer button to add one.
                  </td>
                </tr>
              )}
            </tbody>
          </Table>
        </Card.Body>
      </Card>

        {/* Advance Payment Modal */}
        <Modal
          show={advanceModalShow}
          onHide={handleCloseAdvanceModal}
          backdrop={advSubmitting ? 'static' : true}
          keyboard={!advSubmitting}
          centered
        >
          <Form onSubmit={handleAdvanceSubmit}>
            <Modal.Header closeButton={!advSubmitting}>
              <Modal.Title>Add Advance Payment</Modal.Title>
            </Modal.Header>
            <Modal.Body>
              {error && <Alert variant="danger">{error}</Alert>}
              {success && <Alert variant="success">{success}</Alert>}
              <p>Customer: <strong>{advanceTargetCustomer?.fullName}</strong></p>
              <Form.Group controlId="advanceAmount">
                <Form.Label>Amount (INR)</Form.Label>
                <Form.Control
                  type="number"
                  step="0.01"
                  min="0"
                  value={advanceAmount}
                  onChange={(e) => setAdvanceAmount(e.target.value)}
                  required
                />
              </Form.Group>
            </Modal.Body>
            <Modal.Footer>
              <Button variant="outline-secondary" onClick={handleCloseAdvanceModal} disabled={advSubmitting}>
                Cancel
              </Button>
              <Button variant="success" type="submit" disabled={advSubmitting}>
                {advSubmitting ? 'Adding…' : 'Add Advance'}
              </Button>
            </Modal.Footer>
          </Form>
        </Modal>

      <Modal
        show={showForm}
        onHide={handleCloseForm}
        backdrop={submitting ? 'static' : true}
        keyboard={!submitting}
        centered
        size="lg"
      >
        <Form onSubmit={handleSubmit}>
          <Modal.Header closeButton={!submitting}>
            <Modal.Title>{editingCustomerId ? 'Edit Customer' : 'New Customer'}</Modal.Title>
          </Modal.Header>
          <Modal.Body>
            {error && <Alert variant="danger">{error}</Alert>}
            <Row className="g-3">
              <Form.Group as={Col} md={6} controlId="customerFullName">
                <Form.Label>Full Name</Form.Label>
                <Form.Control
                  type="text"
                  name="fullName"
                  value={formData.fullName}
                  onChange={handleChange}
                  required
                />
              </Form.Group>
              <Form.Group as={Col} md={6} controlId="customerUsername">
                <Form.Label>Username</Form.Label>
                <Form.Control
                  type="text"
                  name="username"
                  value={formData.username}
                  onChange={handleChange}
                  required={!editingCustomerId}
                  disabled={Boolean(editingCustomerId)}
                />
              </Form.Group>
              <Form.Group as={Col} md={6} controlId="customerEmail">
                <Form.Label>Email</Form.Label>
                <Form.Control
                  type="email"
                  name="email"
                  value={formData.email}
                  onChange={handleChange}
                  required={!editingCustomerId}
                />
              </Form.Group>
              <Form.Group as={Col} md={6} controlId="customerPhone">
                <Form.Label>Phone Number</Form.Label>
                <Form.Control
                  type="tel"
                  name="phoneNumber"
                  value={formData.phoneNumber}
                  onChange={handleChange}
                  required
                />
              </Form.Group>
              <Form.Group as={Col} md={6} controlId="customerAadhar">
                <Form.Label>Aadhar Number</Form.Label>
                <Form.Control
                  type="text"
                  name="aadharNumber"
                  value={formData.aadharNumber}
                  onChange={handleChange}
                  placeholder="optional"
                />
              </Form.Group>
              {!editingCustomerId && (
                <>
                  <Form.Group as={Col} md={6} controlId="customerPassword">
                    <Form.Label>Password</Form.Label>
                    <Form.Control
                      type="password"
                      name="password"
                      value={formData.password}
                      onChange={handleChange}
                      required
                    />
                  </Form.Group>
                  <Form.Group as={Col} md={6} controlId="customerConfirmPassword">
                    <Form.Label>Confirm Password</Form.Label>
                    <Form.Control
                      type="password"
                      name="confirmPassword"
                      value={formData.confirmPassword}
                      onChange={handleChange}
                      required
                    />
                  </Form.Group>
                </>
              )}
              <Form.Group as={Col} xs={12} controlId="customerAddress">
                <Form.Label>Address</Form.Label>
                <Form.Control
                  as="textarea"
                  rows={3}
                  name="address"
                  value={formData.address}
                  onChange={handleChange}
                  required
                />
              </Form.Group>
              <Form.Group as={Col} md={4} controlId="customerCity">
                <Form.Label>City</Form.Label>
                <Form.Control
                  type="text"
                  name="city"
                  value={formData.city}
                  onChange={handleChange}
                  required
                />
              </Form.Group>
              <Form.Group as={Col} md={4} controlId="customerState">
                <Form.Label>State</Form.Label>
                <Form.Control
                  type="text"
                  name="state"
                  value={formData.state}
                  onChange={handleChange}
                />
              </Form.Group>
              <Form.Group as={Col} md={4} controlId="customerPincode">
                <Form.Label>Pincode</Form.Label>
                <Form.Control
                  type="text"
                  name="pincode"
                  value={formData.pincode}
                  onChange={handleChange}
                  required
                />
              </Form.Group>
            </Row>
          </Modal.Body>
          <Modal.Footer>
            <Button variant="outline-secondary" onClick={handleCloseForm} disabled={submitting}>
              Cancel
            </Button>
            <Button variant="primary" type="submit" disabled={submitting}>
              {submitting ? 'Saving…' : 'Save Customer'}
            </Button>
          </Modal.Footer>
        </Form>
      </Modal>
    </div>
  );
};

export default ManageCustomers;
