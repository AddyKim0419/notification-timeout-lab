# Local validation receipt

Sample version: `0.1.0`. Public sample: https://adrianstudio3.itch.io/notification-timeout-lab-free-java-sample. License: MIT.

Validated with OpenJDK/Javac **21.0.11** (Homebrew) on the Mac mini, 2026-10-02 UTC.

Command: `./run.sh`.

Result: **7 tests passed, 0 failures; exit 0**. No third-party dependencies or network calls.

Checks:
1. `broken retry produces duplicate effect`
2. `stable key gets one effect and an acknowledgement`
3. `unsupported dedupe stops without blind resend`
4. `permanent response loss stays unknown after bounded retry`
5. `fake contract rejects reused key with changed payload`
6. `positive acceptance lookup resolves unknown without sending`
7. `missing lookup remains unknown without sending`

Observed demo:
```
NAIVE attempts=2 provider_effects=2 client_state=UNKNOWN_RECONCILE_REQUIRED
STABLE_KEY attempts=2 provider_effects=1 client_state=ACCEPTED
NO_DEDUPE attempts=1 provider_effects=1 client_state=UNKNOWN_RECONCILE_REQUIRED
RECONCILE lookup=ACCEPTED attempts=1 provider_effects=1 client_state=ACCEPTED
```

This proves decisions against the stated in-process fake-provider contract. All state is in memory. No delivery, real API lookup, restart, concurrency, persistent outbox, multi-tenant or production verification is claimed.
