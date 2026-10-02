# Tutorial outline: the timeout that already sent

1. Show the four demo output lines. Ask what the client actually knows after a timeout.
2. Read the fake's acceptance-before-exception boundary. Distinguish send calls, accepted effects and recipient delivery.
3. Run the broken assertion: two different attempt keys create two effects, even on a deduping provider.
4. Run the stable-key assertion: identical key/payload yields two calls but one effect. Explain provider-specific scope and retention.
5. Disable dedupe: keep UNKNOWN and do not blindly resend. Use a trusted positive lookup to reconcile without another send; NOT_FOUND stays unknown.
6. State the boundaries: all state is in memory, provider is fake, no restart/concurrency/production guarantee. Invite readers to inspect their provider's documented contract before adapting the pattern.

Run the complete sample with `./run.sh`. Free download: https://adrianstudio3.itch.io/notification-timeout-lab-free-java-sample
