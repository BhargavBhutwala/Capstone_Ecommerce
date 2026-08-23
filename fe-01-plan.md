# FE-01: Frontend Foundation and Build Setup Plan

## Top-Level Overview

This plan implements **Milestone FE-01** only — the frontend scaffold. It creates a standalone React + TypeScript + Vite application under a top-level `frontend/` directory. No authentication logic, API services, pages, or business logic are included. The backend Spring Boot application must not be modified.

**Scope boundary:** Everything in this plan corresponds to Sub-Tasks 1 and 2 of `frontend-implementation-plan.md` — the foundation and directory structure. No routing shell, no auth, no API client, no pages.

**Package manager:** npm (standard default; no alternative is specified in any project document).

**Node version:** Node.js 20 LTS (current LTS as of planning; a `.nvmrc` will be created).

**Git milestone commit message:** `feat(frontend): scaffold React TypeScript Vite application`

---

## Sub-Task A — Initialize Vite + React + TypeScript project

- **Intent** — Create the bare `frontend/` directory with a working Vite React-TS project so the dev and production builds are immediately functional.
- **Expected Outcomes**
  - `frontend/` directory exists at the project root, peer to `src/`, `docs/`, and `pom.xml`.
  - `npm install` succeeds.
  - `npm run dev` starts the Vite dev server.
  - `npm run build` produces a production build in `frontend/dist/`.
  - `npm run preview` serves the production build locally.
- **Todo List**
  1. Create `frontend/package.json` with the project name `ebookstore-frontend`, description, version, and the following dependencies and devDependencies:
     - **dependencies:** `react`, `react-dom`, `react-router-dom`
     - **devDependencies:** `@types/react`, `@types/react-dom`, `typescript`, `vite`, `@vitejs/plugin-react`, `@eslint/js`, `typescript-eslint`, `eslint`, `eslint-plugin-react-hooks`, `eslint-plugin-react-refresh`
  2. Create `frontend/vite.config.ts` — configure Vite with the React plugin and a local dev proxy that forwards `/api` requests to `http://localhost:8080` to avoid CORS friction during development.
  3. Create `frontend/tsconfig.json` and `frontend/tsconfig.app.json` — base TypeScript configuration targeting ES2020, bundler module resolution, strict mode, and JSX preserve for Vite.
  4. Create `frontend/tsconfig.node.json` — TypeScript configuration for Vite config file itself.
  5. Create `frontend/index.html` — the Vite HTML entry point referencing `src/main.tsx`.
  6. Create `frontend/.nvmrc` with Node version `20`.
  7. Create `frontend/.gitignore` — exclude `node_modules/`, `dist/`, `.env.local`, and `.env.*.local`.
  8. Create `frontend/eslint.config.js` — ESLint flat config with React hooks and refresh rules.
- **Relevant Context**
  - `frontend-implementation-plan.md` Sub-Task 1, lines 11–30
  - `frontend-implementation-plan.md` "Frontend technology and build setup" section, lines 279–291
  - Backend API server URL: `http://localhost:8080/api` (from `docs/03-openapi-specification.yaml` line 15)
- **Status** — [ ] pending

---

## Sub-Task B — Create frontend source entry point and App root

- **Intent** — Provide the minimal working `src/main.tsx` and `src/App.tsx` so the Vite build has a valid React application to compile, without any business logic, routing, or feature code.
- **Expected Outcomes**
  - `src/main.tsx` mounts a React 18 root.
  - `src/App.tsx` renders a placeholder that confirms the app is running.
  - The dev build loads in the browser without errors.
- **Todo List**
  1. Create `frontend/src/main.tsx` — React 18 `createRoot` mounting `<App />` into `#root`.
  2. Create `frontend/src/App.tsx` — minimal functional component returning a placeholder `<div>` so the build succeeds. No routing, no logic.
  3. Create `frontend/src/vite-env.d.ts` — standard Vite env type reference.
  4. Create `frontend/src/index.css` — minimal CSS reset or empty file so main.tsx import does not fail.
