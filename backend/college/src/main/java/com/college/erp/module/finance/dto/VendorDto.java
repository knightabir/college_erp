package com.college.erp.module.finance.dto;

import java.util.UUID;

import com.college.erp.module.finance.domain.Vendor;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class VendorDto {

    private UUID id;
    private String code;
    private String name;
    private String contactPerson;
    private String email;
    private String phone;
    private String gstNumber;
    private boolean active;

    public static VendorDto from(Vendor vendor) {
        return VendorDto.builder()
                .id(vendor.getId())
                .code(vendor.getCode())
                .name(vendor.getName())
                .contactPerson(vendor.getContactPerson())
                .email(vendor.getEmail())
                .phone(vendor.getPhone())
                .gstNumber(vendor.getGstNumber())
                .active(vendor.isActive())
                .build();
    }
}
