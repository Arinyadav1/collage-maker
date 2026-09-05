package com.collageMaker.features.home

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.collageMaker.R
import com.collageMaker.data.model.ProcessingStatus
import com.collageMaker.features.components.AppHeader
import com.collageMaker.features.components.CollagePreviewSection
import com.collageMaker.features.components.PersonSummaryCard
import com.collageMaker.features.components.PrimaryButton
import com.collageMaker.features.components.ProcessingProgressCard
import com.collageMaker.features.components.VideoSelectorSection
import com.collageMaker.ui.theme.AppColors
import org.koin.androidx.compose.koinViewModel

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = koinViewModel()
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.event.collect { event ->
            when (event) {
                is HomeEvent.ShowToast -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }
                is HomeEvent.OpenShareSheet -> {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "image/png"
                        putExtra(Intent.EXTRA_STREAM, event.uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share Collage via"))
                }
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = AppColors.customBlack,
        topBar = {
            AppHeader()
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            VideoSelectorSection(
                selectedVideo = state.selectedVideo,
                onCustomVideoPicked = { uri, title -> viewModel.onAction(HomeAction.PickCustomVideo(uri, title)) }
            )

            val isProcessing = state.processingStatus is ProcessingStatus.Processing

            PrimaryButton(
                text = if (isProcessing) stringResource(R.string.button_processing) else stringResource(R.string.button_process_video),
                onClick = { viewModel.onAction(HomeAction.StartProcessing) },
                enabled = state.selectedVideo != null,
                isLoading = isProcessing,
                icon = Icons.Default.AutoAwesome
            )

            if (state.processingStatus is ProcessingStatus.Processing) {
                val status = state.processingStatus as ProcessingStatus.Processing
                ProcessingProgressCard(
                    stepName = status.stepName,
                    progress = status.progress
                )
            }

            val result = state.processingResult
            if (result != null) {
                Text(
                    text = stringResource(R.string.title_recap_summary),
                    style = MaterialTheme.typography.titleLarge,
                    color = AppColors.customWhite,
                    modifier = Modifier.padding(top = 8.dp)
                )

                result.personClusters.forEach { person ->
                    PersonSummaryCard(person = person)
                }

                CollagePreviewSection(
                    result = result,
                    selectedStyle = state.selectedStyle,
                    onStyleChanged = { style -> viewModel.onAction(HomeAction.ChangeCollageStyle(style)) },
                    saveStatus = state.saveStatus,
                    onSaveClicked = { viewModel.onAction(HomeAction.SaveCollageToGallery) },
                    onShareClicked = { viewModel.onAction(HomeAction.ShareCollage) },
                    onResetClicked = { viewModel.onAction(HomeAction.ResetProcessing) }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}