---
name: security-and-hardening
description: Secure application code at trust boundaries - input validation, authn/authz, sessions, secrets, SSRF, uploads, personal data and LLM features. Use while building or reviewing features that touch untrusted data or user sessions, or before a production release; use dependency-scanning for CVE/SBOM scanning in CI and security-hardening for infrastructure.
---

# Security and Hardening

## When to Use

- Accepting user input
- Authentication or authorization
- Storing or transmitting sensitive data
- External API or service integrations
- File uploads, webhooks, callbacks
- Payment or PII data

## Process: Threat Model First

Before hardening, spend five minutes thinking like an attacker:

1. **Map trust boundaries.** Untrusted data enters via HTTP requests, form fields, file uploads, webhooks, third-party APIs, message queues, **LLM output**. Also OS-supplied local values that look internal: another process's command line or environment, shared-volume filenames, job-payload paths. Trust follows who *wrote* a value, not which channel delivered it. Every boundary is attack surface.
2. **Name assets** worth stealing or breaking: credentials, PII, payment data, admin actions, money movement.
3. **Run STRIDE over each boundary.** Quick lens, not ceremony:

| Threat | Ask | Typical mitigation |
|---|---|---|
| **S**poofing | Impersonate a user/service? | Authentication, signature verification |
| **T**ampering | Alter data in transit or at rest? | Integrity checks, parameterized queries, HTTPS |
| **R**epudiation | Deny an action later? | Audit-log security events |
| **I**nformation disclosure | Leak data? | Encryption, field allowlists, generic errors |
| **D**enial of service | Overwhelm it? | Rate limiting, input size caps, timeouts |
| **E**levation of privilege | Gain rights a user should not have? | Authorization checks, least privilege |

4. **Write abuse cases next to use cases.** Per feature, ask "how would I misuse this?" Make that the first test.

If you cannot name a feature's trust boundaries, you cannot secure it yet. OWASP **A04: Insecure Design**: most breaches begin in design, not code.

## The Three-Tier Boundary System

### Always Do (No Exceptions)

- **Validate all external input** at system boundary (API routes, form handlers)
- **Parameterize all database queries**; never concatenate user input into SQL
- **Encode output** against XSS (framework auto-escaping; do not bypass it)
- **Use HTTPS** for all external communication
- **Hash passwords** with bcrypt/scrypt/argon2 (never plaintext)
- **Set security headers** (CSP, HSTS, X-Frame-Options, X-Content-Type-Options)
- **Use httpOnly, secure, sameSite cookies** for sessions
  - *Local-dev exception:* plain-HTTP dev/test may turn `Secure` off via an env flag that defaults to on; never ship that flag enabled. Start HSTS with short `max-age`; add `includeSubDomains`/`preload` only deliberately (same policy as `security-best-practices`)
- **Run detected package manager's native audit** on committed lockfile before every release

### Ask First (Requires Human Approval)

- New authentication flows or auth logic changes
- New categories of stored sensitive data (PII, payment info)
- New external service integrations
- CORS configuration changes
- File upload handlers
- Rate limiting or throttling changes
- Elevated permissions or roles

### Never Do

- **Never commit secrets** to version control (API keys, passwords, tokens)
- **Never log sensitive data** (passwords, tokens, full credit card numbers)
- **Never trust client-side validation** as security boundary
- **Never disable security headers** for convenience
- **Never use `eval()` or `innerHTML`** with user-provided data
- **Never store sessions in client-accessible storage** (localStorage for auth tokens)
- **Never expose stack traces** or internal error details to users

## Hardening Controls

Rules below are the workflow; [references/hardening-patterns.md](references/hardening-patterns.md) shows one implementation per rule. Open a section only on reaching that code.

### Injection, XSS, and access control

- Parameterize every query. Never build SQL, NoSQL or shell commands from input strings.
- Encode output via framework auto-escaping. If raw HTML is unavoidable, sanitize first with an allowlist sanitizer.
- Check **authorization** on every request, not only authentication: user must own, or be permitted on, the specific resource (A01, IDOR).

