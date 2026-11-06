# Writing Review Feedback

Read before posting review comments to a person or team: mentoring-style reviews, contested feedback, comment templates. SKILL.md severity labels (Critical / no prefix / Optional / Nit / FYI) still decide what is required.

## What review is for

- For catching defects and edge cases, keeping code maintainable, sharing knowledge, keeping design consistent.
- Not for showing off knowledge, arguing formatting a formatter or linter should enforce, or rewriting change into your preferred style.
- Don't hand-review mechanically checkable things (formatting, import order, lint rules). Point to the tool, or ask to add it.

## Comment technique

**Be specific, give the cause.** Name trigger, consequence, remedy.

```text
Weak:   "This is wrong."
Better: "Two requests for the same user can both pass the balance check
         before either write commits (lines 40-52). Wrap the check and the
         debit in one transaction or use a conditional update."
```

**Ask when intent unclear; assert when defect demonstrated.** Question invites author to supply missing context:

```text
"What happens here if `items` is empty?"
"How should this behave when the upstream call times out?"
```

Don't phrase reproduced bug as question; that softens real issue (see "Honesty in Review" in SKILL.md).

**Suggest, don't command, for optional changes.** Example: "This logic appears in three places. Would extracting it into one helper make sense?" For required changes, state requirement plainly.

**Use context → issue → remedy for difficult feedback**, not praise sandwich:

```text
Context: Payment logic currently lives inline in the controller.
Issue:   calculateTotal() mixes tax rules, discounts, and DB queries, so none
         of them can be unit-tested in isolation.
Remedy:  Extract the pure calculation into its own function or service and
         keep the query in the controller. Happy to pair on it.
```

**Acknowledge specific good work.** Example: "The table-driven tests cover every boundary in the spec." Don't add generic praise just to soften review.

## When the author disagrees

1. Ask what led to their approach; they may have context you lack.
2. Acknowledge points that change your assessment.
3. Settle factual disputes with code, test, or measurement, not repetition.
4. Escalate real design disagreement to code owner.
5. If concern is not correctness, security, or structural regression, approve and let go.

## Reviewer pitfalls

- Scope creep: "while you're at it…" requests belonging in follow-up.
- Inconsistency: different standards per author.
- Ghosting: requesting changes, then not re-reviewing.

## Comment templates

Requesting a change:

````markdown
**Issue:** <what fails, and under which input or condition>

**Current code** (`path/to/file.ext:LINE`):
```<lang>
<the problematic lines>
```

**Suggested fix:**
```<lang>
<the smallest change that removes the problem>
```

**Why:** <consequence if left as is>
````

Asking a question:

```markdown
**Question:** <what you need to know>
**Context:** <why it matters for correctness or design>
**Suggestion:** <optional alternative>
```

Whole-review summary:

```markdown
## Summary
<one or two sentences: what was reviewed and the overall assessment>

## Required changes
- Critical: <issue> (`file:line`)
- <issue> (`file:line`)

## Suggestions
- Optional: <improvement>

## Questions
- <clarification needed>

## Verdict
Approve | Request changes | Needs discussion
```
