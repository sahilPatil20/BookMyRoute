# 🚌 BookMyRoute

A full-stack bus ticket booking web application built with **React + Vite** (frontend) and **Spring Boot** (backend). Users can search for bus routes, book seats, manage bookings, and interact with an AI chatbot — all managed through a clean admin dashboard.

---

## 📋 Table of Contents

- [Features](#features)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Prerequisites](#prerequisites)
- [Getting Started](#getting-started)
  - [1. Database Setup](#1-database-setup)
  - [2. Backend Setup](#2-backend-setup)
  - [3. Frontend Setup](#3-frontend-setup)
- [Environment Variables](#environment-variables)
- [Default Credentials](#default-credentials)
- [API Overview](#api-overview)
- [Database Schema](#database-schema)

---

## ✨ Features

### 👤 User
- Register & login with JWT-based authentication
- Search bus routes by source, destination & date
- View available schedules with seat availability
- Select seats and book tickets
- View & manage personal bookings
- AI-powered chatbot assistant

### 🛠️ Admin
- Admin dashboard with stats & analytics
- Manage buses (add, edit, activate/deactivate)
- Manage routes and schedules
- View and manage all user bookings
- Manage user accounts

---

## 🧰 Tech Stack

### Frontend
| Technology | Version |
|-----------|---------|
| React | 18.3.1 |
| Vite | 5.3.1 |
| Tailwind CSS | 3.4.4 |
| React Router DOM | 6.24.0 |
| Axios | 1.7.2 |
| Recharts | 2.12.7 |
| React Hot Toast | 2.4.1 |

### Backend
| Technology | Version |
|-----------|---------|
| Java | 17 |
| Spring Boot | 3.2.4 |
| Spring Security | (included) |
| Spring Data JPA | (included) |
| MySQL | 8.0+ |
| JWT (jjwt) | latest |
| MapStruct | latest |

---

## 📁 Project Structure

```
BookMyRoute/
├── frontend/                        # React + Vite application
│   ├── src/
│   │   ├── components/
│   │   │   └── common/              # Navbar, Footer, Chatbot, ProtectedRoute
│   │   ├── pages/                   # Route-level page components
│   │   │   ├── HomePage.jsx
│   │   │   ├── SearchPage.jsx
│   │   │   ├── BookingPage.jsx
│   │   │   ├── MyBookingsPage.jsx
│   │   │   ├── AdminDashboardPage.jsx
│   │   │   └── AuthPages.jsx
│   │   ├── context/                 # Auth context (global state)
│   │   └── services/                # Axios API service layer
│   ├── index.html
│   ├── package.json
│   ├── vite.config.js
│   └── tailwind.config.js
│
├── backend/                         # Spring Boot application
│   ├── src/main/java/com/bookmyroute/
│   │   ├── config/                  # Security, JPA, Admin bootstrap
│   │   ├── controller/              # REST API controllers
│   │   ├── dto/
│   │   │   ├── request/             # Request payload DTOs
│   │   │   └── response/            # Response payload DTOs
│   │   ├── entity/                  # JPA entities
│   │   ├── enums/                   # Enums (BusType, Role, etc.)
│   │   ├── exception/               # Global exception handling
│   │   ├── repository/              # Spring Data JPA repositories
│   │   ├── security/                # JWT filter, utils, UserDetails
│   │   ├── service/                 # Service interfaces
│   │   │   └── impl/                # Service implementations
│   │   └── BookMyRouteApplication.java
│   ├── src/main/resources/
│   │   └── application.properties
│   └── pom.xml
│
├── .gitignore
└── README.md
```

---

## ✅ Prerequisites

Make sure the following are installed on your system:

| Tool | Version | Download |
|------|---------|---------|
| Java JDK | 17+ | https://adoptium.net |
| Maven | 3.8+ | https://maven.apache.org |
| Node.js | 18+ | https://nodejs.org |
| MySQL | 8.0+ | https://dev.mysql.com/downloads |

---

## 🚀 Getting Started

### 1. Database Setup

Open your MySQL client and run:

```sql
CREATE DATABASE bookmyroute;
```

> Spring Boot will automatically create all tables on first startup (`spring.jpa.hibernate.ddl-auto=update`).

---

### 2. Backend Setup

```bash
cd backend
```

If your MySQL credentials differ from the defaults (`root / root`), update `src/main/resources/application.properties`:

```properties
spring.datasource.username=YOUR_MYSQL_USERNAME
spring.datasource.password=YOUR_MYSQL_PASSWORD
```

Then start the backend:

```bash
mvn spring-boot:run
```

The backend runs at: **http://localhost:8080**

---

### 3. Frontend Setup

Open a new terminal:

```bash
cd frontend
npm install
npm run dev
```

The frontend runs at: **http://localhost:3000**

Open **http://localhost:3000** in your browser. 🎉

---

## ⚙️ Environment Variables

All config lives in `backend/src/main/resources/application.properties`. Key settings:

| Property | Default | Description |
|----------|---------|-------------|
| `spring.datasource.username` | `root` | MySQL username |
| `spring.datasource.password` | `root` | MySQL password |
| `spring.datasource.url` | `jdbc:mysql://localhost:3306/bookmyroute` | DB connection URL |
| `app.admin.email` | `book.my.route2026@gmail.com` | Default admin email |
| `app.admin.password` | `Admin@12345` | Default admin password |
| `openai.api.key` | *(empty)* | OpenAI key for chatbot feature |
| `openai.model` | `gpt-5.4-mini` | OpenAI model to use |
| `spring.mail.username` | *(empty)* | SMTP username, usually your Gmail address |
| `spring.mail.password` | *(empty)* | SMTP password, use a Gmail App Password for Gmail |
| `app.mail.enabled` | `true` | Enables or disables outgoing email notifications |
| `app.mail.from` | `MAIL_USERNAME` | Sender email address |
| `app.mail.sender-name` | `BookMyRoute` | Sender display name |
| `app.jwt.expiration-ms` | `86400000` | JWT token expiry (24 hours) |
| `app.cors.allowed-origins` | `http://localhost:3000,http://localhost:5173` | Allowed CORS origins |

You can also pass these as environment variables:

```bash
DB_USERNAME=myuser DB_PASSWORD=mypass mvn spring-boot:run
```

For Gmail email notifications, enable 2-Step Verification in your Google account, create an App Password, then start the backend with:

```bash
MAIL_USERNAME=yourgmail@gmail.com MAIL_PASSWORD=your-app-password MAIL_FROM=yourgmail@gmail.com mvn spring-boot:run
```

After admin login, you can verify SMTP without creating a booking:

```bash
curl -X POST "http://localhost:8080/api/admin/email/test?to=recipient@example.com" \
  -H "Authorization: Bearer YOUR_ADMIN_JWT"
```

---

## 🔐 Default Credentials

| Role | Email | Password |
|------|-------|---------|
| Admin | `book.my.route2026@gmail.com` | `Admin@12345` |

> The admin account is auto-created on first startup. Register new user accounts from the `/register` page.

---

## 📡 API Overview

All endpoints are prefixed with `/api`.

| Module | Base Path | Description |
|--------|-----------|-------------|
| Auth | `/api/auth` | Register, login, token refresh |
| Routes | `/api/routes` | Browse available bus routes |
| Buses | `/api/buses` | Bus information |
| Schedules | `/api/schedules` | Search schedules by route & date |
| Bookings | `/api/bookings` | Create & view bookings |
| Admin | `/api/admin` | Admin-only management endpoints |
| Chatbot | `/api/chatbot` | AI chatbot messages |

---

## 🗄️ Database Schema

The application manages the following core entities:

```
User ──< Booking >── Schedule ──< Bus
                         │
                      Route
Booking ──< BookingSeat
Booking ──  Payment
Bus ──< Seat
```

| Entity | Description |
|--------|-------------|
| `User` | Registered users with ROLE_USER or ROLE_ADMIN |
| `Bus` | Bus details — number, name, type, seats, amenities |
| `Route` | Source to destination route definition |
| `Schedule` | A bus running a route on a specific date & time |
| `Seat` | Individual seats belonging to a bus |
| `Booking` | A user's reservation on a schedule |
| `BookingSeat` | Specific seats tied to a booking |
| `Payment` | Payment record linked to a booking |

---

## 🤖 Chatbot Setup (Optional)

The chatbot requires an OpenAI API key. Add it to `application.properties`:

```properties
openai.api.key=sk-your-openai-api-key-here
openai.model=gpt-5.4-mini
```

Without a key, the chatbot feature will be inactive.

---

## 📦 Build for Production

### Frontend
```bash
cd frontend
npm run build
# Output in frontend/dist/
```

### Backend
```bash
cd backend
mvn clean package
java -jar target/bookmyroute-1.0.0.jar
```
