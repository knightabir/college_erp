# CollegeERP — Frontend

React 19 + Vite SPA for the College ERP backend (`../backend/college`). Talks to the Spring Boot
API over REST/JSON; authenticates with JWT access/refresh tokens; UI built on shadcn (aria-vega
style, React Aria Components) + Tailwind v4.

## Setup

```bash
npm install
cp .env.example .env   # already present locally; edit VITE_API_BASE_URL if the backend isn't on :8080
npm run dev
```

Requires the backend running (`../backend/college`, `./mvnw spring-boot:run`) against a live
Postgres instance — see the backend's own `CLAUDE.md` for that setup. There is no mock-data mode;
every page fetches real data and will show its `ErrorState`/empty state if the backend is
unreachable or a given module has no data yet.

```bash
npm run dev       # start the Vite dev server
npm run build     # production build (fails the same way CI would on a real error)
npm run lint      # ESLint
npm run preview   # preview a production build locally
```

## Architecture

- **Auth** (`src/stores/auth-store.js`, `src/api/auth.js`): JWT access/refresh tokens + the
  logged-in user's profile/roles (from `LoginResponse` — there's no `GET /me`, so roles are only
  as fresh as the last login) persisted to `localStorage` via Zustand. `src/lib/api-client.js`
  attaches the access token to every request and silently refreshes once on a 401 before giving
  up and routing to `/login` (see `SessionGuard`).
- **RBAC** (`src/lib/rbac.js`): mirrors `module.auth.domain.UserRole` and each backend
  controller's `@PreAuthorize` role list. `NAV_ACCESS` gates which sidebar items / routes a role
  can reach at all (`ProtectedRoute` + `RequireRole` in `src/components/auth/`); individual pages
  further gate specific buttons/actions against the same role lists their backend endpoint
  actually enforces. This is a UX layer only — the backend re-authorizes every request
  independently regardless of what the client believes.
- **Server data**: TanStack Query throughout (`src/api/*.js` — one file per backend module, each
  exporting both plain async functions and the React Query hooks pages use). Zustand is reserved
  for the auth session only, per project convention — no server data lives in a Zustand store.
- **Errors**: every backend failure is normalized to an `AppError` (`src/lib/api-error.js`) from
  the RFC 7807 response shape the backend returns — field-level validation errors render inline
  on forms, everything else surfaces via `ErrorState` (queries) or a toast (mutations, see
  `src/lib/query-client.js`'s policy comment). Nothing renders a raw axios/network error to a user.
- **Shared UI** (`src/components/shared/`): `DataTable`, `FormDialog`, `ConfirmDialog`,
  `PageHeader`/`PageContainer`, `StatusBadge`, `EmptyState`/`ErrorState`/loading skeletons, and
  `AcademicScopeSelector` (the Department → Course → Batch → Term → Subject cascade several
  modules need, since the backend has no flat "list everything" endpoint for most of this data).

## Structure

```
src/
  api/            One file per backend module (auth, academic, students, attendance, ...) —
                   raw fetchers + React Query hooks together.
  components/
    auth/          ProtectedRoute, RequireRole, SessionGuard
    layout/         AppLayout, SidebarNav (role-filtered)
    shared/         Reusable page/table/dialog/state primitives
    ui/             shadcn components (generated — avoid hand-editing)
  lib/            api-client, api-error, rbac, query-client, utils
  pages/          One folder per module, matching the sidebar
  providers/      ThemeProvider (light/dark/system, next-themes)
  stores/         auth-store (Zustand)
```

## Known gaps (matches what the backend actually exposes)

A few pages are intentionally narrower than "full CRUD for every table" because the backend
endpoint doesn't exist yet either (see the backend's own `CLAUDE.md` for the authoritative list
of what's built vs. deferred per module — Reports, payroll, admissions/placement UI, and a few
others aren't wired into this frontend for the same reason they're absent from the API). Notably:

- Students has no "list all students" endpoint — listing is always scoped to a batch/section.
- Attendance has no "list sessions" endpoint — a session is opened, marked, and submitted in one
  flow rather than browsed.
- Fee payment is recorded by finance staff, not self-service online checkout (no payment gateway
  integration exists in this system, by design — see ADR-001 air-gap-first).
- Library circulation issues by user ID (there's no cross-module user search endpoint available
  to library staff) rather than a name-search picker.
