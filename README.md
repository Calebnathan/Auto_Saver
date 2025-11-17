# 🚗 Auto Saver

[![Android CI](https://github.com/Calebnathan/Auto_Saver/actions/workflows/android-ci.yml/badge.svg)](https://github.com/Calebnathan/Auto_Saver/actions/workflows/android-ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

Prototype for a car themed budgeting Application

## 📱 Overview
Auto Saver is an Android budgeting application designed to help users track their expenses and manage their finances effectively. The app features a clean, intuitive interface with category-based expense organization and real-time spending summaries.

The modern UI/UX overhaul introduces a three-page navigation structure:
- 🏠 **Home** – Personal dashboard for your spending and goals (with Dashboard and Categories tabs)
- 🏁 **Race** – Competitive budgeting challenges between users
- 🤝 **Social** – Collaborative savings goals with friends and family
- 👥 **Friend Network (coming soon)** – Add friends via email, accept invites, and see their progress inside the Social tab

## ✨ Features
- 🔐 **User Authentication** - Secure login and signup system
- 💰 **Expense Tracking** - Add, view, and organize expenses by category
- 📂 **Category Management** - Create custom categories to organize your spending
- 📊 **Expense Grouping** - Expenses automatically grouped by category with expandable/collapsible views
- 📈 **Spending Insights** - MPAndroidChart-driven graphs with 7/30/90 day presets, configurable metrics, and a shareable full-screen view
- 💳 **Spending Summary** - View total spent and expense count at a glance
- 👥 **Friend Network** - Invite friends via email, manage pending requests, and stay synced in the Social tab
- 👤 **User Profile** - Manage your account information
- 🎨 **Material Design 3** - Modern, beautiful interface

## 🔄 Workflow

### Getting Started
1. **Sign Up** - Create a new account with your credentials
2. **Login** - Access the app with your login ID and password
3. **Welcome** - See your personalized greeting and default categories

### Daily Use
1. **Navigate the App**
   - Use the bottom navigation bar to switch between Home, Race, and Social.
   - On the Home screen, use the tabs to switch between the **Dashboard** and **Categories** views (early-stage placeholders for upcoming features).

2. **Track Expenses** 
   - From Home, open the Categories view (or use the \"Quick Add\" action on the Dashboard once fully wired).
   - Tap the ➕ button in the Total Spent or relevant action area.
   - Enter amount, description, category, and date.
   - Save to see it appear under the category.

3. **Manage Categories**
   - Use the add action (➕) in the Home area.
   - Create categories (e.g., Transportation, Entertainment).
   - Save to start organizing expenses.

4. **View Your Spending**
   - See total spent, goal progress, and the interactive spending graph on Home (Dashboard tab).
   - Tap **View Details** on the graph card to open the full-screen trends view with metric toggles and sharing.
   - Browse the Categories tab to inspect category-level expenses.
### Race Screen 

The Race screen enables competitive/collaborative spending challenges:

Key Features:

• Challenge Lists: Shows active and completed challenges in separate sections
• Create Challenges: FAB opens bottom sheet to create new challenges with name, budget, and date range
• Join by Code: Users can join existing challenges using an invite code
• Challenge Details: Tap any challenge to view detailed info (participants, leaderboard, spending)
• Status Management: Tracks challenge status (Pending, Active, Completed, Cancelled)
• Spending Sync: Syncs user expenses to update challenge leaderboards
• Pull-to-Refresh: Swipe down to reload challenges
• Session Validation: Requires Firebase Auth login to create/join challenges

Architecture:

•  RaceFragment : UI layer with state-based views (loading, empty, error, success)
•  RaceViewModel : Manages challenge lifecycle through  UnifiedRaceRepository
• Uses sealed classes for UI state and events
• Observes challenges in real-time from Firestore
• Validates inputs (dates, budgets, names) before creating challenges

Both screens follow MVVM architecture with Firebase Auth as the identity source and Firestore as the cloud data backend.
The Social screen manages friend connections through Firebase Authentication and Firestore:

Key Features:

• Friend Management: Displays a list of connected friends and pending friend requests in separate RecyclerViews
• Add Friends: Users can send friend invites by email via a bottom sheet dialog
• Accept/Decline Requests: Incoming friend requests can be accepted or declined
• Remove Friends: Users can remove existing friends from their list
• Real-time Updates: Uses Firestore flows to observe friends and requests in real-time
• Session Validation: Checks Firebase Auth current user; disables features if not logged in
• Error Handling: Shows error messages via Snackbars with error coloring

Architecture:

•  SocialFragment : UI layer with RecyclerViews for friends and requests
•  SocialViewModel : Business logic, validates email format, handles friend operations through  UnifiedFriendRepository
• State is managed through StateFlow for friends/requests lists and SharedFlow for one-time UI events

### Social Screen 

The Social screen manages friend connections through Firebase Authentication and Firestore:

Through this feature the user has the ability to add friends via email, accept/decline incoming requests, and remove existing friends. The screen displays connected friends and pending requests in separate RecyclerViews, updating in real-time using Firestore flows.
once the user has added friends they will be able to see their progress inside the Social tab on the Home screen.
key Features:
• Friend Management: Displays a list of connected friends and pending friend requests in separate RecyclerViews
• Add Friends: Users can send friend invites by email via a bottom sheet dialog
• Accept/Decline Requests: Incoming friend requests can be accepted or declined
• Remove Friends: Users can remove existing friends from their list
• Real-time Updates: Uses Firestore flows to observe friends and requests in real-time
• Session Validation: Checks Firebase Auth current user; disables features if not logged in
• Error Handling: Shows error messages via Snackbars with error coloring   \
Architecture:
•  SocialFragment : UI layer with RecyclerViews for friends and requests
•  SocialViewModel : Business logic, validates email format, handles friend operations through  Unified
FriendRepository
• State is managed through StateFlow for friends/requests lists and SharedFlow for one-time UI events

### Menu Options
- ⚙️ **Settings** - Configure app preferences
- 👤 **Profile** - Edit your account information  
- 🔄 **Reset Database** - Clear all data
- 🚪 **Logout** - Sign out of your account

Final POE Video Presentation - https://youtu.be/0px-C5LIeUM
## 📄 License
This project is a prototype for educational purposes.

## 📚 References 

The following references include the sites and videos used in helping create this prototype.

Title: Step-by-Step: Setting Up and Implementing Room Database in Android
Author: Shamsuddoha Ranju
Date: 18/01/2024 
Code Version:(N/A)
Availability: https://medium.com/@sdranju/step-by-step-how-to-setting-up-and-implementing-room-database-aeb211c56702

Title: TextInputEditText - Material Design Edit Text | Android Studio Tutorial
Author: Stevdza-San
Date:  22/03/2021
Code Version:(N/A)
Availability: https://youtu.be/IxhIa3eZxz8?si=POAVCzTzDF1I-pHj

Title: Google Fonts
Author: Google
Date:  (N/A)
Code Version:(N/A)
Availability: https://fonts.google.com/icons

Anthropic. (2025) Claude Sonnet 4.5. Anthropic. Available at:https://www.anthropic.com/claude/sonnet (Accessed: 29/09/2025).

OpenAI. (2025) Chat-GPT 5.0. OpenAI. Available at: https://chatgpt.com/c/68de5ffd-be60-8331-9bc3-2b2177f31b8c (Accessed: 29/09/2025)

The link to our demo video: https://youtu.be/IypmBm6VzJk
