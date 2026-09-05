package com.example.travenorcustomer

import android.app.Application
import com.example.travenorcustomer.data.repositoryModule
import com.example.travenorcustomer.features.viewModelModule
import com.example.travenorcustomer.network.networkModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.GlobalContext.startKoin
import org.koin.core.logger.Level

class KoinStart : Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger()
            androidContext(this@KoinStart)
            printLogger(Level.ERROR)
            modules(
                networkModule, repositoryModule, viewModelModule
            )
        }
    }
}