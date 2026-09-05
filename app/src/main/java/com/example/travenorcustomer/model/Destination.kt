package com.example.travenorcustomer.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class Destination(
    val id: String,

    @SerialName("destination_name")
    val destinationName: String,

    val city: String,
    val country: String,

    val rating: Double,

    @SerialName("total_review")
    val totalReview: Int,

    @SerialName("price_per_person")
    val pricePerPerson: Int,

    val description: String,

    @SerialName("cover_image")
    val coverImage: String,

    @SerialName("owner_id")
    val ownerId: String,

    val images: List<DestinationImage>,

    @SerialName("is_active")
    val isActive : Boolean
)


@Serializable
data class DestinationImage(
    val id: String,
    val image: String
)

