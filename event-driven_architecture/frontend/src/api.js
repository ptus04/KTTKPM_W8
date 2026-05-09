const base = '';

async function request(path, options = {}) {
  const headers = { ...options.headers };
  if (options.body && !headers['Content-Type']) {
    headers['Content-Type'] = 'application/json';
  }
  const res = await fetch(`${base}${path}`, { ...options, headers });
  const text = await res.text();
  let data;
  try {
    data = text ? JSON.parse(text) : null;
  } catch {
    data = text;
  }
  if (!res.ok) {
    const err = new Error((data && data.error) || res.statusText || 'Request failed');
    err.status = res.status;
    err.data = data;
    throw err;
  }
  return data;
}

export function register(username, password) {
  return request('/api/users/register', {
    method: 'POST',
    body: JSON.stringify({ username, password }),
  });
}

export function login(username, password) {
  return request('/api/users/login', {
    method: 'POST',
    body: JSON.stringify({ username, password }),
  });
}

export function fetchMovies() {
  return request('/api/movies');
}

export function createBooking(token, movieId, seats) {
  const normalizedSeats = Array.isArray(seats)
    ? seats.map((s) => String(s || '').trim()).filter(Boolean)
    : [];
  return request('/api/bookings', {
    method: 'POST',
    headers: { Authorization: `Bearer ${token}` },
    body: JSON.stringify({
      movieId,
      seats: normalizedSeats,
      seat: normalizedSeats[0] || '',
    }),
  });
}

export function fetchBookings(token) {
  return request('/api/bookings', {
    headers: { Authorization: `Bearer ${token}` },
  });
}

export function fetchBookingByTicketCode(ticketCode) {
  return request(`/api/bookings/ticket/${encodeURIComponent(ticketCode)}`);
}
