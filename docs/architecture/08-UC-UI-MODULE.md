# 08 — UI Module (Vue.js SPA)

## 1. Summary

The JobAgent frontend is a **Vue.js 3 Single Page Application (SPA)** served separately from the Spring Boot backend.  
It communicates with the backend exclusively via REST APIs and authenticates using OAuth 2.1 / OIDC tokens issued by the embedded Spring Authorization Server.

---

## 2. Technology Stack

| Layer              | Technology                                                       |
|--------------------|------------------------------------------------------------------|
| Framework          | Vue.js 3 (Composition API, `<script setup>`)                    |
| Language           | TypeScript 5                                                     |
| Build Tool         | Vite 6                                                           |
| Routing            | Vue Router 4                                                     |
| State Management   | Pinia                                                            |
| HTTP Client        | Axios (with interceptors for JWT refresh)                        |
| UI Component Lib   | PrimeVue 4 (accessible, enterprise-grade)                       |
| CSS Framework      | Tailwind CSS 4                                                   |
| Charting           | Chart.js + vue-chartjs (application analytics)                   |
| i18n               | Vue I18n (multi-language: EN, NL, DE, FR, ES)                    |
| Form Validation    | VeeValidate + Zod                                                |
| Testing            | Vitest (unit), Vue Test Utils, Playwright (E2E)                  |
| Linting            | ESLint + Prettier                                                |
| Containerisation   | Nginx Alpine Docker image (production), Vite dev server (local)  |

---

## 3. Project Structure

```
jobagent-ui/                          ← Separate root (sibling to backend or sub-folder)
├── public/
│   ├── favicon.ico
│   └── index.html
├── src/
│   ├── main.ts                       ← App bootstrap
│   ├── App.vue                       ← Root component
│   ├── router/
│   │   └── index.ts                  ← Vue Router config
│   ├── stores/                       ← Pinia stores
│   │   ├── auth.store.ts             ← Token management, user state
│   │   ├── cv.store.ts               ← CV upload & parsed data
│   │   ├── jobs.store.ts             ← Job search results, saved jobs
│   │   ├── motivation.store.ts       ← Motivation letters
│   │   └── applications.store.ts     ← Applications dashboard
│   ├── api/                          ← Axios API clients
│   │   ├── client.ts                 ← Base Axios instance + interceptors
│   │   ├── auth.api.ts
│   │   ├── cv.api.ts
│   │   ├── jobs.api.ts
│   │   ├── motivation.api.ts
│   │   └── applications.api.ts
│   ├── views/                        ← Page-level components (routed)
│   │   ├── auth/
│   │   │   ├── LoginView.vue
│   │   │   ├── RegisterView.vue
│   │   │   └── CallbackView.vue      ← OAuth2 callback handler
│   │   ├── dashboard/
│   │   │   └── DashboardView.vue
│   │   ├── cv/
│   │   │   ├── CvUploadView.vue
│   │   │   └── CvDetailView.vue
│   │   ├── jobs/
│   │   │   ├── JobSearchView.vue
│   │   │   ├── JobDetailView.vue
│   │   │   └── SavedJobsView.vue
│   │   ├── motivation/
│   │   │   ├── MotivationGenerateView.vue
│   │   │   └── MotivationDetailView.vue
│   │   ├── applications/
│   │   │   ├── ApplicationsListView.vue
│   │   │   ├── ApplicationDetailView.vue
│   │   │   └── ApplicationStatsView.vue
│   │   └── profile/
│   │       └── ProfileView.vue
│   ├── components/                   ← Reusable UI components
│   │   ├── layout/
│   │   │   ├── AppHeader.vue
│   │   │   ├── AppSidebar.vue
│   │   │   ├── AppFooter.vue
│   │   │   └── AppLayout.vue
│   │   ├── common/
│   │   │   ├── LoadingSpinner.vue
│   │   │   ├── ErrorAlert.vue
│   │   │   ├── ConfirmDialog.vue
│   │   │   └── EmptyState.vue
│   │   ├── cv/
│   │   │   ├── CvUploader.vue
│   │   │   ├── CvSkillBadges.vue
│   │   │   └── CvSummaryCard.vue
│   │   ├── jobs/
│   │   │   ├── JobCard.vue
│   │   │   ├── JobMatchScore.vue
│   │   │   └── JobFilters.vue
│   │   ├── motivation/
│   │   │   ├── LetterEditor.vue
│   │   │   └── LetterPreview.vue
│   │   └── applications/
│   │       ├── ApplicationKanban.vue
│   │       ├── ApplicationTimeline.vue
│   │       └── ApplicationStatsChart.vue
│   ├── composables/                  ← Reusable Composition API hooks
│   │   ├── useAuth.ts
│   │   ├── useApi.ts
│   │   ├── useNotification.ts
│   │   └── usePagination.ts
│   ├── types/                        ← TypeScript interfaces
│   │   ├── auth.types.ts
│   │   ├── cv.types.ts
│   │   ├── job.types.ts
│   │   ├── motivation.types.ts
│   │   └── application.types.ts
│   ├── utils/
│   │   ├── date.ts
│   │   ├── validators.ts
│   │   └── constants.ts
│   ├── i18n/
│   │   ├── index.ts
│   │   └── locales/
│   │       ├── en.json
│   │       ├── nl.json
│   │       ├── de.json
│   │       ├── fr.json
│   │       └── es.json
│   └── assets/
│       └── styles/
│           └── main.css              ← Tailwind imports + custom styles
├── tests/
│   ├── unit/                         ← Vitest unit tests
│   │   ├── stores/
│   │   ├── components/
│   │   └── composables/
│   └── e2e/                          ← Playwright E2E tests
│       ├── auth.spec.ts
│       ├── cv-upload.spec.ts
│       ├── job-search.spec.ts
│       ├── motivation.spec.ts
│       └── applications.spec.ts
├── .env                              ← Local env vars
├── .env.production                   ← Prod env vars
├── index.html
├── vite.config.ts
├── tsconfig.json
├── tailwind.config.ts
├── package.json
├── Dockerfile
└── nginx.conf                        ← Production Nginx config
```

