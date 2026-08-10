# Security Policy

College ERP handles sensitive institutional data — student records, financial transactions,
authentication credentials — so security reports are taken seriously and prioritized over
ordinary bug reports.

## Reporting a vulnerability

**Do not open a public GitHub issue for a security vulnerability.**

Instead, please report it privately using one of the following:

- GitHub's [private vulnerability reporting](https://docs.github.com/en/code-security/security-advisories/guidance-on-reporting-and-writing/privately-reporting-a-security-vulnerability)
  feature on this repository (**Security → Report a vulnerability**), if enabled, or
- Email the maintainers directly with details. Include:
  - A description of the vulnerability and its potential impact
  - Steps to reproduce (a minimal proof-of-concept if possible)
  - The affected module/endpoint/file, if known
  - Any suggested remediation, if you have one

Please give us a reasonable amount of time to respond and address the issue before any public
disclosure.

## What to expect

- **Acknowledgement**: within a few business days of your report.
- **Assessment**: we'll confirm whether it's a valid issue and its severity.
- **Fix**: timeline depends on severity — critical authentication/authorization/data-exposure
  issues are prioritized above all other work.
- You'll be credited in the fix's changelog/release notes, unless you'd prefer to remain
  anonymous.

## Scope

Given the current state of the project (see `CLAUDE.md` for exactly what's built), the areas
most worth scrutiny are:

- Authentication/JWT handling (`security/`, `module/auth`)
- The three-layer RBAC permission evaluation (`security/rbac/`)
- Multi-tenancy isolation — Postgres Row-Level Security and `TenantContext`/
  `TenantAwareDataSource` (`tenant/` kernel, retrofitted so far only onto `auth`/`academic`/
  `student`)
- Append-only/immutable data integrity (`audit_log`, `ledger_entries`, `asset_custody_log`,
  `library_custody_log`) and their DB-level triggers
- File storage (`storage/` kernel) and any path-handling around `fileId`
- SQL injection / unsafe query construction anywhere JPQL or native queries are used

## Supported versions

This project has not yet reached a `1.0` release or a formal versioning/support policy. Security
fixes are applied to the `main` branch; there are no maintained release branches at this time.
