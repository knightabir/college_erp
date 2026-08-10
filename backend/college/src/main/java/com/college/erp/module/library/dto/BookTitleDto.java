package com.college.erp.module.library.dto;

import java.util.UUID;

import com.college.erp.module.library.domain.BookTitle;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BookTitleDto {

    private UUID id;
    private String title;
    private String author;
    private String isbn;
    private String edition;
    private String publisher;
    private Integer publicationYear;
    private String subjectClassification;
    private String language;
    private String description;
    private UUID departmentId;
    private int totalCopies;
    private int availableCopies;
    private boolean active;

    public static BookTitleDto from(BookTitle title) {
        return BookTitleDto.builder()
                .id(title.getId())
                .title(title.getTitle())
                .author(title.getAuthor())
                .isbn(title.getIsbn())
                .edition(title.getEdition())
                .publisher(title.getPublisher())
                .publicationYear(title.getPublicationYear())
                .subjectClassification(title.getSubjectClassification())
                .language(title.getLanguage())
                .description(title.getDescription())
                .departmentId(title.getDepartment() == null ? null : title.getDepartment().getId())
                .totalCopies(title.getTotalCopies())
                .availableCopies(title.getAvailableCopies())
                .active(title.isActive())
                .build();
    }
}
