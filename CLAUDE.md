# CLAUDE.md

## Project Identity

- Project: FSRSpring Vocabulary
- Core scheduling algorithm: FSRS (Free Spaced Repetition Scheduler)
- Architecture style: Modular Monolith
- Primary stack: Java 17, Spring Boot, Spring Data JPA, H2 (dev)

## Product Goal

Build a vocabulary learning system where review timing is driven by FSRS, not fixed intervals, to maximize long-term retention with minimal daily workload.

## Architectural Direction (Modular Monolith)

Use one deployable application, but split business logic into explicit domain modules with clear boundaries.

Suggested modules:
- word-management: word CRUD, metadata, import/export
- review-scheduling: FSRS state, next review date, rating ingestion
- learning-session: quiz/learn flow orchestration
- progress-analytics: mastery, streaks, retention metrics
- platform-core: shared config, errors, security, infra adapters

Rules:
- Inter-module communication must go through public service interfaces, not direct repository coupling.
- Keep module internals private by package discipline.
- Controllers never call repositories directly.
- Domain rules live in services/domain classes, not in controllers.

## FSRS-First Requirements

All scheduling decisions must be based on FSRS state transitions.

Minimum FSRS state per item:
- stability
- difficulty
- retrievability (derived)
- reps / lapses
- last_review_at
- next_review_at

Behavioral requirements:
- Rating input must support at least: Again, Hard, Good, Easy.
- next_review_at must be recalculated after each review event.
- Do not hardcode static interval ladders when FSRS state is available.
- Track review logs as immutable events to allow re-simulation or parameter tuning.

## OOP Principles

- Encapsulation: keep scheduling formulas and state transitions inside dedicated FSRS classes.
- Abstraction: expose scheduler via interface (example: SchedulerEngine).
- Composition over inheritance for algorithm strategies and parameter sets.
- Single responsibility per class; avoid controller-service-repository god classes.

## SOLID Guardrails

- S: one reason to change per class.
- O: add new scheduler strategies without rewriting existing call sites.
- L: any SchedulerEngine implementation must preserve contract semantics.
- I: split broad service APIs into focused interfaces per use case.
- D: high-level learning flows depend on scheduler abstraction, not concrete FSRS class.

## Optimizer and Algorithm Evolution

Treat optimizer work as first-class.

Required capabilities:
- Parameter versioning for FSRS weights.
- Ability to re-run scheduling offline on historical review events.
- Feature flags to switch between parameter sets.
- Migration-safe defaults when FSRS model fields evolve.

Optimization directions:
- interval optimization by retention target
- workload smoothing to avoid review spikes
- cold-start heuristics for new users/words
- adaptive difficulty calibration from user outcomes

## Query and Persistence Optimization

Use JPA with explicit performance discipline.

Rules:
- Avoid N+1 queries: use fetch joins, entity graphs, or batch fetching where needed.
- Read paths should use projections/DTO queries for list screens.
- Index heavily used scheduling filters (example: user_id + next_review_at).
- Keep write operations transactional and short.
- Separate command (write) and query (read) concerns when complexity increases.
- Paginate all potentially large result sets.
- Store review history append-only; aggregate asynchronously when possible.

## Update and Extensibility Policy

Any major logic should be update-friendly.

- Prefer backward-compatible schema changes.
- Use migration scripts for every persistent model change.
- Keep FSRS formulas and coefficients configurable.
- Do not bake environment-specific assumptions into business logic.
- Keep module contracts stable; evolve via additive interfaces and versioned DTOs.

## Code Quality and Testing

Minimum test strategy:
- Unit tests for FSRS state transition and interval calculation.
- Contract tests for scheduler interface implementations.
- Integration tests for repository query performance-critical paths.
- End-to-end tests for learn/review flow.

Quality gates for PRs:
- No cross-module repository leakage.
- No scheduling shortcuts bypassing FSRS engine.
- Query plan reviewed for new heavy endpoints.
- Documentation updated when formulas/parameters/schema change.

## Non-Functional Targets

- Deterministic scheduling logic for identical input/state.
- Predictable latency for next-review list queries.
- Clear observability for scheduler decisions (structured logs/metrics).
- Safe concurrent updates for review submissions.

## Team Working Agreement

- Prefer small, reviewable commits by module.
- Explain algorithmic changes with before/after behavior in PR description.
- Keep technical debt visible with TODOs linked to issues.
- Optimize only after measuring, but always design with performance in mind.
