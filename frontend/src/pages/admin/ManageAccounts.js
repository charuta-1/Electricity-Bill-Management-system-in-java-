import { useEffect, useMemo, useState } from 'react';
import { Alert, Button, Card, Col, Form, Modal, Row, Table } from 'react-bootstrap';
import { FaPlus, FaRegEdit, FaTrashAlt } from 'react-icons/fa';
import api from '../../api/axiosConfig.js';
import AdminTopBar from '../../components/AdminTopBar.js';

const TARIFF_CATEGORIES = [
  { value: 'LT-I', label: 'LT-I · Residential (Domestic)' },
  { value: 'LT-II', label: 'LT-II · Commercial (Shops/Offices)' },
  { value: 'LT-III', label: 'LT-III · Public Services/Industrial' },
  { value: 'LT-IV', label: 'LT-IV · Agricultural Pumps' },
  { value: 'LT-V', label: 'LT-V · Agricultural Allied/Seasonal' }
];

const TARIFF_BY_CONNECTION = {
  RESIDENTIAL: ['LT-I'],
  COMMERCIAL: ['LT-II'],
  INDUSTRIAL: ['LT-III'],
  AGRICULTURAL: ['LT-IV', 'LT-V']
};

const defaultAccountForm = {
  customerId: '',
  meterNumber: '',
  connectionType: 'RESIDENTIAL',
  sanctionedLoad: '1.00',
  connectionDate: new Date().toISOString().slice(0, 10),
  installationAddress: '',
  tariffCategory: TARIFF_CATEGORIES[0].value,
  isActive: true
};

