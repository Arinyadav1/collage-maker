package com.collageMaker.features.home

import androidx.lifecycle.viewModelScope
import com.collageMaker.data.model.ProcessingStatus
import com.collageMaker.data.model.SaveStatus
import com.collageMaker.data.model.VideoItem
import com.collageMaker.data.repository.VideoRepository
import com.collageMaker.features.BaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HomeViewModel(
    private val videoRepository: VideoRepository
) : BaseViewModel<HomeState, HomeAction, HomeEvent>(HomeState()) {

    override fun handleAction(action: HomeAction) {
        when (action) {
            is HomeAction.PickCustomVideo -> {
                val customVideo = VideoItem(
                    id = "custom_${System.currentTimeMillis()}",
                    title = action.title,
                    description = "User Selected Gallery Video",
                    uri = action.uri,
                )
                mutableStateFlow.value = state.copy(
                    selectedVideo = customVideo,
                    processingResult = null,
                    processingStatus = ProcessingStatus.Idle
                )
            }
            is HomeAction.StartProcessing -> {
                startProcessingSelectedVideo()
            }
            is HomeAction.ChangeCollageStyle -> {
                mutableStateFlow.value = state.copy(selectedStyle = action.style)
                if (state.processingResult != null) {
                    startProcessingSelectedVideo()
                }
            }
            is HomeAction.SaveCollageToGallery -> {
                saveCollage()
            }
            is HomeAction.ShareCollage -> {
                shareCollage()
            }
            is HomeAction.DismissError -> {
                mutableStateFlow.value = state.copy(errorMessage = null)
            }
            is HomeAction.ResetProcessing -> {
                mutableStateFlow.value = state.copy(
                    processingStatus = ProcessingStatus.Idle,
                    processingResult = null
                )
            }
        }
    }

    private fun startProcessingSelectedVideo() {
        val video = state.selectedVideo ?: return
        viewModelScope.launch(Dispatchers.Default) {
            mutableStateFlow.value = state.copy(
                processingStatus = ProcessingStatus.Processing("Initializing engine...", 0.0f)
            )

            try {
                val result = videoRepository.processVideo(
                    videoItem = video,
                    collageStyle = state.selectedStyle,
                    onProgress = { stepName, progress ->
                        mutableStateFlow.value = state.copy(
                            processingStatus = ProcessingStatus.Processing(stepName, progress)
                        )
                    }
                )

                mutableStateFlow.value = state.copy(
                    processingStatus = ProcessingStatus.Success(result),
                    processingResult = result
                )
                sendEvent(HomeEvent.ShowToast("Collage generated successfully!"))
            } catch (e: Exception) {
                mutableStateFlow.value = state.copy(
                    processingStatus = ProcessingStatus.Error(e.message ?: "Failed to process video"),
                    errorMessage = e.message ?: "Processing Error"
                )
            }
        }
    }

    private fun saveCollage() {
        val bitmap = state.processingResult?.generatedCollageBitmap ?: return
        viewModelScope.launch(Dispatchers.IO) {
            mutableStateFlow.value = state.copy(saveStatus = SaveStatus.Saving)
            try {
                val filePath = videoRepository.saveCollageToGallery(bitmap)
                mutableStateFlow.value = state.copy(saveStatus = SaveStatus.Saved(filePath))
                sendEvent(HomeEvent.ShowToast("Collage saved to Gallery!"))
            } catch (e: Exception) {
                mutableStateFlow.value = state.copy(
                    saveStatus = SaveStatus.Error(e.message ?: "Save failed")
                )
                sendEvent(HomeEvent.ShowToast("Failed to save collage: ${e.message}"))
            }
        }
    }

    private fun shareCollage() {
        val uri = state.processingResult?.generatedCollageUri
        if (uri != null) {
            sendEvent(HomeEvent.OpenShareSheet(uri))
        } else {
            sendEvent(HomeEvent.ShowToast("Collage image unavailable for sharing"))
        }
    }
}