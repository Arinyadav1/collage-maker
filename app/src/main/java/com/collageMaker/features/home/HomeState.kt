package com.collageMaker.features.home

import com.collageMaker.data.model.CollageStyle
import com.collageMaker.data.model.ProcessingResult
import com.collageMaker.data.model.ProcessingStatus
import com.collageMaker.data.model.SaveStatus
import com.collageMaker.data.model.VideoItem

data class HomeState(
    val sampleVideos: List<VideoItem> = emptyList(),
    val selectedVideo: VideoItem? = null,
    val selectedStyle: CollageStyle = CollageStyle.SOCIAL_POST,
    val processingStatus: ProcessingStatus = ProcessingStatus.Idle,
    val processingResult: ProcessingResult? = null,
    val saveStatus: SaveStatus = SaveStatus.Idle,
    val errorMessage: String? = null
)
