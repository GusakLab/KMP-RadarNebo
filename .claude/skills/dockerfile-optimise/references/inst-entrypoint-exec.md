---
title: Use exec in Entrypoint Scripts
impact: MEDIUM
impactDescription: eliminates 10-second forced kill delay on every container stop
tags: inst, entrypoint, exec, signals, pid1
---

## Use exec in Entrypoint Scripts

Without `exec`, entrypoint shell stays PID 1 and application runs as child. Shell does not forward OS signals (SIGTERM, SIGINT) to children by default, so application never gets stop signal. Docker waits grace period (default 10 seconds), then SIGKILLs whole process tree. No graceful shutdown, connection draining or cleanup.

**Incorrect (no exec -- shell stays PID 1, application cannot receive signals):**

```bash
#!/bin/bash
# docker-entrypoint.sh
setup_database
python manage.py migrate
python manage.py runserver 0.0.0.0:8000
```

(Bash is PID 1 and receives SIGTERM but does not forward it to python. Server runs until SIGKILL after timeout, cutting off in-flight requests and database transactions.)

**Correct (exec replaces shell with application as PID 1):**

```bash
#!/bin/bash
# docker-entrypoint.sh
set -e
setup_database
python manage.py migrate
exec python manage.py runserver 0.0.0.0:8000
```

(`exec` builtin replaces shell with python, making python PID 1. SIGTERM reaches application directly, so it can close database connections, finish in-flight requests and exit cleanly.)

**Correct (exec "$@" pattern -- entrypoint delegates to CMD arguments):**

```bash
#!/bin/bash
# docker-entrypoint.sh
set -e

# Run initialization tasks
if [ "$1" = 'postgres' ]; then
    # Initialize data directory if needed
    if [ -z "$(ls -A "$PGDATA")" ]; then
        initdb --username="$POSTGRES_USER" --pwfile=<(echo "$POSTGRES_PASSWORD")
    fi
fi

# Replace shell with the CMD arguments
exec "$@"
```

(`exec "$@"` hands all CMD arguments to `exec`, making CMD process PID 1. Official Docker images like PostgreSQL, Redis and MySQL use it.)

**Dockerfile using the exec "$@" pattern:**

```dockerfile
FROM python:3.12-slim

WORKDIR /app
COPY requirements.txt ./
RUN pip install --no-cache-dir -r requirements.txt
COPY . .

COPY docker-entrypoint.sh /
RUN chmod +x /docker-entrypoint.sh

ENTRYPOINT ["/docker-entrypoint.sh"]
CMD ["gunicorn", "main:app", "--bind", "0.0.0.0:8000", "--workers", "4"]
```

(CMD supplies default arguments for `exec "$@"`. `docker run myimage celery worker` overrides CMD; script initializes, then runs `exec celery worker`.)

### Guidelines

- Always start entrypoint scripts with `set -e`, so initialization failures abort container start instead of silently continuing.
- Put `exec` on **last line**. Commands after `exec` never run, because `exec` replaces current process.
- Use `exec "$@"`, not hardcoded application command. Keeps initialization (ENTRYPOINT) separate from default command (CMD).
- Use **exec form** ENTRYPOINT (`ENTRYPOINT ["/docker-entrypoint.sh"]`). Shell form (`ENTRYPOINT /docker-entrypoint.sh`) wraps script in `/bin/sh -c`, adding yet another shell layer.

Reference: [Building best practices](https://docs.docker.com/build/building/best-practices/)
