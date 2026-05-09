---
title: Database Settings
parent: Configuration
nav_order: 2
---

# Database Settings (`database.yml`)

## H2 (default)

Best for small/medium servers or simple deployments.

```yaml
type: h2
h2:
  file: data/factions
```

## MySQL/MariaDB

Recommended for larger networks or multi-service hosting.

```yaml
type: mysql
mysql:
  host: localhost
  port: 3306
  database: factions
  username: root
  password: ""
  pool-size: 10
```

## Debug

`debug.jaloquent-logging` can help diagnose DB issues, but should be disabled in production.
