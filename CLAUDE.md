# Caicai — CLAUDE.md

Read this entire file before writing a single line of code. No exceptions.

---

## Project Overview

A weight management and nutrition tracking web app. Users log meals, track macros, set daily goals, and monitor progress over time.

**Tech stack:**
- Backend: Spring Boot 3, Java 17, PostgreSQL, Flyway, Spring Security JWT (HttpOnly cookies), Bucket4j + Redis rate limiting
- Frontend: React (Vite), Tailwind CSS with custom `@theme` variables, date-fns
- Infrastructure: AWS EC2, RDS PostgreSQL, ElastiCache Redis, S3 + CloudFront, GitHub Actions CI/CD
- AI: Anthropic Claude API (goal suggestions only)
- External API: OpenFoodFacts (food search, cached in Redis + PostgreSQL)

---

## Output Discipline

Build one file at a time. After each file, stop and wait for confirmation before
proceeding to the next. Never produce multiple files in a single response.

Order within a step:
1. State which file you're about to write and why
2. Write it
3. Stop — wait for "good" or feedback before continuing

At the end of every step, before committing, provide a PROJECT_STATUS.md update summary containing:
- Which bootstrap step to check off
- Any new decisions to add
- All new files created with their full paths
- All existing files modified with their full paths
- What to set as the Current Task
- Any new blockers or known issues

---

## Before Writing Any Code — Checklist

Before implementing any feature ask yourself:

1. **Does a shared component already exist for this?** Check `ui/` first
2. **Is this mobile-first?** Design mobile layout before desktop
3. **Does this need a Table + Card split?** Any list/table → yes
4. **What are the pros and cons of this approach?**
5. **Do I need a test for this?** Business logic → yes, CRUD → no
6. **What environment variables does this need?** Never hardcode secrets
7. **Is this transactional?** Any write operation → `@Transactional`
8. **Could this cause a SQL injection?** Always use `:param` syntax
9. **Does this throw an exception?** Always use `AppException`, never raw exceptions

---

## Bootstrap Sequence — Build in This Exact Order

Never skip steps. Never build out of order.

```
1.  Database migrations (all tables first)
2.  Backend entities + repositories
3.  GlobalExceptionHandler + AppException (before any service)
4.  Auth backend (register, verify, login, logout, forgot/reset password, demo)
5.  Shared frontend components (ui/)
6.  apiClient + SessionExpiredModal
7.  Auth frontend pages (register, login, forgot password, reset password)
8.  Onboarding flow + AI goal suggestion
9.  Food search (OpenFoodFacts integration + Redis cache)
10. Favourite foods
11. Food log (core feature)
12. Copy day feature
13. Water tracking
14. Weight tracking
15. Goals (current, history)
16. Dashboard (daily, weekly, monthly)
17. Settings (profile, goals)
```

---

## Backlog

- Food search results ordering — favourites first, previously logged second,
  rest alphabetically. Implement in FoodService when building step 9.
- AI food recommendations — suggest foods to user based on remaining
  daily macro goals. Implement after dashboard is built (step 16).
- Structured logging — add log levels and correlation IDs to make
  debugging production issues easier. Implement before first
  production deployment.
- Macro education tooltips — show a small info popup on each macro
  (protein, carbs, fat, calories) explaining what it does and why it matters.
  Extensible for when fiber, sodium, and sugar are added to the UI.
  Implement after dashboard is built (step 16).

---

## Data Model

```
User
├── FoodItem            (OpenFoodFacts cached + user created)
├── FoodLog             (userId, foodItemId, amountGrams, mealType, loggedAt)
├── WaterLog            (userId, amountMl, loggedAt)
├── WeightLog           (userId, weightKg, loggedAt)
├── UserFavouriteFood   (userId, foodItemId)
└── Goal                (userId, calories, protein, carbs, fat, waterMl, startingWeightKg, targetWeightKg, effectiveFrom)
```

### User
```java
id, email, password, name,
age, weightKg, heightCm, gender, activityLevel,
isVerified, isDemo, createdAt
```

