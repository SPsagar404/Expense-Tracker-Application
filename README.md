# 💰 Personal Expense Manager

A full-stack personal finance application built with **Spring Boot** (Java 17) and **React + TypeScript** (Vite). Track expenses, manage budgets, view spending reports, and gain insights – all in a sleek dark-themed UI.

---

## ✨ Features

| Module            | Highlights |
|-------------------|------------|
| **Authentication** | JWT-based login/signup, refresh tokens, BCrypt password hashing, rate limiting |
| **Transactions**   | Full CRUD, CSV import, date/category/merchant filtering, pagination |
| **Budgets**        | Monthly category budgets with real-time utilization tracking & alerts |
| **Reports**        | Monthly summaries with category breakdown & daily spending charts (Redis-cached) |
| **Insights**       | Rule-based flags for high spending & duplicate subscriptions |
| **GDPR**           | Data export & account deletion endpoints |
| **Dashboard**      | Pie chart, bar chart, stats cards, recent transactions, quick-add |

---

## 🏗️ Tech Stack

### Backend
- Java 17, Spring Boot 3.x
- Spring Security (JWT), Spring Data JPA
- PostgreSQL 16, Redis 7
- Flyway migrations, OpenAPI/Swagger
- JUnit 5 + Mockito

### Frontend
- React 19, TypeScript
- Vite, Tailwind CSS v4
- React Router v7, Axios
- Recharts, Heroicons
- Vitest + React Testing Library

### DevOps
- Docker (multi-stage builds)
- Docker Compose
- GitHub Actions CI

---

## 🚀 Quick Start

### Prerequisites
- **Docker & Docker Compose** (recommended), OR:
- Java 17+, Maven, Node 20+, PostgreSQL, Redis

### Option 1: Docker Compose (recommended)

```bash
# Clone the repo
git clone <repo-url>
cd "Expense Tracker App"

# Copy and configure environment
cp .env.example .env

# Start all services
docker-compose up --build
```

- **Frontend**: http://localhost
- **Backend API**: http://localhost:8080
- **Swagger UI**: http://localhost:8080/swagger-ui.html

### Option 2: Local Development

#### Backend

```bash
cd backend

# Ensure PostgreSQL is running with a database called 'expensemanager'
# Ensure Redis is running on port 6379

# Set environment variables (or edit application.yml)
export DB_HOST=localhost DB_PORT=5432 DB_NAME=expensemanager
export DB_USERNAME=postgres DB_PASSWORD=postgres
export REDIS_HOST=localhost REDIS_PORT=6379
export JWT_SECRET="YourSuperSecretKeyForJwtTokenGeneration2024MustBeLongEnough!"

# Build and run
mvn spring-boot:run
```

#### Frontend

```bash
cd frontend

npm install
npm run dev
```

Frontend runs at http://localhost:3000 and proxies API calls to http://localhost:8080.

---

## 🔑 Demo Account

A seeded demo user is available after running Flyway migrations:

| Field    | Value                    |
|----------|--------------------------|
| Email    | demo@expensemanager.com  |
| Password | demo1234                 |

This account comes pre-loaded with 30 transactions, 7 budgets, and 5 subscriptions.

---

## 📁 Project Structure

```
Expense Tracker App/
├── backend/
│   ├── src/main/java/com/expensemanager/
│   │   ├── config/         # Security, Redis, OpenAPI configs
│   │   ├── controller/     # REST controllers
│   │   ├── dto/            # Request/response DTOs
│   │   ├── entity/         # JPA entities
│   │   ├── exception/      # Custom exceptions & global handler
│   │   ├── repository/     # Spring Data JPA repositories
│   │   ├── security/       # JWT util, filter, entry point
│   │   ├── service/        # Business logic
│   │   └── util/           # SecurityUtils
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   └── db/migration/   # Flyway SQL scripts
│   ├── src/test/           # Unit tests
│   ├── pom.xml
│   └── Dockerfile
├── frontend/
│   ├── src/
│   │   ├── api/            # Axios client & API modules
│   │   ├── components/     # Layout + reusable components
│   │   ├── context/        # AuthContext
│   │   ├── pages/          # Login, Signup, Dashboard, etc.
│   │   └── test/           # Frontend tests
│   ├── index.html
│   ├── vite.config.ts
│   ├── nginx.conf
│   └── Dockerfile
├── docker-compose.yml
├── .env.example
├── .github/workflows/ci.yml
└── README.md
```

---

## 📡 API Endpoints

| Method | Endpoint                     | Auth | Description |
|--------|------------------------------|------|-------------|
| POST   | `/api/v1/auth/signup`        | No   | Register new user |
| POST   | `/api/v1/auth/login`         | No   | Login, get JWT |
| POST   | `/api/v1/auth/refresh`       | No   | Refresh access token |
| GET    | `/api/v1/transactions`       | Yes  | List with filters & pagination |
| POST   | `/api/v1/transactions`       | Yes  | Create transaction |
| PUT    | `/api/v1/transactions/{id}`  | Yes  | Update transaction |
| DELETE | `/api/v1/transactions/{id}`  | Yes  | Delete transaction |
| POST   | `/api/v1/transactions/import-csv` | Yes | Import CSV |
| GET    | `/api/v1/budgets`            | Yes  | List budgets |
| POST   | `/api/v1/budgets`            | Yes  | Create budget |
| PUT    | `/api/v1/budgets/{id}`       | Yes  | Update budget |
| DELETE | `/api/v1/budgets/{id}`       | Yes  | Delete budget |
| GET    | `/api/v1/reports/monthly`    | Yes  | Monthly report |
| GET    | `/api/v1/insights/summary`   | Yes  | Spending insights |
| GET    | `/api/v1/gdpr/export`        | Yes  | Export user data |
| DELETE | `/api/v1/gdpr/delete`        | Yes  | Delete user data |

Full interactive docs at `/swagger-ui.html` when running.

---

## 🧪 Running Tests

```bash
# Backend unit tests
cd backend
mvn test

# Frontend tests
cd frontend
npx vitest run
```

---

## 📝 Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `DB_HOST` | localhost | PostgreSQL host |
| `DB_PORT` | 5432 | PostgreSQL port |
| `DB_NAME` | expensemanager | Database name |
| `DB_USERNAME` | postgres | Database user |
| `DB_PASSWORD` | postgres | Database password |
| `REDIS_HOST` | localhost | Redis host |
| `REDIS_PORT` | 6379 | Redis port |
| `JWT_SECRET` | — | JWT signing secret (min 32 chars) |
| `JWT_ACCESS_TOKEN_EXPIRATION` | 900000 | Access token TTL (ms) |
| `JWT_REFRESH_TOKEN_EXPIRATION` | 604800000 | Refresh token TTL (ms) |
| `CORS_ORIGINS` | http://localhost:3000 | Allowed CORS origins |

---

## 📄 License

MIT License – see [LICENSE](LICENSE) for details.
