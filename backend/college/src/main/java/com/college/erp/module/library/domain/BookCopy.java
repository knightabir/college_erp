package com.college.erp.module.library.domain;

import com.college.erp.common.domain.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

/** 04-database-schema.md §11.3, 03-srs.md [LIB-002]. One barcode = one physical copy. */
@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(
    name = "book_copies",
    indexes = {
        @Index(name = "idx_copy_barcode", columnList = "barcode"),
        @Index(name = "idx_copy_title", columnList = "title_id")
    }
)
public class BookCopy extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "title_id", nullable = false)
    private BookTitle title;

    @Column(name = "barcode", nullable = false, unique = true, length = 50)
    private String barcode;

    @Column(name = "accession_number", nullable = false, unique = true, length = 30)
    private String accessionNumber;

    @Column(name = "shelf_location", length = 50)
    private String shelfLocation;

    @Column(name = "rack_number", length = 20)
    private String rackNumber;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "condition", nullable = false, length = 20)
    private CopyCondition condition = CopyCondition.GOOD;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CopyStatus status = CopyStatus.AVAILABLE;

    @Column(name = "barcode_image_file_id", length = 36)
    private String barcodeImageFileId;
}
