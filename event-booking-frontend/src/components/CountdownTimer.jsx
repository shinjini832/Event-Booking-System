import React, { useState, useEffect } from 'react';

const CountdownTimer = ({ targetTime, onExpire }) => {
  const [timeLeft, setTimeLeft] = useState(0);

  useEffect(() => {
    const calculateTimeLeft = () => {
      const difference = new Date(targetTime).getTime() - new Date().getTime();
      if (difference <= 0) {
        setTimeLeft(0);
        if (onExpire) onExpire();
        return 0;
      }
      return Math.floor(difference / 1000);
    };

    setTimeLeft(calculateTimeLeft());

    const timer = setInterval(() => {
      const remaining = calculateTimeLeft();
      if (remaining <= 0) {
        clearInterval(timer);
      }
    }, 1000);

    return () => clearInterval(timer);
  }, [targetTime, onExpire]);

  const minutes = Math.floor(timeLeft / 60);
  const seconds = timeLeft % 60;
  const formattedTime = `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`;

  const isWarning = timeLeft < 120; // under 2 minutes

  return (
    <div className={`countdown-box ${isWarning ? 'warning' : ''}`}>
      <span className="timer-icon">⏳</span>
      <div className="timer-info">
        <span className="timer-label">Hold Expires In</span>
        <span className="timer-value">{formattedTime}</span>
      </div>
    </div>
  );
};

export default CountdownTimer;
