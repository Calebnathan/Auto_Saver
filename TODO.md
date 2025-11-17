# Auto Saver - Modern UI/UX Overhaul TODO

## 🎯 Project Overview
Transform Auto Saver into a modern, 3-page navigation app with:
- **Home**: Personal budget dashboard with spending visualization
- **Race**: Competitive budgeting challenges between users
- **Social**: Collaborative savings goals with friends/family

---

## 📋 Phase 1: Architecture & Navigation Foundation ✅ COMPLETE

### 1.1 Dependencies & Setup ✅
- [x] Add MPAndroidChart library to `app/build.gradle.kts`
  ```kotlin
  implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
  ```
- [x] Add ViewPager2 dependency (if not already present)
- [x] Add Fragment KTX for modern fragment management
- [x] Sync gradle and verify build

### 1.2 Create New File Structure ✅
- [x] Create `app/src/main/java/com/example/auto_saver/ui/` package
- [x] Create subdirectories:
  - [x] `ui/home/` - For home screen fragments & viewmodels
  - [x] `ui/race/` - For competitive features
  - [x] `ui/social/` - For collaborative features
  - [x] `ui/components/` - For reusable UI components
  - [x] `ui/adapters/` - Keep or move existing adapters here

### 1.3 Bottom Navigation Icons & Resources ✅
- [x] Create/obtain vector drawable for home icon (use existing `ic_home.xml`)
- [x] Create vector drawable `ic_race_flag.xml` or use `ic_race_track_dial.xml`
- [x] Create vector drawable `ic_social_people.xml` (Material Icons: people/groups)
- [x] Add to `res/drawable/`

### 1.4 Menu & String Resources ✅
- [x] Create `res/menu/bottom_nav_menu.xml`:
  ```xml
  - item id: nav_home, icon: ic_home, title: "Home"
  - item id: nav_race, icon: ic_race_flag, title: "Race"
  - item id: nav_social, icon: ic_social_people, title: "Social"
  ```
- [x] Add strings to `res/values/strings.xml`:
  - `nav_home`, `nav_race`, `nav_social`
  - `under_construction`, `coming_soon`
  - Graph-related strings

---

## 📋 Phase 2: Main Activity Redesign ✅ COMPLETE

### 2.1 Update MainActivity Layout ✅
- [x] Create new `activity_main.xml` with:
  - [x] MaterialToolbar at top (persistent)
    - [x] Profile picture ImageView (clickable, left side)
    - [x] Title/app name (center)
    - [x] Settings IconButton (right side)
  - [x] FragmentContainerView for content (middle, takes remaining space)
  - [x] BottomNavigationView (bottom, persistent)
- [x] Remove old toggle buttons and view switching logic
- [x] Ensure proper Material3 theming

### 2.2 MainActivity.kt Refactor ✅
- [x] Implement `MainActivity` with bottom navigation:
  - [x] Setup BottomNavigationView listener
  - [x] Fragment transaction logic for nav switching
  - [x] Handle back press behavior (default)
  - [x] Keep top bar profile picture & settings functionality
- [x] Remove old view toggle logic (Dashboard/Categories toggle)
- [x] Migrate existing menu functionality to toolbar
- [x] Created placeholder fragments (HomeFragment, RaceFragment, SocialFragment)

---

## 📋 Phase 3: Home Screen (Page 1)

### 3.1 Home Fragment Structure
- [x] Create `ui/home/HomeFragment.kt`:
  - [x] Hosts ViewPager2 with TabLayout for sub-pages
  - [x] Tab 1: Dashboard
  - [x] Tab 2: Categories & Expenses
- [x] Create `fragment_home.xml` layout with TabLayout + ViewPager2

### 3.2 Dashboard Tab (Home > Dashboard)
- [x] Create `ui/home/DashboardFragment.kt`
- [x] Create `fragment_dashboard.xml` layout:
  - [x] Summary Card (total spent, expense count)
  - [x] **NEW: Spending Graph Card** (compact view) with LineChart placeholder
    - [x] Mini line chart showing last 7-30 days (data + styling)
    - [x] "View Details" button → opens GraphActivity
  - [x] Monthly Goal Progress Card (existing goal data)
  - [x] Quick action buttons (Add Expense, View Analytics)
