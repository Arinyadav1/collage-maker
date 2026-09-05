package com.collageMaker.features


import com.collageMaker.features.home.HomeViewModel
import org.koin.dsl.module

val viewModelModule = module {
    factory { HomeViewModel(get()) }
}