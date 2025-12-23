import { useCallback, useEffect, useMemo, useState } from 'react';
import { FaQrcode } from 'react-icons/fa';
import api from '../../api/axiosConfig.js';

const PayBill = () => {
  const [pendingBills, setPendingBills] = useState([]);
  const [selectedBillId, setSelectedBillId] = useState('');
  const [paymentMethod, setPaymentMethod] = useState('UPI');
  const [amount, setAmount] = useState('');
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const [selectedBillDetail, setSelectedBillDetail] = useState(null);
  const [loadingBill, setLoadingBill] = useState(false);
  const [qrPreviewUrl, setQrPreviewUrl] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const clearQrPreview = useCallback(() => {
    setQrPreviewUrl((prev) => {
      if (prev) {
        window.URL.revokeObjectURL(prev);
      }
      return '';
    });
  }, []);

  const loadPendingBills = useCallback(async () => {
    try {
      const response = await api.get('/customers/self/bills/pending');
      setPendingBills(response.data || []);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to load pending bills');
    }
  }, []);

  useEffect(() => {
    loadPendingBills();
  }, [loadPendingBills]);

  useEffect(() => () => clearQrPreview(), [clearQrPreview]);

  useEffect(() => {
    const fetchBillDetail = async () => {
      if (!selectedBillId) {
        setSelectedBillDetail(null);
        setAmount('');
        clearQrPreview();
        return;
      }

      setLoadingBill(true);
      setError('');

      try {
        const billId = Number(selectedBillId);
        const [detailResponse, qrResponse] = await Promise.all([
          api.get(`/customer/portal/bills/${billId}`),
          api
            .get(`/customers/self/bills/${billId}/qr`, { responseType: 'blob' })
            .catch((qrErr) => {
              if (qrErr?.response?.status === 404) {
                return null;
              }
              throw qrErr;
            })
        ]);

        const detail = detailResponse.data;
        setSelectedBillDetail(detail);

        const suggested = detail.balanceAmount ?? detail.netPayable ?? '';
        const normalizedAmount =
          suggested !== '' && suggested !== null && suggested !== undefined
            ? Number(suggested).toFixed(2)
            : '';
        setAmount(normalizedAmount);

        clearQrPreview();

        if (qrResponse) {
          const qrUrl = window.URL.createObjectURL(
            new Blob([qrResponse.data], { type: 'image/png' })
          );
          setQrPreviewUrl(qrUrl);
        }
      } catch (err) {
        setError(err.response?.data?.message || 'Failed to load bill details');
        setSelectedBillDetail(null);
        clearQrPreview();
      } finally {
        setLoadingBill(false);
      }
    };

    fetchBillDetail();
  }, [selectedBillId, clearQrPreview]);

  const pendingOptions = useMemo(
    () =>
      pendingBills.map((bill) => ({
        id: bill.billId,
        label: `${bill.invoiceNumber} · Due ${new Date(bill.dueDate).toLocaleDateString()} · ₹${(
          bill.balanceAmount ?? bill.netPayable ?? 0
        ).toLocaleString()}`
      })),
    [pendingBills]
  );

  const handleSubmit = async (event) => {
    event.preventDefault();
    setMessage('');
    setError('');

    if (!selectedBillId) {
      setError('Please choose a bill to pay.');
      return;
    }

    const numericAmount = Number(amount || 0);
    if (!numericAmount || Number.isNaN(numericAmount) || numericAmount <= 0) {
      setError('Enter a payment amount greater than zero.');
      return;
    }

    setSubmitting(true);
    try {
      await api.post('/payments', {
        billId: Number(selectedBillId),
        paymentAmount: numericAmount,
        paymentMode: paymentMethod
      });

      setMessage('Payment recorded successfully. You will receive a confirmation email shortly.');
      setSelectedBillId('');
      setSelectedBillDetail(null);
      setAmount('');
      clearQrPreview();

      await loadPendingBills();
    } catch (err) {
      const data = err.response?.data;
      setError(typeof data === 'string' ? data : data?.message || 'Payment failed');
    } finally {
      setSubmitting(false);
    }
  };

  const renderAmountSummary = () => {
    if (!selectedBillDetail) {
      return null;
    }

    const amountValue = selectedBillDetail.balanceAmount ?? selectedBillDetail.netPayable ?? 0;
    return amountValue.toLocaleString();
  };

  return (
    <div className="container py-4">
      <h1 className="h4 mb-3">Pay Bill</h1>
      <p className="text-muted">Settle your outstanding bills using UPI, cash, or cheque payments.</p>

      {message && <div className="alert alert-success">{message}</div>}
      {error && <div className="alert alert-danger">{error}</div>}

      <div className="row g-4">
        <div className="col-lg-6">
          <div className="card card-shadow h-100">
            <div className="card-body">
              <form className="row g-3" onSubmit={handleSubmit}>
                <div className="col-12">
                  <label htmlFor="pending-bill" className="form-label">Pending Bill</label>
                  <select
                    id="pending-bill"
                    className="form-select"
                    value={selectedBillId}
                    onChange={(event) => setSelectedBillId(event.target.value)}
                    required
                  >
                    <option value="">Select bill</option>
                    {pendingOptions.map((option) => (
                      <option key={option.id} value={option.id}>
                        {option.label}
                      </option>
                    ))}
                  </select>
                </div>

                <div className="col-md-6">
                  <label htmlFor="payment-method" className="form-label">Payment Method</label>
                  <select
                    id="payment-method"
                    className="form-select"
                    value={paymentMethod}
                    onChange={(event) => setPaymentMethod(event.target.value)}
                  >
                    <option value="UPI">UPI</option>
                    <option value="CASH">Cash</option>
                    <option value="CHEQUE">Cheque</option>
                  </select>
                </div>

                <div className="col-md-6">
                  <label htmlFor="payment-amount" className="form-label">Amount</label>
                  <input
                    id="payment-amount"
                    type="number"
                    className="form-control"
                    value={amount}
                    onChange={(event) => setAmount(event.target.value)}
                    min="0"
                    step="0.01"
                    required
                  />
                </div>

                <div className="col-12 text-end">
                  <button
                    className="btn btn-primary"
                    type="submit"
                    disabled={submitting || !selectedBillId || !amount || Number(amount) <= 0}
                  >
                    {submitting ? 'Processing…' : 'Submit Payment'}
                  </button>
                </div>
              </form>
            </div>
          </div>
        </div>

        {selectedBillDetail && (
          <div className="col-lg-6">
            <div className="card card-shadow h-100 text-center">
              <div className="card-body d-flex flex-column justify-content-center align-items-center">
                <FaQrcode size={48} className="text-primary mb-3" />
                <p className="text-muted mb-3">Scan this QR with any UPI app to pay instantly.</p>
                {loadingBill ? (
                  <div className="text-muted">Loading payment QR…</div>
                ) : qrPreviewUrl ? (
                  <img
                    src={qrPreviewUrl}
                    alt="UPI QR"
                    className="img-fluid"
                    style={{ maxWidth: '240px' }}
                  />
                ) : (
                  <div className="alert alert-warning w-100">
                    QR code isn&apos;t available for this bill. You can still pay using the invoice reference.
                  </div>
                )}
                <p className="small text-muted mt-3">
                  Invoice: {selectedBillDetail.invoiceNumber} · Amount: ₹{renderAmountSummary()}
                </p>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

export default PayBill;
