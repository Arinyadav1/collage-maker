package com.example.travenorcustomer.features.components

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.travenorcustomer.R
import com.example.travenorcustomer.ui.theme.AppColors

@Composable
fun TravenorIconButton(
    modifier: Modifier = Modifier,
    iconColor: Color = LocalContentColor.current,
    bgColor : Color = AppColors.grey.copy(alpha = 0.1f),
    onClick: () -> Unit
) {
    IconButton(
        modifier = modifier
            .clip(
                shape = CircleShape
            )
            .size(44.dp),
        onClick = { onClick() },
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = bgColor
        )
    ) {
        Icon(
            painter = painterResource(R.drawable.direction_left),
            contentDescription = null,
            tint = iconColor
        )
    }
}