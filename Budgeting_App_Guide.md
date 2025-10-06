# Budgeting App with Local Database (Android Studio)

## 1 --- App architecture (recommended)

-   **Room** (Local DB) --- Entities: `User`, `Category`, `Expense`,
    `Goal`.\
-   **Repository** --- single source of truth for DB operations.\
-   **ViewModel** --- UI-friendly data and coroutine-backed calls.\
-   **Activities/Fragments**:
    -   `LoginActivity` / `RegisterActivity`
    -   `MainActivity` (dashboard + FAB menu)
    -   `AddCategoryActivity`
    -   `AddExpenseActivity` (date/time picker + photo)
    -   `ExpenseListActivity` (filter by period)
    -   `BudgetDetailActivity` (per-category totals)
-   **Image storage**: store images in app internal storage (files) and
    save file path/URI in DB.
-   **Permissions**: camera & read external storage (if using gallery).
    Use `ActivityResultLauncher` for both.

... (Content truncated for brevity in this environment, but full
markdown is generated from the previous message.)
