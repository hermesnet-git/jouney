<!--
Sync Impact Report
- Version change: 2.0.0 → 2.0.1
- Modified principles: none (Governance clarification only, no principle text changed)
- Added sections: Governance — "Documented deviation — automated load/volumetry
  testing" paragraph
- Removed sections: none
- Templates requiring updates:
  - ✅ .specify/templates/plan-template.md (no change needed — this is a
    project-level deviation record, not a new gate)
  - ✅ .specify/templates/spec-template.md (no change needed)
  - ✅ .specify/templates/tasks-template.md (no change needed)
  - ✅ specs/001-workflow-platform-mvp/tasks.md (T080 already marked
    descontinuado, cross-references this entry)
- Follow-up TODOs: none
-->

# Jouney Constitution

## Core Principles

### I. Code Quality

Code MUST be reviewed before merge; no direct commits to `main` bypass review.
Every function and module MUST have a single, clear responsibility — if a change
description needs "and" to explain what it does, split it. Linting and formatting
MUST run clean (zero warnings) before a change is considered done; a project's
configured linter/formatter is the source of truth, not personal style. Dead
code, commented-out blocks, and speculative abstractions (interfaces with one
implementation, config for values that never change) MUST be deleted, not kept
"for later." Public functions, modules, and APIs MUST document their contract
(inputs, outputs, error conditions) where the signature alone doesn't make it
obvious.

**Rationale**: Small, focused, reviewed changes are the cheapest point to catch
defects and design drift. Unreviewed or sprawling changes are where regressions
and unmaintainable abstractions enter the codebase.

### II. User Experience Consistency

Interactions, terminology, and visual patterns MUST be consistent across the
product — the same action (e.g., "delete," "cancel," "save") MUST look and
behave the same way everywhere it appears. New UI MUST reuse existing
components, copy patterns, and error-message conventions rather than inventing
parallel ones. Every user-facing error MUST state what happened and what the
user can do next — no bare stack traces or silent failures reaching the user.
Accessibility basics (keyboard navigation, sufficient color contrast, alt text
for meaningful images) are NON-NEGOTIABLE and MUST NOT be deferred as
"polish."

**Rationale**: Inconsistency forces users to relearn the product in every
screen; accessibility gaps exclude real users and are far cheaper to build in
than to retrofit.

### III. Performance Requirements

Every feature with a user-facing interaction MUST define its performance
budget (e.g., target response time, acceptable payload size) during planning,
not after a user complains. Changes that risk regressing a defined budget
(new dependency on a hot path, unbounded loop over user data, N+1 queries)
MUST be measured before merge, not assumed safe. Performance-sensitive code
MUST prefer the simplest approach that meets the budget — optimize only the
measured bottleneck, not speculatively. Any deliberate performance shortcut
(e.g., an unindexed query, an O(n²) scan) MUST be flagged in code with its
known ceiling and the condition under which it needs revisiting.

**Rationale**: Performance problems are cheap to prevent with a stated budget
and expensive to diagnose once they're live and diffuse across the codebase.

## Governance

This constitution supersedes conflicting team conventions, ad hoc process, or
prior undocumented practice. Any pull request or review MUST verify compliance
with these principles; a reviewer who spots a violation MUST block merge until
it is fixed or explicitly justified.

Amendments require: (1) a documented rationale for the change, (2) an update to
this file with a version bump per semantic versioning (MAJOR for backward-
incompatible principle removal/redefinition, MINOR for a new principle or
materially expanded guidance, PATCH for clarification/wording), and (3)
propagation of the change to any dependent templates or guidance docs in the
same commit. Complexity or deviation from a principle MUST be justified in
writing (e.g., in a plan's Complexity Tracking table) — "it's simpler this way"
is not sufficient on its own; the simpler alternative must be shown insufficient.

**Documented deviation — automated load/volumetry testing (2026-07-19)**: The
`workflow-platform-mvp` feature defers automated load/volumetry test
infrastructure (e.g., a concurrent-execution test validating SC-008's "dezenas
de execuções simultâneas") by explicit user decision, after Docker/Testcontainers
proved unavailable in the development environment and the user chose to run
entirely against a local, non-containerized PostgreSQL instance instead. This
does **not** waive Principle III's requirement to define a performance budget
during planning — the budgets already recorded in `plan.md` and SC-008 in
`spec.md` remain the target. It waives only the requirement for a MUST-measured
automated test proving that budget before merge, until volumetry becomes a
priority for this project. See `specs/001-workflow-platform-mvp/research.md`
and `tasks.md` (T080) for the removed test and full rationale.

**Version**: 2.0.1 | **Ratified**: 2026-07-18 | **Last Amended**: 2026-07-19
