# AI Failure Modes — the unique value of this skill

15 systematic ways LLMs write bad code. AI agents: read first; these patterns most likely enter your output.

## Contents

- 1. Catch-all error handling that swallows failures
- 2. Defensive guards for impossible cases
- 3. Premature abstraction
- 4. Comment pollution
- 5. Code duplication instead of reuse
- 6. Hallucinated APIs and packages
- 7. Generic, intent-less naming
- 8. Long functions doing many things
- 9. Parameter explosion
- 10. Inconsistency with surrounding code
- 11. Dead code, unused imports, half-implementations
- 12. Declares success with mock fallbacks in production code
- 13. Plausible-but-wrong code
- 14. YAGNI violations through speculative configurability
- 15. New dependency for trivial work
- Cross-cutting observation
- Where this skill differs from generic clean-code rules

---

## 1. Catch-all error handling that swallows failures

**Pattern.** Broad catch-all, or null/empty success on any caught error. Hides real bugs.

**Source.** Karpathy: LLMs are unusually afraid of exceptions. Field reports on LLM error suppression agree. Root cause: training reward penalizes propagated exceptions, so model learns suppression.

**Bad:**
```text
getEmail(userId):
  attempt:
    user = userStore.get(userId)
    return user.email
  catch anyError:
    return null
```
Looks safe. Database outage now indistinguishable from "user has no email."

**Good:**
```text
getEmail(userId):
  user = userStore.get(userId)  // storage errors propagate
  return user.email             // null only means the user has no email
```

**Rule.** Catch only specific error types you can recover from. No broad catch-all without documented recovery path. Never return null/empty success from handler unless contract says so.

---

## 2. Defensive guards for impossible cases

**Pattern.** Null, runtime type, or truthiness checks for conditions type system or call graph already prevents.

**Source.** arXiv 2409.19182, "AI-Generated Code Considered Harmful"; HN discussion of defensive code overuse. Same reward shaping as #1.

**Bad:**
```text
total(orderItems):
  if orderItems is null: return 0
  if orderItems is not a collection: return 0
  return sum(order.amount for each non-null order in orderItems)
```
Contract says `orderItems` is collection of orders. No guard can fire on normal call paths.

**Good:**
```text
total(orderItems):
  return sum(order.amount for each order in orderItems)
```

**Rule.** Do not add null, runtime type, or truthiness checks for values type annotation or caller contract already excludes. Trust contract *inside* trust boundary. At boundary itself (external input, payloads, deserialized or cross-process data), validation is required, not defensive bloat.

---

## 3. Premature abstraction

**Pattern.** Factories, strategy classes, base classes, plugin hooks, dependency-injection scaffolding before second concrete user exists.

**Source.** Martin Fowler, "Patterns for Reducing Friction in AI-Assisted Development" names "overeagerness (adding unrequested features)" as documented AI pattern. Also Fowler, "I still care about the code"; Fowler, "Conversation: LLMs and Building Abstractions".

**Bad:**
```text
PaymentProcessor interface
  charge(amount)

CardPaymentProcessor implements PaymentProcessor
  charge(amount):
    return paymentProvider.createCharge(amount).id

PaymentProcessorFactory
  create():
    return new CardPaymentProcessor()
```
One payment processor. Interface, factory, indirection: pure ceremony.

**Good:**
```text
charge(amount):
  return paymentProvider.createCharge(amount).id
```

**Rule.** No interface, abstract class, factory, registry, strategy, or plugin pattern unless two or more concrete implementations exist today or spec explicitly requires extensibility. One implementation: inline it.

---

## 4. Comment pollution

**Pattern.** Line-by-line restating comments, leftover step-number scaffolding, documentation comments paraphrasing signature.

**Source.** HN thread #43929768: *"The most common thing that makes agentic code ugly is the overuse of comments."* arXiv 2402.13013, "Code Needs Comments", and arXiv on multi-intent comment generation: LLM comments answer "what?" not "why?"; ~5 descriptive words versus 19-word mixed-intent author comments.

**Bad:**
```text
// Increment counter by one
counter += 1

// Step 3: return the result
return result
```

**Good:**
```text
counter += 1

// Reset counter at midnight UTC to align with billing periods.
if counter > daily_limit:
    counter = 0
```

**Rule.** Comments explain *why*, never *what*. Strip restating comments and leftover "Step N" scaffolding before finalizing. Comment only non-obvious rationale.

---

## 5. Code duplication instead of reuse

**Pattern.** Copying logic an existing helper provides instead of importing it.

