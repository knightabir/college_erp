package com.college.erp.module.library.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/** 05-api-documentation.md §8 POST /library/catalog/titles. */
@Getter
@Setter
public class CreateBookTitleRequest {

    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Author is required")
    private String author;

    private String isbn;
    private String edition;
    private String publisher;
    private Integer publicationYear;
    private String subjectClassification;
    private String language;
    private String description;
    private UUID departmentId;
}
