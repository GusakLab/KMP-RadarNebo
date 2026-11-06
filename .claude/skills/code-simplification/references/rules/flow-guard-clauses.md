---
title: Use Guard Clauses for Preconditions
impact: HIGH
tags: flow, guards, validation, preconditions, defensive-programming
---

## Use Guard Clauses for Preconditions

Guard clauses check all preconditions at function entry and exit immediately on failure. Clear contract: past the guards, every required condition holds.

**Incorrect (scattered validations):**

```typescript
function sendNotification(user: User, message: string, channel: Channel): void {
  let result;

  if (user && user.email) {
    if (message && message.trim().length > 0) {
      const sanitized = sanitizeMessage(message);

      if (channel === 'email') {
        if (user.emailVerified) {
          result = emailService.send(user.email, sanitized);
        } else {
          console.log('Email not verified');
        }
      } else if (channel === 'sms') {
        if (user.phone) {
          if (user.phoneVerified) {
            result = smsService.send(user.phone, sanitized);
          } else {
            console.log('Phone not verified');
          }
        } else {
          console.log('No phone number');
        }
      }
    } else {
      console.log('Empty message');
    }
  } else {
    console.log('Invalid user');
  }

  return result;
}
```

**Correct (guard clauses at entry):**

```typescript
function sendNotification(user: User, message: string, channel: Channel): void {
  // Guard clauses - all preconditions checked upfront
  if (!user?.email) {
    throw new ValidationError('User must have an email address');
  }
  if (!message?.trim()) {
    throw new ValidationError('Message cannot be empty');
  }
  if (!['email', 'sms'].includes(channel)) {
    throw new ValidationError(`Invalid channel: ${channel}`);
  }

  const sanitized = sanitizeMessage(message);

  // Channel-specific guards
  if (channel === 'email') {
    if (!user.emailVerified) {
      throw new ValidationError('Email address not verified');
    }
    return emailService.send(user.email, sanitized);
  }

  if (channel === 'sms') {
    if (!user.phone) {
      throw new ValidationError('User must have a phone number for SMS');
    }
    if (!user.phoneVerified) {
      throw new ValidationError('Phone number not verified');
    }
    return smsService.send(user.phone, sanitized);
  }
}
```

### Related

- See also [`flow-early-return`](flow-early-return.md) for reducing nesting with early returns generally

### When NOT to Apply

- Partial/graceful degradation required (use defaults instead)
- Hot paths where exceptions cost too much
- Tolerant readers handling malformed input
