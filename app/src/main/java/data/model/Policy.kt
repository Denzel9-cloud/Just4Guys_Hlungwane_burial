package com.just4guys.hlungwaneburial.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Policy(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("policy_number") val policyNumber: String,
    @SerialName("policy_type") val policyType: String,
    @SerialName("cover_amount") val coverAmount: Double,
    @SerialName("monthly_premium") val monthlyPremium: Double,
    val status: String,
    @SerialName("start_date") val startDate: String
)