**Source.** GitClear, AI Copilot Code Quality 2025, 211M-LoC longitudinal analysis: copy-pasted 5+ line blocks up **8x** from 2021 to 2024; copy/pasted lines from 8.3% to 12.3%; refactoring share from 25% to under 10%.

**Rule.** Search codebase for similar function before writing one. If block of ≥5 lines matches existing repo code, extract or call existing function.

---

## 6. Hallucinated APIs and packages

**Pattern.** Imports, methods, or signatures absent from installed library version.

**Source.** Spracklen et al., USENIX Security '25, "Package Hallucinations": 16 models, average hallucination rate 19.6% (commercial ~5%, open source ~21%). Taxonomy: arXiv 2409.20550 "LLM Hallucinations in Practical Code Generation". See also arXiv 2407.09726, "Mitigating Code LLM Hallucinations with API Documentation".

**Rule.** Verify every import and external API call against installed version: read package, check lockfile, or import and inspect. Do not call method based on what "should exist."

---

## 7. Generic, intent-less naming

**Pattern.** `data`, `result`, `item`, `temp`, `value`, `obj`, `info`, `helper`, `manager`, `utils`, `process_*`, `handle_*`, `do_*`.

**Source.** arXiv 2512.01141, "Neural Variable Name Repair": name-repair models target generic identifiers because LLM code over-produces them. arXiv 2510.03178, "When Names Disappear": semantic names anchor generation.

**Bad:**
```text
processData(data):
  result = []
  for item in data:
    temp = item.value * 2
    result.add(temp)
  return result
```

**Good:**
```text
doublePrices(orders):
  return orders.map(order -> order.priceCents * 2)
```

**Rule.** Identifiers must reveal intent. Ban `data`, `result`, `item`, `temp`, `value`, `obj`, `info`, `helper`, `manager`, `handle_*`, `process_*`, `do_*` unless qualified (`raw_csv_bytes`, `parsed_invoice`).

---

## 8. Long functions doing many things

**Pattern.** One function mixing I/O, business logic, formatting, side effects; often because prompt asked for several things at once.

**Source.** arXiv 2512.11922, "Vibe Coding in Practice": AI-assisted "vibe coding" accrues technical debt via architectural inconsistency. Canonical symptom: one function, hundreds of lines, several unrelated concerns, stitched from AI-generated fragments. GitClear 2025: file size 142→267 LoC, cyclomatic complexity 4.2→8.1 in AI-assisted commits. arXiv 2304.10778 compares Copilot/CodeWhisperer/ChatGPT quality.

**Rule.** Function does one thing. If prompt asks for N things, write N functions plus small composer. Refactor ceiling: ~50 lines (target ≤20), ≤4 parameters, cyclomatic complexity ≤10. Refactor before exceeding.

---

## 9. Parameter explosion

**Pattern.** 6+ positional or keyword args that belong in typed config object.

**Source.** arXiv 2304.10778 quality study; Fowler's overeagerness pattern. Trigger: "AI does not pause to extract a config struct."

**Bad:**
```text
sendEmail(to, subject, body, retry=true, backoff="exp",
          html=false, fromAddress=null, encoding="utf-8",
          internal=false, verbose=false)
```

**Good:**
```text
EmailRequest
  to
  subject
  body
  html = false

sendEmail(request: EmailRequest)
```

**Rule.** At 5 parameters, stop and introduce typed request/config object: record, struct, DTO, or equivalent. Do not keep adding positional args.

---

## 10. Inconsistency with surrounding code

**Pattern.** snake_case in camelCase file, new HTTP client when repo has one, new error type despite existing taxonomy, new logging style.

**Source.** Pullflow, "The Context Illusion"; Honeycomb, "How I Code With LLMs These Days"; Stripe Minions architecture writeups (anup.io, Stripe blog). Production fix: make agent read repo-local conventions before writing.

**Rule.** Before writing in file, read it and at least one neighbor. Match casing, import style, error handling pattern, logging style. Reuse project's HTTP, database, logging utilities; do not add new ones.

---

## 11. Dead code, unused imports, half-implementations

**Pattern.** Unreferenced imports, uncalled helpers, unreachable branches, "just in case" exports.

**Source.** arXiv 2411.01414, "A Deep Dive Into LLM Code Generation Mistakes": 7-category taxonomy of non-syntactic mistakes, including specification deviations leaving half-implemented code. GitClear's "added-code dominates moved/deleted" finding quantifies it.

