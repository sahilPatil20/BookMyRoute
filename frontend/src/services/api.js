import axios from 'axios'
import toast from 'react-hot-toast'

const TOKEN_KEY = 'bmr_token'

const api = axios.create({
  baseURL: '/api',
  headers: { 'Content-Type': 'application/json' },
  timeout: 15000,
})

// ── Request interceptor — attach JWT ──────────────────────────
api.interceptors.request.use(config => {
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

// ── Response interceptor — global error handling ──────────────
api.interceptors.response.use(
  (res) => res,
  (err) => {
    const msg = err.response?.data?.message || 'Something went wrong'
    if (err.response?.status === 401) {
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem('bmr_user')
      window.location.href = '/login'
    } else if (err.response?.status !== 404) {
      toast.error(msg)
    }
    return Promise.reject(err)
  }
)

// ── Auth ──────────────────────────────────────────────────────
// POST /api/auth/register  → { name, email, password, phone }
// POST /api/auth/login     → { email, password }
// GET  /api/auth/me        → current user (JWT required)
export const authApi = {
  login:    (data) => api.post('/auth/login', data),
  register: (data) => api.post('/auth/register', data),
  me:       ()     => api.get('/auth/me'),
}

// ── Search / Schedules ────────────────────────────────────────
// GET /api/schedules/search?origin=&destination=&travelDate=&seats=
//   → ApiResponse<List<ScheduleResponse.Search>>
//     fields: scheduleId, origin, destination, departureTime, arrivalTime,
//             baseFare, availableSeats, busName, busType, amenities, durationMins
// GET /api/schedules/:id     → ApiResponse<Schedule>
// GET /api/schedules/:id/seats → ApiResponse<List<SeatInfo>>
//   SeatInfo fields: seatId, seatNumber, seatType
export const searchApi = {
  searchBuses: (params) => api.get('/schedules/search', { params }),
  getSchedule: (id)     => api.get(`/schedules/${id}`),
  getCities:   ()       => api.get('/routes/cities'),
}

// ── Seats ─────────────────────────────────────────────────────
// GET  /api/schedules/:scheduleId/seats  → available seats for a trip
// POST /api/schedules/:scheduleId/seats/check → { seatIds: [] }
export const seatApi = {
  getSeats: (scheduleId) => api.get(`/schedules/${scheduleId}/seats`),
}

// ── Bookings ──────────────────────────────────────────────────
// POST  /api/bookings         → BookingRequest
//   { scheduleId, paymentMethod, passengers: [{seatId, passengerName, passengerAge}] }
//   → ApiResponse<BookingResponse>
//     fields: bookingId, bookingRef, origin, destination, departureTime, arrivalTime,
//             busName, totalAmount, bookingStatus, paymentStatus, paymentMethod, bookedAt,
//             seats:[{seatNumber, seatType, passengerName, passengerAge, fare}]
// GET   /api/bookings/my      → List<BookingResponse>
// GET   /api/bookings/:ref    → BookingResponse
// GET   /api/bookings/:ref/pdf → PDF blob
// PATCH /api/bookings/:ref/cancel
export const bookingApi = {
  createBooking: (data)   => api.post('/bookings', data),
  getMyBookings: ()       => api.get('/bookings/my'),
  getBooking:    (ref)    => api.get(`/bookings/${ref}`),
  downloadTicket: (ref)   => api.get(`/bookings/${ref}/pdf`, { responseType: 'blob' }),
  cancelBooking: (ref)    => api.patch(`/bookings/${ref}/cancel`),
}

export const chatbotApi = {
  sendMessage: (data) => api.post('/chatbot/message', data),
}

// ── Admin ─────────────────────────────────────────────────────
// GET  /api/admin/stats
// CRUD /api/buses, /api/routes, /api/schedules (ADMIN role required)
// GET  /api/bookings (ADMIN) → all bookings
export const adminApi = {
  // Stats (no dedicated admin/stats endpoint in backend — build from buses+routes+bookings)
  getBuses:       (params)   => api.get('/admin/buses', { params }),
  createBus:      (data)     => api.post('/buses', data),
  updateBus:      (id, data) => api.put(`/buses/${id}`, data),
  toggleBus:      (id)       => api.delete(`/buses/${id}`),          // deactivates

  getRoutes:      (params)   => api.get('/admin/routes', { params }),
  createRoute:    (data)     => api.post('/routes', data),
  updateRoute:    (id, data) => api.put(`/routes/${id}`, data),

  getSchedules:   (params)   => api.get('/admin/schedules', { params }),
  createSchedule: (data)     => api.post('/admin/schedules', data),
  updateSchedule: (id, data) => api.put(`/admin/schedules/${id}`, data),
  deactivateSchedule: (id)   => api.delete(`/schedules/${id}`),

  getAllBookings:  (params)   => api.get('/admin/bookings', { params }),
  sendTestEmail:   (to)       => api.post('/admin/email/test', null, { params: { to } }),
}

export default api
