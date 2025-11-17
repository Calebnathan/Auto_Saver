# AGENTS GUIDE (ROOT)

This file applies to the entire repo.

## Project Overview

- Android app: `com.example.auto_saver`
- Local DB: Room (`AppDatabase`) currently used for users, categories, expenses, goals.
- Cloud: Using Firestore with email + password authentication.

## Cloud Architecture (must read)

- Use **Firebase Authentication** as the single source of identity.
- Use `uid` from Firebase Auth as the only user ID in cloud data.
- All user-specific Firestore data must live under `users/{uid}/…`.
- Do not store passwords in Firestore or Room; all auth goes through Firebase Auth.

## Local vs Cloud

- Room may be used as a cache/offline layer, but **Firestore is the source of truth** for user data once cloud integration is complete.
- When adding new features that involve persistence, prefer Firestore first and keep any Room usage in sync with the schema.

## Coding Conventions

- Language: Kotlin for app code.
- Keep changes minimal, focused, and consistent with existing style.
- Do not add license/copyright headers.
- Prefer clear, descriptive names over one-letter variables.
- Avoid adding inline comments unless explicitly requested by the user.

## Agent Behavior

- Do not weaken Firebase security rules (no `allow read, write: if true` in production).
- Treat `FirebaseAuth.currentUser` being null as a hard error that should redirect to login.
- When in doubt about data shape, align with the structures documented in `CLOUD.md`.

