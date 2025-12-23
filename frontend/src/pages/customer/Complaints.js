import { useCallback, useEffect, useState } from 'react';
import api from '../../api/axiosConfig.js';

const Complaints = () => {
  const [complaints, setComplaints] = useState([]);
  const [category, setCategory] = useState('BILLING');
  const [priority, setPriority] = useState('MEDIUM');
  const [description, setDescription] = useState('');
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const loadComplaints = useCallback(async () => {
    try {
      const response = await api.get('/customers/self/complaints');
      setComplaints(response.data || []);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to load complaints');
    }
  }, []);

  useEffect(() => {
    loadComplaints();
  }, [loadComplaints]);

  const handleSubmit = async (event) => {
    event.preventDefault();
    setMessage('');
    setError('');
    if (!description.trim()) {
      setError('Please describe the issue before submitting.');
      return;
    }

    setSubmitting(true);
    try {
      const payload = {
        complaintType: category,
        priority,
        subject: `${category} issue`,
        description: description.trim()
      };

      await api.post('/customer/complaints', payload);
      setMessage('Complaint submitted successfully. Support team will reach out soon.');
      setDescription('');
      await loadComplaints();
    } catch (err) {
      const data = err.response?.data;
      setError(typeof data === 'string' ? data : data?.message || 'Failed to submit complaint');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="container py-4">
      <div className="row g-4">
        <div className="col-lg-5">
          <div className="card card-shadow h-100">
            <div className="card-body">
              <h2 className="h5 mb-3">Raise Complaint</h2>
              {message && <div className="alert alert-success">{message}</div>}
              {error && <div className="alert alert-danger">{error}</div>}
              <form className="row g-3" onSubmit={handleSubmit}>
                <div className="col-12">
                  <label htmlFor="complaint-category" className="form-label">Category</label>
                  <select id="complaint-category" className="form-select" value={category} onChange={(event) => setCategory(event.target.value)}>
                    <option value="BILLING">Billing</option>
                    <option value="SUPPLY">Power Supply</option>
                    <option value="SERVICE">Customer Service</option>
                  </select>
                </div>
                <div className="col-12">
                  <label htmlFor="complaint-priority" className="form-label">Priority</label>
                  <select id="complaint-priority" className="form-select" value={priority} onChange={(event) => setPriority(event.target.value)}>
                    <option value="LOW">Low</option>
                    <option value="MEDIUM">Medium</option>
                    <option value="HIGH">High</option>
                  </select>
                </div>
                <div className="col-12">
                  <label htmlFor="complaint-description" className="form-label">Description</label>
                  <textarea
                    id="complaint-description"
                    className="form-control"
                    rows="4"
                    value={description}
                    onChange={(event) => setDescription(event.target.value)}
                    required
                  />
                </div>
                <div className="col-12 text-end">
                  <button className="btn btn-primary" type="submit" disabled={submitting}>
                    {submitting ? 'Submitting…' : 'Submit'}
                  </button>
                </div>
              </form>
            </div>
          </div>
        </div>
        <div className="col-lg-7">
          <div className="card card-shadow h-100">
            <div className="card-body">
              <h2 className="h5 mb-3">Recent Complaints</h2>
              <div className="table-responsive">
                <table className="table align-middle">
                  <thead>
                    <tr>
                      <th>ID</th>
                      <th>Category</th>
                      <th>Priority</th>
                      <th>Status</th>
                      <th>Updated</th>
                    </tr>
                  </thead>
                  <tbody>
                    {complaints.map((item) => (
                      <tr key={item.complaintId}>
                        <td>{item.trackingNumber}</td>
                        <td>{item.complaintType}</td>
                        <td>
                          <span className="badge bg-primary-subtle text-primary">{item.priority}</span>
                        </td>
                        <td>
                          <span className="badge bg-success-subtle text-success">{item.status}</span>
                        </td>
                        <td>{new Date(item.modifiedAt).toLocaleString()}</td>
                      </tr>
                    ))}
                    {complaints.length === 0 && (
                      <tr>
                        <td colSpan="5" className="text-center text-muted py-4">
                          No complaints raised yet.
                        </td>
                      </tr>
                    )}
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Complaints;
