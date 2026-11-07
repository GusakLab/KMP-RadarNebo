# GraphQL Schema Patterns

Schema-shape patterns that complement `graphql-design.md` (type system, DataLoader, Relay pagination, partial errors). Read this when designing interfaces/unions, mutation payloads, batch mutations, filtering arguments, subscriptions, scalars, directives or schema evolution.

## Schema Organization

### Modular Schema Structure

```graphql
# user.graphql
type User {
  id: ID!
  email: String!
  name: String!
  posts: [Post!]!
}

extend type Query {
  user(id: ID!): User
  users(first: Int, after: String): UserConnection!
}

extend type Mutation {
  createUser(input: CreateUserInput!): CreateUserPayload!
}

# post.graphql
type Post {
  id: ID!
  title: String!
  content: String!
  author: User!
}

extend type Query {
  post(id: ID!): Post
}
```

## Type Design Patterns

### 1. Non-Null Types

```graphql
type User {
  id: ID! # Always required
  email: String! # Required
  phone: String # Optional (nullable)
  posts: [Post!]! # Non-null array of non-null posts
  tags: [String!] # Nullable array of non-null strings
}
```

### 2. Interfaces for Polymorphism

```graphql
interface Node {
  id: ID!
  createdAt: DateTime!
}

type User implements Node {
  id: ID!
  createdAt: DateTime!
  email: String!
}

type Post implements Node {
  id: ID!
  createdAt: DateTime!
  title: String!
}

type Query {
  node(id: ID!): Node
}
```

### 3. Unions for Heterogeneous Results

```graphql
union SearchResult = User | Post | Comment

type Query {
  search(query: String!): [SearchResult!]!
}

# Query example
{
  search(query: "graphql") {
    ... on User {
      name
      email
    }
    ... on Post {
      title
      content
    }
    ... on Comment {
      text
      author {
        name
      }
    }
  }
}
```

### 4. Input Types

```graphql
input CreateUserInput {
  email: String!
  name: String!
  password: String!
  profileInput: ProfileInput
}

input ProfileInput {
  bio: String
  avatar: String
  website: String
}

input UpdateUserInput {
  id: ID!
  email: String
  name: String
  profileInput: ProfileInput
}
```


## Mutation Design Patterns

### 1. Input/Payload Pattern

```graphql
input CreatePostInput {
  title: String!
  content: String!
  tags: [String!]
}

type CreatePostPayload {
  post: Post
  errors: [Error!]
  success: Boolean!
}

type Error {
  field: String
  message: String!
  code: String!
}

type Mutation {
  createPost(input: CreatePostInput!): CreatePostPayload!
}
```

### 2. Optimistic Response Support

```graphql
type UpdateUserPayload {
  user: User
  clientMutationId: String
  errors: [Error!]
}

input UpdateUserInput {
  id: ID!
  name: String
  clientMutationId: String
}

type Mutation {
  updateUser(input: UpdateUserInput!): UpdateUserPayload!
}
```

### 3. Batch Mutations

```graphql
input BatchCreateUserInput {
  users: [CreateUserInput!]!
}

type BatchCreateUserPayload {
  results: [CreateUserResult!]!
  successCount: Int!
  errorCount: Int!
}

type CreateUserResult {
  user: User
  errors: [Error!]
  index: Int!
}

type Mutation {
  batchCreateUsers(input: BatchCreateUserInput!): BatchCreateUserPayload!
}
```

## Field Design

### Arguments and Filtering

```graphql
type Query {
  posts(
    # Pagination
    first: Int = 20
    after: String

    # Filtering
    status: PostStatus
    authorId: ID
    tag: String

    # Sorting
    orderBy: PostOrderBy = CREATED_AT
    orderDirection: OrderDirection = DESC

    # Searching
    search: String
  ): PostConnection!
}

enum PostStatus {
  DRAFT
  PUBLISHED
  ARCHIVED
}

enum PostOrderBy {
  CREATED_AT
  UPDATED_AT
  TITLE
}

enum OrderDirection {
  ASC
  DESC
}
```

### Computed Fields

```graphql
type User {
  firstName: String!
  lastName: String!
  fullName: String! # Computed in resolver
  posts: [Post!]!
  postCount: Int! # Computed, doesn't load all posts
}

type Post {
  likeCount: Int!
  commentCount: Int!
  isLikedByViewer: Boolean! # Context-dependent
}
```

## Subscriptions

