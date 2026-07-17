# Effect-Free Output Model Verification

Date: 2026-07-17 KST

## Scope

This evidence records the implementation of the accepted output-model redesign
that followed the relayed human usability feedback. It covers the maintained
library modules, sample-shop, graph tooling, public docs, and external consumer.

The frozen `afsm-blind-review-input` bundle is intentionally excluded from
source-removal counts because it preserves the exact inputs used by the dated
prior AI review.

## TDD Evidence

Focused core compilation was first run with new three-type API usage before the
production API changed. It failed as expected because the old implementation
required an Effect type parameter. After the implementation change:

```bash
./gradlew :afsm-core:test --no-daemon
./gradlew :afsm-core:apiCheck --no-daemon
```

Result: pass.

The sample tests were then updated to require durable completion state and
verb-named ViewModel APIs before sample production code was migrated. Initial
compilation failed on the old four-type machines, effect DSL/API, and missing
ViewModel verbs. After implementation:

```bash
./gradlew :sample-shop:testDebugUnitTest --no-daemon
./gradlew :sample-shop:generateAfsmMmd --no-daemon
```

Result: pass.

## Module Verification

```bash
./gradlew :afsm-runtime:test :afsm-viewmodel:test :afsm-test:test --no-daemon
./gradlew :afsm-runtime:apiCheck :afsm-viewmodel:apiCheck :afsm-test:apiCheck --no-daemon
./gradlew :afsm-graph-ksp:test --no-daemon
```

Result: pass.

## External Consumer Verification

Current artifacts were published to Maven Local and the separate Android build
was run with refreshed dependencies:

```bash
ANDROID_HOME=/Users/kwak-euijin/Library/Android/sdk \
  ./gradlew -p consumer-smoke \
  -PafsmVersion=0.1.0-SNAPSHOT \
  --refresh-dependencies \
  clean \
  :app:compileDebugKotlin \
  :app:testDebugUnitTest \
  :app:generateAfsmMmd \
  --no-daemon
```

Result: `BUILD SUCCESSFUL`; 20 tasks executed.

## Static Acceptance Audit

Maintained source/API search results:

```text
Effect API/channel/helper declarations or uses: 0 files
Auth/Checkout/ProductEditor Event construction in *Screen.kt: 0 files
sample *Contract.kt files: 0
sample *Flow.kt files: 3
```

`afsm-compose` is absent from settings, release verification, sample, and
consumer dependencies. Five library modules retain API dumps: core, runtime,
test, ViewModel, and graph KSP.

Generated graphs show state/condition/command topology with no Effect labels.
Checkout success is:

```text
PaymentInProgress --> Completed: PaymentSucceeded [matching request]
```

## Evidence Boundary

This proves repository consistency, API removal, graph generation, Maven Local
consumption, and sample behavior. It does not prove that a new human reader now
understands Command without confusion or prefers the revised Android sample.
That requires a fresh controlled first-use session on this revision.