- [x] Create `ui/home/DashboardViewModel.kt`:
  - [x] Observe expenses, totalSpent, goals from repositories
  - [x] Provide graph data (daily aggregated spending)
  - [x] Handle metric configuration (daily/weekly/monthly)

### 3.3 Spending Graph Component
- [x] Create `ui/components/SpendingGraphView.kt`:
  - [x] Custom View wrapping MPAndroidChart LineChart
  - [x] Configurable time range (7/14/30/90 days)
    - [x] Implemented 7/30/90-day presets on Dashboard graph
  - [x] Themed colors matching Material3
  - [x] Touch interactions, zoom, scroll (configured on Dashboard graph)
  - [x] Data point labels (x-axis day-of-month labels with density control)
- [ ] Firebase query for aggregated spending data:
  - [x] Daily totals for selected range (implemented in `DashboardViewModel` + `DashboardFragment`)
  - [ ] Category breakdowns (optional)
  - [x] Comparison with previous period (optional)

### 3.4 Categories & Expenses Tab (Home > Categories)
- [x] Create `ui/home/CategoriesFragment.kt` (placeholder)
- [x] Create `fragment_categories.xml` layout:
  - [x] RecyclerView for category/expense list (reuse grouped adapter)
  - [x] FAB for add actions (expense, category - currently launches AddExpenseActivity)
  - [x] Filter button (placeholder, no-op)
  - [x] Empty state
- [x] Migrate existing categories/expenses logic from MainActivity (now driven by repositories)
- [x] Create `ui/home/CategoriesViewModel.kt`

### 3.5 Enhanced GraphActivity
- [ ] Update existing `GraphActivity.kt`:
  - [x] Toolbar with back button
  - [x] Full-screen spending graph
  - [x] Metric selector (daily/weekly/monthly/yearly)
  - [x] Date range picker (start/end picker + textual summary)
  - [x] Export/share functionality
  - [x] Legend for categories (goal limit lines)
- [x] Update `activity_graph.xml` layout (range summary under start date)

---

## 📋 Phase 4: Race Screen (Page 2 - Competitive)

### 4.1 Firebase Data Models
- [ ] Create `data/model/RaceChallenge.kt`:
  ```kotlin
  data class RaceChallenge(
    val id: String,
    val createdBy: String,
    val participants: List<String>,  // user UIDs
    val budget: Double,
    val startDate: String,
    val endDate: String,
    val status: ChallengeStatus,
    val leaderboard: Map<String, Double>  // uid -> spending
  )
  ```
- [ ] Create `data/model/RaceParticipant.kt`

### 4.2 Firestore Schema
- [ ] Document structure in `firestore.rules`:
  ```
  /challenges/{challengeId}
    - createdBy, budget, period, participants[], status
    - /participants/{uid} → { spending, rank, lastUpdated }
  ```
- [ ] Add security rules for challenges
- [ ] Allow read if user is participant
- [ ] Allow write only to own participation data

### 4.3 Race Remote Data Source
- [ ] Create `data/firestore/RaceRemoteDataSource.kt`:
  - [ ] `observeChallenges(uid)` - get user's active challenges
  - [ ] `createChallenge(challenge)` - create new race
  - [ ] `joinChallenge(challengeId, uid)` - join existing race
  - [ ] `updateParticipantSpending(challengeId, uid, spending)` - sync spending
  - [ ] `deleteChallenge(challengeId)` - creator only

### 4.4 Race Repository
- [ ] Create `data/repository/UnifiedRaceRepository.kt`:
  - [ ] Implements Firestore-first pattern
  - [ ] Cache challenges in Room (optional)
  - [ ] Real-time listeners for leaderboard updates
  - [ ] Integrate with expense tracking to auto-update spending

### 4.5 Race UI Components
- [ ] Create `ui/race/RaceFragment.kt` (placeholder initially):
  - [ ] "Under Construction" message for now
  - [ ] Race icon/illustration
  - [ ] Brief description of upcoming feature
- [ ] Create `fragment_race.xml` layout

### 4.6 Race Implementation (Future)
- [ ] Create active challenges list view
- [ ] Create challenge detail screen with leaderboard
- [ ] Create new challenge creation dialog
- [ ] Join challenge via code/invite
- [ ] Real-time leaderboard updates
- [ ] Winner declaration & notifications
- [ ] Challenge history

---

## 📋 Phase 5: Social Screen (Page 3 - Collaborative)

