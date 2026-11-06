---
title: Use Consistent Vocabulary Throughout
impact: MEDIUM-HIGH
tags: name, consistency, vocabulary, ubiquitous-language, domain
---

## Use Consistent Vocabulary Throughout

One concept with many names forces readers to map synonyms. Pick one term per concept. Use it everywhere: code, comments, documentation, conversation.

**Incorrect (inconsistent terminology):**

```typescript
// Bad: Same concept, different names
class UserService {
  fetchUser(id: string): User { ... }       // "fetch"
  getCustomer(id: string): User { ... }     // "get" + "customer"
  retrieveAccount(id: string): User { ... } // "retrieve" + "account"
  loadClient(id: string): User { ... }      // "load" + "client"
}

// Confusing: Are these the same or different?
const user = service.fetchUser(id);
const customer = service.getCustomer(id);
const account = service.retrieveAccount(id);
```

**Correct (consistent vocabulary):**

```typescript
// Good: One term per concept
class UserService {
  getUser(id: string): User { ... }
  getUserByEmail(email: string): User { ... }
  getUsersByRole(role: string): User[] { ... }
  getCurrentUser(): User { ... }
}

// Clear: All operations work with "users" and "get"
const user = service.getUser(id);
const admin = service.getUserByEmail(adminEmail);
const editors = service.getUsersByRole('editor');
```

**Incorrect (Python - mixed terminology):**

```python
# Bad: delete vs remove vs destroy vs clear
class CartManager:
    def delete_item(self, item_id): ...
    def remove_product(self, product_id): ...
    def destroy_entry(self, entry_id): ...
    def clear_line(self, line_id): ...

# Bad: create vs add vs insert vs new
def create_order(data): ...
def add_order(data): ...
def insert_order(data): ...
def new_order(data): ...
```

**Correct (Python - unified vocabulary):**

```python
# Good: consistent verbs throughout
class CartManager:
    def remove_item(self, item_id): ...
    def remove_all_items(self): ...

# Good: one verb for creation
def create_order(order_data): ...
def create_line_item(item_data): ...
def create_payment(payment_data): ...
```

**Incorrect (Go - entity name confusion):**

```go
// Bad: Is it a Job, Task, Work, or Process?
type JobQueue struct {
    tasks    []Task
    workPool chan Work
    procs    map[string]*Process
}

func (q *JobQueue) AddTask(t Task) error { ... }
func (q *JobQueue) SubmitWork(w Work) error { ... }
func (q *JobQueue) EnqueueJob(j Job) error { ... }
```

**Correct (Go - unified terminology):**

```go
// Good: Everything is a Job
type JobQueue struct {
    pendingJobs  []Job
    workerPool   chan Job
    runningJobs  map[string]*Job
}

func (q *JobQueue) SubmitJob(job Job) error { ... }
func (q *JobQueue) CancelJob(jobID string) error { ... }
func (q *JobQueue) GetJobStatus(jobID string) JobStatus { ... }
```

### Create a Project Vocabulary

Document chosen terms:

| Concept | Use This | NOT These |
|---------|----------|-----------|
| Retrieve data | `get` | fetch, retrieve, load, find, obtain |
| Create entity | `create` | add, insert, new, make, build |
| Remove entity | `delete` | remove, destroy, clear, erase |
| Modify entity | `update` | modify, change, edit, patch |
| Person using app | `user` | customer, client, account, member |

### When NOT to Apply

- Integrating with external APIs that use different terminology
- Domain experts use specific terms (use their ubiquitous language)
- Language idioms dictate different verbs (e.g., `append` in Go)

### References

- Domain-Driven Design: "Ubiquitous Language" pattern
- Clean Code: "Use Solution Domain Names" and "Use Problem Domain Names"