---

## 4. Routing

```typescript
// src/router/index.ts
const routes = [
  // --- Public ---
  { path: '/login',         name: 'Login',         component: () => import('@/views/auth/LoginView.vue') },
  { path: '/register',      name: 'Register',      component: () => import('@/views/auth/RegisterView.vue') },
  { path: '/oauth/callback', name: 'OAuthCallback', component: () => import('@/views/auth/CallbackView.vue') },

  // --- Authenticated (requires auth guard) ---
  {
    path: '/',
    component: () => import('@/components/layout/AppLayout.vue'),
    meta: { requiresAuth: true },
    children: [
      { path: '',                    name: 'Dashboard',          component: () => import('@/views/dashboard/DashboardView.vue') },
      // CV
      { path: 'cv',                  name: 'CvUpload',           component: () => import('@/views/cv/CvUploadView.vue') },
      { path: 'cv/:id',             name: 'CvDetail',            component: () => import('@/views/cv/CvDetailView.vue') },
      // Jobs
      { path: 'jobs',               name: 'JobSearch',           component: () => import('@/views/jobs/JobSearchView.vue') },
      { path: 'jobs/:id',           name: 'JobDetail',           component: () => import('@/views/jobs/JobDetailView.vue') },
      { path: 'jobs/saved',         name: 'SavedJobs',           component: () => import('@/views/jobs/SavedJobsView.vue') },
      // Motivation Letters
      { path: 'motivation/generate', name: 'MotivationGenerate', component: () => import('@/views/motivation/MotivationGenerateView.vue') },
      { path: 'motivation/:id',     name: 'MotivationDetail',    component: () => import('@/views/motivation/MotivationDetailView.vue') },
      // Applications
      { path: 'applications',        name: 'Applications',       component: () => import('@/views/applications/ApplicationsListView.vue') },
      { path: 'applications/:id',   name: 'ApplicationDetail',   component: () => import('@/views/applications/ApplicationDetailView.vue') },
      { path: 'applications/stats', name: 'ApplicationStats',    component: () => import('@/views/applications/ApplicationStatsView.vue') },
      // Profile
      { path: 'profile',            name: 'Profile',             component: () => import('@/views/profile/ProfileView.vue') },
    ],
  },
]
```

---

## 5. Authentication Flow (SPA + Spring Auth Server)

### 5.1 OAuth 2.1 Authorization Code Flow with PKCE

