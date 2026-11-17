package com.just4guys.hlungwaneburial.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Beneficiary(
    val id: String,
    @SerialName("policy_id") val policyId: String,
    @SerialName("full_name") val fullName: String,
    val relationship: String,
    @SerialName("sa_id_number") val saIdNumber: String? = null,
    @SerialName("phone_number") val phoneNumber: String? = null
)