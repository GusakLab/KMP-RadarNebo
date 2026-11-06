# Comments and Formatting — Clean Code Chapters 4 and 5

Source: Robert C. Martin, *Clean Code*. Summaries: Vivek Khatri Ch. 4, Vivek Khatri Ch. 5, LinkedIn summary of Ch. 4.

## Contents

- Comments
  - C1. Acceptable comments
  - C2. Banned comments
  - C3. Docstring discipline
- Formatting
  - Fmt1. Vertical openness separates concepts
  - Fmt2. Vertical density implies association
  - Fmt3. Vertical distance
  - Fmt4. Horizontal density
  - Fmt5. Match the file you're editing
- Self-check for comments and formatting

## Comments

Foundational rule: **"Don't comment bad code — rewrite it."** Comments signal failure to express intent in code; each is rename or extract candidate.

### C1. Acceptable comments

- **Legal headers** — license boilerplate, copyright.
- **Intent** — *why* behind non-obvious decision. Example: `// Use exponential backoff to avoid hammering the rate limiter during retries.`
- **Warnings of consequences** — `// This function is called during transaction commit; do not raise.`
- **TODOs** — sparingly, with tracking ticket. `// TODO(JIRA-1234): switch to streaming once API supports it.`
- **Public API documentation** — docstrings documenting *contract* (preconditions, postconditions, raises), not body.
- **Amplification** — flags something non-obvious. `# The `+ 1` accounts for the inclusive end of the range; see RFC §3.2.`

### C2. Banned comments

Delete on sight:

- **Restating-code comments.** `// increment counter by one` above `counter += 1`. Zero signal, double maintenance.
- **Noise comments.** `# default constructor`, `# getter`, `# returns the day of month`.
- **Banner comments.** `# ====== USER FUNCTIONS ======`. Use class or module split instead.
- **Closing-brace comments.** `}  // end of for loop`. Need one to follow flow? Function is too long.
- **Attributions and journal comments.** `# Updated by Bob on 2023-04-01 to fix bug #42`. Version control records this.
- **Commented-out code.** Delete; git has it. Commented blocks are toxic: readers do not know whether to trust them.
- **`Step 1` / `Step 2` scaffolding.** Common LLM artifact. Make each step named function call; names give structure.

### C3. Docstring discipline

Documentation comment paraphrasing signature is noise:

**Bad:**
```text
add(a, b)
  // Adds a and b and returns the result.
  return a + b
```

**Good:**
```text
add(a, b)
  return a + b
```

Documentation comment earns keep by documenting contract: what may be passed or returned, errors raised, non-obvious side effects.

**Good:**
```text
// Charge a payment source.
// Returns: charge identifier.
// Raises: CardDeclined for decline failures; PaymentProviderError otherwise.
// Side effect: writes an audit record on success.
charge(paymentSourceId, amountCents)
```

---

## Formatting

### Fmt1. Vertical openness separates concepts

Blank lines between concepts, none inside tightly-coupled block. Eye reads them as boundaries.

### Fmt2. Vertical density implies association

Related code sits together. Variable declared 30 lines from use is smell.

### Fmt3. Vertical distance

- **Variables declared close to use.** Not at function top "C-style."
- **Caller above callee.** Top-down: high-level function first, then helpers it calls. Step-down rule ([naming-and-functions.md](naming-and-functions.md) F4).
- **Conceptually related functions adjacent.** If `parse_invoice` and `validate_invoice` are siblings, keep them adjacent, not at opposite ends of file.

### Fmt4. Horizontal density

- Spaces around assignment and comparison operators: `x = 1`, `if x == 1`.
- No space between function name and parenthesis: `f(x)` not `f (x)`.
- Line length: 80 traditional, ≤100–120 acceptable. Beyond 120 is careless.

### Fmt5. Match the file you're editing

Most common cross-cutting violation: new style in file that already has one. In snake_case file, do not introduce camelCase. With double quotes, do not introduce single quotes. With alphabetized imports, do not append at bottom. If project has HTTP client, database wrapper, or logging helper, reuse it; do not add another.

Team rules beat personal preference. Read file, then write.

---

## Self-check for comments and formatting

Before shipping code:

1. Walk every comment you added. If it explains *what*, not *why*, delete it.
2. Walk every documentation comment you added. Paraphrase of signature? Delete it; keep only contract documentation.
3. Any commented-out code? Delete it.
4. Any `Step 1`, `Step 2`, `First, ...`, or `Then, ...` scaffolding comments? Delete.
5. Variables declared near use, not at top?
6. Casing, quoting, import order match file's existing style?
