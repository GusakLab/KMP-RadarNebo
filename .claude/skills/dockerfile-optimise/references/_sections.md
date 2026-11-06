# Sections

Section ID (in parentheses) is filename prefix grouping rules.

---

## 1. Layer Caching & Ordering (cache)

**Impact:** CRITICAL
**Description:** Wrong layer order invalidates all downstream cache: full rebuilds. Cache mounts and instruction order are #1 build time optimization; one misplaced COPY can add minutes to every build.

## 2. Multi-Stage Builds (stage)

**Impact:** CRITICAL
**Description:** Without multi-stage, build tools, compilers and intermediate artifacts ship to production: 2-10x image size bloat, wider attack surface, slower deploys.

## 3. Base Image Selection (base)

**Impact:** HIGH
**Description:** Base image sets size floor, security surface and compatibility for every layer above. Wrong choice cascades through whole image.

## 4. Build Context Management (ctx)

**Impact:** HIGH
**Description:** Large build contexts slow transfer to builder daemon and spuriously invalidate cache. Bind mounts and .dockerignore cut unneeded transfer.

## 5. Security & Secrets (sec)

**Impact:** HIGH
**Description:** Secrets in ARG or ENV persist in image layers forever, readable by anyone with image access. Production images need secret mounts, non-root users and attestations.

## 6. Dependency Management (dep)

**Impact:** MEDIUM-HIGH
**Description:** Package manager operations are costliest build steps. Cache mounts, version pinning and cleanup cut redundant downloads and image size.

## 7. Instruction Patterns (inst)

**Impact:** MEDIUM
**Description:** Wrong CMD/ENTRYPOINT form breaks signal handling. Missing HEALTHCHECK prevents orchestrator health detection. Heredocs make multi-line scripts readable.

## 8. Quality & Validation (lint)

**Impact:** MEDIUM
**Description:** Docker build checks, pipefail, standard labels and single-concern containers catch silent failures and keep long-term image quality.

## 9. Language Patterns (lang)

**Impact:** HIGH
**Description:** Each ecosystem has own dependency layout, cache paths and runtime needs. Language-specific templates applying generic rules avoid most common size, cache and signal-handling mistakes.
