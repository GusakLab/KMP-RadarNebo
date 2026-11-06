# Pre-deployment security checklist and Supabase RLS

Read this before a production release, or when the project uses Supabase (or plain Postgres row-level security) for authorization.

## Pre-deployment security checklist

Run before any production deployment. Every unchecked item needs an owner and a reason.

- [ ] **Secrets**: none in source, history, client bundles or logs; loaded from env or a secret manager
- [ ] **Input validation**: every external input parsed against a schema at the boundary
- [ ] **Injection**: all queries parameterized; no string-built SQL, shell or template input
- [ ] **XSS**: user content encoded or sanitized; CSP configured
- [ ] **CSRF**: protection enabled for cookie-authenticated, state-changing routes
- [ ] **Authentication**: session tokens in `HttpOnly; Secure; SameSite` cookies, not `localStorage`
- [ ] **Authorization**: object- and role-level checks before every sensitive operation
- [ ] **Rate limiting**: on auth and expensive endpoints, backed by a shared store when scaled out
- [ ] **HTTPS**: enforced, with HSTS
- [ ] **Security headers**: CSP, `X-Content-Type-Options`, frame-ancestors / `X-Frame-Options`
- [ ] **Errors**: no stack traces or internal identifiers in responses
- [ ] **Logging**: no secrets, tokens or unnecessary PII in logs
- [ ] **Dependencies**: native audit clean of reachable critical/high findings (see the `dependency-scanning` skill for CI scanning)
- [ ] **Row-level security**: enabled and tested on every exposed table (below)
- [ ] **CORS**: explicit origin allowlist, no `*` with credentials
- [ ] **File uploads**: size, type and content validated; stored outside the web root
- [ ] **Signatures**: webhook and wallet/message signatures verified server-side, when present

## Supabase row-level security

With Supabase, any table in an exposed schema is reachable from the browser through the anon key. RLS is the authorization layer, not an optional extra.

```sql
-- Enable RLS on every table in an exposed schema
ALTER TABLE profiles ENABLE ROW LEVEL SECURITY;

-- Read own row
CREATE POLICY "profiles_select_own" ON profiles
  FOR SELECT TO authenticated
  USING ((select auth.uid()) = id);

-- Update own row and prevent re-assigning ownership
CREATE POLICY "profiles_update_own" ON profiles
  FOR UPDATE TO authenticated
  USING ((select auth.uid()) = id)
  WITH CHECK ((select auth.uid()) = id);

-- Insert only rows owned by the caller
CREATE POLICY "profiles_insert_own" ON profiles
  FOR INSERT TO authenticated
  WITH CHECK ((select auth.uid()) = id);
```

Rules:

- A table with RLS enabled and no policy denies everything; a table without RLS allows everything the role's grants allow. List tables lacking RLS before release.
- `UPDATE` and `INSERT` need `WITH CHECK`, or a user can write rows they cannot read back.
- Wrapping `auth.uid()` in `(select ...)` lets Postgres evaluate it once per statement instead of per row.
- Never ship the `service_role` key to a client; it bypasses RLS. Use it only in trusted server code.
- Do not authorize on `user_metadata` claims in the JWT; users can edit them. Use `app_metadata` or a server-managed table.
- Views and `SECURITY DEFINER` functions can bypass RLS; review them explicitly.

Verify by querying as two different test users and as `anon`: each must see and modify only its own rows.

### Parameterized queries with the Supabase client

```typescript
// Safe: the query builder parameterizes values
const { data } = await supabase.from('users').select('*').eq('email', userEmail)

// Raw SQL: always bind parameters
await db.query('SELECT * FROM users WHERE email = $1', [userEmail])
```

String-built filters (for example interpolating user input into `.or('...')` filter strings) reintroduce injection into the filter grammar; validate or allowlist such values.
