import { useEffect, useMemo, useState } from 'react';
import { Bar, Doughnut, Line } from 'react-chartjs-2';
import { Card, Col, Row } from 'react-bootstrap';
import { BsCurrencyDollar, BsFileEarmarkTextFill, BsPeopleFill, BsExclamationTriangleFill } from 'react-icons/bs';
import {
  ArcElement,
  BarElement,
  CategoryScale,
  Chart as ChartJS,
  Legend,
  LineElement,
  LinearScale,
  PointElement,
  Title,
  Tooltip
} from 'chart.js';
import api from '../../api/axiosConfig.js';
import StatCard from '../../components/StatCard.js';

ChartJS.register(CategoryScale, LinearScale, BarElement, LineElement, PointElement, ArcElement, Title, Tooltip, Legend);

const initialMetrics = {
  totalCustomers: 0,
  totalActiveAccounts: 0,
  newCustomersThisMonth: 0,
  newConnectionsThisMonth: 0,
  totalBilledThisMonth: 0,
  totalCollectedThisMonth: 0,
  totalOutstanding: 0,
  unitsConsumedThisMonth: 0,
  openComplaints: 0,
  inProgressComplaints: 0,
  resolvedToday: 0,
  collectionEfficiency: 0,
  billsGeneratedThisMonth: 0,
  overdueBills: 0
};

const initialStatusSummary = {
  paid: 0,
  unpaid: 0,
  partiallyPaid: 0,
  overdue: 0
};

const formatMonth = (year, month) => {
  const date = new Date(year, month - 1);
  return date.toLocaleString('en-IN', { month: 'short', year: 'numeric' });
};

const formatCurrency = (value) => new Intl.NumberFormat('en-IN', {
  style: 'currency',
  currency: 'INR',
  maximumFractionDigits: 0
}).format(Number(value || 0));

