# College ERP System — API Documentation

**Version:** 1.0  
**Base URL:** `https://erp.college.edu/api/v1`  
**Auth:** Bearer JWT in `Authorization` header  
**Format:** JSON (snake_case keys)  
**Date:** June 2026

---

## Table of Contents

1. [API Conventions](#1-api-conventions)
2. [Authentication](#2-authentication)
3. [Academic Management](#3-academic-management)
4. [Student Management](#4-student-management)
5. [Attendance](#5-attendance)
6. [Examination](#6-examination)
7. [Finance](#7-finance)
8. [Library](#8-library)
9. [Asset Management](#9-asset-management)
10. [HR & Leave](#10-hr--leave)
11. [Communication](#11-communication)
12. [Notifications](#12-notifications)
13. [Admin & Audit](#13-admin--audit)
14. [File Access](#14-file-access)
15. [Error Reference](#15-error-reference)

---

## 1. API Conventions

### 1.1 URL Structure

```
/api/v1/{module}/{resource}
/api/v1/{module}/{resource}/{id}
/api/v1/{module}/{resource}/{id}/{sub-resource}
```

### 1.2 Standard Response Envelope

All successful responses are wrapped in:

```json
{
  "success": true,
  "message": "Optional human-readable message",
  "data": { ... },
  "timestamp": "2026-06-01T10:30:00Z"
}
```

### 1.3 Paginated Response

```json
{
  "success": true,
  "data": {
    "content": [ ... ],
    "page": 0,
    "size": 20,
    "total_elements": 142,
    "total_pages": 8,
    "first": true,
    "last": false
  }
}
```

Query parameters for pagination: `?page=0&size=20`

### 1.4 Error Response (RFC 7807)

```json
{
  "type": "https://erp.college.edu/errors/validation",
  "title": "Validation Failed",
  "status": 400,
  "detail": "One or more fields failed validation",
  "instance": "/api/v1/students",
  "request_id": "a1b2c3d4-...",
  "timestamp": "2026-06-01T10:30:00Z",
  "field_errors": {
    "enrollment_number": ["must not be blank"],
    "personal_email": ["must be a valid email"]
  }
}
```

### 1.5 HTTP Status Codes

| Status | Meaning |
|---|---|
| 200 | OK — read or update succeeded |
| 201 | Created — resource created |
| 204 | No Content — delete or action with no body |
| 400 | Bad Request — validation failure |
| 401 | Unauthorized — missing or invalid token |
| 403 | Forbidden — authenticated but insufficient permission |
| 404 | Not Found — resource does not exist |
| 409 | Conflict — duplicate resource |
| 422 | Unprocessable Entity — business rule violation |
| 500 | Internal Server Error — unexpected server error |

### 1.6 Idempotency

Financial write endpoints accept an optional `Idempotency-Key: <uuid>` header. If the same key is seen twice, the second call returns the original response without re-executing the operation.

### 1.7 Request ID

Every response includes `X-Request-Id` header for log correlation. Include the same header in requests to trace them end-to-end.

---

## 2. Authentication

### POST `/auth/login`
Authenticate and receive tokens.

**Auth required:** No

**Request body:**
```json
{
  "identifier": "jdoe",
  "password": "Secret@123",
  "totp_code": "123456"
}
```
> `totp_code` required only if 2FA is enabled for the account.

**Response 200:**
```json
{
  "success": true,
  "data": {
    "user_id": "550e8400-e29b-41d4-a716-446655440000",
    "username": "jdoe",
    "email": "jdoe@college.edu",
    "full_name": "John Doe",
    "roles": ["FACULTY"],
    "access_token": "eyJhbGciOiJIUzI1NiJ9...",
    "access_token_expires_in": 900000,
    "must_change_password": false,
    "totp_required": false
  }
}
```

**Response 401** — Invalid credentials  
**Response 423** — Account locked

---

### POST `/auth/refresh`
Exchange a refresh token for a new access token.

**Auth required:** No

**Request body:**
```json
{ "refresh_token": "raw-refresh-token-string" }
```

**Response 200:**
```json
{
  "success": true,
  "data": {
    "access_token": "eyJhbGciOiJIUzI1NiJ9...",
    "expires_in": 900000
  }
}
```

---

### POST `/auth/logout`
Revoke all refresh tokens for the current user.

**Auth required:** Yes

**Response 200:**
```json
{ "success": true, "message": "Logged out successfully" }
```

---

### POST `/auth/change-password`
Change the authenticated user's password.

**Auth required:** Yes

**Request body:**
```json
{
  "current_password": "OldPass@1",
  "new_password": "NewPass@1",
  "confirm_new_password": "NewPass@1"
}
```

**Response 200:** `{ "success": true, "message": "Password changed successfully" }`  
**Response 400** — Passwords don't match  
**Response 422** — Current password incorrect

---

## 3. Academic Management

### Departments

#### GET `/departments`
List all departments (paginated).

**Auth:** Authenticated  
**Query:** `?page=0&size=20`

**Response 200:**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": "...",
        "name": "Computer Science",
        "code": "CSE",
        "hod_user_id": "...",
        "contact_email": "cse@college.edu",
        "is_active": true
      }
    ],
    "page": 0, "size": 20, "total_elements": 8, "total_pages": 1
  }
}
```

#### GET `/departments/active`
Get all active departments (for dropdowns).

**Auth:** Authenticated  
**Response:** Array of department objects (no pagination)

#### GET `/departments/{id}`
Get a single department.

**Auth:** Authenticated

#### POST `/departments`
Create a new department.

**Auth:** `SUPER_ADMIN`, `ADMIN`

**Request body:**
```json
{
  "name": "Computer Science",
  "code": "CSE",
  "description": "...",
  "hod_user_id": "...",
  "contact_email": "cse@college.edu",
  "contact_phone": "+91-9876543210"
}
```

**Response 201:** Department object  
**Response 409** — Duplicate code

#### PUT `/departments/{id}`
Update a department.

**Auth:** `SUPER_ADMIN`, `ADMIN`  
**Body:** Same as POST  
**Response 200:** Updated department object

#### PATCH `/departments/{id}/deactivate`
Deactivate a department.

**Auth:** `SUPER_ADMIN`, `ADMIN`  
**Response 200:** `{ "success": true, "message": "Department deactivated" }`

---

### Courses

#### GET `/courses`
List all courses (paginated).

**Auth:** Authenticated  
**Query:** `?page=0&size=20`

#### GET `/courses/{id}`
Get a course by ID.

**Auth:** Authenticated

#### GET `/courses/by-department/{departmentId}`
Get all courses in a department.

**Auth:** Authenticated

#### POST `/courses`
Create a course.

**Auth:** `SUPER_ADMIN`, `ADMIN`

**Request body:**
```json
{
  "department_id": "...",
  "name": "Bachelor of Technology in Computer Science",
  "code": "BTCSE",
  "level": "UG",
  "duration_years": 4,
  "total_credits": 160,
  "intake_capacity": 60,
  "description": "..."
}
```

**`level` values:** `UG` | `PG` | `DIPLOMA` | `PHD` | `CERTIFICATE`

---

### Batches

#### GET `/batches/{id}`
Get a batch by ID.

**Auth:** Authenticated

#### GET `/batches/by-course/{courseId}`
List batches for a course.

**Auth:** Authenticated

#### POST `/batches`
Create a batch.

**Auth:** `SUPER_ADMIN`, `ADMIN`

**Request body:**
```json
{
  "course_id": "...",
  "academic_year_id": "...",
  "label": "B.Tech CSE 2024-28",
  "intake_year": 2024
}
```

#### POST `/batches/{id}/promote-term`
Advance the batch's current term number by 1.

**Auth:** `SUPER_ADMIN`, `ADMIN`  
**Response 200:** `{ "success": true, "message": "Batch promoted to next term" }`  
**Response 422** — Already at final term

#### GET `/batches/{id}/sections`
List all sections in a batch.

**Auth:** Authenticated

#### POST `/batches/{id}/sections`
Add a section to a batch.

**Auth:** `SUPER_ADMIN`, `ADMIN`, `HOD`

**Request body:**
```json
{ "name": "A", "capacity": 60 }
```

**Response 201:** Section object

---

## 4. Student Management

### POST `/students`
Enroll a new student.

**Auth:** `SUPER_ADMIN`, `ADMIN`, `HOD`

**Request body:**
```json
{
  "user_id": "...",
  "enrollment_number": "2024CSE001",
  "first_name": "Arjun",
  "last_name": "Kumar",
  "personal_email": "arjun@gmail.com",
  "phone": "+91-9876543210",
  "date_of_birth": "2004-03-15",
  "gender": "MALE",
  "blood_group": "O+",
  "aadhar_number": "123456789012",
  "category": "GENERAL",
  "address": "123, Main Street",
  "city": "Chennai",
  "state": "Tamil Nadu",
  "pincode": "600001",
  "batch_id": "...",
  "section_id": "..."
}
```

**`gender` values:** `MALE` | `FEMALE` | `OTHER` | `PREFER_NOT_TO_SAY`  
**`category` values:** `GENERAL` | `OBC` | `SC` | `ST` | `EWS`

**Response 201:** Student object

---

### GET `/students/{id}`
Get a student by ID.

**Auth:** Admin/HOD/Faculty roles OR own profile (Student)

---

### GET `/students/me`
Get own student profile (student self-service).

**Auth:** `STUDENT`

---

### GET `/students/enrollment/{number}`
Find a student by enrollment number.

**Auth:** Admin, Finance, Library, Faculty roles

---

### GET `/students/by-section/{sectionId}`
List students in a section (paginated).

**Auth:** `SUPER_ADMIN`, `ADMIN`, `HOD`, `FACULTY`, `EXAM_CELL_OFFICER`  
**Query:** `?page=0&size=50`

---

### GET `/students/by-batch/{batchId}`
List all students in a batch (paginated).

**Auth:** `SUPER_ADMIN`, `ADMIN`, `HOD`, `EXAM_CELL_OFFICER`

---

### GET `/students/{id}/enrollment`
Get a student's current enrollment.

**Auth:** Admin/HOD/Faculty OR own (Student)

**Response 200:**
```json
{
  "success": true,
  "data": {
    "id": "...",
    "student_id": "...",
    "batch_id": "...",
    "section_id": "...",
    "term_id": "...",
    "enrollment_date": "2024-08-01",
    "status": "ACTIVE",
    "is_current": true
  }
}
```

---

### GET `/students/{id}/enrollment/history`
Full enrollment history (all batch/section changes).

**Auth:** `SUPER_ADMIN`, `ADMIN`, `HOD`

---

### POST `/students/{id}/transfer-section`
Transfer a student to a different section.

**Auth:** `SUPER_ADMIN`, `ADMIN`, `HOD`

**Request body:**
```json
{
  "new_section_id": "...",
  "remarks": "Student requested transfer due to schedule conflict"
}
```

**Response 200:** New enrollment object

---

### PUT `/students/{id}`
Update student profile details.

**Auth:** Admin/HOD OR own profile (Student — limited fields)

**Request body:**
```json
{
  "first_name": "Arjun",
  "last_name": "Kumar",
  "phone": "+91-9876543210",
  "blood_group": "O+",
  "address": "456 New Street",
  "city": "Chennai",
  "state": "Tamil Nadu",
  "pincode": "600002"
}
```

---

### PATCH `/students/{id}/graduate`
Mark a student as graduated.

**Auth:** `SUPER_ADMIN`, `ADMIN`

---

## 5. Attendance

### POST `/attendance/sessions`
Open a new attendance session.

**Auth:** `SUPER_ADMIN`, `ADMIN`, `HOD`, `FACULTY`, `LAB_ASSISTANT`

**Request body:**
```json
{
  "offering_id": "...",
  "section_id": "...",
  "session_date": "2026-06-15"
}
```

**Response 201:** AttendanceSession object  
**Response 409** — Session already exists for this offering/section/date

---

### POST `/attendance/sessions/{sessionId}/mark`
Mark attendance for all students in a session.

**Auth:** `SUPER_ADMIN`, `ADMIN`, `HOD`, `FACULTY`, `LAB_ASSISTANT`

**Request body:** Map of `student_id` → `status`
```json
{
  "550e8400-...": "PRESENT",
  "660f9500-...": "ABSENT",
  "770a0600-...": "LATE"
}
```

**`status` values:** `PRESENT` | `ABSENT` | `LATE` | `EXCUSED` | `HOLIDAY`

**Response 200:** `{ "success": true, "message": "Attendance marked successfully" }`

> This endpoint is idempotent — calling it multiple times replaces the records for that session.

---

### POST `/attendance/sessions/{sessionId}/submit`
Submit (lock for editing) an attendance session.

**Auth:** Session's assigned faculty

**Response 200:** `{ "success": true, "message": "Attendance submitted" }`  
**Response 422** — Session is empty or already submitted

---

### GET `/attendance/summary/student/{studentId}/offering/{offeringId}`
Get attendance summary for a student in a subject.

**Auth:** Admin/HOD/Faculty OR own (Student)

**Response 200:**
```json
{
  "success": true,
  "data": {
    "student_id": "...",
    "offering_id": "...",
    "total_classes": 45,
    "present": 38,
    "absent": 7,
    "attendance_percentage": 84.4
  }
}
```

---

### POST `/attendance/condonation`
Apply for attendance condonation.

**Auth:** `STUDENT`

**Request body:**
```json
{
  "offering_id": "...",
  "term_id": "...",
  "reason": "Medical emergency — hospitalized for 10 days"
}
```

**Response 201:** CondonationRequest object  
**Response 422** — Attendance is already above minimum threshold

---

### PATCH `/attendance/condonation/{requestId}`
Approve or reject a condonation request.

**Auth:** `SUPER_ADMIN`, `ADMIN`, `HOD`, `EXAM_CELL_OFFICER`

**Request body:**
```json
{
  "decision": "APPROVED",
  "remarks": "Medical certificate verified"
}
```

**`decision` values:** `APPROVED` | `REJECTED`

---

## 6. Examination

### POST `/exams`
Schedule an exam.

**Auth:** `SUPER_ADMIN`, `ADMIN`, `EXAM_CELL_OFFICER`

**Request body:**
```json
{
  "name": "End Term Examination — Semester III",
  "exam_type": "END_TERM",
  "batch_id": "...",
  "term_id": "...",
  "start_date": "2026-11-01",
  "end_date": "2026-11-15"
}
```

**`exam_type` values:** `INTERNAL_1` | `INTERNAL_2` | `MID_TERM` | `END_TERM` | `PRACTICAL` | `VIVA` | `PROJECT`

---

### POST `/exams/{examId}/hall-tickets/generate`
Generate hall tickets for all eligible students.

**Auth:** `SUPER_ADMIN`, `ADMIN`, `EXAM_CELL_OFFICER`

**Response 200:**
```json
{
  "success": true,
  "data": {
    "generated": 58,
    "ineligible": 2,
    "ineligible_students": [
      { "student_id": "...", "enrollment_number": "2024CSE003", "reason": "Attendance below 75%" }
    ]
  }
}
```

---

### GET `/exams/{examId}/hall-tickets/{studentId}`
Download a student's hall ticket.

**Auth:** Admin/Exam Cell OR own (Student)

**Response 200:** Hall ticket object with student details, exam schedule, seating info

---

### POST `/exams/marks`
Enter marks for a student in a subject.

**Auth:** `FACULTY` (assigned to this offering), `EXAM_CELL_OFFICER`, `SUPER_ADMIN`

**Request body:**
```json
{
  "exam_id": "...",
  "offering_id": "...",
  "student_id": "...",
  "internal_marks": 36.5,
  "external_marks": 54.0
}
```

**Response 201:** MarksEntry object (status: DRAFT)

---

### PATCH `/exams/marks/{marksEntryId}/submit`
Faculty submit marks (lock from further self-edit).

**Auth:** Marks entry's `entered_by` faculty

**Response 200:** Updated MarksEntry (status: SUBMITTED)

---

### PATCH `/exams/marks/{examId}/moderate`
HOD moderates and approves all submitted marks for an exam.

**Auth:** `HOD`, `EXAM_CELL_OFFICER`, `SUPER_ADMIN`

**Response 200:** `{ "success": true, "data": { "moderated_count": 58 } }`

---

### POST `/exams/{examId}/results/publish`
Publish results for an exam.

**Auth:** `EXAM_CELL_OFFICER`, `ADMIN`, `SUPER_ADMIN`

**Response 200:** `{ "success": true, "data": { "published_count": 58 } }`  
**Response 422** — Some marks are not yet moderated

---

### GET `/exams/results/student/{studentId}/term/{termId}`
Get a student's result for a term.

**Auth:** Admin/HOD/Exam Cell OR own (Student)

**Response 200:**
```json
{
  "success": true,
  "data": {
    "student_id": "...",
    "term_id": "...",
    "sgpa": 8.42,
    "cgpa": 8.15,
    "total_credits_earned": 24,
    "result_status": "PASS",
    "subjects": [
      {
        "subject_code": "CS301",
        "subject_name": "Data Structures",
        "internal_marks": 36,
        "external_marks": 54,
        "total": 90,
        "grade": "O",
        "grade_points": 10.0,
        "is_pass": true
      }
    ]
  }
}
```

---

### POST `/exams/revaluation`
Apply for revaluation of a marks entry.

**Auth:** `STUDENT`

**Request body:**
```json
{
  "marks_entry_id": "...",
  "reason": "I believe my answer to Q3 was incorrectly evaluated"
}
```

---

## 7. Finance

### Fee Structure

#### GET `/finance/fee-structures/course/{courseId}`
Get all fee structures for a course.

**Auth:** `HEAD_ACCOUNTANT`, `ACCOUNTANT`, `ADMIN`

#### POST `/finance/fee-structures`
Create a fee structure entry.

**Auth:** `HEAD_ACCOUNTANT`

**Request body:**
```json
{
  "course_id": "...",
  "batch_id": null,
  "fee_head_id": "...",
  "term_number": 1,
  "amount": 45000.00,
  "due_date_offset_days": 30,
  "late_fee_per_day": 50.00
}
```

---

### Fee Invoices

#### POST `/finance/fees/invoices/generate`
Generate a fee invoice for a student.

**Auth:** `HEAD_ACCOUNTANT`, `ACCOUNTANT`

**Request body:**
```json
{
  "student_id": "...",
  "course_id": "...",
  "batch_id": "...",
  "academic_year_id": "...",
  "term_number": 1
}
```

**Response 201:** FeeInvoice object  
**Response 409** — Invoice already exists for this student/batch/term  
**Response 422** — No fee structure defined

---

#### GET `/finance/fees/students/{studentId}/invoices`
Get all invoices for a student.

**Auth:** Admin/Finance roles OR own (Student/Parent)

---

#### GET `/finance/fees/students/{studentId}/outstanding`
Get unpaid/partial invoices.

**Auth:** Finance roles OR Student/Parent

---

### Fee Payments

#### POST `/finance/fees/invoices/{invoiceId}/collect`

**Auth:** `HEAD_ACCOUNTANT`, `ACCOUNTANT`, `FEE_COLLECTION_CLERK`  
Scope: accountant may be restricted to specific departments/fee-heads

**Request body:**
```json
{
  "amount": 25000.00,
  "payment_mode": "ONLINE_UPI",
  "transaction_reference": "TXN20260601001"
}
```

**`payment_mode` values:** `CASH` | `CHEQUE` | `DEMAND_DRAFT` | `ONLINE_UPI` | `ONLINE_CARD` | `ONLINE_NET_BANKING` | `NEFT` | `RTGS`

**Response 201:**
```json
{
  "success": true,
  "message": "Payment recorded — receipt: RCP-2026-001042",
  "data": {
    "id": "...",
    "receipt_number": "RCP-2026-001042",
    "amount": 25000.00,
    "payment_mode": "ONLINE_UPI",
    "payment_timestamp": "2026-06-01T10:30:00Z",
    "status": "SUCCESS"
  }
}
```

**Response 422** — Invoice already paid / amount exceeds balance / invoice cancelled

---

#### GET `/finance/fees/invoices/{invoiceId}/payments`
Get all payments against an invoice.

**Auth:** Finance roles, `AUDITOR`

---

### Ledger

#### GET `/finance/ledger`
Browse ledger entries (paginated).

**Auth:** `HEAD_ACCOUNTANT`, `AUDITOR`  
**Query:** `?page=0&size=50&from=2026-04-01&to=2026-03-31`

#### POST `/finance/ledger/manual-entry`
Post a manual journal entry.

**Auth:** `HEAD_ACCOUNTANT` only

**Request body:**
```json
{
  "account_head_id": "...",
  "entry_type": "DEBIT",
  "amount": 15000.00,
  "description": "Correction entry for over-collection on invoice INV-2026-000042",
  "reference_type": "MANUAL_CORRECTION",
  "reference_id": "INV-2026-000042"
}
```

---

### Financial Year

#### GET `/finance/financial-years/current`
Get the current open financial year.

**Auth:** Finance roles

#### POST `/finance/financial-years`
Create a new financial year.

**Auth:** `HEAD_ACCOUNTANT`, `SUPER_ADMIN`

#### POST `/finance/financial-years/{id}/close`
Initiate financial year close (requires dual approval).

**Auth:** `HEAD_ACCOUNTANT` or `ADMIN`

> The close only executes after both Head Accountant AND Admin have called this endpoint. The first call sets status to `CLOSING_PENDING`. The second call completes the close.

---

### Accountant Permission Grants

#### POST `/finance/accountant-grants`
Grant a scoped permission to a sub-accountant.

**Auth:** `HEAD_ACCOUNTANT`

**Request body:**
```json
{
  "grantee_user_id": "...",
  "permission_key": "FINANCE.FEE_COLLECTION",
  "scope_department_id": "...",
  "scope_fee_head_code": "TUITION",
  "amount_threshold": 50000.00,
  "effective_from": "2026-06-01T00:00:00Z",
  "effective_until": "2026-12-31T23:59:59Z"
}
```

**Response 201:** AccountantPermissionGrant object

#### GET `/finance/accountant-grants/my-grants`
List all grants issued by the current user.

**Auth:** `HEAD_ACCOUNTANT`

#### DELETE `/finance/accountant-grants/{id}`
Revoke a grant immediately.

**Auth:** `HEAD_ACCOUNTANT`

---

## 8. Library

### Catalog

#### GET `/library/catalog/titles`
Search or browse the book catalog.

**Auth:** Authenticated  
**Query:** `?q=clean+code&page=0&size=20`

**Response 200:** Paginated list of BookTitle objects

#### GET `/library/catalog/titles/{id}`
Get a book title by ID.

**Auth:** Authenticated

#### POST `/library/catalog/titles`
Add a new book title.

**Auth:** `CHIEF_LIBRARIAN`

**Request body:**
```json
{
  "title": "Clean Code",
  "author": "Robert C. Martin",
  "isbn": "978-0132350884",
  "edition": "1st",
  "publisher": "Prentice Hall",
  "publication_year": 2008,
  "subject_classification": "005.1",
  "language": "English",
  "description": "...",
  "department_id": "..."
}
```

**Response 201:** BookTitle object

#### POST `/library/catalog/titles/{titleId}/copies`
Add a physical copy to a title (auto-generates barcode).

**Auth:** `CHIEF_LIBRARIAN`, `LIBRARY_ASSISTANT`

**Request body:**
```json
{
  "shelf_location": "A3",
  "rack_number": "R-12"
}
```

**Response 201:**
```json
{
  "success": true,
  "message": "Copy added — barcode: LIB-2026-001042",
  "data": {
    "id": "...",
    "barcode": "LIB-2026-001042",
    "accession_number": "2026-001042",
    "status": "AVAILABLE",
    "condition": "GOOD"
  }
}
```

#### GET `/library/catalog/titles/{titleId}/copies`
List all physical copies of a title.

**Auth:** Library staff

#### GET `/library/catalog/copies/barcode/{barcode}`
Look up a copy by barcode.

**Auth:** Library staff

#### GET `/library/catalog/copies/{copyId}/barcode-image`
Download barcode image for a copy (PNG).

**Auth:** Library staff  
**Response:** `Content-Type: image/png`

---

### Circulation

#### POST `/library/circulation/issue`
Issue a book by scanning barcode + borrower ID.

**Auth:** `CHIEF_LIBRARIAN`, `LIBRARY_ASSISTANT`

**Request body:**
```json
{
  "barcode": "LIB-2026-001042",
  "borrower_user_id": "...",
  "borrower_name": "Arjun Kumar",
  "borrower_type": "STUDENT"
}
```

**`borrower_type` values:** `STUDENT` | `FACULTY` | `STAFF`

**Response 201:** LibraryCustodyLog object  
**Response 422** — Copy not available / borrower has overdue books / loan limit reached

---

#### POST `/library/circulation/return`
Process a book return.

**Auth:** `CHIEF_LIBRARIAN`, `LIBRARY_ASSISTANT`

**Request body:**
```json
{
  "barcode": "LIB-2026-001042",
  "condition_on_return": "GOOD"
}
```

**`condition_on_return` values:** `EXCELLENT` | `GOOD` | `FAIR` | `POOR` | `DAMAGED`

**Response 200:** LibraryCustodyLog object (with fine_amount if overdue)

---

#### POST `/library/circulation/renew`
Renew a currently issued book.

**Auth:** `CHIEF_LIBRARIAN`, `LIBRARY_ASSISTANT`

**Request body:**
```json
{ "barcode": "LIB-2026-001042" }
```

**Response 200:** LibraryCustodyLog object (with updated due_at)  
**Response 422** — Book is overdue / title has a reservation

---

#### GET `/library/circulation/copies/{copyId}/custody-chain`
Full chain-of-custody for a specific copy.

**Auth:** `CHIEF_LIBRARIAN`, `LIBRARY_ASSISTANT`

**Response 200:**
```json
{
  "success": true,
  "data": [
    {
      "id": "...",
      "borrower_name": "Arjun Kumar",
      "borrower_type": "STUDENT",
      "issued_at": "2026-03-01T09:00:00Z",
      "due_at": "2026-03-15T09:00:00Z",
      "returned_at": "2026-03-12T14:30:00Z",
      "condition_on_issue": "GOOD",
      "condition_on_return": "GOOD",
      "fine_amount": null
    },
    {
      "id": "...",
      "borrower_name": "Priya Singh",
      "borrower_type": "FACULTY",
      "issued_at": "2026-04-10T10:00:00Z",
      "due_at": "2026-05-10T10:00:00Z",
      "returned_at": null,
      "condition_on_issue": "GOOD"
    }
  ]
}
```

---

#### PATCH `/library/catalog/copies/{copyId}/mark-lost`
Mark a copy as lost.

**Auth:** `CHIEF_LIBRARIAN`

---

## 9. Asset Management

### POST `/assets`
Register a new asset (auto-generates barcode tag).

**Auth:** `SUPER_ADMIN`, `ASSET_MANAGER`

**Request body:**
```json
{
  "name": "Dell OptiPlex 7090",
  "description": "Desktop computer for faculty use",
  "category_id": "...",
  "department_id": "...",
  "room_location": "Lab 3, Room 201",
  "purchase_date": "2026-01-15",
  "purchase_cost": 65000.00,
  "vendor_id": "...",
  "warranty_expiry": "2029-01-14"
}
```

**Response 201:**
```json
{
  "success": true,
  "message": "Asset registered",
  "data": {
    "id": "...",
    "asset_tag": "COM-001042",
    "name": "Dell OptiPlex 7090",
    "status": "ACTIVE"
  }
}
```

---

### GET `/assets/{id}`
Get asset by ID.

**Auth:** Admin/HOD/Asset Manager/Maintenance

---

### GET `/assets/tag/{tag}`
Look up asset by barcode/tag scan.

**Auth:** `ASSET_MANAGER`, `MAINTENANCE_STAFF`

---

### GET `/assets/by-department/{departmentId}`
List assets in a department (paginated).

**Auth:** Admin/HOD/Asset Manager

---

### GET `/assets/{id}/barcode-image`
Download asset barcode PNG.

**Auth:** `ASSET_MANAGER`  
**Response:** `Content-Type: image/png`

---

### POST `/assets/{id}/transfer`
Transfer an asset to another department.

**Auth:** `SUPER_ADMIN`, `ASSET_MANAGER`

**Request body:**
```json
{
  "to_department_id": "...",
  "to_user_id": "...",
  "to_location": "Room 305, Block B",
  "reason": "New faculty assignment requires this equipment"
}
```

**Response 200:** AssetCustodyLog object

---

### GET `/assets/{id}/custody-chain`
Full custody history for an asset.

**Auth:** Admin/Asset Manager

**Response 200:** Array of AssetCustodyLog objects (oldest first)

---

### POST `/assets/{id}/maintenance-tickets`
Raise a maintenance ticket.

**Auth:** Any authenticated user

**Request body:**
```json
{
  "title": "Projector not displaying correctly",
  "description": "Image is distorted on the left side",
  "priority": "HIGH"
}
```

**`priority` values:** `LOW` | `MEDIUM` | `HIGH` | `CRITICAL`

**Response 201:** MaintenanceTicket object

---

### PATCH `/assets/tickets/{ticketId}/resolve`
Resolve a maintenance ticket.

**Auth:** `ASSET_MANAGER`, `MAINTENANCE_STAFF`

**Request body:**
```json
{ "resolution": "Replaced the HDMI cable. Working correctly now." }
```

---

### PATCH `/assets/{id}/dispose`
Dispose / write-off an asset.

**Auth:** `SUPER_ADMIN`, `ASSET_MANAGER`

**Request body:**
```json
{ "reason": "Beyond economical repair — 8 years old, motherboard failed" }
```

---

## 10. HR & Leave

### POST `/hr/leave/apply`
Apply for leave.

**Auth:** Any authenticated staff

**Request body:**
```json
{
  "leave_type_id": "...",
  "from_date": "2026-07-01",
  "to_date": "2026-07-03",
  "reason": "Family function"
}
```

**Response 201:** LeaveRequest object  
**Response 422** — Insufficient balance

---

### PATCH `/hr/leave/{requestId}/approve`
Approve a leave request.

**Auth:** `SUPER_ADMIN`, `HR_MANAGER`, `HOD`, `ADMIN`

**Request body:**
```json
{ "remarks": "Approved — arrange for a substitute for 2nd and 3rd July" }
```

---

### PATCH `/hr/leave/{requestId}/reject`
Reject a leave request.

**Auth:** `SUPER_ADMIN`, `HR_MANAGER`, `HOD`, `ADMIN`

**Request body:**
```json
{ "remarks": "Rejected — exam duty scheduled on these dates" }
```

---

## 11. Communication

### GET `/notices`
Get all active notices relevant to the current user.

**Auth:** Authenticated  
**Query:** `?page=0&size=20`

---

### POST `/notices`
Create and publish a notice.

**Auth:** `ADMIN`, `HOD`

**Request body:**
```json
{
  "title": "End-Term Examination Schedule",
  "body": "The end-term exams for Semester V will be held from Nov 1–15, 2026...",
  "target_type": "BATCH",
  "target_id": "...",
  "is_pinned": false,
  "expires_at": "2026-11-16T00:00:00Z"
}
```

**`target_type` values:** `ALL` | `DEPARTMENT` | `BATCH` | `ROLE`

---

### POST `/helpdesk`
Raise a helpdesk ticket.

**Auth:** Authenticated

**Request body:**
```json
{
  "title": "Fee payment not reflecting",
  "description": "I paid ₹25,000 via UPI on June 1st but the portal still shows outstanding",
  "category": "FINANCIAL",
  "priority": "HIGH"
}
```

---

### GET `/helpdesk/my-tickets`
List own helpdesk tickets.

**Auth:** Authenticated  
**Query:** `?status=OPEN&page=0&size=20`

---

## 12. Notifications

### GET `/notifications/stream`
Subscribe to real-time notifications via SSE.

**Auth:** Authenticated  
**Response:** `Content-Type: text/event-stream`

SSE events are named `notification`. Each event data is a NotificationRecord JSON object.

```
event: notification
data: {"id":"...","title":"Fee Payment Reminder","body":"Your invoice INV-2026-001 has an outstanding balance of ₹25,000","is_read":false,"created_at":"2026-06-01T08:00:00Z"}
```

---

### GET `/notifications`
Get notification inbox (paginated).

**Auth:** Authenticated  
**Query:** `?page=0&size=20`

---

### GET `/notifications/unread-count`
Get count of unread notifications.

**Auth:** Authenticated

**Response 200:**
```json
{ "success": true, "data": 5 }
```

---

### POST `/notifications/mark-all-read`
Mark all notifications as read.

**Auth:** Authenticated

**Response 200:**
```json
{ "success": true, "message": "Marked 5 notification(s) as read", "data": 5 }
```

---

## 13. Admin & Audit

### User Management

#### POST `/admin/users`
Create a new user account.

**Auth:** `SUPER_ADMIN`, `ADMIN`

**Request body:**
```json
{
  "username": "jdoe",
  "email": "jdoe@college.edu",
  "full_name": "John Doe",
  "password": "TempPass@123",
  "role": "FACULTY"
}
```

**`role` values:** See Roles & Permissions document for full enum list

---

#### GET `/admin/users`
List all users (paginated).

**Auth:** `SUPER_ADMIN`, `ADMIN`

---

#### GET `/admin/users/{id}`
Get user by ID.

**Auth:** `SUPER_ADMIN`, `ADMIN`

---

#### PATCH `/admin/users/{id}/lock`
Lock a user account.

**Auth:** `SUPER_ADMIN`, `ADMIN`

---

#### PATCH `/admin/users/{id}/unlock`
Unlock a user account.

**Auth:** `SUPER_ADMIN`, `ADMIN`

---

#### POST `/admin/users/{id}/roles`
Assign a role to a user.

**Auth:** `SUPER_ADMIN`, `ADMIN`

**Request body:**
```json
{
  "role": "HOD",
  "department_id": "...",
  "expires_at": null
}
```

---

### Audit Log

#### GET `/admin/audit-log`
Search audit log entries.

**Auth:** `SUPER_ADMIN`, `ADMIN`, `AUDITOR`

**Query parameters:**

| Parameter | Type | Description |
|---|---|---|
| `user_id` | UUID | Filter by user who performed the action |
| `entity_type` | String | e.g. `Student`, `FeeInvoice`, `BookCopy` |
| `action` | String | e.g. `CREATE`, `FEE_COLLECTED`, `BOOK_ISSUED` |
| `from` | Date | Start of date range (ISO format) |
| `to` | Date | End of date range (ISO format) |
| `page` | Int | Page number (default: 0) |
| `size` | Int | Page size (default: 50, max: 100) |

**Response 200:**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": "...",
        "username": "jdoe",
        "action": "FEE_COLLECTED",
        "module": "FINANCE",
        "entity_type": "FeePayment",
        "entity_id": "...",
        "description": "Receipt RCP-2026-001042 — ₹25,000 collected",
        "occurred_at": "2026-06-01T10:30:00Z"
      }
    ],
    "page": 0, "size": 50, "total_elements": 1284
  }
}
```

---

## 14. File Access

### GET `/files/{fileId}`
Retrieve a stored file by ID.

**Auth:** Authenticated  
**Response:** Binary file with appropriate `Content-Type`

> In cloud/on-prem mode with S3/MinIO, the app may redirect to a presigned URL.  
> In air-gapped mode, the file is proxied through the Spring Boot app.

---

## 15. Error Reference

### Business Rule Violation Codes

| Scenario | HTTP Status | `detail` |
|---|---|---|
| Duplicate enrollment number | 409 | `"Enrollment number '2024CSE001' already exists"` |
| Duplicate department code | 409 | `"Department with code 'CSE' already exists"` |
| Fee invoice already exists | 409 | `"Fee invoice for this student, batch and term already exists"` |
| Payment exceeds balance | 422 | `"Payment amount exceeds balance due (25000.00)"` |
| Invoice already paid | 422 | `"Invoice INV-2026-001042 is already paid"` |
| Batch at final term | 422 | `"Batch is already in its final term and cannot be promoted"` |
| Book copy not available | 422 | `"Copy [LIB-2026-001042] is not available — current status: ISSUED"` |
| Borrower has overdue | 422 | `"Borrower has overdue books — please return them first"` |
| Loan limit reached | 422 | `"Borrower has reached the maximum loan limit of 3 books"` |
| Renewal blocked by reservation | 422 | `"Cannot renew — another borrower has reserved this title"` |
| Attendance session exists | 409 | `"An attendance session already exists for this offering on 2026-06-15"` |
| Empty session submission | 422 | `"Cannot submit an empty session — mark attendance first"` |
| Self-approval blocked | 422 | `"Segregation of duties: creator cannot be the sole approver"` |
| Insufficient leave balance | 422 | `"Insufficient leave balance. Available: 3, Requested: 5"` |
| Financial year closed | 422 | `"Cannot post entries to a closed financial year"` |
| Year close — dual approval pending | 422 | `"Financial year close requires approval from both Head Accountant and Admin"` |

---

*Document Owner: API Design Team*  
*Last Updated: June 2026*  
*OpenAPI spec auto-generated at: `/v3/api-docs`*  
*Interactive docs at: `/swagger-ui.html` (non-production only)*