The SPA uses the **Authorization Code Flow with PKCE** (no client secret in the browser):

```
User          Vue.js SPA                  Spring Auth Server            Backend API
 │              │                               │                          │
 │ Click Login  │                               │                          │
 │─────────────►│                               │                          │
 │              │ 1. Generate code_verifier      │                          │
 │              │    + code_challenge (S256)     │                          │
 │              │ 2. Redirect to /oauth2/authorize                         │
 │              │    ?response_type=code         │                          │
 │              │    &client_id=jobagent-spa     │                          │
 │              │    &redirect_uri=/oauth/callback                         │
 │              │    &code_challenge=xxx         │                          │
 │              │    &code_challenge_method=S256 │                          │
 │              │───────────────────────────────►│                          │
 │              │                               │  Show login form         │
 │◄─────────────│◄──────────────────────────────│                          │
 │ Enter creds  │                               │                          │
 │─────────────►│───────────────────────────────►│                          │
 │              │                               │  Validate credentials    │
 │              │                               │  Redirect with auth code │
 │              │◄── 302 /oauth/callback?code=xx│                          │
 │              │                               │                          │
 │              │ 3. Exchange code + verifier    │                          │
 │              │    POST /oauth2/token          │                          │
 │              │───────────────────────────────►│                          │
 │              │◄── { access_token, refresh_token, id_token } ────────────│
 │              │                               │                          │
 │              │ 4. Store tokens (memory)       │                          │
 │              │ 5. Call API with Bearer token  │                          │
 │              │──────────────────────────────────────────────────────────►│
 │              │◄──── 200 data ───────────────────────────────────────────│
 │◄── Dashboard │                               │                          │
```

### 5.2 Token Management (Pinia Store)

```typescript
// src/stores/auth.store.ts
export const useAuthStore = defineStore('auth', () => {
  const accessToken = ref<string | null>(null)    // In memory only — never localStorage
  const refreshToken = ref<string | null>(null)
  const user = ref<User | null>(null)

  const isAuthenticated = computed(() => !!accessToken.value)

  async function login() {
    // Redirect to Spring Auth Server with PKCE
    const { codeVerifier, codeChallenge } = generatePkce()
    sessionStorage.setItem('pkce_verifier', codeVerifier)
    window.location.href = buildAuthUrl(codeChallenge)
  }

  async function handleCallback(code: string) {
    const verifier = sessionStorage.getItem('pkce_verifier')!
    const tokens = await authApi.exchangeCode(code, verifier)
    accessToken.value = tokens.accessToken
    refreshToken.value = tokens.refreshToken
    user.value = decodeJwt(tokens.accessToken)
    sessionStorage.removeItem('pkce_verifier')
  }

  async function refresh() {
    const tokens = await authApi.refreshToken(refreshToken.value!)
    accessToken.value = tokens.accessToken
    refreshToken.value = tokens.refreshToken
  }

  function logout() {
    accessToken.value = null
    refreshToken.value = null
    user.value = null
    window.location.href = '/login'
  }

  return { accessToken, refreshToken, user, isAuthenticated, login, handleCallback, refresh, logout }
})
```

### 5.3 Axios Interceptor (Auto-Refresh)

```typescript
// src/api/client.ts
const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
})

apiClient.interceptors.request.use((config) => {
  const auth = useAuthStore()
  if (auth.accessToken) {
    config.headers.Authorization = `Bearer ${auth.accessToken}`
  }
  return config
})

apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    if (error.response?.status === 401 && !error.config._retry) {
      error.config._retry = true
      const auth = useAuthStore()
      await auth.refresh()
      error.config.headers.Authorization = `Bearer ${auth.accessToken}`
      return apiClient(error.config)
    }
    return Promise.reject(error)
  }
)
```

---

## 6. Page Wireframes

### 6.1 Dashboard

