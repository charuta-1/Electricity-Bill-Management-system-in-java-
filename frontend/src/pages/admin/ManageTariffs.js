import { useEffect, useState } from 'react';
import { Alert, Badge, Button, Card, Spinner } from 'react-bootstrap';
import api from '../../api/axiosConfig.js';
import AdminTopBar from '../../components/AdminTopBar.js';

const ManageTariffs = () => {
  const [tariffs, setTariffs] = useState([]);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);

  const formatCurrency = (value) => {
    if (value === null || value === undefined || Number.isNaN(Number(value))) {
      return '₹0.00';
    }
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      minimumFractionDigits: 2
    }).format(value);
  };

  useEffect(() => {
    const loadTariffs = async () => {
      try {
        const response = await api.get('/tariffs');
        setTariffs(response.data);
      } catch (err) {
        setError(err.response?.data?.message || 'Failed to load tariffs');
      } finally {
        setLoading(false);
      }
    };

    loadTariffs();
  }, []);

  return (
    <div className="container py-4">
      <AdminTopBar
        title="Tariff Catalog"
        subtitle="Keep slab rates, subsidies, and fixed charges up to date."
        extra={<Button variant="outline-primary" disabled>Add Tariff</Button>}
      />

      {loading && (
        <Card className="card-shadow mb-4">
          <Card.Body className="text-center text-muted">
            <Spinner animation="border" size="sm" className="me-2" /> Loading tariffs...
          </Card.Body>
        </Card>
      )}
      {error && <Alert variant="danger">{error}</Alert>}

      {!loading && !error && (
        <div className="tariff-grid">
          {tariffs.map((tariff) => (
            <Card key={tariff.tariffId} className="tariff-card">
              <Card.Body>
                <div className="d-flex justify-content-between align-items-start mb-3">
                  <div>
                    <h5 className="mb-1">{tariff.description}</h5>
                    <div className="text-uppercase text-muted small">{tariff.code}</div>
                  </div>
                  <Badge bg="primary" className="text-uppercase">{(tariff.category || 'General')}</Badge>
                </div>
                <div className="tariff-meta">
                  <div>
                    <small className="text-muted">Fixed Charge</small>
                    <div className="fw-semibold">{formatCurrency(tariff.fixedCharge)}</div>
                  </div>
                  <div>
                    <small className="text-muted">Duty Rate</small>
                    <div className="fw-semibold">{tariff.dutyRate || 0}%</div>
                  </div>
                  <div>
                    <small className="text-muted">Slabs</small>
                    <div className="fw-semibold">{tariff.slabs?.length || 0}</div>
                  </div>
                </div>
                {tariff.slabs?.length ? (
                  <div className="tariff-slabs">
                    {tariff.slabs.map((slab, index) => (
                      <div key={`${tariff.tariffId}-slab-${index}`} className="tariff-slab">
                        <span>{slab.description || `Slab ${index + 1}`}</span>
                        <div>
                          <small className="text-muted me-2">Rate</small>
                          <span>{formatCurrency(slab.rate)}</span>
                        </div>
                      </div>
                    ))}
                  </div>
                ) : (
                  <div className="tariff-slabs text-muted small">No slabs configured.</div>
                )}
              </Card.Body>
            </Card>
          ))}

          {tariffs.length === 0 && (
            <Card className="tariff-card">
              <Card.Body className="text-center text-muted">
                No tariff configurations found. Use the controls above to add your first tariff.
              </Card.Body>
            </Card>
          )}
        </div>
      )}
    </div>
  );
};

export default ManageTariffs;