**Rule.** Before finalizing, run linter or static check for unused imports, unused symbols, unreachable branches; remove them. Do not leave function defined unless something calls it now.

---

## 12. "Declares success" — mock fallbacks in production code

**Pattern.** Hardcoded success, fixture data, or empty defaults instead of real work, then task claimed done.

**Source.** Fowler, "Patterns for Reducing Friction" names "declaring success despite failing tests" and "brute-force fixes." claude-code issue #6984 "Systematic Mock Data Generation Bias". Anthropic Claude Code best practices explicitly tell agents: no mock implementations.

**Bad:**
```text
getUserBalance(userId):
  return 1000  // TODO: actual provider call
```
Shipped as implementation: body is fiction.

**Good:**
```text
getUserBalance(userId):
  raise NotImplemented("Wire to billing.getBalance() after auth is available")
```

**Rule.** Never return hardcoded "success" values or fixture data where spec requires real work. Never disable, skip, or change test to make it pass. If you cannot implement, fail explicitly with unimplemented error and say what is missing.

---

## 13. Plausible-but-wrong code

**Pattern.** Compiles, reads right, encodes slightly wrong formula, range, or null semantic. Often lifted from similar-but-different function.

**Source.** arXiv 2411.01414: 4 of 7 mistake categories are non-syntactic semantic mistakes prior work missed; root cause "misunderstanding of specification." Katanaquant, "Your LLM Doesn't Write Correct Code. It Writes Plausible Code". Simon Willison: code hallucinations are *less* dangerous because they fail loudly. Corollary: dangerous class is plausible-but-wrong code that runs.

**Bad:**
```text
# Compute median
median = (values[values.length / 2] + values[values.length / 2 + 1]) / 2
// off-by-one for odd length

# Iterate items
for index from 1 to items.length - 1:
  ...  // silently drops items[0]
```

**Rule.** For any boundary, range, off-by-one, or null-semantic question, first write case enumeration in comment (`cases: empty / one / even / odd / null`); verify each case before code. Never copy similar function and adapt; re-derive from spec.

---

## 14. YAGNI violations — speculative configurability

**Pattern.** Config flags, env vars, optional parameters, feature toggles for nonexistent use cases.

**Source.** Fowler's overeagerness pattern; HN defensive-code thread. Anecdotal, widely observed.

**Bad:**
```text
renderInvoice(invoice,
              format="pdf", template=null, locale="en",
              includeQr=false, currencyOverride=null,
              debug=false, legacyMode=false)
```
Only one caller exists. It passes `(invoice,)`.

**Good:**
```text
renderInvoice(invoice)
```

**Rule.** No optional parameter, config flag, env var, or feature toggle without present-day caller. Delete any `enable_*`, `use_*`, or `*_mode` argument you catch yourself adding; keep single concrete behavior.

---

## 15. New dependency for trivial work

**Pattern.** Third-party package for what standard library, installed dependency, or a few lines cover: micro-dependency for one-line helper, heavy library for one function.

**Source.** Field reports on AI over-building (Fowler's overeagerness pattern); long-standing supply-chain guidance. Every dependency is permanent maintenance surface (version churn, transitive vulnerabilities, audit and licensing weight); small local function carries none. Same emit-more bias as modes 3 and 14.

**Bad:**
```text
# add a package to sum a column of numbers
import stats_helpers
total = stats_helpers.sum_column(rows, "amount")
```

**Good:**
```text
total = sum(row.amount for row in rows)
```

**Rule.** Before adding package, check stdlib, installed dependencies, and whether a few lines solve it. Add dependency only when it owns real complexity you should not re-implement (cryptography, parsing, time zones — illustrative, not exhaustive); never to avoid short function. Inverse of mode 5 and [dry-kiss-yagni.md](dry-kiss-yagni.md) ranked item 8: do not re-implement what platform gives you, do not import what a few lines cover.

---

## Cross-cutting observation

Nine of 15 modes (1, 2, 3, 9, 12, 14, 15, plus parts of 8 and 11) share one root cause: **the model is biased toward emitting more code, more parameters, more guards, more abstractions**, beyond spec minimum. Cure: restraint, not knowledge. Before each line ask: *does the spec require this, today?* If no, do not write it.

## Where this skill differs from generic clean-code rules

[naming-and-functions.md](naming-and-functions.md), [solid.md](solid.md), and [dry-kiss-yagni.md](dry-kiss-yagni.md) cover classic principles: necessary, not sufficient. 15 patterns above are high-leverage check; walk them before delivery.
