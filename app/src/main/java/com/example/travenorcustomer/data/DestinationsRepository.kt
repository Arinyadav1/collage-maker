package com.example.travenorcustomer.data

import com.example.travenorcustomer.model.Destination
import kotlinx.coroutines.flow.Flow

interface DestinationsRepository {
    fun getAllDestination(): Flow<List<Destination>>
    fun getDestination(id : String): Flow<Destination>
}