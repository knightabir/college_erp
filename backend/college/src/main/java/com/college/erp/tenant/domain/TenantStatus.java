package com.college.erp.tenant.domain;

/**
 * No cited requirement for this value set — inferred for basic SaaS tenant lifecycle management
 * (a platform operator needs some way to disable an abusive/non-paying tenant without deleting
 * their data). Deliberately minimal: no TRIAL/billing-plan states, since nothing in this system's
 * scope asked for billing — see Tenant's own javadoc.
 */
public enum TenantStatus {
    ACTIVE,
    SUSPENDED
}