### 5.1 Firebase Data Models
- [x] Create `data/model/FriendRequest.kt` and `data/model/FriendProfile.kt`:
  ```kotlin
  data class FriendRequest(
    val id: String,
    val fromUid: String,
    val toUid: String,
    val toEmail: String,
    val status: FriendRequestStatus,
    val createdAt: Long,
    val updatedAt: Long
  )
  ```
  ```kotlin
  data class FriendProfile(
    val uid: String,
    val email: String,
    val displayName: String?,
    val photoUrl: String?,
    val since: Long
  )
  ```
- [x] Extend user models so friend lookups can surface avatar + name in Social tab
- [ ] Create `data/model/CollaborativeGoal.kt`:
  ```kotlin
  data class CollaborativeGoal(
    val id: String,
    val name: String,  // e.g., "Trip to Japan"
    val createdBy: String,
    val participants: List<String>,
    val totalBudget: Double,
    val currentSaved: Double,
    val categories: List<GoalCategory>,  // food, flights, etc.
    val deadline: String?,
    val progress: Float  // 0-100
  )
  ```
- [ ] Create `data/model/GoalCategory.kt`:
  ```kotlin
  data class GoalCategory(
    val name: String,
    val budget: Double,
    val saved: Double
  )
  ```
- [ ] Create `data/model/GoalContribution.kt`

### 5.2 Firestore Schema
- [x] Document friend collections inside `users/{uid}`:
  ```
  /users/{uid}/friend_requests/{requestId}
    - fromUid, toUid, toEmail, status, createdAt, updatedAt
  /users/{uid}/friends/{friendUid}
    - friendUid, friendEmail, since
  ```
- [x] Rules: only owner can read/write their friend data; request sender can write initial request, receiver can accept/decline
- [x] Add Firestore indexes or email lookup helper (Auth query + cached lowercased email)
- [ ] Document structure in `firestore.rules`:
  ```
  /collaborative_goals/{goalId}
    - name, createdBy, participants[], totalBudget, deadline
    - /categories/{categoryId} → { name, budget, saved }
    - /contributions/{contributionId} → { uid, amount, date, categoryId }
  ```
- [ ] Add security rules
- [ ] Participants can read/write to their own contributions
- [ ] All participants can view goal progress

### 5.3 Social Remote Data Source
- [x] Create `data/firestore/FriendRemoteDataSource.kt`:
  - [ ] `sendFriendRequest(targetEmail)` → resolves email to uid, creates request doc under receiver
  - [ ] `observeFriendRequests(uid)` → stream pending/accepted requests
  - [ ] `acceptFriendRequest(requestId)` / `declineFriendRequest(requestId)`
  - [ ] `observeFriends(uid)` → realtime list for Social tab
- [ ] Create/extend `data/firestore/SocialRemoteDataSource.kt`:
  - [ ] `observeCollaborativeGoals(uid)` - user's shared goals
  - [ ] `createCollaborativeGoal(goal)` - start new group goal
  - [ ] `updateGoalCategory(goalId, category)` - update budget/saved
  - [ ] `addContribution(goalId, contribution)` - log savings
  - [ ] `inviteParticipant(goalId, email)` - send invite (re-use friend lookups when available)

### 5.4 Social Repository
- [x] Create `data/repository/UnifiedFriendRepository.kt`:
  - [ ] Wrap remote source, expose flows for friends + requests
  - [ ] Cache accepted friends in Room for offline support
  - [ ] Provide helper APIs for email search and request lifecycle
- [ ] Create/extend `data/repository/UnifiedSocialRepository.kt`:
  - [ ] Firestore-first pattern
  - [ ] Real-time progress updates
  - [ ] Aggregate contributions per category
  - [ ] Calculate overall progress
  - [ ] Surface friend list to collaborative goal invites

### 5.5 Social UI Components
- [x] Replace placeholder `SocialFragment` with friend-first dashboard:
  - [x] Friend summary card (friend count, quick actions)
  - [x] Reusable `FriendListAdapter` for accepted friends
  - [x] Pending invites chip/button showing count
- [x] Add "Add Friend" CTA (Material bottom sheet with email input, validation, success/empty states)
- [x] Add friend request handling UI to accept/decline invites
- [ ] Keep existing collaborative goals placeholder below friend list until Phase 5.6 ready

