import { useEffect, useMemo, useState } from 'react'
import { FaBus, FaEnvelope, FaPlus, FaRoute, FaTicketAlt } from 'react-icons/fa'
import { MdDashboard, MdSchedule } from 'react-icons/md'
import { adminApi } from '../services/api'
import toast from 'react-hot-toast'
import { format, parseISO } from 'date-fns'

const BUS_TYPES = ['AC', 'NON_AC', 'SLEEPER', 'SEMI_SLEEPER', 'SEATER']

function unwrap(res) {
  return res.data?.data ?? []
}

function fmtDateTime(dt) {
  if (!dt) return '--'
  try { return format(parseISO(dt), 'dd MMM yyyy, HH:mm') } catch { return String(dt).replace('T', ' ') }
}

function minutesToText(mins) {
  const n = Number(mins || 0)
  const h = Math.floor(n / 60)
  const m = n % 60
  return h && m ? `${h}h ${m}m` : h ? `${h}h` : `${m}m`
}

function getBusId(bus) {
  return bus?.id ?? bus?.busId
}

function getRouteId(route) {
  return route?.id ?? route?.routeId
}

function isBusActive(bus) {
  return bus?.isActive !== false && bus?.active !== false
}

function Tab({ active, onClick, icon, label }) {
  return (
    <button
      onClick={onClick}
      className={`inline-flex items-center gap-2 rounded-lg border px-4 py-2.5 text-sm font-800 transition-colors ${
        active
          ? 'border-[#d84e55] bg-[#d84e55]/10 text-[#d84e55]'
          : 'border-gray-200 bg-white text-slate-600 hover:border-[#d84e55]/40 hover:text-[#172033]'
      }`}
    >
      {icon} {label}
    </button>
  )
}

function Field({ label, children }) {
  return (
    <label className="block">
      <span className="mb-1 block text-xs font-800 uppercase tracking-wide text-slate-500">{label}</span>
      {children}
    </label>
  )
}

