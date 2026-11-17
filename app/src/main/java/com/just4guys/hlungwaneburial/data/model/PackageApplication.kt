package com.just4guys.hlungwaneburial.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PackageApplication(
    @SerialName("user_id") val userId: String,
    @SerialName("option_number") val optionNumber: Int
)
