# College ERP System — Project Vision Document

**Version:** 1.0  
**Date:** June 2026  
**Status:** Approved  
**Owner:** Product Team

---

## Table of Contents

1. [Executive Summary](#1-executive-summary)
2. [Problem Statement](#2-problem-statement)
3. [Vision Statement](#3-vision-statement)
4. [Goals and Objectives](#4-goals-and-objectives)
5. [Target Audience](#5-target-audience)
6. [Key Stakeholders](#6-key-stakeholders)
7. [Success Metrics](#7-success-metrics)
8. [Strategic Alignment](#8-strategic-alignment)
9. [High-Level Scope](#9-high-level-scope)
10. [Out of Scope](#10-out-of-scope)
11. [Guiding Principles](#11-guiding-principles)
12. [Risks and Assumptions](#12-risks-and-assumptions)
13. [Timeline Overview](#13-timeline-overview)

---

## 1. Executive Summary

The College ERP System is a comprehensive, full-stack enterprise resource planning platform built specifically for higher education institutions. It consolidates every operational function of a college — academics, student management, finance, library, assets, HR, hostel, and transport — into a single, unified, role-aware platform.

The system is built as a **Java (Spring Boot) backend monolith** with a **React (JavaScript) frontend**, using **PostgreSQL** as the primary database. It follows a **modular architecture** so each domain is independently maintainable while sharing a common platform kernel (authentication, auditing, notifications, file storage).

The platform is designed to run in three deployment environments without any code changes:
- **Cloud (SaaS)** — multi-tenant, managed infrastructure
- **On-Premises** — institution-owned servers with internet access
- **Fully Air-Gapped On-Premises** — zero outbound internet, common in government-affiliated colleges

---

## 2. Problem Statement

### 2.1 Current Reality

Most Indian colleges and universities operate with a deeply fragmented software landscape:

| Function | Typical Current State |
|---|---|
| Student records | Excel spreadsheets or standalone desktop software |
| Fee collection | Manual receipt books or isolated billing software |
| Attendance | Paper registers, sometimes standalone apps |
| Library | Separate standalone LMS or manual cards |
| Assets | Spreadsheets with no audit trail |
| HR & Payroll | Separate HR software or manual salary sheets |
| Examination | Paper-based, manual result compilation |

This fragmentation causes:

- **Data silos** — the same student's data exists in 6+ separate systems with no synchronization
- **No audit trail** — financial transactions, grade changes, and attendance edits happen with no record of who changed what and when
- **Manual reconciliation** — accountants spend days cross-checking fee records against ledger entries
- **Compliance risk** — NAAC/NBA accreditation requires reports that no existing system can generate automatically
- **Lost institutional knowledge** — when a staff member leaves, institutional data leaves with them

### 2.2 The Pain Points in Detail

**For Students:**
- Cannot check their own attendance online
- Fee receipts are paper-only, easily lost
- Library due dates are communicated verbally
- Grade cards are distributed weeks after exams

**For Faculty:**
- Attendance marking is on paper, copied into Excel later
- No way to upload study material centrally
- Leave application requires walking to the HOD's office

**For Administration:**
- Fee defaulter lists must be manually compiled every month
- Asset inventory is unknown until someone physically counts
- Generating an accreditation report takes weeks of data gathering

**For Finance:**
- No double-entry ledger — reconciliation is guesswork
- No scoped access control — the same person collects, approves, and records fees
- Financial year closing is a manual, error-prone process

---

## 3. Vision Statement

> **To build the most complete, trustworthy, and operationally simple college management platform available — one that replaces a college's entire software stack with a single system that every role from student to principal can use confidently, that leaves an immutable audit trail on every action, and that continues to work perfectly whether deployed on a cloud server, a campus server room, or a fully disconnected air-gapped network.**

---

## 4. Goals and Objectives

### 4.1 Primary Goals

| # | Goal | Definition of Done |
|---|---|---|
| G1 | **Consolidate all college operations** | Every function listed in the feature specification is live and used by real users |
| G2 | **Eliminate data silos** | No college operation requires a system outside this ERP |
| G3 | **Full auditability** | Every create, update, financial transaction, and grade change is logged with user + timestamp |
| G4 | **Role-aware access** | Every screen and API enforces the permission model defined in the Roles & Permissions document |
| G5 | **Zero-compromise air-gap** | All features work with zero outbound internet on the air-gapped profile |
| G6 | **Institutional compliance** | The system can auto-generate NAAC, NBA, and statutory report formats |

### 4.2 Secondary Goals

- Reduce fee collection errors to near zero through enforced double-entry accounting
- Reduce time to generate accreditation reports from weeks to hours
- Give students and parents real-time visibility into attendance, grades, and fee dues
- Allow any institution's IT team (not just specialists) to install, operate, and update the system

---

## 5. Target Audience

### 5.1 Primary Users

| Role Group | Description | Size Estimate |
|---|---|---|
| Students | Self-service academic, financial, library portal | 500–15,000 per institution |
| Faculty | Attendance, grading, content upload | 30–500 per institution |
| Administrators (Admin/HOD) | Configuration, approvals, reporting | 5–50 per institution |
| Finance Staff (Accountants) | Fee collection, ledger, payroll | 2–20 per institution |
| Library Staff | Catalog, circulation, barcodes | 1–10 per institution |
| Parents/Guardians | Read-only linked student view | 1–2 per student |

### 5.2 Secondary Users

- **IT Administrators** — system installation, maintenance, user provisioning
- **Auditors** — read-only, time-boxed access to financial records
- **External Recruiters** — placement portal access (limited)
- **Regulatory Bodies** — report recipients (not direct system users)

### 5.3 Target Institution Types

- Engineering colleges (B.Tech, M.Tech)
- Arts, Science, and Commerce colleges
- Polytechnic institutes
- University departments
- Institutions affiliated with state/central universities
- Government and government-aided colleges (air-gapped deployment common here)

---

## 6. Key Stakeholders

| Stakeholder | Role in Project | Primary Interest |
|---|---|---|
| College Principal/Management | Decision maker and buyer | ROI, compliance, reputation |
| Registrar / Admin Office | Primary admin user | Ease of use, data accuracy |
| Head Accountant | Finance module owner | Audit trail, access control, reporting |
| HODs | Academic module users | Faculty and student management |
| IT Department | Deployment and operations | Simple install, minimal maintenance |
| Students | End consumers | Fast, mobile-friendly self-service |
| Parents | Linked consumers | Real-time alerts, fee payment |
| NAAC/NBA Inspectors | Compliance consumers | Report quality and traceability |
| Development Team | Builders | Clear spec, modular architecture |

---

## 7. Success Metrics

### 7.1 Adoption Metrics

| Metric | Target (Year 1) |
|---|---|
| Daily active users / total enrolled students | > 60% |
| Fee transactions processed through the system | > 95% of all collections |
| Attendance marked digitally vs. paper | > 90% |
| Library book issues via barcode scan | 100% |

### 7.2 Operational Metrics

| Metric | Target |
|---|---|
| Fee defaulter list generation time | < 30 seconds (was: 1–2 days) |
| Accreditation report generation time | < 2 hours (was: 2–4 weeks) |
| Attendance shortfall alerts to parents | Automated nightly (was: never) |
| Financial year closing errors | Zero (enforced by system) |

### 7.3 Technical Metrics

| Metric | Target |
|---|---|
| API response time (p95, standard queries) | < 300ms |
| System uptime | > 99.5% |
| Audit log coverage | 100% of write operations |
| Air-gap test pass rate (zero outbound calls) | 100% |

---

## 8. Strategic Alignment

### 8.1 Why Now

- National Education Policy 2020 mandates digital transformation of higher education institutions
- NAAC accreditation criteria increasingly reward data-driven institutional management
- COVID-19 permanently shifted student expectations toward digital-first services
- Government colleges are facing pressure to modernize within air-gapped security constraints

### 8.2 Competitive Differentiation

| Feature | This System | Typical Competitors |
|---|---|---|
| Air-gapped deployment | Native, first-class | Requires cloud connectivity |
| Library copy-level custody chain | Full chain-of-custody per physical copy | Title-level tracking only |
| Finance access control granularity | Scoped per fee-head, department, amount threshold | Role-level only |
| Audit trail immutability | DB-trigger enforced, not just application-level | Application-level logging |
| Single codebase, three deployment modes | Yes — same JAR, different config | Separate editions |

---

## 9. High-Level Scope

### Modules Included (Phase 1 — MVP)

1. Authentication & Role-Based Access Control
2. Academic Management (Departments, Courses, Batches, Sections, Subjects)
3. Student Management (Profiles, Enrollment History, Documents)
4. Attendance Management (Session-based, multi-mode capture)
5. Examination & Results
6. Finance & Accounting (Fees, Double-Entry Ledger, Payroll)
7. Library Management (Catalog, Barcode Generation, Custody Chain, Fines)
8. Asset Management (Registry, Transfer, Maintenance)
9. HR & Leave Management
10. Communication (Notices, Helpdesk)
11. Notifications (In-App SSE, Email, SMS)
12. Admin & Audit (User Management, Audit Log)

### Modules Included (Phase 2)

13. Hostel & Room Management
14. Transport Management
15. Admissions & Merit Processing
16. Placement & Career Services
17. Alumni Management

---

## 10. Out of Scope

- **Learning Management System (LMS)** — deep online learning (video hosting, live classes) is out of scope; integration hooks will be provided
- **Payment gateway** — the system supports recording payments from any mode; integration with specific gateways (Razorpay, PayU) is a configuration concern, not a core feature
- **Biometric hardware firmware** — the system integrates with biometric devices via standard API; firmware development is not in scope
- **Mobile native app** — the React frontend is responsive and mobile-first; a dedicated iOS/Android native app is Phase 3
- **Multi-campus federation** — Phase 1 is single-institution; multi-campus under one admin is Phase 2 extension

---

## 11. Guiding Principles

### P1 — DRY: Don't Repeat Yourself
Every shared function (authentication, auditing, file storage, barcode generation, notifications) is written once in the platform kernel and used across all modules. No module re-implements a concern that the platform already handles.

### P2 — Modular, Not Monolithic in spirit
The codebase is a single deployable unit but each module owns its own domain — its entities, repositories, services, and controllers. Modules communicate only through published service interfaces, never through direct database queries across module boundaries.

### P3 — Audit Everything
Every state change that matters to an institution (financial transaction, grade, attendance edit, permission grant, asset transfer, book custody) must be recorded with who, what, when, and the before/after state. This is non-negotiable.

### P4 — Air-Gap First
Every feature is designed to work without internet connectivity. External dependencies (email, SMS, maps, CDNs) are treated as optional enhancements, not requirements. Degradation when offline must be graceful and explicit, never a silent failure.

### P5 — Append-Only for Audit-Critical Data
Financial ledger entries, library custody logs, asset custody logs, and the system audit log are written once and never modified or deleted. Corrections are new entries. This is enforced at the database level with triggers, not just the application level.

### P6 — Security by Default
Least-privilege access, JWT-based stateless authentication, bcrypt password hashing, and scope-based permission grants are the baseline. No feature ships without security review.

---

## 12. Risks and Assumptions

### 12.1 Risks

| Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|
| Institution's existing data is too dirty to migrate | High | High | Provide import templates and a data-cleaning wizard |
| Faculty resistance to digital attendance | Medium | High | Keep UI simple; support QR and biometric as alternatives to manual |
| Air-gapped network has no NTP | Low | Medium | Support manual clock setting; document the requirement |
| PostgreSQL version not available in air-gapped network | Low | High | Include PostgreSQL offline installer in the air-gap bundle |
| Scope creep from institution-specific customisation requests | High | Medium | Define a clear configuration API; avoid hardcoding institution logic |

### 12.2 Assumptions

- The institution has at least one person who can run Docker Compose commands
- PostgreSQL 15+ can be installed on the target server
- Students and faculty have access to a modern web browser (Chrome, Firefox, Edge)
- For SMS notifications: the institution has a commercial SMS gateway account (not required for core function)
- For air-gapped installs: the institution has an approved process for transferring files across the air gap (USB, approved media)

---

## 13. Timeline Overview

| Phase | Scope | Target Duration |
|---|---|---|
| Phase 0 — Foundation | Auth, RBAC, Academic structure, Student enrollment | 6 weeks |
| Phase 1 — Core Academic | Attendance, Examination, Results | 6 weeks |
| Phase 1 — Finance | Fee collection, Ledger, Accountant grants | 8 weeks |
| Phase 1 — Library | Catalog, Barcode, Circulation, Fines | 4 weeks |
| Phase 1 — Asset & HR | Asset registry, Transfer, Leave management | 4 weeks |
| Phase 1 — Polish | Notifications, Audit log UI, Admin tools, Reporting | 4 weeks |
| Phase 2 | Hostel, Transport, Admissions, Placement | 12 weeks |
| Phase 3 | Native mobile app, Multi-campus, Alumni | TBD |

---

*Document Owner: Product Team*  
*Last Updated: June 2026*  
*Next Review: Before Phase 1 Kickoff*
