import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import api from '../services/api';
import CountdownTimer from '../components/CountdownTimer';

const BookingConfirmation = () => {
  const { id } = useParams();
  const navigate = useNavigate();

  const [booking, setBooking] = useState(null);
  const [loading, setLoading] = useState(true);
  const [confirming, setConfirming] = useState(false);
  const [error, setError] = useState('');
  const [paymentToken, setPaymentToken] = useState('TOK_SIMULATED_SUCCESS_9921');

  const fetchBooking = async () => {
    try {
      const response = await api.get(`/bookings/${id}`);
      setBooking(response.data);
    } catch (err) {
      console.error("Failed to load booking:", err);
      setError("Unable to retrieve booking details.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchBooking();
  }, [id]);

  const handleConfirmPayment = async () => {
    setConfirming(true);
    setError('');

    try {
      const response = await api.post(`/bookings/${id}/confirm`, { paymentToken });
      setBooking(response.data);
    } catch (err) {
      console.error("Payment confirmation failed:", err);
      setError(err.response?.data?.message || 'Payment confirmation failed.');
    } finally {
      setConfirming(false);
    }
  };

  const handleHoldExpired = () => {
    setError("Your 10-minute hold period has expired. Returning to event selection...");
    setTimeout(() => {
      navigate('/');
    }, 3000);
  };

  if (loading) {
    return <div className="spinner-container"><div className="spinner"></div></div>;
  }

  if (!booking) {
    return <div className="page-container"><div className="alert alert-error">{error || 'Booking not found'}</div></div>;
  }

  const isConfirmed = booking.status === 'CONFIRMED';

  return (
    <div className="page-container">
      <div className="checkout-card">
        <div className="checkout-header">
          {isConfirmed ? (
            <div className="success-header">
              <span className="icon">🎉</span>
              <h2>Booking Confirmed!</h2>
              <p>Your tickets have been secured and sent to your email.</p>
            </div>
          ) : (
            <div className="pending-header">
              <h2>Complete Your Order</h2>
              <p>Review details and process payment before hold expires</p>
            </div>
          )}
        </div>

        {!isConfirmed && booking.holdExpiresAt && (
          <CountdownTimer targetTime={booking.holdExpiresAt} onExpire={handleHoldExpired} />
        )}

        {error && <div className="alert alert-error">{error}</div>}

        <div className="booking-details-box">
          <div className="detail-row">
            <span>Booking Reference:</span>
            <strong>#{booking.id}</strong>
          </div>
          <div className="detail-row">
            <span>Event:</span>
            <strong>{booking.eventName}</strong>
          </div>
          <div className="detail-row">
            <span>Status:</span>
            <span className={`status-badge ${booking.status?.toLowerCase()}`}>{booking.status}</span>
          </div>

          <div className="summary-divider"></div>

          <h4>Reserved Seats</h4>
          <ul className="ticket-seats-list">
            {booking.seats?.map((seat) => (
              <li key={seat.eventSeatId}>
                <span>{seat.section} — Seat {seat.seatLabel}</span>
                <strong>${seat.price?.toFixed(2)}</strong>
              </li>
            ))}
          </ul>

          <div className="summary-divider"></div>

          <div className="detail-row total">
            <span>Total Amount Due:</span>
            <strong className="amount">${booking.totalAmount?.toFixed(2)}</strong>
          </div>
        </div>

        {!isConfirmed && (
          <div className="payment-simulation-box">
            <h4>Simulated Payment Gateway</h4>
            <div className="form-group">
              <label>Payment Token Strategy</label>
              <select value={paymentToken} onChange={(e) => setPaymentToken(e.target.value)}>
                <option value="TOK_SIMULATED_SUCCESS_9921">Success Token (Instant Confirmation)</option>
                <option value="FAIL_PAYMENT">Fail Payment Token (Test Error Handling)</option>
              </select>
            </div>

            <button
              className="btn btn-primary btn-block btn-lg"
              disabled={confirming || booking.status !== 'HELD'}
              onClick={handleConfirmPayment}
            >
              {confirming ? 'Processing Payment...' : `Pay $${booking.totalAmount?.toFixed(2)} Now`}
            </button>
          </div>
        )}

        {isConfirmed && (
          <div className="confirmed-actions">
            <button className="btn btn-primary" onClick={() => navigate('/my-bookings')}>View My Tickets</button>
            <button className="btn btn-outline" onClick={() => navigate('/')}>Book More Events</button>
          </div>
        )}
      </div>
    </div>
  );
};

export default BookingConfirmation;