### FoodItem
```java
id, name, brand,
caloriesPer100g, proteinPer100g, carbsPer100g, fatPer100g,
fiberPer100g, sugarPer100g, sodiumPer100g,
source,        // OPENFOODFACTS | USER_CREATED
externalId,    // OpenFoodFacts barcode/id, nullable
createdBy,     // userId, nullable for OpenFoodFacts items
createdAt
```

### FoodLog
```java
id, userId, foodItemId,
amountGrams, mealType,   // BREAKFAST | LUNCH | DINNER | SNACK
loggedAt,                // full timestamp, used for sorting
createdAt
```

### WaterLog
```java
id, userId, amountMl, loggedAt, createdAt
```

### WeightLog
```java
id, userId, weightKg, loggedAt, createdAt
```

### UserFavouriteFood
```java
userId, foodItemId   // composite primary key, no surrogate id needed
```

### Goal
```java
id, userId,
calories, protein, carbs, fat, waterMl,
startingWeightKg,   // captured from user profile at goal creation
targetWeightKg,     // the weight the user wants to reach
effectiveFrom,      // LocalDate — supports full goal history
createdAt
```

**Goal history rule:** Never update an existing goal. Always insert a new record with a new `effectiveFrom`. To find the active goal for a date:
```sql
WHERE user_id = :userId AND effective_from <= :date
ORDER BY effective_from DESC
LIMIT 1
```

---

## API Contract

Every response follows this exact envelope. No exceptions.

### Success (single object)
```json
{
  "data": { }
}
```

### Success (paginated list)
```json
{
  "data": [ ],
  "pagination": {
    "page": 1,
    "size": 20,
    "totalElements": 84,
    "totalPages": 5
  }
}
```

**Pagination is 1-indexed.** Page 1 is the first page. Never use 0-indexed pagination.

### Error
```json
{
  "status": 404,
  "message": "Food item not found with id: 12",
  "fieldErrors": {
    "amountGrams": "Amount must be at least 1",
    "foodId": "Food item is required"
  }
}
```

- `status` — always present, mirrors HTTP status code
- `message` — always present, safe to display in UI
- `fieldErrors` — only present on validation failures, omit otherwise

### Date formats
- **Store:** full ISO timestamp `"2024-01-15T08:30:00"` — never store pre-formatted strings
- **Return from API:** ISO timestamp `"2024-01-15T08:30:00"`
- **Display in frontend:** formatted with date-fns — `"Monday, Jan 15 at 8:30am"`
- **Date-only params:** `"2024-01-15"` (query params for filtering by date)

---

## Endpoints

### Auth
```
POST /api/auth/register
POST /api/auth/login
POST /api/auth/logout
POST /api/auth/forgot-password
POST /api/auth/reset-password
GET  /api/auth/verify?token=xxx
POST /api/auth/demo
```

### User
```
GET    /api/users/me
PUT    /api/users/me
DELETE /api/users/me
POST   /api/users/me/complete-onboarding
```

### Food Items
```
GET    /api/foods?query=chicken&page=1&size=20
GET    /api/foods/{id}
POST   /api/foods
PUT    /api/foods/{id}
DELETE /api/foods/{id}
```

### Favourites
```
GET    /api/foods/favourites
POST   /api/foods/{id}/favourite
DELETE /api/foods/{id}/favourite
```

### Copy Day
```
POST   /api/logs/copy?from=2024-01-14&to=2024-01-15
```

### Food Log
```
GET    /api/logs?date=2024-01-15&page=1&size=20
POST   /api/logs
PUT    /api/logs/{id}
DELETE /api/logs/{id}
```

### Water Log
```
GET    /api/water?date=2024-01-15
POST   /api/water
DELETE /api/water/{id}
```

### Weight Log
```
GET    /api/weight?page=1&size=20
POST   /api/weight
DELETE /api/weight/{id}
```

### Goals
```
POST /api/goals/suggest
GET  /api/goals/current
GET  /api/goals/history?page=1&size=20
POST /api/goals
```

### Dashboard
```
GET /api/dashboard/summary?date=2024-01-15
GET /api/dashboard/weekly?date=2024-01-15
GET /api/dashboard/monthly?date=2024-01-15
```

---

## Copy Day Behaviour

`POST /api/logs/copy?from=2024-01-14&to=2024-01-15`

