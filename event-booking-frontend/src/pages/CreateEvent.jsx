import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../services/api';
import { useAuth } from '../context/AuthContext';

const CreateEvent = () => {
  const { user } = useAuth();
  const navigate = useNavigate();

  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [venueId, setVenueId] = useState('');
  const [eventDate, setEventDate] = useState('');
  const [basePrice, setBasePrice] = useState('100.00');

  const [venues, setVenues] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    const fetchVenues = async () => {
      try {
        const response = await api.get('/events/venues');
        setVenues(response.data);
        if (response.data.length > 0) {
          setVenueId(response.data[0].id);
        }
      } catch (err) {
        console.error("Failed to load venues:", err);
      }
    };
    fetchVenues();
  }, []);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      const isoDate = new Date(eventDate).toISOString();
      await api.post('/events', {
        name,
        description,
        venueId: Number(venueId),
        eventDate: isoDate,
        basePrice: parseFloat(basePrice)
      });

      navigate('/');
    } catch (err) {
      console.error("Create event error:", err);
      setError(err.response?.data?.message || 'Failed to create event.');
    } finally {
      setLoading(false);
    }
  };

  if (user && user.role !== 'ORGANIZER' && user.role !== 'ADMIN') {
    return (
      <div className="page-container">
        <div className="alert alert-error">
          Access Denied: Only Event Organizers or Administrators can publish new events.
        </div>
      </div>
    );
  }

  return (
    <div className="page-container">
      <div className="auth-card" style={{ maxWidth: '600px', margin: '0 auto' }}>
        <div className="auth-header">
          <h2>Create New Event</h2>
          <p>Publish an event and generate interactive venue seat maps automatically</p>
        </div>

        {error && <div className="alert alert-error">{error}</div>}

        <form onSubmit={handleSubmit} className="auth-form">
          <div className="form-group">
            <label>Event Name</label>
            <input
              type="text"
              required
              placeholder="e.g., World Tech Expo 2026"
              value={name}
              onChange={(e) => setName(e.target.value)}
            />
          </div>

          <div className="form-group">
            <label>Description</label>
            <textarea
              rows={4}
              placeholder="Describe the event, artists, keynotes..."
              style={{
                width: '100%',
                padding: '0.8rem 1rem',
                borderRadius: '10px',
                border: '1px solid var(--border-color)',
                background: 'rgba(9, 13, 22, 0.6)',
                color: 'var(--text-primary)',
                fontFamily: 'inherit'
              }}
              value={description}
              onChange={(e) => setDescription(e.target.value)}
            />
          </div>

          <div className="form-group">
            <label>Venue</label>
            <select value={venueId} onChange={(e) => setVenueId(e.target.value)}>
              {venues.map(v => (
                <option key={v.id} value={v.id}>
                  {v.name} ({v.address} — {v.totalCapacity} seats)
                </option>
              ))}
            </select>
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
            <div className="form-group">
              <label>Date & Time</label>
              <input
                type="datetime-local"
                required
                value={eventDate}
                onChange={(e) => setEventDate(e.target.value)}
              />
            </div>

            <div className="form-group">
              <label>Base Price ($)</label>
              <input
                type="number"
                step="0.01"
                min="0"
                required
                value={basePrice}
                onChange={(e) => setBasePrice(e.target.value)}
              />
            </div>
          </div>

          <button type="submit" className="btn btn-primary btn-block btn-lg" disabled={loading}>
            {loading ? 'Publishing Event...' : 'Publish Event'}
          </button>
        </form>
      </div>
    </div>
  );
};

export default CreateEvent;
