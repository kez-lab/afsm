# Afsm IDE graph canvas hardening evidence

Date: 2026-08-29

## Scope

- Replace the fixed row layout with deterministic ELK Layered placement and
  orthogonal route sections.
- Separate read-only `Inspect` behavior from local presentation `Arrange`
  behavior.
- Add state/transition selection, connected-topology highlighting, search,
  focus, compact details, layout direction, zoom, fit, pan, and selected-route
  handles.
- Give the graph the full tool-window width, replace the fixed inspector with a
  selection-only context chip, and move secondary actions into a bottom-right
  floating palette.
- Make transition ownership and direction explicit with source dots, filled
  target arrowheads, repeated mid-route markers, event-first labels, and
  route-plus-endpoint hover highlighting.
- Keep Mermaid as the generated artifact and Java2D as the renderer. No JCEF,
  browser, JavaScript, Kotlin DSL interpretation, or semantic graph editing was
  introduced.

## TDD evidence

The first focused test run failed to compile because the ELK layout engine,
direction, route model, interaction state, search, and transition-label model
did not exist. After the implementation, the complete plugin test suite passed.

```bash
cd afsm-ide-plugin
./gradlew test buildPlugin verifyPluginProjectConfiguration \
  verifyPluginStructure --rerun-tasks --no-build-cache
```

Result: `BUILD SUCCESSFUL`.

For the full-width and direction-clarity refinement, the first focused run
failed to compile because `AfsmGraphOverlayPane` and `AfsmTransitionVisuals`
did not yet exist. After implementation, the focused tests and complete suite
passed.

```bash
./gradlew test \
  --tests io.github.afsm.ide.AfsmTransitionVisualsTest \
  --tests io.github.afsm.ide.AfsmGraphOverlayPaneTest \
  --rerun-tasks --no-build-cache
./gradlew test buildPlugin verifyPluginProjectConfiguration \
  verifyPluginStructure --rerun-tasks --no-build-cache
```

Result: both commands completed with `BUILD SUCCESSFUL`.

The Android Studio-specific test runtime also executed the ELK layout and
Java2D renderer tests, which exercises the plugin libraries alongside Android
Studio's bundled plugins and older ELK copy.

```bash
./gradlew testAndroidStudio \
  --tests io.github.afsm.ide.AfsmDiagramLayoutEngineTest \
  --tests io.github.afsm.ide.AfsmStateDiagramPanelTest \
  --tests io.github.afsm.ide.AfsmGraphOverlayPaneTest \
  --tests io.github.afsm.ide.AfsmTransitionVisualsTest \
  --rerun-tasks --no-build-cache
```

Result: `BUILD SUCCESSFUL` against Android Studio
`AI-261.26222.65.2613.16025427`.

## Compatibility and visual evidence

`./gradlew verifyPlugin` reported `Compatible` for both:

- Android Studio `AI-261.26222.65.2613.16025427`
- IntelliJ IDEA `IU-261.26222.65`

The distribution retains upstream library notices and includes
`META-INF/THIRD_PARTY_NOTICES.md` in the plugin JAR.

`AfsmStateDiagramPanelTest` renders the dense Auth graph to
`afsm-ide-plugin/build/reports/visual-tests/auth-state-machine.png`. Visual
inspection confirmed distinct Login, Register, AuthFailed, AuthSucceeded, and
self-transition routes and labels, with the initial marker moved left of the
state so it does not compete with the self-loop. The refreshed image also
shows source dots, filled target arrows, and repeated direction markers on long
routes, while event text remains visually dominant over guards and commands.

`AfsmGraphPreviewPanelVisualTest` also renders the complete preview panel to
`afsm-ide-plugin/build/reports/visual-tests/afsm-graph-preview.png`. The reviewed
output is copied to `docs/assets/afsm-graph-preview.png` and used as the README
hero, so the public feature image comes from the real Swing/Java2D plugin UI
rather than a design mockup.

## Remaining boundary

The sandbox Android Studio starts with the plugin and logs no Afsm class-loading
or linkage error. A human still needs to install the final archive, click the
gutter icon in a real project, run Refresh, and exercise Inspect/Arrange/search
interactively. This evidence does not claim that final hands-on smoke step.
