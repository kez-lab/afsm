# Afsm Graph Preview

An Android Studio and IntelliJ IDEA plugin that previews Afsm-generated state
graphs beside `@AfsmGraph` declarations.

## Install and use

1. Build with `./gradlew buildPlugin`.
2. In the IDE, choose **Settings | Plugins | Install Plugin from Disk** and
   select `build/distributions/afsm-ide-plugin-0.1.1-SNAPSHOT.zip`.
3. Restart the IDE, open an `@AfsmGraph` declaration, and click its single
   gutter icon. Select **Refresh** to run the existing module
   `generateAfsmMmd` task.

The preview opens in **Inspect** mode. Select a state or transition to highlight
it; a compact context chip appears only while something is selected, and hover
shows its full metadata. Search by state, endpoint, event, guard, or command.
The graph keeps the full tool-window width. Open the bottom-right `⋮` floating
palette for **Focus**, zoom out, 100%, zoom in, **Fit**, mode, direction, and
layout actions. Background-drag pans the canvas and `Ctrl`/`Command` plus the
mouse wheel zooms around the pointer.

Switch to **Arrange** before changing presentation coordinates. States can be
dragged, while route handles appear only for the selected transition and keep
both endpoints attached. **Auto Layout** uses the selected left-to-right or
top-to-bottom direction. Manual layout is stored only in the project's local
IDE settings and never rewrites Kotlin or Mermaid files.

Each transition uses a source dot, a filled target arrowhead, and additional
direction markers on long routes. Its event is the primary label; guard and
command are secondary. Hovering a transition highlights the route and both
endpoint states, which makes event ownership and direction easier to follow at
fit-to-window scale.

The first preview uses the checked-in `afsm-graph/*.mmd` baseline when present;
a successful refresh shows `build/generated/afsm/mmd/*.mmd` instead. Refresh
never runs `updateAfsmMmd` or changes committed graph files.

## Rendering boundary

The plugin keeps Mermaid `.mmd` as Afsm's documentation/export format but does
not run Mermaid or depend on JCEF. It parses only the constrained
`stateDiagram-v2` syntax emitted by Afsm and renders states, initial state,
labelled transitions, self-transitions, and entry/exit notes with Java2D. ELK
Layered supplies deterministic node placement and routed edge sections; it does
not render HTML or JavaScript. Opposite-direction and parallel transitions keep
separate geometry, and the canvas grows with manually moved nodes and path
handles so they remain scrollable instead of being clipped.

## Verification

```bash
./gradlew test buildPlugin verifyPluginProjectConfiguration verifyPluginStructure
./gradlew verifyPlugin
./gradlew testAndroidStudio \
  --tests io.github.afsm.ide.AfsmDiagramLayoutEngineTest \
  --tests io.github.afsm.ide.AfsmStateDiagramPanelTest \
  --tests io.github.afsm.ide.AfsmGraphOverlayPaneTest \
  --tests io.github.afsm.ide.AfsmTransitionVisualsTest
```

The build targets IntelliJ Platform branch 261, including Android Studio 2026.1.
