import { useEffect, useMemo, useState } from 'react';
import { Button, Card, Col, Form, Modal, Row, Spinner, Alert } from 'react-bootstrap';
import { FaSync } from 'react-icons/fa';
import { BsBoxArrowUpRight } from 'react-icons/bs';
import api from '../../api/axiosConfig.js';
import AdminTopBar from '../../components/AdminTopBar.js';

const statusClasses = {
  OPEN: 'badge bg-warning-subtle text-warning fw-semibold',
  IN_PROGRESS: 'badge bg-info-subtle text-info fw-semibold',
  RESOLVED: 'badge bg-success-subtle text-success fw-semibold',
  CLOSED: 'badge bg-secondary-subtle text-secondary fw-semibold'
};

const priorityClasses = {
  LOW: 'badge bg-secondary-subtle text-secondary',
  MEDIUM: 'badge bg-primary-subtle text-primary',
  HIGH: 'badge bg-danger-subtle text-danger',
  URGENT: 'badge bg-dark text-white'
};

const ComplaintsReport = () => {
  const [complaints, setComplaints] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [priorityFilter, setPriorityFilter] = useState('ALL');
  const [searchTerm, setSearchTerm] = useState('');
  const [selectedComplaint, setSelectedComplaint] = useState(null);
  const [showModal, setShowModal] = useState(false);
  const [updateForm, setUpdateForm] = useState({ status: 'OPEN', priority: 'MEDIUM', resolution: '' });
  const [saving, setSaving] = useState(false);
  const [updateError, setUpdateError] = useState('');
  const [updateSuccess, setUpdateSuccess] = useState('');

  const loadComplaints = async () => {
    setLoading(true);
    setError('');
    try {
      const response = await api.get('/admin/complaints');
      setComplaints(response.data || []);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to load complaints report');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadComplaints();
  }, []);

  const filteredComplaints = useMemo(() => {
    return complaints
      .filter((item) => {
        if (statusFilter !== 'ALL' && item.status !== statusFilter) {
          return false;
        }
        if (priorityFilter !== 'ALL' && item.priority !== priorityFilter) {
          return false;
        }
        if (!searchTerm.trim()) {
          return true;
        }
        const haystack = [
          item.complaintNumber,
          item.customer?.fullName,
          item.account?.accountNumber,
          item.complaintType,
          item.description
        ]
          .filter(Boolean)
          .join(' ')
          .toLowerCase();
        return haystack.includes(searchTerm.trim().toLowerCase());
      })
      .sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));
  }, [complaints, statusFilter, priorityFilter, searchTerm]);

  const handleOpenModal = (complaint) => {
    setSelectedComplaint(complaint);
    setUpdateForm({
      status: complaint.status || 'OPEN',
      priority: complaint.priority || 'MEDIUM',
      resolution: complaint.resolution || ''
    });
    setUpdateError('');
    setUpdateSuccess('');
    setShowModal(true);
  };

  const handleCloseModal = () => {
    setShowModal(false);
    setSelectedComplaint(null);
    setSaving(false);
  };

  const handleUpdateChange = (field, value) => {
    setUpdateForm((prev) => ({ ...prev, [field]: value }));
  };

  const handleSave = async (event) => {
    event.preventDefault();
    if (!selectedComplaint) {
      return;
    }
    setSaving(true);
    setUpdateError('');
    setUpdateSuccess('');

    const trimmedResolution = updateForm.resolution?.trim() || '';
    const completedStatus = ['RESOLVED', 'CLOSED'].includes(updateForm.status);

    const payload = {
      status: updateForm.status,
      priority: updateForm.priority,
      resolution: trimmedResolution || null,
      resolvedAt: completedStatus && trimmedResolution ? new Date().toISOString() : null
    };

    try {
      await api.put(`/admin/complaints/${selectedComplaint.complaintId}`, payload);
      await loadComplaints();
      setUpdateSuccess('Complaint updated successfully.');
      setTimeout(() => {
        handleCloseModal();
      }, 800);
    } catch (err) {
      setUpdateError(err.response?.data?.message || 'Unable to update complaint.');
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="container py-4">
      <AdminTopBar
        title="Customer Complaints"
        subtitle="Track and triage issues raised by consumers."
        extra={(
          <Button variant="outline-primary" disabled={loading} onClick={loadComplaints}>
            <FaSync className={loading ? 'me-2 fa-spin' : 'me-2'} /> Refresh
          </Button>
        )}
      />

      <Card className="card-shadow mb-4">
        <Card.Body>
          <Row className="g-3 align-items-end">
            <Col md={4}>
              <Form.Label>Search</Form.Label>
              <Form.Control
                type="text"
                placeholder="Search by ticket, customer, account, or keywords"
                value={searchTerm}
                onChange={(event) => setSearchTerm(event.target.value)}
              />
            </Col>
            <Col md={4}>
              <Form.Label>Status</Form.Label>
              <Form.Select value={statusFilter} onChange={(event) => setStatusFilter(event.target.value)}>
                <option value="ALL">All statuses</option>
                <option value="OPEN">Open</option>
                <option value="IN_PROGRESS">In progress</option>
                <option value="RESOLVED">Resolved</option>
                <option value="CLOSED">Closed</option>
              </Form.Select>
            </Col>
            <Col md={4}>
              <Form.Label>Priority</Form.Label>
              <Form.Select value={priorityFilter} onChange={(event) => setPriorityFilter(event.target.value)}>
                <option value="ALL">All priorities</option>
                <option value="LOW">Low</option>
                <option value="MEDIUM">Medium</option>
                <option value="HIGH">High</option>
                <option value="URGENT">Urgent</option>
              </Form.Select>
            </Col>
          </Row>
        </Card.Body>
      </Card>

      {error && <Alert variant="danger">{error}</Alert>}

      <Card className="card-shadow">
        <div className="table-responsive">
          <table className="table align-middle mb-0 table-hover table-borderless">
            <thead className="table-light">
              <tr>
                <th>Ticket</th>
                <th>Customer</th>
                <th>Account</th>
                <th>Subject &amp; Description</th>
                <th>Type</th>
                <th>Priority</th>
                <th>Status</th>
                <th>Updated</th>
                <th className="text-end">Actions</th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr>
                  <td colSpan={9} className="text-center py-5">
                    <Spinner animation="border" size="sm" className="me-2" /> Loading complaints...
                  </td>
                </tr>
              ) : filteredComplaints.length === 0 ? (
                <tr>
                  <td colSpan={9} className="text-center text-muted py-5">
                    No complaints match the selected filters.
                  </td>
                </tr>
              ) : (
                filteredComplaints.map((complaint) => (
                  <tr key={complaint.complaintId}>
                    <td>
                      <div className="fw-semibold">{complaint.complaintNumber}</div>
                      <div className="text-muted small">
                        Raised {new Date(complaint.createdAt).toLocaleString()}
                      </div>
                    </td>
                    <td>
                      <div>{complaint.customer?.fullName || '—'}</div>
                      <div className="text-muted small">{complaint.customer?.customerNumber || ''}</div>
                    </td>
                    <td>
                      <div>{complaint.account?.accountNumber || '—'}</div>
                      <div className="text-muted small">{complaint.account?.connectionType || ''}</div>
                    </td>
                    <td style={{ maxWidth: '300px' }}>
                      <div className="fw-semibold mb-1">{complaint.subject}</div>
                      <div className="text-muted small text-truncate" title={complaint.description}>
                        {complaint.description}
                      </div>
                    </td>
                    <td className="text-uppercase">{complaint.complaintType}</td>
                    <td>
                      <span className={priorityClasses[complaint.priority] || 'badge bg-secondary-subtle text-secondary'}>
                        {complaint.priority}
                      </span>
                    </td>
                    <td>
                      <span className={statusClasses[complaint.status] || 'badge bg-secondary-subtle text-secondary'}>
                        {complaint.status.replace('_', ' ')}
                      </span>
                    </td>
                    <td>
                      <div>{complaint.updatedAt ? new Date(complaint.updatedAt).toLocaleString() : '—'}</div>
                      {complaint.assignedTo?.fullName && (
                        <div className="text-muted small">Assigned: {complaint.assignedTo.fullName}</div>
                      )}
                    </td>
                    <td className="text-end">
                      <Button
                        variant="outline-primary"
                        size="sm"
                        onClick={() => handleOpenModal(complaint)}
                      >
                        <BsBoxArrowUpRight className="me-2" /> Manage
                      </Button>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </Card>

      <Modal show={showModal} onHide={handleCloseModal} centered size="lg">
        <Modal.Header closeButton>
          <Modal.Title>Manage Complaint</Modal.Title>
        </Modal.Header>
        <Form onSubmit={handleSave}>
          <Modal.Body>
            {updateError && <Alert variant="danger" className="mb-3">{updateError}</Alert>}
            {updateSuccess && <Alert variant="success" className="mb-3">{updateSuccess}</Alert>}

            {selectedComplaint && (
              <>
                <Row className="mb-4 g-3">
                  <Col md={6}>
                    <div className="text-muted text-uppercase small">Ticket</div>
                    <div className="h5 mb-0">{selectedComplaint.complaintNumber}</div>
                    <div className="text-muted small">
                      Raised {new Date(selectedComplaint.createdAt).toLocaleString()}
                    </div>
                  </Col>
                  <Col md={6}>
                    <div className="text-muted text-uppercase small">Customer</div>
                    <div className="h6 mb-0">{selectedComplaint.customer?.fullName || '—'}</div>
                    <div className="text-muted small">{selectedComplaint.customer?.customerNumber || ''}</div>
                  </Col>
                </Row>

                <Card className="bg-light border-0 mb-4">
                  <Card.Body>
                    <div className="text-muted text-uppercase small mb-2">Description</div>
                    <p className="mb-0">{selectedComplaint.description}</p>
                  </Card.Body>
                </Card>

                <Row className="g-3">
                  <Col md={4}>
                    <Form.Group controlId="complaintStatus">
                      <Form.Label>Status</Form.Label>
                      <Form.Select
                        value={updateForm.status}
                        onChange={(event) => handleUpdateChange('status', event.target.value)}
                      >
                        <option value="OPEN">Open</option>
                        <option value="IN_PROGRESS">In Progress</option>
                        <option value="RESOLVED">Resolved</option>
                        <option value="CLOSED">Closed</option>
                      </Form.Select>
                    </Form.Group>
                  </Col>
                  <Col md={4}>
                    <Form.Group controlId="complaintPriority">
                      <Form.Label>Priority</Form.Label>
                      <Form.Select
                        value={updateForm.priority}
                        onChange={(event) => handleUpdateChange('priority', event.target.value)}
                      >
                        <option value="LOW">Low</option>
                        <option value="MEDIUM">Medium</option>
                        <option value="HIGH">High</option>
                        <option value="URGENT">Urgent</option>
                      </Form.Select>
                    </Form.Group>
                  </Col>
                  <Col md={4}>
                    <Form.Group controlId="complaintType">
                      <Form.Label>Type</Form.Label>
                      <Form.Control value={selectedComplaint.complaintType} readOnly />
                    </Form.Group>
                  </Col>
                </Row>

                <Form.Group controlId="complaintResolution" className="mt-4">
                  <Form.Label>Resolution Notes</Form.Label>
                  <Form.Control
                    as="textarea"
                    rows={4}
                    placeholder="Document the steps taken or resolution provided..."
                    value={updateForm.resolution || ''}
                    onChange={(event) => handleUpdateChange('resolution', event.target.value)}
                  />
                  <Form.Text className="text-muted">
                    Provide clear guidance or follow-up actions shared with the customer.
                  </Form.Text>
                </Form.Group>
              </>
            )}
          </Modal.Body>
          <Modal.Footer>
            <Button variant="outline-secondary" onClick={handleCloseModal} disabled={saving}>
              Cancel
            </Button>
            <Button variant="primary" type="submit" disabled={saving}>
              {saving ? <Spinner animation="border" size="sm" className="me-2" /> : null}
              Save Changes
            </Button>
          </Modal.Footer>
        </Form>
      </Modal>
    </div>
  );
};

export default ComplaintsReport;
