package com.ProyectoTGS.BackCore.Services;

import entities.TariffEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import repositories.TariffRepository;
import services.TariffService;

import java.util.Collections;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TariffServiceTest {

    @Mock
    private TariffRepository tariffRepository;

    @InjectMocks
    private TariffService tariffService;

    private TariffEntity currentTariff;

    @BeforeEach
    void setUp() {
        // Current tariff
        currentTariff = TariffEntity.builder()
                .id(1L)
                .dailyRentFee(5000)
                .dailyLateFee(2000)
                .repairFee(1500)
                .build();
    }

    // =======================================================================
    // EPIC 4: QUERY TARIFFS (getTariff)
    // =======================================================================

    @Test
    void getTariff_Success() {
        // ARRANGE: the database returns the current tariff
        when(tariffRepository.findAll()).thenReturn(List.of(currentTariff));

        // ACT
        TariffEntity result = tariffService.getTariff();

        // ASSERT
        assertNotNull(result);
        assertEquals(5000, result.getDailyRentFee());
    }

    @Test
    void getTariff_FailsIfNoTariffIsConfigured() {
        // ARRANGE: the database returns an empty list
        when(tariffRepository.findAll()).thenReturn(Collections.emptyList());

        // ACT & ASSERT: fails when no tariff is configured
        assertThrows(RuntimeException.class, () -> {
            tariffService.getTariff();
        }, "Must throw when no tariff is configured.");
    }

    // =======================================================================
    // EPIC 4: UPDATE TARIFFS (updateTariff)
    // =======================================================================

    @Test
    void updateTariff_Success() {
        // ARRANGE: new tariff values
        TariffEntity updatedValues = TariffEntity.builder()
                .dailyRentFee(6000)
                .dailyLateFee(3000)
                .repairFee(2500)
                .build();

        // MOCK: 1. current tariff, 2. persistence
        when(tariffRepository.findAll()).thenReturn(List.of(currentTariff));
        when(tariffRepository.save(any(TariffEntity.class))).thenReturn(currentTariff);

        // ACT
        TariffEntity result = tariffService.updateTariff(updatedValues);

        // ASSERT: the values of the existing entity (currentTariff) were updated
        assertEquals(6000, result.getDailyRentFee());
        assertEquals(3000, result.getDailyLateFee());
        verify(tariffRepository, times(1)).save(currentTariff);
    }

    @Test
    void updateTariff_FailsWhenRentFeeIsZero() {
        TariffEntity invalid = TariffEntity.builder().dailyRentFee(0).dailyLateFee(3000).repairFee(2500).build();

        assertThrows(IllegalArgumentException.class, () -> tariffService.updateTariff(invalid));
        verify(tariffRepository, never()).save(any());
    }

    @Test
    void updateTariff_FailsWhenLateFeeIsNegative() {
        TariffEntity invalid = TariffEntity.builder().dailyRentFee(1000).dailyLateFee(-1).repairFee(2500).build();

        assertThrows(IllegalArgumentException.class, () -> tariffService.updateTariff(invalid));
        verify(tariffRepository, never()).save(any());
    }

    @Test
    void updateTariff_FailsWhenRepairFeeIsMissing() {
        TariffEntity invalid = TariffEntity.builder().dailyRentFee(1000).dailyLateFee(3000).repairFee(null).build();

        assertThrows(IllegalArgumentException.class, () -> tariffService.updateTariff(invalid));
        verify(tariffRepository, never()).save(any());
    }

    @Test
    void updateTariff_AllowsZeroFees() {
        TariffEntity freeFees = TariffEntity.builder().dailyRentFee(1).dailyLateFee(0).repairFee(0).build();
        when(tariffRepository.findAll()).thenReturn(List.of(currentTariff));
        when(tariffRepository.save(currentTariff)).thenReturn(currentTariff);

        TariffEntity result = tariffService.updateTariff(freeFees);

        assertEquals(0, result.getDailyLateFee());
        assertEquals(0, result.getRepairFee());
    }

// =======================================================================
    // SPECIFIC GETTERS
    // =======================================================================

    @Test
    void getDailyLateFee_Success() {
        // ARRANGE: getTariff() returns the current tariff
        when(tariffRepository.findAll()).thenReturn(List.of(currentTariff));

        // ACT
        double fee = tariffService.getDailyLateFee();

        // ASSERT
        assertEquals(2000, fee); // value from setUp
    }

    @Test
    void getDailyRentFee_Success() {
        // ARRANGE: getTariff() returns the current tariff
        when(tariffRepository.findAll()).thenReturn(List.of(currentTariff));

        // ACT
        double fee = tariffService.getDailyRentFee();

        // ASSERT
        assertEquals(5000, fee); // value from setUp
    }

    @Test
    void getRepairFee_Success() {
        // ARRANGE: getTariff() returns the current tariff
        when(tariffRepository.findAll()).thenReturn(List.of(currentTariff));

        // ACT
        double fee = tariffService.getRepairFee();

        // ASSERT
        assertEquals(1500, fee); // value from setUp
    }
}