package com.college.erp.module.library.service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.college.erp.audit.AuditAction;
import com.college.erp.audit.AuditLogWriter;
import com.college.erp.common.exception.BusinessRuleViolationException;
import com.college.erp.common.exception.ResourceNotFoundException;
import com.college.erp.module.academic.domain.Department;
import com.college.erp.module.academic.service.DepartmentService;
import com.college.erp.module.library.domain.BookCopy;
import com.college.erp.module.library.domain.BookCopyRepository;
import com.college.erp.module.library.domain.BookTitle;
import com.college.erp.module.library.domain.BookTitleRepository;
import com.college.erp.module.library.domain.CopyCondition;
import com.college.erp.module.library.domain.CopyStatus;
import com.college.erp.module.library.dto.BookCopyDto;
import com.college.erp.module.library.dto.BookTitleDto;
import com.college.erp.module.library.dto.CreateBookCopyRequest;
import com.college.erp.module.library.dto.CreateBookTitleRequest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** 03-srs.md [LIB-001]/[LIB-002], 05-api-documentation.md §8 "Catalog". */
@Slf4j
@Service
@RequiredArgsConstructor
public class CatalogService {

    private static final String MODULE = "LIBRARY";

    private final BookTitleRepository bookTitleRepository;
    private final BookCopyRepository bookCopyRepository;
    private final DepartmentService departmentService;
    private final BarcodeService barcodeService;
    private final AuditLogWriter auditLogWriter;

    @Transactional
    public BookTitleDto createTitle(CreateBookTitleRequest request) {
        Department department = request.getDepartmentId() == null
                ? null : departmentService.getDepartmentEntity(request.getDepartmentId());

        BookTitle title = BookTitle.builder()
                .title(request.getTitle())
                .author(request.getAuthor())
                .isbn(request.getIsbn())
                .edition(request.getEdition())
                .publisher(request.getPublisher())
                .publicationYear(request.getPublicationYear())
                .subjectClassification(request.getSubjectClassification())
                .language(request.getLanguage() == null || request.getLanguage().isBlank()
                        ? "English" : request.getLanguage())
                .description(request.getDescription())
                .department(department)
                .build();

        BookTitle saved = bookTitleRepository.save(title);
        auditLogWriter.write(AuditAction.CREATE, MODULE, "BookTitle", saved.getId().toString(),
                "Added title: " + saved.getTitle());

        return BookTitleDto.from(saved);
    }

    @Transactional(readOnly = true)
    public Page<BookTitleDto> search(String query, Pageable pageable) {
        return bookTitleRepository.search(query, pageable).map(BookTitleDto::from);
    }

    @Transactional(readOnly = true)
    public BookTitleDto getTitle(UUID id) {
        return BookTitleDto.from(getTitleEntity(id));
    }

    @Transactional
    public BookCopyDto addCopy(UUID titleId, CreateBookCopyRequest request) {
        BookTitle title = getTitleEntity(titleId);

        String accessionNumber = generateAccessionNumber();
        String barcode = "LIB-" + accessionNumber;

        BookCopy copy = BookCopy.builder()
                .title(title)
                .barcode(barcode)
                .accessionNumber(accessionNumber)
                .shelfLocation(request.getShelfLocation())
                .rackNumber(request.getRackNumber())
                .build();

        BookCopy saved = bookCopyRepository.save(copy);
        title.incrementCopyCounts();
        bookTitleRepository.save(title);

        auditLogWriter.write(AuditAction.CREATE, MODULE, "BookCopy", saved.getId().toString(),
                "Added copy " + barcode + " for title " + title.getTitle());
        log.info("Book copy added: [{}] for title [{}]", barcode, title.getTitle());

        return BookCopyDto.from(saved);
    }

    @Transactional(readOnly = true)
    public List<BookCopyDto> listCopies(UUID titleId) {
        return bookCopyRepository.findByTitleIdAndDeletedFalse(titleId).stream()
                .map(BookCopyDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public BookCopyDto getByBarcode(String barcode) {
        return BookCopyDto.from(getCopyEntityByBarcode(barcode));
    }

    /** Regenerated on demand from the barcode string, never persisted — module.storage (which
     *  barcode_image_file_id implies) doesn't exist yet, and there's no need for it: a Code 128
     *  barcode is fully reconstructible from its content string alone. */
    @Transactional(readOnly = true)
    public byte[] getBarcodeImage(UUID copyId) {
        BookCopy copy = getCopyEntity(copyId);
        return barcodeService.generateCode128Png(copy.getBarcode());
    }

    @Transactional
    public void markLost(UUID copyId) {
        BookCopy copy = getCopyEntity(copyId);
        if (copy.getStatus() == CopyStatus.ISSUED) {
            throw new BusinessRuleViolationException("Cannot mark a currently-issued copy as lost directly — process the return first");
        }

        boolean wasAvailable = copy.getStatus() == CopyStatus.AVAILABLE;
        copy.setStatus(CopyStatus.LOST);
        copy.setCondition(CopyCondition.LOST);
        bookCopyRepository.save(copy);

        if (wasAvailable) {
            BookTitle title = copy.getTitle();
            title.adjustAvailableCopies(-1);
            bookTitleRepository.save(title);
        }

        auditLogWriter.write(AuditAction.BOOK_MARKED_LOST, MODULE, "BookCopy", copyId.toString(),
                "Marked lost: " + copy.getBarcode());
    }

    BookTitle getTitleEntity(UUID id) {
        return bookTitleRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("BookTitle", id));
    }

    BookCopy getCopyEntity(UUID id) {
        return bookCopyRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("BookCopy", id));
    }

    BookCopy getCopyEntityByBarcode(String barcode) {
        return bookCopyRepository.findByBarcodeAndDeletedFalse(barcode)
                .orElseThrow(() -> new ResourceNotFoundException("BookCopy with barcode", barcode));
    }

    /** Sequential-looking (not a real DB sequence — count()+1, acceptable at this scale, unlike
     *  invoice/receipt numbers which use random suffixes specifically to avoid the race this has). */
    private String generateAccessionNumber() {
        long sequence = bookCopyRepository.count() + 1;
        return LocalDate.now().getYear() + "-" + String.format("%06d", sequence);
    }
}