- Copies all FoodLog entries from `from` date to `to` date
- Creates new log entries — never moves or modifies originals
- Sets `loggedAt` to current timestamp on all copied entries
- If `to` date already has log entries, append — never overwrite
- Returns the newly created log entries

---

## Auth Flow

### Register
```
1. POST /api/auth/register
2. Backend sends verification email with token
3. Frontend shows "Check your email" screen
4. User clicks link → GET /api/auth/verify?token=xxx
5. Backend marks isVerified=true, returns JWT cookie
6. Frontend redirects to /onboarding
```

### Onboarding
```
1. User enters: age, weight, height, gender, activity level, goal type, target weight
2. POST /api/goals/suggest → AI returns suggested targets
3. Frontend shows suggestions with explanation text
4. User confirms or adjusts → POST /api/goals
   (startingWeightKg captured from user's entered weight automatically)
5. First WeightLog entry created automatically from starting weight
6. Redirect to /dashboard
7. Onboarding is skippable — show helper text:
   "You can set your goals anytime in Settings"
8. 8. If skipped → POST /api/users/me/complete-onboarding, 
   no Goal record created, dashboard shows empty goal state
```

### Login
```
1. POST /api/auth/login
2. Backend returns JWT cookie
3. hasCompletedOnboarding = true  → /dashboard
4. hasCompletedOnboarding = false → /onboarding
```

### Password Reset
```
1. POST /api/auth/forgot-password (sends email with token)
2. User clicks link → opens /reset-password?token=xxx in app
3. User enters new password
4. POST /api/auth/reset-password
5. Redirect to /login
```

### Demo
```
1. POST /api/auth/demo
2. Backend creates ephemeral user with seeded data
3. JWT cookie → redirect to /dashboard
```

---

## Food Search Flow

Live search with debounce + Redis cache. Never call OpenFoodFacts on every keystroke.

```
Frontend:
  User types → debounce 300ms → GET /api/foods?query=chicken

Backend:
  1. Check Redis cache for query key
  2. Cache hit → return immediately
  3. Cache miss → query OpenFoodFacts API
  4. Store results in Redis (TTL: 24 hours)
  5. Store new FoodItems in PostgreSQL
  6. Return results
```

**Cache key format:** `food_search:{query}` (lowercase, trimmed)

**Search result ordering:**
1. User's favourite foods that match the query (top)
2. Previously logged foods that match the query
3. OpenFoodFacts / cached results

---

## AI Goal Suggestion

Used only for `POST /api/goals/suggest`. No other AI usage.

```
POST /api/goals/suggest
Body: {
  age, weightKg, heightCm, gender,
  activityLevel,   // SEDENTARY | LIGHT | MODERATE | ACTIVE | VERY_ACTIVE
  goalType         // LOSE_WEIGHT | MAINTAIN | GAIN_MUSCLE
}
Response: {
  "data": {
    "calories": 2100,
    "protein": 160,
    "carbs": 210,
    "fat": 70,
    "waterMl": 2800,
    "explanation": "Based on your profile..."
  }
}
```

**AI prompt pattern (backend):**
```
System: You are a nutrition expert. Return only valid JSON with no preamble or markdown.
User:   Age: {age}, weight: {weightKg}kg, height: {heightCm}cm,
        gender: {gender}, activity: {activityLevel}, goal: {goalType},
        target weight: {targetWeightKg}kg.
        Return: { calories, protein, carbs, fat, waterMl, explanation }
```

This endpoint suggests only — it never saves. The user confirms, then `POST /api/goals` saves it.

---

## Dashboard Response Shape

### Daily summary
```json
{
  "data": {
    "date": "2024-01-15",
    "totals":   { "calories": 1840, "protein": 120, "carbs": 200, "fat": 65, "waterMl": 1200 },
    "goal":     { "calories": 2100, "protein": 160, "carbs": 210, "fat": 70,  "waterMl": 2500 },
    "progress": { "calories": 88,   "protein": 75,  "carbs": 95,  "fat": 93,  "waterMl": 48 },
    "weight": {
      "current": 82.5,
      "starting": 90.0,
      "target": 75.0,
      "progress": 47
    },
    "logsByMealType": {
      "BREAKFAST": [ ],
      "LUNCH":     [ ],
      "DINNER":    [ ],
      "SNACK":     [ ]
    }
  }
}
```

