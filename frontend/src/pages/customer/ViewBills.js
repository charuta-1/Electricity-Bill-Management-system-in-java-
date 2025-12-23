import { useEffect, useState } from 'react';
import { FaDownload, FaEye, FaQrcode } from 'react-icons/fa';
import api from '../../api/axiosConfig.js';

const ViewBills = () => {
  const [bills, setBills] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [previewBill, setPreviewBill] = useState(null);
  const [previewLoading, setPreviewLoading] = useState(false);

  const formatDate = (dateValue) => {
    if (!dateValue) return '—';
    const date = new Date(dateValue);
    return Number.isNaN(date.getTime()) ? '—' : date.toLocaleDateString();
  };

  const formatCurrency = (value) => {
    if (value === null || value === undefined) return '—';
    const numeric = typeof value === 'number' ? value : Number(value);
    if (Number.isNaN(numeric)) {
      return value;
    }
    return `₹${numeric.toLocaleString(undefined, {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2
    })}`;
  };

  useEffect(() => {
    const fetchBills = async () => {
      try {
        const response = await api.get('/customers/self/bills');
        setBills(response.data);
      } catch (err) {
        setError(err.response?.data?.message || 'Failed to fetch bills');
      } finally {
        setLoading(false);
      }
    };

    fetchBills();
  }, []);

  const downloadBill = async (bill) => {
    try {
      const response = await api.get(`/customers/self/bills/${bill.billId}/pdf`, { responseType: 'blob' });
      const url = window.URL.createObjectURL(new Blob([response.data], { type: 'application/pdf' }));
      const link = document.createElement('a');
      link.href = url;
      const invoiceSlug = bill.invoiceNumber ? bill.invoiceNumber.replace(/\//g, '-') : `bill-${bill.billId}`;
      link.setAttribute('download', `${invoiceSlug}.pdf`);
      document.body.appendChild(link);
      link.click();
      link.parentNode.removeChild(link);
      window.URL.revokeObjectURL(url);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to download bill');
    }
  };

  const openPreview = async (bill) => {
    if (previewBill) {
      closePreview();
    }
    setError('');
    setPreviewLoading(true);
    setPreviewBill({ listItem: bill });

    try {
      const [detailResponse, pdfResponse, qrResponse] = await Promise.all([
        api.get(`/customer/portal/bills/${bill.billId}`),
        api.get(`/customers/self/bills/${bill.billId}/pdf`, { responseType: 'blob' }),
        api
          .get(`/customers/self/bills/${bill.billId}/qr`, { responseType: 'blob' })
          .catch((qrErr) => {
            if (qrErr.response?.status === 404) {
              return null;
            }
            throw qrErr;
          })
      ]);

      const pdfUrl = window.URL.createObjectURL(new Blob([pdfResponse.data], { type: 'application/pdf' }));
      const qrUrl = qrResponse
        ? window.URL.createObjectURL(new Blob([qrResponse.data], { type: 'image/png' }))
        : null;

      setPreviewBill({
        bill: detailResponse.data,
        listItem: bill,
        pdfUrl,
        qrUrl
      });
    } catch (err) {
      setPreviewBill(null);
      setError(err.response?.data?.message || 'Unable to preview bill');
    } finally {
      setPreviewLoading(false);
    }
  };

  const closePreview = () => {
    if (previewBill?.pdfUrl) {
      window.URL.revokeObjectURL(previewBill.pdfUrl);
    }
    if (previewBill?.qrUrl) {
      window.URL.revokeObjectURL(previewBill.qrUrl);
    }
    setPreviewLoading(false);
    setPreviewBill(null);
  };

  const billRows = bills.map((bill) => {
    const badgeClass = bill.billStatus === 'PAID'
      ? 'bg-success-subtle text-success'
      : bill.billStatus === 'OVERDUE'
        ? 'bg-danger-subtle text-danger'
        : 'bg-warning-subtle text-warning';

    return (
      <tr key={bill.billId}>
        <td>{bill.invoiceNumber}</td>
        <td>{formatDate(bill.billDate)}</td>
        <td>{formatDate(bill.dueDate)}</td>
        <td>{formatCurrency(bill.netPayable ?? bill.balanceAmount)}</td>
        <td>
          <span className={`badge ${badgeClass}`}>
            {bill.billStatus}
          </span>
        </td>
        <td className="text-end">
          <div className="btn-group" role="group" aria-label="Bill actions">
            <button
              type="button"
              className="btn btn-outline-secondary btn-sm"
              onClick={() => openPreview(bill)}
            >
              <FaEye className="me-2" /> Preview
            </button>
            <button
              type="button"
              className="btn btn-outline-primary btn-sm"
              onClick={() => downloadBill(bill)}
            >
              <FaDownload className="me-2" /> Download PDF
            </button>
          </div>
        </td>
      </tr>
    );
  });

  return (
    <div className="container py-4">
      <h1 className="h4 mb-3">My Bills</h1>
      <p className="text-muted">Access past invoices and track payment status.</p>

      {loading && <div className="alert alert-info">Loading bills...</div>}
      {error && <div className="alert alert-danger">{error}</div>}

      <div className="card card-shadow">
        <div className="table-responsive">
          <table className="table align-middle">
            <thead>
              <tr>
                <th>Invoice</th>
                <th>Bill Date</th>
                <th>Due Date</th>
                <th>Amount</th>
                <th>Status</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {billRows}
              {bills.length === 0 && !loading && (
                <tr>
                  <td colSpan="6" className="text-center text-muted py-4">
                    No bills available yet.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>

      {previewBill && (
        <>
          <div className="modal fade show d-block" tabIndex="-1" role="dialog">
            <div className="modal-dialog modal-xl modal-dialog-scrollable" role="document">
              <div className="modal-content">
                <div className="modal-header">
                  <h5 className="modal-title">
                    Invoice {previewBill.bill?.invoiceNumber || previewBill.listItem?.invoiceNumber}
                  </h5>
                  <button type="button" className="btn-close" aria-label="Close" onClick={closePreview}></button>
                </div>
                <div className="modal-body">
                  {previewLoading ? (
                    <div className="d-flex align-items-center justify-content-center py-5">
                      <div className="spinner-border text-primary me-3" role="status">
                        <span className="visually-hidden">Loading...</span>
                      </div>
                      <span className="text-muted">Fetching invoice preview…</span>
                    </div>
                  ) : (
                    <div className="row g-4">
                      <div className="col-lg-8">
                        {previewBill.pdfUrl ? (
                          <iframe
                            title="Bill PDF preview"
                            src={previewBill.pdfUrl}
                            className="w-100 border rounded"
                            style={{ minHeight: '500px' }}
                          />
                        ) : (
                          <div className="alert alert-warning">
                            PDF preview is not available for this bill.
                          </div>
                        )}
                      </div>
                      <div className="col-lg-4">
                        <div className="mb-4">
                          <h6 className="text-uppercase text-muted small">Summary</h6>
                          <ul className="list-unstyled small mb-0">
                            <li><strong>Bill Month:</strong> {previewBill.bill?.billMonth || '—'}</li>
                            <li><strong>Bill Date:</strong> {formatDate(previewBill.bill?.billDate || previewBill.listItem?.billDate)}</li>
                            <li><strong>Due Date:</strong> {formatDate(previewBill.bill?.dueDate || previewBill.listItem?.dueDate)}</li>
                            <li><strong>Units Consumed:</strong> {previewBill.bill?.unitsConsumed ?? previewBill.listItem?.unitsConsumed ?? '—'}</li>
                            <li><strong>Net Payable:</strong> {formatCurrency(previewBill.bill?.netPayable ?? previewBill.listItem?.netPayable)}</li>
                            <li><strong>Amount Paid:</strong> {formatCurrency(previewBill.bill?.amountPaid)}</li>
                            <li><strong>Balance:</strong> {formatCurrency(previewBill.bill?.balanceAmount ?? previewBill.listItem?.balanceAmount)}</li>
                            <li><strong>Status:</strong> {previewBill.bill?.status || previewBill.listItem?.billStatus}</li>
                          </ul>
                        </div>
                        <div className="p-3 border rounded">
                          <div className="d-flex align-items-center mb-2">
                            <FaQrcode className="me-2 text-primary" />
                            <h6 className="mb-0">Scan &amp; Pay</h6>
                          </div>
                          {previewBill.qrUrl ? (
                            <div className="text-center">
                              <img
                                src={previewBill.qrUrl}
                                alt="Bill payment QR code"
                                className="img-fluid border rounded"
                              />
                              <p className="text-muted small mt-2 mb-0">
                                Scan in your UPI app to pay instantly.
                              </p>
                            </div>
                          ) : (
                            <p className="text-muted small mb-0">
                              QR code isn&apos;t available for this invoice. You can still download the PDF to pay manually.
                            </p>
                          )}
                        </div>
                      </div>
                    </div>
                  )}
                </div>
                <div className="modal-footer">
                  <button type="button" className="btn btn-secondary" onClick={closePreview}>
                    Close
                  </button>
                  <button
                    type="button"
                    className="btn btn-primary"
                    onClick={() => (previewBill.bill || previewBill.listItem) && downloadBill(previewBill.bill || previewBill.listItem)}
                    disabled={previewLoading}
                  >
                    <FaDownload className="me-2" /> Download PDF
                  </button>
                </div>
              </div>
            </div>
          </div>
          <div className="modal-backdrop fade show"></div>
        </>
      )}
    </div>
  );
};

export default ViewBills;
