package com.example.travenorcustomer.data

import android.util.Log
import android.util.Log.*
import com.example.travenorcustomer.model.Booking
import com.example.travenorcustomer.network.Constants
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.annotations.SupabaseExperimental
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.decodeRecord
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import io.github.jan.supabase.realtime.selectAsFlow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement


class BookingRepositoryImpl(
    private val supabaseClient: SupabaseClient,
    private val realtimeChannel: RealtimeChannel
) : BookingRepository {

    private var realtimeFlow: Flow<Booking>? = null

    override suspend fun bookingStatus(): Flow<Booking>? {
        if (realtimeChannel.status.value != RealtimeChannel.Status.SUBSCRIBED) {

            realtimeFlow = realtimeChannel.postgresChangeFlow<PostgresAction.Update>(
                schema = Constants.PUBLIC_SCHEME
            ) {
                table = Constants.BOOKING
            }.map { event ->
                event.decodeRecord<Booking>()
            }

            realtimeChannel.subscribe()

        }
        return realtimeFlow
    }


    override suspend fun requestBooking(booking: Booking): Booking = supabaseClient
        .from(Constants.BOOKING)
        .insert(booking) {
            select()
        }
        .decodeSingle<Booking>()

}