Patterns: [Injection](references/hardening-patterns.md#injection), [XSS](references/hardening-patterns.md#cross-site-scripting-xss), [Access control](references/hardening-patterns.md#broken-access-control).

### Authentication and sessions

- Hash passwords with bcrypt (≥12 rounds), scrypt or argon2. Session secret from environment, never code.
- Session cookies: `httpOnly`, `secure`, `sameSite: 'lax'` or `'strict'` (CSRF defense; `'none'` sends cookie on cross-site requests), bounded `maxAge`.

Pattern: [Authentication](references/hardening-patterns.md#broken-authentication).

### Headers, CORS, and responses

- Security headers on every response (helmet or framework equivalent). CSP starts at `default-src 'self'`; tighten, never loosen.
- CORS: explicit origin list from configuration. Never `*` with credentials.
- Strip sensitive fields (`passwordHash`, reset tokens) from every response. Generic error bodies; internals to server logs only.

Patterns: [Misconfiguration](references/hardening-patterns.md#security-misconfiguration), [Sensitive data exposure](references/hardening-patterns.md#sensitive-data-exposure).

### Input validation and uploads

- Validate at boundary with a schema: allowlisted shape, lengths, enums, formats. Reject with 422 plus structured details. Downstream code uses only the parsed, typed value.
- Uploads: allowlist MIME types, cap size, verify content (magic bytes) when it matters. Extension proves nothing.

Patterns: [Schema validation](references/hardening-patterns.md#schema-validation-at-boundaries), [File upload](references/hardening-patterns.md#file-upload-safety).

### Server-side fetches (SSRF)

Any user-influenced URL (webhooks, import-from-URL, image proxies, link previews) can target internal services. Allowlist scheme and host. Resolve **all** DNS records; reject any private or reserved address (loopback, link-local `169.254.169.254`, private, unique-local; IPv4 and IPv6). Forbid redirects. DNS-rebinding TOCTOU gap remains: for high-risk surfaces, pin resolved IP or front with a filtering agent.

Pattern: [SSRF](references/hardening-patterns.md#server-side-request-forgery-ssrf).

### Destructive operations on derived paths

Delete, move or overwrite is only as safe as the value naming its target. Trust follows who *wrote* that value, not which channel delivered it: another process's command line is as attacker-controlled as a form field. Shape check proves well-formedness, not authorization. Before the call, require all three: resolved target (symlinks resolved) under an **allowlisted root**; at least one level **below** that root; **ownership evidence read before the operation**. On refusal, log rejected target and stop; never fall back to a broader default path.

Why the check is weaker than it reads (marker self-attestation, check/use races): [Destructive paths](references/hardening-patterns.md#destructive-operations-on-derived-paths). Worked code: security checks above and the repository's documented requirements.

### Rate limiting

Limit API generally, auth endpoints strictly (about 10 attempts per 15 minutes). Once more than one process serves traffic, in-memory counters silently become `max × instances`, or never fire on serverless: back the limiter with a shared store.

Pattern: [Rate limiting](references/hardening-patterns.md#rate-limiting).

### Secrets

Secrets come from environment. Commit `.env.example` with placeholders; gitignore real `.env*` files and key material; check staged diff before committing, printing file names only, never matched values. **Secret reaching a remote is compromised on arrival: rotate first, then purge history.** Purging rewrites history and force-pushes: confirm with user first.

Pattern: [Secrets management](references/hardening-patterns.md#secrets-management).

### Dependencies and supply chain

1. **Find installation boundary and manager.** Use workspace root owning the lockfile, or an independent nested project only when outside that workspace. Corroborate `packageManager` (if present), lockfile and CI; stop on disagreement or competing lockfiles. Pin manager version.
2. **Block dependency scripts before first execution.** Bootstrap with scripts disabled or a documented fail-closed policy. Inspect pending script source, approve only the minimum, commit the policy, verify with a clean frozen/immutable install. Never blanket-approve.
3. **Run native audit on committed lockfile before every release.** Triage critical/high by **reachability** (runtime, build, test, deploy paths) and fix availability. Never auto-apply forced remediation (`npm audit fix --force` or equivalent): forced fixes may cross declared dependency ranges. Preview, read changelogs, test each upgrade. Document each deferral with reason and review date.
4. **Audits only match known advisories.** They do not catch newly malicious or typosquatted packages (`cross-env` vs `crossenv`). Review new dependencies, lockfile diffs and script-policy changes together: ownership, maintenance, release age, provenance, transitive graph. Verify registry signatures where supported (`npm audit signatures`, `pnpm audit signatures`); missing signatures signal investigation, not automatic proof of compromise (A06, LLM03).

Triage decision tree: [Dependency audit triage](references/hardening-patterns.md#dependency-audit-triage). CI scanner setup (Snyk, Dependabot, OSV, SBOM generation): `dependency-scanning` skill.

### Personal data and privacy

Hardening asks "can an attacker read it?" Privacy asks "should *we* hold it at all, and for how long?" Treat personal data as a liability to minimize.

- **Classify fields as you add them** (non-personal, PII, sensitive); handle each class accordingly. Data you cannot find, you cannot protect or delete on request.
- **Collect only against a stated purpose.** "Might be useful later" is latent breach scope, not a purpose. Keep PII out of telemetry (ops view: `observability-and-instrumentation` skill).
- **Set retention up front, then delete.** Each personal-data store needs a TTL and working deletion path, covering backups, caches, search indexes, analytics copies.
- **Support data-subject rights your jurisdiction requires** (GDPR, CCPA and kin): export, correct, delete. Design schema so a user's data is findable and erasable, not smeared irreversibly across systems.
- **Consent gates collection and third-party sharing, and is auditable.** PII sent to an analytics, ad or LLM vendor is sharing; vendor needs a data-processing agreement. Make region a configurable policy, not a hardcoded assumption.

Classification table: [Data classification](references/hardening-patterns.md#data-classification). Privacy incident starts the breach-notification clock; run postmortem with `debugging-and-error-recovery` skill.

### AI / LLM features

Calling an LLM (chatbots, summarizers, agents, RAG) adds attack surface. Map it to the [OWASP Top 10 for LLM Applications (2025)](https://genai.owasp.org/llm-top-10/):

- **Model output is untrusted input** (LLM05). Never into `eval`, SQL, a shell, `innerHTML` or a file path. Parse defensively, validate against a schema, then encode.
- **Prompts can be hijacked** (LLM01). Untrusted context text (user message, fetched page, PDF) can carry instructions. System prompt is not a security boundary; enforce permissions in code.
- **Keep secrets, other tenants' data and the full system prompt out of the context window** (LLM02, LLM07). Scope tool permissions, validate every tool argument, confirm destructive actions (LLM06). Cap tokens, request rate, recursion depth (LLM10). Partition RAG embeddings per tenant; validate documents before indexing (LLM08).

Pattern: [LLM output handling](references/hardening-patterns.md#llm-output-handling).

## Review Checklist

Before sign-off, walk security checks above and the repository's documented requirements: authentication, authorization, input, data protection and privacy, headers and CORS, dependencies and supply chain, AI/LLM, error handling.

Before a production release, or when authorization runs through Supabase/Postgres row-level security, run [the pre-deployment checklist and RLS rules](references/predeploy-and-supabase.md).

## Common Rationalizations

| Rationalization | Reality |
|---|---|
| "This is an internal tool, security doesn't matter" | Internal tools get compromised. Attackers target the weakest link. |
| "We'll add security later" | Retrofitting security is 10x harder than building it in. Add it now. |
| "No one would try to exploit this" | Automated scanners will find it. Obscurity is not security. |
| "The framework handles security" | Frameworks give tools, not guarantees. You must still use them correctly. |
| "It's just a prototype" | Prototypes become production. Security habits from day one. |
| "Compliance is legal's problem, not ours" | Export, deletion, retention and consent are schema and code. Legal can't bolt them on after PII is smeared across ten systems. |

## Red Flags

- User input passed directly into DB queries, shell commands or HTML rendering
- Delete, move or overwrite targeting a path from a payload, config value or another process's command line, guarded only by a path shape check
- Permissive CORS: wildcard (`*`) or reflected origins, or credentials allowed for untrusted origins (no CORS config at all is the safe default)
- Secrets, PII or full system prompt inside an LLM context window
- PII sent to analytics/ad/LLM vendors without consent or data-processing agreement
- "Delete my account" only flipping a flag while personal data lingers in stores and backups

## Verification

After implementing security-relevant code:

- [ ] Native audit: no unmitigated reachable critical/high findings; CI keeps authoritative lockfile and blocks unreviewed dependency scripts
- [ ] No secrets in source or git history
- [ ] All user input validated at system boundaries
- [ ] Destructive filesystem operations resolve symlinks, then verify allowlisted root, minimum depth, ownership before running
- [ ] Authentication and authorization checked on every protected endpoint
- [ ] Security headers in responses (check with browser DevTools)
- [ ] Error responses expose no internal details
- [ ] Rate limiting on auth endpoints, backed by a shared store when more than one instance serves traffic
- [ ] Server-side URL fetches validated against an allowlist (no SSRF)
- [ ] LLM/model output validated and encoded before use (if AI features present)
- [ ] Personal data classified, minimized to a stated purpose, retention-limited
- [ ] Deletion and export work end-to-end (including backups, caches, analytics copies)
