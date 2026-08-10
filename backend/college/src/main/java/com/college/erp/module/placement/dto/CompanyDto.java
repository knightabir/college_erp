package com.college.erp.module.placement.dto;

import java.util.UUID;

import com.college.erp.module.placement.domain.Company;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CompanyDto {

    private UUID id;
    private String name;
    private String industry;
    private String website;
    private String contactPerson;
    private String contactEmail;
    private String contactPhone;

    public static CompanyDto from(Company company) {
        return CompanyDto.builder()
                .id(company.getId())
                .name(company.getName())
                .industry(company.getIndustry())
                .website(company.getWebsite())
                .contactPerson(company.getContactPerson())
                .contactEmail(company.getContactEmail())
                .contactPhone(company.getContactPhone())
                .build();
    }
}