### Weekly / Monthly summary
```json
{
  "data": [
    {
      "date": "2024-01-15",
      "calories": 1840, "protein": 120, "carbs": 200, "fat": 65, "waterMl": 1200,
      "weightKg": 82.5
    }
  ]
}
```

One entry per day, aggregated totals only. Used for charts. `weightKg` is null for days with no weight log — frontend handles gaps in the weight trend line gracefully.

---

## Error Handling

### AppException (backend)
Always throw `AppException`. Never throw `RuntimeException`, `IllegalArgumentException`, or raw Spring exceptions.

```java
throw new AppException(HttpStatus.NOT_FOUND, "Food item not found with id: " + id);
throw new AppException(HttpStatus.FORBIDDEN, "You don't own this log entry");
throw new AppException(HttpStatus.BAD_REQUEST, "Goal type is invalid");
```

### GlobalExceptionHandler rules
- Catches ALL exceptions — nothing leaks to Spring's default error handler
- Returns the standard error envelope on every error
- `fieldErrors` only on `MethodArgumentNotValidException`, null on all others
- Never expose stack traces in responses
- Never log sensitive data in error messages

### Frontend error handling
```javascript
// apiClient throws consistently shaped errors
try {
  const data = await apiClient('/api/logs', { method: 'POST', body })
} catch (err) {
  if (err.fieldErrors) setFieldErrors(err.fieldErrors)  // inline validation
  else setError(err.message)                             // general error
}
```

Two cases only. Never handle errors any other way.

---

## apiClient

Single HTTP wrapper. Every API call goes through this. Never use raw `fetch` in components.

**Behavior:**
- Prepends `VITE_API_URL` from env
- Always sends `credentials: 'include'` (cookies)
- Unwraps `{ data }` from successful responses automatically
- On `401` → triggers `SessionExpiredModal` (global, not per-component)
- On error → throws `{ message, fieldErrors }` consistently
- On network failure → throws `{ message: "Network error, please try again" }`

**SessionExpiredModal:**
- Lives at app root level — one instance only
- Shows "Your session has expired. Please log in again." with a login button
- Never instantiated per-component

---

## Modularity Rules

The backend is domain-based. Adding a new feature means adding a new package — never polluting existing ones.

- Each domain owns its own service, repository, controller, and DTOs
- Never call one domain's repository from another domain's service — go through the service layer
- Dashboard is the only exception — it reads from multiple domains but never writes
- Dashboard service delegates calculations to each domain's own service — never duplicates logic
- New features require a dashboard update — keep the dashboard service thin

---

## Pros/Cons Framework

Always think through trade-offs before implementing. Document the decision:

```
Decision: [what you're deciding]
Option A: [approach]
  Pros: ...
  Cons: ...
Option B: [approach]
  Pros: ...
  Cons: ...
Chosen: [A or B] because [reason]
```

Never optimize prematurely. If there's no evidence of a problem, don't solve it.

---

## Design System

### Theme variables in `index.css`

```css
@theme {
  --color-green:         #10b981;
  --color-green-light:   #86efac;
  --color-green-bg:      #052e16;
  --color-orange:        #f97316;
  --color-orange-bg:     #431407;
  --color-blue:          #3b82f6;
  --color-blue-bg:       #172554;
  --color-yellow:        #eab308;
  --color-yellow-bg:     #422006;
  --color-red:           #ef4444;
  --color-red-bg:        #450a0a;
  --color-purple:        #a855f7;
  --color-purple-bg:     #2e1065;
  --color-bg-page:       #0f0f0f;
  --color-bg-card:       #1a1a1a;
  --color-bg-input:      #242424;
  --color-border:        #2e2e2e;
  --color-border-light:  #3a3a3a;
  --color-text-primary:  #f5f5f5;
  --color-text-secondary:#d4d4d4;
  --color-text-muted:    #737373;
}
```

Rules:
- Always use theme variables — never hardcode hex colors
- Dark theme only
- Consistent spacing: `gap-3`, `p-4`, `rounded-xl` for cards
- Inputs: `bg-bg-input border border-border rounded-lg px-3 py-2`
- Primary button: `bg-green text-white rounded-lg px-4 py-2 font-semibold hover:opacity-90`
- Danger: `bg-transparent text-red border border-red rounded-md px-2.5 py-1`

