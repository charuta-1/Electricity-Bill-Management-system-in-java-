import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { Button, Card, Col, Row } from 'react-bootstrap';
import { BsFileEarmarkMedicalFill, BsFileEarmarkCheckFill, BsLightningFill } from 'react-icons/bs';
import api from '../../api/axiosConfig.js';
import { useAuth } from '../../context/AuthContext.js';
import StatCard from '../../components/StatCard.js';
import UsageChart from './UsageChart.js';

const formatCurrency = (value) => new Intl.NumberFormat('en-IN', {
  style: 'currency',
  currency: 'INR',
  maximumFractionDigits: 0
}).format(Number(value || 0));

const CustomerDashboard = () => {
  const { user } = useAuth();
  const [dashboardData, setDashboardData] = useState({
    amountDue: 0,
    lastBill: 0,
    lastUnits: 0,
    nextDueDate: null
  });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    const fetchSummary = async () => {
      try {
        const response = await api.get('/customers/self/summary');
        const payload = response.data || {};
        setDashboardData({
          amountDue: Number(payload.outstandingAmount || 0),
          lastBill: Number(payload.lastBillAmount || 0),
          lastUnits: Number(payload.averageConsumption || 0),
          nextDueDate: payload.nextDueDate || null
        });
      } catch (err) {
        setError(err.response?.data?.message || 'Failed to load account summary');
      } finally {
        setLoading(false);
      }
    };

    fetchSummary();
  }, []);

  return (
    <div>
      <div className="page-header">
        <h2>My Dashboard</h2>
        <p className="text-muted">Welcome back, {user?.fullName || 'Customer'}!</p>
      </div>

      {loading && <div className="alert alert-info">Loading dashboard...</div>}
      {error && <div className="alert alert-danger">{error}</div>}

      <Row className="g-4 mb-4">
        <Col md={4} sm={6}>
          <StatCard
            title="Current Amount Due"
            value={formatCurrency(dashboardData.amountDue)}
            icon={<BsFileEarmarkMedicalFill />}
            color="danger"
          />
        </Col>
        <Col md={4} sm={6}>
          <StatCard
            title="Last Bill Total"
            value={formatCurrency(dashboardData.lastBill)}
            icon={<BsFileEarmarkCheckFill />}
            color="success"
          />
        </Col>
        <Col md={4} sm={6}>
          <StatCard
            title="Last Reading (Units)"
            value={`${dashboardData.lastUnits} kWh`}
            icon={<BsLightningFill />}
            color="info"
          />
        </Col>
      </Row>

      <Row className="g-4">
        <Col md={7}>
          <Card className="card-shadow h-100">
            <Card.Body>
              <Card.Title>My Monthly Consumption (Units)</Card.Title>
              <UsageChart compact />
            </Card.Body>
          </Card>
        </Col>
        <Col md={5}>
          <Card className="card-shadow h-100 quick-actions">
            <Card.Body className="d-flex flex-column">
              <Card.Title>Quick Actions</Card.Title>
              <Button as={Link} to="/customer/pay" variant="primary" size="lg" className="w-100 mb-3">
                Pay My Bill
              </Button>
              <Button as={Link} to="/customer/bills" variant="outline-primary" className="w-100 mb-3">
                View All Bills
              </Button>
              <Button as={Link} to="/customer/complaints" variant="outline-secondary" className="w-100">
                File a Complaint
              </Button>
              <div className="mt-auto text-muted small pt-3">
                Next due date:{' '}
                <strong>
                  {dashboardData.nextDueDate
                    ? new Date(dashboardData.nextDueDate).toLocaleDateString()
                    : 'No dues'}
                </strong>
              </div>
            </Card.Body>
          </Card>
        </Col>
      </Row>
    </div>
  );
};

export default CustomerDashboard;
