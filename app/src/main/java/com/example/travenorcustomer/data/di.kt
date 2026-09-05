package com.example.travenorcustomer.data

import com.example.travenorcustomer.data.BookingRepository
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val repositoryModule = module {

    singleOf(::AuthenticationRepositoryImpl) { bind<AuthenticationRepository>() }
    singleOf(::DestinationsRepositoryImpl) { bind<DestinationsRepository>() }
    singleOf(::BookingRepositoryImpl) { bind<BookingRepository>() }
}