- **Relevant Context**
  - `frontend-implementation-plan.md` directory structure, lines 295–332
  - Sub-Task 2 of `frontend-implementation-plan.md`, lines 32–47
- **Status** — [ ] pending

---

## Sub-Task C — Create the prescribed directory structure

- **Intent** — Establish all domain-oriented source directories as empty placeholder modules so the project structure is stable before any feature code is written. This matches the prescribed structure from `frontend-implementation-plan.md` exactly.
- **Expected Outcomes**
  - All prescribed directories exist under `frontend/src/`.
  - Each leaf directory contains a `.gitkeep` or a minimal index file so it is tracked by git.
  - The structure is immediately usable by subsequent sub-tasks without reorganization.
- **Todo List**
  1. Create the following directories under `frontend/src/` with a `.gitkeep` in each empty leaf:
     ```
     src/app/
     src/app/providers/
     src/app/layout/
     src/routes/
     src/api/
     src/features/auth/
     src/features/catalog/
     src/features/cart/
     src/features/address/
     src/features/checkout/
     src/features/payment/
     src/features/orders/
     src/components/ui/
     src/components/states/
     src/components/forms/
     src/hooks/
     src/utils/
     src/test/
     src/types/
     ```
  2. Move `App.tsx` into `src/app/App.tsx` and update the import in `src/main.tsx` accordingly.
- **Relevant Context**
  - Prescribed structure: `frontend-implementation-plan.md` lines 293–332
  - Backend modular structure alignment: `AGENTS.md` lines 110–130
- **Status** — [ ] pending

---

## Sub-Task D — Environment configuration

- **Intent** — Provide environment variable configuration so the API base URL is environment-driven from day one, without hard-coding any URL.
- **Expected Outcomes**
  - `frontend/.env.example` documents all required frontend env vars with safe example values.
  - `frontend/.env.local` (gitignored) can be created by a developer by copying `.env.example`.
  - `VITE_API_BASE_URL` is defined and accessible via `import.meta.env`.
- **Todo List**
  1. Create `frontend/.env.example` with `VITE_API_BASE_URL=http://localhost:8080/api`.
  2. Confirm `.env.local` is listed in `frontend/.gitignore` (covered in Sub-Task A step 7).
  3. Do not create an actual `.env.local` file — developers copy `.env.example`.
- **Relevant Context**
  - `frontend-implementation-plan.md` "Environment configuration plan", lines 554–560
  - Security rules: never commit secrets or `.env` files with real values
- **Status** — [ ] pending

---

## Sub-Task E — Frontend README

- **Intent** — Provide clear local startup instructions that stand independently from the backend Maven instructions, so any developer can start the frontend without reading the entire project.
- **Expected Outcomes**
  - `frontend/README.md` exists with prerequisites, setup steps, available scripts, environment configuration, and how to run alongside the backend.
  - No backend Maven commands are duplicated — the README links to `mvp-implementation-plan.md` for backend setup.
- **Todo List**
  1. Create `frontend/README.md` covering:
     - Prerequisites (Node 20, npm)
     - Clone/install: `cd frontend && npm install`
     - Environment setup: copy `.env.example` to `.env.local`
     - Dev server: `npm run dev` (Vite on port 5173)
     - Production build: `npm run build`
     - Preview build: `npm run preview`
     - Lint: `npm run lint`
     - Running alongside the backend (backend must be running on port 8080; Vite proxy forwards `/api` automatically in dev)
     - Note that `frontend/` is independent from the Maven build and has its own `node_modules/`
- **Relevant Context**
  - `frontend-implementation-plan.md` Sub-Task 1 item 6, line 25
  - `frontend-implementation-plan.md` "Local development setup plan", lines 562–567
- **Status** — [ ] pending

---

## Validation Checklist (to verify after implementation)

