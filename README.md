# Accepted, then timed out: a Java notification lab

A single runnable scenario: a synthetic provider accepts a request, but the client receives a timeout. Compare a broken retry, stable-key retry, and UNKNOWN handling with read-only acceptance lookup. No login, database, Docker, Maven/Gradle, network, credentials or external provider is needed.

This free sample is licensed under MIT (see LICENSE). Download it from the linked itch.io page or run the source locally. The separate paid kit is not included.

## Run

Install **JDK 21 or newer** (both `java` and `javac` on PATH). The source targets Java 21. POSIX shell required for the convenience script; on Windows run the Java commands below directly after creating `build/classes`.

```sh
./run.sh
```

Equivalent commands, from the sample root:

```sh
mkdir -p build/classes
javac --release 21 -d build/classes src/main/java/sample/TimeoutLab.java src/test/java/sample/TimeoutLabTest.java
java -cp build/classes sample.TimeoutLabTest
java -cp build/classes sample.TimeoutLab
```

Assertions explicitly throw `AssertionError`; they do not depend on JVM `-ea`. Any failing test stops the script with a nonzero exit. The broken example's test passes by confirming duplicate provider effects.

## Expected demo

```text
NAIVE attempts=2 provider_effects=2 client_state=UNKNOWN_RECONCILE_REQUIRED
STABLE_KEY attempts=2 provider_effects=1 client_state=ACCEPTED
NO_DEDUPE attempts=1 provider_effects=1 client_state=UNKNOWN_RECONCILE_REQUIRED
RECONCILE lookup=ACCEPTED attempts=1 provider_effects=1 client_state=ACCEPTED
```

| Approach | Provider send calls | Accepted effects | Client result |
|---|---:|---:|---|
| Broken retry with a different key per attempt | 2 | 2 | UNKNOWN_RECONCILE_REQUIRED |
| Reference retry with stable key and identical payload | 2 | 1 | ACCEPTED |
| No dedupe support: stop after ambiguous timeout | 1 | 1 | UNKNOWN_RECONCILE_REQUIRED |
| Trusted positive lookup after UNKNOWN | remains 1 | remains 1 | ACCEPTED |

`ACCEPTED` means the provider accepted a request, not successful delivery to a recipient. UNKNOWN represents unresolved uncertainty, not proof of failure. A NOT_FOUND lookup remains UNKNOWN and does not authorize resend.

## Exact fake-provider contract

- The first call for a new key records acceptance, increments the effect count, then throws `AmbiguousTimeout`.
- With stable-key support enabled, repeating the same key and identical payload acknowledges the prior acceptance without adding an effect. Reusing a key with a different payload throws `IllegalArgumentException`.
- With stable-key support disabled, every send call causes another accepted effect. The reference client therefore makes only one send attempt after uncertainty.
- `alwaysTimeout=true` also loses responses to repeated keys. The reference client's budget is two attempts, after which it stays UNKNOWN even though the fake contains one acceptance.
- Dedupe and lookup scope is one `FakeProvider` object, one synthetic provider account, and a key string. Retention is the entire lifetime of that object; there is no expiry. A new object starts empty. This assumption is stronger than many real providers.
- Lookup is a read-only in-memory teaching interface keyed by the request's correlation key. A positive result is treated as authoritative acceptance. NOT_FOUND does not prove non-acceptance. Some real providers cannot query by a client key; this sample does not invent that capability for them.
- The fake stores acceptance records only in memory; client state is an immutable `Outcome` value. **No state is persisted.** There is no real lookup API, webhook or reconciliation worker.

The tests validate client decisions against this explicit fake contract. They do not test actual network timeouts, delivery, restarts, concurrency, outbox transactions, multi-tenant isolation, provider key-expiry windows, or production safety. There are no post-commit worker or receipt-ordering scenarios here.

## Sample files

Start with `src/main/java/sample/TimeoutLab.java`, then the seven checks in `src/test/java/sample/TimeoutLabTest.java`. `TEST-EVIDENCE.md` records the local run. `TUTORIAL-OUTLINE.md` is a short content plan. `LICENSE` contains the MIT terms for this free sample.


## Continue with the paid teaching kit

[Notification Failure Lab for Spring Boot on Gumroad](https://adrianstudio3.gumroad.com/l/iqwjwr) contains the broader PostgreSQL-backed lab: post-commit recovery, concurrent workers and receipt ordering, plus the guide. Those paid source files are **not included** in this free sample. The MIT license applies only to this sample; it does not grant access to or change the paid kit's separate buyer license.


## Authorship and seller disclosure

This sample was written with AI assistance and checked with the seven executable assertions in `TimeoutLabTest`. The fake-provider model and its limitations are explicit; those test results do not establish production readiness or actual provider behavior.

The related paid teaching kit is sold by Adrian Studio (public seller handle `adrianstudio3`). The paid link below is a seller link, not an independent recommendation. This useful standalone MIT lab runs without purchasing anything.

- [Free sample on itch.io](https://adrianstudio3.itch.io/notification-timeout-lab-free-java-sample)
- [Tutorial: A notification timeout can hide a successful send](https://adrianstudio3.hashnode.dev/a-notification-timeout-can-hide-a-successful-send)
- [Related paid teaching kit on Gumroad](https://adrianstudio3.gumroad.com/l/iqwjwr)
