---
title: Apply Single Responsibility Principle
impact: CRITICAL
tags: struct, srp, solid, class-design
---

## Apply Single Responsibility Principle

Class should have only one reason to change. With multiple responsibilities, changing one risks breaking others.

**Incorrect (multiple responsibilities):**

```typescript
class UserService {
  private users: Map<string, User> = new Map()

  createUser(data: UserData): User {
    const user = { id: crypto.randomUUID(), ...data }
    this.users.set(user.id, user)

    // Sending email is a separate responsibility
    const emailBody = `Welcome ${user.name}! Your account is ready.`
    this.sendEmail(user.email, 'Welcome', emailBody)

    // Logging is a separate responsibility
    console.log(`[${new Date().toISOString()}] User created: ${user.id}`)

    return user
  }

  private sendEmail(to: string, subject: string, body: string): void {
    // SMTP logic here
  }
}
```

**Correct (separated responsibilities):**

```typescript
class UserService {
  constructor(
    private userRepository: UserRepository,
    private notificationService: NotificationService,
    private logger: Logger
  ) {}

  createUser(data: UserData): User {
    const user = this.userRepository.create(data)
    this.notificationService.sendWelcomeEmail(user)
    this.logger.info('User created', { userId: user.id })
    return user
  }
}

class UserRepository {
  private users: Map<string, User> = new Map()

  create(data: UserData): User {
    const user = { id: crypto.randomUUID(), ...data }
    this.users.set(user.id, user)
    return user
  }
}

class NotificationService {
  sendWelcomeEmail(user: User): void {
    const body = `Welcome ${user.name}! Your account is ready.`
    this.sendEmail(user.email, 'Welcome', body)
  }

  private sendEmail(to: string, subject: string, body: string): void {
    // SMTP logic here
  }
}
```

Reference: [Single Responsibility Principle](https://en.wikipedia.org/wiki/Single-responsibility_principle)
