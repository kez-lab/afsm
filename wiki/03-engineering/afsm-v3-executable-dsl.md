---
title: Afsm v3 Executable DSL
updated: 2026-07-17
status: implemented
---

# Afsm v3 Executable DSL

## Decision

One scoped phase/data DSL is the executable reducer, graph topology source, and
pure test target. It models `State`, `Event`, and `Command`; Android concerns
remain in the host/ViewModel.

## Core Shape

```kotlin
val machine: AfsmDefaultMachine<State, Event, Command> = afsmMachine {
    initial(Phase.Idle, Data())

    phase(Phase.Idle) {
        on<Event.Start> {
            transitionTo(Phase.Working)
        }
    }

    phase(Phase.Working) {
        onEnter {
            command("StartWork") { Command.StartWork(data.id) }
        }
        on<Event.Succeeded> {
            transitionTo(Phase.Completed)
        }
    }

    phase(Phase.Completed)
}
```

`AfsmMachine<State, Event, Command>` is used when the host supplies dynamic
initial state. `AfsmDefaultMachine` is used for genuine static defaults.

## DSL Vocabulary

- `phase`: phase-local rule scope,
- `on<Event>`: typed event handler,
- `updateData`: extended state update,
- `transitionTo`: phase change,
- `command`: host-work value,
- `case`: named conditional branch only,
- `ignore`: expected harmless no-op,
- `invalid`: explicit rejection,
- `onEnter` / `onExit`: phase lifecycle actions,
- `invoke`: phase-owned cancellable command work.

## Conditional-Only case

Unconditional actions compose directly in one `on<Event>` block. `case`
requires a condition and exists when alternative conditions should be named in
code, tests, and graph topology.

Mixing direct actions with `case`, `ignore`, or `invalid` decisions in one
handler is rejected at machine construction because it makes branch selection
ambiguous.

## Read-Only Conditions and Factories

Condition scopes and payload-phase factory scopes expose the current phase,
data, and event but cannot mutate. Actions execute in declared order. A payload
phase factory observes prior `updateData` results.

## Execution Order

For a phase change:

```text
onExit -> selected branch actions -> target phase factory -> onEnter
```

Initial state construction does not execute `onEnter`.

## Decisions

- phase changed -> `Transitioned`,
- accepted without phase change -> `Handled`,
- explicit expected no-op -> `Ignored`,
- no matching rule or explicit rejection -> `Invalid`.

Ignored/invalid results cannot execute accepted work.

## Graph Contract

Topology metadata is produced while the executable definition is built. The
default graph shows phases, transitions, condition labels, command labels, and
entry/exit work. Tests remain authoritative for payloads and no-op decisions.

## Removed Pre-Release Shape

The former fourth Effect type, effect operation, topology labels, and UI output
channel were removed before publication. Historical design steps remain in the
decision and implementation logs.
