## Description

<!-- What does this PR change, and why? Link the relevant planning-doc requirement code
     (e.g. [FIN-004]) or CLAUDE.md section if applicable. -->

Closes #<!-- issue number, if any -->

## Type of change

- [ ] Bug fix
- [ ] New feature / module work
- [ ] Documentation only
- [ ] Refactor / cleanup (no behavior change)
- [ ] CI / build / tooling
- [ ] Other (describe below)

## Area(s) touched

- [ ] Backend — module: <!-- e.g. module/finance -->
- [ ] Backend — kernel: <!-- e.g. security, notification -->
- [ ] Frontend
- [ ] Database migration (new `V{n}__...sql`)
- [ ] Docs (`01`–`08-*.md`, `CLAUDE.md`, `README.md`)

## How was this tested?

<!-- Commands run, manual verification steps, screenshots for UI changes, etc. -->

```bash

```

## Checklist

- [ ] I read `CONTRIBUTING.md` and the relevant planning document(s) for this change.
- [ ] `./mvnw test` passes locally (backend changes) — or `npm run lint && npm run build`
      passes locally (frontend changes).
- [ ] I did not edit an already-released Flyway migration — new changes use a new
      `V{n}__{description}.sql`.
- [ ] I did not add cross-module repository access — cross-module calls go through the other
      module's service (`ModuleBoundaryTest` must stay green).
- [ ] I added/updated tests covering this change.
- [ ] I updated `CLAUDE.md` and/or the relevant planning doc if this change alters documented
      behavior or introduces a deliberate deviation from the spec.
- [ ] No secrets, credentials, or `.env` files are included in this diff.

## Notes for reviewers

<!-- Anything a reviewer should pay special attention to: known trade-offs, follow-ups deferred
     on purpose, deviations from the docs and why. -->