### 5.6 Social Implementation (Future)
- [ ] Active collaborative goals list
- [ ] Goal detail with category breakdown & progress bars
- [ ] Create new goal wizard
- [ ] Add categories to goal
- [ ] Log contributions dialog
- [ ] Participant management
- [ ] Progress notifications
- [ ] Goal completion celebration

---

## 📋 Phase 6: Data Aggregation & Graph Metrics

### 6.1 Expense Aggregation Utilities
- [ ] Create `utils/SpendingAggregator.kt`:
  - [ ] `aggregateByDay(expenses, dateRange)` → Map<String, Double>
  - [ ] `aggregateByWeek(expenses, dateRange)` → Map<String, Double>
  - [ ] `aggregateByMonth(expenses, dateRange)` → Map<String, Double>
  - [ ] `aggregateByCategory(expenses, dateRange)` → Map<String, CategoryTotal>

### 6.2 Graph Data Preparation
- [ ] Create `ui/components/GraphDataProvider.kt`:
  - [ ] Transform aggregated data to MPAndroidChart format
  - [ ] Handle empty states
  - [ ] Provide comparison data (current vs previous period)
  - [ ] Color coding for over/under budget

### 6.3 ConfigurableMetrics System
- [ ] Create preferences for graph configuration:
  - [ ] Default time range (7/30/90 days)
  - [ ] Metric type (daily/weekly/monthly)
  - [ ] Show/hide average line
  - [ ] Show/hide budget threshold
- [ ] Add UI in SettingsActivity or graph screen

### 6.4 Firebase Queries Optimization
- [ ] Index expenses collection by (uid, date)
- [ ] Implement pagination for large date ranges
- [ ] Cache aggregated data to reduce reads
- [ ] Background sync for graph data

---

## 📋 Phase 7: Testing & Refinement

### 7.1 Unit Tests
- [ ] Test `SpendingAggregator` calculations
- [ ] Test `DashboardViewModel` data flows
- [ ] Test `RaceRepository` (when implemented)
- [ ] Test `SocialRepository` (when implemented)

### 7.2 UI/UX Testing
- [ ] Navigation flow (bottom nav, tabs, back button)
- [ ] Graph interactions (zoom, scroll, touch)
- [ ] Empty states for all screens
- [ ] Loading states during Firebase fetches
- [ ] Error handling & retry mechanisms

### 7.3 Firebase Testing
- [ ] Verify security rules work correctly
- [ ] Test offline behavior (Firestore persistence)
- [ ] Monitor read/write costs
- [ ] Optimize queries

### 7.4 Design Polish
- [ ] Consistent spacing and margins
- [ ] Material3 color scheme throughout
- [ ] Smooth transitions and animations
- [ ] Dark mode support
- [ ] Accessibility (content descriptions, contrast)

---

## 📋 Phase 8: Migration & Cleanup

### 8.1 Existing Features Audit & Integration Plan

**Current Activities (11 total):**

1. **MainActivity** → WILL BE REFACTORED
   - Current: Toggle between Dashboard/Categories views
   - New: Host bottom navigation with fragments
   - Keep: Profile picture, settings button, menu functionality

2. **LoginActivity** → KEEP AS-IS
   - Firebase authentication with email/password
   - Navigate to MainActivity on success
   - No changes needed

3. **SignUpActivity** → KEEP AS-IS
   - User registration with Firebase
   - Profile photo upload
   - No changes needed

4. **AddExpenseActivity** → KEEP + ENHANCE
   - Current: Full-screen activity for adding expenses
   - Keep: All existing functionality (amount, category, date, time, description, photo)
   - Enhance: Could be converted to dialog/bottom sheet (optional)
   - Access from: FAB on Categories tab (Home page)

5. **AddCategoryActivity** → KEEP AS-IS
   - Full-screen activity for creating categories
   - Access from: Categories tab menu or "Manage Categories" button

6. **GoalsActivity** → INTEGRATE INTO DASHBOARD
   - Current: Separate activity for setting monthly min/max goals
   - New: Link from "Manage Goals" button on Dashboard
   - Keep: All existing goal setting functionality
   - Consider: Inline goal editing on dashboard (future enhancement)

7. **GraphActivity** → ENHANCE FOR NEW SYSTEM
   - Current: Basic graph view (needs verification of current implementation)
   - New: Full-screen detailed graph with:
     - Multiple metric types (daily/weekly/monthly)
     - Date range selector
     - Category breakdown
     - Export functionality
   - Access from: "View Details" button on Dashboard graph card

