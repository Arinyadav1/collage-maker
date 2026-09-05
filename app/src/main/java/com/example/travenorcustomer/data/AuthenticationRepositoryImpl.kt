package com.example.travenorcustomer.data

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.OTP

class AuthenticationRepositoryImpl(
    private val supabaseClient: SupabaseClient

) : AuthenticationRepository {

    override suspend fun otpGeneration(email: String) {
        return supabaseClient.auth.signInWith(OTP) {
            this.email = email
        }
    }

    override suspend fun otpValidation(email: String, otp: String) {
        return supabaseClient.auth.verifyEmailOtp(
            type = OtpType.Email.EMAIL,
            email = email,
            token = otp
        )
    }

    override fun authSession() = supabaseClient.auth.sessionStatus

    override fun userInfo() = supabaseClient.auth.currentUserOrNull()

}