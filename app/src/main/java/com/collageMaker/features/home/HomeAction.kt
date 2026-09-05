package com.collageMaker.features.home

import android.net.Uri
import com.collageMaker.data.model.CollageStyle
import com.collageMaker.data.model.VideoItem

sealed interface HomeAction {
    data class PickCustomVideo(val uri: Uri, val title: String) : HomeAction
    object StartProcessing : HomeAction
    data class ChangeCollageStyle(val style: CollageStyle) : HomeAction
    object SaveCollageToGallery : HomeAction
    object ShareCollage : HomeAction
    object DismissError : HomeAction
    object ResetProcessing : HomeAction
}
