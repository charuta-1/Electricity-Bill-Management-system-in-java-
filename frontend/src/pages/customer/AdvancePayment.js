import React, { useState, useEffect } from 'react';
import { BsCheckCircleFill, BsXCircleFill, BsWallet2 } from 'react-icons/bs';
import api from '../../api/axiosConfig';

function AdvancePayment() {
  const [amount, setAmount] = useState('');
  const [currentBalance, setCurrentBalance] = useState(0);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');

  useEffect(() => {
    fetchAdvanceBalance();
  }, []);

  const fetchAdvanceBalance = async () => {
    try {
      setLoading(true);
      const response = await api.get('/customer/advance-payment');
      setCurrentBalance(response.data.balance || 0);
    } catch (err) {
      console.error('Error fetching advance payment:', err);
      setError('Failed to load advance payment balance');
    } finally {
      setLoading(false);
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setMessage('');
    setError('');

    const numericAmount = parseFloat(amount);
    if (isNaN(numericAmount) || numericAmount <= 0) {
      setError('Please enter a valid amount greater than zero');
      return;
    }

    try {
      setSubmitting(true);
      const response = await api.post('/customer/advance-payment', {
        amount: numericAmount
      });
      
      setMessage(response.data.message || 'Advance payment added successfully');
      setCurrentBalance(response.data.balance || 0);
      setAmount('');
      
      // Refresh balance after a short delay
      setTimeout(() => {
        fetchAdvanceBalance();
      }, 1000);
    } catch (err) {
      console.error('Error adding advance payment:', err);
      const errorMsg = err.response?.data?.message || err.response?.data || 'Failed to add advance payment';
      setError(typeof errorMsg === 'string' ? errorMsg : 'An error occurred');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="container-fluid py-4">
      <div className="row">
        <div className="col-12">
          <h2 className="mb-3">
            <BsWallet2 className="me-2" size={32} style={{ verticalAlign: 'middle' }} />
            Advance Payment
          </h2>
          <p className="text-muted">Add funds to your account for future bill payments</p>
        </div>
      </div>

      <div className="row mt-4">
        {/* Current Balance Card */}
        <div className="col-md-6 mb-4">
          <div className="card border-0 shadow-sm h-100">
            <div className="card-body">
              <h5 className="card-title text-muted mb-3">Current Advance Balance</h5>
              {loading ? (
                <div className="text-center py-4">
                  <div className="spinner-border text-primary" role="status">
                    <span className="visually-hidden">Loading...</span>
                  </div>
                </div>
              ) : (
                <div className="text-center py-3">
                  <h1 className="display-4 text-success mb-0">
                    ₹{currentBalance.toFixed(2)}
                  </h1>
                  <p className="text-muted mt-2">Available Credit</p>
                </div>
              )}
              <div className="alert alert-info mt-3" role="alert">
                <small>
                  <strong>Note:</strong> Your advance payment will be automatically applied to future bills,
                  reducing your net payable amount.
                </small>
              </div>
            </div>
          </div>
        </div>

        {/* Add Advance Payment Form */}
        <div className="col-md-6 mb-4">
          <div className="card border-0 shadow-sm h-100">
            <div className="card-body">
              <h5 className="card-title mb-4">Add Advance Payment</h5>
              
              {message && (
                <div className="alert alert-success d-flex align-items-center" role="alert">
                  <BsCheckCircleFill className="me-2" size={20} />
                  <div>{message}</div>
                </div>
              )}

              {error && (
                <div className="alert alert-danger d-flex align-items-center" role="alert">
                  <BsXCircleFill className="me-2" size={20} />
                  <div>{error}</div>
                </div>
              )}

              <form onSubmit={handleSubmit}>
                <div className="mb-4">
                  <label htmlFor="advance-amount" className="form-label">
                    Amount to Add (₹)
                  </label>
                  <input
                    type="number"
                    className="form-control form-control-lg"
                    id="advance-amount"
                    placeholder="Enter amount"
                    value={amount}
                    onChange={(e) => setAmount(e.target.value)}
                    min="1"
                    step="0.01"
                    required
                    disabled={submitting}
                  />
                  <div className="form-text">
                    Minimum amount: ₹1.00
                  </div>
                </div>

                {/* Quick Amount Buttons */}
                <div className="mb-4">
                  <label className="form-label">Quick Select</label>
                  <div className="d-flex flex-wrap gap-2">
                    {[500, 1000, 2000, 5000].map((value) => (
                      <button
                        key={value}
                        type="button"
                        className="btn btn-outline-primary btn-sm"
                        onClick={() => setAmount(value.toString())}
                        disabled={submitting}
                      >
                        ₹{value}
                      </button>
                    ))}
                  </div>
                </div>

                <button
                  type="submit"
                  className="btn btn-primary btn-lg w-100"
                  disabled={submitting || !amount}
                >
                  {submitting ? (
                    <>
                      <span className="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>
                      Processing...
                    </>
                  ) : (
                    <>
                      <BsWallet2 className="me-2" size={20} style={{ verticalAlign: 'middle' }} />
                      Add ₹{amount || '0.00'}
                    </>
                  )}
                </button>
              </form>

              <div className="mt-4 pt-3 border-top">
                <h6 className="text-muted mb-2">Benefits of Advance Payment:</h6>
                <ul className="list-unstyled small text-muted">
                  <li className="mb-1">✓ Automatic deduction from future bills</li>
                  <li className="mb-1">✓ No late payment charges</li>
                  <li className="mb-1">✓ Peace of mind with prepaid credit</li>
                  <li className="mb-1">✓ Reflected in your bill PDF</li>
                </ul>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* How It Works */}
      <div className="row mt-4">
        <div className="col-12">
          <div className="card border-0 shadow-sm">
            <div className="card-body">
              <h5 className="card-title mb-3">How Advance Payment Works</h5>
              <div className="row">
                <div className="col-md-4 mb-3">
                  <div className="d-flex align-items-start">
                    <div className="badge bg-primary rounded-circle p-2 me-3">1</div>
                    <div>
                      <h6>Add Funds</h6>
                      <p className="text-muted small mb-0">
                        Add any amount to your advance payment balance
                      </p>
                    </div>
                  </div>
                </div>
                <div className="col-md-4 mb-3">
                  <div className="d-flex align-items-start">
                    <div className="badge bg-primary rounded-circle p-2 me-3">2</div>
                    <div>
                      <h6>Auto-Applied</h6>
                      <p className="text-muted small mb-0">
                        Your advance payment is automatically applied to new bills
                      </p>
                    </div>
                  </div>
                </div>
                <div className="col-md-4 mb-3">
                  <div className="d-flex align-items-start">
                    <div className="badge bg-primary rounded-circle p-2 me-3">3</div>
                    <div>
                      <h6>Reduced Bills</h6>
                      <p className="text-muted small mb-0">
                        Pay less or nothing if your advance covers the full amount
                      </p>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

export default AdvancePayment;
