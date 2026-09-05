package com.example.travenorcustomer.data

import com.example.travenorcustomer.model.Destination
import com.example.travenorcustomer.network.Constants
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class DestinationsRepositoryImpl(
    private val supabaseClient: SupabaseClient
) : DestinationsRepository {

    override fun getAllDestination(): Flow<List<Destination>> = flow {
        val destinations = supabaseClient
            .from(Constants.ALL_DESTINATION)
            .select {
                filter {
                    eq(Constants.IS_ACTIVE, true)
                }
            }
            .decodeList<Destination>()

        emit(destinations)
    }

    override fun getDestination(id: String): Flow<Destination> = flow {
        val destinations = supabaseClient
            .from(Constants.ALL_DESTINATION)
            .select {
                filter {
                    eq("id", id)
                }
            }
            .decodeSingle<Destination>()

        emit(destinations)
    }

}