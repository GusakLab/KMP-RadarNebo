---
title: Handle Errors Immediately After the Call
impact: LOW-MEDIUM
tags: idiom, go, error-handling, explicit-errors
---

## Handle Errors Immediately After the Call

Always check errors right after a call returns. Do not defer or accumulate error checks. Add context when propagating.

**Incorrect (deferred or accumulated error checks):**

```go
// Bad: errors accumulate, hard to know which failed
func processFiles(paths []string) error {
    var lastErr error

    for _, path := range paths {
        data, err := os.ReadFile(path)
        if err != nil {
            lastErr = err // Previous errors lost
        }

        result, err := process(data)
        if err != nil {
            lastErr = err
        }

        err = save(result)
        if err != nil {
            lastErr = err
        }
    }

    return lastErr
}

// Bad: ignoring error
func quickRead(path string) []byte {
    data, _ := os.ReadFile(path) // Silent failure
    return data
}
```

**Correct (handle immediately with context):**

```go
func processFiles(paths []string) error {
    for _, path := range paths {
        data, err := os.ReadFile(path)
        if err != nil {
            return fmt.Errorf("reading %s: %w", path, err)
        }

        result, err := process(data)
        if err != nil {
            return fmt.Errorf("processing %s: %w", path, err)
        }

        if err := save(result); err != nil {
            return fmt.Errorf("saving result for %s: %w", path, err)
        }
    }

    return nil
}

// When you need to continue despite errors, collect them explicitly
func processAllFiles(paths []string) error {
    var errs []error

    for _, path := range paths {
        if err := processFile(path); err != nil {
            errs = append(errs, fmt.Errorf("%s: %w", path, err))
            continue // Explicit decision to continue
        }
    }

    return errors.Join(errs...) // Go 1.20+
}
```

### When Ignoring Errors is Acceptable

```go
// Explicitly document why it's safe to ignore
_ = writer.Close() // Best effort cleanup, already returning another error

// Or use a helper for intentional ignores
func must[T any](v T, err error) T {
    if err != nil {
        panic(err)
    }
    return v
}
```