| Check | Command |
|---|---|
| Install succeeds | `cd frontend && npm install` |
| Dev build starts | `npm run dev` |
| Production build succeeds | `npm run build` |
| Preview serves | `npm run preview` |
| Lint passes | `npm run lint` |

---

## Files to be Created

| File | Purpose |
|---|---|
| `frontend/package.json` | npm project definition, scripts, dependencies |
| `frontend/vite.config.ts` | Vite + React plugin + dev proxy config |
| `frontend/tsconfig.json` | Root TypeScript config |
| `frontend/tsconfig.app.json` | App-level TypeScript config |
| `frontend/tsconfig.node.json` | Vite config TypeScript config |
| `frontend/index.html` | Vite HTML entry point |
| `frontend/.nvmrc` | Node version pin |
| `frontend/.gitignore` | Frontend-specific git exclusions |
| `frontend/eslint.config.js` | ESLint flat config |
| `frontend/.env.example` | Documented environment variable template |
| `frontend/README.md` | Frontend local development guide |
| `frontend/src/main.tsx` | React 18 root mount |
| `frontend/src/App.tsx` | Placeholder app component (later moved to src/app/) |
| `frontend/src/app/App.tsx` | App root component (after directory structure step) |
| `frontend/src/vite-env.d.ts` | Vite environment type declarations |
| `frontend/src/index.css` | Minimal CSS entry |
| `frontend/src/app/providers/` | Empty, `.gitkeep` |
| `frontend/src/app/layout/` | Empty, `.gitkeep` |
| `frontend/src/routes/` | Empty, `.gitkeep` |
| `frontend/src/api/` | Empty, `.gitkeep` |
| `frontend/src/features/auth/` | Empty, `.gitkeep` |
| `frontend/src/features/catalog/` | Empty, `.gitkeep` |
| `frontend/src/features/cart/` | Empty, `.gitkeep` |
| `frontend/src/features/address/` | Empty, `.gitkeep` |
| `frontend/src/features/checkout/` | Empty, `.gitkeep` |
| `frontend/src/features/payment/` | Empty, `.gitkeep` |
| `frontend/src/features/orders/` | Empty, `.gitkeep` |
| `frontend/src/components/ui/` | Empty, `.gitkeep` |
| `frontend/src/components/states/` | Empty, `.gitkeep` |
| `frontend/src/components/forms/` | Empty, `.gitkeep` |
| `frontend/src/hooks/` | Empty, `.gitkeep` |
| `frontend/src/utils/` | Empty, `.gitkeep` |
| `frontend/src/test/` | Empty, `.gitkeep` |
| `frontend/src/types/` | Empty, `.gitkeep` |

## Files to Not Modify

- `AGENTS.md`
- `pom.xml`
- `src/` (Spring Boot source)
- `docs/` (requirements, data model, OpenAPI)
- `mvp-implementation-plan.md`
- `frontend-implementation-plan.md`
- Any Flyway migrations
- Any backend test files

## Dependencies to Add (frontend only)

**Runtime dependencies:**
| Package | Purpose |
|---|---|
| `react` | Core React library |
| `react-dom` | React DOM renderer |
| `react-router-dom` | Client-side routing |

**Dev dependencies:**
| Package | Purpose |
|---|---|
| `typescript` | TypeScript compiler |
| `vite` | Build tool and dev server |
| `@vitejs/plugin-react` | React fast refresh support |
| `@types/react` | React TypeScript types |
| `@types/react-dom` | React DOM TypeScript types |
| `eslint` | Linter |
| `@eslint/js` | ESLint JS rules |
| `typescript-eslint` | TypeScript ESLint integration |
| `eslint-plugin-react-hooks` | React hooks lint rules |
| `eslint-plugin-react-refresh` | Vite React refresh lint rules |

No testing libraries, no HTTP client, no React Query, no form libraries — those are for later sub-tasks.
