---
title: Python - build a venv in a builder stage, copy it to a slim runtime
impact: HIGH
impactDescription: removes compilers and package-manager caches from the runtime image; dependency layer survives source edits
tags: lang, python, uv, poetry, pip, venv, multi-stage
---

## Python - build a venv in a builder stage, copy it to a slim runtime

Installing dependencies and source in one step rebuilds every dependency on each code change, and leaves build tools (gcc, headers, pip caches) in the image. Install the locked dependencies into a virtual environment in a builder stage, then copy only that venv and the source into a runtime stage based on the **same** Python image (the venv's interpreter path must exist at the same location).

**Incorrect (single stage, source copied before dependencies, runs as root):**

```dockerfile
FROM python:3.12
WORKDIR /app
COPY . .
RUN pip install -r requirements.txt
CMD ["python", "app.py"]
# Every source change re-runs pip; ~1GB image with compilers; root user
```

**Correct (uv, locked, two-step sync for caching):**

```dockerfile
# syntax=docker/dockerfile:1
FROM python:3.12-slim AS builder
# Pin the uv image to a specific version instead of :latest
COPY --from=ghcr.io/astral-sh/uv:<uv-version> /uv /uvx /bin/
ENV UV_COMPILE_BYTECODE=1 UV_LINK_MODE=copy
WORKDIR /app
# 1) Dependencies only (cached until the lockfile changes)
RUN --mount=type=cache,target=/root/.cache/uv \
    --mount=type=bind,source=uv.lock,target=uv.lock \
    --mount=type=bind,source=pyproject.toml,target=pyproject.toml \
    uv sync --locked --no-install-project --no-dev
# 2) The project itself
COPY . .
RUN --mount=type=cache,target=/root/.cache/uv \
    uv sync --locked --no-dev

FROM python:3.12-slim
RUN useradd --uid 10001 --no-create-home appuser
WORKDIR /app
COPY --from=builder --chown=appuser:appuser /app /app
ENV PATH="/app/.venv/bin:$PATH" PYTHONUNBUFFERED=1
USER appuser
EXPOSE 8000
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
```

**Alternative (pip with requirements.txt):**

```dockerfile
FROM python:3.12-slim AS builder
RUN python -m venv /opt/venv
ENV PATH="/opt/venv/bin:$PATH"
COPY requirements.txt .
RUN --mount=type=cache,target=/root/.cache/pip pip install -r requirements.txt

FROM python:3.12-slim
COPY --from=builder /opt/venv /opt/venv
ENV PATH="/opt/venv/bin:$PATH"
# ... copy source, non-root USER, CMD
```

For Poetry, install with `poetry install --no-root --only main` and `POETRY_VIRTUALENVS_IN_PROJECT=1`, then copy `/app/.venv`; or `poetry export` to requirements and use the pip pattern (the export command is a plugin in recent Poetry versions; check the installed version).

**Pitfalls:**
- Alpine uses musl; many wheels are glibc-only, so packages like numpy may compile from source. Prefer `-slim`.
- Packages that link system libraries need the `-dev` package in the builder and only the runtime library (for example `libpq5`) in the final stage.
- Run Django `collectstatic` in the builder or as root before `USER`, not at container start.
- Distroless Python images ship the distribution's Python version, which may differ from `python:3.x`; the venv only works if the interpreter version matches.

Reference: [uv: Using uv in Docker](https://docs.astral.sh/uv/guides/integration/docker/)
