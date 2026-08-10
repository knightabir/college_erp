# College ERP System — Coding Standards Document

**Version:** 1.0  
**Date:** June 2026  
**Status:** Enforced — violations block PR merge  
**Applies to:** Java (Spring Boot backend), SQL (Flyway migrations), JavaScript (React frontend)

---

## Table of Contents

1. [Core Philosophy](#1-core-philosophy)
2. [Java — General Standards](#2-java--general-standards)
3. [Java — Spring Boot Specifics](#3-java--spring-boot-specifics)
4. [Java — JPA and Database](#4-java--jpa-and-database)
5. [Java — Security and Auth](#5-java--security-and-auth)
6. [SQL — Migration Standards](#6-sql--migration-standards)
7. [JavaScript — General Standards](#7-javascript--general-standards)
8. [JavaScript — React Specifics](#8-javascript--react-specifics)
9. [JavaScript — State Management](#9-javascript--state-management)
10. [Naming Conventions](#10-naming-conventions)
11. [Error Handling Standards](#11-error-handling-standards)
12. [Logging Standards](#12-logging-standards)
13. [Testing Standards](#13-testing-standards)
14. [Git Workflow](#14-git-workflow)
15. [Code Review Checklist](#15-code-review-checklist)
16. [Tooling and Enforcement](#16-tooling-and-enforcement)

---

## 1. Core Philosophy

### 1.1 DRY — Don't Repeat Yourself

**Write every piece of logic exactly once. Then import it.**

This is the single most important rule in this codebase. Before writing any utility function, validation, formatter, or shared component, check if one already exists in:
- Java: `com.college.erp.common.*` or the platform kernel packages
- JavaScript: `src/shared/utils/`, `src/shared/hooks/`, `src/shared/components/`

If it doesn't exist and you'll need it more than once, create it in the shared location. **Never copy-paste code from one module to another.**

Examples of violations that will be rejected in code review:
```java
// ❌ WRONG — date formatting duplicated in two service classes
// In StudentService:
String formatted = date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

// In ExamService (copy-pasted):
String formatted = date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

// ✅ CORRECT — written once in common
// DateUtil.formatDisplayDate(date) — used everywhere
```

```javascript
// ❌ WRONG — currency formatting duplicated
// In FeeInvoicePage.jsx:
const formatted = `₹${amount.toFixed(2)}`;

// In PaymentHistoryPage.jsx (copy-pasted):
const formatted = `₹${amount.toFixed(2)}`;

// ✅ CORRECT — written once in shared/utils/currencyUtils.js
import { formatCurrency } from '@/shared/utils/currencyUtils';
```

### 1.2 Single Responsibility

Each class, function, and component does exactly one thing. A service class manages one domain entity or one closely related group. A React component either renders UI or manages state — not both in a complex way.

### 1.3 Explicit Over Clever

Readable, straightforward code is preferred over clever one-liners. The next developer reading your code (who might be you in 6 months) should understand it in one pass.

---

## 2. Java — General Standards

### 2.1 Language Version

Use Java 21 features where they improve readability:
- **Records** for simple data carriers (not JPA entities)
- **Text blocks** for multi-line strings (SQL, JSON test fixtures)
- **Pattern matching** for `instanceof` checks
- **Switch expressions** over switch statements

```java
// ✅ Java 21 switch expression
String label = switch (role) {
    case STUDENT -> "Student";
    case FACULTY -> "Faculty";
    case HOD     -> "Head of Department";
    default      -> role.name();
};

// ✅ Text block for JPQL
@Query("""
    SELECT s FROM Student s
    WHERE s.enrollmentNumber = :number
    AND s.deleted = false
    """)
Optional<Student> findByEnrollmentNumber(@Param("number") String number);

// ✅ Pattern matching instanceof
if (auth.getPrincipal() instanceof UserPrincipal principal) {
    return Optional.of(principal.getId());
}
```

### 2.2 Lombok Usage

Lombok is used to eliminate boilerplate. Required annotations per class type:

**JPA Entities:**
```java
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Student extends BaseEntity { ... }
```

**Append-only entities (no @Setter — immutable after construction):**
```java
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LedgerEntry { ... }
```

**Service classes:**
```java
@Slf4j
@Service
@RequiredArgsConstructor    // ← constructor injection (preferred over @Autowired)
public class StudentService {
    private final StudentRepository studentRepository;  // immutable, non-null
}
```

**DTOs (request/response):**
```java
@Getter
@Setter
public class CreateStudentRequest {
    @NotBlank
    private String enrollmentNumber;
}

@Getter
@Builder
public class StudentDto {
    private UUID id;
    private String enrollmentNumber;
}
```

### 2.3 Immutability

Prefer immutable objects. Use `List.of()`, `Map.of()`, `Set.of()` where the collection does not need to change. Declare fields `final` wherever possible.

```java
// ✅ Immutable field in service
private final StudentRepository studentRepository;

// ✅ Immutable local collection
Set<String> roles = roleAssignmentRepository
    .findActiveRolesByUserId(userId)
    .stream()
    .map(UserRole::name)
    .collect(Collectors.toUnmodifiableSet());   // ← unmodifiable

// ❌ Unnecessary mutability
List<String> roles = new ArrayList<>();
roles.add("STUDENT");
```

### 2.4 Optional Usage

```java
// ✅ Return Optional from repository — don't return null
Optional<Student> findByEnrollmentNumberAndDeletedFalse(String number);

// ✅ Handle Optional with orElseThrow in service
Student student = studentRepository
    .findByEnrollmentNumberAndDeletedFalse(number)
    .orElseThrow(() -> new ResourceNotFoundException(
        "Student with enrollment number: " + number));

// ❌ Never return null where Optional is expected
// ❌ Never use Optional.get() without checking isPresent()
// ❌ Never use Optional as a field type in an entity
```

### 2.5 Stream API

```java
// ✅ Readable pipeline
Set<String> activeRoles = assignments.stream()
    .filter(RoleAssignment::isEffective)
    .map(ra -> ra.getRole().name())
    .collect(Collectors.toUnmodifiableSet());

// ❌ Over-complex stream that would be clearer as a loop
// Use for-each when the operation has side effects or is multi-step
```

---

## 3. Java — Spring Boot Specifics

### 3.1 Controller Rules

Controllers are thin. They translate HTTP to service calls and back.

```java
// ✅ CORRECT — thin controller
@PostMapping
@PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
public ResponseEntity<ApiResponse<Department>> create(
        @Valid @RequestBody CreateDeptRequest req) {

    Department dept = Department.builder()
            .name(req.getName())
            .code(req.getCode().toUpperCase())
            .build();

    return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Department created",
                    departmentService.create(dept)));
}

// ❌ WRONG — business logic in controller
@PostMapping
public ResponseEntity<?> create(@RequestBody CreateDeptRequest req) {
    // ❌ Validation logic belongs in service
    if (req.getCode().length() > 20) throw new RuntimeException("too long");
    // ❌ Repository access belongs in service
    if (departmentRepository.existsByCode(req.getCode())) throw new RuntimeException("exists");
    Department dept = new Department();
    dept.setName(req.getName());
    return ResponseEntity.ok(departmentRepository.save(dept));
}
```

**Controller checklist:**
- `@Valid` on all `@RequestBody` parameters
- `@PreAuthorize` on every non-public endpoint
- One service call per endpoint
- No business logic
- No repository access
- No direct entity returns — use DTOs

### 3.2 Service Rules

```java
// ✅ Service class structure
@Slf4j
@Service
@RequiredArgsConstructor
public class DepartmentService {

    private static final String MODULE = "ACADEMIC";  // ← for audit log

    private final DepartmentRepository departmentRepository;
    private final AuditLogWriter auditLogWriter;

    @Transactional
    public Department create(Department department) {
        // 1. Business rule checks first
        if (departmentRepository.existsByCodeAndDeletedFalse(department.getCode())) {
            throw new DuplicateResourceException(
                    "Department with code '" + department.getCode() + "' already exists");
        }

        // 2. Save
        Department saved = departmentRepository.save(department);

        // 3. Side effects (async — never blocks the transaction)
        auditLogWriter.write(AuditAction.CREATE, MODULE,
                "Department", saved.getId().toString(),
                "Created: " + saved.getName());

        // 4. Log at INFO for important operations
        log.info("Department created: [{}] {}", saved.getCode(), saved.getName());

        return saved;
    }

    // Read methods are explicitly @Transactional(readOnly = true)
    @Transactional(readOnly = true)
    public Department getById(UUID id) {
        return departmentRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department", id));
    }
}
```

**Service checklist:**
- `@Transactional` on all write methods
- `@Transactional(readOnly = true)` on all read methods
- Validate business rules before touching the database
- Throw typed exceptions — never raw `RuntimeException`
- Write to `AuditLogWriter` for all create/update/delete operations
- Log at `INFO` level for significant operations, `DEBUG` for detailed tracing

### 3.3 Configuration Properties

Always use `@ConfigurationProperties` for typed config. Never use `@Value` for multi-property groups.

```java
// ✅ CORRECT — typed, nested, auto-completed in IDE
@ConfigurationProperties(prefix = "erp.notification")
public class NotificationProperties {
    private Email email = new Email();
    private Sms sms = new Sms();

    @Getter @Setter
    public static class Email {
        private boolean enabled;
        private String from;
    }
}

// ❌ WRONG — scattered, no grouping, breaks on rename
@Value("${erp.notification.email.enabled}")
private boolean emailEnabled;

@Value("${erp.notification.email.from}")
private String emailFrom;
```

### 3.4 Dependency Injection

Constructor injection always. No field injection (`@Autowired` on fields).

```java
// ✅ Constructor injection (via @RequiredArgsConstructor)
@RequiredArgsConstructor
public class SomeService {
    private final SomeRepository repo;
    private final AuditLogWriter auditWriter;
}

// ❌ Field injection — cannot be unit tested without Spring context
@Autowired
private SomeRepository repo;
```

### 3.5 @Async Methods

```java
// ✅ CORRECT — async method cannot return a value the caller awaits
@Async
@Transactional(propagation = Propagation.REQUIRES_NEW)
public void write(AuditAction action, ...) {
    // Runs in separate thread + separate transaction
    // REQUIRES_NEW ensures the audit entry is committed even if caller rolls back
}

// ❌ WRONG — @Async with void but has side effects that caller depends on
// If caller needs the result, don't use @Async
```

---

## 4. Java — JPA and Database

### 4.1 Entity Design

```java
// ✅ CORRECT entity
@Entity
@Table(
    name = "students",
    indexes = {
        @Index(name = "idx_student_enrollment", columnList = "enrollment_number", unique = true)
    }
)
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Student extends BaseEntity {

    @Column(name = "enrollment_number", nullable = false, unique = true, length = 30)
    private String enrollmentNumber;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    // ✅ Lazy fetch for all associations — only load when explicitly accessed
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "batch_id", nullable = false)
    private Batch batch;

    // ✅ Business method on entity for domain behavior
    public String getFullName() {
        return firstName + " " + lastName;
    }
}
```

**Entity checklist:**
- All `@ManyToOne` and `@OneToMany` use `FetchType.LAZY`
- All `@Column` annotations specify `name`, `nullable`, and `length` (for strings)
- No `FetchType.EAGER` — this causes N+1 query problems
- No bidirectional `@OneToMany` with `CascadeType.ALL` unless you own both sides
- `@Index` annotations on all columns used in WHERE clauses

### 4.2 Repository Design

```java
// ✅ CORRECT — explicit, type-safe, tested
public interface StudentRepository extends JpaRepository<Student, UUID> {

    // ✅ Partial filter on non-deleted only
    Optional<Student> findByEnrollmentNumberAndDeletedFalse(String number);

    // ✅ Existence check — cheaper than loading the entity
    boolean existsByEnrollmentNumberAndDeletedFalse(String number);

    // ✅ JPQL for complex queries — readable and parameterized
    @Query("""
        SELECT s FROM Student s
        JOIN Enrollment e ON e.student.id = s.id
        WHERE e.section.id = :sectionId
        AND e.is_current = true
        AND s.deleted = false
        ORDER BY s.lastName, s.firstName
        """)
    Page<Student> findBySectionId(@Param("sectionId") UUID sectionId, Pageable pageable);
}

// ❌ WRONG
public interface StudentRepository extends JpaRepository<Student, UUID> {
    // ❌ Never return all rows without pagination for potentially large tables
    List<Student> findAll();

    // ❌ Raw string SQL in application code
    @Query(value = "SELECT * FROM students WHERE deleted = false", nativeQuery = true)
    List<Student> findAllRaw();
}
```

### 4.3 Transaction Boundaries

```java
// ✅ CORRECT — one service method = one transaction boundary
@Transactional
public FeePayment collectPayment(...) {
    // Invoice load, payment create, invoice update, ledger post — ONE transaction
    // Either all succeed or none do
    FeeInvoice invoice = loadInvoice(invoiceId);
    FeePayment payment = createPayment(invoice, ...);
    updateInvoiceStatus(invoice, payment.getAmount());
    ledgerService.postFeeCollection(payment, invoice);
    return payment;
}

// ❌ WRONG — multiple transactions for one logical operation
// If the ledger post fails, the payment is already committed — data corruption
public FeePayment collectPayment(...) {
    FeePayment payment = paymentService.create(...);   // transaction 1
    invoiceService.update(...);                         // transaction 2
    ledgerService.post(...);                            // transaction 3
}
```

### 4.4 No Cross-Module SQL Joins

```java
// ✅ CORRECT — cross-module data via service call
@Service
@RequiredArgsConstructor
public class FeeCollectionService {
    private final StudentService studentService;  // service interface

    public void generateInvoice(UUID studentId, ...) {
        // Get student data through StudentService, not directly through StudentRepository
        Student student = studentService.getById(studentId);
    }
}

// ❌ WRONG — directly accessing another module's repository
@Service
@RequiredArgsConstructor
public class FeeCollectionService {
    private final StudentRepository studentRepository;  // ← cross-module repo access: FORBIDDEN
}
```

---

## 5. Java — Security and Auth

### 5.1 Every Endpoint Must Have Authorization

```java
// ✅ CORRECT — explicit on every method
@GetMapping("/{id}")
@PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','HOD') or " +
              "@studentSecurityService.isOwnProfile(#id, principal)")
public ResponseEntity<ApiResponse<Student>> getById(@PathVariable UUID id) { ... }

// ❌ WRONG — missing @PreAuthorize (defaults to authenticated-only, no role check)
@GetMapping("/{id}")
public ResponseEntity<ApiResponse<Student>> getById(@PathVariable UUID id) { ... }
```

### 5.2 Never Log Sensitive Data

```java
// ✅ CORRECT
log.info("Login attempt for user [{}]", identifier);
log.info("Password changed for user [{}]", userId);

// ❌ WRONG — never log passwords, tokens, secrets
log.debug("Login with password: {}", password);
log.info("Access token: {}", accessToken);
log.info("Aadhar number: {}", student.getAadharNumber());
```

### 5.3 Input Validation

```java
// ✅ Always validate at the DTO level
public class CreateStudentRequest {
    @NotBlank(message = "Enrollment number is required")
    @Size(max = 30, message = "Enrollment number must not exceed 30 characters")
    @Pattern(regexp = "^[A-Z0-9]+$", message = "Enrollment number must be alphanumeric uppercase")
    private String enrollmentNumber;

    @NotNull(message = "Date of birth is required")
    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;

    @NotBlank
    @Email(message = "Must be a valid email address")
    private String personalEmail;
}

// ❌ Never trust client input without validation
// ❌ Never do manual null checks in service that @NotNull at DTO level covers
```

---

## 6. SQL — Migration Standards

### 6.1 Migration File Naming

```
V{number}__{description}.sql

Examples:
V001__create_user_tables.sql
V002__create_audit_log.sql
V015__add_student_alumni_profile.sql
V016__alter_fee_invoices_add_scholarship_id.sql
```

**Rules:**
- Numbers are sequential integers with no gaps
- Description is lowercase with underscores
- One concern per migration file (don't mix user table creation with library tables)
- Never edit a previously released migration — always create a new one

### 6.2 SQL Style

```sql
-- ✅ CORRECT SQL style
CREATE TABLE fee_payments (
    id                   UUID          NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    invoice_id           UUID          NOT NULL REFERENCES fee_invoices(id),
    receipt_number       VARCHAR(30)   NOT NULL UNIQUE,
    amount               NUMERIC(14,2) NOT NULL,
    payment_mode         VARCHAR(30)   NOT NULL,
    payment_timestamp    TIMESTAMPTZ   NOT NULL,
    status               VARCHAR(20)   NOT NULL DEFAULT 'SUCCESS',
    is_deleted           BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at           TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    created_by           UUID,
    updated_by           UUID
);

-- ✅ Index naming: idx_{table}_{column}
CREATE INDEX idx_payment_invoice ON fee_payments(invoice_id);
CREATE UNIQUE INDEX idx_payment_receipt ON fee_payments(receipt_number);
```

**SQL Rules:**
- All keywords `UPPERCASE`
- All identifiers `snake_case`
- Every `NOT NULL` column gets a `DEFAULT` where appropriate
- Foreign keys are always declared explicitly
- All amounts use `NUMERIC(14,2)` — never `FLOAT` or `DOUBLE`
- All timestamps use `TIMESTAMPTZ` — never `TIMESTAMP` (no timezone)
- Comments explain WHY, not what the SQL does (the SQL is self-explanatory)

### 6.3 Irreversible Migration Handling

```sql
-- ✅ For DROP TABLE or DROP COLUMN — precede with a deprecation migration
-- V024__deprecate_old_notification_column.sql
-- (Application code stops using this column in the same release)

-- V025__remove_deprecated_notification_column.sql
ALTER TABLE notification_records DROP COLUMN IF EXISTS legacy_channel;
-- "IF EXISTS" makes it safe to run on environments where the column may already be gone
```

---

## 7. JavaScript — General Standards

### 7.1 Module System

Use ES modules exclusively. CommonJS (`require`, `module.exports`) is not used in the frontend.

```javascript
// ✅ ES module imports
import { useState, useEffect } from 'react';
import { formatCurrency } from '@/shared/utils/currencyUtils';
import { useStudentProfile } from '@/hooks/useStudentProfile';

// ❌ CommonJS — not used
const React = require('react');
```

### 7.2 Path Aliases

Use `@/` for imports from `src/`. No relative paths climbing more than one level.

```javascript
// ✅ Clean alias import
import { DataTable } from '@/shared/components/DataTable';
import { usePermission } from '@/shared/hooks/usePermission';

// ❌ Deep relative paths — fragile and unreadable
import { DataTable } from '../../../shared/components/DataTable';
```

### 7.3 Constants

```javascript
// ✅ Named constants for magic values
const MAX_LOAN_DAYS = 14;
const ATTENDANCE_MINIMUM_PCT = 75;
const RECEIPT_PREFIX = 'RCP';

// ✅ Enum-like objects for string literals
export const PaymentMode = Object.freeze({
    CASH: 'CASH',
    ONLINE_UPI: 'ONLINE_UPI',
    ONLINE_CARD: 'ONLINE_CARD',
    NEFT: 'NEFT',
    RTGS: 'RTGS',
});

// ❌ Magic strings scattered across components
if (payment.mode === 'ONLINE_UPI') { ... }  // what if the spelling changes?
```

### 7.4 Async/Await

```javascript
// ✅ async/await with proper error handling
async function collectPayment(invoiceId, payload) {
    try {
        const response = await apiClient.post(
            `/finance/fees/invoices/${invoiceId}/collect`,
            payload
        );
        return response.data;
    } catch (error) {
        // error is already normalized by the Axios interceptor
        throw error;
    }
}

// ❌ Promise chains with .then().catch() — harder to read
apiClient.post('/collect', payload)
    .then(res => res.data)
    .catch(err => { throw err; });
```

---

## 8. JavaScript — React Specifics

### 8.1 Component Rules

```jsx
// ✅ CORRECT component structure
// 1. Named export (not default export for shared components)
// 2. Props destructured in signature
// 3. Hooks at the top (Rules of Hooks)
// 4. Derived values computed from state/props
// 5. Event handlers before JSX
// 6. JSX at the bottom

export function FeeInvoiceCard({ invoice, onPayClick }) {
    // Hooks first
    const { can } = usePermission();
    const { mutate: collectPayment, isLoading } = useCollectPayment();

    // Derived values
    const isPaid = invoice.status === 'PAID';
    const formattedBalance = formatCurrency(invoice.balance_due);

    // Event handlers
    const handlePayClick = () => onPayClick(invoice.id);

    // JSX last
    return (
        <Card>
            <p>Invoice: {invoice.invoice_number}</p>
            <p>Balance: {formattedBalance}</p>
            {can('FINANCE.FEE_COLLECTION') && !isPaid && (
                <Button onClick={handlePayClick} loading={isLoading}>
                    Collect Payment
                </Button>
            )}
        </Card>
    );
}
```

### 8.2 Component Size

A component that exceeds 150 lines of JSX is a signal to split it. Extract:
- Repeated sections into sub-components
- Complex conditional rendering into separate components
- Reusable logic into custom hooks

### 8.3 Prop Types / JSDoc

```javascript
/**
 * @param {Object} props
 * @param {import('@/types/student').Student} props.student
 * @param {() => void} props.onEdit
 * @param {boolean} [props.compact=false]
 */
export function StudentCard({ student, onEdit, compact = false }) { ... }
```

### 8.4 Avoid Prop Drilling

When a prop needs to pass through more than two levels, extract it into a TanStack Query hook (if it's server state) or a small Zustand slice (if it's UI state).

```javascript
// ❌ Prop drilling — passing userId through 3 levels
<Dashboard userId={userId}>
    <AttendanceSection userId={userId}>
        <AttendanceChart userId={userId} />
    </AttendanceSection>
</Dashboard>

// ✅ Each component reads from the hook directly
// In AttendanceChart.jsx:
const { data: summary } = useAttendanceSummary(studentId);
```

### 8.5 Key Props in Lists

```jsx
// ✅ CORRECT — stable, unique key
{invoices.map(invoice => (
    <FeeInvoiceRow key={invoice.id} invoice={invoice} />
))}

// ❌ WRONG — index as key (breaks on reorder/delete)
{invoices.map((invoice, index) => (
    <FeeInvoiceRow key={index} invoice={invoice} />
))}
```

---

## 9. JavaScript — State Management

### 9.1 TanStack Query for Server State

```javascript
// ✅ CORRECT — server state in TanStack Query
// hooks/useStudentProfile.js
export function useStudentProfile(studentId) {
    return useQuery({
        queryKey: ['student', studentId],
        queryFn: () => studentApi.getById(studentId),
        enabled: !!studentId,
        staleTime: 5 * 60 * 1000,   // 5 minutes before re-fetch
    });
}

// Mutation
export function useCollectPayment() {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: ({ invoiceId, payload }) =>
            financeApi.collectPayment(invoiceId, payload),
        onSuccess: (data, { invoiceId }) => {
            // Invalidate related queries after mutation succeeds
            queryClient.invalidateQueries({ queryKey: ['invoice', invoiceId] });
            queryClient.invalidateQueries({ queryKey: ['outstanding-invoices'] });
        },
    });
}
```

### 9.2 Zustand for UI State

```javascript
// ✅ CORRECT — only non-server UI state in Zustand
// store/uiStore.js
import { create } from 'zustand';

export const useUiStore = create((set) => ({
    sidebarOpen: true,
    toggleSidebar: () => set(state => ({ sidebarOpen: !state.sidebarOpen })),

    activeFilters: {},
    setFilter: (key, value) =>
        set(state => ({ activeFilters: { ...state.activeFilters, [key]: value } })),
    clearFilters: () => set({ activeFilters: {} }),
}));

// ❌ WRONG — server data in Zustand
export const useStore = create((set) => ({
    students: [],                              // ← server data belongs in TanStack Query
    fetchStudents: async () => {
        const data = await api.get('/students');
        set({ students: data });
    }
}));
```

---

## 10. Naming Conventions

### 10.1 Java Naming

| Element | Convention | Example |
|---|---|---|
| Class | PascalCase | `StudentService`, `FeeInvoice` |
| Interface | PascalCase (no `I` prefix) | `FileStorageService`, `NotificationChannel` |
| Method | camelCase, verb-first | `createStudent()`, `getById()`, `findActiveByUserId()` |
| Variable | camelCase | `studentId`, `invoiceNumber`, `paidAmount` |
| Constant | UPPER_SNAKE_CASE | `MAX_LOGIN_ATTEMPTS`, `MODULE` |
| Package | lowercase.with.dots | `com.college.erp.module.finance.service` |
| Test class | `{ClassUnderTest}Test` | `StudentServiceTest` |
| Exception | `{Descriptive}Exception` | `ResourceNotFoundException` |

### 10.2 SQL Naming

| Element | Convention | Example |
|---|---|---|
| Table | snake_case, plural | `fee_invoices`, `attendance_records` |
| Column | snake_case | `invoice_number`, `is_deleted` |
| Primary key | `id` | `id UUID NOT NULL` |
| Foreign key | `{table_singular}_id` | `student_id`, `invoice_id` |
| Index | `idx_{table}_{columns}` | `idx_invoice_student` |
| Unique constraint | `uq_{table}_{columns}` | `uq_sections_batch_name` |
| Trigger | `trg_{table}_{action}` | `trg_audit_log_no_delete` |
| Function | `fn_{description}` | `fn_reject_modification` |

### 10.3 JavaScript Naming

| Element | Convention | Example |
|---|---|---|
| Component | PascalCase | `FeeInvoiceCard`, `DataTable` |
| Hook | `use` prefix + camelCase | `useStudentProfile`, `usePermission` |
| API file | `{module}.api.js` | `finance.api.js`, `library.api.js` |
| Store | `use{Name}Store` | `useAuthStore`, `useUiStore` |
| Utility function | camelCase, verb-first | `formatCurrency()`, `parseDate()` |
| Constant | UPPER_SNAKE_CASE | `MAX_BOOKS_PER_BORROWER` |
| Event handler | `handle{Event}` | `handleSubmit`, `handleRowClick` |
| Boolean prop/state | `is{State}` or `has{Thing}` | `isLoading`, `hasError`, `isPaid` |

---

## 11. Error Handling Standards

### 11.1 Java — Throw Typed, Handle Once

```java
// ✅ CORRECT — throw typed exception in service
public Department getById(UUID id) {
    return departmentRepository.findByIdAndDeletedFalse(id)
            .orElseThrow(() -> new ResourceNotFoundException("Department", id));
}

// ✅ Handle once — GlobalExceptionHandler maps typed exceptions to HTTP responses
// No try/catch in controllers
// No try/catch in services unless you specifically need to handle a recoverable error

// ❌ WRONG — catch and rethrow as generic exception (loses type information)
try {
    return departmentRepository.findByIdAndDeletedFalse(id)
            .orElseThrow(() -> new ResourceNotFoundException("Department", id));
} catch (Exception e) {
    throw new RuntimeException(e);  // ← loses the 404 → 500 mapping
}
```

### 11.2 JavaScript — Normalize at API Layer

```javascript
// ✅ CORRECT — errors normalized once in api/client.js interceptor
// Components receive a consistent error shape: { title, detail, fieldErrors }

// In a component:
const { error } = useStudentProfile(studentId);
if (error) {
    return <ErrorMessage title={error.title} detail={error.detail} />;
}

// ❌ WRONG — each component handles raw axios errors differently
try {
    const res = await axios.get(`/students/${id}`);
} catch (e) {
    if (e.response?.status === 404) { ... }
    else if (e.response?.status === 403) { ... }
    // repeated in every component that makes API calls
}
```

---

## 12. Logging Standards

### 12.1 Log Levels

| Level | When to Use |
|---|---|
| `ERROR` | Unexpected exceptions that indicate a bug or infrastructure failure |
| `WARN` | Expected failures that require attention (account locked, overdue fine detected) |
| `INFO` | Significant business events (user created, fee collected, book issued) |
| `DEBUG` | Detailed tracing for development/troubleshooting (query results, object states) |
| `TRACE` | Never used in production |

### 12.2 Log Format

```java
// ✅ CORRECT — include identifying context, use parameterized messages
log.info("Student enrolled: [{}] {} → batch [{}]",
    student.getEnrollmentNumber(), student.getFullName(), batchId);

log.warn("Account locked: user [{}] after {} failed attempts",
    username, failedAttempts);

log.error("Failed to send email notification to user [{}]: {}",
    userId, ex.getMessage(), ex);

// ❌ WRONG — string concatenation (expensive if level is disabled)
log.debug("Processing student: " + student.getFullName());

// ❌ WRONG — logging sensitive data
log.info("Login attempt: username={} password={}", username, password);

// ❌ WRONG — exception without the exception object (loses stack trace)
log.error("Something went wrong: " + ex.getMessage());
```

### 12.3 MDC Context

The `RequestIdFilter` puts `requestId` and `userId` into MDC. These appear in every log line automatically via `logback-spring.xml`. No need to manually include them in log calls.

---

## 13. Testing Standards

### 13.1 Test Structure (AAA)

Every test follows **Arrange → Act → Assert**:

```java
@Test
@DisplayName("collectPayment() — should update invoice to PARTIAL when partial amount paid")
void collectPayment_partial() {
    // ARRANGE
    FeeInvoice invoice = buildUnpaidInvoice(new BigDecimal("50000.00"));
    UUID invoiceId = UUID.randomUUID();
    when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));
    when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(invoiceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    doNothing().when(ledgerService).postFeeCollection(any(), any());

    // ACT
    FeePayment result = feeCollectionService.collectPayment(
            invoiceId, new BigDecimal("25000.00"),
            PaymentMode.CASH, null, UUID.randomUUID());

    // ASSERT
    assertThat(result.getAmount()).isEqualByComparingTo("25000.00");
    assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.PARTIAL);
    assertThat(invoice.getPaidAmount()).isEqualByComparingTo("25000.00");
}
```

### 13.2 Test Naming

```java
// Pattern: {method}_{scenario}_{expectedOutcome}
void collectPayment_alreadyPaid_throwsBusinessRuleViolation()
void getById_notFound_throwsResourceNotFoundException()
void issueBook_borrowerHasOverdue_throwsBusinessRuleViolation()
void validateToken_tamperedToken_returnsFalse()
```

### 13.3 What to Test

**Unit tests (JUnit 5 + Mockito) — mock all dependencies:**
- All service methods (both happy path and error cases)
- Custom permission evaluator logic
- Business rule calculations (attendance %, fine calculation, balance check)
- ArchUnit module boundary rules

**Integration tests (Spring Boot Test + Testcontainers) — real PostgreSQL:**
- Repository custom queries
- End-to-end request/response for critical flows (fee collection, library issue)
- Flyway migration runs without errors

**What NOT to test:**
- Lombok-generated getters/setters
- Simple pass-through controller methods (covered by integration tests)
- Spring Boot auto-configuration

### 13.4 AssertJ over JUnit Assertions

```java
// ✅ AssertJ — fluent, readable, informative failure messages
assertThat(result.getAmount()).isEqualByComparingTo("25000.00");
assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.PARTIAL);
assertThatThrownBy(() -> service.getById(unknownId))
    .isInstanceOf(ResourceNotFoundException.class)
    .hasMessageContaining("Department");

// ❌ JUnit assertEquals — less readable, poor failure messages
assertEquals(new BigDecimal("25000.00"), result.getAmount());
```

---

## 14. Git Workflow

### 14.1 Branch Naming

```
feature/{ticket-id}-{short-description}
fix/{ticket-id}-{short-description}
chore/{description}
docs/{description}

Examples:
feature/ERP-142-library-barcode-generation
fix/ERP-189-attendance-session-duplicate-check
chore/upgrade-spring-boot-3.3
```

### 14.2 Commit Messages (Conventional Commits)

```
{type}({scope}): {description}

Types: feat | fix | docs | test | refactor | chore | perf

Examples:
feat(library): add per-copy barcode generation via ZXing
fix(finance): prevent double payment on idempotency key retry
test(attendance): add service unit tests for condonation workflow
refactor(common): extract date formatting into DateUtil
chore(deps): upgrade JJWT to 0.12.5
```

### 14.3 Pull Request Requirements

Every PR must:
1. Pass all CI checks (compile, unit tests, ArchUnit, lint)
2. Include tests for new service methods (unit tests minimum)
3. Fill in the PR template including "Air-gap impact" section
4. Have at least one code review approval
5. Not decrease test coverage below the baseline
6. Include the Flyway migration in the same PR as the code that uses it

---

## 15. Code Review Checklist

### Reviewer must check:

**Architecture:**
- [ ] No business logic in controllers
- [ ] No cross-module repository access
- [ ] Shared utility reused from `common` — not duplicated
- [ ] Shared React component reused from `shared/` — not duplicated

**Security:**
- [ ] Every controller endpoint has `@PreAuthorize`
- [ ] No sensitive data (passwords, tokens, PII) in log statements
- [ ] Input validated at DTO level with annotations
- [ ] No raw string SQL concatenation

**Database:**
- [ ] Flyway migration included for schema changes
- [ ] Migration is numbered correctly (next in sequence)
- [ ] All list endpoints use pagination (no unbounded `findAll()`)
- [ ] New associations use `FetchType.LAZY`

**DRY:**
- [ ] No duplicated logic that already exists in `common` or `shared/`
- [ ] No copy-pasted code from another module

**Testing:**
- [ ] New service methods have unit tests
- [ ] Both happy path and error cases are tested
- [ ] `@DisplayName` is descriptive

**Air-gap:**
- [ ] No new CDN references in frontend
- [ ] No new external HTTP calls without a feature flag + fallback
- [ ] PR template "Air-gap impact" section is filled in

---

## 16. Tooling and Enforcement

### 16.1 Java Tooling

| Tool | Purpose | Runs at |
|---|---|---|
| Maven Compiler | Build + dependency management | `mvn compile` |
| Checkstyle | Code style enforcement | `mvn checkstyle:check` |
| SpotBugs | Static bug detection | `mvn spotbugs:check` |
| ArchUnit | Module boundary enforcement | `mvn test` |
| JUnit 5 + Mockito | Unit tests | `mvn test` |
| Testcontainers | Integration tests with real Postgres | `mvn verify` |

### 16.2 JavaScript Tooling

| Tool | Purpose | Runs at |
|---|---|---|
| ESLint | Code style + error detection | `pnpm lint` |
| Prettier | Auto-formatting | `pnpm format` |
| Vite | Build and dev server | `pnpm dev`, `pnpm build` |
| Vitest | Unit tests | `pnpm test` |
| React Testing Library | Component tests | `pnpm test` |
| axe-core | Accessibility audit | `pnpm test` |

### 16.3 ESLint Rules (key rules enforced)

```javascript
// .eslintrc enforces:
"no-console": "error"           // use structured logging, not console.log
"react-hooks/rules-of-hooks": "error"
"react-hooks/exhaustive-deps": "warn"
"no-unused-vars": "error"
"import/no-duplicates": "error"
"no-duplicate-imports": "error"
```

### 16.4 Pre-commit Hooks

Husky runs on every `git commit`:
1. ESLint + Prettier on staged JavaScript files
2. Commit message format validation (Conventional Commits)

Violations fail the commit locally — code never reaches CI in a broken state.

---

*Document Owner: Engineering Lead*  
*Last Updated: June 2026*  
*Enforcement: CI pipeline blocks PR merge on any violation*
