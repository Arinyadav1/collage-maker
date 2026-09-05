package com.collageMaker

import android.app.Application
import com.collageMaker.data.repositoryModule
import com.collageMaker.features.viewModelModule
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
                repositoryModule,
                viewModelModule
            )
        }
    }
}