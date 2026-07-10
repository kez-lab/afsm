# First-Use API Experiment Verification

Date: 2026-07-10

Purpose: test alternative Afsm machine declaration shapes against realistic
Draft, Auth, and Checkout behavior before changing the unpublished production
API.

## Prototype Source

- `afsm-core/src/test/kotlin/afsm/core/AfsmFirstUseApiExperimentTest.kt`

The test-only source compares a staged three-type declaration, named type
channels, and a composed feature value. No production source was changed.

## Passing Verification

```bash
./gradlew :afsm-core:test --tests 'afsm.core.AfsmFirstUseApiExperimentTest'
./gradlew :afsm-core:check
```

Result: passed.

## Intentional Compile-Failure Probes

Temporary failing declarations were added, compiled, and then removed.

- Supplying three explicit type arguments to a five-type direct-machine
  function failed with `Inapplicable candidate(s)`.
- Omitting five generic superclass arguments from the proposed feature object
  failed with `5 type arguments expected for class 'ExperimentalFeature'`.
- Using an Auth event inside a Draft builder failed with `Type argument is not
  within its bounds: should be subtype of DraftEvent`.
- Giving the named-channel/feature candidates the wrong command or effect token
  remained type-safe but reported an unresolved `draftFlow()` receiver mismatch.

## Result

- Exact Candidates B and D are invalid Kotlin shapes.
- Their compiling fallbacks and Candidate C preserve the tested behaviors but
  add staged builders, tokens, or a feature container.
- The next hypothesis is an explicitly typed top-level machine property plus
  KSP property discovery.
- Human first-use preference remains unverified.
