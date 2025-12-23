import { useEffect, useMemo, useState } from 'react';
import { Alert, Button, Card, Col, Form, Row } from 'react-bootstrap';
import api from '../../api/axiosConfig.js';
import { useAuth } from '../../context/AuthContext.js';
import AdminTopBar from '../../components/AdminTopBar.js';

const getCurrentBillingMonth = () => new Date().toISOString().slice(0, 7);

const getNextBillingMonth = (lastMonth) => {
  if (!lastMonth || !/^\d{4}-\d{2}$/.test(lastMonth)) {
    return getCurrentBillingMonth();
  }
  const [yearStr, monthStr] = lastMonth.split('-');
  const year = Number(yearStr);
  const month = Number(monthStr);
  const nextDate = new Date(year, month, 1); // month is 1-based in data
  const nextYear = nextDate.getFullYear();
  const nextMonth = nextDate.getMonth() + 1; // JS month is 0-based
  return `${nextYear}-${String(nextMonth).padStart(2, '0')}`;
};

const AddReading = () => {
  const { user } = useAuth();
  const [accounts, setAccounts] = useState([]);
  const [selectedAccount, setSelectedAccount] = useState('');
  const [selectedAccountDetails, setSelectedAccountDetails] = useState(null);
  const [meterNumber, setMeterNumber] = useState('');
  const [previousReading, setPreviousReading] = useState(0);
  const [currentReading, setCurrentReading] = useState(0);
  const [billingMonth, setBillingMonth] = useState(getCurrentBillingMonth());
  const [readingType, setReadingType] = useState('ACTUAL');
  const [remarks, setRemarks] = useState('');
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const [loadingAccounts, setLoadingAccounts] = useState(false);
  const [loadingLastReading, setLoadingLastReading] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [selectedTariffCategory, setSelectedTariffCategory] = useState('ALL');

  useEffect(() => {
    const loadAccounts = async () => {
      setLoadingAccounts(true);
      try {
        const response = await api.get('/admin/accounts');
        const normalized = (response.data ?? []).map((account) => ({
          ...account,
          customerName: account.customer?.fullName || account.customerName || 'Unassigned',
          tariffCategory: account.tariffCategory || 'UNKNOWN',
          meterNumber: account.meterNumber || ''
        }));
        setAccounts(normalized);
      } catch (err) {
        console.error('Failed to fetch accounts', err);
        setError(err.response?.data?.message || 'Failed to fetch accounts');
      } finally {
        setLoadingAccounts(false);
      }
    };

    loadAccounts();
  }, []);

  useEffect(() => {
    if (!selectedAccount) {
      setPreviousReading(0);
      setCurrentReading(0);
      setBillingMonth(getCurrentBillingMonth());
      setSelectedAccountDetails(null);
      setMeterNumber('');
      return;
    }

    const accountDetails = accounts.find((account) => String(account.accountId) === String(selectedAccount));
    setSelectedAccountDetails(accountDetails || null);
    setMeterNumber(accountDetails?.meterNumber || '');

    const fetchLatestReading = async () => {
      setLoadingLastReading(true);
      setError('');
      setMessage('');
      try {
        const response = await api.get(`/admin/readings/account/${selectedAccount}`);
        const latest = response.data?.[0];
        const lastCurrent = latest?.currentReading ?? 0;
        setPreviousReading(lastCurrent);
        setCurrentReading(lastCurrent);
        setBillingMonth(getNextBillingMonth(latest?.billingMonth));
      } catch (err) {
        console.error('Failed to load last reading', err);
        setError(err.response?.data?.message || 'Unable to load last reading');
        setPreviousReading(0);
        setCurrentReading(0);
        setBillingMonth(getCurrentBillingMonth());
      } finally {
        setLoadingLastReading(false);
      }
    };

    fetchLatestReading();
  }, [selectedAccount, accounts]);

  const handleSubmit = async (event) => {
    event.preventDefault();
    setMessage('');
    setError('');

    if (!selectedAccount) {
      setError('Please select an account before submitting.');
      return;
    }

    if (!billingMonth) {
      setError('Billing month is required.');
      return;
    }

    if (currentReading < previousReading) {
      setError('Current reading cannot be less than the previous reading.');
      return;
    }

    setSubmitting(true);
    try {
      await api.post('/admin/readings', {
        accountId: Number(selectedAccount),
        currentReading,
        billingMonth,
        readingType,
        remarks: remarks.trim() || undefined
      });
      setMessage('Meter reading saved and bill generation triggered successfully.');
      setPreviousReading(0);
      setCurrentReading(0);
      setSelectedAccount('');
      setSelectedAccountDetails(null);
      setMeterNumber('');
      setBillingMonth(getCurrentBillingMonth());
      setReadingType('ACTUAL');
      setRemarks('');
    } catch (err) {
      setError(err.response?.data?.message || 'Unable to save meter reading');
    } finally {
      setSubmitting(false);
    }
  };
  const tariffCategories = useMemo(() => {
    const unique = new Set();
    accounts.forEach((account) => {
      if (account.tariffCategory) {
        unique.add(account.tariffCategory);
      }
    });
    return Array.from(unique).sort();
  }, [accounts]);

  const filteredAccounts = useMemo(() => {
    if (selectedTariffCategory === 'ALL') {
      return accounts;
    }
    return accounts.filter((account) => account.tariffCategory === selectedTariffCategory);
  }, [accounts, selectedTariffCategory]);

  useEffect(() => {
    if (tariffCategories.length === 1 && selectedTariffCategory !== tariffCategories[0]) {
      setSelectedTariffCategory(tariffCategories[0]);
      return;
    }

    if (tariffCategories.length === 0 && selectedTariffCategory !== 'ALL') {
      setSelectedTariffCategory('ALL');
    }
  }, [tariffCategories, selectedTariffCategory]);

  const handleAccountChange = (event) => {
    setSelectedAccount(event.target.value);
  };

  const handleTariffFilterChange = (event) => {
    setSelectedTariffCategory(event.target.value);
    setSelectedAccount('');
    setSelectedAccountDetails(null);
    setMeterNumber('');
    setPreviousReading(0);
    setCurrentReading(0);
    setBillingMonth(getCurrentBillingMonth());
  };

  if (!user || user.role !== 'ADMIN') {
    return (
      <div className="container py-5 text-center">
        <div className="alert alert-warning d-inline-flex align-items-center gap-2">
          <span className="fw-semibold">Access restricted.</span>
          <span>Only administrator accounts can record meter readings.</span>
        </div>
      </div>
    );
  }

  return (
    <div className="container py-4">
      <AdminTopBar
        title="Add Meter Reading"
        subtitle="Select an account and submit the latest reading to generate the bill."
      />

      {message && <Alert variant="success">{message}</Alert>}
      {error && <Alert variant="danger">{error}</Alert>}

      <Card className="card-shadow">
        <Card.Body>
          <Form onSubmit={handleSubmit}>
            <Row className="g-3">
              <Form.Group as={Col} md={4} controlId="readingTariff">
                <Form.Label>Tariff Category</Form.Label>
                <Form.Select
                  value={selectedTariffCategory}
                  onChange={handleTariffFilterChange}
                  disabled={loadingAccounts}
                >
                  <option value="ALL">All categories</option>
                  {tariffCategories.map((category) => (
                    <option key={category} value={category}>
                      {category}
                    </option>
                  ))}
                </Form.Select>
              </Form.Group>

              <Form.Group as={Col} md={6} controlId="readingAccount">
                <Form.Label>Account</Form.Label>
                <Form.Select
                  value={selectedAccount}
                  onChange={handleAccountChange}
                  disabled={loadingAccounts}
                  required
                >
                  <option value="">Select account</option>
                  {filteredAccounts.map((account) => (
                    <option key={account.accountId} value={account.accountId}>
                      {account.accountNumber} · {account.customerName}
                    </option>
                  ))}
                </Form.Select>
              </Form.Group>

              <Form.Group as={Col} md={2} controlId="readingMeter">
                <Form.Label>Meter Number</Form.Label>
                <Form.Control type="text" value={meterNumber} readOnly placeholder="Auto" />
              </Form.Group>

              {selectedAccountDetails && (
                <Col xs={12}>
                  <Alert variant="info" className="d-flex flex-wrap gap-3 mb-0">
                    <div><strong>Customer:</strong> {selectedAccountDetails.customerName}</div>
                    <div><strong>Tariff:</strong> {selectedAccountDetails.tariffCategory}</div>
                    <div><strong>Connection:</strong> {selectedAccountDetails.connectionType || 'N/A'}</div>
                    {selectedAccountDetails.sanctionedLoad && (
                      <div>
                        <strong>Sanctioned Load:</strong> {selectedAccountDetails.sanctionedLoad}
                      </div>
                    )}
                  </Alert>
                </Col>
              )}

              <Form.Group as={Col} md={3} controlId="readingPrevious">
                <Form.Label>Previous Reading</Form.Label>
                <Form.Control type="number" value={previousReading} min="0" readOnly />
              </Form.Group>

              <Form.Group as={Col} md={3} controlId="readingCurrent">
                <Form.Label>Current Reading</Form.Label>
                <Form.Control
                  type="number"
                  value={currentReading}
                  onChange={(event) => setCurrentReading(Number(event.target.value))}
                  min={previousReading}
                  required
                />
              </Form.Group>

              <Form.Group as={Col} md={3} controlId="readingMonth">
                <Form.Label>Billing Month</Form.Label>
                <Form.Control
                  type="month"
                  value={billingMonth}
                  onChange={(event) => setBillingMonth(event.target.value)}
                  required
                />
              </Form.Group>

              <Form.Group as={Col} md={3} controlId="readingType">
                <Form.Label>Reading Type</Form.Label>
                <Form.Select value={readingType} onChange={(event) => setReadingType(event.target.value)}>
                  <option value="ACTUAL">Actual</option>
                  <option value="ESTIMATED">Estimated</option>
                </Form.Select>
              </Form.Group>

              <Form.Group as={Col} xs={12} controlId="readingRemarks">
                <Form.Label>Remarks (optional)</Form.Label>
                <Form.Control
                  as="textarea"
                  rows={2}
                  value={remarks}
                  onChange={(event) => setRemarks(event.target.value)}
                  placeholder="Any notes about this reading"
                />
              </Form.Group>

              <Col xs={12} className="text-end">
                <Button
                  variant="primary"
                  type="submit"
                  disabled={!selectedAccount || submitting || loadingLastReading}
                >
                  {submitting ? 'Saving…' : 'Save Reading'}
                </Button>
              </Col>
            </Row>
          </Form>
          {loadingLastReading && (
            <div className="text-muted small mt-2">Fetching last recorded reading…</div>
          )}
        </Card.Body>
      </Card>
    </div>
  );
};

export default AddReading;
