package com.college.erp.module.academic.dto;

import java.util.UUID;

import com.college.erp.module.academic.domain.SubjectOffering;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SubjectOfferingDto {

    private UUID id;
    private UUID batchId;
    private UUID subjectId;
    private String subjectName;
    private UUID termId;
    private int maxInternalMarks;
    private int maxExternalMarks;
    private int passMarksInternal;
    private int passMarksExternal;
    private boolean elective;

    public static SubjectOfferingDto from(SubjectOffering offering) {
        return SubjectOfferingDto.builder()
                .id(offering.getId())
                .batchId(offering.getBatch().getId())
                .subjectId(offering.getSubject().getId())
                .subjectName(offering.getSubject().getName())
                .termId(offering.getTerm().getId())
                .maxInternalMarks(offering.getMaxInternalMarks())
                .maxExternalMarks(offering.getMaxExternalMarks())
                .passMarksInternal(offering.getPassMarksInternal())
                .passMarksExternal(offering.getPassMarksExternal())
                .elective(offering.isElective())
                .build();
    }
}
