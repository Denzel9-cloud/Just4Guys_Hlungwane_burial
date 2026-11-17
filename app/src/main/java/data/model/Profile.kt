package com.just4guys.hlungwaneburial.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Profile(
    val id: String,
    @SerialName("full_name") val fullName: String? = null, // Made nullable just in case
    val email: String? = null,
    @SerialName("sa_id_number") val saIdNumber: String? = null,
    @SerialName("phone_number") val phoneNumber: String? = null,
    val province: String? = null,
    val role: String = "family"
)