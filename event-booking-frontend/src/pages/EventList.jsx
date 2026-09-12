import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import api from '../services/api';

const EventList = () => {
  const [events, setEvents] = useState([]);
  const [loading, setLoading] = useState(true);
  const [city, setCity] = useState('');
  const [keyword, setKeyword] = useState('');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);

  const fetchEvents = async () => {
    setLoading(true);
    try {
      const response = await api.get('/events', {
        params: { city, keyword, page, size: 6 }
      });
      setEvents(response.data.content);
      setTotalPages(response.data.totalPages);
    } catch (err) {
      console.error("Failed to load events:", err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchEvents();
  }, [city, keyword, page]);

  return (
    <div className="page-container">
      <header className="hero-banner">
        <h1>Discover & Book Premium Events</h1>
        <p>Real-time seat mapping with zero double-booking guarantees</p>

        <div className="search-bar">
          <input
            type="text"
            placeholder="Search events by name or keyword..."
            value={keyword}
            onChange={(e) => { setKeyword(e.target.value); setPage(0); }}
          />
          <input
            type="text"
            placeholder="Filter by city/venue..."
            value={city}
            onChange={(e) => { setCity(e.target.value); setPage(0); }}
          />
        </div>
      </header>

      <main className="events-grid-section">
        {loading ? (
          <div className="spinner-container"><div className="spinner"></div></div>
        ) : events.length === 0 ? (
          <div className="no-data">No upcoming events match your search criteria.</div>
        ) : (
          <div className="events-grid">
            {events.map((evt) => (
              <div className="event-card" key={evt.id}>
                <div className="event-card-header">
                  <span className="event-date">{new Date(evt.eventDate).toLocaleDateString(undefined, { weekday: 'short', month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' })}</span>
                  <span className="price-tag">${evt.basePrice?.toFixed(2)}</span>
                </div>
                <div className="event-card-body">
                  <h3>{evt.name}</h3>
                  <p className="description">{evt.description}</p>
                  <div className="venue-info">
                    <span>📍 {evt.venue?.name}</span>
                    <span className="address">{evt.venue?.address}</span>
                  </div>
                </div>
                <div className="event-card-footer">
                  <div className="seat-badge">
                    <span className="dot green"></span>
                    <strong>{evt.availableSeats}</strong> / {evt.totalSeats} seats available
                  </div>
                  <Link to={`/events/${evt.id}`} className="btn btn-primary btn-sm">
                    Select Seats
                  </Link>
                </div>
              </div>
            ))}
          </div>
        )}

        {totalPages > 1 && (
          <div className="pagination">
            <button className="btn btn-outline" disabled={page === 0} onClick={() => setPage(p => p - 1)}>Previous</button>
            <span className="page-num">Page {page + 1} of {totalPages}</span>
            <button className="btn btn-outline" disabled={page >= totalPages - 1} onClick={() => setPage(p => p + 1)}>Next</button>
          </div>
        )}
      </main>
    </div>
  );
};

export default EventList;
