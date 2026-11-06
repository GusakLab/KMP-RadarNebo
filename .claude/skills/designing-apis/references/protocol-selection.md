# API Protocol Selection Guide

## Table of Contents

- [Quick Decision Tree](#quick-decision-tree)
- [Protocol Comparison](#protocol-comparison)
- [REST: Request-Response Pattern](#rest-request-response-pattern)
  - [When to Use](#when-to-use)
  - [Advantages](#advantages)
  - [Disadvantages](#disadvantages)
  - [Example Use Cases](#example-use-cases)
- [GraphQL: Client-Specified Queries](#graphql-client-specified-queries)
  - [When to Use](#when-to-use)
  - [Advantages](#advantages)
  - [Disadvantages](#disadvantages)
  - [Example Use Cases](#example-use-cases)
- [WebSockets: Real-Time Bidirectional](#websockets-real-time-bidirectional)
  - [When to Use](#when-to-use)
  - [Advantages](#advantages)
  - [Disadvantages](#disadvantages)
  - [Example Use Cases](#example-use-cases)
- [Message Queues: Event-Driven](#message-queues-event-driven)
  - [When to Use](#when-to-use)
  - [Advantages](#advantages)
  - [Disadvantages](#disadvantages)
  - [Example Use Cases](#example-use-cases)
- [Hybrid Approaches](#hybrid-approaches)
  - [REST + WebSockets](#rest-websockets)
  - [REST + Message Queue](#rest-message-queue)
  - [GraphQL + Subscriptions](#graphql-subscriptions)

## Quick Decision Tree

```
START: What type of API do you need?
  │
  ├─► Public API for third-party developers?
  │     YES ──► REST (widest compatibility, best tooling)
  │
  ├─► Complex data with many relationships?
  │     YES ──► GraphQL (client-controlled queries)
  │
  ├─► Real-time bidirectional communication?
  │     YES ──► WebSockets (low latency, push updates)
  │
  ├─► Event-driven, decoupled microservices?
  │     YES ──► Message Queue (Kafka, RabbitMQ)
  │
  └─► Simple CRUD, caching important?
        YES ──► REST (HTTP caching, stateless)
```

## Protocol Comparison

| Factor | REST | GraphQL | WebSocket | Message Queue |
|--------|------|---------|-----------|---------------|
| **Simplicity** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐ |
| **Public API** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐ | ⭐ |
| **Complex Data** | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐ | ⭐⭐ |
| **Caching** | ⭐⭐⭐⭐⭐ | ⭐⭐ | ⭐ | ⭐ |
| **Real-time** | ⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| **Bandwidth** | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐ |
| **Tooling** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐ |

## REST: Request-Response Pattern

### When to Use
- Public APIs, diverse clients
- CRUD on resources
- Caching critical (HTTP caching)
- Simple, predictable patterns
- Wide client compatibility
- Team knows HTTP/REST

### Advantages
- ✅ Simple, well-understood HTTP semantics
- ✅ Built-in caching (HTTP headers)
- ✅ Stateless, scales horizontally
- ✅ Mature tooling
- ✅ Works everywhere (browsers, mobile, IoT)

### Disadvantages
- ❌ Over-fetching (more data than needed)
- ❌ Under-fetching (multiple requests needed)
- ❌ Version management complexity
- ❌ No built-in real-time updates

### Example Use Cases
- SaaS public APIs (Stripe, Twilio)
- Mobile app backends
- Microservices (internal APIs)
- CRUD applications

## GraphQL: Client-Specified Queries

### When to Use
- Complex, interconnected data models
- Mobile apps (minimize over-fetching)
- Clients need flexible queries
- Rapid frontend iteration
- Multiple client types, different needs
- Real-time subscriptions

### Advantages
- ✅ Client specifies exact data
- ✅ Single endpoint for all queries
- ✅ Strongly typed schema
- ✅ Real-time subscriptions
- ✅ Introspection for tooling

### Disadvantages
- ❌ More complex than REST
- ❌ Harder caching
- ❌ N+1 query problem (requires DataLoader)
- ❌ Harder monitoring and rate limiting
- ❌ Learning curve

### Example Use Cases
- GitHub API
- Facebook/Instagram apps
- Content management systems
- Internal tools with complex data

## WebSockets: Real-Time Bidirectional

### When to Use
- Real-time updates (chat, notifications)
- Low latency
- Bidirectional (server can push)
- Long-lived connections acceptable

### Advantages
- ✅ Persistent connection
- ✅ Bidirectional (client and server both send)
- ✅ Low overhead (no HTTP header per message)
- ✅ Low latency

### Disadvantages
- ❌ Stateful (harder to scale)
- ❌ Connection management complexity
- ❌ No HTTP caching
- ❌ Firewall/proxy issues

### Example Use Cases
- Chat applications (Slack)
- Real-time dashboards
- Collaborative editing (Google Docs)
- Gaming
- Live sports scores

## Message Queues: Event-Driven

### When to Use
- Decoupled microservice communication
- Event sourcing and CQRS
- High-throughput message processing
- Guaranteed delivery

### Advantages
- ✅ Publish-subscribe
- ✅ Durable message storage
- ✅ Ordered processing
- ✅ Decoupled services
- ✅ High throughput

### Disadvantages
- ❌ Extra infrastructure (Kafka, RabbitMQ)
- ❌ Eventual consistency
- ❌ Complex error handling
- ❌ Message ordering challenges

### Example Use Cases
- Microservices event bus
- Order processing systems
- Log aggregation
- IoT data streams

## Hybrid Approaches

### REST + WebSockets
- REST for CRUD
- WebSockets for real-time updates
- Example: chat app (REST for message history, WebSockets for live chat)

### REST + Message Queue
- REST for synchronous requests
- Queue for async background jobs
- Example: e-commerce (REST for orders, queue for email notifications)

### GraphQL + Subscriptions
- GraphQL queries/mutations for data
- GraphQL subscriptions (over WebSockets) for real-time
- Example: social media feed

## RPC-Style Options: tRPC and gRPC

Tree above covers resource, query, push and event styles. Two RPC styles common for internal or same-language consumers:

```
Who consumes the API?
├── Third parties / many platforms ........ REST + OpenAPI
├── Several frontends, nested data ........ GraphQL
├── TypeScript client + server in one repo  tRPC (types shared, no spec to maintain)
├── Internal services, latency-sensitive,
│   polyglot, streaming ................... gRPC (Protobuf contract, HTTP/2)
├── Browser push / bidirectional .......... WebSocket or SSE (+ AsyncAPI)
└── Fire-and-forget, decoupled services ... Message broker (+ AsyncAPI)
```

| Factor | REST | GraphQL | tRPC | gRPC |
|--------|------|---------|------|------|
| Best for | Public APIs | Complex client data needs | TS monorepos | Internal service-to-service |
| Contract | OpenAPI (optional) | SDL | TypeScript types | `.proto` (required) |
| Type safety | Via codegen | Via codegen | Automatic (TS only) | Via codegen, many languages |
| HTTP caching | Native | Hard | Limited | None |
| Browser support | Native | Native | Native | Needs gRPC-Web/Connect |
| Non-TS clients | Yes | Yes | No (effectively) | Yes |

Selection questions: Who consumes it, and are they external? Is every client TypeScript? How nested is the data? Does HTTP caching matter? Need streaming?

Implementation (frameworks, server code) lives in `implementing-api-patterns` skill.
