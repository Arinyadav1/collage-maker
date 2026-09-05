package com.example.travenorcustomer.features.details

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.travenorcustomer.R
import com.example.travenorcustomer.features.components.TravenorAsyncImage
import com.example.travenorcustomer.features.components.TravenorExpandableText
import com.example.travenorcustomer.features.components.TravenorIconButton
import com.example.travenorcustomer.features.components.TravenorShimmerLoadingOverlay
import com.example.travenorcustomer.features.components.TravenorTextButton
import com.example.travenorcustomer.ui.theme.AppColors
import com.example.travenorcustomer.ui.theme.AppTypography
import org.koin.androidx.compose.koinViewModel


@Composable
fun DetailsScreen(
    modifier: Modifier = Modifier,
    onNavigationBack: () -> Unit,
    viewModel: DetailsViewModel = koinViewModel()
) {
    val state = viewModel.stateFlow.collectAsStateWithLifecycle().value
    var bookingStatus by rememberSaveable { mutableStateOf(UiStatus.BOOK_NOW.value) }

    LaunchedEffect(Unit) {
        viewModel.event.collect { event ->
            when (event) {
                DetailsEvent.OnNavigateBack -> onNavigationBack()
                DetailsEvent.OnAcceptBooking -> bookingStatus = UiStatus.ACCEPTED.value
                DetailsEvent.OnRequestBooking -> bookingStatus = UiStatus.REQUESTING.value
            }
        }
    }
    Box(modifier = modifier.fillMaxSize()) {

        TravenorAsyncImage(
            image = state.destination?.coverImage,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.7f),
            contentScale = ContentScale.Crop
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp)
                .statusBarsPadding()
        ) {

            TravenorIconButton(
                iconColor = Color.White,
                onClick = {
                    viewModel.onAction(DetailsAction.OnNavigateBack)
                },
                bgColor = Color(0xFF1B1E28).copy(alpha = 0.2f),
                modifier = Modifier.align(Alignment.CenterStart)
            )

            Text(
                text = stringResource(R.string.detail),
                style = AppTypography.detailSmallTitle,
                color = Color.White,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        DestinationBottomCard(
            onBookNowClick = {
                viewModel.onAction(DetailsAction.OnRequestBooking)
            },
            modifier = Modifier.align(Alignment.BottomCenter),
            state = state,
            bookingStatus = bookingStatus
        )
    }
}


@Composable
fun DestinationBottomCard(
    onBookNowClick: () -> Unit,
    modifier: Modifier = Modifier,
    state: DetailsState,
    bookingStatus: String
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                .background(Color.White)
                .padding(20.dp)
        ) {

            Box(
                modifier = Modifier
                    .size(width = 36.dp, height = 5.dp)
                    .clip(RoundedCornerShape(50))
                    .background(AppColors.lightSub.copy(alpha = 0.3f))
                    .align(Alignment.CenterHorizontally)
            )

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = state.destination?.destinationName.orEmpty(),
                        style = AppTypography.detailTitle,
                        fontFamily = FontFamily(
                            Font(R.font.sf_pro_rounded_medium)
                        ),
                        fontSize = 24.sp,
                        lineHeight = 32.sp
                    )
                    Text(
                        text = "${state.destination?.city}, ${state.destination?.country}",
                        color = AppColors.lightSub,
                        style = AppTypography.detailBody,
                    )
                }
                Image(
                    painter = painterResource(R.drawable.user),
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(
                            CircleShape
                        )
                )
            }

            Spacer(Modifier.height(25.dp))

            Row(
                modifier = modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.location),
                        contentDescription = null,
                        tint = AppColors.lightSub,
                        modifier = Modifier.size(16.dp)
                    )

                    Text(
                        text = state.destination?.city.orEmpty(),
                        style = AppTypography.detailBody,
                        color = AppColors.lightSub
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        tint = AppColors.lightYellow,
                        modifier = Modifier.size(17.dp)
                    )

                    Spacer(modifier.height(5.dp))

                    Text(
                        text = state.destination?.rating.toString(),
                        style = AppTypography.detailBody,
                    )

                    Text(
                        text = "(${state.destination?.totalReview})",
                        style = AppTypography.detailBody,
                        color = AppColors.lightSub
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$${state.destination?.pricePerPerson}/",
                        style = AppTypography.detailBody,
                        color = AppColors.primaryBlue
                    )

                    Text(
                        text = stringResource(R.string.person),
                        style = AppTypography.detailBody,
                        color = AppColors.lightSub
                    )
                }
            }

            Spacer(Modifier.height(25.dp))

            Row(
                modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val imageList = state.destination?.images
                imageList?.forEachIndexed { index, value ->
                    if (index <= 4) {
                        Box {

                            TravenorAsyncImage(
                                image = value.image,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            )

                            if (imageList.size - 5 != 0) {
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.Black.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "+" + (imageList.size - 5), fontFamily = FontFamily(
                                            Font(R.font.sf_ui_display_regular)
                                        ), fontSize = 14.sp, color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(25.dp))

            Text(
                text = stringResource(R.string.about_destination),
                style = AppTypography.detailSmallTitle
            )

            Spacer(Modifier.height(10.dp))

            TravenorExpandableText(state.destination?.description.orEmpty())

            Spacer(Modifier.height(25.dp))

            TravenorTextButton(
                onClick = {
                    onBookNowClick()
                },
                btnText = bookingStatus
            )

        }

        if (state.isLoading) {
            TravenorShimmerLoadingOverlay()
        }
    }
}