8. **CategoryAnalyticsActivity** → KEEP + ADD TO MENU
   - Current: Category spending breakdown by period
   - Keep: All period selection (this month, last month, this year, custom)
   - Access from: Dashboard menu or Categories tab menu
   - This is DIFFERENT from the new graph feature (analytics vs spending trend)

9. **ProfileActivity** → KEEP + UPDATE NAV
   - Current: User profile management, photo upload
   - Keep: All existing functionality
   - Access from: Top bar profile picture click

10. **SettingsActivity** → KEEP + UPDATE NAV
    - Current: App settings (theme toggle, etc.)
    - Keep: All existing settings
    - Add: Graph preferences (default range, metric type)
    - Access from: Top bar settings icon

11. **PhotoViewerActivity** → KEEP AS-IS
    - View full-screen expense photos
    - No changes needed

**Feature Accessibility Matrix:**

| Feature | Current Access | New Access Point | Priority |
|---------|---------------|------------------|----------|
| Add Expense | FAB in MainActivity | FAB in Categories tab | High |
| Add Category | Menu in MainActivity | Categories tab menu | High |
| Remove Category | Menu in MainActivity | Categories tab menu | High |
| View Expenses | Categories view toggle | Categories tab | High |
| Filter Expenses | Filter button | Categories tab toolbar | High |
| Monthly Goals | Menu → GoalsActivity | Dashboard "Manage Goals" button | High |
| View Graph | Menu → GraphActivity | Dashboard "View Details" button | High |
| Category Analytics | Menu → CategoryAnalyticsActivity | Dashboard/Categories menu | Medium |
| Profile | Menu → ProfileActivity | Top bar profile picture | High |
| Settings | Menu → SettingsActivity | Top bar settings icon | High |
| Theme Toggle | Settings menu | SettingsActivity | Medium |
| Logout | Menu | Top bar menu (retain) | High |
| Database Reset | Menu | SettingsActivity (move from menu) | Low |

### 8.2 Preserved Features Checklist
- [ ] ✅ Login/Sign up flows (no changes)
- [ ] ✅ Add expense with photo upload
- [ ] ✅ Create/delete categories
- [ ] ✅ View expenses grouped by category
- [ ] ✅ Expand/collapse category sections
- [ ] ✅ Filter expenses by date range
- [ ] ✅ Set monthly min/max spending goals
- [ ] ✅ View goal progress on dashboard
- [ ] ✅ Category spending analytics by period
- [ ] ✅ User profile management
- [ ] ✅ Profile photo upload
- [ ] ✅ Theme switching (light/dark mode)
- [ ] ✅ App settings
- [ ] ✅ Logout functionality
- [ ] ✅ Photo viewer for expenses

### 8.3 New Features Being Added
- [ ] 🆕 Bottom navigation (Home, Race, Social)
- [ ] 🆕 Tabbed interface on Home (Dashboard ↔ Categories)
- [ ] 🆕 Interactive spending graph on Dashboard
- [ ] 🆕 Full-screen enhanced graph view
- [ ] 🆕 Configurable graph metrics (daily/weekly/monthly)
- [ ] 🆕 Race challenges (competitive budgeting)
- [ ] 🆕 Social collaborative goals
- [ ] 🆕 Real-time leaderboards
- [ ] 🆕 Progress bars for collaborative savings

### 8.4 Code Cleanup
- [ ] Remove old MainActivity view toggle code
- [ ] Remove unused layout files
- [ ] Update navigation paths in all activities
- [ ] Consolidate duplicate code
- [ ] Add KDoc comments to new classes

### 8.3 Documentation
- [ ] Update README.md with new features
- [ ] Document Firebase schema in CLOUD.md
- [ ] Add screenshots of new UI
- [ ] Update AGENTS.md if needed

---

## 📋 Implementation Priority (Suggested Order)

### **IMMEDIATE (Foundation)**
1. ✅ Phase 1: Dependencies & file structure
2. ✅ Phase 2: MainActivity redesign with bottom nav
3. ✅ Phase 3.1-3.2: Home fragment with dashboard tab
4. ✅ Phase 3.3: Basic spending graph component