### Typography — Strict Rules
Two font sizes cover 95% of the UI:
- `text-sm` — secondary info, labels, helper text, timestamps
- `text-base` — all body text, inputs, buttons

Exceptions (use sparingly, never invent new ones):
- `text-xs` — field error messages only
- `text-lg` — page titles only
- `text-2xl` — stat values in StatCard only

**Never use:** `text-xl`, `text-3xl`, `text-4xl`, or any size not listed above. If you feel you need a new size, you don't — adjust weight or color instead.

### Emojis — Never in UI Text
Never use emojis in headings, labels, buttons, error messages, or body text.

Emojis are only acceptable in `EmptyState` icon prop:
```jsx
<EmptyState icon="🍽️" title="No meals logged yet" ... />
```

Everywhere else — no emojis. Use Lucide icons for all iconography.

---

## Shared Components — Build These First

Before writing any page, build these.

```
src/components/ui/
  StatCard.jsx        ← { label, value, color, unit }
  PageHeader.jsx      ← { title, action? }
  EmptyState.jsx      ← { icon, title, description, action? }
  FilterPills.jsx     ← { options, active, onChange }
  Pagination.jsx      ← { currentPage, totalPages, onPageChange }
  LoadingSpinner.jsx
  ProgressBar.jsx     ← { value, max, color }
  MacroBadge.jsx      ← { protein, carbs, fat, calories }
  SessionExpiredModal ← global, lives in App.jsx
  Button.jsx          ← { children, variant, loading, disabled, fullWidth }
  Input.jsx           ← { label, type, value, onChange, error, placeholder }
  RadioCard.jsx       ← { label, description?, icon?, selected, onClick }
  FoodItemCard.jsx    ← { food, onAdd } — food name, brand, macro dots (Protein/Carbs/Fat), + button  
  FilterPills.jsx     ← { options: [{ value, label }], active, onChange }
```

Usage examples:
```jsx
<StatCard label="Calories today" value={1840} color="text-green" unit="kcal" />
<PageHeader title="Food Log" action={<button>+ Add Meal</button>} />
<EmptyState icon="🍽️" title="No meals logged yet" description="..." action={<button>Add Meal</button>} />
```

---

## Component Architecture

### Folder structure
```
pages/
  dashboard/
    Dashboard.jsx
  food-log/
    FoodLog.jsx
    FoodLogView.jsx
    FoodLogTable.jsx
    FoodLogCard.jsx
  water/
    Water.jsx
    WaterView.jsx
    WaterCard.jsx
  goals/
    Goals.jsx
    GoalHistory.jsx
  onboarding/
    Onboarding.jsx
    StepBasics.jsx
    StepGoals.jsx
    StepSuggestion.jsx
  settings/
    Settings.jsx
```

### Container/Presentational (strict)
```
API → Page → View → Table/Card
```
- **Page** — API calls, state management only
- **View** — filter, pagination, delete logic
- **Table/Card** — display only, no API calls, props only

### Table/Card split — always for lists
```jsx
<div className="hidden md:block">
  <FoodLogTable entries={paginatedEntries} onDelete={handleDelete} />
</div>
<div className="md:hidden">
  <FoodLogCard entries={paginatedEntries} onDelete={handleDelete} />
</div>
```

---

## Responsive Design — Mobile First

Always design mobile first. Never add a feature without thinking about mobile.

```jsx
{/* Sidebar — desktop only */}
<aside className="hidden md:flex ...">

{/* Bottom nav — mobile only */}
<nav className="md:hidden fixed bottom-0 ...">

{/* Extra bottom padding on mobile for bottom nav */}
<main className="flex-1 p-5 pb-20 md:pb-5 ...">
```

Grid patterns:
```jsx
<div className="grid grid-cols-2 md:grid-cols-4 gap-3">  // stats
<div className="grid grid-cols-1 md:grid-cols-2 gap-4">  // forms
```

---

## Backend Structure

