# E-Bookstore Frontend

React + TypeScript + Vite frontend for the E-Bookstore MVP.

The frontend lives entirely under `frontend/` and is independent of the Spring Boot Maven build.
Backend setup instructions are in [`mvp-implementation-plan.md`](../mvp-implementation-plan.md).

---

## Prerequisites

| Tool | Version |
|------|---------|
| Node.js | 20 LTS ([nvm](https://github.com/nvm-sh/nvm) recommended) |
| npm | 10+ (bundled with Node 20) |

If you use nvm, run the following from the `frontend/` directory:

```bash
nvm use
```

This reads `.nvmrc` and switches to Node 20 automatically.

---

## Installation

From the **project root**:

```bash
cd frontend
npm install
```

---

## Environment configuration

Copy the example env file and adjust values for your local setup:

```bash
cp .env.example .env.local
```

| Variable | Default | Purpose |
|----------|---------|---------|
| `VITE_API_BASE_URL` | `http://localhost:8080/api` | Spring Boot backend API base URL |

> **Note:** During local development the Vite dev server automatically proxies all
> `/api` requests to `http://localhost:8080`, so you rarely need to change this value.
> `.env.local` is gitignored and must never contain secrets.

---

## Available scripts

All commands are run from inside the `frontend/` directory.

| Command | Description |
|---------|-------------|
| `npm run dev` | Start Vite dev server on [http://localhost:5173](http://localhost:5173) |
| `npm run build` | Type-check and produce a production build in `dist/` |
| `npm run preview` | Serve the production build locally for smoke-testing |
| `npm run lint` | Run ESLint across all TypeScript source files |

---

## Running alongside the backend

1. Start the Spring Boot backend (see [`mvp-implementation-plan.md`](../mvp-implementation-plan.md)):
   ```bash
   # From the project root
   ./mvnw spring-boot:run
   ```
   The backend listens on `http://localhost:8080`.

2. In a separate terminal, start the Vite dev server:
   ```bash
   cd frontend
   npm run dev
   ```
   The frontend is available at `http://localhost:5173`.

3. The Vite dev server proxies all `/api` requests to `http://localhost:8080` — no CORS
   configuration change is needed on the backend during development.

---

## Project structure

```
frontend/
├── public/                  Static assets served as-is
├── src/
│   ├── app/                 App shell, providers, layout
│   │   ├── App.tsx
│   │   ├── providers/       React context providers (added in later milestones)
│   │   └── layout/          Shared layout components (added in later milestones)
│   ├── routes/              Route definitions and guards (added in FE-02)
│   ├── api/                 API client and domain service modules (added in FE-02)
│   ├── features/            Domain feature modules
│   │   ├── auth/            (added in FE-02)
│   │   ├── catalog/         (added in FE-03)
│   │   ├── cart/            (added in FE-04)
│   │   ├── address/         (added in FE-05)
│   │   ├── checkout/        (added in FE-05)
│   │   ├── payment/         (added in FE-06)
│   │   └── orders/          (added in FE-07)
│   ├── components/          Shared UI components
│   │   ├── ui/              Generic UI primitives
│   │   ├── states/          Loading, empty, error states
│   │   └── forms/           Shared form field components
│   ├── hooks/               Shared custom React hooks
│   ├── utils/               Utility functions
│   ├── types/               Shared TypeScript types (OpenAPI-aligned DTOs)
│   ├── test/                Test helpers and setup
│   ├── main.tsx             React 18 entry point
│   ├── index.css            Global CSS reset
│   └── vite-env.d.ts        Vite environment type declarations
├── .env.example             Environment variable template
├── .gitignore               Frontend-specific git exclusions
├── .nvmrc                   Node version pin (20)
├── eslint.config.js         ESLint flat config
├── index.html               Vite HTML entry point
├── package.json             npm project definition
├── tsconfig.json            TypeScript project references root
├── tsconfig.app.json        App TypeScript config
├── tsconfig.node.json       Vite config TypeScript config
└── vite.config.ts           Vite configuration with dev proxy
```

---

## Notes

- `frontend/` is **not** part of the Maven build. Running `mvn` from the project root does
  not install Node dependencies or build the frontend.
- `node_modules/` and `dist/` are gitignored — never commit them.
- All frontend dependencies are scoped to `frontend/package.json` and have no effect on
  the Spring Boot backend.
