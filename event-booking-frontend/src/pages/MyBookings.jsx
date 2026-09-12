import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import api from '../services/api';

const MyBookings = () => {
  const [bookings, setBookings] = useState([]);
  const [loading, setLoading] = useState(true);
  const [cancellingId, setCancellingId] = useState(null);

  const fetchBookings = async () => {
    try {
      const response = await api.get('/bookings/my');
      setBookings(response.data);
    } catch (err) {
      console.error("Failed to load user bookings:", err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchBookings();
  }, []);

  const handleCancel = async (bookingId) => {
    if (!window.confirm("Are you sure you want to cancel this booking? Released seats will become available to other users.")) return;

    setCancellingId(bookingId);
    try {
      await api.post(`/bookings/${bookingId}/cancel?reason=User+requested+cancellation`);
      fetchBookings();
    } catch (err) {
      alert(err.response?.data?.message || "Failed to cancel booking.");
    } finally {
      setCancellingId(null);
    }
  };

  if (loading) {
    return <div className="spinner-container"><div className="spinner"></div></div>;
  }

  return (
    <div className="page-container">
      <div className="page-header">
        <h2>My Tickets & Reservations</h2>
        <p>Manage active seat holds, confirmed tickets, and past bookings</p>
      </div>

      {bookings.length === 0 ? (
        <div className="empty-state">
          <span className="icon">🎟️</span>
          <h3>No Bookings Found</h3>
          <p>You haven't reserved or purchased any event tickets yet.</p>
          <Link to="/" className="btn btn-primary">Browse Events</Link>
        </div>
      ) : (
        <div className="bookings-list">
          {bookings.map((b) => (
            <div className="booking-card" key={b.id}>
              <div className="booking-card-header">
                <div>
                  <span className="booking-id">Order #{b.id}</span>
                  <h3>{b.eventName}</h3>
                </div>
                <span className={`status-badge ${b.status?.toLowerCase()}`}>{b.status}</span>
              </div>

              <div className="booking-card-body">
                <div className="seats-preview">
                  <strong>Seats ({b.seats?.length}):</strong>
                  <div className="seat-tags">
                    {b.seats?.map((s) => (
                      <span key={s.eventSeatId} className="seat-tag">
                        {s.section} - {s.seatLabel}
                      </span>
                    ))}
                  </div>
                </div>

                <div className="booking-meta">
                  <div className="meta-item">
                    <span>Total Amount</span>
                    <strong>${b.totalAmount?.toFixed(2)}</strong>
                  </div>
                  {b.status === 'HELD' && b.holdExpiresAt && (
                    <div className="meta-item warning">
                      <span>Hold Expires</span>
                      <strong>{new Date(b.holdExpiresAt).toLocaleTimeString()}</strong>
                    </div>
                  )}
                </div>
              </div>

              <div className="booking-card-footer">
                {b.status === 'HELD' && (
                  <Link to={`/bookings/${b.id}/confirm`} className="btn btn-primary btn-sm">
                    Complete Payment
                  </Link>
                )}

                {(b.status === 'HELD' || b.status === 'CONFIRMED') && (
                  <button
                    className="btn btn-danger-outline btn-sm"
                    disabled={cancellingId === b.id}
                    onClick={() => handleCancel(b.id)}
                  >
                    {cancellingId === b.id ? 'Cancelling...' : 'Cancel Booking'}
                  </button>
                )}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};

export default MyBookings;