```
com.caicai.auth
com.caicai.user
com.caicai.food
com.caicai.log
com.caicai.water
com.caicai.goal
com.caicai.dashboard
com.caicai.config
com.caicai.common
com.caicai.email
```

### Service pattern
```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FoodLogService {
    private final FoodLogRepository foodLogRepository;

    @Transactional
    public FoodLogDto createEntry(Long userId, CreateFoodLogDto dto) { ... }

    public List<FoodLogDto> getEntriesByDate(Long userId, LocalDate date) { ... }
}
```

Always use `@Transactional` on writes. Multi-step operations must be in one transaction.

---

## Validation

### Backend DTO
```java
public class CreateFoodLogDto {
    @NotNull(message = "Food item is required")
    private Long foodId;

    @NotNull(message = "Date is required")
    private LocalDate date;

    @NotNull(message = "Amount is required")
    @Min(value = 1, message = "Amount must be at least 1")
    private Integer amountGrams;
}
```

### Frontend inline errors
```jsx
const [fieldErrors, setFieldErrors] = useState({})

const handleSubmit = async () => {
    setFieldErrors({})
    try {
        await apiClient(...)
    } catch (err) {
        if (err.fieldErrors) setFieldErrors(err.fieldErrors)
        else setError(err.message)
    }
}

<input className={`... ${fieldErrors.amountGrams ? 'border-red' : 'border-border'}`} />
{fieldErrors.amountGrams && <div className="text-red text-xs mt-1">{fieldErrors.amountGrams}</div>}
```

Clear error on change:
```jsx
onChange={e => {
    setValue(e.target.value)
    setFieldErrors(prev => ({ ...prev, fieldName: null }))
}}
```

### Password rules
- Min 6 characters, one digit, one uppercase
- Backend: `@Pattern(regexp = "^(?=.*[0-9])(?=.*[A-Z]).{6,}$")`

---

## Database Migrations

Start at V1, increment always.

Rules:
- Forward-only — never modify an existing migration
- Wrap multi-statement migrations in a transaction
- Never `DELETE` without `WHERE`
- Never `DROP TABLE` without confirmation
- Test locally before pushing

---

## Security Rules

- Never string concatenation in `@Query` — always `:param`
- Never return sensitive fields in DTOs
- Never log sensitive data
- Always verify resource ownership in service layer
- All secrets via environment variables
- `@Transactional` on all writes
- `APP_COOKIE_SECURE=true` in production

---

## Testing

Write tests alongside services — not after.

### What to test
- Business logic — calorie totals, macro calculations, goal progress
- Edge cases — zero values, date boundaries, exceeding limits
- Failure cases — not found, unauthorized, validation errors

### What NOT to test
- Simple CRUD (`return repository.save(entity)`)
- DTOs, entities, controllers, repositories

### Pattern (AAA)
```java
@ExtendWith(MockitoExtension.class)
class FoodLogServiceTest {
    @Mock private FoodLogRepository repo;
    @InjectMocks private FoodLogService service;

    @Test
    @DisplayName("Daily total sums all entries for date")
    void getDailyTotal_multipleLogs_returnsSum() {
        // Arrange
        when(repo.findByUserIdAndDate(1L, LocalDate.now()))
                .thenReturn(List.of(buildLog(500), buildLog(700)));
        // Act
        DailySummaryDto result = service.getDailySummary(1L, LocalDate.now());
        // Assert
        assertThat(result.getTotalCalories()).isEqualTo(1200);
    }
}
```

---

## Rate Limiting

```java
case "/api/auth/login"           -> { capacity = 5;  duration = Duration.ofMinutes(1); }
case "/api/auth/register"        -> { capacity = 3;  duration = Duration.ofMinutes(1); }
case "/api/auth/forgot-password" -> { capacity = 3;  duration = Duration.ofMinutes(10); }
case "/api/auth/reset-password"  -> { capacity = 5;  duration = Duration.ofMinutes(10); }
case "/api/auth/verify"          -> { capacity = 5;  duration = Duration.ofMinutes(10); }
case "/api/auth/demo"            -> { capacity = 3;  duration = Duration.ofMinutes(10); }
default -> {
    if (uri.startsWith("/api/")) { capacity = 100; duration = Duration.ofMinutes(1); }
}
```

