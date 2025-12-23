import { useState } from 'react';
import api from '../../api/axiosConfig.js';
import AdminTopBar from '../../components/AdminTopBar.js';

const GenerateBills = () => {
  const [month, setMonth] = useState('');
  const [status, setStatus] = useState('');
  const [error, setError] = useState('');

  const handleGenerate = async (event) => {
    event.preventDefault();
    setStatus('');
    setError('');
    try {
      const response = await api.post('/bills/generate', { billingMonth: month });
      setStatus(response.data?.message || 'Bills generated successfully.');
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to generate bills');
    }
  };

  return (
    <div className="container py-4">
      <AdminTopBar
        title="Generate Monthly Bills"
        subtitle="Launch batch bill calculation for all active accounts for a given cycle."
      />

      {status && <div className="alert alert-success">{status}</div>}
      {error && <div className="alert alert-danger">{error}</div>}

      <div className="card card-shadow">
        <div className="card-body">
          <form className="row g-3" onSubmit={handleGenerate}>
            <div className="col-md-6">
              <label htmlFor="billing-month" className="form-label">Billing Month</label>
              <input
                id="billing-month"
                type="month"
                className="form-control"
                value={month}
                onChange={(event) => setMonth(event.target.value)}
                required
              />
            </div>
            <div className="col-12 text-end">
              <button className="btn btn-primary" type="submit">
                Start Batch Bill Generation
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
};

export default GenerateBills;
