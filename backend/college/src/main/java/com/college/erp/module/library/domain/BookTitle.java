package com.college.erp.module.library.domain;

import com.college.erp.common.domain.BaseEntity;
import com.college.erp.module.academic.domain.Department;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 04-database-schema.md §11.2, 03-srs.md [LIB-001]. total_copies/available_copies are derived
 *  counts, kept in sync by CatalogService/CirculationService — never set directly by callers. */
@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(
    name = "book_titles",
    indexes = {
        @Index(name = "idx_book_isbn", columnList = "isbn"),
        @Index(name = "idx_book_subject", columnList = "subject_classification")
    }
)
public class BookTitle extends BaseEntity {

    @Column(name = "title", nullable = false, columnDefinition = "TEXT")
    private String title;

    @Column(name = "author", nullable = false, length = 300)
    private String author;

    @Column(name = "isbn", length = 20)
    private String isbn;

    @Column(name = "edition", length = 50)
    private String edition;

    @Column(name = "publisher", length = 200)
    private String publisher;

    @Column(name = "publication_year")
    private Integer publicationYear;

    @Column(name = "subject_classification", length = 100)
    private String subjectClassification;

    @Builder.Default
    @Column(name = "language", nullable = false, length = 30)
    private String language = "English";

    @Column(name = "cover_image_file_id", length = 36)
    private String coverImageFileId;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    @Builder.Default
    @Column(name = "total_copies", nullable = false)
    private int totalCopies = 0;

    @Builder.Default
    @Column(name = "available_copies", nullable = false)
    private int availableCopies = 0;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    public void incrementCopyCounts() {
        totalCopies++;
        availableCopies++;
    }

    public void adjustAvailableCopies(int delta) {
        availableCopies += delta;
    }
}
