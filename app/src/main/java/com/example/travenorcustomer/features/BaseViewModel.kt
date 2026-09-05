package com.example.travenorcustomer.features

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

abstract class BaseViewModel<State, Action, Event>(
    initialState: State
) : ViewModel() {

    protected val mutableStateFlow = MutableStateFlow(initialState)
    val stateFlow: StateFlow<State> = mutableStateFlow.asStateFlow()

    protected val state: State get() = mutableStateFlow.value
    protected val evenFlow = MutableSharedFlow<Event>()
    val event: SharedFlow<Event> = evenFlow.asSharedFlow()

    fun onAction(action: Action) {
        handleAction(action)
    }

    protected abstract fun handleAction(action: Action)

    protected fun sendEvent(event: Event) {
        viewModelScope.launch {
            evenFlow.emit(event)
        }
    }
}
