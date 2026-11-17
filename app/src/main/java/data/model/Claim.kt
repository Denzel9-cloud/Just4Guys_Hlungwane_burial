package com.just4guys.hlungwaneburial.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Claim(
    val id: String,
    @SerialName("policy_id") val policyId: String,
    @SerialName("claim_number") val claimNumber: String,
    @SerialName("deceased_full_name") val deceasedFullName: String,
    @SerialName("date_of_death") val dateOfDeath: String,
    @SerialName("claim_amount") val claimAmount: Double,
    val status: String,
    @SerialName("submitted_at") val submittedAt: String
)