const ManageAccounts = () => {
  const [accounts, setAccounts] = useState([]);
  const [customers, setCustomers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [formData, setFormData] = useState({ ...defaultAccountForm });
  const [showForm, setShowForm] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [editingId, setEditingId] = useState(null);
  const [deletingId, setDeletingId] = useState(null);
  const [loadingNextMeter, setLoadingNextMeter] = useState(false);
  const [nextMeterError, setNextMeterError] = useState('');

  const loadData = async () => {
    setLoading(true);
    setError('');
    try {
      const [accountsRes, customersRes] = await Promise.all([
        api.get('/admin/accounts'),
        api.get('/admin/customers')
      ]);
      setAccounts(accountsRes.data || []);
      setCustomers(customersRes.data || []);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to load accounts');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const fetchNextMeterNumber = async () => {
    try {
      const { data } = await api.get('/admin/accounts/next-meter');
      setNextMeterError('');
      return data;
    } catch (err) {
      console.error('Failed to fetch next meter number', err);
      setNextMeterError('Unable to estimate the next meter number automatically. A unique number will be assigned on save.');
      return '';
    }
  };

  const handleOpenForm = async (account = null) => {
    setError('');
    setSuccess('');
    if (account) {
      setEditingId(account.accountId);
      setNextMeterError('');
      const connectionType = account.connectionType || 'RESIDENTIAL';
      const allowedTariffs = TARIFF_BY_CONNECTION[connectionType] || [TARIFF_CATEGORIES[0].value];
      const normalizedTariff = (account.tariffCategory || '').toUpperCase();
      const resolvedTariff = allowedTariffs.includes(normalizedTariff)
        ? normalizedTariff
        : allowedTariffs[0];
      setFormData({
        customerId: account.customer?.customerId || '',
        meterNumber: account.meterNumber || '',
        connectionType,
        sanctionedLoad: String(account.sanctionedLoad ?? '1.00'),
        connectionDate: account.connectionDate || new Date().toISOString().slice(0, 10),
  installationAddress: account.installationAddress || '',
        tariffCategory: resolvedTariff,
        isActive: account.isActive !== false
      });
      setShowForm(true);
    } else {
      setEditingId(null);
      setFormData({ ...defaultAccountForm, meterNumber: '' });
      setNextMeterError('');
      setShowForm(true);
      setLoadingNextMeter(true);
      const nextMeter = await fetchNextMeterNumber();
      setFormData((prev) => ({ ...prev, meterNumber: nextMeter || '' }));
      setLoadingNextMeter(false);
    }
  };

  const handleCloseForm = () => {
    if (submitting) return;
    setShowForm(false);
    setEditingId(null);
  setFormData({ ...defaultAccountForm });
    setLoadingNextMeter(false);
    setNextMeterError('');
  };

  const handleChange = (event) => {
    const { name, value, type, checked } = event.target;
    if (name === 'connectionType') {
      const allowed = TARIFF_BY_CONNECTION[value] || [TARIFF_CATEGORIES[0].value];
      setFormData((prev) => ({
        ...prev,
        connectionType: value,
        tariffCategory: allowed.includes(prev.tariffCategory) ? prev.tariffCategory : allowed[0]
      }));
      return;
    }
    setFormData((prev) => ({
      ...prev,
      [name]: type === 'checkbox' ? checked : value
    }));
  };

  const accountPayload = useMemo(() => {
    const payload = {
      customer: { customerId: Number(formData.customerId) },
      connectionType: formData.connectionType,
      sanctionedLoad: Number(formData.sanctionedLoad || 0).toFixed(2),
      connectionDate: formData.connectionDate,
      installationAddress: formData.installationAddress.trim(),
      tariffCategory: formData.tariffCategory,
      isActive: Boolean(formData.isActive)
    };

    if (editingId) {
      payload.meterNumber = formData.meterNumber.trim();
    }

    return payload;
  }, [formData, editingId]);

  const availableTariffs = useMemo(() => {
    const allowed = TARIFF_BY_CONNECTION[formData.connectionType] || TARIFF_CATEGORIES.map((item) => item.value);
    return TARIFF_CATEGORIES.filter((tariff) => allowed.includes(tariff.value));
  }, [formData.connectionType]);

  useEffect(() => {
    if (availableTariffs.length === 0) {
      return;
    }
    if (!availableTariffs.some((tariff) => tariff.value === formData.tariffCategory)) {
      setFormData((prev) => ({ ...prev, tariffCategory: availableTariffs[0].value }));
    }
  }, [availableTariffs, formData.tariffCategory]);

  const handleSubmit = async (event) => {
    event.preventDefault();
    setSubmitting(true);
    setError('');
    setSuccess('');

    try {
      if (!formData.customerId) {
        throw new Error('Select a customer before saving the account');
      }

      if (editingId) {
        await api.put(`/admin/accounts/${editingId}`, accountPayload);
        setSuccess('Account updated successfully.');
      } else {
        await api.post('/admin/accounts', accountPayload);
        setSuccess('Account created successfully.');
      }

      await loadData();
      handleCloseForm();
    } catch (err) {
      const data = err.response?.data;
      const message = typeof data === 'string' ? data : data?.message || err.message || 'Failed to save account';
      setError(message);
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = async (accountId) => {
    if (!window.confirm('Delete this account? This will remove related meter readings and bills.')) {
      return;
    }

    setDeletingId(accountId);
    setError('');
    setSuccess('');
    try {
      await api.delete(`/admin/accounts/${accountId}`);
      setSuccess('Account deleted successfully.');
      await loadData();
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to delete account');
    } finally {
      setDeletingId(null);
    }
  };

  const renderCustomerName = (account) => {
    if (account.customer?.fullName) {
      return account.customer.fullName;
    }
    if (account.customerName) {
      return account.customerName;
    }
    return '—';
  };

  return (
    <div className="container py-4">
      <AdminTopBar
        title="Service Accounts"
        subtitle="Overview of active connections and assigned tariffs."
        extra={(
          <Button variant="primary" onClick={() => handleOpenForm()}>
            <FaPlus className="me-2" /> New Account
          </Button>
        )}
      />

      {loading && <Alert variant="info">Loading accounts...</Alert>}
      {error && !showForm && <Alert variant="danger">{error}</Alert>}
      {success && <Alert variant="success">{success}</Alert>}

      <Card className="card-shadow">
        <Card.Body className="p-0">
          <Table responsive hover borderless className="align-middle mb-0">
            <thead className="table-light">
              <tr>
                <th>Account #</th>
                <th>Customer</th>
                <th>Meter</th>
                <th>Connection Type</th>
                <th>Tariff</th>
                <th>Status</th>
                <th className="text-end">Actions</th>
              </tr>
            </thead>
            <tbody>
              {accounts.map((account) => (
                <tr key={account.accountId}>
                  <td>{account.accountNumber}</td>
                  <td>{renderCustomerName(account)}</td>
                  <td>{account.meterNumber}</td>
                  <td>{account.connectionType}</td>
                  <td>{account.tariffCategory}</td>
                  <td>
                    <span className={`badge ${account.isActive !== false ? 'bg-success-subtle text-success' : 'bg-secondary'}`}>
                      {account.isActive !== false ? 'Active' : 'Inactive'}
                    </span>
                  </td>
                  <td className="text-end">
                    <div className="d-inline-flex gap-2">
                      <Button
                        size="sm"
                        variant="outline-secondary"
                        onClick={() => handleOpenForm(account)}
                      >
                        <FaRegEdit className="me-1" /> Edit
                      </Button>
                      <Button
                        size="sm"
                        variant="outline-danger"
                        onClick={() => handleDelete(account.accountId)}
                        disabled={deletingId === account.accountId}
                      >
                        {deletingId === account.accountId ? 'Deleting…' : (<><FaTrashAlt className="me-1" /> Delete</>)}
                      </Button>
                    </div>
                  </td>
                </tr>
              ))}
              {accounts.length === 0 && !loading && (
                <tr>
                  <td colSpan="7" className="text-center py-4 text-muted">
                    No accounts found. Create a customer first and link a new account.
                  </td>
                </tr>
              )}
            </tbody>
          </Table>
        </Card.Body>
      </Card>

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
            <Modal.Title>{editingId ? 'Edit Service Account' : 'New Service Account'}</Modal.Title>
          </Modal.Header>
          <Modal.Body>
            {error && <Alert variant="danger">{error}</Alert>}
            <Row className="g-3">
              <Form.Group as={Col} md={6} controlId="accountCustomer">
                <Form.Label>Customer</Form.Label>
                <Form.Select
                  name="customerId"
                  value={formData.customerId}
                  onChange={handleChange}
                  required
                  disabled={Boolean(editingId)}
                >
                  <option value="">Select customer</option>
                  {customers.map((customer) => (
                    <option key={customer.customerId} value={customer.customerId}>
                      {customer.fullName} ({customer.customerNumber})
                    </option>
                  ))}
                </Form.Select>
              </Form.Group>
              <Form.Group as={Col} md={6} controlId="accountMeter">
                <Form.Label>Meter Number</Form.Label>
                <Form.Control
                  type="text"
                  name="meterNumber"
                  value={formData.meterNumber}
                  onChange={editingId ? handleChange : undefined}
                  readOnly={!editingId}
                  required={Boolean(editingId)}
                />
                {!editingId && (
                  <Form.Text>
                    {loadingNextMeter
                      ? 'Generating next meter number…'
                      : nextMeterError
                        ? nextMeterError
                        : formData.meterNumber
                          ? `Auto-assigned as ${formData.meterNumber}`
                          : 'Meter number will be generated automatically'}
                  </Form.Text>
                )}
              </Form.Group>
              <Form.Group as={Col} md={6} controlId="accountConnectionType">
                <Form.Label>Connection Type</Form.Label>
                <Form.Select
                  name="connectionType"
                  value={formData.connectionType}
                  onChange={handleChange}
                >
                  <option value="RESIDENTIAL">Residential</option>
                  <option value="COMMERCIAL">Commercial</option>
                  <option value="INDUSTRIAL">Industrial</option>
                  <option value="AGRICULTURAL">Agricultural</option>
                </Form.Select>
              </Form.Group>
              <Form.Group as={Col} md={6} controlId="accountTariff">
                <Form.Label>Tariff Category</Form.Label>
                <Form.Select
                  name="tariffCategory"
                  value={formData.tariffCategory}
                  onChange={handleChange}
                  required
                >
                  {availableTariffs.map((tariff) => (
                    <option key={tariff.value} value={tariff.value}>
                      {tariff.label}
                    </option>
                  ))}
                </Form.Select>
              </Form.Group>
              <Form.Group as={Col} md={4} controlId="accountSanctionedLoad">
                <Form.Label>Sanctioned Load (kW)</Form.Label>
                <Form.Control
                  type="number"
                  min="0"
                  step="0.01"
                  name="sanctionedLoad"
                  value={formData.sanctionedLoad}
                  onChange={handleChange}
                  required
                />
              </Form.Group>
              <Form.Group as={Col} md={4} controlId="accountConnectionDate">
                <Form.Label>Connection Date</Form.Label>
                <Form.Control
                  type="date"
                  name="connectionDate"
                  value={formData.connectionDate}
                  onChange={handleChange}
                  required
                />
              </Form.Group>
              <Form.Group as={Col} md={4} controlId="accountActive" className="d-flex align-items-center">
                <Form.Check
                  type="checkbox"
                  id="accountActiveCheck"
                  label="Active connection"
                  name="isActive"
                  checked={formData.isActive}
                  onChange={handleChange}
                  className="mt-md-4"
                />
              </Form.Group>
              <Form.Group as={Col} xs={12} controlId="accountInstallationAddress">
                <Form.Label>Installation Address</Form.Label>
                <Form.Control
                  as="textarea"
                  rows={3}
                  name="installationAddress"
                  value={formData.installationAddress}
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
              {submitting ? 'Saving…' : 'Save Account'}
            </Button>
          </Modal.Footer>
        </Form>
      </Modal>
    </div>
  );
};

export default ManageAccounts;