---

## Environment Variables

### Local (`application-local.yml`)
```yaml
DB_URL: jdbc:postgresql://localhost:5432/caicai_db
DB_USERNAME: your_username
DB_PASSWORD: your_password
JWT_SECRET: your_secret_min_32_chars
MAIL_USERNAME: your@gmail.com
MAIL_PASSWORD: your_app_password
REDIS_HOST: localhost
CORS_ORIGINS: http://localhost:5173
FRONTEND_URL: http://localhost:5173
APP_COOKIE_SECURE: false
ANTHROPIC_API_KEY: your_key
```

### Production (EC2 `~/.env`)
```bash
export DB_URL=jdbc:postgresql://xxx.rds.amazonaws.com:5432/caicai_db
export DB_USERNAME=caicai_admin
export DB_PASSWORD=xxx
export JWT_SECRET=xxx
export MAIL_USERNAME=xxx
export MAIL_PASSWORD=xxx
export REDIS_HOST=master.xxx.cache.amazonaws.com
export CORS_ORIGINS=https://xxx.cloudfront.net
export FRONTEND_URL=https://xxx.cloudfront.net
export APP_COOKIE_SECURE=true
export ANTHROPIC_API_KEY=xxx
```

### `application.yml` pattern
```yaml
app:
  frontend-url: ${FRONTEND_URL:http://localhost:5173}
  cors:
    allowed-origins: ${CORS_ORIGINS:http://localhost:5173}
  cookie:
    secure: ${APP_COOKIE_SECURE:false}
  anthropic:
    api-key: ${ANTHROPIC_API_KEY}
```

Always `${ENV_VAR:default}` — never hardcode.

---

## Local vs Production — Critical Differences

Never assume local and production behave the same. These are the known differences and how to handle them.

### Redis — Fail Fast
The app will not start without a Redis connection. This is intentional — never mock or skip Redis locally. Behavior must match production.

```bash
# If app fails to start, Redis is not running. Fix:
docker start caicai-redis

# First time setup:
docker run -d -p 6379:6379 --name caicai-redis redis
```

Never add a Redis health check that degrades gracefully locally. If Redis is down, the app is broken — fix it.

### Cookie Security
`APP_COOKIE_SECURE=false` locally, `true` in production. This is not optional.

Secure cookies only transmit over HTTPS. Production runs over HTTPS via CloudFront. Local runs over HTTP. If `APP_COOKIE_SECURE=true` locally, auth silently breaks — cookies are set but never sent. Always verify this flag when debugging auth issues locally.

### OpenFoodFacts — Graceful Degradation
If OpenFoodFacts is unreachable (down, slow, or rate limited):
- Return whatever exists in Redis cache or PostgreSQL
- Never throw a 500 — return empty results gracefully
- Log the failure server-side with the error details
- Never block the user from logging food they've searched before

```java
// Wrap OpenFoodFacts call specifically — not a general catch-all
try {
    results = openFoodFactsClient.search(query);
    cacheResults(query, results);
    saveNewItems(results);
} catch (Exception e) {
    log.warn("OpenFoodFacts unreachable for query '{}': {}", query, e.getMessage());
    // fall through — return cached results only
}
return getCachedResults(query);
```

This means local development works offline as long as you've searched for the food at least once before.

---

## Local Development

```bash
# start Redis (required — app will not start without it)
docker start caicai-redis
# or first time:
docker run -d -p 6379:6379 --name caicai-redis redis

# backend
cd caicai-backend
./mvnw spring-boot:run

# frontend
cd caicai-frontend
npm run dev
```

Requires `application-local.yml` (hyphen not dot) with all secrets. Spring profile `local` must be active in IDE run config.

---

## Deployment

### Automatic (push to `main`)
Both repos have GitHub Actions that auto-deploy on push to `main`.

**Backend deploy.yml key steps:**
```yaml
- name: Build JAR
  run: |
    chmod +x ./mvnw
    ./mvnw clean package -DskipTests

- name: Upload JAR to EC2
  uses: appleboy/scp-action@master

- name: Restart Spring Boot
  uses: appleboy/ssh-action@master
  with:
    script: |
      pkill java || true
      sleep 2
      source ~/.env
      java -jar ~/caicai-0.0.1-SNAPSHOT.jar > ~/app.log 2>&1 &
```

