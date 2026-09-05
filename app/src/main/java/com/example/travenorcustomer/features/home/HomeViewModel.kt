package com.example.travenorcustomer.features.home

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.example.travenorcustomer.data.AuthenticationRepository
import com.example.travenorcustomer.data.DestinationsRepository
import com.example.travenorcustomer.features.BaseViewModel
import com.example.travenorcustomer.model.Destination
import io.github.jan.supabase.auth.user.UserInfo
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(
    val repository: AuthenticationRepository,
    val destinations: DestinationsRepository
) : BaseViewModel<HomeState, HomeAction, HomeEvent>(initialState = HomeState()) {

    init {
        mutableStateFlow.update {
            it.copy(
                userInfo = repository.userInfo()
            )
        }
        getAllDestinations()
    }

    private fun getAllDestinations() {
        viewModelScope.launch {
            destinations.getAllDestination()
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
                            destinations = data,
                            isLoading = false
                        )
                    }
                }
        }
    }

    override fun handleAction(action: HomeAction) {
        when (action) {
            is HomeAction.OnNavigateToDetailScreen -> {
                sendEvent(
                    HomeEvent.NavigateToDetailScreen(
                        action.destinationId,
                        state.userInfo?.id ?: ""
                    )
                )
            }
        }
    }
}

data class HomeState(
    val userInfo: UserInfo? = null,
    val destinations: List<Destination> = emptyList(),
    val isLoading: Boolean = false
) {


}

sealed interface HomeEvent {
    data class NavigateToDetailScreen(val destinationId: String, val userId: String) : HomeEvent
}

sealed interface HomeAction {
    data class OnNavigateToDetailScreen(val destinationId: String) : HomeAction
}