```
┌─────────────────────────────────────────────────────────────────────────┐
│ ◉ JobAgent                                    Jane Doe ▼  🔔  🌐 EN    │
├──────────┬──────────────────────────────────────────────────────────────┤
│          │                                                              │
│ Dashboard│  Welcome back, Jane!                                         │
│          │                                                              │
│ 📄 CV    │  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐       │
│          │  │ 47        │ │ 5        │ │ 2        │ │ 92%      │       │
│ 🔍 Jobs  │  │ Applied   │ │ Interviews│ │ Offers   │ │ Match    │       │
│          │  └──────────┘ └──────────┘ └──────────┘ └──────────┘       │
│ 💌 Letters│                                                             │
│          │  Recent Applications          Recent Job Matches             │
│ 📨 Apply │  ┌─────────────────────┐     ┌─────────────────────┐       │
│          │  │ Sr. Java Dev - Acme  │     │ Lead Dev - TechCo   │       │
│ 📊 Apps  │  │ ✅ Submitted  Feb 18 │     │ 94% match           │       │
│          │  ├─────────────────────┤     ├─────────────────────┤       │
│ ⚙ Profile│  │ DevOps Eng - Beta   │     │ Architect - StartX   │       │
│          │  │ 🕐 Pending   Feb 19 │     │ 91% match           │       │
│          │  └─────────────────────┘     └─────────────────────┘       │
└──────────┴──────────────────────────────────────────────────────────────┘
```

### 6.2 Job Search

```
┌─────────────────────────────────────────────────────────────────────────┐
│ 🔍 Search Jobs                                                          │
│                                                                         │
│ ┌─────────────────────────────────────────────┐  ☑ Remote only          │
│ │ Senior Java Developer                        │  📍 Amsterdam, NL      │
│ └─────────────────────────────────────────────┘  💰 Min €70k  [Search]  │
│                                                                         │
│ 97 results                                          Sort: Match ▼       │
│                                                                         │
│ ┌───────────────────────────────────────────────────────────────────┐   │
│ │ ⭐ Senior Java Developer — Acme Corp                   92% match  │   │
│ │    📍 Amsterdam, NL  |  🏠 Remote  |  💰 €80k–€100k              │   │
│ │    ✅ Java  ✅ Spring Boot  ✅ K8s  ⚠️ Terraform                  │   │
│ │    [Generate Letter]  [Save]  [View Details]                       │   │
│ ├───────────────────────────────────────────────────────────────────┤   │
│ │ ⭐ Lead Backend Engineer — TechCo                      89% match  │   │
│ │    📍 Berlin, DE  |  🏠 Hybrid  |  💰 €90k–€110k                 │   │
│ │    ✅ Java  ✅ Microservices  ⚠️ Go                                │   │
│ │    [Generate Letter]  [Save]  [View Details]                       │   │
│ └───────────────────────────────────────────────────────────────────┘   │
│                                                                         │
│                       [1] [2] [3] [4] [5] →                             │
└─────────────────────────────────────────────────────────────────────────┘
```

### 6.3 Applications Kanban

```
┌─────────────────────────────────────────────────────────────────────────┐
│ 📊 My Applications                    [List View] [Kanban View] [Stats] │
│                                                                         │
│ ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐  │
│ │ PENDING  │  │SUBMITTED │  │INTERVIEW │  │ OFFERED  │  │ ACCEPTED │  │
│ │   (2)    │  │   (30)   │  │   (5)    │  │   (2)    │  │   (0)    │  │
│ │          │  │          │  │          │  │          │  │          │  │
│ │┌────────┐│  │┌────────┐│  │┌────────┐│  │┌────────┐│  │          │  │
│ ││ DevOps ││  ││ Sr Dev ││  ││ Lead   ││  ││Archit. ││  │  Empty   │  │
│ ││ Beta   ││  ││ Acme   ││  ││ TechCo ││  ││ StartX ││  │  State   │  │
│ ││ Feb 19 ││  ││ Feb 18 ││  ││ Mar 5  ││  ││ Mar 10 ││  │          │  │
│ │└────────┘│  │└────────┘│  │└────────┘│  │└────────┘│  │          │  │
│ │┌────────┐│  │┌────────┐│  │          │  │          │  │          │  │
│ ││ Data   ││  ││ ...    ││  │          │  │          │  │          │  │
│ ││ Gamma  ││  ││ +28    ││  │          │  │          │  │          │  │
│ │└────────┘│  │└────────┘│  │          │  │          │  │          │  │
│ └──────────┘  └──────────┘  └──────────┘  └──────────┘  └──────────┘  │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 7. Environment Variables

```bash
# .env (local development)
VITE_API_BASE_URL=http://localhost:8080
VITE_AUTH_ISSUER=http://localhost:8080
VITE_AUTH_CLIENT_ID=jobagent-spa
VITE_AUTH_REDIRECT_URI=http://localhost:5173/oauth/callback

