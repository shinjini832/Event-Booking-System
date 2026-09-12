# Deployment Guide — Render & Vercel

This guide outlines the exact step-by-step procedure to deploy the **Event Booking Platform** to **Render** (Backend + Database) and **Vercel** (Frontend).

---

## 1. Database Setup (Cloud MySQL or Render PostgreSQL)

### Option A: Free Managed Cloud MySQL (e.g. Aiven / Railway / PlanetScale)
1. Create a free MySQL database instance on [Aiven.io](https://aiven.io) or [Railway.app](https://railway.app).
2. Note down your Database credentials:
   - **Host / Port**: e.g., `mysql-12345.aivencloud.com:12345`
   - **Database Name**: `event_booking_db`
   - **Username**: `u12345`
   - **Password**: `p12345`

---

## 2. Deploy Backend on Render

1. Log into your [Render Dashboard](https://dashboard.render.com).
2. Click **New +** -> **Web Service**.
3. Connect your GitHub Repository: `Event Ticket Booking System`.
4. Configure Web Service details:
   - **Name**: `event-booking-backend`
   - **Root Directory**: `event-booking-backend`
   - **Environment**: `Docker` (or `Java` if using Maven build)
   - **Dockerfile Path**: `./Dockerfile` (or `Dockerfile`)
   - **Region**: Choose closest to you (e.g. Frankfurt / Singapore / Oregon)
5. Add **Environment Variables** under the **Environment** tab:
   - `SPRING_DATASOURCE_URL` = `jdbc:mysql://<YOUR-MYSQL-HOST>:<PORT>/<DATABASE-NAME>?createDatabaseIfNotExist=true&useSSL=true&allowPublicKeyRetrieval=true&serverTimezone=UTC`
   - `SPRING_DATASOURCE_USERNAME` = `<YOUR-MYSQL-USER>`
   - `SPRING_DATASOURCE_PASSWORD` = `<YOUR-MYSQL-PASSWORD>`
   - `SPRING_MAIL_HOST` = `smtp.gmail.com`
   - `SPRING_MAIL_PORT` = `587`
   - `SPRING_MAIL_USERNAME` = `shinjini832@gmail.com`
   - `SPRING_MAIL_PASSWORD` = `iqkvkqneghkgazyq`
   - `JWT_SECRET` = `404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970`
   - `CORS_ALLOWED_ORIGINS` = `https://<YOUR-VERCEL-APP-NAME>.vercel.app`
6. Click **Create Web Service**.
7. Render will build the Docker container, run Flyway migrations automatically, and publish your API at `https://event-booking-backend.onrender.com`.

---

## 3. Deploy Frontend on Vercel

1. Log into your [Vercel Dashboard](https://vercel.com).
2. Click **Add New...** -> **Project**.
3. Import your GitHub repository `Event Ticket Booking System`.
4. Configure Project settings:
   - **Framework Preset**: `Vite`
   - **Root Directory**: Select `event-booking-frontend`
   - **Build Command**: `npm run build`
   - **Output Directory**: `dist`
5. Add **Environment Variables**:
   - `VITE_API_BASE_URL` = `https://event-booking-backend.onrender.com/api`
6. Click **Deploy**.
7. Vercel will build the React application and issue your live frontend URL (e.g. `https://event-booking-frontend.vercel.app`).

---

## 4. Final Live Connection Verification

1. Copy your Vercel URL (e.g., `https://event-booking-frontend.vercel.app`).
2. Go back to **Render Dashboard** -> `event-booking-backend` -> **Environment**.
3. Update `CORS_ALLOWED_ORIGINS` to match your exact Vercel URL:
   ```env
   CORS_ALLOWED_ORIGINS=https://event-booking-frontend.vercel.app
   ```
4. Click **Save Changes** (Render will re-deploy with CORS permissions active).
5. Open your Vercel URL in your browser, register a new account, pick seats, and verify real-time seat locking and email confirmations!
