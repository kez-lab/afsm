---
title: Afsm IDE Graph Preview
updated: 2026-08-29
status: implemented-pending-interactive-smoke
---

# Afsm IDE Graph Preview

## Decision

Ship a separate IntelliJ Platform plugin named `Afsm Graph Preview`. It gives
Android Studio and IntelliJ IDEA users a gutter action beside `@AfsmGraph` and
a right-side tool window that renders the selected machine's generated graph.

This slice implements the graph preview prototype from RFC #58. It does not add
runtime tracing, replay, time travel, or graph-to-source navigation.

## Authoritative Data Path

The IDE must not reinterpret the Afsm Kotlin DSL. A refresh runs the consuming
module's existing `generateAfsmMmd` Gradle task and renders the generated
`build/generated/afsm/mmd/*.mmd` result.

```text
@AfsmGraph source
-> existing KSP registry
-> compiled machine.topology
-> generateAfsmMmd
-> generated .mmd
-> local IDE renderer
```

Shallow PSI/text inspection may locate the annotation, declaration, and graph
filename. It is not allowed to infer phases, transitions, conditions, or
commands. Helper functions and ordinary Kotlin expressions make a second DSL
interpreter incomplete and vulnerable to drift.

## IDE UX Contract

- A gutter icon appears on eligible Kotlin `@AfsmGraph` declarations.
- Exactly one icon is attached to the annotation identifier; Kotlin PSI parent
  nodes must not create duplicate icons on the same line.
- Clicking it selects the machine and opens the `Afsm Graph` tool window.
- The checked-in `afsm-graph/` baseline is displayed immediately when present.
- `Refresh` runs `generateAfsmMmd` through the IDE's Gradle integration.
- A successful refresh replaces the preview with the generated build output.
- A failed refresh keeps the last successful graph visible and marks it stale
  while directing the developer to the Gradle build output.
- Preview refresh never runs `updateAfsmMmd`; committing a baseline remains an
  explicit maintainer action.
- Developers may drag state nodes and transition control handles. Those edits
  alter only the local IDE layout stored in the project workspace; endpoints
  remain attached to their generated source and target states, and Refresh
  never rewrites the graph baseline.
- The default `Inspect` mode supports selection, search, focus, zoom, and pan
  without moving diagram elements. `Arrange` must be selected explicitly before
  nodes or transition routes can move.
- Transition control handles are hidden until a transition is selected in
  `Arrange`; the canvas must not present layout controls as graph semantics.
- States and transitions are selectable. Selection highlights the chosen item
  and its directly connected topology. A compact floating context chip appears
  only while something is selected; hover tooltips expose the remaining
  read-only metadata without reserving a permanent inspector column. Search
  selects and centers a matching state or transition.
- The graph keeps the full tool-window width. Refresh, search, and graph status
  remain in a minimal top bar; mode, layout, focus, zoom, and fit controls live
  in a collapsible bottom-right floating action palette over the canvas.
- `Auto Layout` computes deterministic presentation coordinates from the parsed
  graph. Left-to-right is the default reading direction and top-to-bottom is an
  available alternative. Layout never rewrites MMD or Kotlin.
- The graph canvas supports zoom in/out, 100%, fit-to-window, Ctrl/Command plus
  mouse-wheel zoom, and background-drag panning. Canvas bounds follow moved
  nodes and transition controls so editable content remains reachable.
- Newly selected graphs fit into the current viewport without enlarging above
  100%. Auto Layout restores deterministic node and route placement in the
  selected direction, then fits the restored graph.
- The first version refreshes explicitly. Save-debounced refresh is deferred
  until build latency and cancellation behavior are measured in real use.

## Rendering and Safety

- Keep generated Mermaid as the checked-in documentation and CLI format.
- The IDE preview parses only the constrained Afsm-generated Mermaid output;
  it never interprets the Kotlin DSL or general Mermaid syntax.
- Render the resulting state/transition model with a pure-JVM Java2D component,
  so the preview does not depend on JCEF, Node, Chromium, a CDN, or a network
  resource.
- Use the pure-Java ELK Layered engine for default node placement and routed
  sections while retaining Java2D painting and hit testing. ELK integration
  must pass archive-size, license-notice, performance, and Plugin Verifier
  gates before distribution.
- The first renderer targets Afsm states, initial state, labelled transitions,
  self-transitions, and entry/exit work notes. It intentionally is not a
  general Mermaid renderer.
- Parallel transitions must use distinct curve lanes and label positions;
  opposite-direction transitions between the same state pair share that lane
  allocation rather than crossing through the same center line.
- Every transition renders a visible source dot, a filled target arrowhead, and
  at least one mid-route direction marker on a sufficiently long route. The
  event is the dominant label text; guard and command remain secondary. Hover
  highlights the whole route and both endpoint states so event ownership and
  direction remain readable after fit-to-window scaling.
- Self-transitions and initial markers must reserve visible padding rather than
  being clipped or obscured by node content.
- Never load consuming-project classes into the IDE process. The existing
  Gradle `JavaExec` exporter remains the isolation boundary.

## Build and Distribution Boundary

`afsm-ide-plugin/` is an independent Gradle build with its own Gradle 9 wrapper
and Java 21 toolchain. It is not a root Afsm subproject, is not part of the
Maven Central publication bundle, and does not change the root Android build's
Gradle/JDK contract.

The initial compatibility floor is IntelliJ Platform branch `261` (2026.1).
The plugin depends only on the platform, Kotlin language support, and Gradle
integration so the same archive can be verified against IntelliJ IDEA and
Android Studio builds on that platform branch.

## Graph Meaning

The preview preserves the existing reading contract:

- graph: whole-flow topology, named conditions, and labelled work,
- machine: exact data, guards, commands, and execution order,
- tests: payload behavior and graph-invisible `Handled`, `Ignored`, and
  `Invalid` decisions.

The preview is not a complete executable specification and must not imply that
graph-invisible policies are absent.

## Verification Contract

- Pure tests cover graph target resolution, output selection, stale-state
  behavior, constrained MMD parsing, lane allocation, layout persistence, zoom
  bounds, and Java2D rendering.
- IntelliJ light-fixture tests cover the Kotlin gutter marker and tool-window
  activation path.
- `buildPlugin` must produce an installable ZIP that has no browser or
  JavaScript runtime dependency.
- Plugin Verifier must check the targeted IntelliJ IDEA and Android Studio
  builds before compatibility is claimed.
- A sandbox IDE smoke test must open an Afsm machine, run Refresh, and render a
  real generated graph before the prototype is considered complete.
- On Android Studio 261, saving documents before Refresh must occur in a write
  action; an EDT toolbar callback alone is insufficient.

## Deferred Scope

- save-debounced automatic refresh,
- graph node/edge to DSL source navigation,
- semantic graph authoring such as creating, deleting, or reconnecting states
  and transitions,
- multiple variants and aggregated multi-module graph selection,
- remote-development split-mode optimization,
- runtime trace inspection, replay, or time travel.
