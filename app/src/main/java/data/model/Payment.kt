package com.just4guys.hlungwaneburial.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Payment(
    val id: String,
    @SerialName("policy_id") val policyId: String,
    val amount: Double,
    @SerialName("payment_date") val paymentDate: String,
    val method: String,
    val status: String,
    val reference: String? = null
)