# Goal Contract

This concise file is reviewed by the Human before `GO`. Once activated, its content hash is immutable for the active Goal.

```text
Goal ID:
Objective:
Non-goals:
S-level / route:
Acceptance checks:
-
Automatic repair budget: 3 focused attempts per unchanged blocker

## Allowed Files / Areas
<!-- harness:allowed-paths:start -->
-
<!-- harness:allowed-paths:end -->

## Forbidden Actions
<!-- harness:forbidden-actions:start -->
- secrets, credentials, private browser state
- destructive actions, database writes or migrations, deployment/restart, production remote mutation, paid actions
<!-- harness:forbidden-actions:end -->

## Stop Conditions
<!-- harness:stop-conditions:start -->
- material objective, acceptance, architecture, or authorization change
- action or file outside the allowed boundary
- fresh authority required for a protected action
- verification cannot continue safely
- the same blocker remains after three focused repairs
<!-- harness:stop-conditions:end -->
```

The contract contains no raw prompts, secrets, transcripts, command output, or source dumps. A generic Goal `GO` never authorizes a protected action listed above.
