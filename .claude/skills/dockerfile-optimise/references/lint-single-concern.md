---
title: One Concern per Container
impact: MEDIUM
impactDescription: enables independent scaling, updates, and failure isolation per service
tags: lint, single-concern, architecture, decoupling
---

## One Concern per Container

Several services in one container (e.g. web server, application and background worker under supervisord) cannot be scaled, updated, restarted or monitored independently. One crash takes down the rest. One service needing capacity forces scaling whole bundle. Logs from several processes interleave, hurting debugging and structured logging pipelines.

**Incorrect (multiple services in one container via supervisord):**

```dockerfile
FROM python:3.12-slim

RUN apt-get update && apt-get install -y --no-install-recommends \
    nginx \
    supervisor \
    && rm -rf /var/lib/apt/lists/*

COPY supervisord.conf /etc/supervisord.conf
COPY nginx.conf /etc/nginx/nginx.conf
COPY . /app

RUN pip install --no-cache-dir -r /app/requirements.txt

EXPOSE 80

CMD ["supervisord", "-c", "/etc/supervisord.conf"]
```

(Container runs nginx, gunicorn and celery worker under supervisord. If celery worker OOMs, supervisord restarts it, but nginx/gunicorn may drop requests meanwhile.)

**Correct (separate containers for each concern):**

Web server container:

```dockerfile
# web.Dockerfile
FROM python:3.12-slim

WORKDIR /app
COPY requirements.txt ./
RUN pip install --no-cache-dir -r requirements.txt
COPY . .

USER nobody
EXPOSE 8000
CMD ["gunicorn", "app:create_app()", "--bind", "0.0.0.0:8000", "--workers", "4"]
```

Background worker container:

```dockerfile
# worker.Dockerfile
FROM python:3.12-slim

WORKDIR /app
COPY requirements.txt ./
RUN pip install --no-cache-dir -r requirements.txt
COPY . .

USER nobody
CMD ["celery", "-A", "tasks", "worker", "--loglevel=info", "--concurrency=2"]
```

Connected via Docker Compose:

```yaml
services:
  web:
    build:
      dockerfile: web.Dockerfile
    ports:
      - "8000:8000"
    deploy:
      replicas: 2

  worker:
    build:
      dockerfile: worker.Dockerfile
    deploy:
      replicas: 4

  nginx:
    image: nginx:1.27-alpine
    ports:
      - "80:80"
    volumes:
      - ./nginx.conf:/etc/nginx/nginx.conf:ro
    depends_on:
      - web
```

(Each service scales alone: worker at 4 replicas, web at 2. Worker crash leaves web requests unaffected. One log stream per container keeps aggregation simple.)

### Not a Hard Rule

"One concern per container" is guideline, not strict "one process per container" rule. Helpers that directly support main process are fine:

- Log rotation sidecar managing main process's log files
- Init process like `tini` reaping zombie processes
- Startup script running migrations before handing off to application

**Tightly coupled helpers** are acceptable; **independent services** belong in separate containers. If process could reasonably have own scaling policy, deployment lifecycle or health check, give it own container.

### Signals and Health

Separate containers also simplify signal handling and health checks:

```dockerfile
# Each container has a clear health check for its single concern
HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
    CMD curl -f http://localhost:8000/health || exit 1
```

With several services in one container, single `HEALTHCHECK` cannot represent all of them. One healthy process masks failures in others.

Reference: [Building best practices](https://docs.docker.com/build/building/best-practices/)
