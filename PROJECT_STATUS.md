# Caicai — Project Status

> Paste this file alongside CLAUDE.md at the start of every new chat session.
> Keep this file updated after every session.

---

## Current Status

**Phase:** started — ready to build  
**Last updated:** 2026-05-29

---

## Bootstrap Progress

```
[x] 1. Database migrations
[x] 2. Backend entities + repositories
[x] 3.  GlobalExceptionHandler + AppException
[x] 4.  Auth backend (register, verify, login, logout, forgot/reset password, demo)
[x] 5.  Shared frontend components (ui/)
[x] 6.  apiClient + SessionExpiredModal
[x] 7.  Auth frontend pages
[x] 8.  Onboarding flow + AI goal suggestion
[x] 9.  Food search (OpenFoodFacts + Redis cache)
[x] 10. Favourite foods
[x] 11. Food log
[x] 12. Copy day feature
[x] 9.  Water tracking backend
[x] 10. Weight tracking backend
[x] 11. Goals backend (suggest + save)
[x] 12. Dashboard backend (daily, weekly, monthly)
[x] 13. Settings backend (profile, goals)
[x] 14. Shared frontend components — partial (Input, Button, AuthShell, StatCard, PageHeader, EmptyState, FilterPills, FoodItemCard, Pagination, LoadingSpinner, ProgressBar, MacroBadge, RadioCard)
[x] 15. apiClient + SessionExpiredModal
[x] 16. Auth frontend pages
[x] 17. Onboarding frontend
[x] 18. Layout shell (BottomNav + Sidebar) + remaining shared components
[x] 19. Use another food API
[ ] 20. Food search + Favourite foods frontend
[ ] 21. Food log + Copy day frontend
[ ] 22. Water tracking frontend
[ ] 23. Weight tracking frontend
[ ] 24. Goals frontend
[ ] 25. Dashboard frontend
[ ] 26. Settings frontend
```

---

## Decisions Made This Session

- Added Button and Input to shared ui/ components (not in original CLAUDE.md)
- Deferred SectionHeader, ErrorMessage, MacroCard, CalorieRing — no immediate use case
- CalorieRing deferred to step 16 (dashboard)
- MacroCard deferred to step 11 (food log) where need will be clearer
- Macro education tooltips added to backlog (implement after step 16)

- App.jsx uses a div wrapper instead of fragment to set bg-bg-page as global background
- SessionExpiredModal tested and working

- Changed --color-green from #22c55e to #10b981 (less neon, more refined)
- Removed @import "tailwindcss" from index.css — @tailwindcss/vite plugin handles it automatically
- Added path alias @ → src/ in vite.config.js

- Added noValidate to all auth forms — browser native validation disabled
- Fixed apiClient 401 handling — auth endpoints don't trigger SessionExpiredModal
- Moved json parsing before 401 check in apiClient

- Gender options: Male and Female only (DESIGN.md is correct). Mockup showing "Other" was a reference artefact, not a spec. Onboarding.jsx updated accordingly.
- Output discipline rule added to CLAUDE.md — one file at a time, stop and wait for confirmation
- RestTemplate declared as a @Bean in RestTemplateConfig — Spring Boot does not auto-configure it
- Anthropic API key must be set as environment variable ANTHROPIC_API_KEY and referenced in application.properties as anthropic.api-key=${ANTHROPIC_API_KEY}

- Demo users go through onboarding (hasCompletedOnboarding = false) — AI suggestion
  is a key feature worth showcasing
- Demo banner added to onboarding — informs user what the flow is for
- AuthContext deferred from step 7 — being built now as part of step 8
- Auth routing strategy: App load calls GET /api/users/me to check auth state.
  401 = not authenticated. 200 = authenticated, route based on hasCompletedOnboarding
- Login redirects to /onboarding if hasCompletedOnboarding = false, /dashboard if true
- Skip onboarding sets hasCompletedOnboarding = true on backend — never re-asked on next login
- Gender.OTHER left in User enum — frontend only exposes Male/Female, no migration needed now

