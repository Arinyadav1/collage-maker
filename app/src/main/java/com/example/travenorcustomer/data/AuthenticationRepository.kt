package com.example.travenorcustomer.data

import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.user.UserInfo
import kotlinx.coroutines.flow.StateFlow

interface AuthenticationRepository {
    suspend fun otpGeneration(email: String)

    suspend fun otpValidation(
        email: String, otp: String
    )
    fun authSession (): StateFlow<SessionStatus>
    fun userInfo (): UserInfo?
}