# Caicai — Project Status

> Paste this file alongside CLAUDE.md at the start of every new chat session.
> Keep this file updated after every session.

---

## Current Status

**Phase:** started — ready to build  
**Last updated:** 2026-05-28 12:53

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
[ ] 9.  Food search (OpenFoodFacts + Redis cache)
[ ] 10. Favourite foods
[ ] 11. Food log
[ ] 12. Copy day feature
[ ] 13. Water tracking
[ ] 14. Weight tracking
[ ] 15. Goals (current, history)
[ ] 16. Dashboard (daily, weekly, monthly)
[ ] 17. Settings (profile, goals)
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

---

## Files Created So Far

_List every file created, so the next session knows what exists._
Frontend

src/components/ui/StatCard.jsx
src/components/ui/PageHeader.jsx
src/components/ui/EmptyState.jsx
src/components/ui/FilterPills.jsx
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


src/main/java/com/caicai/goal/Goal.java
src/main/java/com/caicai/goal/GoalController.java
src/main/java/com/caicai/goal/GoalDtos.java
src/main/java/com/caicai/goal/GoalRepository.java
src/main/java/com/caicai/goal/GoalService.java

src/main/java/com/caicai/config/RestTemplateConfig.java

src/main/java/com/caicai/log/FoodLog.java
src/main/java/com/caicai/log/FoodLogRepository.java

src/main/java/com/caicai/user/User.java
src/main/java/com/caicai/user/UserRepository.java
src/main/java/com/caicai/user/UserService.java
src/main/java/com/caicai/user/UserDtos.java
src/main/java/com/caicai/user/UserController.java

src/main/java/com/caicai/water/WaterLog.java
src/main/java/com/caicai/water/WaterRepository.java

src/main/java/com/caicai/weight/WeightLog.java
src/main/java/com/caicai/weight/WeightRepository.java


---

## Current Task

Step 9 — Food search (OpenFoodFacts + Redis cache)
---

## Known Issues / Blockers

_Anything broken, unclear, or blocking progress._

None.

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