```graphql
type Subscription {
  postAdded: Post!

  postUpdated(postId: ID!): Post!

  userStatusChanged(userId: ID!): UserStatus!
}

type UserStatus {
  userId: ID!
  online: Boolean!
  lastSeen: DateTime!
}

# Client usage
subscription {
  postAdded {
    id
    title
    author {
      name
    }
  }
}
```

## Custom Scalars

```graphql
scalar DateTime
scalar Email
scalar URL
scalar JSON
scalar Money

type User {
  email: Email!
  website: URL
  createdAt: DateTime!
  metadata: JSON
}

type Product {
  price: Money!
}
```

## Directives

### Built-in Directives

```graphql
type User {
  name: String!
  email: String! @deprecated(reason: "Use emails field instead")
  emails: [String!]!

  # Conditional inclusion
  privateData: PrivateData @include(if: $isOwner)
}

# Query
query GetUser($isOwner: Boolean!) {
  user(id: "123") {
    name
    privateData @include(if: $isOwner) {
      ssn
    }
  }
}
```

### Custom Directives

```graphql
directive @auth(requires: Role = USER) on FIELD_DEFINITION

enum Role {
  USER
  ADMIN
  MODERATOR
}

type Mutation {
  deleteUser(id: ID!): Boolean! @auth(requires: ADMIN)
  updateProfile(input: ProfileInput!): User! @auth
}
```

## Error Handling

### Union Error Pattern

```graphql
type User {
  id: ID!
  email: String!
}

type ValidationError {
  field: String!
  message: String!
}

type NotFoundError {
  message: String!
  resourceType: String!
  resourceId: ID!
}

type AuthorizationError {
  message: String!
}

union UserResult = User | ValidationError | NotFoundError | AuthorizationError

type Query {
  user(id: ID!): UserResult!
}

# Usage
{
  user(id: "123") {
    ... on User {
      id
      email
    }
    ... on NotFoundError {
      message
      resourceType
    }
    ... on AuthorizationError {
      message
    }
  }
}
```

### Errors in Payload

```graphql
type CreateUserPayload {
  user: User
  errors: [Error!]
  success: Boolean!
}

type Error {
  field: String
  message: String!
  code: ErrorCode!
}

enum ErrorCode {
  VALIDATION_ERROR
  UNAUTHORIZED
  NOT_FOUND
  INTERNAL_ERROR
}
```


## Protecting the Server

GraphQL lets clients write arbitrarily deep and wide queries, so limit cost before execution:

| Control | What it stops | How |
|---------|---------------|-----|
| Depth limit | `user { friends { friends { friends ... } } }` | Validation rule rejecting depth > N (often 7-10); most servers ship a plugin or rule |
| Complexity/cost limit | Wide list fan-out (`first: 1000` on nested lists) | Assign a cost per field, multiply by list size arguments, reject above a budget |
| Max page size | Unbounded lists | Cap `first`/`last` in the resolver or schema directive |
| Persisted / trusted queries | Arbitrary queries from public clients | Accept only registered query hashes in production |
| Introspection policy | Schema discovery by attackers | Disable or restrict in production for private APIs |
| Batching limit | Many operations in one HTTP request | Cap operations per request |

Check the installed server library's docs for the exact rule or plugin name; do not hand-roll an AST walker unless the library has none.

## Schema Evolution

GraphQL APIs are usually versionless: evolve additively and deprecate.

```graphql
type User {
  name: String! @deprecated(reason: "Use firstName and lastName")
  firstName: String!
  lastName: String!
  email: String          # new fields start nullable/optional
}
```

1. Add new fields/types; never change an existing field's type or nullability in a breaking direction (nullable -> non-null on an *input* breaks clients; non-null -> nullable on an *output* breaks clients).
2. Mark replaced fields `@deprecated(reason: ...)`; track usage via field-level metrics.
3. Remove only after usage reaches zero (or an announced sunset date).
4. Run a schema diff (breaking-change check) in CI on every schema change.

## Best Practices Summary

1. **Nullability**: Start output fields nullable unless guaranteed; make inputs non-null only when required.
2. **Input types**: One `input` per mutation; return a dedicated payload type.
3. **Errors**: Expected business errors in payloads or result unions; reserve top-level `errors` for unexpected failures.
4. **Pagination**: Relay connections for lists that can grow (see `graphql-design.md`).
5. **Naming**: camelCase fields, PascalCase types, SCREAMING_SNAKE enum values.
6. **Deprecation** over removal; diff the schema in CI.
7. **DataLoader** for every relationship resolver.
8. **Cost limits** before exposing the endpoint publicly.
9. **Custom scalars** for domain types (DateTime, Email, URL, Money).
10. **Descriptions** on every type and field (they become the docs).
