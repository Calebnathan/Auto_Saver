# ✅ Summary of Requirements & Task Delegation Plan

## ✅ Summary of Requirements

### Core App Features
- User **login** system with username + password.  
- Ability to create **categories** (e.g., Food, Travel).  
- Ability to create an **expense entry** (date, start/end time, description, category).  
- Option to **add a photograph** to an expense entry.  
- Ability to set **minimum and maximum monthly spending goals**.  
- Ability to view a **list of expense entries** within a user-selected period.  
- If photos exist for entries, they must be **accessible from the list**.  
- Ability to view the **total spent per category** during a user-selected period.  
- **Data persistence**: store everything in a local database (SQLite, RoomDB, or similar).  

### Technical & Development Requirements
- App must have a **user-friendly UI** that handles invalid inputs gracefully.  
- App should run with only **minor bugs**.  
- Version control with **GitHub** (repository, README, frequent commits).  
- **Automated testing** with GitHub Actions (build and run tests to ensure portability).  

### Demonstration Requirements
- A **professional demo video** showing app features.  
- Video must include a **voiceover** explaining features.  
- Video must be **compressed** for easy upload.  

---

## ✅ Task Delegation Plan

### Developers (2 people)
**Developer 1 (Backend & Database):**
- Implement **login system** with username/password.  
- Set up **SQLite/RoomDB database** for data persistence.  
- Handle **expense entry CRUD (Create, Read, Update, Delete)** operations.  
- Implement **minimum/maximum goals logic**.  

**Developer 2 (Frontend & Integration):**
- Implement **UI logic** for creating categories, adding expenses, and attaching photos.  
- Build the **list view** with filters for selectable time periods.  
- Implement **expense summaries (total per category)**.  
- Connect UI with backend (database integration).  
- Set up **GitHub Actions** for automated testing & builds.  

---

### Artists/Designers (2 people)
**Designer 1 (UI/UX Designer):**
- Design a **clean, accessible interface** (buttons, menus, input fields).  
- Ensure **ease of use** (good contrast, readable fonts, logical navigation).  
- Create **wireframes/mockups** before development.  
- Support developers in implementing **error feedback states** (e.g., wrong password, invalid input).  

**Designer 2 (Graphic/Media Designer):**
- Create **icons and visual assets** for categories (e.g., food, travel, bills).  
- Design app **branding elements** (logo, splash screen, color scheme).  
- Assist with the **demo video production** (editing, voiceover script visuals).  
- Prepare **compressed graphics/media** optimized for mobile performance.  

---

## ✅ Team Collaboration Notes
- **Shared Responsibilities:**  
  - Everyone contributes to the **README & GitHub commits**.  
  - Designers + Developers should work closely to ensure UI mockups match implementation.  
  - Demo video: Developers show functionality, Designers make it polished.  
