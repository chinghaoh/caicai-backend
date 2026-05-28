# Caicai — Project Status

> Paste this file alongside CLAUDE.md at the start of every new chat session.
> Keep this file updated after every session.

---

## Current Status

**Phase:** started — ready to build  
**Last updated:** 2026-05-28

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
[ ] 13. Settings backend (profile, goals)
[ ] 14. Shared frontend components (ui/)
[ ] 15. apiClient + SessionExpiredModal
[ ] 16. Auth frontend pages
[ ] 17. Onboarding flow + AI goal suggestion frontend
[ ] 18. Food search + Favourite foods frontend
[ ] 19. Food log + Copy day frontend
[ ] 20. Water tracking frontend
[ ] 21. Weight tracking frontend
[ ] 22. Goals frontend
[ ] 23. Dashboard frontend
[ ] 14. Settings frontend
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
= OpenFoodFacts rate limiting is a known issue — try to look for alternative later, cached PostgreSQL data still work

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
src/apiClient.js
src/App.jsx
src/pages/auth/Login.jsx
src/pages/auth/Register.jsx
src/pages/auth/ForgotPassword.jsx
src/pages/auth/ResetPassword.jsx

src/components/ui/RadioCard.jsx
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
src/main/java/com/caicai/food/OpenFoodFactsClient.java
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


---

## Current Task

Step 13 — Settings backend (profile, goals)
---

## Known Issues / Blockers

_Anything broken, unclear, or blocking progress._

None.

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

**End of session:**
1. Check off completed bootstrap steps
2. Add any new decisions to "Decisions Made"
3. Update "Files Created So Far"
4. Set "Current Task" to the next step
5. Note any blockers
