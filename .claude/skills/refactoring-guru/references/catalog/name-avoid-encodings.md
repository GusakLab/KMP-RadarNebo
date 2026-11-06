---
title: Avoid Type Encodings in Names
impact: HIGH
tags: name, encodings, hungarian-notation, prefixes
---

## Avoid Type Encodings in Names

Don't embed type information in names. Encoded types become lies after refactoring.

**Incorrect (type encodings in names):**

```typescript
interface IUserService {  // 'I' prefix for interface
  getUser(userId: string): User
}

class UserServiceImpl implements IUserService {  // 'Impl' suffix
  private strUserName: string  // Hungarian notation
  private arrUserIds: string[]  // Type in name
  private nUserCount: number  // 'n' for number
  private bIsActive: boolean  // 'b' for boolean
  private oUserConfig: UserConfig  // 'o' for object
  private lstUsers: User[]  // 'lst' for list

  getUserByIdString(userIdStr: string): User {  // Redundant 'String'
    return this.userMapObject.get(userIdStr)
  }
}
```

**Correct (clean names, no encodings):**

```typescript
interface UserService {  // No 'I' prefix needed
  getUser(userId: string): User
}

class DefaultUserService implements UserService {  // Descriptive, not 'Impl'
  private userName: string
  private userIds: string[]
  private userCount: number
  private isActive: boolean
  private userConfig: UserConfig
  private users: User[]

  getUser(userId: string): User {
    return this.userMap.get(userId)
  }
}
```

**Alternatives to prefixes:**
- Instead of `IUserService`: `UserService` for interface, `HttpUserService` for implementation
- Instead of `Impl` suffix: descriptive names like `CachedUserService`, `MockUserService`
- Instead of Hungarian notation: let type system and IDE show types

**When encodings are acceptable:**
- Language conventions require them (C# interfaces commonly use `I`)
- Distinguishing otherwise identical names (`abstract class User` vs `class UserEntity`)

Reference: [Clean Code - Meaningful Names](https://www.oreilly.com/library/view/clean-code-a/9780136083238/)
