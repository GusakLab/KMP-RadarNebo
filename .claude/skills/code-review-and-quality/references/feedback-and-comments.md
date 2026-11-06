# Writing Review Feedback

Read this when you are about to post review comments to a person or a team. That includes mentoring-style reviews, contested feedback, and cases where you need a comment template. The severity labels in SKILL.md (Critical / no prefix / Optional / Nit / FYI) still decide what is required.

## What review is for

- Use review to catch defects and edge cases, keep the code maintainable, share knowledge, and keep design consistent.
- Don't use review to show off knowledge, to argue about formatting a formatter or linter should enforce, or to rewrite the change into your preferred style.
- Don't review mechanically checkable things by hand (formatting, import order, lint rules). Point to the tool, or ask for it to be added.

## Comment technique

**Be specific and give the cause.** Name the trigger, the consequence, and a remedy.

```text
Weak:   "This is wrong."
Better: "Two requests for the same user can both pass the balance check
         before either write commits (lines 40-52). Wrap the check and the
         debit in one transaction or use a conditional update."
```

**Ask when intent is unclear, and assert when the defect is demonstrated.** A question invites the author to supply missing context:

```text
"What happens here if `items` is empty?"
"How should this behave when the upstream call times out?"
```

Don't phrase a reproduced bug as a question. That softens a real issue (see "Honesty in Review" in SKILL.md).

**Suggest rather than command for optional changes.** Example: "This logic appears in three places. Would extracting it into one helper make sense?" For required changes, state the requirement plainly.

**Use context → issue → remedy for difficult feedback** instead of a praise sandwich:

```text
Context: Payment logic currently lives inline in the controller.
Issue:   calculateTotal() mixes tax rules, discounts, and DB queries, so none
         of them can be unit-tested in isolation.
Remedy:  Extract the pure calculation into its own function or service and
         keep the query in the controller. Happy to pair on it.
```

**Acknowledge good work when it is specific.** For example: "The table-driven tests cover every boundary in the spec." Don't add generic praise just to soften a review.

## When the author disagrees

1. Ask what led to their approach. They may have context you lack.
2. Acknowledge points that change your assessment.
3. Settle factual disputes with code, a test, or a measurement, not with repetition.
4. Escalate to the code owner when there is a real design disagreement.
5. If the concern is not a correctness, security, or structural regression, approve and let it go.

## Reviewer pitfalls

- Perfectionism: blocking on style preferences.
- Scope creep: "while you're at it…" requests that belong in a follow-up.
- Inconsistency: applying different standards to different authors.
- Ghosting: requesting changes and then not re-reviewing.
- Rubber-stamping, or bike-shedding on trivia while a structural problem goes unmentioned.

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
