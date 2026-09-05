package com.collageMaker.features.home

import android.net.Uri

sealed interface HomeEvent {
    data class ShowToast(val message: String) : HomeEvent
    data class OpenShareSheet(val uri: Uri) : HomeEvent
}
