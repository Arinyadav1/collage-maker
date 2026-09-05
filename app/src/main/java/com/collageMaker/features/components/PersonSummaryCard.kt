package com.collageMaker.features.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
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
import com.collageMaker.data.model.PersonClusterResult
import com.collageMaker.ui.theme.AppColors
import kotlin.math.roundToInt

@Composable
fun PersonSummaryCard(
    person: PersonClusterResult,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.cardBackground),
        border = androidx.compose.foundation.BorderStroke(1.dp, AppColors.border)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                bitmap = person.croppedFaceTile.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(12.dp))
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.label_person, person.personId),
                        style = MaterialTheme.typography.titleMedium,
                        color = AppColors.customWhite
                    )

                    Box(
                        modifier = Modifier
                            .background(
                                color = AppColors.primaryBlue,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (person.appearanceCount == 1) {
                                stringResource(R.string.label_single_appearance)
                            } else {
                                stringResource(R.string.label_appearances, person.appearanceCount)
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = AppColors.customWhite
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                val frame = person.bestRepresentativeFrame
                val frontalityPct = (100 - (kotlin.math.abs(frame.eulerY) + kotlin.math.abs(frame.eulerZ)).roundToInt()).coerceIn(0, 100)
                val eyePct = (((frame.leftEyeOpenProb ?: 0.8f) + (frame.rightEyeOpenProb ?: 0.8f)) * 50).roundToInt().coerceIn(0, 100)
                val smilePct = ((frame.smilingProb ?: 0.5f) * 100).roundToInt().coerceIn(0, 100)

                Text(
                    text = stringResource(R.string.label_frontality, frontalityPct) + " • " +
                            stringResource(R.string.label_eyes_open, eyePct) + " • " +
                            stringResource(R.string.label_smiling, smilePct),
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.lightSub
                )
            }
        }
    }
}
