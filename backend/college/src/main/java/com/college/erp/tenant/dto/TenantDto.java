package com.college.erp.tenant.dto;

import java.util.UUID;

import com.college.erp.tenant.domain.Tenant;
import com.college.erp.tenant.domain.TenantStatus;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TenantDto {

    private UUID id;
    private String name;
    private String code;
    private String affiliatedUniversity;
    private String accreditation;
    private Integer establishedYear;
    private String city;
    private String state;
    private String country;
    private String contactEmail;
    private String contactPhone;
    private String website;
    private String timezone;
    private TenantStatus status;

    public static TenantDto from(Tenant tenant) {
        return TenantDto.builder()
                .id(tenant.getId())
                .name(tenant.getName())
                .code(tenant.getCode())
                .affiliatedUniversity(tenant.getAffiliatedUniversity())
                .accreditation(tenant.getAccreditation())
                .establishedYear(tenant.getEstablishedYear())
                .city(tenant.getCity())
                .state(tenant.getState())
                .country(tenant.getCountry())
                .contactEmail(tenant.getContactEmail())
                .contactPhone(tenant.getContactPhone())
                .website(tenant.getWebsite())
                .timezone(tenant.getTimezone())
                .status(tenant.getStatus())
                .build();
    }
}
