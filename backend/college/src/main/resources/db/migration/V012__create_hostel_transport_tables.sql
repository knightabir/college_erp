-- Hostel & Transport modules: hostel_blocks, rooms, room_allocations, outpass_requests,
-- routes, vehicles, student_route_mappings. Source of truth: 04-database-schema.md §14.
--
-- Unlike every other module built so far, Hostel & Transport have NO 03-srs.md functional
-- requirements ([XXX-nnn] codes) and NO 05-api-documentation.md endpoint section — the docs only
-- carry the DDL. 01-project-vision.md line 308 places "Hostel, Transport" in Phase 2 (alongside
-- Admissions/Placement), which explains why: this is schema drafted ahead of its own SRS/API
-- pass. Built here from the DDL plus ordinary domain sense (room capacity, outpass
-- apply/approve/reject/cancel mirroring the shape of leave_requests) rather than any cited
-- requirement — see the module's CLAUDE.md deviations section for the specific inferences made.

CREATE TABLE hostel_blocks (
    id         UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    name       VARCHAR(100) NOT NULL,
    gender     VARCHAR(10)  NOT NULL,  -- MALE | FEMALE | MIXED
    warden_id  UUID         REFERENCES users(id),
    capacity   INT,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID, updated_by UUID
);

CREATE TABLE rooms (
    id          UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    block_id    UUID        NOT NULL REFERENCES hostel_blocks(id),
    room_number VARCHAR(20) NOT NULL,
    floor       INT,
    capacity    INT         NOT NULL DEFAULT 2,
    room_type   VARCHAR(20),             -- SINGLE | DOUBLE | TRIPLE
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID, updated_by UUID,
    UNIQUE (block_id, room_number)
);

CREATE TABLE room_allocations (
    id         UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    room_id    UUID        NOT NULL REFERENCES rooms(id),
    student_id UUID        NOT NULL REFERENCES students(id),
    from_date  DATE        NOT NULL,
    to_date    DATE,
    status     VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    -- ACTIVE | VACATED
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID, updated_by UUID
);

CREATE TABLE outpass_requests (
    id            UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    student_id    UUID        NOT NULL REFERENCES students(id),
    from_datetime TIMESTAMPTZ NOT NULL,
    to_datetime   TIMESTAMPTZ NOT NULL,
    reason        TEXT        NOT NULL,
    destination   VARCHAR(200),
    status        VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    -- PENDING | APPROVED | REJECTED | CANCELLED
    approved_by   UUID        REFERENCES users(id),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID, updated_by UUID
);

CREATE TABLE routes (
    id          UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    description TEXT,
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID, updated_by UUID
);

CREATE TABLE vehicles (
    id              UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    registration_no VARCHAR(20) NOT NULL UNIQUE,
    vehicle_type    VARCHAR(30),
    capacity        INT,
    driver_name     VARCHAR(100),
    driver_phone    VARCHAR(20),
    driver_license  VARCHAR(30),
    license_expiry  DATE,
    is_active       BOOLEAN     NOT NULL DEFAULT TRUE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID, updated_by UUID
);

CREATE TABLE student_route_mappings (
    id         UUID    NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    student_id UUID    NOT NULL REFERENCES students(id),
    route_id   UUID    NOT NULL REFERENCES routes(id),
    vehicle_id UUID    REFERENCES vehicles(id),
    stop_name  VARCHAR(100),
    is_active  BOOLEAN NOT NULL DEFAULT TRUE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID, updated_by UUID
);
