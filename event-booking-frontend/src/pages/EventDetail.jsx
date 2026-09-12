import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import api from '../services/api';

const EventDetail = () => {
  const { id } = useParams();
  const navigate = useNavigate();

  const [event, setEvent] = useState(null);
  const [seats, setSeats] = useState([]);
  const [selectedSeatIds, setSelectedSeatIds] = useState([]);
  const [loading, setLoading] = useState(true);
  const [holding, setHolding] = useState(false);
  const [toastMessage, setToastMessage] = useState(null);

  const fetchEventAndSeats = async () => {
    try {
      const [evtRes, seatsRes] = await Promise.all([
        api.get(`/events/${id}`),
        api.get(`/events/${id}/seats`)
      ]);
      setEvent(evtRes.data);
      setSeats(seatsRes.data);
    } catch (err) {
      console.error("Error fetching event details:", err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchEventAndSeats();

    // Step 19: Poll seat map every 5 seconds while user is on seat selection page
    const pollInterval = setInterval(() => {
      api.get(`/events/${id}/seats`)
        .then(res => setSeats(res.data))
        .catch(err => console.error("Seat poll error:", err));
    }, 5000);

    return () => clearInterval(pollInterval);
  }, [id]);

  const toggleSeatSelection = (seat) => {
    if (seat.status !== 'AVAILABLE') return;

    // Step 20: Optimistic UI update
    if (selectedSeatIds.includes(seat.eventSeatId)) {
      setSelectedSeatIds(selectedSeatIds.filter(sId => sId !== seat.eventSeatId));
    } else {
      setSelectedSeatIds([...selectedSeatIds, seat.eventSeatId]);
    }
  };

  const handleHoldSeats = async () => {
    if (selectedSeatIds.length === 0) return;

    setHolding(true);
    setToastMessage(null);

    const idempotencyKey = `hold-key-${Date.now()}-${Math.random().toString(36).substring(2, 9)}`;

    try {
      const response = await api.post(
        '/bookings/hold',
        { eventId: Number(id), seatIds: selectedSeatIds },
        { headers: { 'Idempotency-Key': idempotencyKey } }
      );

      // Successfully held! Navigate to confirmation checkout page
      navigate(`/bookings/${response.data.id}/confirm`);
    } catch (err) {
      console.error("Hold failed:", err);
      // Step 20 Rollback on 409 conflict
      const message = err.response?.data?.message || "One or more seats were just taken. Please choose different seats.";
      setToastMessage({ type: 'error', text: message });

      // Clear optimistic selection & refetch seat map
      setSelectedSeatIds([]);
      fetchEventAndSeats();
    } finally {
      setHolding(false);
    }
  };

  if (loading || !event) {
    return <div className="spinner-container"><div className="spinner"></div></div>;
  }

  // Calculate total price of selected seats
  const selectedSeatsList = seats.filter(s => selectedSeatIds.includes(s.eventSeatId));
  const totalPrice = selectedSeatsList.reduce((sum, s) => sum + (s.price || 0), 0);

  return (
    <div className="page-container">
      {toastMessage && (
        <div className={`toast toast-${toastMessage.type}`}>
          <span>⚠️ {toastMessage.text}</span>
          <button onClick={() => setToastMessage(null)}>✕</button>
        </div>
      )}

      <div className="event-detail-header">
        <div>
          <h2>{event.name}</h2>
          <p className="subtext">📍 {event.venue?.name} — {event.venue?.address}</p>
        </div>
        <div className="event-detail-meta">
          <span className="date-badge">🗓️ {new Date(event.eventDate).toLocaleDateString(undefined, { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric', hour: '2-digit', minute: '2-digit' })}</span>
        </div>
      </div>

      <div className="seat-booking-wrapper">
        <div className="seat-map-section">
          <div className="stage-banner">
            <span>🎭 STAGE / SCREEN</span>
          </div>

          <div className="seat-legend">
            <div className="legend-item"><span className="seat-sample available"></span> Available</div>
            <div className="legend-item"><span className="seat-sample selected"></span> Selected</div>
            <div className="legend-item"><span className="seat-sample held"></span> Reserved (Held)</div>
            <div className="legend-item"><span className="seat-sample booked"></span> Booked</div>
          </div>

          <div className="seat-grid">
            {seats.map((seat) => {
              const isSelected = selectedSeatIds.includes(seat.eventSeatId);
              let seatClass = 'seat';

              if (isSelected) {
                seatClass += ' selected';
              } else if (seat.status === 'AVAILABLE') {
                seatClass += ' available';
              } else if (seat.status === 'HELD') {
                seatClass += ' held';
              } else if (seat.status === 'BOOKED') {
                seatClass += ' booked';
              }

              return (
                <button
                  key={seat.eventSeatId}
                  className={seatClass}
                  disabled={seat.status !== 'AVAILABLE'}
                  onClick={() => toggleSeatSelection(seat)}
                  title={`${seat.section} — Seat ${seat.seatLabel} ($${seat.price?.toFixed(2)})`}
                >
                  <span className="seat-label">{seat.seatLabel}</span>
                  <span className="seat-price">${seat.price?.toFixed(0)}</span>
                </button>
              );
            })}
          </div>
        </div>

        <div className="booking-summary-sidebar">
          <div className="summary-card">
            <h3>Selected Seats</h3>
            {selectedSeatsList.length === 0 ? (
              <p className="empty-selection">Click on available seats to add them to your order.</p>
            ) : (
              <ul className="selected-seats-list">
                {selectedSeatsList.map(s => (
                  <li key={s.eventSeatId}>
                    <span>{s.section} — {s.seatLabel}</span>
                    <strong>${s.price?.toFixed(2)}</strong>
                  </li>
                ))}
              </ul>
            )}

            <div className="summary-divider"></div>

            <div className="total-row">
              <span>Total Price:</span>
              <span className="total-amount">${totalPrice.toFixed(2)}</span>
            </div>

            <button
              className="btn btn-primary btn-block btn-lg"
              disabled={selectedSeatIds.length === 0 || holding}
              onClick={handleHoldSeats}
            >
              {holding ? 'Locking Seats...' : `Hold ${selectedSeatIds.length} Seat(s)`}
            </button>
            <p className="hold-hint">💡 Seats are locked for 10 minutes once reserved.</p>
          </div>
        </div>
      </div>
    </div>
  );
};

export default EventDetail;
