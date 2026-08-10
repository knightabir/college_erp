package com.college.erp.module.placement.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.college.erp.module.placement.domain.Company;
import com.college.erp.module.placement.domain.DriveApplication;
import com.college.erp.module.placement.domain.DriveApplicationRepository;
import com.college.erp.module.placement.domain.OfferStatus;
import com.college.erp.module.placement.domain.PlacementDrive;
import com.college.erp.module.placement.domain.PlacementOffer;
import com.college.erp.module.placement.domain.PlacementOfferRepository;
import com.college.erp.module.placement.dto.PlacementStatsDto;
import com.college.erp.module.student.domain.Student;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/** INVENTED module — see V016__create_placement_tables.sql's header comment. */
@ExtendWith(MockitoExtension.class)
class PlacementAnalyticsServiceTest {

    @Mock
    private DriveApplicationRepository driveApplicationRepository;
    @Mock
    private PlacementOfferRepository placementOfferRepository;

    private PlacementAnalyticsService placementAnalyticsService;

    private DriveApplication buildApplication() {
        Company company = Company.builder().name("Acme").build();
        company.setId(UUID.randomUUID());
        PlacementDrive drive = PlacementDrive.builder().company(company).title("SDE").build();
        drive.setId(UUID.randomUUID());
        Student student = Student.builder().enrollmentNumber("2024CSE00" + Math.random())
                .firstName("A").lastName("B").build();
        student.setId(UUID.randomUUID());
        DriveApplication application = DriveApplication.builder().drive(drive).student(student).build();
        application.setId(UUID.randomUUID());
        return application;
    }

    @Test
    @DisplayName("getStatsForDrive() — averages and maxes CTC across accepted/offered offers")
    void getStatsForDrive_computesAverageAndHighestCtc() {
        // ARRANGE
        placementAnalyticsService = new PlacementAnalyticsService(driveApplicationRepository, placementOfferRepository);
        UUID driveId = UUID.randomUUID();
        DriveApplication app1 = buildApplication();
        DriveApplication app2 = buildApplication();
        DriveApplication app3 = buildApplication(); // no offer

        PlacementOffer offer1 = PlacementOffer.builder().driveApplication(app1)
                .ctc(BigDecimal.valueOf(1200000)).status(OfferStatus.ACCEPTED)
                .offerDate(java.time.LocalDate.now()).build();
        PlacementOffer offer2 = PlacementOffer.builder().driveApplication(app2)
                .ctc(BigDecimal.valueOf(1800000)).status(OfferStatus.OFFERED)
                .offerDate(java.time.LocalDate.now()).build();

        when(driveApplicationRepository.findByDriveIdAndDeletedFalse(driveId))
                .thenReturn(List.of(app1, app2, app3));
        when(placementOfferRepository.findByDriveApplicationIdAndDeletedFalse(app1.getId()))
                .thenReturn(Optional.of(offer1));
        when(placementOfferRepository.findByDriveApplicationIdAndDeletedFalse(app2.getId()))
                .thenReturn(Optional.of(offer2));
        when(placementOfferRepository.findByDriveApplicationIdAndDeletedFalse(app3.getId()))
                .thenReturn(Optional.empty());

        // ACT
        PlacementStatsDto stats = placementAnalyticsService.getStatsForDrive(driveId);

        // ASSERT
        assertThat(stats.getTotalDriveApplications()).isEqualTo(3);
        assertThat(stats.getTotalOffers()).isEqualTo(2);
        assertThat(stats.getAcceptedOffers()).isEqualTo(1);
        assertThat(stats.getAverageCtc()).isEqualByComparingTo("1500000.00");
        assertThat(stats.getHighestCtc()).isEqualByComparingTo("1800000");
    }

    @Test
    @DisplayName("getStatsForDrive() — no offers at all: zeroed stats, no division-by-zero")
    void getStatsForDrive_noOffers_returnsZeroedStats() {
        // ARRANGE
        placementAnalyticsService = new PlacementAnalyticsService(driveApplicationRepository, placementOfferRepository);
        UUID driveId = UUID.randomUUID();
        DriveApplication app1 = buildApplication();
        when(driveApplicationRepository.findByDriveIdAndDeletedFalse(driveId)).thenReturn(List.of(app1));
        when(placementOfferRepository.findByDriveApplicationIdAndDeletedFalse(app1.getId()))
                .thenReturn(Optional.empty());

        // ACT
        PlacementStatsDto stats = placementAnalyticsService.getStatsForDrive(driveId);

        // ASSERT
        assertThat(stats.getTotalOffers()).isZero();
        assertThat(stats.getAverageCtc()).isEqualByComparingTo("0");
        assertThat(stats.getHighestCtc()).isEqualByComparingTo("0");
    }
}