- apiClient returns json.data directly (envelope already unwrapped).
  Never do data.data in components — use data directly.
- Anthropic model string is claude-opus-4-5 (not claude-opus-4-20250514)
- AI suggestion failure falls through to manual entry (AdjustForm) instead of dead-end error
- Placeholder /dashboard route added to App.jsx — will be replaced in step 16
- completeOnboarding() called in AuthContext after save and skip to keep in-memory user state in sync

- Redis is used as a fetch-guard only (presence of key = query already fetched from OpenFoodFacts). Results are always read from PostgreSQL. Not a product cache — PostgreSQL is the result store.
- Minimum query length of 2 characters enforced in FoodController before hitting the service.
- FoodDtos.FoodItemResponse includes isFavourite to avoid a second request from the frontend.
- Result ordering in FoodService: favourites first, then alphabetical. Previously-logged ordering deferred to step 11 when FoodLog queries are available.
- Redis is a fetch-guard only — presence of key means query already fetched from OpenFoodFacts. Results always read from PostgreSQL. Not a product cache.
- Only cache if OpenFoodFacts returns results. Empty results do not set the cache key — prevents poisoning the cache on 503s.
- User-Agent header required by OpenFoodFacts — anonymous requests get rate limited with 503. Configured via app.openfoodfacts.user-agent in application.yml.
- created_by is null for OpenFoodFacts items by design — only set for USER_CREATED foods.
- FoodItemCard.jsx frontend test deferred to step 11 — testing in isolation with hardcoded props doesn't prove integration. Real test is when wired to live search in food log page.
- Food log page loads with empty state + search bar. No preloaded products. Results appear on user input with 300ms debounce. Recent foods (from FoodLog) shown when search is empty — implement in step 11.
- ObjectMapper and TypeReference were incorrectly included in FoodService imports — removed.
- @RequiredArgsConstructor dropped from OpenFoodFactsClient — @Value doesn't work with Lombok's generated constructors, switched to manual constructor.

- addFavourite is idempotent — If you favourite something that's already a favourite, nothing happens. No error, no duplicate — it just ignores the request.
- apiClient is a named export, not default. Always import as: import { apiClient } from '@/apiClient'

- apiClient is a named export. Always import as: import { apiClient } from '@/apiClient'. Never use default import.

- AppException constructor signature is (HttpStatus status, String message) — status first, message second
- FoodLogResponse includes computed macros (calories, protein, carbs, fat) scaled to amountGrams — avoids client-side calculation
- loggedAt stored as date.atStartOfDay() — client sends date only, not a full timestamp
- BigDecimal macro fields must be converted via .doubleValue() before arithmetic in FoodLogService
- UserService.getIdByEmail(String email) added as a wrapper around userRepository.findByEmail
- JwtAuthFilter sets principal as User entity, not UserDetails — always use @AuthenticationPrincipal User user in controllers, never UserDetails. Call user.getId() directly

- apiClient must handle empty responses (204 No Content) — use response.text() then JSON.parse only if non-empty, otherwise default to {}
- Never call JSON.stringify on body in components — apiClient handles serialization itself
- OpenFoodFacts rate limiting is a known issue — try to look for alternative later, cached PostgreSQL data still work

- Frontend will be fully rebuilt based on new designs created before starting step 14
- All backend is complete — do not touch backend during frontend pass unless a bug is found
- Existing frontend pages (auth, onboarding, food log) will be reviewed and updated to match new designs during the frontend pass

