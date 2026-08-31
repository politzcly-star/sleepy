# Context Capsule

Use this bounded, incremental state only for long-running or overnight Goals, expected compaction, or multi-milestone work. Do not create it for routine small fixes. Refer to durable files rather than copying them. Do not include raw logs, full diffs, transcripts, secrets, private payloads, database dumps, or browser state.

```text
Goal ID:
Context epoch:
Contract reference / hash:
Objective and current milestone:
Durable decisions / non-goals:
Hot files / symbols:
Changed since previous epoch:
Checks and verification state:
Unresolved risks / forbidden actions:
Evidence references:
Next action:
```

Keep the rendered capsule below 16 KiB. If it is stale, malformed, oversized, or secret-like, do not inject it into a resumed session; reload the frozen contract and repository authority files instead.
