---
title: Use JSON Form for CMD and ENTRYPOINT
impact: MEDIUM
impactDescription: eliminates 10-second forced kill timeout on every container stop
tags: inst, cmd, entrypoint, exec-form, signals
---

## Use JSON Form for CMD and ENTRYPOINT

Shell form (`CMD command arg1`) wraps process in `/bin/sh -c`, so shell becomes PID 1 instead of application. Shell does not forward OS signals (SIGTERM, SIGINT) to child, so application cannot shut down gracefully. In orchestrators like Kubernetes or Docker Swarm, every deploy, restart or scale-down hits 10-second forced kill timeout.

**Incorrect (shell form -- shell is PID 1, application gets no signals):**

```dockerfile
FROM node:22-slim

WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci --production
COPY . .

EXPOSE 3000
CMD node server.js
```

(`CMD node server.js` becomes `CMD ["/bin/sh", "-c", "node server.js"]`. After 10-second grace period, Docker sends SIGKILL, killing process without cleanup.)

**Correct (JSON/exec form -- application is PID 1, receives signals directly):**

```dockerfile
FROM node:22-slim

WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci --production
COPY . .

EXPOSE 3000
CMD ["node", "server.js"]
```

(`CMD ["node", "server.js"]` runs node as PID 1, no intermediate shell. SIGTERM reaches node, so it can close database connections, flush logs and finish in-flight requests before exit.)

**Correct (ENTRYPOINT in exec form for Python applications):**

```dockerfile
FROM python:3.12-slim

WORKDIR /app
COPY requirements.txt ./
RUN pip install --no-cache-dir -r requirements.txt
COPY . .

EXPOSE 8000
ENTRYPOINT ["python", "-m", "uvicorn"]
CMD ["main:app", "--host", "0.0.0.0", "--port", "8000"]
```

(Both use exec form. ENTRYPOINT sets executable; CMD sets default arguments. `docker run myimage --workers 4` replaces CMD, keeps ENTRYPOINT: `python -m uvicorn --workers 4`.)

### Shell Form vs Exec Form Reference

| Instruction | Shell Form | Exec Form |
|-------------|-----------|-----------|
| CMD | `CMD node server.js` | `CMD ["node", "server.js"]` |
| ENTRYPOINT | `ENTRYPOINT python app.py` | `ENTRYPOINT ["python", "app.py"]` |
| PID 1 | `/bin/sh -c` | Your application |
| Signal forwarding | No | Yes |
| Variable expansion | Yes (`$HOME`) | No (use shell explicitly if needed) |

### When Shell Form is Acceptable

Shell form is needed for shell features like variable expansion, pipes or redirection. Then add `exec` for proper signal handling:

```dockerfile
CMD ["sh", "-c", "exec java $JAVA_OPTS -jar /app/server.jar"]
```

(`exec` replaces shell with java, restoring PID 1 signal handling while keeping variable expansion.)

Reference: [Building best practices](https://docs.docker.com/build/building/best-practices/)
