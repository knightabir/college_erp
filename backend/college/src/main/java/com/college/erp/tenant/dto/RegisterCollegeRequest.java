package com.college.erp.tenant.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * "Register your college" — the one public, unauthenticated endpoint in this entire system
 * (POST /api/v1/register, see RegistrationController). Combines the college's own details with
 * its first administrator's account details in a single call: submitting this creates a new
 * {@code Tenant} row and that tenant's own SUPER_ADMIN account together, one transaction, and logs
 * the new admin straight in (see RegisterCollegeResponse) — the standard "sign up and land inside
 * your own workspace" SaaS flow.
 *
 * <p>No planning document specifies this endpoint or payload shape at all — multi-tenancy was
 * named only as a one-paragraph future-cloud-mode aspiration in 08-system-architecture.md §5.2,
 * with zero DDL/FRs/endpoints anywhere else (see V017's header comment). Field list here is this
 * session's best-effort "college details a single college must have", chosen for parity with
 * Tenant's own column list — not transcribed from a spec.
 */
@Getter
@Setter
public class RegisterCollegeRequest {

    // ---- College details ----

    @NotBlank
    @Size(max = 200)
    private String collegeName;

    /** Short human-friendly code/slug, e.g. "srcem" — must be unique across the whole platform. */
    @NotBlank
    @Size(max = 30)
    private String collegeCode;

    private String affiliatedUniversity;

    private String accreditation;

    private Integer establishedYear;

    private String address;

    private String city;

    private String state;

    private String pincode;

    @NotBlank
    @Email
    private String collegeContactEmail;

    private String collegeContactPhone;

    private String website;

    // ---- First administrator's account (this tenant's own SUPER_ADMIN) ----

    @NotBlank
    @Size(max = 50)
    private String adminUsername;

    @NotBlank
    @Email
    private String adminEmail;

    @NotBlank
    @Size(max = 150)
    private String adminFullName;

    @NotBlank
    @Size(min = 8)
    private String adminPassword;

    private String adminPhone;
}