**Critical:** `source ~/.env` must run before `java -jar` — environment variables are lost on restart otherwise.

**Frontend deploy.yml key steps:**
```yaml
- name: Build
  run: npm run build
  env:
    VITE_API_URL: ${{ secrets.VITE_API_URL }}

- run: aws s3 sync dist/ s3://caicai-frontend --delete
- run: aws cloudfront create-invalidation --distribution-id ${{ secrets.CLOUDFRONT_DISTRIBUTION_ID }} --paths "/*"
```

---

## AWS Infrastructure

| Service | Resource | Purpose |
|---------|----------|---------|
| EC2 | t3.micro, Amazon Linux 2023 | Spring Boot API |
| RDS | db.t3.micro, PostgreSQL 18 | Database |
| ElastiCache | cache.t3.micro, Redis 7.1 | Rate limiting + food search cache |
| S3 | `caicai-frontend` | Frontend files |
| CloudFront | xxx.cloudfront.net | CDN + HTTPS + API proxy |

**CloudFront behaviors:**
- `/*` → S3 (frontend)
- `/api/*` → EC2:8080 (API, CachingDisabled, AllViewer)

**Why CloudFront proxies API:** Frontend is HTTPS, EC2 is HTTP. Browsers block mixed content. CloudFront routes `/api/*` to EC2 internally.

**Security groups:**
- `caicai-ec2-sg` → 8080 open, 22 open
- `caicai-rds-sg` → 5432 from EC2 only
- `caicai-redis-sg` → 6379 from EC2 only

**ElastiCache TLS:** Use `rediss://` (double s) in Redisson config for ElastiCache.

---

## Lessons Learned — Never Repeat These

1. **Build shared components first** — StatCard, PageHeader, EmptyState before any pages
2. **Mobile-first from day one** — never retrofit responsive design
3. **Table/Card split from day one** — not as a refactor later
4. **Write tests alongside services** — not as a backlog item
5. **GlobalExceptionHandler in step 3** — before any service, or error handling becomes inconsistent
6. **Always throw AppException** — never raw exceptions, never per-service error handling
7. **`source ~/.env` in deploy script** — env vars lost on restart otherwise
8. **File names are case-sensitive on Linux** — `apiClient` not `ApiClient`, CI/CD will fail
9. **`application-local.yml` not `application.local.yml`** — Spring only loads hyphenated profiles
10. **`rediss://` not `redis://` for ElastiCache TLS**
11. **CloudFront as API proxy** — solves mixed content without SSL cert on EC2
12. **Never use root AWS account** — always IAM user with least privilege
13. **`.env.production` is gitignored** — Vite bakes values at compile time
14. **Set RDS security group correctly** — don't use the default VPC security group
15. **Node 20+ for Vite builds** — Node 18 has `CustomEvent` issues
16. **Always `isDemo(false)` in User.builder()** — explicit is better than relying on Java default
17. **Pagination is 1-indexed** — never 0-indexed, frontend and backend must agree
18. **Never update a Goal record** — always insert new with effectiveFrom, history must be preserved
19. **AI suggests, user confirms** — never silently save AI-generated values
20. **Food search debounce is 300ms** — never fire on every keystroke
21. **Redis is required locally** — app fails fast without it, never mock it
22. **`APP_COOKIE_SECURE=false` locally** — secure cookies over HTTP silently breaks auth
23. **OpenFoodFacts failures are non-fatal** — catch specifically, log, return cached results
24. **Hibernate naming strategy doesn't handle numbers correctly** — caloriesPer100g becomes calories_per100g not   
    calories_per_100g. Always use explicit @Column(name = "...") for fields with numbers in the name.
25. **To view live logs on EC2 : ssh into instance and run `tail -f ~/app.log`** . Always check logs before assuming production is broken.
26. **401 on auth endpoints is a login failure, not session expiry** —
    never trigger SessionExpiredModal on /api/auth/* routes
27. In IntelliJ, system environment variables are not passed to the JVM automatically —
        either add them to the run configuration or use raw values in application-local.yml.
        Never commit raw secrets to git.
