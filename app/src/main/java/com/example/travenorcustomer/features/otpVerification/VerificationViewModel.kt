package com.example.travenorcustomer.features.otpVerification

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.example.travenorcustomer.data.AuthenticationRepository
import com.example.travenorcustomer.features.BaseViewModel
import com.example.travenorcustomer.features.navigation.VerificationScreenRoute
import io.github.jan.supabase.auth.exception.AuthRestException
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class VerificationViewModel(
    private val repository: AuthenticationRepository, savedStateHandle: SavedStateHandle
) : BaseViewModel<VerificationState, VerificationAction, VerificationEvent>(VerificationState()) {
    private val route = savedStateHandle.toRoute<VerificationScreenRoute>()
    private var countdownJob: Job? = null

    init {
        mutableStateFlow.update {
            it.copy(
                email = route.email,
            )
        }
    }

    private fun countDown() {
        countdownJob = viewModelScope.launch {
            countdownTimer(90).collect { time ->
                mutableStateFlow.update {
                    it.copy(
                        countDownTimer = time
                    )
                }
            }
        }
    }

    override fun handleAction(action: VerificationAction) {
        when (action) {
            is VerificationAction.OnOtpValueChange -> {
                mutableStateFlow.update {
                    it.copy(
                        otpValues = it.otpValues.mapIndexed { i, old ->
                            if (i == action.index) action.value else old
                        },
                        error = null
                    )
                }
            }

            VerificationAction.OnVerify -> {
                countdownJob?.cancel()

                if (!state.loading) {
                    countDown()
                    otpVerification()
                }
            }

            VerificationAction.OnNavigateBack -> {
                sendEvent(
                    VerificationEvent.OnNavigationBack
                )
            }

            VerificationAction.OnResendHandle -> {
                if (state.countDownTimer == null) {
                    countDown()
                    reSendOtp()
                }
            }
        }
    }

    fun otpVerification() {
        mutableStateFlow.update {
            it.copy(
                isShowResend = true, loading = true, error = null
            )
        }
        viewModelScope.launch {
            try {
                delay(10000)
                repository.otpValidation(
                    email = route.email, otp = state.otpValues.joinToString("")
                )

                sendEvent(
                    VerificationEvent.OnNavigationToHomeScreen
                )

                mutableStateFlow.update {
                    it.copy(
                        isShowResend = false, loading = false, countDownTimer = null
                    )
                }
            } catch (e: AuthRestException) {
                mutableStateFlow.update {
                    it.copy(
                        error = e.description, loading = false, countDownTimer = null
                    )
                }

            } catch (e: Throwable) {
                mutableStateFlow.update {
                    it.copy(
                        error = e.message, loading = false, countDownTimer = null
                    )
                }
            }
        }
    }

    private fun reSendOtp() {
        mutableStateFlow.update {
            it.copy(
                resendLoading = true
            )
        }
        viewModelScope.launch {
            delay(1000)

            repository.otpGeneration(
                email = route.email,
            )

            mutableStateFlow.update {
                it.copy(
                    loading = false,
                    isShowResend = false,
                    resendLoading = false,
                    countDownTimer = null
                )
            }

        }
    }

}

data class VerificationState(
    val email: String = "",
    val error: String? = null,
    val loading: Boolean = false,
    val resendLoading: Boolean = false,
    val otpValues: List<String> = listOf("", "", "", "", "", ""),
    val countDownTimer: String? = null,
    val isShowResend: Boolean = false,
)

sealed interface VerificationAction {
    data class OnOtpValueChange(val index: Int, val value: String) : VerificationAction
    object OnVerify : VerificationAction
    object OnNavigateBack : VerificationAction
    object OnResendHandle : VerificationAction
}

sealed interface VerificationEvent {
    object OnNavigationBack : VerificationEvent
    object OnNavigationToHomeScreen : VerificationEvent
}

fun countdownTimer(totalSeconds: Int): Flow<String?> = flow {
    for (seconds in totalSeconds downTo 0) {
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        emit("$minutes : $remainingSeconds")
        delay(1000)
    }

    emit(null)
}