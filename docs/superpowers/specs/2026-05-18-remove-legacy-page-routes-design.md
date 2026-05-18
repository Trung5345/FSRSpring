# Remove Legacy Page Routes Design

## Goal

Stop the Spring Boot backend from serving the legacy static HTML interface during deployment while keeping existing static files untouched.

## Current State

- The modern UI lives in `frontend/`.
- The backend still contains `PageController`, which maps `/`, `/vocabulary`, `/import`, `/quiz`, `/progress`, `/flashcards`, and `/profile` to legacy HTML files under `src/main/resources/static`.
- Static files remain present, but they should no longer be selected by backend page routes.

## Recommended Approach

Delete `PageController` entirely.

This is the smallest complete change because the controller exists only to forward requests into the old HTML interface. Removing it prevents backend-owned page routes from invoking the legacy UI while leaving API controllers, static resources, and the Next.js frontend unchanged.

## Alternatives Considered

1. Keep `PageController` and redirect its routes to the new frontend.
   - Useful only if backend URLs must remain public entry points.
   - Requires a deployment-specific frontend base URL strategy and adds behavior the user did not request.
2. Remove only `/`.
   - Leaves the other legacy page routes active, so it does not fully solve the deployment problem.

## Scope

### In scope

- Remove `src/main/java/com/fsrspring/vocab/controller/PageController.java`.
- Verify no remaining code references depend on that controller.
- Run backend verification after removal.

### Out of scope

- Deleting files in `src/main/resources/static`.
- Changing Next.js frontend routing.
- Changing OAuth redirect behavior or API endpoints.

## Expected Behavior

- Requests previously handled by `PageController` are no longer forwarded to legacy HTML pages by the backend.
- Existing API endpoints continue to work unchanged.
- Static files remain in the repository but are no longer explicitly invoked by backend page routes.

## Verification

- Search for remaining `forward:/...html` references after removal.
- Run the backend test suite or at minimum the relevant Maven verification command to ensure application compilation still succeeds.