### **SHORT-TERM (Home Complete)**
5. ✅ Phase 3.4: Categories tab
6. ✅ Phase 3.5: Enhanced graph activity
7. ✅ Phase 6: Data aggregation & metrics

### **MID-TERM (Placeholders)**
8. ✅ Phase 4.5 & 5.5: Race & Social placeholder screens
9. ✅ Phase 7: Testing & refinement
10. ✅ Phase 8: Migration & cleanup

### **LONG-TERM (Full Features)**
11. Phase 4 (full): Race implementation
12. Phase 5 (full): Social implementation
13. Advanced features (notifications, invites, etc.)

---

## 🎨 Design Specifications

### Top Bar
- Height: 56dp
- Profile picture: 40dp circle, 8dp margin from left
- Settings icon: 24dp, 16dp margin from right
- Background: `colorPrimaryContainer` or `colorSurface`

### Bottom Navigation
- Height: 56dp (standard)
- Icons: 24dp
- Active color: `colorPrimary`
- Inactive color: `colorOnSurfaceVariant`
- Text: 12sp

### Graph Card (Dashboard)
- Height: 200dp (compact), 300dp+ (full screen)
- Corner radius: 16dp
- Padding: 16dp
- Chart colors: gradients from `colorPrimary` to `colorSecondary`

### Cards
- Corner radius: 12dp
- Elevation: 2dp
- Margin: 16dp horizontal, 8dp vertical
- Padding: 16dp

---

## 🔐 Firebase Security Rules Updates

### Challenges Collection
```javascript
match /challenges/{challengeId} {
  allow read: if request.auth != null && 
    request.auth.uid in resource.data.participants;
  allow create: if request.auth != null;
  allow update: if request.auth != null && 
    request.auth.uid in resource.data.participants;
  allow delete: if request.auth != null && 
    request.auth.uid == resource.data.createdBy;
}
```

### Collaborative Goals Collection
```javascript
match /collaborative_goals/{goalId} {
  allow read: if request.auth != null && 
    request.auth.uid in resource.data.participants;
  allow create: if request.auth != null;
  allow update: if request.auth != null && 
    request.auth.uid in resource.data.participants;
  allow delete: if request.auth != null && 
    request.auth.uid == resource.data.createdBy;
}
```

---

## 📝 Notes & Decisions

### Graph Library Choice
- **MPAndroidChart**: Mature, feature-rich, good documentation
- Alternative: Vico (modern, Jetpack Compose ready for future migration)

### Navigation Pattern
- Bottom nav (primary): Home, Race, Social
- Tabs (secondary): Dashboard ↔ Categories on Home screen
- This keeps all personal tracking in one place

### Data Sync Strategy
- Race challenges: Batch update spending once daily or on app open
- Collaborative goals: Real-time updates when contributions made
- Graph data: Cache last 90 days, fetch older on demand

### Future Enhancements
- [ ] Push notifications for race updates
- [ ] In-app messaging for collaborative goals
- [ ] Challenge templates (weekly, monthly)
- [ ] Gamification (badges, streaks)
- [ ] Export reports (PDF, CSV)
- [ ] Widget support

---

## ⚠️ Important Reminders

1. **Firebase costs**: Monitor reads/writes, use offline persistence
2. **User privacy**: Ensure users consent before sharing data in races/goals
3. **Testing**: Test with multiple accounts for social features
4. **Performance**: Paginate large lists, lazy load graph data
5. **Theme consistency**: Use Material3 theming system throughout
6. **Accessibility**: Add content descriptions, support TalkBack
7. **Error handling**: Graceful degradation when Firebase unavailable

---

## 🚀 Getting Started

1. Read through this entire TODO
2. Set up development environment
3. Start with Phase 1 (dependencies)
4. Follow implementation priority order
5. Test frequently, commit often
6. Update this TODO as you progress

---

**Last Updated**: 2025-11-17
**Status**: Phase 1 & 2 Complete ✅ | Phase 3.1 Home Tabs Structure Complete ✅
**Next Step**: Implement Phase 3.2 Dashboard Tab (cards, graph, viewmodel)
**Build Status**: ✅ BUILD SUCCESSFUL in 40s (all 74 tests passed)
**Recent Improvements**:
- Cleaned up MainActivity.kt unused imports (Menu, MenuItem)
- Simplified RaceFragment.kt and SocialFragment.kt (removed redundant TextView manipulation)
- All fragments now properly leverage layout resources
