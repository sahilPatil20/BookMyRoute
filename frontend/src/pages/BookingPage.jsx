import { useEffect, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { format, parseISO } from 'date-fns'
import toast from 'react-hot-toast'
import { FaArrowLeft, FaArrowRight, FaBus, FaCheck, FaCheckCircle, FaCreditCard, FaDownload, FaMobileAlt, FaShieldAlt, FaUniversity, FaUser } from 'react-icons/fa'
import { MdEventSeat, MdPayment } from 'react-icons/md'
import { bookingApi, seatApi } from '../services/api'
import { useAuth } from '../context/AuthContext'

function fmtTime(dt) {
  if (!dt) return '--'
  try { return format(parseISO(dt), 'HH:mm') } catch { return dt.slice(11, 16) || '--' }
}

function StepBar({ step }) {
  const steps = [
    { label: 'Seats', icon: <MdEventSeat /> },
    { label: 'Passengers', icon: <FaUser /> },
    { label: 'Payment', icon: <MdPayment /> },
  ]

  return (
    <div className="mb-6 overflow-x-auto">
      <div className="mx-auto flex min-w-max items-center justify-center">
        {steps.map((item, i) => (
          <div key={item.label} className="flex items-center">
            <div className={`flex h-10 items-center gap-2 rounded-lg border px-4 text-sm font-800 transition-colors ${
              i < step
                ? 'border-emerald-600 bg-emerald-600 text-white'
                : i === step
                  ? 'border-[#d84e55] bg-[#d84e55] text-white'
                  : 'border-gray-200 bg-white text-slate-400'
            }`}>
              {i < step ? <FaCheck /> : item.icon}
              {item.label}
            </div>
            {i < steps.length - 1 && (
              <div className={`h-px w-8 ${i < step ? 'bg-emerald-600' : 'bg-gray-200'}`} />
            )}
          </div>
        ))}
      </div>
    </div>
  )
}

function SeatGrid({ seats, selected, onToggle, loading }) {
  const rows = []
  for (let i = 0; i < seats.length; i += 4) rows.push(seats.slice(i, i + 4))

  if (loading) {
    return (
      <div className="rounded-lg border border-dashed border-gray-300 bg-slate-50 py-14 text-center">
        <FaBus className="mx-auto mb-3 animate-pulse text-4xl text-[#d84e55]" />
        <p className="text-sm font-700 text-slate-500">Loading seat map...</p>
      </div>
    )
  }

  if (!seats.length) {
    return (
      <div className="rounded-lg border border-dashed border-gray-300 bg-slate-50 py-14 text-center">
        <MdEventSeat className="mx-auto mb-3 text-4xl text-slate-300" />
        <p className="text-sm font-700 text-slate-500">No seat data is available for this schedule.</p>
      </div>
    )
  }

  return (
    <div>
      <div className="mb-5 flex flex-wrap justify-center gap-4">
        {[
          ['Available', 'border-gray-300 bg-white'],
          ['Selected', 'border-[#d84e55] bg-[#d84e55]'],
          ['Booked', 'border-gray-200 bg-gray-100'],
        ].map(([label, style]) => (
          <div key={label} className="flex items-center gap-2 text-sm text-slate-600">
            <div className={`h-6 w-6 rounded-md border ${style}`} />
            <span>{label}</span>
          </div>
        ))}
      </div>

      <div className="mx-auto max-w-sm rounded-lg border border-gray-200 bg-white p-5 shadow-sm">
        <div className="mb-4 flex justify-center">
          <div className="flex items-center gap-2 rounded-t-lg bg-[#172033] px-8 py-1.5 text-xs font-800 text-white">
            <FaBus /> Front
          </div>
        </div>

        <div className="flex flex-col gap-2">
          {rows.map((row, rowIndex) => (
            <div key={rowIndex} className="flex items-center justify-center gap-2">
              {row.slice(0, 2).map(seat => (
                <button
                  key={seat.seatId}
                  type="button"
                  disabled={seat.status === 'booked'}
                  onClick={() => onToggle(seat)}
                  className={`seat ${seat.status === 'booked' ? 'seat-booked' : selected.find(s => s.seatId === seat.seatId) ? 'seat-selected' : 'seat-available'}`}
                  aria-pressed={Boolean(selected.find(s => s.seatId === seat.seatId))}
                >
                  {seat.seatNumber}
                </button>
              ))}
              <div className="w-5" />
              {row.slice(2).map(seat => (
                <button
                  key={seat.seatId}
                  type="button"
                  disabled={seat.status === 'booked'}
                  onClick={() => onToggle(seat)}
                  className={`seat ${seat.status === 'booked' ? 'seat-booked' : selected.find(s => s.seatId === seat.seatId) ? 'seat-selected' : 'seat-available'}`}
                  aria-pressed={Boolean(selected.find(s => s.seatId === seat.seatId))}
                >
                  {seat.seatNumber}
                </button>
              ))}
            </div>
          ))}
        </div>
      </div>
    </div>
  )
}

const PAYMENT_METHODS = [
  { key: 'UPI', label: 'UPI', desc: 'Google Pay, PhonePe, Paytm', icon: <FaMobileAlt /> },
  { key: 'CARD', label: 'Credit / Debit Card', desc: 'Visa, Mastercard, RuPay', icon: <FaCreditCard /> },
  { key: 'NET_BANKING', label: 'Net Banking', desc: 'All major banks', icon: <FaUniversity /> },
  { key: 'WALLET', label: 'Wallet', desc: 'Fast wallet checkout', icon: <MdPayment /> },
]

export default function BookingPage() {
  const { state } = useLocation()
  const navigate = useNavigate()
  const { user } = useAuth()

  const bus = state?.bus
  const [step, setStep] = useState(0)
  const [seats, setSeats] = useState([])
  const [seatsLoading, setSeatsLoading] = useState(true)
  const [selected, setSelected] = useState([])
  const [passengers, setPassengers] = useState([])
  const [payMethod, setPayMethod] = useState('UPI')
  const [loading, setLoading] = useState(false)
  const [downloading, setDownloading] = useState(false)
  const [confirmed, setConfirmed] = useState(null)

  useEffect(() => {
    if (!bus?.scheduleId) return
    setSeatsLoading(true)
    seatApi.getSeats(bus.scheduleId)
      .then(res => {
        const raw = res.data?.data ?? []
        setSeats(raw.map(seat => ({ ...seat, status: 'available' })))
      })
      .catch(() => setSeats([]))
      .finally(() => setSeatsLoading(false))
  }, [bus?.scheduleId])

  if (!bus) {
    return (
      <div className="page-shell flex items-center justify-center p-4">
        <div className="card max-w-md p-8 text-center">
          <FaBus className="mx-auto mb-4 text-5xl text-slate-300" />
          <h1 className="text-2xl font-800 text-[#172033]">No bus selected</h1>
          <p className="mt-2 text-sm text-slate-500">Search for a route and choose a bus before booking seats.</p>
          <button onClick={() => navigate('/search')} className="btn-primary mt-6">
            <FaArrowLeft /> Search buses
          </button>
        </div>
      </div>
    )
  }

  const toggleSeat = (seat) => {
    setSelected(prev =>
      prev.find(s => s.seatId === seat.seatId)
        ? prev.filter(s => s.seatId !== seat.seatId)
        : [...prev, seat]
    )
  }

  const totalFare = selected.length * Number(bus.baseFare)
  const depTime = fmtTime(bus.departureTime)
  const arrTime = fmtTime(bus.arrivalTime)

  const goToPassengers = () => {
    if (!selected.length) {
      toast.error('Please select at least one seat')
      return
    }
    setPassengers(selected.map(s => ({ seatId: s.seatId, seatLabel: s.seatNumber, name: '', age: '' })))
    setStep(1)
  }

  const goToPayment = () => {
    const invalid = passengers.find(p => !p.name.trim() || !p.age)
    if (invalid) {
      toast.error('Please fill all passenger details')
      return
    }
    setStep(2)
  }

  const handlePayment = async () => {
    setLoading(true)
    try {
      const payload = {
        scheduleId: bus.scheduleId,
        paymentMethod: payMethod,
        passengers: passengers.map(p => ({
          seatId: p.seatId,
          passengerName: p.name.trim(),
          passengerAge: parseInt(p.age, 10),
        })),
      }
      const { data } = await bookingApi.createBooking(payload)
      const booking = data?.data
      setConfirmed({
        ref: booking?.bookingRef || 'BMR-CONFIRMED',
        amount: booking?.totalAmount || totalFare,
        status: booking?.bookingStatus || 'CONFIRMED',
        emailSent: booking?.notificationEmailSent === true,
        emailMessage: booking?.notificationEmailMessage,
      })
      setStep(3)
      if (booking?.notificationEmailSent === true) {
        toast.success('Booking confirmed and email sent')
      } else {
        toast.success('Booking confirmed')
      }
    } finally {
      setLoading(false)
    }
  }

  const handleDownloadTicket = async () => {
    if (!confirmed?.ref) return

    setDownloading(true)
    try {
      const response = await bookingApi.downloadTicket(confirmed.ref)
      const blob = new Blob([response.data], { type: 'application/pdf' })
      const url = window.URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = `BookMyRoute-${confirmed.ref}.pdf`
      document.body.appendChild(link)
      link.click()
      link.remove()
      window.URL.revokeObjectURL(url)
    } finally {
      setDownloading(false)
    }
  }

  if (step === 3 && confirmed) {
    return (
      <div className="page-shell flex items-center justify-center p-4">
        <div className="card w-full max-w-lg p-8 text-center">
          <div className="mx-auto mb-5 flex h-20 w-20 items-center justify-center rounded-full bg-emerald-600 text-white">
            <FaCheckCircle className="text-4xl" />
          </div>
          <h1 className="text-3xl font-800 text-[#172033]">Booking confirmed</h1>
          <p className="mt-2 text-sm text-slate-500">
            {confirmed.emailSent
              ? `Your ticket has been sent to ${user?.email || 'your email'}.`
              : confirmed.emailMessage || 'Email notification was not sent. You can still download your ticket PDF.'}
          </p>

          <div className="my-6 rounded-lg bg-[#172033] p-4">
            <p className="font-mono text-xl font-800 tracking-wider text-[#f59e0b]">{confirmed.ref}</p>
            <p className="mt-1 text-xs text-slate-300">Booking reference</p>
          </div>

          <div className="mb-6 grid grid-cols-2 gap-3 text-left">
            {[
              ['Route', `${bus.origin} to ${bus.destination}`],
              ['Bus', bus.busName],
              ['Departure', depTime],
              ['Seats', selected.map(s => s.seatNumber).join(', ')],
              ['Passengers', selected.length],
              ['Total', `Rs ${confirmed.amount}`],
            ].map(([label, value]) => (
              <div key={label} className="rounded-lg bg-slate-50 p-3">
                <p className="text-xs font-800 uppercase text-slate-400">{label}</p>
                <p className="mt-1 text-sm font-800 text-[#172033]">{value}</p>
              </div>
            ))}
          </div>

          <div className="grid gap-3 sm:grid-cols-3">
            <button onClick={handleDownloadTicket} disabled={downloading} className="btn-primary">
              <FaDownload /> {downloading ? 'Downloading...' : 'Ticket PDF'}
            </button>
            <button onClick={() => navigate('/my-bookings')} className="btn-outline">My bookings</button>
            <button onClick={() => navigate('/search')} className="btn-outline">Book another</button>
          </div>
        </div>
      </div>
    )
  }

  return (
    <div className="page-shell py-8">
      <div className="section-wrap max-w-5xl">
        <StepBar step={step} />

        <div className="card mb-6 p-5">
          <div className="grid gap-5 lg:grid-cols-[1fr_auto_auto] lg:items-center">
            <div className="flex items-center gap-3">
              <div className="flex h-11 w-11 items-center justify-center rounded-lg bg-[#d84e55] text-white">
                <FaBus />
              </div>
              <div>
                <p className="font-800 text-[#172033]">{bus.busName}</p>
                <p className="text-sm text-slate-500">{bus.busType}</p>
              </div>
            </div>

            <div className="grid grid-cols-[1fr_auto_1fr] items-center gap-4 text-center">
              <div>
                <p className="text-xl font-800 text-[#172033]">{depTime}</p>
                <p className="text-sm text-slate-500">{bus.origin}</p>
              </div>
              <FaArrowRight className="text-slate-300" />
              <div>
                <p className="text-xl font-800 text-[#172033]">{arrTime}</p>
                <p className="text-sm text-slate-500">{bus.destination}</p>
              </div>
            </div>

            <div className="rounded-lg bg-slate-50 px-4 py-3 text-right">
              <p className="text-2xl font-800 text-[#d84e55]">Rs {bus.baseFare}<span className="text-sm font-normal text-slate-400">/seat</span></p>
              {selected.length > 0 && <p className="text-sm font-800 text-emerald-600">Total: Rs {totalFare}</p>}
            </div>
          </div>
        </div>

        {step === 0 && (
          <div className="card p-6">
            <h2 className="mb-6 flex items-center gap-2 text-2xl font-800 text-[#172033]">
              <MdEventSeat className="text-[#d84e55]" /> Select seats
            </h2>
            <SeatGrid seats={seats} selected={selected} onToggle={toggleSeat} loading={seatsLoading} />
            {selected.length > 0 && (
              <div className="mt-6 flex flex-col gap-4 rounded-lg border border-amber-300 bg-amber-50 p-4 sm:flex-row sm:items-center sm:justify-between">
                <div>
                  <p className="font-800 text-[#172033]">Selected: {selected.map(s => s.seatNumber).join(', ')}</p>
                  <p className="text-sm text-slate-500">{selected.length} seat{selected.length > 1 ? 's' : ''} x Rs {bus.baseFare}</p>
                </div>
                <button onClick={goToPassengers} className="btn-primary">
                  Continue <FaArrowRight />
                </button>
              </div>
            )}
          </div>
        )}

        {step === 1 && (
          <div className="card p-6">
            <h2 className="mb-6 flex items-center gap-2 text-2xl font-800 text-[#172033]">
              <FaUser className="text-[#d84e55]" /> Passenger details
            </h2>
            <div className="grid gap-4">
              {passengers.map((passenger, i) => (
                <div key={passenger.seatId} className="rounded-lg border border-gray-200 p-4">
                  <p className="mb-3 flex items-center gap-2 text-sm font-800 text-[#172033]">
                    <span className="flex h-7 w-7 items-center justify-center rounded-md bg-[#d84e55] text-xs text-white">{passenger.seatLabel}</span>
                    Passenger {i + 1}
                  </p>
                  <div className="grid gap-3 sm:grid-cols-2">
                    <label>
                      <span className="mb-1 block text-xs font-800 uppercase text-slate-500">Full name</span>
                      <input
                        value={passenger.name}
                        onChange={e => setPassengers(items => items.map((item, j) => j === i ? { ...item, name: e.target.value } : item))}
                        placeholder="Enter full name"
                        className="input-field"
                      />
                    </label>
                    <label>
                      <span className="mb-1 block text-xs font-800 uppercase text-slate-500">Age</span>
                      <input
                        type="number"
                        min="1"
                        max="120"
                        value={passenger.age}
                        onChange={e => setPassengers(items => items.map((item, j) => j === i ? { ...item, age: e.target.value } : item))}
                        placeholder="Age"
                        className="input-field"
                      />
                    </label>
                  </div>
                </div>
              ))}
            </div>
            <div className="mt-6 grid gap-3 sm:grid-cols-2">
              <button onClick={() => setStep(0)} className="btn-outline"><FaArrowLeft /> Back</button>
              <button onClick={goToPayment} className="btn-primary">Continue <FaArrowRight /></button>
            </div>
          </div>
        )}

        {step === 2 && (
          <div className="card p-6">
            <h2 className="mb-6 flex items-center gap-2 text-2xl font-800 text-[#172033]">
              <MdPayment className="text-[#d84e55]" /> Payment
            </h2>
            <div className="grid gap-6 lg:grid-cols-2">
              <div>
                <p className="mb-3 text-sm font-800 text-slate-500">Select payment method</p>
                <div className="grid gap-2">
                  {PAYMENT_METHODS.map(method => (
                    <button
                      key={method.key}
                      type="button"
                      onClick={() => setPayMethod(method.key)}
                      className={`flex items-center gap-3 rounded-lg border p-4 text-left transition-colors ${
                        payMethod === method.key
                          ? 'border-[#d84e55] bg-[#d84e55]/5'
                          : 'border-gray-200 bg-white hover:border-[#d84e55]/50'
                      }`}
                    >
                      <span className="text-xl text-[#d84e55]">{method.icon}</span>
                      <span className="min-w-0 flex-1">
                        <span className="block font-800 text-[#172033]">{method.label}</span>
                        <span className="block text-xs text-slate-500">{method.desc}</span>
                      </span>
                      {payMethod === method.key && <FaCheckCircle className="text-[#d84e55]" />}
                    </button>
                  ))}
                </div>
              </div>

              <div>
                <p className="mb-3 text-sm font-800 text-slate-500">Order summary</p>
                <div className="rounded-lg border border-gray-200 bg-slate-50 p-4">
                  <div className="grid gap-3">
                    {selected.map(seat => (
                      <div key={seat.seatId} className="flex justify-between text-sm">
                        <span className="text-slate-600">Seat {seat.seatNumber}</span>
                        <span className="font-800 text-[#172033]">Rs {bus.baseFare}</span>
                      </div>
                    ))}
                  </div>
                  <div className="mt-4 flex justify-between border-t border-dashed border-gray-300 pt-4">
                    <span className="font-800 text-[#172033]">Total</span>
                    <span className="text-xl font-800 text-[#d84e55]">Rs {totalFare}</span>
                  </div>
                </div>
                <div className="mt-3 flex items-center gap-2 rounded-lg border border-emerald-200 bg-emerald-50 p-3 text-xs font-700 text-emerald-700">
                  <FaShieldAlt /> Your payment is secured and encrypted.
                </div>
              </div>
            </div>

            <div className="mt-6 grid gap-3 sm:grid-cols-2">
              <button onClick={() => setStep(1)} className="btn-outline"><FaArrowLeft /> Back</button>
              <button onClick={handlePayment} disabled={loading} className="btn-primary">
                {loading ? 'Processing...' : `Pay Rs ${totalFare}`} <FaArrowRight />
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  )
}
