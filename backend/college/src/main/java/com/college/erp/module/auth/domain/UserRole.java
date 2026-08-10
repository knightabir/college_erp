package com.college.erp.module.auth.domain;

/**
 * NOT YET COMPLETE: 02-prd.md §6 references a separate "Roles & Permissions" document (not
 * present in this repository) that presumably enumerates the full role catalog — 08-system-
 * architecture.md's file tree comments "Enum: all 24 roles". Only the roles explicitly named
 * across the available docs (frontend shell layouts in 08-system-architecture.md §11, personas in
 * 02-prd.md, auth notes in 05-api-documentation.md — e.g. FEE_COLLECTION_CLERK only surfaces
 * there, on POST /finance/fees/invoices/{id}/collect) are listed below. Extend this enum once the
 * remaining roles are specified — adding a new value here needs no migration (role is a plain
 * VARCHAR column, see V001), just a matching RoleTemplate entry if the new role should get
 * unconditional (Layer 1) permissions.
 */
public enum UserRole {
    SUPER_ADMIN,
    ADMIN,
    IT_ADMIN,
    HOD,
    FACULTY,
    LAB_ASSISTANT,
    EXAM_CELL,
    HEAD_ACCOUNTANT,
    ACCOUNTANT,
    FEE_COLLECTION_CLERK,
    AUDITOR,
    CHIEF_LIBRARIAN,
    LIBRARY_ASSISTANT,
    ASSET_MANAGER,
    MAINTENANCE_STAFF,
    HR_MANAGER,
    ADMISSIONS_OFFICER,
    PLACEMENT_OFFICER,
    HOSTEL_WARDEN,
    TRANSPORT_MANAGER,
    STUDENT,
    PARENT,
    ALUMNI
}
