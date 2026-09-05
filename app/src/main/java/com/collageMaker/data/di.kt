package com.collageMaker.data


import com.collageMaker.data.repository.VideoRepository
import org.koin.dsl.module

val repositoryModule = module {
    single { VideoRepository(get()) }
}