- AuthShell promoted to shared component — used by all 4 auth pages
- text-2xl font-bold text-green for "Caicai" brand wordmark on auth pages
- Inline style used for radial gradient (#052e16 → #0f0f0f) — no Tailwind utility covers this
- Input accepts optional labelAction prop for inline label-row elements
- PROJECT_STATUS.md summary provided at end of task, not after every file

- Onboarding layout: centered max-w-3xl card, header + progress bar outside card, justify-center on outer div
- Onboarding background: same radial gradient as AuthShell (radial-gradient(ellipse 80% 60% at 0% 0%, #052e16 0%, #0f0f0f 60%)), applied inline — no separate component yet
- Onboarding desktop fix was layout-only — no logic changes, all three step files untouched

- /profile — read-only profile page (name, member since, weight progress, nutrition goals, personal details)
- /settings — account actions only (change email, change password, delete account)
- Desktop: sidebar user card + name link to /profile. Logout is inline below name in same card.
- Mobile: MobileHeader avatar links to /profile. Logout lives inside /profile page at the bottom.
- No /profile/:userId — no social features, revisit if ever scoped
- Page title removed from Sidebar — pages own their own titles in content area
- MobileHeader: fixed h-14, pt-14 on main content to clear it
- UserAvatar component duplicated in Sidebar and MobileHeader — extract to src/components/ui/UserAvatar.jsx during polish pass

- FatSecret Basic tier — OAuth 1.0 HMAC-SHA1, POST to server.api
- All methods go through BASE_URL — no separate endpoint per method
- FoodSource enum: OPENFOODFACTS replaced with FATSECRET
- Search returns Per Xg descriptions — normalized to per 100g using regex + math
- Foods with non-gram servings (oz, named) call food.get for full serving data
- Null fiber/sugar/sodium is expected — not all FatSecret entries have micronutrient data
- Redis fetch-guard prefix updated to fatsecret_search:
- food_items and related tables truncated — all OpenFoodFacts data wiped
- FATSECRET_ID and FATSECRET_SECRET env vars required in run config
---

## Files Created So Far

_List every file created, so the next session knows what exists._
Frontend

src/components/ui/StatCard.jsx
src/components/ui/PageHeader.jsx
src/components/ui/EmptyState.jsx
src/components/ui/FilterPills.jsx
src/components/ui/FoodItemCard.jsx
src/components/ui/Pagination.jsx
src/components/ui/LoadingSpinner.jsx
src/components/ui/ProgressBar.jsx
src/components/ui/MacroBadge.jsx
src/components/ui/SessionExpiredModal.jsx
src/components/ui/Input.jsx
src/components/ui/Button.jsx
src/components/ui/AuthShell.jsx
src/components/ui/RadioCard.jsx


src/components/layout/BottomNav.jsx
src/components/layout/Sidebar.jsx
src/components/layout/AppShell.jsx
src/components/layout/MobileHeader.js

src/apiClient.js
src/App.jsx

src/pages/auth/Login.jsx
src/pages/auth/Register.jsx
src/pages/auth/ForgotPassword.jsx
src/pages/auth/ResetPassword.jsx

src/context/AuthContext.jsx
src/pages/onboarding/Onboarding.jsx
src/pages/onboarding/StepBasics.jsx
src/pages/onboarding/StepGoals.jsx
src/pages/onboarding/StepSuggestion.jsx


src/pages/food-log/FoodLogView.jsx
src/pages/food-log/FoodLogTable.jsx
src/pages/food-log/FoodLogCard.jsx
src/pages/food-log/FoodLog.jsx

Backend

src/main/java/com/caicai/auth/AuthController.java
src/main/java/com/caicai/auth/AuthDtos.java
src/main/java/com/caicai/auth/AuthService.java
src/main/java/com/caicai/auth/VerificationToken.java
src/main/java/com/caicai/auth/VerificationRepository.java

src/main/java/com/caicai/common/AppException.java
src/main/java/com/caicai/common/GlobalExceptionHandler.java

src/main/java/com/caicai/config/JwtAuthFilter.java
src/main/java/com/caicai/config/JwtUtil.java
src/main/java/com/caicai/config/RateLimitFilter.java
src/main/java/com/caicai/config/RedisConfig.java
src/main/java/com/caicai/config/SecurityConfig.java

src/main/java/com/caicai/email/EmailService.java

src/main/java/com/caicai/food/FoodItem.java
src/main/java/com/caicai/food/FoodItemRepository.java
src/main/java/com/caicai/food/UserFavouriteFood.java
src/main/java/com/caicai/food/UserFavouriteFoodRepository.java
src/main/java/com/caicai/food/FoodDtos.java
src/main/java/com/caicai/food/FatSecretClient.java
src/main/java/com/caicai/food/FoodService.java
src/main/java/com/caicai/food/FoodController.java


src/main/java/com/caicai/goal/Goal.java
src/main/java/com/caicai/goal/GoalController.java
src/main/java/com/caicai/goal/GoalDtos.java
src/main/java/com/caicai/goal/GoalRepository.java
src/main/java/com/caicai/goal/GoalService.java

src/main/java/com/caicai/config/RestTemplateConfig.java

src/main/java/com/caicai/log/FoodLog.java

src/main/java/com/caicai/log/FoodLogRepository.java
src/main/java/com/caicai/log/FoodLogDtos.java
src/main/java/com/caicai/log/FoodLogService.java
src/main/java/com/caicai/log/FoodLogController.java

src/main/java/com/caicai/user/User.java
src/main/java/com/caicai/user/UserRepository.java
src/main/java/com/caicai/user/UserService.java
src/main/java/com/caicai/user/UserDtos.java
src/main/java/com/caicai/user/UserController.java

src/main/java/com/caicai/water/WaterLog.java
src/main/java/com/caicai/water/WaterLogRepository.java
src/main/java/com/caicai/water/WaterDtos.java
src/main/java/com/caicai/water/WaterService.java
src/main/java/com/caicai/water/WaterController.java

src/main/java/com/caicai/weight/WeightLog.java
src/main/java/com/caicai/weight/WeightRepository.java
src/main/java/com/caicai/weight/WeightDtos.java
src/main/java/com/caicai/weight/WeightService.java
src/main/java/com/caicai/weight/WeightController.java

src/main/java/com/caicai/dashboard/DashboardDtos.java
src/main/java/com/caicai/dashboard/DashboardService.java
src/main/java/com/caicai/dashboard/DashboardController.java
---

## Current Task

Current task: Step 20 — Food search + Favourite foods frontend
---

## Known Issues / Blockers

- No reference design exists yet for BottomNav/Sidebar — need to design from scratch based on DESIGN.md spec
- DESIGN.md says mobile nav: Dashboard | Log | Trends | Goals | Settings (5 items)
- DESIGN.md says desktop: sidebar, hidden md:flex
- Active state: white icon + green dot indicator, never full green icon
- Must confirm desktop sidebar nav labels and icons with user before writing any code
- Step 18 blocked — user will present nav/sidebar design before any code is written.
- Do not start Step 18 until design is confirmed.


---

## Backlog
- Edit food log entry — PUT /api/food-logs/{id}, change amountGrams and mealType. Frontend: edit modal. Implement during polish pass.
- OpenFoodFacts replacement — find alternative food data API. Cached PostgreSQL data works in the meantime.
- Need to rethink our current designs after the whole frontend has been implemented
- Write backend service tests for all business logic once all backend steps are complete (steps 9–13).
  Cover: calorie/macro totals, goal progress calculations, dashboard aggregations, date boundary edge cases, ownership checks.
- AI food recommendations — suggest foods to user based on remaining
  daily macro goals. Implement after dashboard is built (step 16).
- Structured logging — add log levels and correlation IDs to make
  debugging production issues easier. Implement before first
  production deployment.
- Macro education tooltips — show a small info popup on each macro
  (protein, carbs, fat, calories) explaining what it does and why it matters.
  Extensible for when fiber, sodium, and sugar are added to the UI.
  Implement after dashboard is built (step 16).
-  Review all service methods for single point of failure — decide whether to use fault-tolerant try/catch per section (dashboard pattern)
   or let exceptions propagate (domain endpoints). Document the decision per feature during polish pass.
---

## How To Use This File

**Start of session:**
1. Paste CLAUDE.md
2. Paste this file
3. Say: "Continue from where we left off. Current task is [X]."
4. For any frontend task: discuss and confirm the design before writing code.
   Share reference screenshots if available. Agree on desktop layout explicitly.

**End of session:**
1. Check off completed bootstrap steps
2. Add any new decisions to "Decisions Made"
3. Update "Files Created So Far"
4. Set "Current Task" to the next step
5. Note any blockers
