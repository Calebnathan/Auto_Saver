# Cloud Architecture Documentation

This document describes the Firestore cloud data architecture for Auto Saver.

## Authentication

- **Firebase Authentication** is the single source of identity
- All user IDs (`uid`) come from Firebase Auth
- Passwords are never stored in Firestore or Room
- `FirebaseAuth.currentUser` being null should redirect to login

## Collections Overview

### `public_users/{uid}`

**Purpose:** Publicly searchable user profiles for friend discovery.

**Security:** Any authenticated user can read. Only the profile owner can write.

**Schema:**
```typescript
{
  email: string,              // User's email (lowercase recommended)
  emailLowercase: string,     // Normalized email for case-insensitive queries
  displayName: string,        // User's full name
  photoUrl?: string,          // Optional profile photo URL/path
  createdAt: Timestamp,
  updatedAt: Timestamp
}
```

**Notes:**
- This collection is automatically synced when users create/update their profile
- Contains only safe, public information needed for friend discovery
- Enables email-based friend searches without exposing private user data

### `users/{uid}`

**Purpose:** Private user profile data.

**Security:** Only the owner can read/write.

**Schema:**
```typescript
{
  fullName: string,
  contact: string,            // Email or phone (7-20 chars)
  contactLowercase?: string,  // Lowercase version for queries
  profilePhotoPath?: string,
  createdAt: Timestamp,
  updatedAt: Timestamp
}
```

**Subcollections:**

#### `users/{uid}/categories/{categoryId}`
```typescript
{
  name: string,
  createdAt: Timestamp,
  updatedAt: Timestamp
}
```

#### `users/{uid}/goals/{month}`
```typescript
{
  month: string,          // Format: "YYYY-MM" (also used as document ID)
  minGoal: number,
  maxGoal: number,
  createdAt: Timestamp,
  updatedAt: Timestamp
}
```

#### `users/{uid}/expenses/{expenseId}`
```typescript
{
  categoryId: string,
  date: string,           // Format: "YYYY-MM-DD"
  amount: number,         // >= 0.01
  description?: string,
  startTime?: string,
  endTime?: string,
  photoPath?: string,
  createdAt: Timestamp,
  updatedAt: Timestamp
}
```

#### `users/{uid}/friend_requests/{requestId}`

**Purpose:** Friend requests received by this user.

**Schema:**
```typescript
{
  fromUid: string,        // UID of requester
  toUid: string,          // UID of recipient (matches {uid})
  fromEmail: string,
  toEmail: string,
  status: "PENDING" | "ACCEPTED" | "DECLINED",
  createdAt: Timestamp,
  updatedAt: Timestamp
}
```

**Security:**
- Anyone authenticated can create (with restrictions)
- Only the request recipient can read/update/delete
- Document ID must match the requester's UID

#### `users/{uid}/friends/{friendUid}`

**Purpose:** List of accepted friends.

**Schema:**
```typescript
{
  friendUid: string,      // UID of the friend
  friendEmail: string,
  displayName: string,
  photoUrl?: string,
  since: Timestamp,       // When friendship was established
  createdAt: Timestamp,
  updatedAt: Timestamp
}
```

**Security:**
- Only the owner can read/write
- Automatically added when a friend request is accepted

### `collaborative_goals/{goalId}`

**Purpose:** Shared savings goals between friends.

**Schema:**
```typescript
{
  name: string,
  description?: string,
  createdBy: string,          // UID of creator
  participants: string[],     // Array of participant UIDs
  totalBudget: number,
  currentSaved: number,
  categories: GoalCategory[],
  deadline?: string,
  status: "ACTIVE" | "COMPLETED" | "CANCELLED",
  createdAt: Timestamp,
  updatedAt: Timestamp
}
```

**Subcollection:**

#### `collaborative_goals/{goalId}/contributions/{contributionId}`
```typescript
{
  goalId: string,
  categoryId: string,
  uid: string,            // Contributor's UID
  amount: number,
  date: string,           // "YYYY-MM-DD"
  note?: string,
  createdAt: Timestamp
}
```

**Security:** Contributions are immutable after creation.

### `challenges/{challengeId}`

**Purpose:** Competitive spending challenges (Race feature).

**Schema:**
```typescript
{
  name: string,
  createdBy: string,          // UID of creator
  createdByEmail: string,
  budget: number,             // Target budget
  startDate: string,          // "YYYY-MM-DD"
  endDate: string,            // "YYYY-MM-DD"
  status: "PENDING" | "ACTIVE" | "COMPLETED" | "CANCELLED",
  participants: string[],     // Array of participant UIDs
  inviteCode: string,         // 6-character code for joining
  createdAt: Timestamp,
  updatedAt: Timestamp
}
```

**Subcollection:**

#### `challenges/{challengeId}/participants/{participantUid}`
```typescript
{
  uid: string,
  email: string,
  displayName?: string,
  totalSpent: number,
  rank: number,
  joinedAt: Timestamp,
  lastSyncedAt: Timestamp
}
```

## Data Flow

### Creating/Updating User Profiles
1. User updates profile in app
2. App writes to both `users/{uid}` AND `public_users/{uid}` atomically
3. `public_users` stays in sync for friend searches

### Adding Friends
1. User enters friend's email
2. App queries `public_users` collection by `emailLowercase`
3. If found, creates friend request in `users/{targetUid}/friend_requests/{currentUid}`
4. Target user sees request in their friend requests list
5. On acceptance:
   - Friend request status updated to "ACCEPTED"
   - Friend documents created in both users' `friends` subcollections

### Collaborative Goals
1. Users can only see goals they participate in
2. All participants can add contributions
3. Contributions update the goal's `currentSaved` value

### Race Challenges
1. Creator generates 6-char invite code
2. Friends join using the code
3. Participant spending is synced from their expenses
4. Leaderboard ranks by total spending vs budget

## Security Considerations

- **Never** weaken security rules to allow unrestricted access
- User data is scoped under `users/{uid}` for privacy
- `public_users` contains ONLY safe, searchable data
- Always check `request.auth.uid` matches document owner
- Firebase Auth must be the source of truth for identity
- Use batch writes to maintain consistency across collections

## Indexing

Firestore may require composite indexes for complex queries. Create them via:
- Firebase Console → Firestore → Indexes
- Or follow the error link when queries fail in development

Common indexes needed:
- `public_users` collection: `emailLowercase` (ascending)
- `users/{uid}/expenses` subcollection: `date` (descending), `categoryId` (ascending)
- `challenges` collection: `inviteCode` (ascending), `status` (ascending)

## Migration Notes

See `MIGRATION_FRIEND_LOOKUP.md` for instructions on migrating existing user data to the new `public_users` collection.