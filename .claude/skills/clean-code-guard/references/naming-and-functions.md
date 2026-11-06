# Naming and Functions — Clean Code Chapters 2 and 3

Source: Robert C. Martin, *Clean Code*. Sample chapters: Pearson PDF. Summaries: Vivek Khatri's Ch. 2 notes, Herberto Graça's Ch. 3 summary.

## Contents

- Meaningful names
  - N1. Intention-revealing
  - N2. No disinformation, no encodings
  - N3. Meaningful distinctions
  - N4. Searchable, pronounceable
  - N5. Class names are nouns, method names are verbs
  - N6. Banned generic names
- Functions
  - F1. Small. Then smaller.
  - F2. Do one thing
  - F3. One level of abstraction per function
  - F4. Step-down rule
  - F5. Few arguments
  - F6. No flag arguments
  - F7. No output arguments / Command-Query Separation
  - F8. No side effects in queries
  - F9. Prefer exceptions to return codes
  - F10. Duplication is the root evil
- Self-check for naming and functions

## Meaningful names

### N1. Intention-revealing

Name tells *why it exists, what it does, and how it's used*. If name needs explanatory comment, it is wrong.

**Bad:**
```text
d  // elapsed time in days
ts = []
fn(xs)
```

**Good:**
```text
elapsedDays
timestamps
filterOverdueInvoices(invoices)
```

### N2. No disinformation, no encodings

No Hungarian notation (`strName`, `iCount`). No interface-prefix `I` (`IUserService`). No member prefix `m_`. No "List" suffix unless type is actually list (`accountList` for a `set` is disinformation).

**Bad:** `strFirstName`, `IUserRepo`, `m_count`, `userArray` (when not an array).

**Good:** `first_name`, `UserRepo`, `count`, `users_by_id`.

### N3. Meaningful distinctions

No noise-word distinctions. `ProductInfo`, `ProductData`, `Product`: what's the difference? Same for `getActiveAccount` vs. `getActiveAccountInfo`. If distinction is real, name it.

### N4. Searchable, pronounceable

Single-letter names acceptable in short loop scope (`for i in range(...)`); elsewhere they hurt grep. `MAX_RETRIES` is searchable; `7` is not.

Name unreadable aloud in code review is bad. Book's real-world example `genymdhms`; fix: `generation_timestamp`.

### N5. Class names are nouns, method names are verbs

`User`, `Invoice`, `Account`: classes are things. `saveInvoice`, `computeTotal`, `notifyUser`: methods are actions. Class `ProcessInvoice` and method `Invoice` are both wrong.

### N6. Banned generic names

Unqualified, these names always violate intention-revealing:

- `data`, `data2`, `data_final`
- `result`, `result_final`
- `item`, `value`, `temp`, `obj`, `info`
- `helper`, `manager`, `utils`, `common`
- `handle_*`, `process_*`, `do_*` (when `*` is also generic)

Qualified versions fine: `raw_csv_bytes`, `parsed_invoice`, `dedup_by_email`.

---

## Functions

### F1. Small. Then smaller.

Target ≤20 lines. Uncle Bob's harder pass: goal is 2–4 lines. Function that does not fit on screen does too much. Extract.

### F2. Do one thing

Function does one thing when you cannot extract another function from it whose name is not restatement of its body. If `compute_invoice` holds 10-line block you could meaningfully call `apply_discount`, original did more than one thing.

### F3. One level of abstraction per function

Mixing levels: most common subtle defect. Do not put HTTP call, SQL query, regex parse, and business rule in one function: four levels.

**Bad:**
```text
renderUserReport(userId):
  connection = openDatabaseConnection()
  row = queryUserRow(connection, userId)
  displayName = row.firstName + " " + row.lastName
  markup = "<h1>" + displayName + "</h1>"
  return markup
```
Four levels: connection, query, formatting, presentation.

**Good:**
```text
renderUserReport(userId):
  user = userRepository.findById(userId)
  return userReportView.render(user)
```

### F4. Step-down rule

Read file top-to-bottom; each function followed by functions one abstraction level below. Callers above callees.

### F5. Few arguments

Zero best. One fine. Two OK. Three "should be avoided." Four or more "requires very special justification", usually meaning pass config object.

At five parameters, stop and extract request/config object: record, struct, DTO, or equivalent.

### F6. No flag arguments

Boolean parameter that switches behavior is always wrong. Split into two functions.

**Bad:**
```text
render(invoice, asHtml):
  if asHtml:
    ...
  else:
    ...
```

**Good:**
```text
renderInvoiceHtml(invoice)
renderInvoicePdf(invoice)
```

Same for `mode="x"` string enums that change behavior. If `mode` parameterizes data (locale, currency), fine. If it parameterizes *which function runs*, split.

### F7. No output arguments / Command-Query Separation

Function returns value (query) or has side effect (command). Never both.

**Bad:**
```text
save(record) -> boolean
// Returns true if saved, false if record was not found.
```
What does bool mean? Success? Found-ness? Caller cannot tell.

**Good:**
```text
save(record)
recordExists(recordId) -> boolean
```

### F8. No side effects in queries

Getter-, finder-, or predicate-style function must not mutate state. Caching? Log cache write at debug level; do not change observable behavior.

### F9. Prefer exceptions to return codes

`if save(x):` is code smell. Save either succeeds (returns nothing) or raises (`InvoiceSaveError`). Return codes spread up call stack, get forgotten; exceptions cannot be silently ignored.

### F10. Duplication is the root evil

If two functions share non-trivial block, extract it. See [dry-kiss-yagni.md](dry-kiss-yagni.md) for when this is wrong (Sandi Metz's "wrong abstraction" caveat).

---

## Self-check for naming and functions

Before shipping code:

1. Every name answers "what does this represent" without comment?
2. Functions ≤20 lines?
3. Functions doing one thing? (Can you extract another function with non-restating name? If yes, it does more than one.)
4. Mixed abstraction levels eliminated?
5. Any function with >4 parameters? Extract config object.
6. Boolean flag arguments? Split.
7. Any function returning value *and* mutating state callers depend on? Split.
