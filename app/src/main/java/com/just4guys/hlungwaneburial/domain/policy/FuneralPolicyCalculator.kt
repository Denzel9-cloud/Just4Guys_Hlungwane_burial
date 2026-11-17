package com.just4guys.hlungwaneburial.domain.policy

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

/** Pure calculations only. Never approve claims or execute payouts from an Android client. */
object FuneralPolicyCalculator {
    private val ZERO = BigDecimal("0.00")
    private fun money(value: BigDecimal) = value.setScale(2, RoundingMode.HALF_UP)

    /** Calculate overdue premiums using explicit unpaid billing months, not a guessed policy age. */
    fun arrears(monthlyPremium: BigDecimal, unpaidMonths: Collection<YearMonth>): BigDecimal {
        require(monthlyPremium >= BigDecimal.ZERO) { "Premium cannot be negative" }
        return money(monthlyPremium.multiply(BigDecimal(unpaidMonths.distinct().size)))
    }

    /** Trial/waiting-period rules are configurable and must be approved by staff/legal. */
    fun withinTrial(startDate: LocalDate, eventDate: LocalDate, trialDays: Long): Boolean {
        require(trialDays >= 0) { "Trial period cannot be negative" }
        require(!eventDate.isBefore(startDate)) { "Event precedes policy start" }
        return ChronoUnit.DAYS.between(startDate, eventDate) < trialDays
    }

    /** Three calendar months, not a fixed 90-day approximation. */
    fun trialEndsOn(startDate: LocalDate): LocalDate = startDate.plusMonths(3)

    fun withinThreeMonthTrial(startDate: LocalDate, eventDate: LocalDate): Boolean {
        require(!eventDate.isBefore(startDate)) { "Event precedes policy start" }
        return eventDate.isBefore(trialEndsOn(startDate))
    }

    data class ClaimEstimate(
        val grossBenefit: BigDecimal,
        val trialDeduction: BigDecimal,
        val arrearsDeduction: BigDecimal,
        val estimatedNetBenefit: BigDecimal,
        val unappliedDeductions: BigDecimal
    )

    /** Preview only. Assumes deductions are permitted by the applicable signed policy. */
    fun estimateClaim(
        grossBenefit: BigDecimal,
        arrears: BigDecimal,
        isTrialClaim: Boolean,
        trialDeductionPerClaim: BigDecimal = BigDecimal("1000.00")
    ): ClaimEstimate {
        require(grossBenefit >= BigDecimal.ZERO && arrears >= BigDecimal.ZERO && trialDeductionPerClaim >= BigDecimal.ZERO) {
            "Amounts cannot be negative"
        }
        val gross = money(grossBenefit)
        val trial = if (isTrialClaim) money(trialDeductionPerClaim) else ZERO
        val overdue = money(arrears)
        val requestedDeductions = trial.add(overdue)
        val net = gross.subtract(requestedDeductions).max(ZERO)
        return ClaimEstimate(gross, trial, overdue, money(net), money(requestedDeductions.subtract(gross).max(ZERO)))
    }
}