# .env.production
VITE_API_BASE_URL=https://api-eu.jobagent.com
VITE_AUTH_ISSUER=https://auth-eu.jobagent.com
VITE_AUTH_CLIENT_ID=jobagent-spa
VITE_AUTH_REDIRECT_URI=https://app.jobagent.com/oauth/callback
```

---

## 8. Registered OAuth2 Client (SPA)

The SPA must be registered as a **public client** (no secret) in the Spring Authorization Server:

```java
RegisteredClient spaClient = RegisteredClient.withId(UUID.randomUUID().toString())
    .clientId("jobagent-spa")
    .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)     // Public client
    .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
    .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
    .redirectUri("http://localhost:5173/oauth/callback")             // Local dev
    .redirectUri("https://app.jobagent.com/oauth/callback")          // Production
    .postLogoutRedirectUri("http://localhost:5173/login")
    .postLogoutRedirectUri("https://app.jobagent.com/login")
    .scope(OidcScopes.OPENID)
    .scope(OidcScopes.PROFILE)
    .clientSettings(ClientSettings.builder()
        .requireProofKey(true)                                        // Enforce PKCE
        .requireAuthorizationConsent(false)                           // Skip consent for first-party app
        .build())
    .tokenSettings(TokenSettings.builder()
        .accessTokenTimeToLive(Duration.ofMinutes(15))
        .refreshTokenTimeToLive(Duration.ofDays(7))
        .reuseRefreshTokens(false)                                    // Rotate refresh tokens
        .build())
    .build();
```

---

## 9. CORS Configuration

The Spring Boot backend must allow cross-origin requests from the SPA:

```java
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Value("${app.cors.allowed-origins}")
    private String[] allowedOrigins;    // http://localhost:5173, https://app.jobagent.com

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
            .allowedOrigins(allowedOrigins)
            .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
            .allowedHeaders("*")
            .allowCredentials(true)
            .maxAge(3600);
    }
}
```

---

## 10. Docker Setup

### 10.1 Dockerfile (Production — Nginx)

```dockerfile
# Build stage
FROM node:22-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

