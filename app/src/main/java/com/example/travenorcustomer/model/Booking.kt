package com.example.travenorcustomer.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Booking(
    val id: String? = null,

    val status: String? = null,

    @SerialName("user_id")
    val userId: String? = null,

    @SerialName("destination_id")
    val destinationId: String? = null,

    @SerialName("owner_id")
    val ownerId: String? = null
)

enum class Status{
    Accept,
    Reject
}