export default function AdminDashboardPage() {
  const [tab, setTab] = useState('dashboard')
  const [buses, setBuses] = useState([])
  const [routes, setRoutes] = useState([])
  const [bookings, setBookings] = useState([])
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [testEmail, setTestEmail] = useState('')
  const [sendingEmail, setSendingEmail] = useState(false)

  const [busForm, setBusForm] = useState({
    busNumber: '',
    busName: '',
    busType: 'AC',
    totalSeats: 40,
    amenities: 'WiFi, USB, Water',
  })

  const [routeForm, setRouteForm] = useState({
    origin: '',
    destination: '',
    distanceKm: '',
    durationMins: '',
  })

  const [scheduleForm, setScheduleForm] = useState({
    busId: '',
    routeId: '',
    departureTime: '',
    arrivalTime: '',
    baseFare: '',
    availableSeats: '',
  })

  const loadAdminData = async () => {
    setLoading(true)
    try {
      const [busRes, routeRes, bookingRes] = await Promise.all([
        adminApi.getBuses(),
        adminApi.getRoutes(),
        adminApi.getAllBookings(),
      ])
      const nextBuses = unwrap(busRes)
      const nextRoutes = unwrap(routeRes)
      const firstActiveBus = nextBuses.find(isBusActive)
      setBuses(nextBuses)
      setRoutes(nextRoutes)
      setBookings(unwrap(bookingRes))
      setScheduleForm(f => ({
        ...f,
        busId: f.busId || String(getBusId(firstActiveBus) || ''),
        routeId: f.routeId || String(getRouteId(nextRoutes[0]) || ''),
        availableSeats: f.availableSeats || String(firstActiveBus?.totalSeats || ''),
      }))
    } catch {
      toast.error('Unable to load admin data')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { loadAdminData() }, [])

  const activeBuses = useMemo(
    () => buses.filter(isBusActive),
    [buses]
  )

  const stats = [
    { label: 'Available buses', value: activeBuses.length, icon: <FaBus />, color: 'text-[#d84e55]', bg: 'bg-[#d84e55]/10' },
    { label: 'Routes', value: routes.length, icon: <FaRoute />, color: 'text-[#2563eb]', bg: 'bg-[#2563eb]/10' },
    { label: 'User bookings', value: bookings.length, icon: <FaTicketAlt />, color: 'text-[#059669]', bg: 'bg-[#059669]/10' },
    { label: 'Revenue', value: `Rs ${bookings.reduce((sum, b) => sum + Number(b.totalAmount || 0), 0).toLocaleString('en-IN')}`, icon: <MdDashboard />, color: 'text-[#f59e0b]', bg: 'bg-[#f59e0b]/10' },
  ]

  const updateBusForm = key => e => setBusForm(f => ({ ...f, [key]: e.target.value }))
  const updateRouteForm = key => e => setRouteForm(f => ({ ...f, [key]: e.target.value }))
  const updateScheduleForm = key => e => setScheduleForm(f => ({ ...f, [key]: e.target.value }))

  const createBus = async (e) => {
    e.preventDefault()
    setSaving(true)
    try {
      await adminApi.createBus({
        busNumber: busForm.busNumber.trim(),
        busName: busForm.busName.trim(),
        busType: busForm.busType,
        totalSeats: Number(busForm.totalSeats),
        amenities: busForm.amenities.trim(),
        isActive: true,
      })
      toast.success('Bus added')
      setBusForm({ busNumber: '', busName: '', busType: 'AC', totalSeats: 40, amenities: 'WiFi, USB, Water' })
      await loadAdminData()
      setTab('buses')
    } finally {
      setSaving(false)
    }
  }

  const createRoute = async (e) => {
    e.preventDefault()
    if (routeForm.origin.trim().toLowerCase() === routeForm.destination.trim().toLowerCase()) {
      toast.error('Source and destination must be different')
      return
    }
    setSaving(true)
    try {
      await adminApi.createRoute({
        origin: routeForm.origin.trim(),
        destination: routeForm.destination.trim(),
        distanceKm: Number(routeForm.distanceKm),
        durationMins: Number(routeForm.durationMins),
      })
      toast.success('Route added')
      setRouteForm({ origin: '', destination: '', distanceKm: '', durationMins: '' })
      await loadAdminData()
      setTab('routes')
    } finally {
      setSaving(false)
    }
  }

  const createSchedule = async (e) => {
    e.preventDefault()
    const selectedBus = buses.find(bus => String(getBusId(bus)) === String(scheduleForm.busId))
    setSaving(true)
    try {
      await adminApi.createSchedule({
        busId: Number(scheduleForm.busId),
        routeId: Number(scheduleForm.routeId),
        departureTime: scheduleForm.departureTime,
        arrivalTime: scheduleForm.arrivalTime,
        baseFare: Number(scheduleForm.baseFare),
        availableSeats: Number(scheduleForm.availableSeats || selectedBus?.totalSeats || 0),
        isActive: true,
      })
      toast.success('Schedule added. This bus will now appear in search results for the route.')
      setScheduleForm({
        busId: String(getBusId(activeBuses[0]) || ''),
        routeId: String(getRouteId(routes[0]) || ''),
        departureTime: '',
        arrivalTime: '',
        baseFare: '',
        availableSeats: String(activeBuses[0]?.totalSeats || ''),
      })
      await loadAdminData()
      setTab('schedules')
    } finally {
      setSaving(false)
    }
  }

  const deactivateBus = async (id) => {
    await adminApi.toggleBus(id)
    toast.success('Bus deactivated')
    await loadAdminData()
  }

  const sendTestEmail = async (e) => {
    e.preventDefault()
    setSendingEmail(true)
    try {
      const { data } = await adminApi.sendTestEmail(testEmail.trim())
      const delivery = data?.data
      if (delivery?.sent) {
        toast.success('Test email sent')
      } else {
        toast.error(delivery?.message || 'Test email was not sent')
      }
    } finally {
      setSendingEmail(false)
    }
  }

  return (
    <div className="page-shell">
      <div className="border-b border-gray-200 bg-white">
        <div className="section-wrap py-6">
          <h1 className="text-2xl font-800 text-[#172033]">Admin panel</h1>
          <p className="mt-1 text-sm text-slate-500">Manage buses, source-destination routes, schedules, and user bookings.</p>
        </div>
      </div>

      <div className="section-wrap py-8">
        <div className="mb-6 flex flex-wrap gap-2">
          <Tab active={tab === 'dashboard'} onClick={() => setTab('dashboard')} icon={<MdDashboard />} label="Dashboard" />
          <Tab active={tab === 'buses'} onClick={() => setTab('buses')} icon={<FaBus />} label="Buses" />
          <Tab active={tab === 'routes'} onClick={() => setTab('routes')} icon={<FaRoute />} label="Routes" />
          <Tab active={tab === 'schedules'} onClick={() => setTab('schedules')} icon={<MdSchedule />} label="Schedules" />
          <Tab active={tab === 'bookings'} onClick={() => setTab('bookings')} icon={<FaTicketAlt />} label="User bookings" />
        </div>

        {loading ? (
          <div className="card p-10 text-center text-slate-500">Loading admin data...</div>
        ) : (
          <>
            {tab === 'dashboard' && (
              <div className="grid gap-5">
                <div className="grid gap-5 md:grid-cols-2 xl:grid-cols-4">
                  {stats.map(item => (
                    <div key={item.label} className="card p-5">
                      <div className={`mb-4 flex h-11 w-11 items-center justify-center rounded-lg ${item.bg} ${item.color}`}>
                        {item.icon}
                      </div>
                      <p className="text-3xl font-800 text-[#172033]">{item.value}</p>
                      <p className="mt-1 text-sm text-slate-500">{item.label}</p>
                    </div>
                  ))}
                </div>

                <form onSubmit={sendTestEmail} className="card p-5">
                  <h2 className="mb-4 flex items-center gap-2 text-lg font-800 text-[#172033]"><FaEnvelope /> Test email notifications</h2>
                  <div className="grid gap-3 md:grid-cols-[1fr_auto]">
                    <input
                      required
                      type="email"
                      value={testEmail}
                      onChange={e => setTestEmail(e.target.value)}
                      placeholder="recipient@example.com"
                      className="input-field"
                    />
                    <button disabled={sendingEmail} className="btn-primary">
                      {sendingEmail ? 'Sending...' : 'Send test'}
                    </button>
                  </div>
                </form>
              </div>
            )}

            {tab === 'buses' && (
              <div className="grid gap-6 xl:grid-cols-[380px_1fr]">
                <form onSubmit={createBus} className="card h-fit p-5">
                  <h2 className="mb-4 flex items-center gap-2 text-lg font-800 text-[#172033]"><FaPlus /> Add bus</h2>
                  <div className="grid gap-4">
                    <Field label="Bus number">
                      <input required value={busForm.busNumber} onChange={updateBusForm('busNumber')} placeholder="MH12AB1234" className="input-field" />
                    </Field>
                    <Field label="Bus name">
                      <input required value={busForm.busName} onChange={updateBusForm('busName')} placeholder="Pune Express" className="input-field" />
                    </Field>
                    <div className="grid grid-cols-2 gap-3">
                      <Field label="Bus type">
                        <select value={busForm.busType} onChange={updateBusForm('busType')} className="input-field">
                          {BUS_TYPES.map(type => <option key={type}>{type}</option>)}
                        </select>
                      </Field>
                      <Field label="Total seats">
                        <input required type="number" min="1" value={busForm.totalSeats} onChange={updateBusForm('totalSeats')} className="input-field" />
                      </Field>
                    </div>
                    <Field label="Amenities">
                      <input value={busForm.amenities} onChange={updateBusForm('amenities')} placeholder="WiFi, USB, Water" className="input-field" />
                    </Field>
                    <button disabled={saving} className="btn-primary">Add bus</button>
                  </div>
                </form>

                <div className="card overflow-hidden">
                  <div className="border-b border-gray-100 p-5">
                    <h2 className="text-lg font-800 text-[#172033]">Available buses</h2>
                    <p className="mt-1 text-sm text-slate-500">These active buses can be assigned to schedules.</p>
                  </div>
                  <div className="overflow-x-auto">
                    <table className="w-full text-sm">
                      <thead className="bg-slate-50 text-left text-xs font-800 uppercase text-slate-500">
                        <tr>
                          <th className="px-4 py-3">Bus</th>
                          <th className="px-4 py-3">Number</th>
                          <th className="px-4 py-3">Type</th>
                          <th className="px-4 py-3">Seats</th>
                          <th className="px-4 py-3">Status</th>
                          <th className="px-4 py-3"></th>
                        </tr>
                      </thead>
                      <tbody>
                        {buses.map(bus => (
                          <tr key={getBusId(bus)} className="border-t border-gray-100">
                            <td className="px-4 py-3 font-800 text-[#172033]">{bus.busName}</td>
                            <td className="px-4 py-3 text-slate-600">{bus.busNumber}</td>
                            <td className="px-4 py-3 text-slate-600">{bus.busType}</td>
                            <td className="px-4 py-3 text-slate-600">{bus.totalSeats}</td>
                            <td className="px-4 py-3">
                              <span className={`badge ${bus.isActive !== false ? 'text-emerald-700' : 'text-slate-500'}`}>
                                {bus.isActive !== false ? 'Active' : 'Inactive'}
                              </span>
                            </td>
                            <td className="px-4 py-3 text-right">
                              {bus.isActive !== false && (
                                <button onClick={() => deactivateBus(getBusId(bus))} className="text-sm font-800 text-red-600 hover:underline">
                                  Deactivate
                                </button>
                              )}
                            </td>
                          </tr>
                        ))}
                        {buses.length === 0 && (
                          <tr><td colSpan={6} className="px-4 py-8 text-center text-slate-500">No buses added yet.</td></tr>
                        )}
                      </tbody>
                    </table>
                  </div>
                </div>
              </div>
            )}

            {tab === 'routes' && (
              <div className="grid gap-6 xl:grid-cols-[380px_1fr]">
                <form onSubmit={createRoute} className="card h-fit p-5">
                  <h2 className="mb-4 flex items-center gap-2 text-lg font-800 text-[#172033]"><FaPlus /> Add route</h2>
                  <div className="grid gap-4">
                    <Field label="Source / from">
                      <input required value={routeForm.origin} onChange={updateRouteForm('origin')} placeholder="Pune" className="input-field" />
                    </Field>
                    <Field label="Destination / to">
                      <input required value={routeForm.destination} onChange={updateRouteForm('destination')} placeholder="Mumbai" className="input-field" />
                    </Field>
                    <div className="grid grid-cols-2 gap-3">
                      <Field label="Distance km">
                        <input required type="number" min="1" value={routeForm.distanceKm} onChange={updateRouteForm('distanceKm')} className="input-field" />
                      </Field>
                      <Field label="Duration mins">
                        <input required type="number" min="1" value={routeForm.durationMins} onChange={updateRouteForm('durationMins')} className="input-field" />
                      </Field>
                    </div>
                    <button disabled={saving} className="btn-primary">Add route</button>
                  </div>
                </form>

                <div className="card overflow-hidden">
                  <div className="border-b border-gray-100 p-5">
                    <h2 className="text-lg font-800 text-[#172033]">Source to destination routes</h2>
                    <p className="mt-1 text-sm text-slate-500">Create routes first, then schedule buses on them.</p>
                  </div>
                  <div className="grid gap-3 p-5 md:grid-cols-2">
                    {routes.map(route => (
                      <div key={getRouteId(route)} className="rounded-lg border border-gray-200 p-4">
                        <div className="flex items-center justify-between gap-3">
                          <p className="font-800 text-[#172033]">{route.origin} to {route.destination}</p>
                          <FaRoute className="text-[#d84e55]" />
                        </div>
                        <p className="mt-2 text-sm text-slate-500">{route.distanceKm} km · {minutesToText(route.durationMins)}</p>
                      </div>
                    ))}
                    {routes.length === 0 && <p className="text-sm text-slate-500">No routes added yet.</p>}
                  </div>
                </div>
              </div>
            )}

            {tab === 'schedules' && (
              <div className="grid gap-6 xl:grid-cols-[420px_1fr]">
                <form onSubmit={createSchedule} className="card h-fit p-5">
                  <h2 className="mb-4 flex items-center gap-2 text-lg font-800 text-[#172033]"><FaPlus /> Assign bus to route</h2>
                  <div className="grid gap-4">
                    <Field label="Bus">
                      <select required value={scheduleForm.busId} onChange={updateScheduleForm('busId')} className="input-field">
                        {activeBuses.map(bus => <option key={getBusId(bus)} value={getBusId(bus)}>{bus.busName} ({bus.busNumber})</option>)}
                      </select>
                    </Field>
                    <Field label="Route">
                      <select required value={scheduleForm.routeId} onChange={updateScheduleForm('routeId')} className="input-field">
                        {routes.map(route => <option key={getRouteId(route)} value={getRouteId(route)}>{route.origin} to {route.destination}</option>)}
                      </select>
                    </Field>
                    <div className="grid grid-cols-2 gap-3">
                      <Field label="Departure">
                        <input required type="datetime-local" value={scheduleForm.departureTime} onChange={updateScheduleForm('departureTime')} className="input-field" />
                      </Field>
                      <Field label="Arrival">
                        <input required type="datetime-local" value={scheduleForm.arrivalTime} onChange={updateScheduleForm('arrivalTime')} className="input-field" />
                      </Field>
                    </div>
                    <div className="grid grid-cols-2 gap-3">
                      <Field label="Base fare">
                        <input required type="number" min="1" value={scheduleForm.baseFare} onChange={updateScheduleForm('baseFare')} className="input-field" />
                      </Field>
                      <Field label="Available seats">
                        <input required type="number" min="1" value={scheduleForm.availableSeats} onChange={updateScheduleForm('availableSeats')} className="input-field" />
                      </Field>
                    </div>
                    <button disabled={saving || activeBuses.length === 0 || routes.length === 0} className="btn-primary">
                      Create schedule
                    </button>
                    {(activeBuses.length === 0 || routes.length === 0) && (
                      <p className="text-xs text-red-600">Add at least one active bus and one route before creating a schedule.</p>
                    )}
                  </div>
                </form>

                <div className="card p-5">
                  <h2 className="text-lg font-800 text-[#172033]">How availability works</h2>
                  <p className="mt-2 text-sm leading-6 text-slate-500">
                    A bus becomes searchable for passengers only after you create a schedule that connects an active bus
                    with a source-destination route, departure time, arrival time, fare, and available seats.
                  </p>
                  <div className="mt-5 grid gap-3 md:grid-cols-3">
                    <div className="rounded-lg bg-slate-50 p-4">
                      <p className="font-800 text-[#172033]">1. Add bus</p>
                      <p className="mt-1 text-sm text-slate-500">Create bus number, type and seats.</p>
                    </div>
                    <div className="rounded-lg bg-slate-50 p-4">
                      <p className="font-800 text-[#172033]">2. Add route</p>
                      <p className="mt-1 text-sm text-slate-500">Set source and destination.</p>
                    </div>
                    <div className="rounded-lg bg-slate-50 p-4">
                      <p className="font-800 text-[#172033]">3. Create schedule</p>
                      <p className="mt-1 text-sm text-slate-500">Passengers can then find it in search.</p>
                    </div>
                  </div>
                </div>
              </div>
            )}

            {tab === 'bookings' && (
              <div className="card overflow-hidden">
                <div className="border-b border-gray-100 p-5">
                  <h2 className="text-lg font-800 text-[#172033]">Bookings made by users</h2>
                  <p className="mt-1 text-sm text-slate-500">All passenger bookings returned by the admin bookings API.</p>
                </div>
                <div className="overflow-x-auto">
                  <table className="w-full text-sm">
                    <thead className="bg-slate-50 text-left text-xs font-800 uppercase text-slate-500">
                      <tr>
                        <th className="px-4 py-3">Ref</th>
                        <th className="px-4 py-3">Customer</th>
                        <th className="px-4 py-3">Route</th>
                        <th className="px-4 py-3">Bus</th>
                        <th className="px-4 py-3">Departure</th>
                        <th className="px-4 py-3">Seats</th>
                        <th className="px-4 py-3">Amount</th>
                        <th className="px-4 py-3">Status</th>
                      </tr>
                    </thead>
                    <tbody>
                      {bookings.map(booking => (
                        <tr key={booking.bookingRef} className="border-t border-gray-100">
                          <td className="px-4 py-3 font-mono text-xs text-slate-600">{booking.bookingRef}</td>
                          <td className="px-4 py-3">
                            <p className="font-800 text-[#172033]">{booking.customerName || '--'}</p>
                            <p className="text-xs text-slate-500">{booking.customerEmail || '--'}</p>
                          </td>
                          <td className="px-4 py-3 font-800 text-[#172033]">{booking.origin} to {booking.destination}</td>
                          <td className="px-4 py-3 text-slate-600">{booking.busName}</td>
                          <td className="px-4 py-3 text-slate-600">{fmtDateTime(booking.departureTime)}</td>
                          <td className="px-4 py-3 text-slate-600">{booking.seats?.map(s => s.seatNumber).join(', ') || '--'}</td>
                          <td className="px-4 py-3 font-800 text-[#d84e55]">Rs {booking.totalAmount}</td>
                          <td className="px-4 py-3"><span className="badge">{booking.bookingStatus}</span></td>
                        </tr>
                      ))}
                      {bookings.length === 0 && (
                        <tr><td colSpan={8} className="px-4 py-8 text-center text-slate-500">No user bookings found yet.</td></tr>
                      )}
                    </tbody>
                  </table>
                </div>
              </div>
            )}
          </>
        )}
      </div>
    </div>
  )
}
