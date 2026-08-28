# Afsm Graph Canvas UI Research

Date: 2026-08-29

## Project trigger

- The first Java2D preview proved that Afsm graphs can render without JCEF, but
  real Auth graph screenshots exposed label collisions, clipped content, and
  unclear manipulation affordances.
- The user requested reference research followed by implementation of a more
  complete state-machine graph experience.

## Official reference findings

- Android Navigation Editor:
  <https://developer.android.com/guide/navigation/design/editor>
  - Separates destinations/structure, graph canvas, and selected-item
    attributes.
  - Its graph is an editable XML source, unlike Afsm's derived graph.
- Stately Studio design and canvas controls:
  <https://stately.ai/docs/design-mode>
  <https://stately.ai/docs/canvas-view-controls>
  - Separates pointer and hand modes, exposes a structure panel and selected
    details, supports center/zoom-to-selection, and keeps simulation distinct
    from design.
- IntelliJ diagram toolbar and context menu:
  <https://www.jetbrains.com/help/idea/diagram-toolbar-and-context-menu.html>
  - Establishes IDE conventions for layout, orientation, edge routing, zoom,
    fit, search, source navigation, and export.
- Qt Creator SCXML editor:
  <https://doc.qt.io/qtcreator/creator-how-to-create-state-charts.html>
  - Shows transition bend points only after selecting a transition.
- React Flow controls and minimap:
  <https://reactflow.dev/api-reference/components/controls>
  <https://reactflow.dev/api-reference/components/minimap>
  - Uses compact viewport controls, an interaction lock, and an optional
    navigable overview for large graphs.
- Eclipse Layout Kernel:
  <https://eclipse.dev/elk/reference/algorithms/org-eclipse-elk-layered.html>
  <https://eclipse.dev/elk/gettingstarted.html>
  - The pure-Java layered engine supports multi-edges, self-loops, labels,
    ports, and orthogonal/spline routing and is published on Maven Central.
  - Version checked for the implementation spike: `0.12.0`.
- IntelliJ Diagram API boundary:
  <https://platform.jetbrains.com/t/any-documentation-for-com-intellij-diagrams/320>
  - JetBrains states that the diagram provider extension has no public
    documentation and the feature belongs to IntelliJ Ultimate. It is not a
    suitable cross-IDE foundation for the Afsm plugin.

## Synthesis

- Afsm must borrow analysis and navigation affordances, not semantic graph
  authoring. The generated graph is derived from compiled topology.
- Keep the public Swing/Java2D renderer and use ELK only for deterministic
  coordinates and routing. Do not depend on JCEF or undocumented IntelliJ
  diagram APIs.
- Default to inspection. Manual node and route manipulation requires an
  explicit Arrange mode; connection endpoints and topology remain locked.
- Selection, search, focus, layout direction, and compact viewport controls
  precede source navigation and runtime inspection.
