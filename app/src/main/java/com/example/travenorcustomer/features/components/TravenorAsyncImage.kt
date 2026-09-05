package com.example.travenorcustomer.features.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.SubcomposeAsyncImage
import coil.request.CachePolicy

@Composable
fun TravenorAsyncImage(
    modifier: Modifier = Modifier,
    image: Any?,
    contentScale: ContentScale = ContentScale.Fit
) {
    val context = LocalContext.current
    val imageLoader =
        ImageLoader.Builder(context).crossfade(true).diskCachePolicy(CachePolicy.ENABLED)
            .build()

    SubcomposeAsyncImage(
        modifier = modifier,
        model = image,
        loading = {
            TravenorShimmerLoadingOverlay()
        },
        error = {
            TravenorShimmerLoadingOverlay()
        },
        contentDescription = null,
        imageLoader = imageLoader,
        contentScale = contentScale,
    )
}