package com.collageMaker.features.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.collageMaker.R
import com.collageMaker.data.model.CollageStyle
import com.collageMaker.data.model.ProcessingResult
import com.collageMaker.data.model.SaveStatus
import com.collageMaker.ui.theme.AppColors

@Composable
fun CollagePreviewSection(
    result: ProcessingResult,
    selectedStyle: CollageStyle,
    onStyleChanged: (CollageStyle) -> Unit,
    saveStatus: SaveStatus,
    onSaveClicked: () -> Unit,
    onShareClicked: () -> Unit,
    onResetClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.title_generated_collage),
            style = MaterialTheme.typography.titleLarge,
            color = AppColors.customWhite,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Text(
            text = stringResource(R.string.label_select_style),
            style = MaterialTheme.typography.bodyMedium,
            color = AppColors.lightSub,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CollageStyle.values().forEach { style ->
                val isSelected = selectedStyle == style
                val label = when (style) {
                    CollageStyle.SOCIAL_POST -> stringResource(R.string.style_social_post)
                    CollageStyle.STORY_REEL -> stringResource(R.string.style_story_reel)
                    CollageStyle.EDITORIAL_CARD -> stringResource(R.string.style_editorial)
                }

                FilterChip(
                    selected = isSelected,
                    onClick = { onStyleChanged(style) },
                    label = { Text(text = label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AppColors.primaryBlue,
                        selectedLabelColor = AppColors.customWhite,
                        containerColor = AppColors.cardBackground,
                        labelColor = AppColors.lightSub
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        if (result.generatedCollageBitmap != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(9f / 16f)
                    .clip(RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = AppColors.customBlack),
                border = androidx.compose.foundation.BorderStroke(1.dp, AppColors.accent)
            ) {
                Image(
                    bitmap = result.generatedCollageBitmap.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PrimaryButton(
                text = if (saveStatus is SaveStatus.Saved) stringResource(R.string.status_saved) else stringResource(R.string.button_save_gallery),
                onClick = onSaveClicked,
                isLoading = saveStatus is SaveStatus.Saving,
                icon = Icons.Default.Download,
                modifier = Modifier.weight(1f)
            )

            PrimaryButton(
                text = stringResource(R.string.button_share_collage),
                onClick = onShareClicked,
                icon = Icons.Default.Share,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onResetClicked,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, AppColors.border)
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                tint = AppColors.lightSub,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.button_reset),
                style = MaterialTheme.typography.labelLarge,
                color = AppColors.lightSub
            )
        }
    }
}