# Production stage
FROM nginx:1.27-alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
```

### 10.2 Nginx Config

```nginx
server {
    listen 80;
    server_name _;
    root /usr/share/nginx/html;
    index index.html;

    # SPA fallback — all routes serve index.html
    location / {
        try_files $uri $uri/ /index.html;
    }

    # API proxy (optional — if not using separate API domain)
    location /api/ {
        proxy_pass http://jobagent-app:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # Auth server proxy
    location /oauth2/ {
        proxy_pass http://jobagent-app:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }

    location /.well-known/ {
        proxy_pass http://jobagent-app:8080;
        proxy_set_header Host $host;
    }

    # Cache static assets
    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff2?)$ {
        expires 1y;
        add_header Cache-Control "public, immutable";
    }

    # Security headers
    add_header X-Frame-Options "SAMEORIGIN" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-XSS-Protection "1; mode=block" always;
    add_header Referrer-Policy "strict-origin-when-cross-origin" always;
    add_header Content-Security-Policy "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; connect-src 'self' ${API_BASE_URL};" always;
}
```

### 10.3 Docker Compose (addition)

```yaml
# Add to existing docker-compose.yml
services:
  # ...existing services (postgres, redis, kafka, ollama, jobagent-app)...

  jobagent-ui:
    build:
      context: ./jobagent-ui
      dockerfile: Dockerfile
    ports: ["5173:80"]
    depends_on: [jobagent-app]
    environment:
      VITE_API_BASE_URL: http://localhost:8080
      VITE_AUTH_ISSUER: http://localhost:8080
```

For **local development** (without Docker), run the Vite dev server directly:

```bash
cd jobagent-ui
npm install
npm run dev    # → http://localhost:5173 with HMR
```

---

## 11. View ↔ Use Case Mapping

| View                        | Use Case(s)                    | API Endpoints Used                          |
|-----------------------------|--------------------------------|---------------------------------------------|
| `LoginView`                 | UC-01 Login                    | `/oauth2/authorize`, `/oauth2/token`        |
| `RegisterView`              | UC-01 Register                 | `POST /api/v1/auth/register`                |
| `CvUploadView`              | UC-02 Upload CV                | `POST /api/v1/cv`                           |
| `CvDetailView`              | UC-02 View CV                  | `GET /api/v1/cv`, `GET /api/v1/cv/{id}/download` |
| `JobSearchView`             | UC-03 Search Jobs              | `GET /api/v1/jobs`                          |
| `JobDetailView`             | UC-03 Job Detail               | `GET /api/v1/jobs/{id}`                     |
| `SavedJobsView`             | UC-03 Saved Jobs               | `GET /api/v1/jobs/saved`                    |
| `MotivationGenerateView`    | UC-04 Generate Letter          | `POST /api/v1/motivation-letters`           |
| `MotivationDetailView`      | UC-04 View/Edit Letter         | `GET/PUT /api/v1/motivation-letters/{id}`   |
| `ApplicationsListView`      | UC-06 View Applications        | `GET /api/v1/applications`                  |
| `ApplicationDetailView`     | UC-05 Apply + UC-06 Detail     | `POST /api/v1/applications`, `GET /api/v1/applications/{id}` |
| `ApplicationStatsView`      | UC-06 Statistics               | `GET /api/v1/applications/stats`            |
| `ProfileView`               | UC-01 Profile                  | `GET/PUT /api/v1/users/profile`             |

---

## 12. Responsive Design

| Breakpoint     | Layout                                                   |
|----------------|----------------------------------------------------------|
| `< 640px` (sm) | Mobile: sidebar collapses to hamburger menu; cards stack |
| `640–1024px`   | Tablet: sidebar collapses; 2-column card grid            |
| `> 1024px`     | Desktop: full sidebar + 3-column grid                    |

---

## 13. Accessibility (a11y)

- PrimeVue components are WCAG 2.1 AA compliant out-of-the-box.
- All interactive elements have `aria-label` attributes.
- Keyboard navigation for all flows.
- Colour contrast ratios ≥ 4.5:1.
- Screen-reader announcements for async state changes (loading, success, error).

---

## 14. Testing Strategy

| Level   | Tool               | Scope                                                      |
|---------|--------------------|-------------------------------------------------------------|
| Unit    | Vitest + Vue Test Utils | Pinia stores, composables, utility functions           |
| Component | Vitest + Vue Test Utils | Individual component rendering + interactions       |
| Integration | Vitest + MSW (Mock Service Worker) | Full page views with mocked API responses  |
| E2E     | Playwright         | Critical user flows: register → upload CV → search → apply  |
| Visual  | Playwright screenshots | Regression testing for UI appearance                    |

### 14.1 E2E Test Example

```typescript
// tests/e2e/auth.spec.ts
import { test, expect } from '@playwright/test'

test('user can register and see dashboard', async ({ page }) => {
  await page.goto('/register')
  await page.fill('[data-testid="email"]', 'test@example.com')
  await page.fill('[data-testid="password"]', 'S3cur3!Pass')
  await page.fill('[data-testid="fullName"]', 'Test User')
  await page.selectOption('[data-testid="country"]', 'NL')
  await page.click('[data-testid="register-btn"]')

  await expect(page).toHaveURL('/')
  await expect(page.locator('[data-testid="welcome-message"]')).toContainText('Welcome')
})
```

---

## 15. Build & Deploy Pipeline

```
jobagent-ui CI (GitHub Actions)
  ├── npm ci
  ├── npm run lint
  ├── npm run type-check
  ├── npm run test:unit
  ├── npm run build
  ├── npm run test:e2e (Playwright against staging API)
  ├── docker build → ghcr.io/jobagent/jobagent-ui:${SHA}
  └── helm upgrade jobagent-ui --set image.tag=${SHA}
```

---

## 16. Performance

| Strategy                    | Implementation                                          |
|-----------------------------|---------------------------------------------------------|
| Code splitting              | Lazy-loaded routes via `() => import(...)` in Vue Router |
| Tree shaking                | Vite + ES modules; PrimeVue components imported individually |
| Asset caching               | Content-hashed filenames; Nginx `immutable` cache headers |
| API response caching        | Pinia stores cache recent data; SWR pattern for freshness |
| Bundle size target          | < 200 KB gzipped (initial load)                         |
