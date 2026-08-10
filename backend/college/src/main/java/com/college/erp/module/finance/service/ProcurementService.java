package com.college.erp.module.finance.service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.college.erp.audit.AuditAction;
import com.college.erp.audit.AuditLogWriter;
import com.college.erp.common.exception.BusinessRuleViolationException;
import com.college.erp.common.exception.DuplicateResourceException;
import com.college.erp.common.exception.ResourceNotFoundException;
import com.college.erp.module.finance.domain.FinancialYear;
import com.college.erp.module.finance.domain.PurchaseOrder;
import com.college.erp.module.finance.domain.PurchaseOrderRepository;
import com.college.erp.module.finance.domain.PurchaseOrderStatus;
import com.college.erp.module.finance.domain.Vendor;
import com.college.erp.module.finance.domain.VendorRepository;
import com.college.erp.module.finance.dto.CreatePurchaseOrderRequest;
import com.college.erp.module.finance.dto.CreateVendorRequest;
import com.college.erp.module.finance.dto.PurchaseOrderDto;
import com.college.erp.module.finance.dto.VendorDto;

import lombok.RequiredArgsConstructor;

/**
 * 03-srs.md [FIN-010]. Three-way match (PO → GRN → vendor invoice) isn't implemented — no GRN
 * (goods-received-note) table anywhere in 04-database-schema.md, same undocumented-entity
 * situation as ExamSchedule/SeatingAllocation. PO create→approve is built; receiving isn't.
 */
@Service
@RequiredArgsConstructor
public class ProcurementService {

    private static final String MODULE = "FINANCE";

    private final VendorRepository vendorRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final FinancialYearService financialYearService;
    private final SegregationOfDutiesGuard segregationOfDutiesGuard;
    private final AuditLogWriter auditLogWriter;

    @Transactional
    public VendorDto createVendor(CreateVendorRequest request) {
        String code = request.getCode().toUpperCase();
        if (vendorRepository.existsByCodeAndDeletedFalse(code)) {
            throw new DuplicateResourceException("Vendor with code '" + code + "' already exists");
        }

        Vendor vendor = Vendor.builder()
                .code(code)
                .name(request.getName())
                .contactPerson(request.getContactPerson())
                .email(request.getEmail())
                .phone(request.getPhone())
                .address(request.getAddress())
                .gstNumber(request.getGstNumber())
                .panNumber(request.getPanNumber())
                .bankAccountNumber(request.getBankAccountNumber())
                .ifscCode(request.getIfscCode())
                .build();

        Vendor saved = vendorRepository.save(vendor);
        auditLogWriter.write(AuditAction.CREATE, MODULE, "Vendor", saved.getId().toString(),
                "Created: " + saved.getName());

        return VendorDto.from(saved);
    }

    @Transactional(readOnly = true)
    public List<VendorDto> listActiveVendors() {
        return vendorRepository.findByActiveTrueAndDeletedFalse().stream()
                .map(VendorDto::from)
                .toList();
    }

    /** For other modules that need the managed entity — see BatchService.getBatchEntity javadoc. */
    @Transactional(readOnly = true)
    public Vendor getVendorEntity(UUID id) {
        return vendorRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor", id));
    }

    @Transactional
    public PurchaseOrderDto createPurchaseOrder(CreatePurchaseOrderRequest request, UUID createdByUserId) {
        Vendor vendor = vendorRepository.findByIdAndDeletedFalse(request.getVendorId())
                .orElseThrow(() -> new ResourceNotFoundException("Vendor", request.getVendorId()));
        FinancialYear financialYear = financialYearService.getCurrentEntity();

        PurchaseOrder po = PurchaseOrder.builder()
                .poNumber(generatePoNumber())
                .vendor(vendor)
                .departmentId(request.getDepartmentId())
                .financialYear(financialYear)
                .totalAmount(request.getTotalAmount())
                .description(request.getDescription())
                .poDate(request.getPoDate())
                .expectedDeliveryDate(request.getExpectedDeliveryDate())
                .status(PurchaseOrderStatus.PENDING_APPROVAL)
                .build();
        po.setCreatedBy(createdByUserId);

        PurchaseOrder saved = purchaseOrderRepository.save(po);
        auditLogWriter.write(AuditAction.CREATE, MODULE, "PurchaseOrder", saved.getId().toString(),
                "Created PO " + saved.getPoNumber() + " for vendor " + vendor.getName());

        return PurchaseOrderDto.from(saved);
    }

    @Transactional
    public PurchaseOrderDto approve(UUID id, UUID approverUserId) {
        PurchaseOrder po = purchaseOrderRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("PurchaseOrder", id));

        if (po.getStatus() != PurchaseOrderStatus.PENDING_APPROVAL) {
            throw new BusinessRuleViolationException("Purchase order is not pending approval");
        }
        segregationOfDutiesGuard.assertNotSelfApproval(po.getCreatedBy(), approverUserId, po.getTotalAmount());

        po.setStatus(PurchaseOrderStatus.APPROVED);
        po.setApprovedByUserId(approverUserId);
        PurchaseOrder saved = purchaseOrderRepository.save(po);

        auditLogWriter.write(AuditAction.UPDATE, MODULE, "PurchaseOrder", id.toString(),
                "Approved PO " + saved.getPoNumber());

        return PurchaseOrderDto.from(saved);
    }

    @Transactional(readOnly = true)
    public List<PurchaseOrderDto> listByVendor(UUID vendorId) {
        return purchaseOrderRepository.findByVendorIdAndDeletedFalse(vendorId).stream()
                .map(PurchaseOrderDto::from)
                .toList();
    }

    private String generatePoNumber() {
        return "PO-" + LocalDate.now().getYear() + "-"
                + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }
}
