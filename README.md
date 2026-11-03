# Invoice Platform

A full-stack invoicing and billing application with a React frontend and a Spring Boot API. The backend uses PostgreSQL and Flyway database migrations.

## Features

- User authentication and business setup
- Customer and product management
- Invoice creation, payment tracking, and document handling
- Sales reports and CSV/Excel exports
- Scheduled invoice reminders

## Technology

- **Frontend:** React, Vite, React Router
- **Backend:** Java 21, Spring Boot 3, Spring Data JPA, Spring Security
- **Database:** PostgreSQL 16, Flyway
- **Local infrastructure:** Docker Compose

## Requirements

- Java 21
- Maven 3.9 or later
- Node.js and npm
- Docker Desktop (for the local PostgreSQL database)

## Run locally

1. Start PostgreSQL:

   ```powershell
   cd backend
   docker compose up -d postgres
   ```

2. In a terminal, start the backend:

   ```powershell
   cd backend
   mvn spring-boot:run
   ```

   The API listens on `http://localhost:8080/api/v1`. Database migrations run automatically on startup.

3. In another terminal, install frontend dependencies and start Vite:

   ```powershell
   cd frontend
   npm ci
   npm run dev
   ```

   Open the local URL printed by Vite (by default, `http://localhost:5173`). Vite proxies `/api` requests to `http://localhost:8080`. To use another backend address, set `VITE_BACKEND_URL` in the frontend environment.

The local Compose database uses the development database name and credentials in `backend/docker-compose.yml`. Do not use these defaults in a deployed environment.

## Configuration

The backend reads configuration from environment variables. Common database variables are `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, and `DB_PASSWORD`. Optional integrations and settings include `JWT_SECRET`, `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `REMINDER_ENABLED`, `STORAGE_TYPE`, and `STORAGE_PATH`.

Keep credentials and other secrets in local environment files or deployment secret settings. Never commit real `.env` files or production secrets.

## Tests and build

Run frontend tests and create a production build:

```powershell
cd frontend
npm test
npm run build
```

Run backend tests from the backend directory:

```powershell
cd backend
mvn test
```

Some backend tests may require Docker for Testcontainers.