const AdminDashboard = () => {
  const [metrics, setMetrics] = useState(initialMetrics);
  const [collectionTrend, setCollectionTrend] = useState([]);
  const [consumptionTrend, setConsumptionTrend] = useState([]);
  const [statusSummary, setStatusSummary] = useState(initialStatusSummary);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    const loadDashboard = async () => {
      setLoading(true);
      setError('');
      try {
        const [metricsRes, collectionRes, consumptionRes, statusRes] = await Promise.all([
          api.get('/admin/reports/dashboard'),
          api.get('/admin/reports/collections', { params: { months: 6 } }),
          api.get('/admin/reports/consumption', { params: { months: 6 } }),
          api.get('/admin/reports/bills/status-summary')
        ]);

        const payload = metricsRes.data || {};
        setMetrics({
          totalCustomers: payload.totalCustomers || 0,
          totalActiveAccounts: payload.totalActiveAccounts || 0,
          newCustomersThisMonth: payload.newCustomersThisMonth || 0,
          newConnectionsThisMonth: payload.newConnectionsThisMonth || 0,
          totalBilledThisMonth: Number(payload.totalBilledThisMonth || 0),
          totalCollectedThisMonth: Number(payload.totalCollectedThisMonth || 0),
          totalOutstanding: Number(payload.totalOutstanding || 0),
          unitsConsumedThisMonth: payload.unitsConsumedThisMonth || 0,
          openComplaints: payload.openComplaints || 0,
          inProgressComplaints: payload.inProgressComplaints || 0,
          resolvedToday: payload.resolvedToday || 0,
          collectionEfficiency: Number(payload.collectionEfficiency || 0),
          billsGeneratedThisMonth: payload.billsGeneratedThisMonth || 0,
          overdueBills: payload.overdueBills || 0
        });

        setCollectionTrend(collectionRes.data || []);
        setConsumptionTrend(consumptionRes.data || []);
        setStatusSummary(statusRes.data || initialStatusSummary);
      } catch (err) {
        setError(err.response?.data?.message || 'Failed to load dashboard data');
      } finally {
        setLoading(false);
      }
    };

    loadDashboard();
  }, []);

  const revenueChartData = useMemo(
    () => ({
      labels: collectionTrend.map((item) => formatMonth(item.year, item.month)),
      datasets: [
        {
          label: 'Collections (₹)',
          data: collectionTrend.map((item) => Number(item.totalAmount || 0)),
          backgroundColor: 'rgba(37, 99, 235, 0.65)',
          borderRadius: 10,
          maxBarThickness: 48
        }
      ]
    }),
    [collectionTrend]
  );

  const consumptionChartData = useMemo(
    () => ({
      labels: consumptionTrend.map((item) => formatMonth(item.year, item.month)),
      datasets: [
        {
          label: 'Energy Consumption (kWh)',
          data: consumptionTrend.map((item) => Number(item.units || 0)),
          borderColor: '#0ea5e9',
          backgroundColor: 'rgba(14, 165, 233, 0.15)',
          tension: 0.35,
          fill: true,
          pointRadius: 4,
          pointBackgroundColor: '#0ea5e9'
        }
      ]
    }),
    [consumptionTrend]
  );

  const billStatusChartData = useMemo(() => {
    const paid = Number(statusSummary.paid || 0);
    const partial = Number(statusSummary.partiallyPaid || 0);
    const unpaid = Number(statusSummary.unpaid || 0) + partial;
    const overdue = Number(statusSummary.overdue || 0);

    return {
      labels: ['Paid', 'Unpaid', 'Overdue'],
      datasets: [
        {
          label: 'Bills',
          data: [paid, unpaid, overdue],
          backgroundColor: ['#22c55e', '#3b82f6', '#f97316'],
          borderWidth: 0
        }
      ]
    };
  }, [statusSummary]);

  const chartOptions = {
    responsive: true,
    plugins: {
      legend: {
        position: 'bottom'
      }
    },
    scales: {
      y: {
        beginAtZero: true,
        ticks: {
          callback: (value) => `₹${value.toLocaleString()}`
        }
      }
    }
  };

  const lineChartOptions = {
    responsive: true,
    plugins: {
      legend: {
        position: 'bottom'
      }
    }
  };

  return (
    <div>
      <div className="page-header">
        <h2>Welcome, Admin!</h2>
        <p className="text-muted">Here is your system overview.</p>
      </div>

      {loading && <div className="alert alert-info">Loading dashboard data...</div>}
      {error && <div className="alert alert-danger">{error}</div>}

      <Row className="g-4 mb-4">
        <Col md={3} sm={6}>
          <StatCard
            title="Total Revenue"
            value={formatCurrency(metrics.totalCollectedThisMonth)}
            icon={<BsCurrencyDollar />}
            color="success"
          />
        </Col>
        <Col md={3} sm={6}>
          <StatCard
            title="Total Customers"
            value={metrics.totalCustomers.toLocaleString()}
            icon={<BsPeopleFill />}
            color="primary"
          />
        </Col>
        <Col md={3} sm={6}>
          <StatCard
            title="Bills Generated (This Month)"
            value={metrics.billsGeneratedThisMonth.toLocaleString()}
            icon={<BsFileEarmarkTextFill />}
            color="info"
          />
        </Col>
        <Col md={3} sm={6}>
          <StatCard
            title="Overdue Bills"
            value={metrics.overdueBills.toLocaleString()}
            icon={<BsExclamationTriangleFill />}
            color="danger"
          />
        </Col>
      </Row>

      <Row className="g-4 mb-4">
        <Col md={7}>
          <Card className="card-shadow h-100">
            <Card.Body>
              <Card.Title>Monthly Revenue (Last 6 Months)</Card.Title>
              {collectionTrend.length === 0 ? (
                <p className="text-muted text-center mb-0">No revenue data available yet.</p>
              ) : (
                <Bar data={revenueChartData} options={chartOptions} height={200} />
              )}
            </Card.Body>
          </Card>
        </Col>
        <Col md={5}>
          <Card className="card-shadow h-100">
            <Card.Body>
              <Card.Title>Bills by Status</Card.Title>
              {statusSummary.paid + statusSummary.unpaid + statusSummary.partiallyPaid + statusSummary.overdue === 0 ? (
                <p className="text-muted text-center mb-0">No billing activity recorded.</p>
              ) : (
                <Doughnut data={billStatusChartData} options={{ plugins: { legend: { position: 'bottom' } } }} />
              )}
            </Card.Body>
          </Card>
        </Col>
      </Row>

      <Row className="g-4">
        <Col lg={7}>
          <Card className="card-shadow h-100">
            <Card.Body>
              <Card.Title>Monthly Energy Consumption</Card.Title>
              {consumptionTrend.length === 0 ? (
                <p className="text-muted text-center mb-0">No consumption data available.</p>
              ) : (
                <Line data={consumptionChartData} options={lineChartOptions} height={220} />
              )}
            </Card.Body>
          </Card>
        </Col>
        <Col lg={5}>
          <Card className="card-shadow h-100">
            <Card.Body className="d-flex flex-column gap-3">
              <Card.Title>Collections & Complaints Snapshot</Card.Title>
              <div>
                <small className="text-muted text-uppercase">Total Billed This Month</small>
                <h4 className="fw-semibold mb-0">{formatCurrency(metrics.totalBilledThisMonth)}</h4>
              </div>
              <div>
                <small className="text-muted text-uppercase">Collected This Month</small>
                <h4 className="text-success fw-semibold mb-0">{formatCurrency(metrics.totalCollectedThisMonth)}</h4>
              </div>
              <div>
                <small className="text-muted text-uppercase">Outstanding Balance</small>
                <h4 className="text-danger fw-semibold mb-0">{formatCurrency(metrics.totalOutstanding)}</h4>
              </div>
              <hr />
              <Row>
                <Col xs={4} className="text-center">
                  <div className="text-muted small">Open</div>
                  <div className="h4 mb-0">{metrics.openComplaints}</div>
                </Col>
                <Col xs={4} className="text-center">
                  <div className="text-muted small">In Progress</div>
                  <div className="h4 mb-0">{metrics.inProgressComplaints}</div>
                </Col>
                <Col xs={4} className="text-center">
                  <div className="text-muted small">Resolved</div>
                  <div className="h4 mb-0">{metrics.resolvedToday}</div>
                </Col>
              </Row>
              <div className="text-muted small">
                Collection Efficiency: <strong>{metrics.collectionEfficiency.toFixed(2)}%</strong>
              </div>
            </Card.Body>
          </Card>
        </Col>
      </Row>
    </div>
  );
};

export default AdminDashboard;
