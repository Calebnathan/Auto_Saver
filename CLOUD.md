# Cloud Architecture: Firebase Auth + Firestore

This document describes the preferred cloud data model and login flow for Auto Saver when backed by Firebase Authentication + Cloud Firestore. It is written for other agents that will implement or refactor code in this repo.

## 1. Core Principles

- Single source of identity: use Firebase Authentication as the only authority for user accounts.
- UID everywhere: use `FirebaseAuth.getInstance().currentUser?.uid` as the canonical user ID.
- Per-user data isolation: all user-specific documents live under `users/{uid}/…`.
- No passwords outside Auth: do not store plaintext or hashed passwords in Firestore or Room.
- Room is optional: keep Room only as an offline cache; Firestore is the source of truth.

## 2. Firestore Data Model

Map the existing Room entities (`User`, `Category`, `Expense`, `Goal`) to Firestore as follows.

### 2.1 Collections & Documents

- `users/{uid}`
  - Doc ID: `uid` from Firebase Auth.
  - Fields:
    - `fullName: string`
    - `contact: string`
    - `profilePhotoPath: string | null` (local file path, not uploaded to cloud)
    - `createdAt: Timestamp`
    - `updatedAt: Timestamp`

- `users/{uid}/categories/{categoryId}`
  - Mirrors `Category`.
  - Fields:
    - `name: string`
    - `createdAt: Timestamp`
    - `updatedAt: Timestamp`

- `users/{uid}/goals/{monthId}`
  - Mirrors `Goal`.
  - Doc ID: `monthId` like `2024-11`.
  - Fields:
    - `month: string`
    - `minGoal: number`
    - `maxGoal: number`
    - `createdAt: Timestamp`
    - `updatedAt: Timestamp`

- `users/{uid}/expenses/{expenseId}`
  - Mirrors `Expense`.
  - Fields:
    - `date: string` (ISO `yyyy-MM-dd`) or `Timestamp`
    - `amount: number`
    - `description: string | null`
    - `categoryId: string`
    - `startTime: string | null`
    - `endTime: string | null`
    - `photoPath: string | null` (local file path, not uploaded to cloud)
    - `createdAt: Timestamp`
    - `updatedAt: Timestamp`

Do not store `userId` inside these documents; it is implied by the path.

## 3. Auth Flow

### 3.1 Sign Up

1. Validate inputs locally (full name, contact, email/username, password).
2. Call `FirebaseAuth.createUserWithEmailAndPassword(email, password)`.
3. On success:
   - Get `uid = auth.currentUser!!.uid`.
   - Create `users/{uid}` document with profile fields.
   - If a profile photo exists:
     - Save it to local app storage
     - Store the local file path in `profilePhotoPath` (not uploaded to cloud)

### 3.2 Login

1. Call `FirebaseAuth.signInWithEmailAndPassword(email, password)`.
2. On success:
   - Get `uid = auth.currentUser!!.uid`.
   - Read `users/{uid}` to get profile data.
   - Store `uid` and `fullName` in `UserPreferences` instead of an integer Room ID.

### 3.3 Session Management

- Use `FirebaseAuth.getInstance().currentUser` as the primary session check.
- `UserPreferences` should cache:
  - `uid` (string)
  - `userName` (e.g., full name)
- `isLoggedIn()` should verify both local prefs and `currentUser != null`.
- On logout:
  - Call `FirebaseAuth.signOut()`.
  - Clear `UserPreferences` session data.

## 4. Firestore Security Rules

Keep all user data isolated by UID.

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /users/{userId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;

      match /categories/{categoryId} {
        allow read, write: if request.auth != null && request.auth.uid == userId;
      }

      match /goals/{goalId} {
        allow read, write: if request.auth != null && request.auth.uid == userId;
      }

      match /expenses/{expenseId} {
        allow read, write: if request.auth != null && request.auth.uid == userId;
      }
    }
  }
}
```

Agents may add validation (e.g., `amount > 0`, `minGoal <= maxGoal`) but must preserve per-user isolation.

## 5. Local Photo Storage

Photos (profile and expense) are stored locally on the device, not uploaded to Firebase Storage:
- Profile photos: Saved in app-specific internal storage with unique filenames
- Expense photos: Saved in app-specific internal storage with unique filenames
- Only the local file paths are stored in Firestore (in `profilePhotoPath` and `photoPath` fields)
- Photos remain device-specific and are not synced across devices

## 6. Agent Guidelines

- Always derive the current user from `FirebaseAuth.currentUser`, not Room.
- Never implement custom password storage or checks.
- Read/write only within `users/{uid}/…` paths.
- Treat Room (if used) as a cache; do not diverge from Firestore schema.
- For graphs/analytics, query `users/{uid}/expenses` with date ranges and aggregate client-side unless a Cloud Function is specified.
