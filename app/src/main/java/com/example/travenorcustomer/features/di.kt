package com.example.travenorcustomer.features

import com.example.travenorcustomer.features.details.DetailsViewModel
import com.example.travenorcustomer.features.home.HomeViewModel
import com.example.travenorcustomer.features.navigation.AppRootViewModel
import com.example.travenorcustomer.features.otpVerification.VerificationViewModel
import com.example.travenorcustomer.features.signIn.SignInViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val viewModelModule = module {

    viewModelOf(::SignInViewModel)
    viewModelOf(::VerificationViewModel)
    viewModelOf(::AppRootViewModel)
    viewModelOf(::HomeViewModel)
    viewModelOf(::DetailsViewModel)
}