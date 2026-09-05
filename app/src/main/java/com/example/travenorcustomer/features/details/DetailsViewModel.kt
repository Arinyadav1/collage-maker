package com.example.travenorcustomer.features.details

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.example.travenorcustomer.data.BookingRepository
import com.example.travenorcustomer.data.DestinationsRepository
import com.example.travenorcustomer.features.BaseViewModel
import com.example.travenorcustomer.features.navigation.DetailScreenRoute
import com.example.travenorcustomer.model.Booking
import com.example.travenorcustomer.model.Destination
import com.example.travenorcustomer.model.Status
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DetailsViewModel(
    private val destinationsRepository: DestinationsRepository,
    private val bookingRepository: BookingRepository,
    val savedStateHandle: SavedStateHandle
) : BaseViewModel<DetailsState, DetailsAction, DetailsEvent>(initialState = DetailsState()) {

    private val route = savedStateHandle.toRoute<DetailScreenRoute>()

    init {
        getDestination(route.destinationId)
        bookingStatus()
    }

    private fun bookingStatus() {
        viewModelScope.launch {
            bookingRepository.bookingStatus()
                ?.catch { }
                ?.collect { booking ->
                    Log.d("ARINYADAV", booking.toString())
                    if (
                        Status.Accept.name == booking.status && state.requestId == booking.id
                    ) {
                        sendEvent(DetailsEvent.OnAcceptBooking)
                    } else {
                        sendEvent(DetailsEvent.OnNavigateBack)
                    }
                }

        }
    }

    private fun requestBooking(booking: Booking) {

        sendEvent(DetailsEvent.OnRequestBooking)

        viewModelScope.launch {
            try {
                mutableStateFlow.update {
                    it.copy(
                        requestId = bookingRepository.requestBooking(booking).id
                    )
                }
            } catch (e: Throwable) {

            }
        }

    }

    private fun getDestination(id: String) {
        viewModelScope.launch {
            destinationsRepository.getDestination(id)
                .onStart {
                    mutableStateFlow.update {
                        it.copy(
                            isLoading = true
                        )
                    }
                }
                .catch { error ->

                }
                .collect { data ->
                    mutableStateFlow.update {
                        it.copy(
                            destination = data,
                            isLoading = false
                        )
                    }
                }
        }
    }

    override fun handleAction(action: DetailsAction) {
        when (action) {
            DetailsAction.OnRequestBooking -> {
                requestBooking(
                    booking = Booking(
                        userId = route.userId,
                        ownerId = state.destination?.ownerId,
                        destinationId = state.destination?.id
                    )
                )
            }

            DetailsAction.OnNavigateBack -> {
                sendEvent(DetailsEvent.OnNavigateBack)
            }
        }

    }

}


data class DetailsState(
    val destination: Destination? = null,
    val isLoading: Boolean = false,
    val requestId: String? = null,
)

sealed interface DetailsAction {
    object OnRequestBooking : DetailsAction
    object OnNavigateBack : DetailsAction
}

sealed interface DetailsEvent {
    object OnNavigateBack : DetailsEvent
    object OnAcceptBooking : DetailsEvent
    object OnRequestBooking : DetailsEvent
}

enum class UiStatus(val value: String) {
    ACCEPTED("Accepted"),
    REQUESTING("Requesting..."),
    BOOK_NOW("Book Now")
}