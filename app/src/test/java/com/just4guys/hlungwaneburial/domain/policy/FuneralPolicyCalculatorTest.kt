package com.just4guys.hlungwaneburial.domain.policy

import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

class FuneralPolicyCalculatorTest {
    @Test fun `three calendar months handles February correctly`() {
        val start = LocalDate.of(2026, 1, 15)
        assertEquals(LocalDate.of(2026, 4, 15), FuneralPolicyCalculator.trialEndsOn(start))
        assertTrue(FuneralPolicyCalculator.withinThreeMonthTrial(start, LocalDate.of(2026, 4, 14)))
        assertFalse(FuneralPolicyCalculator.withinThreeMonthTrial(start, LocalDate.of(2026, 4, 15)))
    }
    @Test fun `calculates distinct unpaid months`() {
        val result = FuneralPolicyCalculator.arrears(BigDecimal("250.00"), listOf(YearMonth.of(2026,1), YearMonth.of(2026,2), YearMonth.of(2026,2)))
        assertEquals(BigDecimal("500.00"), result)
    }
    @Test fun `trial boundary is exclusive`() {
        val start = LocalDate.of(2026, 1, 1)
        assertTrue(FuneralPolicyCalculator.withinTrial(start, start.plusDays(29), 30))
        assertFalse(FuneralPolicyCalculator.withinTrial(start, start.plusDays(30), 30))
    }
    @Test fun `deducts trial charge and arrears in estimate`() {
        val result = FuneralPolicyCalculator.estimateClaim(BigDecimal("15000"), BigDecimal("500"), true)
        assertEquals(BigDecimal("13500.00"), result.estimatedNetBenefit)
    }
    @Test fun `net estimate never becomes negative`() {
        val result = FuneralPolicyCalculator.estimateClaim(BigDecimal("900"), BigDecimal("300"), true)
        assertEquals(BigDecimal("0.00"), result.estimatedNetBenefit)
        assertEquals(BigDecimal("400.00"), result.unappliedDeductions)
    }
    @Test(expected = IllegalArgumentException::class) fun `rejects negative benefits`() {
        FuneralPolicyCalculator.estimateClaim(BigDecimal("-1"), BigDecimal.ZERO, false)
    }
}
