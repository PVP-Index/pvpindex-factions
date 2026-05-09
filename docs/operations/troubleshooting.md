---
title: Troubleshooting
parent: Operations
nav_order: 2
---

# Troubleshooting

## Java class version mismatch

Symptoms:

- `class file has wrong version 69.0, should be 65.0`

Cause:

- Dependency compiled for newer Java than your build lane.

Fix:

- Pin Java 21-compatible versions in `pom.xml`.

## Maven compiler `this.hashes` null crash

Symptoms:

- `Cannot load from object array because "this.hashes" is null`

Mitigation:

- Disable incremental compilation in compiler plugin config.
- Use compiler fork mode.

## Missing artifacts from JitPack

Symptoms:

- `Could not find artifact ...`

Fix:

- Verify exact artifactId casing and tag version in JitPack metadata.
